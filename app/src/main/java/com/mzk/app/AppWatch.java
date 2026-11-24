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
import android.service.notification.StatusBarNotification;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// بيراقب التطبيق اللي شغال دلوقتي وبيفتح نافذة السؤال لما تدخل تطبيق مختار. حساب الوقت نفسه في UsageCalc
public class AppWatch extends Service {
    static volatile boolean running = false;
    final Handler h = new Handler(Looper.getMainLooper());
    UsageStatsManager usm;
    long lastEv = 0;
    long lastPrompt = 0;
    long lastRefresh = 0;
    String cur = "";
    int wait = 30;
    int away = 0;
    Set<String> tracked = new HashSet<String>();
    Map<String, Long> until = new HashMap<String, Long>();
    Set<String> fresh = new HashSet<String>();
    Map<String, Long> lastCommit = new HashMap<String, Long>();

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

    // الخدمة محتاجة إشعار (قاعدة أندرويد). بنستخدم نفس إشعار المهام (رقم 1000) بدل إشعار «المراقبة» المنفصل
    void startFg() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notifs.ensure(this);
            Notification n = null;
            try {
                for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                    if (sbn.getId() == 1000) n = sbn.getNotification();
                }
            } catch (Exception e) {
            }
            if (n == null) n = Notifs.idleNote(this, "مفيش مهام دلوقتي");
            startForeground(1000, n);
            try {
                nm.cancel(56);
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        startFg();
        if (!running) {
            running = true;
            load();
            lastEv = System.currentTimeMillis() - 600000L;
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
        try {
            stopForeground(Service.STOP_FOREGROUND_DETACH);
        } catch (Exception e) {
        }
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
        until.clear();
        lastCommit.clear();
        try {
            JSONArray commits = new JSONArray(sp().getString("commits", "[]"));
            for (int k = 0; k < commits.length(); k++) {
                JSONObject c = commits.optJSONObject(k);
                if (c == null) continue;
                String p = c.optString("p");
                until.put(p, Long.valueOf(c.optLong("u")));
                lastCommit.put(p, Long.valueOf(c.optLong("id")));
            }
        } catch (Exception e) {
        }
        readCfg();
    }

    final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            try {
                step();
            } catch (Exception e) {
            }
            h.postDelayed(this, OverlayUi.checkShown ? 500 : 2000);
        }
    };

    void step() throws Exception {
        long now = System.currentTimeMillis();
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
        if (now - lastRefresh > 1800000L) {
            lastRefresh = now;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    UsageCalc.refresh(AppWatch.this);
                }
            }).start();
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean on = pm.isInteractive();
        // لو خرجت من التطبيق (أو قفلت الشاشة) وشاشة السؤال لسه ظاهرة: تتقفل لوحدها
        String cp = OverlayUi.checkPkg;
        if (OverlayUi.checkShown && cp != null && !"__test".equals(cp)) {
            if (!on || !cp.equals(cur)) {
                away++;
                if (away >= 2) {
                    away = 0;
                    fresh.add(cp);
                    OverlayUi.dismissCheck(this);
                }
            } else {
                away = 0;
            }
        } else {
            away = 0;
        }
        if (!on || !tracked.contains(cur)) return;
        long u = 0L;
        if (until.containsKey(cur)) u = until.get(cur).longValue();
        if (now >= u && !OverlayUi.checkShown && now - lastPrompt > 4000L) {
            lastPrompt = now;
            prompt(cur, now);
        }
    }

    String nameOf(String p) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString();
        } catch (Exception e) {
            return p;
        }
    }

    void prompt(String p, long now) {
        try {
            Long lc = lastCommit.get(p);
            boolean again = !fresh.contains(p) && lc != null && dayKey(lc.longValue()).equals(dayKey(now));
            OverlayUi.showCheck(getApplicationContext(), p, nameOf(p), again ? wait : 0);
        } catch (Exception e) {
        }
    }

    void addCommit(String p, String n, String w, long u) {
        if (p == null) return;
        synchronized (UsageCalc.LOCK) {
            try {
                JSONArray commits = new JSONArray(sp().getString("commits", "[]"));
                JSONObject c = new JSONObject();
                long now = System.currentTimeMillis();
                c.put("id", now);
                c.put("p", p);
                c.put("n", n == null ? p : n);
                c.put("w", w == null ? "" : w);
                c.put("u", u);
                c.put("use", 0L);
                c.put("over", 0L);
                commits.put(c);
                while (commits.length() > 300) commits.remove(0);
                sp().edit().putString("commits", commits.toString()).apply();
                until.put(p, Long.valueOf(u));
                lastCommit.put(p, Long.valueOf(now));
                fresh.remove(p);
            } catch (Exception e) {
            }
        }
    }
}
