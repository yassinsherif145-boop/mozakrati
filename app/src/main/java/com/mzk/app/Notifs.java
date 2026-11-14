package com.mzk.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class Notifs {
    static final String CH = "tasks";
    static final int FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    static final int GREY = 0xFF37474F;

    static void ensure(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel tasks = new NotificationChannel(CH, "مهام اليوم", NotificationManager.IMPORTANCE_LOW);
            NotificationChannel alarm = new NotificationChannel("alarm", "المنبه", NotificationManager.IMPORTANCE_HIGH);
            try {
                tasks.setBypassDnd(true);
                alarm.setBypassDnd(true);
            } catch (Exception e) {
            }
            nm.createNotificationChannel(tasks);
            nm.createNotificationChannel(alarm);
        }
    }

    static void clear(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences p = c.getSharedPreferences("mzk", Context.MODE_PRIVATE);
        String ids = p.getString("nids", "");
        for (String s : ids.split(",")) {
            if (!s.isEmpty()) nm.cancel(Integer.parseInt(s));
        }
        nm.cancel(900);
        p.edit().putString("nids", "").putString("lastjson", "").apply();
    }

    static int ugColor(int lv) {
        if (lv >= 2) return 0xFFB23A3A;
        if (lv == 1) return 0xFFA8741A;
        return 0xFF5F8070;
    }

    static String ugLabel(int lv) {
        if (lv >= 2) return "لا يوجد وقت آخر";
        if (lv == 1) return "من الأفضل أن تبدأ دلوقتي";
        return "على المسار";
    }

    static int ugIcon(int lv) {
        if (lv >= 2) return R.drawable.ic_u2;
        if (lv == 1) return R.drawable.ic_u1;
        return R.drawable.ic_u0;
    }

    // الإلحاح بيظهر للامتحان والواجب وللمهام اللي ليها ميعاد بس
    static boolean hasUg(JSONObject t) {
        String ty = t.optString("ty");
        return "exam".equals(ty) || "hw".equals(ty) || t.optInt("n", 99) < 90;
    }

    static String kindOf(JSONObject t) {
        String kw = t.optString("kw", "");
        if (!kw.isEmpty()) return kw;
        String ty = t.optString("ty");
        if ("exam".equals(ty)) return "امتحان";
        if ("hw".equals(ty)) return "واجب";
        if ("resp".equals(ty)) return "مسؤول";
        if ("dev".equals(ty)) return "التطوير";
        return "";
    }

    static String leftOf(JSONObject t) {
        int n = t.optInt("n", 99);
        if (n >= 90) return "";
        if (n <= 0) return "النهاردة";
        if (n == 1) return "بكرة";
        return "باقي " + n + " أيام";
    }

    static String meta(JSONObject t) {
        StringBuilder sb = new StringBuilder(kindOf(t));
        String l = leftOf(t);
        if (!l.isEmpty()) sb.append(" · ").append(l);
        if (hasUg(t)) sb.append(" · ").append(ugLabel(t.optInt("lv")));
        return sb.toString();
    }

    static String partsOf(JSONObject t, int max) {
        JSONArray ps = t.optJSONArray("parts");
        StringBuilder sb = new StringBuilder();
        if (ps != null) {
            for (int j = 0; j < ps.length() && j < max; j++) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(ps.optString(j));
            }
        }
        return sb.toString();
    }

    static void fillRow(RemoteViews r, JSONObject t, int maxParts) {
        int lv = t.optInt("lv");
        boolean ug = hasUg(t);
        r.setImageViewResource(R.id.ic, ugIcon(lv));
        r.setInt(R.id.ic, "setColorFilter", ugColor(lv));
        r.setViewVisibility(R.id.ic, ug ? View.VISIBLE : View.INVISIBLE);
        r.setTextViewText(R.id.rt, t.optString("t"));
        r.setTextViewText(R.id.rm, meta(t));
        r.setTextColor(R.id.rm, ug ? ugColor(lv) : 0xFF607D8B);
        String ps = partsOf(t, maxParts);
        r.setTextViewText(R.id.rp, ps);
        r.setViewVisibility(R.id.rp, ps.isEmpty() ? View.GONE : View.VISIBLE);
    }

    static PendingIntent act(Context c, int code, String action, String id, String js) {
        Intent in = new Intent(c, ActionActivity.class);
        in.setAction(action);
        in.putExtra("id", id);
        in.putExtra("js", js);
        return PendingIntent.getActivity(c, code, in, FLAGS);
    }

    static Notification.Action action(Context c, int icon, String label, PendingIntent pi) {
        return new Notification.Action.Builder(Icon.createWithResource(c, icon), label, pi).build();
    }

    static void chrono(Notification.Builder b, JSONObject ses) {
        if (ses == null) return;
        long end = ses.optLong("end", 0);
        if (end > System.currentTimeMillis()) {
            b.setShowWhen(true);
            b.setUsesChronometer(true);
            b.setChronometerCountDown(true);
            b.setWhen(end);
        }
    }

    // جلسة شغالة: راحة / تمت / إنهاء. من غير جلسة: أزرار التصنيفات
    static void addActions(Context c, Notification.Builder b, String topId, JSONObject ses, JSONArray cats) {
        if (ses != null) {
            b.addAction(action(c, android.R.drawable.ic_media_pause, "راحة",
                    act(c, 1100, "brk", "", "MZK.brk()")));
            if (topId != null && !topId.isEmpty()) {
                b.addAction(action(c, android.R.drawable.checkbox_on_background, "تمت",
                        act(c, 1000, "done" + topId, topId, "")));
            }
            b.addAction(action(c, android.R.drawable.ic_menu_close_clear_cancel, "إنهاء",
                    act(c, 1200, "endS", "", "MZK.endS()")));
        } else if (cats != null) {
            for (int i = 0; i < cats.length() && i < 3; i++) {
                JSONObject ct = cats.optJSONObject(i);
                if (ct == null) continue;
                String k = ct.optString("k");
                b.addAction(action(c, android.R.drawable.ic_media_play, ct.optString("n"),
                        act(c, 1300 + i, "go" + k, "", "MZK.go('" + k + "')")));
            }
        }
    }

    // إشعار واحد: أول مهمة حسب الأولوية. لما تفتحه بيظهر باقي المهام مترتبة بالإلحاح
    static void show(Context c, String json) {
        try {
            SharedPreferences p = c.getSharedPreferences("mzk", Context.MODE_PRIVATE);
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            ensure(c);
            boolean present = false;
            try {
                for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                    if (sbn.getId() == 1000) present = true;
                }
            } catch (Exception e) {
            }
            if (present && json.equals(p.getString("lastjson", ""))) return;
            clear(c);
            JSONArray a;
            JSONObject root = null;
            String jt = json.trim();
            if (jt.startsWith("{")) {
                root = new JSONObject(jt);
                a = root.optJSONArray("tasks");
            } else {
                a = new JSONArray(jt);
            }
            if (a == null) a = new JSONArray();
            JSONObject ses = root == null ? null : root.optJSONObject("ses");
            JSONArray cats = root == null ? null : root.optJSONArray("cats");
            PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class), FLAGS);

            if (a.length() == 0) {
                Notification.Builder e = new Notification.Builder(c, CH)
                        .setSmallIcon(R.drawable.ic_stat)
                        .setColor(GREY)
                        .setContentTitle(ses != null ? ses.optString("n") : "لا يوجد مهام اليوم")
                        .setContentText(ses != null ? "مفيش مهام في التصنيف ده" : "خلصت كل مهام النهاردة")
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setContentIntent(open);
                chrono(e, ses);
                addActions(c, e, "", ses, cats);
                nm.notify(1000, e.build());
                p.edit().putString("nids", "1000,").putString("lastjson", json).apply();
                return;
            }

            JSONObject t = a.getJSONObject(0);
            String id = t.getString("id");
            String ty = t.optString("ty");
            int icon = "exam".equals(ty) ? R.drawable.ic_stat_exam : ("hw".equals(ty) ? R.drawable.ic_stat_hw : R.drawable.ic_stat);

            RemoteViews small = new RemoteViews(c.getPackageName(), R.layout.notif_small);
            fillRow(small, t, 1);
            RemoteViews big = new RemoteViews(c.getPackageName(), R.layout.notif_big);
            big.setTextViewText(R.id.hd, ses != null ? ses.optString("n") : "مهام اليوم");
            big.removeAllViews(R.id.list);
            for (int i = 0; i < a.length() && i < 5; i++) {
                RemoteViews row = new RemoteViews(c.getPackageName(), R.layout.notif_row);
                fillRow(row, a.getJSONObject(i), i < 2 ? 3 : 0);
                big.addView(R.id.list, row);
            }

            Notification.Builder b = new Notification.Builder(c, CH)
                    .setSmallIcon(icon)
                    .setColor(GREY)
                    .setContentTitle(t.optString("t"))
                    .setContentText(meta(t))
                    .setStyle(new Notification.DecoratedCustomViewStyle())
                    .setCustomContentView(small)
                    .setCustomBigContentView(big)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(open);
            chrono(b, ses);
            addActions(c, b, id, ses, cats);
            nm.notify(1000, b.build());
            p.edit().putString("nids", "1000,").putString("lastjson", json).apply();
        } catch (Exception e) {
        }
    }
}
