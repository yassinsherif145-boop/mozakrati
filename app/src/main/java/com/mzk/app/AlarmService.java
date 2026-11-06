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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class AlarmService extends Service {
    static volatile boolean running = false;
    MediaPlayer mp;
    final Handler h = new Handler(Looper.getMainLooper());
    final Runnable loop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            if (!AlarmActivity.visible) {
                Util.openAlarm(AlarmService.this, "#alarm");
            }
            h.postDelayed(this, 2000);
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
            stopSound();
            return START_NOT_STICKY;
        }
        Notifs.ensure(this);
        Intent ai = new Intent(this, AlarmActivity.class);
        ai.putExtra("route", "#alarm");
        ai.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(this, 77, ai,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, "alarm")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("وقت المذاكرة")
                .setContentText("افتح وابدأ")
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
                .build();
        startForeground(55, n);
        running = true;
        startSound();
        h.removeCallbacks(loop);
        h.post(loop);
        return START_NOT_STICKY;
    }

    void startSound() {
        stopSound();
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setStreamVolume(AudioManager.STREAM_ALARM,
                    am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mp.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
        }
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
        stopSound();
        stopForeground(true);
        super.onDestroy();
    }
}
