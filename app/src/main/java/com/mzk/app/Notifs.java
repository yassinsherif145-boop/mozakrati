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
    static final int RED = 0xFFD32F2F;
    static final int BLUE = 0xFF1976D2;

    static void ensure(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(CH, "مهام اليوم", NotificationManager.IMPORTANCE_LOW));
            nm.createNotificationChannel(new NotificationChannel("alarm", "المنبه", NotificationManager.IMPORTANCE_HIGH));
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

    static void fill(RemoteViews rv, int col, String tag, String title, String body, int count) {
        rv.setInt(R.id.bar, "setBackgroundColor", col);
        rv.setTextViewText(R.id.tag, tag);
        rv.setInt(R.id.tag, "setBackgroundColor", col);
        rv.setTextColor(R.id.tag, 0xFFFFFFFF);
        rv.setTextViewText(R.id.title, title);
        rv.setTextViewText(R.id.parts, body);
        rv.setViewVisibility(R.id.parts, body.isEmpty() ? View.GONE : View.VISIBLE);
        rv.setTextViewText(R.id.cnt, String.valueOf(count));
        rv.setTextColor(R.id.cnt, col);
    }

    // إشعار واحد بس: أقرب مهمة. لما تخلصها بيظهر اللي بعدها، ولو خلصت الكل بيكتب «لا يوجد مهام اليوم»
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
            String jt = json.trim();
            if (jt.startsWith("{")) {
                a = new JSONObject(jt).optJSONArray("tasks");
            } else {
                a = new JSONArray(jt);
            }
            if (a == null) a = new JSONArray();
            PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class), FLAGS);

            if (a.length() == 0) {
                Notification.Builder e = new Notification.Builder(c, CH)
                        .setSmallIcon(R.drawable.ic_stat)
                        .setColor(0xFF2E7D32)
                        .setContentTitle("✅ لا يوجد مهام اليوم")
                        .setContentText("خلصت كل مهام النهاردة")
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setContentIntent(open);
                nm.notify(1000, e.build());
                p.edit().putString("nids", "1000,").putString("lastjson", json).apply();
                return;
            }

            JSONObject t = a.getJSONObject(0);
            String id = t.getString("id");
            String title = t.getString("t");
            boolean ex = "exam".equals(t.optString("ty"));
            int col = ex ? RED : BLUE;
            int n = t.optInt("n");
            int acc = t.optInt("acc");
            String kw = t.optString("kw", "");
            String left = n <= 0 ? "النهاردة" : (n == 1 ? "بكرة" : "باقي " + n + " أيام");
            String kind = kw.isEmpty() ? (ex ? "امتحان" : "واجب") : kw;
            String tag = kind + " · " + left + (acc > 0 ? " · متراكم " + acc : "");

            StringBuilder allp = new StringBuilder();
            String first = "";
            JSONArray parts = t.optJSONArray("parts");
            if (parts != null) {
                for (int j = 0; j < parts.length(); j++) {
                    if (allp.length() > 0) allp.append("\n");
                    allp.append("• ").append(parts.getString(j));
                    if (j == 0) first = "• " + parts.getString(j);
                }
            }
            if (allp.length() == 0) {
                allp.append("مهمة النهاردة");
                first = "مهمة النهاردة";
            }

            RemoteViews small = new RemoteViews(c.getPackageName(), R.layout.notif_task);
            fill(small, col, tag, title, first, a.length());
            RemoteViews big = new RemoteViews(c.getPackageName(), R.layout.notif_task);
            fill(big, col, tag, title, allp.toString(), a.length());

            Intent done = new Intent(c, ActionActivity.class);
            done.setAction("done" + id);
            done.putExtra("id", id);
            Intent later = new Intent(c, AlarmActivity.class);
            later.setAction("later" + id);
            later.putExtra("route", "#reason");
            PendingIntent pd = PendingIntent.getActivity(c, 1000, done, FLAGS);
            PendingIntent pl = PendingIntent.getActivity(c, 1500, later, FLAGS);

            Notification.Action aDone = new Notification.Action.Builder(
                    Icon.createWithResource(c, android.R.drawable.checkbox_on_background), "تمت", pd).build();
            Notification.Action aLater = new Notification.Action.Builder(
                    Icon.createWithResource(c, android.R.drawable.ic_menu_recent_history), "راكمت", pl).build();

            Notification.Builder b = new Notification.Builder(c, CH)
                    .setSmallIcon(ex ? R.drawable.ic_stat_exam : R.drawable.ic_stat_hw)
                    .setColor(col)
                    .setContentTitle(title)
                    .setContentText(first)
                    .setStyle(new Notification.DecoratedCustomViewStyle())
                    .setCustomContentView(small)
                    .setCustomBigContentView(big)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(open)
                    .addAction(aDone)
                    .addAction(aLater);
            nm.notify(1000, b.build());
            p.edit().putString("nids", "1000,").putString("lastjson", json).apply();
        } catch (Exception e) {
        }
    }
}
