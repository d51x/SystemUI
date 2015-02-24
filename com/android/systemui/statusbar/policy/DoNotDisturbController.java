package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class DoNotDisturbController implements OnSharedPreferenceChangeListener, OnCheckedChangeListener {
    private CompoundButton mCheckBox;
    private Context mContext;
    private boolean mDoNotDisturb;
    SharedPreferences mPrefs;

    public DoNotDisturbController(Context context, CompoundButton checkbox) {
        boolean z = false;
        this.mContext = context;
        this.mPrefs = Prefs.read(context);
        this.mPrefs.registerOnSharedPreferenceChangeListener(this);
        this.mDoNotDisturb = this.mPrefs.getBoolean("do_not_disturb", false);
        this.mCheckBox = checkbox;
        checkbox.setOnCheckedChangeListener(this);
        if (!this.mDoNotDisturb) {
            z = true;
        }
        checkbox.setChecked(z);
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        boolean value = !checked;
        if (value != this.mDoNotDisturb) {
            Editor editor = Prefs.edit(this.mContext);
            editor.putBoolean("do_not_disturb", value);
            editor.apply();
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        boolean z = false;
        boolean val = prefs.getBoolean("do_not_disturb", false);
        if (val != this.mDoNotDisturb) {
            this.mDoNotDisturb = val;
            CompoundButton compoundButton = this.mCheckBox;
            if (!val) {
                z = true;
            }
            compoundButton.setChecked(z);
        }
    }

    public void release() {
        this.mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }
}
