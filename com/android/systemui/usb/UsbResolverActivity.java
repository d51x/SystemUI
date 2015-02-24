package com.android.systemui.usb;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.IUsbManager.Stub;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.widget.CheckBox;
import com.android.internal.app.ResolverActivity;
import java.util.ArrayList;

public class UsbResolverActivity extends ResolverActivity {
    private UsbAccessory mAccessory;
    private UsbDevice mDevice;
    private UsbDisconnectedReceiver mDisconnectedReceiver;

    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Parcelable targetParcelable = intent.getParcelableExtra("android.intent.extra.INTENT");
        if (targetParcelable instanceof Intent) {
            Intent target = (Intent) targetParcelable;
            ArrayList<ResolveInfo> rList = intent.getParcelableArrayListExtra("rlist");
            super.onCreate(savedInstanceState, target, getResources().getText(17040333), null, rList, true);
            CheckBox alwaysUse = (CheckBox) findViewById(16908888);
            if (alwaysUse != null) {
                if (this.mDevice == null) {
                    alwaysUse.setText(2131296295);
                } else {
                    alwaysUse.setText(2131296294);
                }
            }
            this.mDevice = (UsbDevice) target.getParcelableExtra("device");
            if (this.mDevice != null) {
                this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mDevice);
                return;
            }
            this.mAccessory = (UsbAccessory) target.getParcelableExtra("accessory");
            if (this.mAccessory == null) {
                Log.e("UsbResolverActivity", "no device or accessory");
                finish();
                return;
            }
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mAccessory);
            return;
        }
        Log.w("UsbResolverActivity", "Target is not an intent: " + targetParcelable);
        finish();
    }

    protected void onDestroy() {
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        super.onDestroy();
    }

    protected void onIntentSelected(ResolveInfo ri, Intent intent, boolean alwaysCheck) {
        try {
            IUsbManager service = Stub.asInterface(ServiceManager.getService("usb"));
            int uid = ri.activityInfo.applicationInfo.uid;
            if (this.mDevice != null) {
                service.grantDevicePermission(this.mDevice, uid);
                if (alwaysCheck) {
                    service.setDevicePackage(this.mDevice, ri.activityInfo.packageName);
                } else {
                    service.setDevicePackage(this.mDevice, null);
                }
            } else if (this.mAccessory != null) {
                service.grantAccessoryPermission(this.mAccessory, uid);
                if (alwaysCheck) {
                    service.setAccessoryPackage(this.mAccessory, ri.activityInfo.packageName);
                } else {
                    service.setAccessoryPackage(this.mAccessory, null);
                }
            }
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e("UsbResolverActivity", "startActivity failed", e);
            }
        } catch (RemoteException e2) {
            Log.e("UsbResolverActivity", "onIntentSelected failed", e2);
        }
    }
}
