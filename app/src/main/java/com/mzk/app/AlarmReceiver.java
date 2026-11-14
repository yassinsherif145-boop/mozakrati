package com.mzk.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if ("RING".equals(a)) {
            start(c, null);
            Sched.arm(c);
        } else if ("RSN".equals(a)) {
            start(c, "reason");
            Sched.arm(c);
        } else if ("WKL".equals(a)) {
            start(c, "weekly");
            Sched.arm(c);
        } else if ("CUT".equals(a)) {
            Notifs.clear(c);
            Util.openAlarm(c, "#warn");
            Sched.arm(c);
        }
    }

    static void start(Context c, String mode) {
        Intent s = new Intent(c, AlarmService.class);
        if (mode != null) s.putExtra("mode", mode);
        if (Build.VERSION.SDK_INT >= 26) {
            c.startForegroundService(s);
        } else {
            c.startService(s);
        }
    }
}
