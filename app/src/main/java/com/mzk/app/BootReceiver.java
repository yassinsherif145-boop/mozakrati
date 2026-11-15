package com.mzk.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        try {
            Sched.arm(c);
        } catch (Exception e) {
        }
        try {
            AppWatch.ensure(c);
        } catch (Exception e) {
        }
    }
}
