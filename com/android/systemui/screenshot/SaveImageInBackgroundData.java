package com.android.systemui.screenshot;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

// compiled from: GlobalScreenshot.java
class SaveImageInBackgroundData {
    Context context;
    Runnable finisher;
    int iconSize;
    Bitmap image;
    Uri imageUri;
    int result;

    SaveImageInBackgroundData() {
    }
}
