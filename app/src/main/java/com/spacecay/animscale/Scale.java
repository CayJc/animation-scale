package com.spacecay.animscale;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;

final class Scale {
    static final String[] KEYS = {
            Settings.Global.WINDOW_ANIMATION_SCALE,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            Settings.Global.ANIMATOR_DURATION_SCALE
    };
    static final String[] NAMES = {
            "Window animation",
            "Transition animation",
            "Animator duration"
    };

    private Scale() {}

    static boolean canWrite(Context c) {
        return c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static float get(Context c, String key) {
        return Settings.Global.getFloat(c.getContentResolver(), key, 1f);
    }

    static boolean set(Context c, String key, float v) {
        try {
            Settings.Global.putFloat(c.getContentResolver(), key, v);
            prefs(c).edit().putFloat(key, v).putBoolean("saved", true).apply();
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    static boolean setAll(Context c, float v) {
        boolean ok = true;
        for (String k : KEYS) ok &= set(c, k, v);
        if (ok && Math.abs(v - 1f) > 0.01f) prefs(c).edit().putFloat("fast", v).apply();
        return ok;
    }

    /** The speed the tile switches to when leaving 1x. */
    static float fast(Context c) {
        return prefs(c).getFloat("fast", 0.5f);
    }

    /** Re-apply the user's chosen speeds (e.g. after reboot or a Samsung reset). */
    static void reapply(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("saved", false) || !canWrite(c)) return;
        for (String k : KEYS) {
            if (p.contains(k)) {
                try {
                    Settings.Global.putFloat(c.getContentResolver(), k, p.getFloat(k, 1f));
                } catch (SecurityException ignored) {}
            }
        }
    }

    static String label(float v) {
        if (v < 0.01f) return "Off";
        if (Math.abs(v - Math.round(v)) < 0.01f) return Math.round(v) + "x";
        String s = String.valueOf(Math.round(v * 100f) / 100f);
        return s + "x";
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("anim", Context.MODE_PRIVATE);
    }
}
