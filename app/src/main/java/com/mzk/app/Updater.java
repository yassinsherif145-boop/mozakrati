package com.mzk.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// تحديث التطبيق من صفحة Releases على GitHub (الـ workflow بينشر APK جديد بعد كل بناء)
public class Updater {
    static final String API = "https://api.github.com/repos/yassinsherif145-boop/mozakrati/releases/latest";
    static volatile String st = "idle";
    static volatile String msg = "";
    static volatile int cur = 0, nw = 0, pct = 0;
    static volatile String url = "";
    static volatile String tag = "";
    static volatile boolean busy = false;

    static int curCode(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    static String json() {
        try {
            JSONObject o = new JSONObject();
            o.put("st", st);
            o.put("msg", msg);
            o.put("cur", cur);
            o.put("nw", nw);
            o.put("pct", pct);
            o.put("tag", tag);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    static void check(final Context c0) {
        if (busy) return;
        busy = true;
        final Context c = c0.getApplicationContext();
        st = "checking";
        msg = "";
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection h = null;
                try {
                    cur = curCode(c);
                    h = (HttpURLConnection) new URL(API).openConnection();
                    h.setConnectTimeout(12000);
                    h.setReadTimeout(15000);
                    h.setRequestProperty("Accept", "application/vnd.github+json");
                    h.setRequestProperty("User-Agent", "mozakrati-app");
                    int code = h.getResponseCode();
                    if (code != 200) {
                        st = "err";
                        msg = code == 404 ? "مفيش نسخة منشورة لسه (أو المستودع مش Public)" : "فشل الاتصال (" + code + ")";
                        return;
                    }
                    InputStream in = h.getInputStream();
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    byte[] b = new byte[4096];
                    int n;
                    while ((n = in.read(b)) > 0) bo.write(b, 0, n);
                    in.close();
                    JSONObject o = new JSONObject(bo.toString("UTF-8"));
                    tag = o.optString("tag_name", "");
                    String digits = tag.replaceAll("[^0-9]", "");
                    nw = digits.isEmpty() ? 0 : 100 + Integer.parseInt(digits);
                    url = "";
                    JSONArray as = o.optJSONArray("assets");
                    if (as != null) {
                        for (int i = 0; i < as.length(); i++) {
                            JSONObject a = as.getJSONObject(i);
                            if (a.optString("name").endsWith(".apk")) {
                                url = a.optString("browser_download_url");
                                break;
                            }
                        }
                    }
                    if (url.isEmpty() || nw == 0) {
                        st = "err";
                        msg = "النسخة المنشورة مفيهاش APK";
                    } else if (nw > cur) {
                        st = "new";
                    } else {
                        st = "none";
                    }
                } catch (Exception e) {
                    st = "err";
                    msg = "مفيش نت أو الاتصال فشل";
                } finally {
                    busy = false;
                    try {
                        if (h != null) h.disconnect();
                    } catch (Exception e) {
                    }
                }
            }
        }).start();
    }

    static void install(final Context c0) {
        final Context c = c0.getApplicationContext();
        try {
            if (Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls()) {
                st = "perm";
                msg = "فعّل «السماح من هذا المصدر» لمذاكرتي، وارجع دوس تحديث تاني";
                Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return;
            }
        } catch (Exception e) {
        }
        if (busy || url.isEmpty()) return;
        busy = true;
        st = "dl";
        pct = 0;
        msg = "";
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection h = null;
                try {
                    File dir = new File(c.getCacheDir(), "apk");
                    dir.mkdirs();
                    File[] old = dir.listFiles();
                    if (old != null) {
                        for (File f : old) f.delete();
                    }
                    File out = new File(dir, "mozakrati.apk");
                    h = (HttpURLConnection) new URL(url).openConnection();
                    h.setConnectTimeout(15000);
                    h.setReadTimeout(30000);
                    h.setRequestProperty("User-Agent", "mozakrati-app");
                    if (h.getResponseCode() != 200) {
                        st = "err";
                        msg = "فشل التحميل (" + h.getResponseCode() + ")";
                        return;
                    }
                    long total = h.getContentLength();
                    InputStream in = h.getInputStream();
                    OutputStream os = new FileOutputStream(out);
                    byte[] b = new byte[16384];
                    long got = 0;
                    int n;
                    while ((n = in.read(b)) > 0) {
                        os.write(b, 0, n);
                        got += n;
                        if (total > 0) pct = (int) (got * 100L / total);
                    }
                    os.close();
                    in.close();
                    if (out.length() < 100000) {
                        st = "err";
                        msg = "الملف المحمّل ناقص";
                        return;
                    }
                    pct = 100;
                    st = "ready";
                    Uri u = androidx.core.content.FileProvider.getUriForFile(c, c.getPackageName() + ".fp", out);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(u, "application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(i);
                } catch (Exception e) {
                    st = "err";
                    msg = "فشل التحميل أو التثبيت";
                } finally {
                    busy = false;
                    try {
                        if (h != null) h.disconnect();
                    } catch (Exception e) {
                    }
                }
            }
        }).start();
    }
}
