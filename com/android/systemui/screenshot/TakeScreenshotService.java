package com.android.systemui.screenshot;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class TakeScreenshotService extends Service {
    private static GlobalScreenshot mScreenshot;
    private Handler mHandler;

    public TakeScreenshotService() {
        this.mHandler = new Handler() {

            class AnonymousClass_1 implements Runnable {
                final /* synthetic */ Messenger val$callback;

                AnonymousClass_1(Messenger messenger) {
                    this.val$callback = messenger;
                }

                public void run() {
                    try {
                        this.val$callback.send(Message.obtain(null, 1));
                    } catch (RemoteException e) {
                    }
                }
            }

            public void handleMessage(Message msg) {
                boolean z = true;
                switch (msg.what) {
                    case 1:
                        Messenger callback = msg.replyTo;
                        if (mScreenshot == null) {
                            mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
                        }
                        GlobalScreenshot access$000 = mScreenshot;
                        Runnable anonymousClass_1 = new AnonymousClass_1(callback);
                        boolean z2 = msg.arg1 > 0;
                        if (msg.arg2 <= 0) {
                            z = false;
                        }
                        access$000.takeScreenshot(anonymousClass_1, z2, z);
                    default:
                        break;
                }
            }
        };
    }

    public IBinder onBind(Intent intent) {
        return new Messenger(this.mHandler).getBinder();
    }
}
