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
            Intent s = new Intent(c, AlarmService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                c.startForegroundService(s);
            } else {
                c.startService(s);
            }
            Sched.arm(c);
        } else if ("CUT".equals(a)) {
            Notifs.clear(c);
            Util.openAlarm(c, "#warn");
            Sched.arm(c);
        }
    }
}
