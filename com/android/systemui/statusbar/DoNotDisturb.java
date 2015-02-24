package com.android.systemui.statusbar;

import android.app.StatusBarManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import com.android.systemui.statusbar.policy.Prefs;

public class DoNotDisturb implements OnSharedPreferenceChangeListener {
    private Context mContext;
    private boolean mDoNotDisturb;
    SharedPreferences mPrefs;
    private StatusBarManager mStatusBar;

    public DoNotDisturb(Context context) {
        this.mContext = context;
        this.mStatusBar = (StatusBarManager) context.getSystemService("statusbar");
        this.mPrefs = Prefs.read(context);
        this.mPrefs.registerOnSharedPreferenceChangeListener(this);
        this.mDoNotDisturb = this.mPrefs.getBoolean("do_not_disturb", false);
        updateDisableRecord();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        boolean val = prefs.getBoolean("do_not_disturb", false);
        if (val != this.mDoNotDisturb) {
            this.mDoNotDisturb = val;
            updateDisableRecord();
        }
    }

    private void updateDisableRecord() {
        this.mStatusBar.disable(this.mDoNotDisturb ? 917504 : 0);
    }
}
