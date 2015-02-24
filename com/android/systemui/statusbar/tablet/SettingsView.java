package com.android.systemui.statusbar.tablet;

import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import com.android.systemui.statusbar.policy.AirplaneModeController;
import com.android.systemui.statusbar.policy.AutoRotateController;
import com.android.systemui.statusbar.policy.AutoRotateController.RotationLockCallbacks;
import com.android.systemui.statusbar.policy.BrightnessController;
import com.android.systemui.statusbar.policy.DoNotDisturbController;
import com.android.systemui.statusbar.policy.ToggleSlider;

public class SettingsView extends LinearLayout implements OnClickListener {
    AirplaneModeController mAirplane;
    BrightnessController mBrightness;
    DoNotDisturbController mDoNotDisturb;
    AutoRotateController mRotate;
    View mRotationLockContainer;
    View mRotationLockSeparator;

    public SettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        Context context = getContext();
        this.mAirplane = new AirplaneModeController(context, (CompoundButton) findViewById(2131493013));
        findViewById(2131493014).setOnClickListener(this);
        this.mRotationLockContainer = findViewById(2131493017);
        this.mRotationLockSeparator = findViewById(2131493021);
        this.mRotate = new AutoRotateController(context, (CompoundButton) findViewById(2131493020), new RotationLockCallbacks() {
            public void setRotationLockControlVisibility(boolean show) {
                int i;
                int i2 = 0;
                View view = SettingsView.this.mRotationLockContainer;
                if (show) {
                    i = 0;
                } else {
                    i = 8;
                }
                view.setVisibility(i);
                View view2 = SettingsView.this.mRotationLockSeparator;
                if (!show) {
                    i2 = 8;
                }
                view2.setVisibility(i2);
            }
        });
        this.mBrightness = new BrightnessController(context, (ToggleSlider) findViewById(2131493023));
        this.mDoNotDisturb = new DoNotDisturbController(context, (CompoundButton) findViewById(2131493026));
        findViewById(2131493027).setOnClickListener(this);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAirplane.release();
        this.mDoNotDisturb.release();
        this.mRotate.release();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case 2131493014:
                onClickNetwork();
            case 2131493027:
                onClickSettings();
            default:
                break;
        }
    }

    private StatusBarManager getStatusBarManager() {
        return (StatusBarManager) getContext().getSystemService("statusbar");
    }

    private void onClickNetwork() {
        getContext().startActivity(new Intent("android.settings.WIFI_SETTINGS").setFlags(268435456));
        getStatusBarManager().collapse();
    }

    private void onClickSettings() {
        getContext().startActivity(new Intent("android.settings.SETTINGS").setFlags(268435456));
        getStatusBarManager().collapse();
    }
}
