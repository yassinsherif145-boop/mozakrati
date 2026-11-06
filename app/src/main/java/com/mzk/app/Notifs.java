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

import org.json.JSONArray;
import org.json.JSONObject;

public class Notifs {
    static final String CH = "tasks";
    static final int FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

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
                StringBuilder text = new StringBuilder();
                if ("exam".equals(t.optString("ty"))) {
                    text.append("امتحان — باقي ").append(t.optInt("n")).append(" أيام");
                }
                JSONArray parts = t.optJSONArray("parts");
                if (parts != null) {
                    for (int j = 0; j < parts.length(); j++) {
                        if (text.length() > 0) text.append("\n");
                        text.append("• ").append(parts.getString(j));
                    }
                }
                if (text.length() == 0) text.append("مهمة النهاردة");

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
                        Icon.createWithResource(c, android.R.drawable.ic_menu_recent_history), "تراكم", pl).build();

                Notification.Builder b = new Notification.Builder(c, CH)
                        .setSmallIcon(android.R.drawable.ic_menu_agenda)
                        .setContentTitle(title)
                        .setContentText(text.toString().split("\n")[0])
                        .setStyle(new Notification.BigTextStyle().bigText(text.toString()))
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
                    .setSmallIcon(android.R.drawable.ic_menu_agenda)
                    .setContentTitle("مهام النهاردة (" + a.length() + ")")
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
