package com.mzk.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class AlarmActivity extends Activity {
    static volatile boolean visible = false;
    static volatile boolean ready = false;
    final Handler hh = new Handler(Looper.getMainLooper());
    WebView wv;
    BroadcastReceiver rc;
    boolean loaded = false;
    String curRoute = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        ready = false;
        wv = new WebView(this);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        wv.setWebChromeClient(new WebChromeClient());
        wv.addJavascriptInterface(new Bridge(this), "Android");
        setContentView(wv);
        load(getIntent());
        rc = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                finish();
            }
        };
        registerReceiver(rc, new IntentFilter("com.mzk.app.CLOSE"));
        // لو الصفحة ما فتحتش في 7 ثواني نجرّب النسخة المدمجة، ولو لسه ما فتحتش نطلّع زرار إيقاف
        hh.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ready || wv == null) return;
                try {
                    String r = getIntent().getStringExtra("route");
                    if (r == null) r = "#alarm";
                    wv.loadUrl(Util.ASSET + r);
                } catch (Exception e) {
                }
                hh.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!ready) fallback();
                    }
                }, 8000);
            }
        }, 7000);
    }

    void fallback() {
        try {
            LinearLayout l = new LinearLayout(this);
            l.setOrientation(LinearLayout.VERTICAL);
            l.setGravity(Gravity.CENTER);
            l.setPadding(48, 48, 48, 48);
            l.setBackgroundColor(0xFF1E1B4B);
            TextView t = new TextView(this);
            t.setText("الصفحة مش بتفتح");
            t.setTextColor(0xFFFFFFFF);
            t.setTextSize(22);
            t.setGravity(Gravity.CENTER);
            Button b = new Button(this);
            b.setText("إيقاف المنبه");
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Util.stopAlarm(AlarmActivity.this);
                    finish();
                }
            });
            l.addView(t);
            l.addView(b);
            setContentView(l);
        } catch (Exception e) {
        }
    }

    void load(Intent i) {
        String r = i.getStringExtra("route");
        if (r == null) r = "#alarm";
        if (!loaded) {
            loaded = true;
            curRoute = r;
            wv.loadUrl(Util.pageUrl(this) + r);
        } else if (!r.equals(curRoute)) {
            curRoute = r;
            wv.evaluateJavascript("try{MZK.show('" + r + "')}catch(e){}", null);
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        load(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;
        OverlayUi.hideReopen(this);
        try {
            if (OverlayUi.bubbleView != null) OverlayUi.bubbleView.setVisibility(android.view.View.GONE);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible = false;
        try {
            if (OverlayUi.bubbleView != null) OverlayUi.bubbleView.setVisibility(android.view.View.VISIBLE);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onDestroy() {
        visible = false;
        hh.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(rc);
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // الرجوع معطّل عمداً
    }
}