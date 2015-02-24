package com.android.systemui.usb;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.util.Slog;
import com.android.internal.app.ExternalMediaFormatActivity;

public class StorageNotification extends StorageEventListener {
    private Handler mAsyncEventHandler;
    private Context mContext;
    private Notification mMediaStorageNotification;
    private StorageManager mStorageManager;
    private boolean mUmsAvailable;
    private Notification mUsbStorageNotification;

    class AnonymousClass_1 implements Runnable {
        final /* synthetic */ boolean val$connected;

        AnonymousClass_1(boolean z) {
            this.val$connected = z;
        }

        public void run() {
            StorageNotification.this.onUsbMassStorageConnectionChangedAsync(this.val$connected);
        }
    }

    class AnonymousClass_2 implements Runnable {
        final /* synthetic */ String val$newState;
        final /* synthetic */ String val$oldState;
        final /* synthetic */ String val$path;

        AnonymousClass_2(String str, String str2, String str3) {
            this.val$path = str;
            this.val$oldState = str2;
            this.val$newState = str3;
        }

        public void run() {
            StorageNotification.this.onStorageStateChangedAsync(this.val$path, this.val$oldState, this.val$newState);
        }
    }

    public StorageNotification(Context context) {
        this.mContext = context;
        this.mStorageManager = (StorageManager) context.getSystemService("storage");
        boolean connected = this.mStorageManager.isUsbMassStorageConnected();
        Slog.d("StorageNotification", String.format("Startup with UMS connection %s (media state %s)", new Object[]{Boolean.valueOf(this.mUmsAvailable), Environment.getExternalStorageState()}));
        HandlerThread thr = new HandlerThread("SystemUI StorageNotification");
        thr.start();
        this.mAsyncEventHandler = new Handler(thr.getLooper());
        onUsbMassStorageConnectionChanged(connected);
    }

    public void onUsbMassStorageConnectionChanged(boolean connected) {
        this.mAsyncEventHandler.post(new AnonymousClass_1(connected));
    }

    private void onUsbMassStorageConnectionChangedAsync(boolean connected) {
        this.mUmsAvailable = connected;
        String st = Environment.getExternalStorageState();
        Slog.i("StorageNotification", String.format("UMS connection changed to %s (media state %s)", new Object[]{Boolean.valueOf(connected), st}));
        if (connected) {
            if (st.equals("removed") || st.equals("checking")) {
                connected = false;
            }
        }
        updateUsbMassStorageNotification(connected);
    }

    public void onStorageStateChanged(String path, String oldState, String newState) {
        this.mAsyncEventHandler.post(new AnonymousClass_2(path, oldState, newState));
    }

    private void onStorageStateChangedAsync(String str, String str2, String str3) {
        Slog.i("StorageNotification", String.format("Media {%s} state changed from {%s} -> {%s}", new Object[]{str, str2, str3}));
        Intent intent;
        if (str3.equals("shared")) {
            intent = new Intent();
            intent.setClass(this.mContext, UsbStorageActivity.class);
            setUsbStorageNotification(17040434, 17040435, 17301642, false, true, PendingIntent.getActivity(this.mContext, 0, intent, 0));
        } else if (str3.equals("checking")) {
            setMediaStorageNotification(17040464, 17040465, 17301675, true, false, null);
            updateUsbMassStorageNotification(false);
        } else if (str3.equals("mounted")) {
            setMediaStorageNotification(0, 0, 0, false, false, null);
            updateUsbMassStorageNotification(this.mUmsAvailable);
        } else if (str3.equals("unmounted")) {
            if (this.mStorageManager.isUsbMassStorageEnabled()) {
                setMediaStorageNotification(0, 0, 0, false, false, null);
                updateUsbMassStorageNotification(false);
            } else if (str2.equals("shared")) {
                setMediaStorageNotification(0, 0, 0, false, false, null);
                updateUsbMassStorageNotification(this.mUmsAvailable);
            } else {
                if (Environment.isExternalStorageRemovable()) {
                    setMediaStorageNotification(17040472, 17040473, 17301626, true, true, null);
                } else {
                    setMediaStorageNotification(0, 0, 0, false, false, null);
                }
                updateUsbMassStorageNotification(this.mUmsAvailable);
            }
        } else if (str3.equals("nofs")) {
            intent = new Intent();
            intent.setClass(this.mContext, ExternalMediaFormatActivity.class);
            setMediaStorageNotification(17040466, 17040467, 17301627, true, false, PendingIntent.getActivity(this.mContext, 0, intent, 0));
            updateUsbMassStorageNotification(this.mUmsAvailable);
        } else if (str3.equals("unmountable")) {
            intent = new Intent();
            intent.setClass(this.mContext, ExternalMediaFormatActivity.class);
            setMediaStorageNotification(17040468, 17040469, 17301627, true, false, PendingIntent.getActivity(this.mContext, 0, intent, 0));
            updateUsbMassStorageNotification(this.mUmsAvailable);
        } else if (str3.equals("removed")) {
            setMediaStorageNotification(0, 0, 0, false, false, null);
            updateUsbMassStorageNotification(false);
        } else if (str3.equals("bad_removal")) {
            setMediaStorageNotification(0, 0, 0, false, false, null);
            updateUsbMassStorageNotification(false);
        } else {
            Slog.w("StorageNotification", String.format("Ignoring unknown state {%s}", new Object[]{str3}));
        }
    }

