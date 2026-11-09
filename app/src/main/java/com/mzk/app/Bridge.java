package com.mzk.app;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
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
            Util.silence(c, 0);
        } else if ("silence".equals(ev)) {
            Util.silence(c, v);
        } else if ("closeui".equals(ev)) {
            Util.closeAlarmUi(c);
        } else if ("openmain".equals(ev)) {
            Util.openMain(c);
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
    public boolean saveUpdate(String html, int ver) {
        return Util.saveOverride(c, html);
    }

    @JavascriptInterface
    public boolean rollbackUpdate() {
        return Util.rollbackOverride(c);
    }

    @JavascriptInterface
    public void resetUpdate() {
        Util.resetOverride(c);
    }

    @JavascriptInterface
    public void pageOk() {
        c.getSharedPreferences("mzk", Context.MODE_PRIVATE).edit().putBoolean("pending", false).apply();
    }

    @JavascriptInterface
    public void reloadPage() {
        if (c instanceof MainActivity) {
            ((MainActivity) c).reload();
        }
    }

    @JavascriptInterface
    public void saveState(String json) {
        try {
            c.getSharedPreferences("mzk", Context.MODE_PRIVATE).edit().putString("state", json).apply();
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public String loadState() {
        try {
            return c.getSharedPreferences("mzk", Context.MODE_PRIVATE).getString("state", "");
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public void pickTone() {
        if (c instanceof MainActivity) {
            ((MainActivity) c).pickTone();
        }
    }

    @JavascriptInterface
    public String toneName() {
        try {
            String s = c.getSharedPreferences("mzk", Context.MODE_PRIVATE).getString("tone", "");
            if (s.isEmpty()) return "الافتراضية";
            Ringtone r = RingtoneManager.getRingtone(c, Uri.parse(s));
            if (r != null) return r.getTitle(c);
        } catch (Exception e) {
        }
        return "مخصصة";
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
