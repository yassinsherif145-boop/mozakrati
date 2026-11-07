package com.mzk.app;

import android.content.Context;
import android.content.Intent;

public class Util {

    public static void closeAlarmUi(Context c) {
        Intent i = new Intent("com.mzk.app.CLOSE");
        i.setPackage(c.getPackageName());
        c.sendBroadcast(i);
    }

    public static void stopAlarm(Context c) {
        try {
            c.stopService(new Intent(c, AlarmService.class));
        } catch (Exception e) {
        }
    }

    public static void silence(Context c) {
        silence(c, 0);
    }

    // sec = 0: هدوء لحد ما المستخدم يخرج من الشاشة. sec > 0: هدوء المدة دي ثم يرن تاني لو مفيش نشاط
    public static void silence(Context c, int sec) {
        try {
            Intent i = new Intent(c, AlarmService.class);
            i.setAction("SILENCE");
            i.putExtra("sec", sec);
            c.startService(i);
        } catch (Exception e) {
        }
    }

    public static void openAlarm(Context c, String route) {
        try {
            Intent i = new Intent(c, AlarmActivity.class);
            i.putExtra("route", route);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            c.startActivity(i);
        } catch (Exception e) {
        }
    }
}
