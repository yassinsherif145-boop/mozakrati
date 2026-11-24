package com.mzk.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class AlarmService extends Service {
    static volatile boolean running = false;
    MediaPlayer mp;
    long quietUntil = 0;
    long grace = 0;
    long nextTry = 0;
    long endAt = 0;
    String route = "#alarm";
    final Handler h = new Handler(Looper.getMainLooper());

    // كل ثانيتين: لو الشاشة اتقفلت ترجّعها ويرن الصوت، ولو فترة الهدوء خلصت يرن تاني
    final Runnable loop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            if (endAt > 0 && System.currentTimeMillis() > endAt) {
                Util.closeAlarmUi(AlarmService.this);
                Util.stopAlarm(AlarmService.this);
                return;
            }
            if (!AlarmActivity.visible) {
                Util.openAlarm(AlarmService.this, route);
                if (mp == null && System.currentTimeMillis() > grace) startSound();
            } else if (quietUntil > 0 && quietUntil != Long.MAX_VALUE
                    && System.currentTimeMillis() >= quietUntil && mp == null) {
                quietUntil = 0;
                startSound();
            }
            h.postDelayed(this, 2000);
        }
    };

    // كل 800 ملي: يرفع الصوت لأقصى درجة لو حد نزّله
    final Runnable vol = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            if (mp != null) forceVolume();
            h.postDelayed(this, 800);
        }
    };

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        String a = (i == null) ? null : i.getAction();
        if ("SILENCE".equals(a)) {
            if (!running) {
                stopSelf();
                return START_NOT_STICKY;
            }
            int sec = i.getIntExtra("sec", 0);
            quietUntil = sec > 0 ? System.currentTimeMillis() + sec * 1000L : Long.MAX_VALUE;
            stopSound();
            return START_NOT_STICKY;
        }
        Notifs.ensure(this);
        String mode = (i == null) ? null : i.getStringExtra("mode");
        boolean weekly = "weekly".equals(mode);
        boolean reason = "reason".equals(mode) || weekly;
        route = weekly ? "#wk" : (reason ? "#pend" : "#alarm");
        Intent ai = new Intent(this, AlarmActivity.class);
        ai.putExtra("route", route);
        ai.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(this, 77, ai,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, Notifs.ALARM)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(weekly ? "السجل الأسبوعي" : (reason ? "اكتب الأسباب" : "وقت المذاكرة"))
                .setContentText(weekly ? "افتح واكتب السجل الأسبوعي" : (reason ? "افتح واكتب الأسباب" : "افتح وابدأ"))
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
                .build();
        startForeground(55, n);
        running = true;
        quietUntil = 0;
        grace = 0;
        nextTry = 0;
        endAt = reason ? System.currentTimeMillis() + 10 * 60 * 1000L : 0;
        if (reason) {
            // هادي في الأول عشان لو مفيش حاجة تتكتب، وبيرن لو سبت الشاشة
            grace = System.currentTimeMillis() + 8000;
            quietUntil = System.currentTimeMillis() + 25000;
        } else {
            startSound();
        }
        h.removeCallbacks(loop);
        h.removeCallbacks(vol);
        h.post(loop);
        h.post(vol);
        return START_NOT_STICKY;
    }

    void forceVolume() {
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            try {
                am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0);
            } catch (Exception e) {
            }
            int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) < max) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0);
            }
        } catch (Exception e) {
        }
    }

    boolean play(Uri uri) {
        if (uri == null) return false;
        try {
            mp = new MediaPlayer();
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer m, int what, int extra) {
                    try {
                        m.release();
                    } catch (Exception e) {
                    }
                    if (mp == m) mp = null;
                    return true;
                }
            });
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mp.setDataSource(this, uri);
            mp.setLooping(true);
            mp.setVolume(1f, 1f);
            mp.prepare();
            mp.start();
            return true;
        } catch (Exception e) {
            try {
                if (mp != null) mp.release();
            } catch (Exception x) {
            }
            mp = null;
            return false;
        }
    }

    void startSound() {
        if (System.currentTimeMillis() < nextTry) return;
        stopSound();
        forceVolume();
        String saved = getSharedPreferences("mzk", Context.MODE_PRIVATE).getString("tone", "");
        Uri[] cands = new Uri[]{
                saved.isEmpty() ? null : Uri.parse(saved),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        };
        for (Uri u : cands) {
            if (play(u)) return;
        }
        nextTry = System.currentTimeMillis() + 20000;
    }

    void stopSound() {
        if (mp != null) {
            try {
                mp.stop();
            } catch (Exception e) {
            }
            mp.release();
            mp = null;
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        h.removeCallbacks(loop);
        h.removeCallbacks(vol);
        stopSound();
        stopForeground(true);
        super.onDestroy();
    }
}
