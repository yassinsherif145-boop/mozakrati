package com.mzk.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

// شاشة السؤال: بتعمل إيه + هتوقف الساعة كام
public class CheckActivity extends Activity {
    static volatile boolean visible = false;
    final Handler h = new Handler(Looper.getMainLooper());
    String pkg;
    String name;
    int left;
    EditText ed;
    NumberPicker nh;
    NumberPicker nm;
    NumberPicker np;
    TextView info;
    Button ok;

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    long target() {
        int hh = (nh.getValue() % 12) + (np.getValue() == 1 ? 12 : 0);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hh);
        c.set(Calendar.MINUTE, nm.getValue());
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long t = c.getTimeInMillis();
        if (t <= System.currentTimeMillis() + 30000L) t += 24L * 3600000L;
        return t;
    }

    void refresh() {
        long t = target();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        int hh = c.get(Calendar.HOUR_OF_DAY);
        int mi = c.get(Calendar.MINUTE);
        String clock = ((hh % 12 == 0) ? 12 : hh % 12) + ":" + (mi < 10 ? "0" : "") + mi + (hh < 12 ? " ص" : " م");
        long mins = (t - System.currentTimeMillis()) / 60000L;
        String d = mins >= 60 ? (mins / 60) + " س و " + (mins % 60) + " د" : mins + " دقيقة";
        info.setText("هتوقف الساعة " + clock + " (بعد " + d + ")");
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        pkg = getIntent().getStringExtra("p");
        name = getIntent().getStringExtra("n");
        if (name == null) name = pkg;
        left = getIntent().getIntExtra("w", 0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dp(20), dp(32), dp(20), dp(20));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView t = new TextView(this);
        t.setText("بتعمل إيه في " + name + "؟");
        t.setTextSize(22);
        t.setTextColor(Color.parseColor("#111111"));
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        root.addView(t);

        ed = new EditText(this);
        ed.setHint("اكتب هتعمل إيه");
        ed.setTextColor(Color.parseColor("#111111"));
        ed.setHintTextColor(Color.parseColor("#888888"));
        ed.setTextSize(18);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.topMargin = dp(14);
        root.addView(ed, lp1);

        TextView t2 = new TextView(this);
        t2.setText("هتوقف الساعة كام؟");
        t2.setTextSize(18);
        t2.setTextColor(Color.parseColor("#111111"));
        t2.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = dp(20);
        root.addView(t2, lp2);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setGravity(Gravity.CENTER);

        nh = new NumberPicker(this);
        nh.setMinValue(1);
        nh.setMaxValue(12);
        nm = new NumberPicker(this);
        nm.setMinValue(0);
        nm.setMaxValue(59);
        nm.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int v) {
                return v < 10 ? "0" + v : "" + v;
            }
        });
        np = new NumberPicker(this);
        np.setMinValue(0);
        np.setMaxValue(1);
        np.setDisplayedValues(new String[]{"ص", "م"});

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, 30);
        int hh = c.get(Calendar.HOUR_OF_DAY);
        nh.setValue(hh % 12 == 0 ? 12 : hh % 12);
        nm.setValue(c.get(Calendar.MINUTE));
        np.setValue(hh >= 12 ? 1 : 0);

        NumberPicker.OnValueChangeListener ls = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker p, int o, int n) {
                refresh();
            }
        };
        nh.setOnValueChangedListener(ls);
        nm.setOnValueChangedListener(ls);
        np.setOnValueChangedListener(ls);
        row.addView(nh);
        row.addView(nm);
        row.addView(np);
        root.addView(row);

        info = new TextView(this);
        info.setTextSize(16);
        info.setTextColor(Color.parseColor("#444444"));
        info.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        root.addView(info);

        ok = new Button(this);
        ok.setText("تأكيد");
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp3.topMargin = dp(20);
        root.addView(ok, lp3);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(root);
        setContentView(sv);
        refresh();

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String w = ed.getText().toString().trim();
                if (w.length() < 2) {
                    Toast.makeText(CheckActivity.this, "اكتب هتعمل إيه", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent s = new Intent(CheckActivity.this, AppWatch.class);
                s.setAction("COMMIT");
                s.putExtra("p", pkg);
                s.putExtra("n", name);
                s.putExtra("w", w);
                s.putExtra("u", target());
                try {
                    startService(s);
                } catch (Exception e) {
                }
                finish();
            }
        });

        if (left > 0) {
            ok.setEnabled(false);
            ok.setText("استنى " + left + " ثانية");
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    left--;
                    if (left <= 0) {
                        ok.setEnabled(true);
                        ok.setText("تأكيد");
                    } else {
                        ok.setText("استنى " + left + " ثانية");
                        h.postDelayed(this, 1000);
                    }
                }
            }, 1000);
        }
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
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // الرجوع معطّل عمداً
    }
}
