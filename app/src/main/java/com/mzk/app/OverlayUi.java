package com.mzk.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
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
    static volatile String checkPkg = null;
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

    static EditText numEdit(Context c, String v) {
        EditText e = new EditText(c);
        e.setText(v);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        e.setTextSize(34);
        e.setTypeface(null, Typeface.BOLD);
        e.setGravity(Gravity.CENTER);
        e.setTextColor(Color.parseColor("#111111"));
        e.setBackgroundColor(Color.parseColor("#EEEEEE"));
        e.setSelectAllOnFocus(true);
        return e;
    }

    static int parseInt2(EditText e) {
        try {
            String s = e.getText().toString().trim();
            if (s.isEmpty()) return -1;
            return Integer.parseInt(s);
        } catch (Exception ex) {
            return -1;
        }
    }

    // الساعة (1-12) والدقيقة + ص/م -> دقايق من نص الليل، أو -1 لو غلط
    static int minsOf(EditText eh, EditText em, int ap) {
        int h = parseInt2(eh);
        int m = parseInt2(em);
        if (h < 1 || h > 12 || m < 0 || m > 59) return -1;
        return (h % 12 + (ap == 1 ? 12 : 0)) * 60 + m;
    }

    static void refreshInfo(TextView info, EditText eh, EditText em, int ap) {
        int mins = minsOf(eh, em, ap);
        if (mins < 0) {
            info.setText("اكتب الساعة والدقايق");
        } else {
            info.setText("بعد " + afterTxt(targetOf(mins)));
        }
    }

    // المدة بالدقايق من خانتين: ساعات ودقايق (الخانة الفاضية = صفر). 0 أو أكتر من 24 ساعة = غلط
    static int durMins(EditText eh, EditText em) {
        int h = parseInt2(eh);
        int m = parseInt2(em);
        if (h < 0) h = 0;
        if (m < 0) m = 0;
        int d = h * 60 + m;
        return d > 1440 ? -1 : d;
    }

    static void refreshDur(TextView info, EditText eh, EditText em) {
        int d = durMins(eh, em);
        if (d <= 0) {
            info.setText("اكتب المدة");
        } else {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MINUTE, d);
            info.setText("هتوقف الساعة " + clock(c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)));
        }
    }

    static void paintAp(Button am, Button pm, int ap) {
        am.setBackgroundColor(Color.parseColor(ap == 0 ? "#7C5CE0" : "#9E9E9E"));
        pm.setBackgroundColor(Color.parseColor(ap == 1 ? "#7C5CE0" : "#9E9E9E"));
    }

    // سؤال: بتعمل إيه في التطبيق ده + هتقعد قد إيه (ساعات ودقايق)
    static void showCheck(final Context ctx, final String pkg, final String name, final int wait) {
        try {
            if (checkShown || !can(ctx)) return;
            final WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            LinearLayout col = column(ctx);
            col.addView(label(ctx, "ليه دخلت " + name + "؟", 22, true));
            final EditText ed = edit(ctx, "اكتب السبب");
            col.addView(ed, lp(ctx, 14));
            col.addView(label(ctx, "هتقعد قد إيه؟", 18, false), lp(ctx, 22));

            final int[] sel = new int[]{15};
            final EditText em = numEdit(ctx, "30");
            em.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
            em.setVisibility(View.GONE);
            final int[] opts = new int[]{1, 5, 15, -1};
            final String[] names = new String[]{"1 د", "5 د", "15 د", "مخصص"};
            final Button[] chips = new Button[4];
            final Button ok = btn(ctx, "افتح لمدة 15 د");
            LinearLayout r1 = new LinearLayout(ctx);
            r1.setOrientation(LinearLayout.HORIZONTAL);
            r1.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            r1.setGravity(Gravity.CENTER);
            for (int i = 0; i < 4; i++) {
                final int idx = i;
                Button c = btn(ctx, names[i]);
                chips[i] = c;
                c.setAlpha(opts[i] == sel[0] ? 1f : 0.45f);
                c.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sel[0] = opts[idx];
                        for (int k = 0; k < 4; k++) chips[k].setAlpha(k == idx ? 1f : 0.45f);
                        em.setVisibility(opts[idx] < 0 ? View.VISIBLE : View.GONE);
                        int m = opts[idx] > 0 ? opts[idx] : parseInt2(em);
                        ok.setText(m > 0 ? "افتح لمدة " + m + " د" : "اكتب المدة بالدقايق");
                    }
                });
                r1.addView(c, weight());
            }
            col.addView(r1, lp(ctx, 10));
            col.addView(em, lp(ctx, 8));
            em.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a1, int b1, int c1) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a1, int b1, int c1) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (sel[0] < 0) {
                        int m = parseInt2(em);
                        ok.setText(m > 0 ? "افتح لمدة " + m + " د" : "اكتب المدة بالدقايق");
                    }
                }
            });

            final SharedPreferences wp = ctx.getSharedPreferences("watch", Context.MODE_PRIVATE);
            final boolean needGate = wait > 0 && wp.getBoolean("ch", true);
            final int chn = Math.max(1, Math.min(4, wp.getInt("chn", 1)));
            final boolean[] gate = new boolean[]{!needGate};
            final boolean[] timeUp = new boolean[]{wait <= 0};
            final Runnable upd = new Runnable() {
                @Override
                public void run() {
                    boolean v = gate[0] && timeUp[0];
                    ok.setEnabled(v);
                    ok.setAlpha(v ? 1f : 0.5f);
                }
            };
            if (needGate) {
                final java.util.List<String> pool = new java.util.ArrayList<String>();
                final java.util.Map<String, String> authOf = new java.util.HashMap<String, String>();
                try {
                    org.json.JSONArray ja = new org.json.JSONArray(wp.getString("sn", "[]"));
                    for (int q = 0; q < ja.length(); q++) {
                        String x = ja.optString(q).trim();
                        String au = "";
                        int ci = x.indexOf((char) 1);
                        if (ci >= 0) {
                            au = x.substring(ci + 1).trim();
                            x = x.substring(0, ci).trim();
                        }
                        if (x.length() > 8) {
                            pool.add(x);
                            authOf.put(x, au);
                        }
                    }
                } catch (Exception ex) {
                }
                if (pool.isEmpty()) {
                    pool.add("Efficiency is doing things right; effectiveness is doing the right things.");
                    authOf.put("Efficiency is doing things right; effectiveness is doing the right things.", "Peter Drucker");
                    pool.add("Well done is better than well said.");
                    authOf.put("Well done is better than well said.", "Benjamin Franklin");
                    pool.add("We are what we repeatedly do. Excellence, then, is not an act, but a habit.");
                    authOf.put("We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Will Durant");
                    pool.add("Nothing is particularly hard if you divide it into small jobs.");
                    authOf.put("Nothing is particularly hard if you divide it into small jobs.", "Henry Ford");
                    pool.add("Chance favors only the prepared mind.");
                    authOf.put("Chance favors only the prepared mind.", "Louis Pasteur");
                    pool.add("The main thing is to keep the main thing the main thing.");
                    authOf.put("The main thing is to keep the main thing the main thing.", "Stephen Covey");
                }
                java.util.Collections.shuffle(pool);
                String ctS = wp.getString("ct", "copy,upper,rev,ini");
                final String[] types = ctS.trim().isEmpty() ? new String[]{"copy"} : ctS.split(",");
                final String[][] chs = new String[chn][];
                for (int q = 0; q < chn; q++) {
                    chs[q] = chal(pool.get(q % pool.size()), types[(int) (Math.random() * types.length)].trim());
                }
                final int[] ix = new int[]{0};
                final TextView st = label(ctx, "خلصت وقتك. " + chs[0][0], 16, true);
                col.addView(st, lp(ctx, 18));
                final TextView sl = label(ctx, chs[0][2] + auOf(authOf, chs[0][2]), 18, false);
                sl.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                sl.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                col.addView(sl, lp(ctx, 8));
                final EditText es = edit(ctx, "اكتب هنا");
                es.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                es.setLongClickable(false);
                es.setTextIsSelectable(false);
                col.addView(es, lp(ctx, 8));
                startRing(ctx);
                es.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int a1, int b1, int c1) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int a1, int b1, int c1) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        hushRing(ctx, 20000);
                        String[] cur = chs[ix[0]];
                        String typed = s.toString().trim();
                        boolean hit = "ini".equals(cur[3]) ? typed.equalsIgnoreCase(cur[1]) : typed.equals(cur[1]);
                        if (!hit) return;
                        ix[0]++;
                        if (ix[0] >= chn) {
                            gate[0] = true;
                            stopRing();
                            upd.run();
                        } else {
                            st.setText("تحدي " + (ix[0] + 1) + "/" + chn + ": " + chs[ix[0]][0]);
                            sl.setText(chs[ix[0]][2] + auOf(authOf, chs[ix[0]][2]));
                            es.setText("");
                        }
                    }
                });
            }
            col.addView(ok, lp(ctx, 22));
            if (wait > 0) {
                ok.setEnabled(false);
                ok.setAlpha(0.5f);
                final CharSequence okTxt = ok.getText();
                final int[] left = new int[]{wait};
                H.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!checkShown) return;
                        left[0]--;
                        if (left[0] <= 0) {
                            timeUp[0] = true;
                            upd.run();
                        } else {
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
                        Toast.makeText(ctx, "اكتب السبب", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int dur = sel[0] > 0 ? sel[0] : parseInt2(em);
                    if (dur <= 0) {
                        Toast.makeText(ctx, "اكتب المدة بالدقايق", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        if (!"__test".equals(pkg)) {
                        Intent s = new Intent(ctx, AppWatch.class);
                        s.setAction("COMMIT");
                        s.putExtra("p", pkg);
                        s.putExtra("n", name);
                        s.putExtra("w", w);
                        s.putExtra("u", System.currentTimeMillis() + dur * 60000L);
                        ctx.startService(s);
                        }
                    } catch (Exception e) {
                    }
                    dismissCheck(ctx);
                }
            });
            ScrollView sv = wrap(ctx, col);
            wm.addView(sv, params());
            checkView = sv;
            checkPkg = pkg;
            checkShown = true;
        } catch (Exception e) {
        }
    }

    static void dismissCheck(Context ctx) {
        stopRing();
        try {
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            if (checkView != null) wm.removeView(checkView);
        } catch (Exception e) {
        }
        checkView = null;
        checkPkg = null;
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

    static String auOf(java.util.Map<String, String> m, String s) {
        String a = m.get(s);
        return (a == null || a.isEmpty()) ? "" : "\n\u2014 " + a;
    }

    // [instruction, expected answer, text to show, type]
    static String[] chal(String sent, String type) {
        String[] w = sent.trim().split("\\s+");
        if ("upper".equals(type)) {
            return new String[]{"اكتب الجملة دي كلها بحروف كبيرة (CAPITAL):", sent.trim().toUpperCase(java.util.Locale.US), sent.trim(), "upper"};
        }
        if ("rev".equals(type)) {
            StringBuilder b = new StringBuilder();
            for (int i = w.length - 1; i >= 0; i--) {
                if (b.length() > 0) b.append(" ");
                b.append(w[i]);
            }
            return new String[]{"اكتب كلمات الجملة بالعكس (من آخر كلمة لأول كلمة، بنفس الترقيم):", b.toString(), sent.trim(), "rev"};
        }
        if ("ini".equals(type)) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < w.length; i++) {
                for (int k = 0; k < w[i].length(); k++) {
                    char ch = w[i].charAt(k);
                    if (Character.isLetterOrDigit(ch)) {
                        b.append(Character.toUpperCase(ch));
                        break;
                    }
                }
            }
            return new String[]{"اكتب أول حرف من كل كلمة ورا بعض:", b.toString(), sent.trim(), "ini"};
        }
        return new String[]{"انسخ الجملة دي بالظبط:", sent.trim(), sent.trim(), "copy"};
    }

    // ---------- alarm-like ringing (volume forced up while the challenge is open) ----------
    static Ringtone ring = null;
    static boolean ringing = false;

    static boolean ringWanted = false;
    static int ringTok = 0;

    static void hushRing(final Context c, long ms) {
        final int t = ++ringTok;
        stopRingNow();
        H.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (t == ringTok && ringWanted) startRing(c);
            }
        }, ms);
    }

    static void startRing(final Context c) {
        try {
            stopRingNow();
            ringWanted = true;
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ring = RingtoneManager.getRingtone(c, u);
            if (ring == null) return;
            if (Build.VERSION.SDK_INT >= 21) {
                ring.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            }
            if (Build.VERSION.SDK_INT >= 28) ring.setLooping(true);
            ringing = true;
            final AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            H.post(new Runnable() {
                @Override
                public void run() {
                    if (!ringing || ring == null) return;
                    try {
                        am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
                        if (!ring.isPlaying()) ring.play();
                    } catch (Exception e) {
                    }
                    H.postDelayed(this, 3000);
                }
            });
        } catch (Exception e) {
        }
    }

    static void stopRing() {
        ringWanted = false;
        ringTok++;
        stopRingNow();
    }

    static void stopRingNow() {
        ringing = false;
        try {
            if (ring != null) ring.stop();
        } catch (Exception e) {
        }
        ring = null;
    }

    // ---------- floating shrinking-ring bubble for the running timer ----------
    static View bubbleView = null;

    static String clk(long ms) {
        long t = Math.max(0L, ms) / 1000L;
        return String.format(java.util.Locale.US, "%d:%02d", t / 60, t % 60);
    }

    static class RingView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        long s = 0, e = 0;
        boolean brk = false;

        RingView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas cv) {
            int w = getWidth(), h = getHeight();
            float st = dp(getContext(), 4);
            RectF r = new RectF(st, st, w - st, h - st);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xE6212121);
            cv.drawCircle(w / 2f, h / 2f, w / 2f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(st);
            p.setColor(0x55FFFFFF);
            cv.drawArc(r, 0, 360, false, p);
            long now = System.currentTimeMillis();
            float frac;
            String txt;
            if (e > now) {
                frac = Math.max(0f, Math.min(1f, (float) (e - now) / (float) Math.max(1L, e - s)));
                txt = clk(e - now);
            } else if (e > 0) {
                frac = 0f;
                txt = "+" + clk(now - e);
            } else {
                frac = 1f;
                txt = clk(now - s);
            }
            p.setColor(brk ? 0xFFFFB74D : 0xFF4FC3F7);
            p.setStrokeCap(Paint.Cap.ROUND);
            cv.drawArc(r, -90, 360f * frac, false, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(dp(getContext(), 11));
            p.setTypeface(Typeface.DEFAULT_BOLD);
            cv.drawText(txt, w / 2f, h / 2f + dp(getContext(), 5), p);
        }
    }

    static View panelView = null;
    static boolean bubbleHidden = false;
    static long hiddenS = 0L;

    static void hidePanel(Context ctx) {
        try {
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            if (panelView != null) wm.removeView(panelView);
        } catch (Exception e) {
        }
        panelView = null;
    }

    static void showBubblePanel(final Context ctx, final RingView bv) {
        try {
            if (panelView != null) {
                hidePanel(ctx);
                return;
            }
            final WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setBackgroundColor(0xF2212121);
            col.setPadding(dp(ctx, 8), dp(ctx, 8), dp(ctx, 8), dp(ctx, 8));
            String[] names = new String[]{"استراحة", "فتح التطبيق", "إخفاء العداد", "إنهاء العداد"};
            for (int q = 0; q < names.length; q++) {
                final int id = q;
                Button b = btn(ctx, names[q]);
                col.addView(b, lp(ctx, 6));
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hidePanel(ctx);
                        try {
                            if (id == 0) {
                                Intent i = new Intent(ctx, TimerReceiver.class);
                                i.setAction("PAUSE");
                                ctx.sendBroadcast(i);
                            } else if (id == 1) {
                                Intent in = new Intent(ctx, MainActivity.class);
                                in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                ctx.startActivity(in);
                            } else if (id == 2) {
                                bubbleHidden = true;
                                hiddenS = bv.s;
                                hideBubble(ctx);
                            } else {
                                Util.openAlarm(ctx, "#stop");
                            }
                        } catch (Exception e) {
                        }
                    }
                });
            }
            int type = Build.VERSION.SDK_INT >= 26
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    dp(ctx, 170), WindowManager.LayoutParams.WRAP_CONTENT, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.END;
            lp.x = dp(ctx, 12);
            lp.y = dp(ctx, 190);
            wm.addView(col, lp);
            panelView = col;
            H.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hidePanel(ctx);
                }
            }, 8000);
        } catch (Exception e) {
        }
    }

    static void hideBubble(Context ctx) {
        hidePanel(ctx);
        try {
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            if (bubbleView != null) wm.removeView(bubbleView);
        } catch (Exception e) {
        }
        bubbleView = null;
    }

    static void bubble(final Context ctx, final String json) {
        H.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (json == null || json.isEmpty()) {
                        hideBubble(ctx);
                        return;
                    }
                    JSONObject o = new JSONObject(json);
                    long sNew = o.optLong("s", 0L);
                    if (bubbleHidden && sNew == hiddenS) {
                        hideBubble(ctx);
                        return;
                    }
                    if (sNew != hiddenS) bubbleHidden = false;
                    if (!o.optBoolean("bub", true) || !can(ctx)) {
                        hideBubble(ctx);
                        return;
                    }
                    final WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
                    RingView v;
                    if (bubbleView == null) {
                        v = new RingView(ctx);
                        int type = Build.VERSION.SDK_INT >= 26
                                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                : WindowManager.LayoutParams.TYPE_PHONE;
                        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                                dp(ctx, 58), dp(ctx, 58), type,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                                PixelFormat.TRANSLUCENT);
                        lp.gravity = Gravity.TOP | Gravity.END;
                        lp.x = dp(ctx, 12);
                        lp.y = dp(ctx, 120);
                        final float[] d = new float[4];
                        v.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, android.view.MotionEvent ev) {
                                switch (ev.getAction()) {
                                    case android.view.MotionEvent.ACTION_DOWN:
                                        d[0] = ev.getRawX();
                                        d[1] = ev.getRawY();
                                        d[2] = lp.x;
                                        d[3] = lp.y;
                                        return true;
                                    case android.view.MotionEvent.ACTION_MOVE:
                                        lp.x = (int) (d[2] - (ev.getRawX() - d[0]));
                                        lp.y = (int) (d[3] + (ev.getRawY() - d[1]));
                                        try {
                                            wm.updateViewLayout(view, lp);
                                        } catch (Exception e) {
                                        }
                                        return true;
                                    case android.view.MotionEvent.ACTION_UP:
                                        if (Math.abs(ev.getRawX() - d[0]) < 12 && Math.abs(ev.getRawY() - d[1]) < 12) {
                                            showBubblePanel(ctx, (RingView) view);
                                        }
                                        return true;
                                }
                                return false;
                            }
                        });
                        wm.addView(v, lp);
                        bubbleView = v;
                        H.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (bubbleView == null) return;
                                bubbleView.invalidate();
                                H.postDelayed(this, 1000);
                            }
                        }, 1000);
                    } else {
                        v = (RingView) bubbleView;
                    }
                    v.s = o.optLong("s", System.currentTimeMillis());
                    v.e = o.optLong("e", 0);
                    v.brk = "break".equals(o.optString("k"));
                    v.invalidate();
                } catch (Exception e) {
                }
            }
        });
    }
}
