package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.os.AsyncTask;
import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import com.android.systemui.statusbar.policy.ToggleSlider.Listener;

public class BrightnessController implements Listener {
    private Context mContext;
    private ToggleSlider mControl;
    private IPowerManager mPower;

    class AnonymousClass_1 implements Runnable {
        final /* synthetic */ int val$val;

        AnonymousClass_1(int i) {
            this.val$val = i;
        }

        public void run() {
            System.putInt(BrightnessController.this.mContext.getContentResolver(), "screen_brightness", this.val$val);
        }
    }

    public BrightnessController(Context context, ToggleSlider control) {
        int value;
        boolean z = false;
        this.mContext = context;
        this.mControl = control;
        boolean automaticAvailable = context.getResources().getBoolean(17891345);
        this.mPower = Stub.asInterface(ServiceManager.getService("power"));
        if (automaticAvailable) {
            int automatic;
            try {
                automatic = System.getInt(this.mContext.getContentResolver(), "screen_brightness_mode");
            } catch (SettingNotFoundException e) {
                automatic = 0;
            }
            if (automatic != 0) {
                z = true;
            }
            control.setChecked(z);
        } else {
            control.setChecked(false);
        }
        try {
            value = System.getInt(this.mContext.getContentResolver(), "screen_brightness");
        } catch (SettingNotFoundException e2) {
            value = 255;
        }
        control.setMax(235);
        control.setValue(value - 20);
        control.setOnChangedListener(this);
    }

    public void onChanged(ToggleSlider view, boolean tracking, boolean automatic, int value) {
        setMode(automatic ? 1 : 0);
        if (!automatic) {
            int val = value + 20;
            setBrightness(val);
            if (!tracking) {
                AsyncTask.execute(new AnonymousClass_1(val));
            }
        }
    }

    private void setMode(int mode) {
        System.putInt(this.mContext.getContentResolver(), "screen_brightness_mode", mode);
    }

    private void setBrightness(int brightness) {
        try {
            this.mPower.setBacklightBrightness(brightness);
        } catch (RemoteException e) {
        }
    }
}
