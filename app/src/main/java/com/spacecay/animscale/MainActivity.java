package com.spacecay.animscale;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    static final float[] STEPS = {0f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 5f, 10f};
    static final float[] PRESETS = {0f, 0.5f, 0.75f, 1f};
    static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";

    static final int BG = 0xFF0E1116, CARD = 0xFF181D25, BTN = 0xFF242B36, TXT = 0xFFF2F4F8,
            SUB = 0xFF9AA4B2, ACC = 0xFF6EA8FE, OK = 0xFF4ADE80, WARN = 0xFFFBBF24;

    private TextView status;
    private LinearLayout setupCard;
    private final Button[] presetBtns = new Button[PRESETS.length];
    private final SeekBar[] bars = new SeekBar[3];
    private final TextView[] vals = new TextView[3];

    private final Shizuku.OnRequestPermissionResultListener permListener = (code, result) -> {
        if (result == PackageManager.PERMISSION_GRANTED) runGrant();
        else runOnUiThread(() -> toast("Shizuku permission was denied"));
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildUi());
        Shizuku.addRequestPermissionResultListener(permListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener);
        super.onDestroy();
    }

    // ---------- UI ----------

    private View buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(28), dp(18), dp(28));
        sv.addView(root);

        root.addView(text("Animation Scale", 26, TXT, true));
        TextView sub = text("Faster animations without keeping Developer options on.", 14, SUB, false);
        sub.setPadding(0, dp(4), 0, dp(16));
        root.addView(sub);

        LinearLayout statusCard = card();
        status = text("", 15, TXT, true);
        statusCard.addView(status);
        root.addView(statusCard);

        // One-time setup
        setupCard = card();
        setupCard.addView(text("One-time setup", 18, TXT, true));
        setupCard.addView(para("Android only lets an app change animation speed with a special permission. "
                + "Grant it once — after that you can turn Developer options off for good."));

        setupCard.addView(heading("Option A — on the phone (Shizuku)"));
        setupCard.addView(para("1. Install Shizuku from the Play Store\n"
                + "2. Settings → Developer options → turn on Wireless debugging\n"
                + "3. Open Shizuku → Pairing → follow its steps → Start\n"
                + "4. Come back here and tap the button"));
        Button grant = button("Grant with Shizuku", ACC, BG);
        grant.setOnClickListener(v -> grantWithShizuku());
        setupCard.addView(grant, fullWidth());

        setupCard.addView(heading("Option B — from a PC (USB cable, no Wi-Fi)"));
        setupCard.addView(para("On the phone:\n"
                + "1. Settings → About phone → Software information → tap Build number 7 times\n"
                + "2. Settings → Developer options → turn on USB debugging\n\n"
                + "On the PC (Windows):\n"
                + "3. Download \"SDK Platform-Tools for Windows\" from developer.android.com/tools/releases/platform-tools and unzip it\n"
                + "4. Open the platform-tools folder, click the address bar, type cmd, press Enter\n"
                + "5. Connect the phone by USB (use a data cable). If asked, choose \"Transferring files\"\n"
                + "6. Type: adb devices — then tap Allow on the phone (tick Always allow)\n"
                + "7. Run adb devices again — you should see your phone with the word \"device\"\n"
                + "8. Paste the command below and press Enter. No output means it worked\n"
                + "9. Unplug, tap Check again here, then turn Developer options off"));
        final String cmd = "adb shell pm grant " + getPackageName()
                + " android.permission.WRITE_SECURE_SETTINGS";
        TextView cmdView = text(cmd, 13, TXT, false);
        cmdView.setTypeface(Typeface.MONOSPACE);
        cmdView.setTextIsSelectable(true);
        cmdView.setPadding(dp(12), dp(10), dp(12), dp(10));
        cmdView.setBackground(round(BG, 10));
        setupCard.addView(cmdView, fullWidth());
        Button copy = button("Copy command", BTN, TXT);
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("adb", cmd));
            toast("Copied");
        });
        setupCard.addView(copy, fullWidth());

        Button check = button("Check again", BTN, TXT);
        check.setOnClickListener(v -> refresh());
        setupCard.addView(check, fullWidth());
        root.addView(setupCard);

        // Presets
        LinearLayout speed = card();
        speed.addView(text("Speed", 18, TXT, true));
        speed.addView(para("Sets all three at once. Lower = faster."));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);
        for (int i = 0; i < PRESETS.length; i++) {
            final float v = PRESETS[i];
            Button pb = button(Scale.label(v), BTN, TXT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            lp.setMargins(i == 0 ? 0 : dp(4), 0, i == PRESETS.length - 1 ? 0 : dp(4), 0);
            pb.setOnClickListener(x -> applyAll(v));
            presetBtns[i] = pb;
            row.addView(pb, lp);
        }
        speed.addView(row);
        root.addView(speed);

        // Individual sliders
        LinearLayout fine = card();
        fine.addView(text("Fine tune", 18, TXT, true));
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setPadding(0, dp(14), 0, 0);
            TextView name = text(Scale.NAMES[i], 15, TXT, false);
            line.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            vals[i] = text("", 15, ACC, true);
            line.addView(vals[i]);
            fine.addView(line);

            SeekBar sb = new SeekBar(this);
            sb.setMax(STEPS.length - 1);
            sb.setProgressTintList(ColorStateList.valueOf(ACC));
            sb.setThumbTintList(ColorStateList.valueOf(ACC));
            sb.setProgressBackgroundTintList(ColorStateList.valueOf(SUB));
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                    if (!fromUser) return;
                    float v = STEPS[p];
                    if (Scale.set(MainActivity.this, Scale.KEYS[idx], v)) {
                        vals[idx].setText(Scale.label(v));
                        highlightPresets();
                    } else {
                        toast("Permission missing — do the setup first");
                        refresh();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar s) {}
                @Override public void onStopTrackingTouch(SeekBar s) {}
            });
            bars[i] = sb;
            fine.addView(sb, fullWidth());
        }
        root.addView(fine);

        LinearLayout tips = card();
        tips.addView(text("Tips", 18, TXT, true));
        tips.addView(para("• Turn Developer options OFF first, then pick your speed here. Switching them off "
                + "resets the speed to 1x.\n"
                + "• Add the \"Animation speed\" tile in Quick Settings (pencil/edit button) to flip between "
                + "your speed and 1x with one tap.\n"
                + "• Your speed is re-applied automatically after a restart."));
        root.addView(tips);

        return sv;
    }

    private void refresh() {
        boolean ok = Scale.canWrite(this);
        if (ok) {
            status.setText("✓  Ready — Developer options can stay off");
            status.setTextColor(OK);
        } else {
            status.setText("⚠  One-time setup needed");
            status.setTextColor(WARN);
        }
        setupCard.setVisibility(ok ? View.GONE : View.VISIBLE);
        for (Button b : presetBtns) b.setEnabled(ok);
        for (int i = 0; i < 3; i++) {
            float v = Scale.get(this, Scale.KEYS[i]);
            bars[i].setEnabled(ok);
            bars[i].setProgress(nearest(v));
            vals[i].setText(Scale.label(v));
        }
        highlightPresets();
    }

    private void highlightPresets() {
        float w = Scale.get(this, Scale.KEYS[0]);
        float t = Scale.get(this, Scale.KEYS[1]);
        float a = Scale.get(this, Scale.KEYS[2]);
        boolean ok = Scale.canWrite(this);
        for (int i = 0; i < PRESETS.length; i++) {
            float p = PRESETS[i];
            boolean on = same(w, p) && same(t, p) && same(a, p);
            presetBtns[i].setBackground(round(on ? ACC : BTN, 12));
            presetBtns[i].setTextColor(on ? BG : TXT);
            presetBtns[i].setAlpha(ok ? 1f : 0.4f);
        }
    }

    private void applyAll(float v) {
        if (Scale.setAll(this, v)) {
            refresh();
            toast("Animations set to " + Scale.label(v));
        } else {
            toast("Permission missing — do the setup first");
            refresh();
        }
    }

    // ---------- Shizuku grant ----------

    private void grantWithShizuku() {
        boolean alive;
        try {
            alive = Shizuku.pingBinder();
        } catch (Throwable e) {
            alive = false;
        }
        if (!alive) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
            if (launch != null) {
                toast("Shizuku isn't running — start it, then come back");
                startActivity(launch);
            } else {
                toast("Install Shizuku first");
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + SHIZUKU_PKG)));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + SHIZUKU_PKG)));
                }
            }
            return;
        }
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) runGrant();
            else Shizuku.requestPermission(1);
        } catch (Throwable e) {
            toast("Shizuku error: " + e.getMessage());
        }
    }

    private void runGrant() {
        new Thread(() -> {
            int code;
            String err = null;
            try {
                Method m = Shizuku.class.getDeclaredMethod("newProcess",
                        String[].class, String[].class, String.class);
                m.setAccessible(true);
                Process p = (Process) m.invoke(null, new Object[]{
                        new String[]{"pm", "grant", getPackageName(),
                                "android.permission.WRITE_SECURE_SETTINGS"}, null, null});
                code = p.waitFor();
            } catch (Throwable e) {
                code = -1;
                err = e.getClass().getSimpleName();
            }
            final int c = code;
            final String er = err;
            runOnUiThread(() -> {
                refresh();
                if (Scale.canWrite(this)) {
                    Scale.reapply(this);
                    toast("Done! Turn Developer options off, then pick your speed.");
                } else {
                    toast("Grant failed" + (er != null ? " (" + er + ")" : " (code " + c + ")"));
                }
            });
        }).start();
    }

    // ---------- helpers ----------

    private static boolean same(float a, float b) {
        return Math.abs(a - b) < 0.01f;
    }

    private static int nearest(float v) {
        int best = 0;
        for (int i = 1; i < STEPS.length; i++) {
            if (Math.abs(STEPS[i] - v) < Math.abs(STEPS[best] - v)) best = i;
        }
        return best;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(round(CARD, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        c.setLayoutParams(lp);
        return c;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView para(String s) {
        TextView t = text(s, 14, SUB, false);
        t.setLineSpacing(dp(2), 1f);
        t.setPadding(0, dp(6), 0, dp(4));
        return t;
    }

    private TextView heading(String s) {
        TextView t = text(s, 15, TXT, true);
        t.setPadding(0, dp(16), 0, 0);
        return t;
    }

    private Button button(String label, int bg, int fg) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(fg);
        b.setGravity(Gravity.CENTER);
        b.setStateListAnimator(null);
        b.setBackground(round(bg, 12));
        return b;
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, 0);
        return lp;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
