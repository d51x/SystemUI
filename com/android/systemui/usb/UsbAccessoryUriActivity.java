package com.android.systemui.usb;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController.AlertParams;

public class UsbAccessoryUriActivity extends AlertActivity implements OnClickListener {
    private UsbAccessory mAccessory;
    private Uri mUri;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        this.mAccessory = (UsbAccessory) intent.getParcelableExtra("accessory");
        String uriString = intent.getStringExtra("uri");
        this.mUri = uriString == null ? null : Uri.parse(uriString);
        if (this.mUri == null) {
            Log.e("UsbAccessoryUriActivity", "could not parse Uri " + uriString);
            finish();
            return;
        }
        String scheme = this.mUri.getScheme();
        if ("http".equals(scheme) || "https".equals(scheme)) {
            AlertParams ap = this.mAlertParams;
            ap.mTitle = this.mAccessory.getDescription();
            if (ap.mTitle == null || ap.mTitle.length() == 0) {
                ap.mTitle = getString(2131296292);
            }
            ap.mMessage = getString(2131296291, new Object[]{this.mUri});
            ap.mPositiveButtonText = getString(2131296293);
            ap.mNegativeButtonText = getString(17039360);
            ap.mPositiveButtonListener = this;
            ap.mNegativeButtonListener = this;
            setupAlert();
            return;
        }
        Log.e("UsbAccessoryUriActivity", "Uri not http or https: " + this.mUri);
        finish();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            Intent intent = new Intent("android.intent.action.VIEW", this.mUri);
            intent.addCategory("android.intent.category.BROWSABLE");
            intent.addFlags(268435456);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e("UsbAccessoryUriActivity", "startActivity failed for " + this.mUri);
            }
        }
        finish();
    }
}
