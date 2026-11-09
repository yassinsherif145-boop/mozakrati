package com.mzk.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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

    static final String ASSET = "file:///android_asset/index.html";

    static File ovFile(Context c) {
        return new File(c.getFilesDir(), "index_override.html");
    }

    static File prevFile(Context c) {
        return new File(c.getFilesDir(), "index_prev.html");
    }

    static int vcode(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return -1;
        }
    }

    static void copy(File a, File b) throws Exception {
        FileInputStream in = new FileInputStream(a);
        FileOutputStream out = new FileOutputStream(b);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        out.close();
    }

    // لو فيه تحديث محمّل للصفحة بيستخدمه، غير كده النسخة اللي جوه الـAPK
    public static String pageUrl(Context c) {
        try {
            File f = ovFile(c);
            if (f.exists()) {
                SharedPreferences p = c.getSharedPreferences("mzk", Context.MODE_PRIVATE);
                if (p.getInt("updVc", -1) != vcode(c)) {
                    f.delete();
                } else {
                    return "file://" + f.getAbsolutePath();
                }
            }
        } catch (Exception e) {
        }
        return ASSET;
    }

    public static boolean saveOverride(Context c, String html) {
        try {
            File f = ovFile(c);
            if (f.exists()) copy(f, prevFile(c));
            FileOutputStream o = new FileOutputStream(f);
            o.write(html.getBytes("UTF-8"));
            o.close();
            c.getSharedPreferences("mzk", Context.MODE_PRIVATE).edit().putInt("updVc", vcode(c)).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean rollbackOverride(Context c) {
        try {
            File p = prevFile(c);
            if (!p.exists()) return false;
            copy(p, ovFile(c));
            p.delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void resetOverride(Context c) {
        try {
            ovFile(c).delete();
            prevFile(c).delete();
        } catch (Exception e) {
        }
    }

    public static void openMain(Context c) {
        try {
            Intent i = new Intent(c, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            c.startActivity(i);
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
