package com.mzk.app;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @JavascriptInterface
    public void speak(String t) {
        Speak.say(c, t);
    }

    @JavascriptInterface
    public void setTimer(String json) {
        Notifs.timer(c, json);
    }

    @JavascriptInterface
    public String watchStatus() {
        try {
            return "{\"usage\":" + AppWatch.hasUsage(c) + ",\"overlay\":" + Settings.canDrawOverlays(c) + "}";
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public void openUsageSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public void openOverlaySettings() {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + c.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public String appsList() {
        try {
            PackageManager pm = c.getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> l = pm.queryIntentActivities(q, 0);
            JSONArray a = new JSONArray();
            Set<String> seen = new HashSet<String>();
            for (ResolveInfo r : l) {
                String p = r.activityInfo.packageName;
                if (p.equals(c.getPackageName()) || seen.contains(p)) continue;
                seen.add(p);
                JSONObject o = new JSONObject();
                o.put("p", p);
                o.put("n", r.loadLabel(pm).toString());
                a.put(o);
            }
            return a.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    @JavascriptInterface
    public void setTracked(String json) {
        try {
            JSONObject o = new JSONObject(json);
            JSONArray a = o.optJSONArray("a");
            StringBuilder sb = new StringBuilder();
            if (a != null) {
                for (int k = 0; k < a.length(); k++) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(a.optString(k));
                }
            }
            int w = o.optInt("w", 30);
            SharedPreferences s = c.getSharedPreferences("watch", Context.MODE_PRIVATE);
            boolean changed = !sb.toString().equals(s.getString("tracked", "")) || w != s.getInt("wait", 30);
            s.edit().putString("tracked", sb.toString()).putInt("wait", w).apply();
            if (changed || (!AppWatch.running && sb.length() > 0)) {
                AppWatch.ensure(c);
            }
        } catch (Exception e) {
        }
    }

    @JavascriptInterface
    public String appIcons(String json) {
        try {
            JSONArray a = new JSONArray(json);
            PackageManager pm = c.getPackageManager();
            JSONObject out = new JSONObject();
            for (int k = 0; k < a.length(); k++) {
                String p = a.optString(k);
                try {
                    Drawable d = pm.getApplicationIcon(p);
                    Bitmap bm = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888);
                    Canvas cv = new Canvas(bm);
                    d.setBounds(0, 0, 72, 72);
                    d.draw(cv);
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.PNG, 100, bo);
                    out.put(p, "data:image/png;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP));
                } catch (Exception e) {
                }
            }
            return out.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public String takePz() {
        try {
            SharedPreferences s = c.getSharedPreferences("mzk", Context.MODE_PRIVATE);
            String v = s.getString("pzq", "");
            if (!v.isEmpty()) s.edit().remove("pzq").apply();
            return v;
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String usageData() {
        try {
            UsageCalc.refresh(c);
            SharedPreferences s = c.getSharedPreferences("watch", Context.MODE_PRIVATE);
            return "{\"u\":" + s.getString("usage", "{}") + ",\"h\":" + s.getString("hours", "{}")
                    + ",\"n\":" + s.getString("names", "{}") + ",\"c\":" + s.getString("commits", "[]") + "}";
        } catch (Exception e) {
            return "{}";
        }
    }
}
