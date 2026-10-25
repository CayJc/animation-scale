package com.spacecay.animscale;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class AnimTileService extends TileService {

    @Override
    public void onStartListening() {
        update();
    }

    @Override
    public void onClick() {
        if (!Scale.canWrite(this)) {
            openApp();
            return;
        }
        float cur = Scale.get(this, Scale.KEYS[0]);
        float target = Math.abs(cur - 1f) < 0.01f ? Scale.fast(this) : 1f;
        Scale.setAll(this, target);
        update();
    }

    private void update() {
        Tile t = getQsTile();
        if (t == null) return;
        float cur = Scale.get(this, Scale.KEYS[0]);
        boolean fast = Math.abs(cur - 1f) > 0.01f;
        t.setLabel("Animation speed");
        if (Scale.canWrite(this)) {
            t.setState(fast ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            t.setSubtitle(Scale.label(cur));
        } else {
            t.setState(Tile.STATE_INACTIVE);
            t.setSubtitle("Tap to set up");
        }
        t.updateTile();
    }

    @SuppressWarnings("deprecation")
    private void openApp() {
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE));
        } else {
            startActivityAndCollapse(i);
        }
    }
}
