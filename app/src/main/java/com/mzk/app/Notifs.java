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
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class Notifs {
    static final String CH = "tasks";
    static final int FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    static final int RED = 0xFFEF5350;
    static final int BLUE = 0xFF42A5F5;

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

    static void fill(RemoteViews rv, int col, String tag, String title, String body) {
        rv.setInt(R.id.bar, "setBackgroundColor", col);
        rv.setTextViewText(R.id.tag, tag);
        rv.setTextColor(R.id.tag, col);
        rv.setTextViewText(R.id.title, title);
        rv.setTextViewText(R.id.parts, body);
        rv.setViewVisibility(R.id.parts, body.isEmpty() ? View.GONE : View.VISIBLE);
    }

    static void show(Context c, String json) {
        try {
            SharedPreferences p = c.getSharedPreferences("mzk", Context.MODE_PRIVATE);
            if (json.equals(p.getString("lastjson", ""))) return;
            ensure(c);
            clear(c);
            JSONArray a = new JSONArray(json);
            if (a.length() == 0) {
                p.edit().putString("lastjson", json).apply();
                return;
            }
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class), FLAGS);
            StringBuilder ids = new StringBuilder();
            for (int i = 0; i < a.length(); i++) {
                JSONObject t = a.getJSONObject(i);
                int nid = 1000 + i;
                String id = t.getString("id");
                String title = t.getString("t");
                boolean ex = "exam".equals(t.optString("ty"));
                int col = ex ? RED : BLUE;
                int n = t.optInt("n");
                String left = n <= 0 ? "النهاردة" : (n == 1 ? "بكرة" : "باقي " + n + " أيام");
                String tag = ex ? "📝 امتحان · " + left : "📘 واجب";

                StringBuilder all = new StringBuilder();
                String first = "";
                JSONArray parts = t.optJSONArray("parts");
                if (parts != null) {
                    for (int j = 0; j < parts.length(); j++) {
                        if (all.length() > 0) all.append("\n");
                        all.append("• ").append(parts.getString(j));
                        if (j == 0) first = "• " + parts.getString(j);
                    }
                }
                if (all.length() == 0) {
                    all.append("مهمة النهاردة");
                    first = "مهمة النهاردة";
                }

                RemoteViews small = new RemoteViews(c.getPackageName(), R.layout.notif_task);
                fill(small, col, tag, title, first);
                RemoteViews big = new RemoteViews(c.getPackageName(), R.layout.notif_task);
                fill(big, col, tag, title, all.toString());

                Intent done = new Intent(c, ActionActivity.class);
                done.setAction("done" + id);
                done.putExtra("id", id);
                Intent later = new Intent(c, AlarmActivity.class);
                later.setAction("later" + id);
                later.putExtra("route", "#reason");
                PendingIntent pd = PendingIntent.getActivity(c, nid, done, FLAGS);
                PendingIntent pl = PendingIntent.getActivity(c, nid + 500, later, FLAGS);

                Notification.Action aDone = new Notification.Action.Builder(
                        Icon.createWithResource(c, android.R.drawable.checkbox_on_background), "تمت", pd).build();
                Notification.Action aLater = new Notification.Action.Builder(
                        Icon.createWithResource(c, android.R.drawable.ic_menu_recent_history), "راكمت", pl).build();

                Notification.Builder b = new Notification.Builder(c, CH)
                        .setSmallIcon(R.drawable.ic_stat)
                        .setColor(col)
                        .setSubText("My Studying Session")
                        .setContentTitle(title)
                        .setContentText(first)
                        .setStyle(new Notification.DecoratedCustomViewStyle())
                        .setCustomContentView(small)
                        .setCustomBigContentView(big)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setGroup("mzk")
                        .setContentIntent(open)
                        .addAction(aDone)
                        .addAction(aLater);
                nm.notify(nid, b.build());
                ids.append(nid).append(",");
            }
            Notification.Builder s = new Notification.Builder(c, CH)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setColor(BLUE)
                    .setContentTitle("مهام النهاردة (" + a.length() + ")")
                    .setSubText("My Studying Session")
                    .setGroup("mzk")
                    .setGroupSummary(true)
                    .setOngoing(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(open);
            nm.notify(900, s.build());
            p.edit().putString("nids", ids.toString()).putString("lastjson", json).apply();
        } catch (Exception e) {
        }
    }
}
