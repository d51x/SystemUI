package com.android.systemui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Slog;

public class DreamsDockLauncher extends Activity {

    public static class DockEventReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            boolean activateOnDock;
            if (Secure.getInt(context.getContentResolver(), "screensaver_activate_on_dock", 0) != 0) {
                activateOnDock = true;
            } else {
                activateOnDock = false;
            }
            if (activateOnDock && "android.intent.action.DOCK_EVENT".equals(intent.getAction())) {
                int state = intent.getExtras().getInt("android.intent.extra.DOCK_STATE", 0);
                if (state == 1 || state == 3 || state == 4) {
                    DreamsDockLauncher.launchDream(context);
                }
            }
        }
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        launchDream(this);
        finish();
    }

    private static void launchDream(Context context) {
        try {
            String component = Secure.getString(context.getContentResolver(), "screensaver_component");
            if (component == null) {
                component = context.getResources().getString(17039404);
            }
            if (component != null) {
                context.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
                Intent zzz = new Intent("android.intent.action.MAIN").setComponent(ComponentName.unflattenFromString(component)).addFlags(1350828036);
                Slog.v("DreamsDockLauncher", "Starting screen saver on dock event: " + component);
                context.startActivity(zzz);
                return;
            }
            Slog.e("DreamsDockLauncher", "Couldn't start screen saver: none selected");
        } catch (ActivityNotFoundException e) {
            Slog.e("DreamsDockLauncher", "Couldn't start screen saver: none installed");
        }
    }
}
