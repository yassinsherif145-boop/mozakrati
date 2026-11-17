package com.mzk.app;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// بيحسب وقت الاستخدام من سجل أحداث الأندرويد نفسه (مش من عداد بيعدّ لوحده)، فمش بيتأثر لو الخدمة اتقفلت
public class UsageCalc {
    static final Object LOCK = new Object();
    static final long DAY = 86400000L;
    static final long MAXSEG = 6L * 3600000L;

    static long startOfDay(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    static void close(Map<String, List<long[]>> out, String p, Long a, long b) {
        if (a == null) return;
        long s = a.longValue();
        if (b <= s) return;
        long e = Math.min(b, s + MAXSEG);
        List<long[]> l = out.get(p);
        if (l == null) {
            l = new ArrayList<long[]>();
            out.put(p, l);
        }
        l.add(new long[]{s, e});
    }

    // فترات الاستخدام لكل تطبيق: كل فترة من فتح الشاشة لحد قفلها أو الخروج من التطبيق
    static Map<String, List<long[]>> segments(Context c, long from, long to, Set<String> pkgs) {
        Map<String, List<long[]>> out = new HashMap<String, List<long[]>>();
        UsageStatsManager usm = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents ue = usm.queryEvents(from, to);
        UsageEvents.Event e = new UsageEvents.Event();
        Map<String, Set<String>> open = new HashMap<String, Set<String>>();
        Map<String, Long> start = new HashMap<String, Long>();
        while (ue.hasNextEvent()) {
            ue.getNextEvent(e);
            int t = e.getEventType();
            long ts = e.getTimeStamp();
            String p = e.getPackageName();
            if (t == 1) {
                if (!pkgs.contains(p)) continue;
                Set<String> s = open.get(p);
                if (s == null) {
                    s = new HashSet<String>();
                    open.put(p, s);
                }
                if (s.isEmpty()) start.put(p, Long.valueOf(ts));
                String cls = e.getClassName();
                s.add(cls == null ? "" : cls);
            } else if (t == 2) {
                Set<String> s = open.get(p);
                if (s == null || s.isEmpty()) continue;
                String cls = e.getClassName();
                s.remove(cls == null ? "" : cls);
                if (s.isEmpty()) close(out, p, start.get(p), ts);
            } else if (t == 16 || t == 17 || t == 26) {
                for (String q : new ArrayList<String>(open.keySet())) {
                    Set<String> s = open.get(q);
                    if (s != null && !s.isEmpty()) {
                        s.clear();
                        close(out, q, start.get(q), ts);
                    }
                }
            }
        }
        for (String q : open.keySet()) {
            Set<String> s = open.get(q);
            if (s != null && !s.isEmpty()) close(out, q, start.get(q), to);
        }
        return out;
    }

    static void addSeg(JSONObject usage, Map<String, long[]> hmap, String p, long a, long b) throws Exception {
        long cur = a;
        while (cur < b) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(cur);
            int hh = c.get(Calendar.HOUR_OF_DAY);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            long end = Math.min(b, c.getTimeInMillis() + 3600000L);
            if (end <= cur) end = b;
            long d = end - cur;
            String k = AppWatch.dayKey(cur);
            JSONObject o = usage.optJSONObject(k);
            if (o == null) {
                o = new JSONObject();
                usage.put(k, o);
            }
            o.put(p, o.optLong(p) + d);
            long[] hv = hmap.get(k);
            if (hv == null) {
                hv = new long[24];
                hmap.put(k, hv);
            }
            hv[hh] += d;
            cur = end;
        }
    }

    static long overlap(long a1, long a2, long b1, long b2) {
        return Math.max(0L, Math.min(a2, b2) - Math.max(a1, b1));
    }

    static void prune(JSONObject usage, JSONObject hours) {
        while (usage.length() > 400) {
            String oldest = null;
            Iterator<String> it = usage.keys();
            while (it.hasNext()) {
                String k = it.next();
                if (oldest == null || k.compareTo(oldest) < 0) oldest = k;
            }
            if (oldest == null) break;
            usage.remove(oldest);
            hours.remove(oldest);
        }
    }

    // بيعيد حساب آخر 8 أيام من سجل الأندرويد ويحفظها، وبيسيب الأيام الأقدم زي ما اتحفظت
    static void refresh(Context c) {
        synchronized (LOCK) {
            try {
                if (!AppWatch.hasUsage(c)) return;
                SharedPreferences sp = c.getSharedPreferences("watch", Context.MODE_PRIVATE);
                String csv = sp.getString("tracked", "");
                if (csv.isEmpty()) return;
                Set<String> pk = new HashSet<String>();
                for (String s : csv.split(",")) {
                    if (!s.isEmpty()) pk.add(s);
                }
                long now = System.currentTimeMillis();
                long from = startOfDay(now - 8L * DAY);
                Map<String, List<long[]>> segs = segments(c, from, now, pk);
                JSONObject usage = new JSONObject(sp.getString("usage", "{}"));
                JSONObject hours = new JSONObject(sp.getString("hours", "{}"));
                JSONObject names = new JSONObject(sp.getString("names", "{}"));
                for (int i = 0; i <= 9; i++) {
                    String k = AppWatch.dayKey(from + i * DAY + 12L * 3600000L);
                    JSONObject o = usage.optJSONObject(k);
                    if (o != null) {
                        for (String p : pk) {
                            o.remove(p);
                        }
                    }
                    hours.remove(k);
                }
                Map<String, long[]> hmap = new HashMap<String, long[]>();
                PackageManager pm = c.getPackageManager();
                for (Map.Entry<String, List<long[]>> en : segs.entrySet()) {
                    String p = en.getKey();
                    for (long[] sg : en.getValue()) {
                        addSeg(usage, hmap, p, sg[0], sg[1]);
                    }
                    try {
                        names.put(p, pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString());
                    } catch (Exception e2) {
                    }
                }
                for (Map.Entry<String, long[]> en : hmap.entrySet()) {
                    JSONArray a = new JSONArray();
                    for (int h = 0; h < 24; h++) {
                        a.put(en.getValue()[h]);
                    }
                    hours.put(en.getKey(), a);
                }
                JSONArray commits = new JSONArray(sp.getString("commits", "[]"));
                for (int i = 0; i < commits.length(); i++) {
                    JSONObject cm = commits.optJSONObject(i);
                    if (cm == null) continue;
                    long id = cm.optLong("id");
                    String p = cm.optString("p");
                    if (id < from || !pk.contains(p)) continue;
                    long u = cm.optLong("u");
                    long next = Long.MAX_VALUE;
                    for (int j = i + 1; j < commits.length(); j++) {
                        JSONObject c2 = commits.optJSONObject(j);
                        if (c2 != null && p.equals(c2.optString("p"))) {
                            next = c2.optLong("id");
                            break;
                        }
                    }
                    long overEnd = Math.min(next, u + MAXSEG);
                    long use = 0L;
                    long over = 0L;
                    List<long[]> l = segs.get(p);
                    if (l != null) {
                        for (long[] sg : l) {
                            use += overlap(sg[0], sg[1], id, u);
                            over += overlap(sg[0], sg[1], u, overEnd);
                        }
                    }
                    cm.put("use", use);
                    cm.put("over", over);
                }
                prune(usage, hours);
                sp.edit()
                        .putString("usage", usage.toString())
                        .putString("hours", hours.toString())
                        .putString("names", names.toString())
                        .putString("commits", commits.toString())
                        .apply();
            } catch (Exception e) {
            }
        }
    }
}
