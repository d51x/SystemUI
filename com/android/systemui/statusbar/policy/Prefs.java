package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Prefs {
    public static SharedPreferences read(Context context) {
        return context.getSharedPreferences("status_bar", 0);
    }

    public static Editor edit(Context context) {
        return context.getSharedPreferences("status_bar", 0).edit();
    }
}