    void updateUsbMassStorageNotification(boolean z) {
        if (z) {
            Intent intent = new Intent();
            intent.setClass(this.mContext, UsbStorageActivity.class);
            intent.setFlags(268435456);
            setUsbStorageNotification(17040432, 17040433, 17302828, false, true, PendingIntent.getActivity(this.mContext, 0, intent, 0));
            return;
        }
        setUsbStorageNotification(0, 0, 0, false, false, null);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void setUsbStorageNotification(int r13_titleId, int r14_messageId, int r15_icon, boolean r16_sound, boolean r17_visible, android.app.PendingIntent r18_pi) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.usb.StorageNotification.setUsbStorageNotification(int, int, int, boolean, boolean, android.app.PendingIntent):void");
        /*
        this = this;
        monitor-enter(r12);
        if (r17 != 0) goto L_0x0009;
    L_0x0003:
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        if (r8 != 0) goto L_0x0009;
    L_0x0007:
        monitor-exit(r12);
        return;
    L_0x0009:
        r8 = r12.mContext;	 Catch:{ all -> 0x0088 }
        r9 = "notification";
        r5 = r8.getSystemService(r9);	 Catch:{ all -> 0x0088 }
        r5 = (android.app.NotificationManager) r5;	 Catch:{ all -> 0x0088 }
        if (r5 == 0) goto L_0x0007;
    L_0x0015:
        if (r17 == 0) goto L_0x007c;
    L_0x0017:
        r6 = android.content.res.Resources.getSystem();	 Catch:{ all -> 0x0088 }
        r7 = r6.getText(r13);	 Catch:{ all -> 0x0088 }
        r3 = r6.getText(r14);	 Catch:{ all -> 0x0088 }
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        if (r8 != 0) goto L_0x0038;
    L_0x0027:
        r8 = new android.app.Notification;	 Catch:{ all -> 0x0088 }
        r8.<init>();	 Catch:{ all -> 0x0088 }
        r12.mUsbStorageNotification = r8;	 Catch:{ all -> 0x0088 }
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r8.icon = r15;	 Catch:{ all -> 0x0088 }
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r9 = 0;
        r8.when = r9;	 Catch:{ all -> 0x0088 }
    L_0x0038:
        if (r16 == 0) goto L_0x008b;
    L_0x003a:
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r9 = r8.defaults;	 Catch:{ all -> 0x0088 }
        r9 = r9 | 1;
        r8.defaults = r9;	 Catch:{ all -> 0x0088 }
    L_0x0042:
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r9 = 2;
        r8.flags = r9;	 Catch:{ all -> 0x0088 }
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r8.tickerText = r7;	 Catch:{ all -> 0x0088 }
        if (r18 != 0) goto L_0x005a;
    L_0x004d:
        r2 = new android.content.Intent;	 Catch:{ all -> 0x0088 }
        r2.<init>();	 Catch:{ all -> 0x0088 }
        r8 = r12.mContext;	 Catch:{ all -> 0x0088 }
        r9 = 0;
        r10 = 0;
        r18 = android.app.PendingIntent.getBroadcast(r8, r9, r2, r10);	 Catch:{ all -> 0x0088 }
    L_0x005a:
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r9 = r12.mContext;	 Catch:{ all -> 0x0088 }
        r0 = r18;
        r8.setLatestEventInfo(r9, r7, r3, r0);	 Catch:{ all -> 0x0088 }
        r8 = 1;
        r9 = r12.mContext;	 Catch:{ all -> 0x0088 }
        r9 = r9.getContentResolver();	 Catch:{ all -> 0x0088 }
        r10 = "adb_enabled";
        r11 = 0;
        r9 = android.provider.Settings.Secure.getInt(r9, r10, r11);	 Catch:{ all -> 0x0088 }
        if (r8 != r9) goto L_0x0094;
    L_0x0073:
        r1 = 1;
    L_0x0074:
        if (r1 != 0) goto L_0x007c;
    L_0x0076:
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r0 = r18;
        r8.fullScreenIntent = r0;	 Catch:{ all -> 0x0088 }
    L_0x007c:
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r4 = r8.icon;	 Catch:{ all -> 0x0088 }
        if (r17 == 0) goto L_0x0096;
    L_0x0082:
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r5.notify(r4, r8);	 Catch:{ all -> 0x0088 }
        goto L_0x0007;
    L_0x0088:
        r8 = move-exception;
        monitor-exit(r12);
        throw r8;
    L_0x008b:
        r8 = r12.mUsbStorageNotification;	 Catch:{ all -> 0x0088 }
        r9 = r8.defaults;	 Catch:{ all -> 0x0088 }
        r9 = r9 & -2;
        r8.defaults = r9;	 Catch:{ all -> 0x0088 }
        goto L_0x0042;
    L_0x0094:
        r1 = 0;
        goto L_0x0074;
    L_0x0096:
        r5.cancel(r4);	 Catch:{ all -> 0x0088 }
        goto L_0x0007;
        */
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void setMediaStorageNotification(int r10_titleId, int r11_messageId, int r12_icon, boolean r13_visible, boolean r14_dismissable, android.app.PendingIntent r15_pi) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.usb.StorageNotification.setMediaStorageNotification(int, int, int, boolean, boolean, android.app.PendingIntent):void");
        /*
        this = this;
        monitor-enter(r9);
        if (r13 != 0) goto L_0x0009;
    L_0x0003:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        if (r6 != 0) goto L_0x0009;
    L_0x0007:
        monitor-exit(r9);
        return;
    L_0x0009:
        r6 = r9.mContext;	 Catch:{ all -> 0x007b }
        r7 = "notification";
        r3 = r6.getSystemService(r7);	 Catch:{ all -> 0x007b }
        r3 = (android.app.NotificationManager) r3;	 Catch:{ all -> 0x007b }
        if (r3 == 0) goto L_0x0007;
    L_0x0015:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        if (r6 == 0) goto L_0x0022;
    L_0x0019:
        if (r13 == 0) goto L_0x0022;
    L_0x001b:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r2 = r6.icon;	 Catch:{ all -> 0x007b }
        r3.cancel(r2);	 Catch:{ all -> 0x007b }
    L_0x0022:
        if (r13 == 0) goto L_0x006f;
    L_0x0024:
        r4 = android.content.res.Resources.getSystem();	 Catch:{ all -> 0x007b }
        r5 = r4.getText(r10);	 Catch:{ all -> 0x007b }
        r1 = r4.getText(r11);	 Catch:{ all -> 0x007b }
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        if (r6 != 0) goto L_0x0041;
    L_0x0034:
        r6 = new android.app.Notification;	 Catch:{ all -> 0x007b }
        r6.<init>();	 Catch:{ all -> 0x007b }
        r9.mMediaStorageNotification = r6;	 Catch:{ all -> 0x007b }
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r7 = 0;
        r6.when = r7;	 Catch:{ all -> 0x007b }
    L_0x0041:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r7 = r6.defaults;	 Catch:{ all -> 0x007b }
        r7 = r7 & -2;
        r6.defaults = r7;	 Catch:{ all -> 0x007b }
        if (r14 == 0) goto L_0x007e;
    L_0x004b:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r7 = 16;
        r6.flags = r7;	 Catch:{ all -> 0x007b }
    L_0x0051:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r6.tickerText = r5;	 Catch:{ all -> 0x007b }
        if (r15 != 0) goto L_0x0064;
    L_0x0057:
        r0 = new android.content.Intent;	 Catch:{ all -> 0x007b }
        r0.<init>();	 Catch:{ all -> 0x007b }
        r6 = r9.mContext;	 Catch:{ all -> 0x007b }
        r7 = 0;
        r8 = 0;
        r15 = android.app.PendingIntent.getBroadcast(r6, r7, r0, r8);	 Catch:{ all -> 0x007b }
    L_0x0064:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r6.icon = r12;	 Catch:{ all -> 0x007b }
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r7 = r9.mContext;	 Catch:{ all -> 0x007b }
        r6.setLatestEventInfo(r7, r5, r1, r15);	 Catch:{ all -> 0x007b }
    L_0x006f:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r2 = r6.icon;	 Catch:{ all -> 0x007b }
        if (r13 == 0) goto L_0x0084;
    L_0x0075:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r3.notify(r2, r6);	 Catch:{ all -> 0x007b }
        goto L_0x0007;
    L_0x007b:
        r6 = move-exception;
        monitor-exit(r9);
        throw r6;
    L_0x007e:
        r6 = r9.mMediaStorageNotification;	 Catch:{ all -> 0x007b }
        r7 = 2;
        r6.flags = r7;	 Catch:{ all -> 0x007b }
        goto L_0x0051;
    L_0x0084:
        r3.cancel(r2);	 Catch:{ all -> 0x007b }
        goto L_0x0007;
        */
    }
}
