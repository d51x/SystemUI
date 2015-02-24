package com.android.systemui.statusbar.phone;

import android.app.StatusBarManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.storage.StorageManager;
import com.android.internal.telephony.IccCard.State;
import com.android.systemui.usb.StorageNotification;

public class PhoneStatusBarPolicy {
    private static final int[][] sWifiSignalImages;
    private boolean mBluetoothEnabled;
    private final Context mContext;
    private final Handler mHandler;
    private int mInetCondition;
    private BroadcastReceiver mIntentReceiver;
    private boolean mIsWifiConnected;
    private int mLastWifiSignalLevel;
    private final StatusBarManager mService;
    State mSimState;
    private StorageManager mStorageManager;
    private boolean mVolumeVisible;

    static {
        sWifiSignalImages = new int[][]{new int[]{2130837679, 2130837681, 2130837683, 2130837685}, new int[]{2130837680, 2130837682, 2130837684, 2130837686}};
    }

    public PhoneStatusBarPolicy(Context context) {
        this.mHandler = new Handler();
        this.mSimState = State.READY;
        this.mBluetoothEnabled = false;
        this.mLastWifiSignalLevel = -1;
        this.mIsWifiConnected = false;
        this.mInetCondition = 0;
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.ALARM_CHANGED")) {
                    PhoneStatusBarPolicy.this.updateAlarm(intent);
                } else if (action.equals("android.intent.action.SYNC_STATE_CHANGED")) {
                    PhoneStatusBarPolicy.this.updateSyncState(intent);
                } else if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED") || action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                    PhoneStatusBarPolicy.this.updateBluetooth(intent);
                } else if (action.equals("android.media.RINGER_MODE_CHANGED")) {
                    PhoneStatusBarPolicy.this.updateVolume();
                } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    PhoneStatusBarPolicy.this.updateSimState(intent);
                } else if (action.equals("com.android.internal.telephony.cdma.intent.action.TTY_ENABLED_CHANGE")) {
                    PhoneStatusBarPolicy.this.updateTTY(intent);
                }
            }
        };
        this.mContext = context;
        this.mService = (StatusBarManager) context.getSystemService("statusbar");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ALARM_CHANGED");
        filter.addAction("android.intent.action.SYNC_STATE_CHANGED");
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("com.android.internal.telephony.cdma.intent.action.TTY_ENABLED_CHANGE");
        this.mContext.registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
        this.mStorageManager = (StorageManager) context.getSystemService("storage");
        this.mStorageManager.registerListener(new StorageNotification(context));
        this.mService.setIcon("tty", 2130837674, 0, null);
        this.mService.setIconVisibility("tty", false);
        this.mService.setIcon("cdma_eri", 2130837653, 0, null);
        this.mService.setIconVisibility("cdma_eri", false);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int bluetoothIcon = 2130837623;
        if (adapter != null) {
            this.mBluetoothEnabled = adapter.getState() == 12;
            if (adapter.getConnectionState() == 2) {
                bluetoothIcon = 2130837624;
            }
        }
        this.mService.setIcon("bluetooth", bluetoothIcon, 0, null);
        this.mService.setIconVisibility("bluetooth", this.mBluetoothEnabled);
        this.mService.setIcon("alarm_clock", 2130837604, 0, null);
        this.mService.setIconVisibility("alarm_clock", false);
        this.mService.setIcon("sync_active", 2130837672, 0, null);
        this.mService.setIcon("sync_failing", 2130837673, 0, null);
        this.mService.setIconVisibility("sync_active", false);
        this.mService.setIconVisibility("sync_failing", false);
        this.mService.setIcon("volume", 2130837651, 0, null);
        this.mService.setIconVisibility("volume", false);
        updateVolume();
    }

    private final void updateAlarm(Intent intent) {
        this.mService.setIconVisibility("alarm_clock", intent.getBooleanExtra("alarmSet", false));
    }

    private final void updateSyncState(Intent intent) {
    }

    private final void updateSimState(Intent intent) {
        String stateExtra = intent.getStringExtra("ss");
        if ("ABSENT".equals(stateExtra)) {
            this.mSimState = State.ABSENT;
        } else if ("READY".equals(stateExtra)) {
            this.mSimState = State.READY;
        } else if ("LOCKED".equals(stateExtra)) {
            String lockedReason = intent.getStringExtra("reason");
            if ("PIN".equals(lockedReason)) {
                this.mSimState = State.PIN_REQUIRED;
            } else if ("PUK".equals(lockedReason)) {
                this.mSimState = State.PUK_REQUIRED;
            } else {
                this.mSimState = State.NETWORK_LOCKED;
            }
        } else {
            this.mSimState = State.UNKNOWN;
        }
    }

    private final void updateVolume() {
        boolean visible;
        int iconId;
        String contentDescription;
        int ringerMode = ((AudioManager) this.mContext.getSystemService("audio")).getRingerMode();
        if (ringerMode == 0 || ringerMode == 1) {
            visible = true;
        } else {
            visible = false;
        }
        if (ringerMode == 1) {
            iconId = 2130837652;
            contentDescription = this.mContext.getString(2131296362);
        } else {
            iconId = 2130837651;
            contentDescription = this.mContext.getString(2131296363);
        }
        if (visible) {
            this.mService.setIcon("volume", iconId, 0, contentDescription);
        }
        if (visible != this.mVolumeVisible) {
            this.mService.setIconVisibility("volume", visible);
            this.mVolumeVisible = visible;
        }
    }

    private final void updateBluetooth(Intent intent) {
        int iconId = 2130837623;
        String contentDescription = null;
        String action = intent.getAction();
        if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
            boolean z;
            if (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE) == 12) {
                z = true;
            } else {
                z = false;
            }
            this.mBluetoothEnabled = z;
        } else if (!action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
            return;
        } else {
            if (intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", 0) == 2) {
                iconId = 2130837624;
                contentDescription = this.mContext.getString(2131296318);
            } else {
                contentDescription = this.mContext.getString(2131296319);
            }
        }
        this.mService.setIcon("bluetooth", iconId, 0, contentDescription);
        this.mService.setIconVisibility("bluetooth", this.mBluetoothEnabled);
    }

    private final void updateTTY(Intent intent) {
        String action = intent.getAction();
        if (intent.getBooleanExtra("ttyEnabled", false)) {
            this.mService.setIcon("tty", 2130837674, 0, this.mContext.getString(2131296361));
            this.mService.setIconVisibility("tty", true);
            return;
        }
        this.mService.setIconVisibility("tty", false);
    }
}
