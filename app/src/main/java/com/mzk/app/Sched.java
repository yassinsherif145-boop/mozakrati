package com.mzk.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public class Sched {

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("mzk", Context.MODE_PRIVATE);
    }

    static void save(Context c, String json) {
        prefs(c).edit().putString("sched", json).apply();
    }

    static PendingIntent firePi(Context c, int code, String action) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction(action);
        return PendingIntent.getBroadcast(c, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent showPi(Context c) {
        return PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static long next(JSONArray a, long now) {
        if (a == null) return 0;
        long best = 0;
        for (int i = 0; i < a.length(); i++) {
            long t = a.optLong(i);
            if (t > now + 1000 && (best == 0 || t < best)) best = t;
        }
        return best;
    }

    static void exact(AlarmManager am, long t, PendingIntent pi) {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, t, pi);
            }
        } catch (Exception e) {
            am.set(AlarmManager.RTC_WAKEUP, t, pi);
        }
    }

    static void arm(Context c) {
        try {
            JSONObject o = new JSONObject(prefs(c).getString("sched", "{}"));
            long now = System.currentTimeMillis();
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            long r = next(o.optJSONArray("rings"), now);
            long k = next(o.optJSONArray("cuts"), now);
            long q = next(o.optJSONArray("rsn"), now);
            if (r > 0) {
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(r, showPi(c)), firePi(c, 1, "RING"));
            } else {
                am.cancel(firePi(c, 1, "RING"));
            }
            if (k > 0) {
                exact(am, k, firePi(c, 3, "CUT"));
            } else {
                am.cancel(firePi(c, 3, "CUT"));
            }
            if (q > 0) {
                exact(am, q, firePi(c, 4, "RSN"));
            } else {
                am.cancel(firePi(c, 4, "RSN"));
            }
        } catch (Exception e) {
        }
    }

    static void snooze(Context c, int min) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            long t = System.currentTimeMillis() + min * 60000L;
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, showPi(c)), firePi(c, 2, "RING"));
        } catch (Exception e) {
        }
    }
}
