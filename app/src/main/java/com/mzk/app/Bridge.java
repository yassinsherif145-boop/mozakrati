package com.mzk.app;

import android.content.Context;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.FileOutputStream;

public class Bridge {
    final Context c;

    Bridge(Context c) {
        this.c = c;
    }

    @JavascriptInterface
    public void onAlarm(String ev, int v) {
        if ("started".equals(ev) || "reasoned".equals(ev)) {
            Util.stopAlarm(c);
            Util.closeAlarmUi(c);
        } else if ("snooze".equals(ev)) {
            Util.stopAlarm(c);
            Sched.snooze(c, v);
            Util.closeAlarmUi(c);
        } else if ("reason".equals(ev)) {
            Util.silence(c);
        } else if ("closeui".equals(ev)) {
            Util.closeAlarmUi(c);
        }
    }

    @JavascriptInterface
    public void setTasks(String json) {
        Notifs.show(c, json);
    }

    @JavascriptInterface
    public void setSchedule(String json) {
        Sched.save(c, json);
        Sched.arm(c);
    }

    @JavascriptInterface
    public void testRing(int min) {
        Sched.snooze(c, min);
    }

    @JavascriptInterface
    public String saveText(String name, String text) {
        try {
            File d = c.getExternalFilesDir(null);
            File f = new File(d, name);
            FileOutputStream o = new FileOutputStream(f);
            o.write(text.getBytes("UTF-8"));
            o.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            return "فشل: " + e.getMessage();
        }
    }
}
