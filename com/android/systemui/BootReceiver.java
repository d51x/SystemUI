package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.System;
import android.util.Slog;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        try {
            if (System.getInt(context.getContentResolver(), "show_processes", 0) != 0) {
                context.startService(new Intent(context, LoadAverageService.class));
            }
        } catch (Throwable e) {
            Slog.e("SystemUIBootReceiver", "Can't start load average service", e);
        }
    }
}
