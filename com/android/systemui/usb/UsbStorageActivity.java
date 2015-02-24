package com.android.systemui.usb;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.IMountService.Stub;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;

public class UsbStorageActivity extends Activity implements OnCancelListener, OnClickListener {
    private Handler mAsyncStorageHandler;
    private TextView mBanner;
    private boolean mDestroyed;
    private ImageView mIcon;
    private TextView mMessage;
    private Button mMountButton;
    private ProgressBar mProgressBar;
    private StorageEventListener mStorageListener;
    private StorageManager mStorageManager;
    private Handler mUIHandler;
    private Button mUnmountButton;
    private BroadcastReceiver mUsbStateReceiver;

    class AnonymousClass_3 implements Runnable {
        final /* synthetic */ boolean val$usbStorageInUse;

        AnonymousClass_3(boolean z) {
            this.val$usbStorageInUse = z;
        }

        public void run() {
            UsbStorageActivity.this.switchDisplayAsync(this.val$usbStorageInUse);
        }
    }

    class AnonymousClass_6 implements Runnable {
        final /* synthetic */ int val$id;

        AnonymousClass_6(int i) {
            this.val$id = i;
        }

        public void run() {
            if (!UsbStorageActivity.this.mDestroyed) {
                UsbStorageActivity.this.removeDialog(this.val$id);
                UsbStorageActivity.this.showDialog(this.val$id);
            }
        }
    }

    class AnonymousClass_8 implements Runnable {
        final /* synthetic */ boolean val$on;

        AnonymousClass_8(boolean z) {
            this.val$on = z;
        }

        public void run() {
            if (this.val$on) {
                UsbStorageActivity.this.mStorageManager.enableUsbMassStorage();
            } else {
                UsbStorageActivity.this.mStorageManager.disableUsbMassStorage();
            }
        }
    }

    public UsbStorageActivity() {
        this.mStorageManager = null;
        this.mUsbStateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.hardware.usb.action.USB_STATE")) {
                    UsbStorageActivity.this.handleUsbStateChanged(intent);
                }
            }
        };
        this.mStorageListener = new StorageEventListener() {
            public void onStorageStateChanged(String path, String oldState, String newState) {
                UsbStorageActivity.this.switchDisplay(newState.equals("shared"));
            }
        };
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mStorageManager == null) {
            this.mStorageManager = (StorageManager) getSystemService("storage");
            if (this.mStorageManager == null) {
                Log.w("UsbStorageActivity", "Failed to get StorageManager");
            }
        }
        this.mUIHandler = new Handler();
        HandlerThread thr = new HandlerThread("SystemUI UsbStorageActivity");
        thr.start();
        this.mAsyncStorageHandler = new Handler(thr.getLooper());
        getWindow().addFlags(4194304);
        if (Environment.isExternalStorageRemovable()) {
            getWindow().addFlags(524288);
        }
        setContentView(17367234);
        this.mIcon = (ImageView) findViewById(16908294);
        this.mBanner = (TextView) findViewById(16909147);
        this.mMessage = (TextView) findViewById(16908299);
        this.mMountButton = (Button) findViewById(16909148);
        this.mMountButton.setOnClickListener(this);
        this.mUnmountButton = (Button) findViewById(16909149);
        this.mUnmountButton.setOnClickListener(this);
        this.mProgressBar = (ProgressBar) findViewById(16908301);
    }

    protected void onDestroy() {
        super.onDestroy();
        this.mDestroyed = true;
    }

    private void switchDisplay(boolean usbStorageInUse) {
        this.mUIHandler.post(new AnonymousClass_3(usbStorageInUse));
    }

    private void switchDisplayAsync(boolean usbStorageInUse) {
        if (usbStorageInUse) {
            this.mProgressBar.setVisibility(8);
            this.mUnmountButton.setVisibility(0);
            this.mMountButton.setVisibility(8);
            this.mIcon.setImageResource(17303025);
            this.mBanner.setText(17040436);
            this.mMessage.setText(17040437);
            return;
        }
        this.mProgressBar.setVisibility(8);
        this.mUnmountButton.setVisibility(8);
        this.mMountButton.setVisibility(0);
        this.mIcon.setImageResource(17303024);
        this.mBanner.setText(17040428);
        this.mMessage.setText(17040429);
    }

    protected void onResume() {
        super.onResume();
        this.mStorageManager.registerListener(this.mStorageListener);
        registerReceiver(this.mUsbStateReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        try {
            this.mAsyncStorageHandler.post(new Runnable() {
                public void run() {
                    UsbStorageActivity.this.switchDisplay(UsbStorageActivity.this.mStorageManager.isUsbMassStorageEnabled());
                }
            });
        } catch (Exception ex) {
            Log.e("UsbStorageActivity", "Failed to read UMS enable state", ex);
        }
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.mUsbStateReceiver);
        if (this.mStorageManager == null && this.mStorageListener != null) {
            this.mStorageManager.unregisterListener(this.mStorageListener);
        }
    }

    private void handleUsbStateChanged(Intent intent) {
        if (!intent.getExtras().getBoolean("connected")) {
            finish();
        }
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        return service != null ? Stub.asInterface(service) : null;
    }

    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case 1:
                return new Builder(this).setTitle(17040440).setPositiveButton(17040443, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        UsbStorageActivity.this.switchUsbMassStorage(true);
                    }
                }).setNegativeButton(17039360, null).setMessage(17040441).setOnCancelListener(this).create();
            case 2:
                return new Builder(this).setTitle(17040442).setNeutralButton(17040443, null).setMessage(17040431).setOnCancelListener(this).create();
            default:
                return null;
        }
    }

    private void scheduleShowDialog(int id) {
        this.mUIHandler.post(new AnonymousClass_6(id));
    }

    private void switchUsbMassStorage(boolean on) {
        this.mUIHandler.post(new Runnable() {
            public void run() {
                UsbStorageActivity.this.mUnmountButton.setVisibility(8);
                UsbStorageActivity.this.mMountButton.setVisibility(8);
                UsbStorageActivity.this.mProgressBar.setVisibility(0);
            }
        });
        this.mAsyncStorageHandler.post(new AnonymousClass_8(on));
    }

    private void checkStorageUsers() {
        this.mAsyncStorageHandler.post(new Runnable() {
            public void run() {
                UsbStorageActivity.this.checkStorageUsersAsync();
            }
        });
    }

    private void checkStorageUsersAsync() {
        IMountService ims = getMountService();
        if (ims == null) {
            scheduleShowDialog(2);
        }
        boolean showDialog = false;
        try {
            int[] stUsers = ims.getStorageUsers(Environment.getExternalStorageDirectory().toString());
            if (stUsers == null || stUsers.length <= 0) {
                List<ApplicationInfo> infoList = ((ActivityManager) getSystemService("activity")).getRunningExternalApplications();
                if (infoList != null && infoList.size() > 0) {
                    showDialog = true;
                }
                if (showDialog) {
                    switchUsbMassStorage(true);
                } else {
                    scheduleShowDialog(1);
                }
            }
            showDialog = true;
            if (showDialog) {
                switchUsbMassStorage(true);
            } else {
                scheduleShowDialog(1);
            }
        } catch (RemoteException e) {
            scheduleShowDialog(2);
        }
    }

    public void onClick(View v) {
        if (v == this.mMountButton) {
            checkStorageUsers();
        } else if (v == this.mUnmountButton) {
            switchUsbMassStorage(false);
        }
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
