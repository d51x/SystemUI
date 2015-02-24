package com.android.systemui.power;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings.System;
import android.util.Slog;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.SystemUI;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class PowerUI extends SystemUI {
    int mBatteryLevel;
    TextView mBatteryLevelTextView;
    int mBatteryStatus;
    Handler mHandler;
    private BroadcastReceiver mIntentReceiver;
    int mInvalidCharger;
    AlertDialog mInvalidChargerDialog;
    int mLowBatteryAlertCloseLevel;
    AlertDialog mLowBatteryDialog;
    int[] mLowBatteryReminderLevels;
    int mPlugType;

    class AnonymousClass_2 implements OnClickListener {
        final /* synthetic */ Intent val$intent;

        AnonymousClass_2(Intent intent) {
            this.val$intent = intent;
        }

        public void onClick(DialogInterface dialog, int which) {
            PowerUI.this.mContext.startActivity(this.val$intent);
            PowerUI.this.dismissLowBatteryWarning();
        }
    }

    public PowerUI() {
        this.mHandler = new Handler();
        this.mBatteryLevel = 100;
        this.mBatteryStatus = 1;
        this.mPlugType = 0;
        this.mInvalidCharger = 0;
        this.mLowBatteryReminderLevels = new int[2];
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean oldPlugged = false;
                if (intent.getAction().equals("android.intent.action.BATTERY_CHANGED")) {
                    boolean plugged;
                    int oldBatteryLevel = PowerUI.this.mBatteryLevel;
                    PowerUI.this.mBatteryLevel = intent.getIntExtra("level", 100);
                    int i = PowerUI.this.mBatteryStatus;
                    PowerUI.this.mBatteryStatus = intent.getIntExtra("status", 1);
                    int oldPlugType = PowerUI.this.mPlugType;
                    PowerUI.this.mPlugType = intent.getIntExtra("plugged", 1);
                    int oldInvalidCharger = PowerUI.this.mInvalidCharger;
                    PowerUI.this.mInvalidCharger = intent.getIntExtra("invalid_charger", 0);
                    if (PowerUI.this.mPlugType != 0) {
                        plugged = true;
                    } else {
                        plugged = false;
                    }
                    if (oldPlugType != 0) {
                        oldPlugged = true;
                    }
                    int oldBucket = PowerUI.this.findBatteryLevelBucket(oldBatteryLevel);
                    int bucket = PowerUI.this.findBatteryLevelBucket(PowerUI.this.mBatteryLevel);
                    if (oldInvalidCharger != 0 || PowerUI.this.mInvalidCharger == 0) {
                        if (oldInvalidCharger != 0 && PowerUI.this.mInvalidCharger == 0) {
                            PowerUI.this.dismissInvalidChargerDialog();
                        } else if (PowerUI.this.mInvalidChargerDialog != null) {
                            return;
                        }
                        if (!plugged) {
                            if ((bucket < oldBucket || oldPlugged) && PowerUI.this.mBatteryStatus != 1 && bucket < 0) {
                                PowerUI.this.showLowBatteryWarning();
                                if (bucket != oldBucket || oldPlugged) {
                                    PowerUI.this.playLowBatterySound();
                                    return;
                                }
                                return;
                            }
                        }
                        if (plugged || (bucket > oldBucket && bucket > 0)) {
                            PowerUI.this.dismissLowBatteryWarning();
                            return;
                        } else if (PowerUI.this.mBatteryLevelTextView != null) {
                            PowerUI.this.showLowBatteryWarning();
                            return;
                        } else {
                            return;
                        }
                    }
                    Slog.d("PowerUI", "showing invalid charger warning");
                    PowerUI.this.showInvalidChargerDialog();
                    return;
                }
                Slog.w("PowerUI", "unknown intent: " + intent);
            }
        };
    }

    public void start() {
        this.mLowBatteryAlertCloseLevel = this.mContext.getResources().getInteger(17694745);
        this.mLowBatteryReminderLevels[0] = this.mContext.getResources().getInteger(17694744);
        this.mLowBatteryReminderLevels[1] = this.mContext.getResources().getInteger(17694743);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        this.mContext.registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
    }

    private int findBatteryLevelBucket(int level) {
        if (level >= this.mLowBatteryAlertCloseLevel) {
            return 1;
        }
        if (level >= this.mLowBatteryReminderLevels[0]) {
            return 0;
        }
        for (int i = this.mLowBatteryReminderLevels.length - 1; i >= 0; i--) {
            if (level <= this.mLowBatteryReminderLevels[i]) {
                return -1 - i;
            }
        }
        throw new RuntimeException("not possible!");
    }

    void dismissLowBatteryWarning() {
        if (this.mLowBatteryDialog != null) {
            Slog.i("PowerUI", "closing low battery warning: level=" + this.mBatteryLevel);
            this.mLowBatteryDialog.dismiss();
        }
    }

    void showLowBatteryWarning() {
        Slog.i("PowerUI", (this.mBatteryLevelTextView == null ? "showing" : "updating") + " low battery warning: level=" + this.mBatteryLevel + " [" + findBatteryLevelBucket(this.mBatteryLevel) + "]");
        CharSequence levelText = this.mContext.getString(2131296272, new Object[]{Integer.valueOf(this.mBatteryLevel)});
        if (this.mBatteryLevelTextView != null) {
            this.mBatteryLevelTextView.setText(levelText);
            return;
        }
        View v = View.inflate(this.mContext, 2130903040, null);
        this.mBatteryLevelTextView = (TextView) v.findViewById(2131492868);
        this.mBatteryLevelTextView.setText(levelText);
        Builder b = new Builder(this.mContext);
        b.setCancelable(true);
        b.setTitle(2131296270);
        b.setView(v);
        b.setIconAttribute(16843605);
        b.setPositiveButton(17039370, null);
        Intent intent = new Intent("android.intent.action.POWER_USAGE_SUMMARY");
        intent.setFlags(1484783616);
        if (intent.resolveActivity(this.mContext.getPackageManager()) != null) {
            b.setNegativeButton(2131296274, new AnonymousClass_2(intent));
        }
        AlertDialog d = b.create();
        d.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                PowerUI.this.mLowBatteryDialog = null;
                PowerUI.this.mBatteryLevelTextView = null;
            }
        });
        d.getWindow().setType(2003);
        d.show();
        this.mLowBatteryDialog = d;
    }

    void playLowBatterySound() {
        ContentResolver cr = this.mContext.getContentResolver();
        if (System.getInt(cr, "power_sounds_enabled", 1) == 1) {
            String soundPath = System.getString(cr, "low_battery_sound");
            if (soundPath != null) {
                Uri soundUri = Uri.parse("file://" + soundPath);
                if (soundUri != null) {
                    Ringtone sfx = RingtoneManager.getRingtone(this.mContext, soundUri);
                    if (sfx != null) {
                        sfx.setStreamType(1);
                        sfx.play();
                    }
                }
            }
        }
    }

    void dismissInvalidChargerDialog() {
        if (this.mInvalidChargerDialog != null) {
            this.mInvalidChargerDialog.dismiss();
        }
    }

    void showInvalidChargerDialog() {
        Slog.d("PowerUI", "showing invalid charger dialog");
        dismissLowBatteryWarning();
        Builder b = new Builder(this.mContext);
        b.setCancelable(true);
        b.setMessage(2131296273);
        b.setIconAttribute(16843605);
        b.setPositiveButton(17039370, null);
        AlertDialog d = b.create();
        d.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                PowerUI.this.mInvalidChargerDialog = null;
                PowerUI.this.mBatteryLevelTextView = null;
            }
        });
        d.getWindow().setType(2003);
        d.show();
        this.mInvalidChargerDialog = d;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("mLowBatteryAlertCloseLevel=");
        pw.println(this.mLowBatteryAlertCloseLevel);
        pw.print("mLowBatteryReminderLevels=");
        pw.println(Arrays.toString(this.mLowBatteryReminderLevels));
        pw.print("mInvalidChargerDialog=");
        pw.println(this.mInvalidChargerDialog == null ? "null" : this.mInvalidChargerDialog.toString());
        pw.print("mLowBatteryDialog=");
        pw.println(this.mLowBatteryDialog == null ? "null" : this.mLowBatteryDialog.toString());
        pw.print("mBatteryLevel=");
        pw.println(Integer.toString(this.mBatteryLevel));
        pw.print("mBatteryStatus=");
        pw.println(Integer.toString(this.mBatteryStatus));
        pw.print("mPlugType=");
        pw.println(Integer.toString(this.mPlugType));
        pw.print("mInvalidCharger=");
        pw.println(Integer.toString(this.mInvalidCharger));
        pw.print("bucket: ");
        pw.println(Integer.toString(findBatteryLevelBucket(this.mBatteryLevel)));
    }
}
