package com.mzk.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TimerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        try {
            String a = i.getAction();
            if ("PAUSE".equals(a)) {
                if (OverlayUi.can(c)) {
                    OverlayUi.showPause(c.getApplicationContext(), System.currentTimeMillis());
                } else {
                    Util.openAlarm(c, "#pz");
                }
            }
        } catch (Exception e) {
        }
    }
}
