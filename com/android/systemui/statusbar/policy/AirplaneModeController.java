package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.provider.Settings.System;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AirplaneModeController extends BroadcastReceiver implements OnCheckedChangeListener {
    private boolean mAirplaneMode;
    private CompoundButton mCheckBox;
    private Context mContext;

    class AnonymousClass_1 implements Runnable {
        final /* synthetic */ boolean val$enabled;

        AnonymousClass_1(boolean z) {
            this.val$enabled = z;
        }

        public void run() {
            System.putInt(AirplaneModeController.this.mContext.getContentResolver(), "airplane_mode_on", this.val$enabled ? 1 : 0);
            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
            intent.addFlags(536870912);
            intent.putExtra("state", this.val$enabled);
            AirplaneModeController.this.mContext.sendBroadcast(intent);
        }
    }

    public AirplaneModeController(Context context, CompoundButton checkbox) {
        this.mContext = context;
        this.mAirplaneMode = getAirplaneMode();
        this.mCheckBox = checkbox;
        checkbox.setChecked(this.mAirplaneMode);
        checkbox.setOnCheckedChangeListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        context.registerReceiver(this, filter);
    }

    public void release() {
        this.mContext.unregisterReceiver(this);
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        if (checked != this.mAirplaneMode) {
            this.mAirplaneMode = checked;
            unsafe(checked);
        }
    }

    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
            boolean enabled = intent.getBooleanExtra("state", false);
            if (enabled != this.mAirplaneMode) {
                this.mAirplaneMode = enabled;
                this.mCheckBox.setChecked(enabled);
            }
        }
    }

    private boolean getAirplaneMode() {
        return System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    private void unsafe(boolean enabled) {
        AsyncTask.execute(new AnonymousClass_1(enabled));
    }
}
