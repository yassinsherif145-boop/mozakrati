package com.mzk.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Calendar;

// نوافذ بتظهر فوق أي تطبيق (محتاجة إذن الظهور فوق التطبيقات)
public class OverlayUi {
    static volatile boolean checkShown = false;
    static volatile boolean pauseShown = false;
    static View checkView = null;
    static View pauseView = null;
    static final Handler H = new Handler(Looper.getMainLooper());

    static boolean can(Context c) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c);
    }

    static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    static WindowManager.LayoutParams params() {
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        return lp;
    }

    static TextView label(Context c, String t, int sp, boolean bold) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(Color.parseColor("#111111"));
        v.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }

    static Button btn(Context c, String t) {
        Button b = new Button(c);
        b.setText(t);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.parseColor("#37474F"));
        b.setAllCaps(false);
        return b;
    }

    static EditText edit(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextColor(Color.parseColor("#111111"));
        e.setHintTextColor(Color.parseColor("#888888"));
        e.setTextSize(18);
        e.setBackgroundColor(Color.parseColor("#EEEEEE"));
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        e.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return e;
    }

    static LinearLayout column(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.WHITE);
        col.setPadding(dp(c, 20), dp(c, 36), dp(c, 20), dp(c, 20));
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return col;
    }

    static LinearLayout.LayoutParams lp(Context c, int top) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        l.topMargin = dp(c, top);
        return l;
    }

    static ScrollView wrap(Context c, LinearLayout col) {
        ScrollView sv = new ScrollView(c);
        sv.setBackgroundColor(Color.WHITE);
        sv.setFillViewport(true);
        sv.addView(col);
        return sv;
    }

    static String clock(int mins) {
        int hh = mins / 60;
        int mi = mins % 60;
        return ((hh % 12 == 0) ? 12 : hh % 12) + ":" + (mi < 10 ? "0" : "") + mi + (hh < 12 ? " ص" : " م");
    }

    static long targetOf(int mins) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, mins / 60);
        c.set(Calendar.MINUTE, mins % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long t = c.getTimeInMillis();
        if (t <= System.currentTimeMillis() + 30000L) t += 24L * 3600000L;
        return t;
    }

    static String afterTxt(long t) {
        long mins = (t - System.currentTimeMillis()) / 60000L;
        return mins >= 60 ? (mins / 60) + " س و " + (mins % 60) + " د" : mins + " دقيقة";
    }

    static void render(TextView big, TextView info, int mins) {
        big.setText(clock(mins));
        info.setText("بعد " + afterTxt(targetOf(mins)));
    }

    static Button adj(final Context ctx, String t, final int delta, final int[] m, final TextView big, final TextView info) {
        Button b = btn(ctx, t);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m[0] = ((m[0] + delta) % 1440 + 1440) % 1440;
                render(big, info, m[0]);
            }
        });
        return b;
    }

    static LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        l.leftMargin = 4;
        l.rightMargin = 4;
        return l;
    }

    // سؤال: بتعمل إيه في التطبيق ده + هتوقف الساعة كام
    static void showCheck(final Context ctx, final String pkg, final String name, final int wait) {
        try {
            if (checkShown || !can(ctx)) return;
            final WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            LinearLayout col = column(ctx);
            col.addView(label(ctx, "بتعمل إيه في " + name + "؟", 22, true));
            final EditText ed = edit(ctx, "اكتب هتعمل إيه");
            col.addView(ed, lp(ctx, 14));
            col.addView(label(ctx, "هتوقف الساعة كام؟", 18, false), lp(ctx, 22));

            final int[] m = new int[1];
            Calendar c0 = Calendar.getInstance();
            c0.add(Calendar.MINUTE, 30);
            m[0] = ((c0.get(Calendar.HOUR_OF_DAY) * 60 + c0.get(Calendar.MINUTE) + 4) / 5 * 5) % 1440;

            final TextView big = label(ctx, "", 46, true);
            big.setGravity(Gravity.CENTER);
            big.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            col.addView(big, lp(ctx, 8));
            final TextView info = label(ctx, "", 15, false);
            info.setGravity(Gravity.CENTER);
            info.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            info.setTextColor(Color.parseColor("#555555"));
            col.addView(info);
            render(big, info, m[0]);

            LinearLayout r1 = new LinearLayout(ctx);
            r1.setOrientation(LinearLayout.HORIZONTAL);
            r1.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            r1.addView(adj(ctx, "-1س", -60, m, big, info), weight());
            r1.addView(adj(ctx, "+1س", 60, m, big, info), weight());
            r1.addView(adj(ctx, "-5د", -5, m, big, info), weight());
            r1.addView(adj(ctx, "+5د", 5, m, big, info), weight());
            col.addView(r1, lp(ctx, 14));

            LinearLayout r2 = new LinearLayout(ctx);
            r2.setOrientation(LinearLayout.HORIZONTAL);
            r2.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            Button am = btn(ctx, "ص");
            am.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (m[0] >= 720) m[0] -= 720;
                    render(big, info, m[0]);
                }
            });
            Button pm = btn(ctx, "م");
            pm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (m[0] < 720) m[0] += 720;
                    render(big, info, m[0]);
                }
            });
            r2.addView(am, weight());
            r2.addView(pm, weight());
            col.addView(r2, lp(ctx, 8));

            final Button ok = btn(ctx, "تأكيد");
            col.addView(ok, lp(ctx, 22));
            if (wait > 0) {
                ok.setEnabled(false);
                ok.setAlpha(0.5f);
                ok.setText("استنى " + wait + " ثانية");
                final int[] left = new int[]{wait};
                H.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!checkShown) return;
                        left[0]--;
                        if (left[0] <= 0) {
                            ok.setEnabled(true);
                            ok.setAlpha(1f);
                            ok.setText("تأكيد");
                        } else {
                            ok.setText("استنى " + left[0] + " ثانية");
                            H.postDelayed(this, 1000);
                        }
                    }
                }, 1000);
            }
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String w = ed.getText().toString().trim();
                    if (w.length() < 2) {
                        Toast.makeText(ctx, "اكتب هتعمل إيه", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        Intent s = new Intent(ctx, AppWatch.class);
                        s.setAction("COMMIT");
                        s.putExtra("p", pkg);
                        s.putExtra("n", name);
                        s.putExtra("w", w);
                        s.putExtra("u", targetOf(m[0]));
                        ctx.startService(s);
                    } catch (Exception e) {
                    }
                    dismissCheck(ctx);
                }
            });
            ScrollView sv = wrap(ctx, col);
            wm.addView(sv, params());
            checkView = sv;
            checkShown = true;
        } catch (Exception e) {
        }
    }

    static void dismissCheck(Context ctx) {
        try {
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            if (checkView != null) wm.removeView(checkView);
        } catch (Exception e) {
        }
        checkView = null;
        checkShown = false;
    }

    // شاشة "هتعمل إيه" بعد الإيقاف المؤقت. بتتسجل الإجابة وبيبدأ عداد الاستراحة
    static void showPause(final Context ctx, final long at) {
        try {
            if (pauseShown || !can(ctx)) return;
            final WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            LinearLayout col = column(ctx);
            col.addView(label(ctx, "هتعمل إيه؟", 26, true));
            final EditText ed = edit(ctx, "اكتب هتعمل إيه");
            col.addView(ed, lp(ctx, 16));
            final Button ok = btn(ctx, "تم");
            ok.setEnabled(false);
            ok.setAlpha(0.5f);
            col.addView(ok, lp(ctx, 20));
            ed.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    boolean v = s.toString().trim().length() >= 2;
                    ok.setEnabled(v);
                    ok.setAlpha(v ? 1f : 0.5f);
                }
            });
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String w = ed.getText().toString().trim();
                    if (w.length() < 2) return;
                    try {
                        long now = System.currentTimeMillis();
                        JSONObject o = new JSONObject();
                        o.put("t", at);
                        o.put("w", w);
                        o.put("a", now);
                        SharedPreferences sp = ctx.getSharedPreferences("mzk", Context.MODE_PRIVATE);
                        sp.edit().putString("pzq", o.toString()).apply();
                        JSONObject n = new JSONObject();
                        n.put("k", "break");
                        n.put("l", w);
                        n.put("s", now);
                        Notifs.timer(ctx, n.toString());
                    } catch (Exception e) {
                    }
                    dismissPause(ctx);
                }
            });
            ScrollView sv = wrap(ctx, col);
            wm.addView(sv, params());
            pauseView = sv;
            pauseShown = true;
        } catch (Exception e) {
        }
    }

    static void dismissPause(Context ctx) {
        try {
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            if (pauseView != null) wm.removeView(pauseView);
        } catch (Exception e) {
        }
        pauseView = null;
        pauseShown = false;
    }
}
