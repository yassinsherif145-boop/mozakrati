package com.mzk.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class AlarmActivity extends Activity {
    static volatile boolean visible = false;
    WebView wv;
    BroadcastReceiver rc;
    boolean loaded = false;

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
        wv = new WebView(this);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
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
    }

    void load(Intent i) {
        String r = i.getStringExtra("route");
        if (r == null) r = "#alarm";
        if (!loaded) {
            loaded = true;
            wv.loadUrl("file:///android_asset/index.html" + r);
        } else {
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible = false;
    }

    @Override
    protected void onDestroy() {
        visible = false;
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
