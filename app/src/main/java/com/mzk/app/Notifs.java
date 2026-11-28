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
    static final String CH = "tasks3";
    static final String ALARM = "alarm2";
    static final int FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    static final int RED = 0xFFD32F2F;
    static final int BLUE = 0xFF1976D2;

    static void ensure(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel tc = new NotificationChannel(CH, "مهام اليوم", NotificationManager.IMPORTANCE_MIN);
            NotificationChannel ac = new NotificationChannel(ALARM, "المنبه", NotificationManager.IMPORTANCE_HIGH);
            try {
                // يتخطى وضع عدم الإزعاج (بيشتغل بس لو المستخدم أدّى صلاحية "عدم الإزعاج" للتطبيق)
                tc.setBypassDnd(true);
                ac.setBypassDnd(true);
            } catch (Exception e) {
            }
            nm.createNotificationChannel(tc);
            try {
                nm.deleteNotificationChannel("tasks");
                nm.deleteNotificationChannel("tasks2");
                nm.deleteNotificationChannel("alarm");
            } catch (Exception e) {
            }
            nm.createNotificationChannel(ac);
            nm.createNotificationChannel(new NotificationChannel("idle", "إشعار الخلفية (صامت)", NotificationManager.IMPORTANCE_MIN));
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
        cancelKids(nm);
        p.edit().putString("nids", "").putString("lastjson", "").apply();
        if (AppWatch.running) idle(c, "مفيش مهام دلوقتي");
    }

    static void cancelKids(NotificationManager nm) {
        for (int i = 0; i < 10; i++) nm.cancel(1010 + i);
    }

    // إشعار بسيط. لما مراقبة التطبيقات شغالة بنستخدمه بدل إشعار «المراقبة» المنفصل (الأندرويد بيفرض إشعار مع أي خدمة شغالة في الخلفية)
    static Notification idleNote(Context c, String text) {
        PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class), FLAGS);
        return new Notification.Builder(c, "idle")
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("مذاكرتي")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(open)
                .build();
    }

    static void idle(Context c, String text) {
        try {
            if (c.getSharedPreferences("mzk", Context.MODE_PRIVATE).getBoolean("tmrOn", false)) return;
            ensure(c);
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(1000, idleNote(c, text));
        } catch (Exception e) {
        }
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
    static String ugLabel(int lv) {
        if (lv >= 2) return "لا يوجد وقت آخر";
        if (lv == 1) return "من الأفضل أن تبدأ دلوقتي";
        return "على المسار";
    }

    static int ugColor(int lv) {
        if (lv >= 2) return 0xFFB23A3A;
        if (lv == 1) return 0xFFA8741A;
        return 0xFF5F8070;
    }

    static int ugIcon(int lv) {
        if (lv >= 2) return R.drawable.ic_u2;
        if (lv == 1) return R.drawable.ic_u1;
        return R.drawable.ic_u0;
    }

    static void fillRow(RemoteViews r, JSONObject t) {
        int lv = t.optInt("lv");
        r.setTextViewText(R.id.rt, t.optString("t"));
        r.setTextViewText(R.id.rm, ugLabel(lv));
        r.setTextColor(R.id.rm, ugColor(lv));
        r.setImageViewResource(R.id.ic, ugIcon(lv));
        r.setInt(R.id.ic, "setColorFilter", ugColor(lv));
        if (t.has("dl")) {
            int d = t.optInt("dl");
            String x = d <= 0 ? "آخر يوم" : d <= 2 ? "متبقي " + d + " يوم" : "متبقي " + d + " أيام";
            r.setTextViewText(R.id.r2, x);
            r.setViewVisibility(R.id.r2, View.VISIBLE);
        }
    }

    static String plain(JSONObject t) {
        String x = ugLabel(t.optInt("lv"));
        if (t.has("dl")) {
            int d = t.optInt("dl");
            x += " · " + (d <= 0 ? "آخر يوم" : d <= 2 ? "متبقي " + d + " يوم" : "متبقي " + d + " أيام");
        }
        return x;
    }

    static void show(Context c, String json) {
        try {
            SharedPreferences p = c.getSharedPreferences("mzk", Context.MODE_PRIVATE);
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            ensure(c);
            try {
                if (new JSONObject(json).optBoolean("hush", false)) {
                    NotificationManager nmh = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
                    nmh.cancel(1000);
                    nmh.cancel(900);
                    cancelKids(nmh);
                    p.edit().putString("nids", "").putString("lastjson", "").putString("lastTasks", "").apply();
                    if (AppWatch.running) idle(c, "مفيش مهام دلوقتي");
                    return;
                }
                if (new JSONObject(json).optBoolean("rest", false)) {
                    p.edit().putString("lastTasks", "").apply();
                    clear(c);
                    return;
                }
            } catch (Exception e) {
            }
            p.edit().putString("lastTasks", json).apply();
            boolean present = false;
            boolean tmr = false;
            try {
                for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                    if (sbn.getId() == 1000) present = true;
                    if (sbn.getId() == 1001) tmr = true;
                }
            } catch (Exception e) {
            }
            if (p.getBoolean("tmrOn", false)) tmr = true;
            if (tmr) {
                holdForTimer(c);
                return;
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
            if (a == null || a.length() == 0) return;
            RemoteViews small = new RemoteViews(c.getPackageName(), R.layout.notif_row);
            fillRow(small, a.getJSONObject(0));
            small.setTextViewText(R.id.nc, String.valueOf(a.length()));
            small.setViewVisibility(R.id.nc, View.VISIBLE);
            RemoteViews big = new RemoteViews(c.getPackageName(), R.layout.notif_big);
            big.removeAllViews(R.id.list);
            for (int i = 0; i < a.length() && i < 6; i++) {
                RemoteViews row = new RemoteViews(c.getPackageName(), R.layout.notif_row);
                fillRow(row, a.getJSONObject(i));
                big.addView(R.id.list, row);
            }
            String mode = "g";
            try {
                if (jt.startsWith("{")) mode = new JSONObject(jt).optString("nm", "g");
            } catch (Exception e) {
            }
            PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class), FLAGS);
            int n = a.length();
            if (false && n >= 2) {
                // مجمّع: مهمة قدّام والباقي بالسحب لتحت (الأندرويد بيقفله لوحده)
                long base = System.currentTimeMillis();
                StringBuilder ids = new StringBuilder("1000,");
                int kids = Math.min(n, 8);
                for (int i = kids - 1; i >= 0; i--) {
                    RemoteViews row = new RemoteViews(c.getPackageName(), R.layout.notif_row);
                    fillRow(row, a.getJSONObject(i));
                    Notification.Builder kb = new Notification.Builder(c, CH)
                            .setSmallIcon(R.drawable.ic_stat)
                            .setStyle(new Notification.DecoratedCustomViewStyle())
                            .setContentTitle(a.getJSONObject(i).optString("t"))
                            .setContentText(plain(a.getJSONObject(i)))
                            .setCustomContentView(row)
                            .setGroup("mzk_tasks")
                            .setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY)
                            .setSortKey(String.format(java.util.Locale.US, "%02d", i))
                            .setShowWhen(false)
                            .setWhen(base - i * 1000L)
                            .setOngoing(true)
                            .setOnlyAlertOnce(true)
                            .setVisibility(Notification.VISIBILITY_PUBLIC)
                            .setContentIntent(open);
                    nm.notify(1010 + i, kb.build());
                    ids.append(1010 + i).append(",");
                }
                Notification.Builder sb = new Notification.Builder(c, CH)
                        .setSmallIcon(R.drawable.ic_stat)
                        .setStyle(new Notification.DecoratedCustomViewStyle())
                        .setContentTitle(a.getJSONObject(0).optString("t"))
                        .setContentText(plain(a.getJSONObject(0)) + " · +" + (n - 1))
                        .setCustomContentView(small)
                        .setGroup("mzk_tasks")
                        .setGroupSummary(true)
                        .setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY)
                        .setNumber(n)
                        .setShowWhen(false)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setContentIntent(open);
                nm.notify(1000, sb.build());
                p.edit().putString("nids", ids.toString()).putString("lastjson", json).apply();
                return;
            }
            Notification.Builder b = new Notification.Builder(c, CH)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setStyle(new Notification.DecoratedCustomViewStyle())
                    .setCustomContentView(small)
                    .setNumber(n)
                    .setShowWhen(false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(open);
            b.setContentTitle(a.getJSONObject(0).optString("t")).setContentText(plain(a.getJSONObject(0)));
            if (n >= 2) b.setCustomBigContentView(big);
            nm.notify(1000, b.build());
            p.edit().putString("nids", "1000,").putString("lastjson", json).apply();
        } catch (Exception e) {
        }
    }

    static void timer(Context c, String json) {
        try {
            OverlayUi.bubble(c, json);
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (json == null || json.isEmpty()) {
                nm.cancel(1001);
                // رجّع إشعار المهام اللي كان مستخبي وقت العداد
                SharedPreferences sp = c.getSharedPreferences("mzk", Context.MODE_PRIVATE);
                sp.edit().putBoolean("tmrOn", false).apply();
                String lt = sp.getString("lastTasks", "");
                if (!lt.isEmpty()) show(c, lt);
                else if (AppWatch.running) idle(c, "مفيش مهام دلوقتي");
                else nm.cancel(1000);
                return;
            }
            ensure(c);
            JSONObject o = new JSONObject(json);
            String k = o.optString("k");
            String l = o.optString("l");
            long s = o.optLong("s", System.currentTimeMillis());
            long e = o.optLong("e", 0);
            long fz = o.optLong("fz", -1L);
            String mt = o.optString("m");
            c.getSharedPreferences("mzk", Context.MODE_PRIVATE).edit().putBoolean("tmrOn", true).apply();
            Notification.Builder b = new Notification.Builder(c, CH)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(l.isEmpty() ? "عداد" : l)
                    .setContentText(fz >= 0 ? "متوقفة مؤقتًا · " + OverlayUi.clk(fz) : mt)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setUsesChronometer(fz < 0)
                    .setWhen(e > System.currentTimeMillis() ? e : s)
                    .setChronometerCountDown(fz < 0 && e > System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class), FLAGS));
            if ("break".equals(k)) {
                Intent in = new Intent(c, ActionActivity.class);
                in.setAction("resumeT");
                in.putExtra("js", "MZK.resumeT()");
                b.addAction(new Notification.Action.Builder(Icon.createWithResource(c, android.R.drawable.ic_media_play),
                        "استكمال", PendingIntent.getActivity(c, 1101, in, FLAGS)).build());
            } else if ("task".equals(k)) {
                Intent in = new Intent(c, TimerReceiver.class);
                in.setAction("PAUSE");
                b.addAction(new Notification.Action.Builder(Icon.createWithResource(c, android.R.drawable.ic_media_pause),
                        "إيقاف مؤقت", PendingIntent.getBroadcast(c, 1102, in, FLAGS)).build());
            }
            if (AppWatch.running) {
                nm.cancel(1001);
                nm.notify(1000, b.build());
            } else {
                nm.notify(1001, b.build());
            }
            holdForTimer(c);
        } catch (Exception e) {
        }
    }

    // لما العداد شغال: شريط المهام بيختفي (وبيرجع لما العداد يخلص)
    static void holdForTimer(Context c) {
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            c.getSharedPreferences("mzk", Context.MODE_PRIVATE).edit().putString("lastjson", "").putString("nids", "").apply();
            nm.cancel(900);
            cancelKids(nm);
            if (!AppWatch.running) nm.cancel(1000);
        } catch (Exception e) {
        }
    }

    static void showOld(Context c, String json) {
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
