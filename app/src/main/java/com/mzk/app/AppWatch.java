package com.mzk.app;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// بيراقب التطبيق اللي شغال دلوقتي، وبيسجل الوقت، وبيفتح شاشة السؤال لما تدخل تطبيق مختار
public class AppWatch extends Service {
    static volatile boolean running = false;
    final Handler h = new Handler(Looper.getMainLooper());
    UsageStatsManager usm;
    long lastEv = 0;
    long lastTick = 0;
    long lastSave = 0;
    long lastPrompt = 0;
    String cur = "";
    boolean dirty = false;
    int wait = 30;
    Set<String> tracked = new HashSet<String>();
    JSONObject usage = new JSONObject();
    JSONObject hours = new JSONObject();
    JSONObject names = new JSONObject();
    JSONArray commits = new JSONArray();
    Map<String, Long> until = new HashMap<String, Long>();
    Map<String, JSONObject> lastC = new HashMap<String, JSONObject>();

    SharedPreferences sp() {
        return getSharedPreferences("watch", Context.MODE_PRIVATE);
    }

    static boolean hasUsage(Context c) {
        try {
            AppOpsManager ao = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            int m = ao.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), c.getPackageName());
            return m == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    static String dayKey(long t) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(t));
    }

    // يشغّل الخدمة لو فيه تطبيقات مختارة والإذن موجود، وغير كده يوقفها
    static void ensure(Context c) {
        try {
            SharedPreferences s = c.getSharedPreferences("watch", Context.MODE_PRIVATE);
            boolean any = !s.getString("tracked", "").isEmpty();
            if (any && hasUsage(c)) {
                Intent i = new Intent(c, AppWatch.class);
                i.setAction("CFG");
                if (Build.VERSION.SDK_INT >= 26) {
                    c.startForegroundService(i);
                } else {
                    c.startService(i);
                }
            } else {
                c.stopService(new Intent(c, AppWatch.class));
            }
        } catch (Exception e) {
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    void startFg() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(new NotificationChannel("watch", "مراقبة التطبيقات", NotificationManager.IMPORTANCE_LOW));
            }
            Notification n = new Notification.Builder(this, "watch")
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle("مراقبة التطبيقات شغالة")
                    .setOngoing(true)
                    .build();
            startForeground(56, n);
        } catch (Exception e) {
        }
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        startFg();
        if (!running) {
            running = true;
            load();
            lastTick = System.currentTimeMillis();
            lastEv = lastTick - 5000;
            h.post(tick);
        }
        String a = (i == null) ? null : i.getAction();
        if ("CFG".equals(a)) {
            readCfg();
        } else if ("COMMIT".equals(a)) {
            addCommit(i.getStringExtra("p"), i.getStringExtra("n"), i.getStringExtra("w"), i.getLongExtra("u", 0L));
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        h.removeCallbacks(tick);
        if (dirty) save();
        super.onDestroy();
    }

    void readCfg() {
        tracked.clear();
        String a = sp().getString("tracked", "");
        for (String s : a.split(",")) {
            if (!s.isEmpty()) tracked.add(s);
        }
        wait = sp().getInt("wait", 30);
    }

    void load() {
        try {
            usage = new JSONObject(sp().getString("usage", "{}"));
        } catch (Exception e) {
            usage = new JSONObject();
        }
        try {
            hours = new JSONObject(sp().getString("hours", "{}"));
        } catch (Exception e) {
            hours = new JSONObject();
        }
        try {
            names = new JSONObject(sp().getString("names", "{}"));
        } catch (Exception e) {
            names = new JSONObject();
        }
        try {
            commits = new JSONArray(sp().getString("commits", "[]"));
        } catch (Exception e) {
            commits = new JSONArray();
        }
        until.clear();
        lastC.clear();
        for (int k = 0; k < commits.length(); k++) {
            JSONObject c = commits.optJSONObject(k);
            if (c == null) continue;
            String p = c.optString("p");
            until.put(p, c.optLong("u"));
            lastC.put(p, c);
        }
        readCfg();
    }

    void save() {
        try {
            sp().edit()
                    .putString("usage", usage.toString())
                    .putString("hours", hours.toString())
                    .putString("names", names.toString())
                    .putString("commits", commits.toString())
                    .apply();
        } catch (Exception e) {
        }
        dirty = false;
        lastSave = System.currentTimeMillis();
    }

    final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            try {
                step();
            } catch (Exception e) {
            }
            h.postDelayed(this, 2000);
        }
    };

    void step() throws Exception {
        long now = System.currentTimeMillis();
        long dt = Math.min(now - lastTick, 5000L);
        lastTick = now;
        if (tracked.isEmpty() || !hasUsage(this)) return;
        if (usm == null) usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents ue = usm.queryEvents(lastEv, now);
        UsageEvents.Event e = new UsageEvents.Event();
        while (ue.hasNextEvent()) {
            ue.getNextEvent(e);
            int t = e.getEventType();
            String p = e.getPackageName();
            if (t == 1) {
                cur = p;
            } else if (t == 2) {
                if (p.equals(cur)) cur = "";
            } else if (t == 16) {
                cur = "";
            }
        }
        lastEv = now;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean on = pm.isInteractive();
        if (!on || !tracked.contains(cur)) {
            if (dirty && now - lastSave > 15000L) save();
            return;
        }
        addUse(cur, dt, now);
        long u = 0L;
        if (until.containsKey(cur)) u = until.get(cur).longValue();
        if (now >= u && !CheckActivity.visible && now - lastPrompt > 4000L) {
            lastPrompt = now;
            prompt(cur, now);
        }
        if (dirty && now - lastSave > 15000L) save();
    }

    void prune() {
        while (usage.length() > 45) {
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

    void addUse(String p, long dt, long now) throws Exception {
        String day = dayKey(now);
        JSONObject d = usage.optJSONObject(day);
        if (d == null) {
            d = new JSONObject();
            usage.put(day, d);
            prune();
        }
        d.put(p, d.optLong(p) + dt);
        JSONArray hr = hours.optJSONArray(day);
        if (hr == null) {
            hr = new JSONArray();
            for (int k = 0; k < 24; k++) hr.put(0L);
            hours.put(day, hr);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        int hh = cal.get(Calendar.HOUR_OF_DAY);
        hr.put(hh, hr.optLong(hh) + dt);
        JSONObject lc = lastC.get(p);
        if (lc != null) {
            if (now <= lc.optLong("u")) {
                lc.put("use", lc.optLong("use") + dt);
            } else {
                lc.put("over", lc.optLong("over") + dt);
            }
        }
        dirty = true;
    }

    String nameOf(String p) {
        try {
            PackageManager pm = getPackageManager();
            String n = pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString();
            names.put(p, n);
            return n;
        } catch (Exception e) {
            return p;
        }
    }

    void prompt(String p, long now) {
        try {
            Intent i = new Intent(this, CheckActivity.class);
            i.putExtra("p", p);
            i.putExtra("n", nameOf(p));
            JSONObject lc = lastC.get(p);
            boolean again = lc != null && dayKey(lc.optLong("id")).equals(dayKey(now));
            i.putExtra("w", again ? wait : 0);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        } catch (Exception e) {
        }
    }

    void addCommit(String p, String n, String w, long u) {
        try {
            if (p == null) return;
            JSONObject c = new JSONObject();
            c.put("id", System.currentTimeMillis());
            c.put("p", p);
            c.put("n", n == null ? p : n);
            c.put("w", w == null ? "" : w);
            c.put("u", u);
            c.put("use", 0L);
            c.put("over", 0L);
            commits.put(c);
            while (commits.length() > 300) commits.remove(0);
            until.put(p, u);
            lastC.put(p, c);
            names.put(p, n == null ? p : n);
            save();
        } catch (Exception e) {
        }
    }
}
