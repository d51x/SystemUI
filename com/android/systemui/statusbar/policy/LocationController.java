package com.android.systemui.statusbar.policy;

import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public class LocationController extends BroadcastReceiver {
    private Context mContext;
    private INotificationManager mNotificationService;

    public LocationController(Context context) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.GPS_ENABLED_CHANGE");
        filter.addAction("android.location.GPS_FIX_CHANGE");
        context.registerReceiver(this, filter);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        this.mNotificationService = NotificationManager.getService();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onReceive(android.content.Context r14_context, android.content.Intent r15_intent) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.LocationController.onReceive(android.content.Context, android.content.Intent):void");
        /*
        this = this;
        r1 = 0;
        r6 = r15.getAction();
        r0 = "enabled";
        r7 = r15.getBooleanExtra(r0, r1);
        r0 = "android.location.GPS_FIX_CHANGE";
        r0 = r6.equals(r0);
        if (r0 == 0) goto L_0x006e;
    L_0x0013:
        if (r7 == 0) goto L_0x006e;
    L_0x0015:
        r9 = 17302838; // 0x1080536 float:2.4982994E-38 double:8.548738E-317;
        r11 = 2131296374; // 0x7f090076 float:1.8210663E38 double:1.0530003195E-314;
        r12 = 1;
    L_0x001c:
        if (r12 == 0) goto L_0x0084;
    L_0x001e:
        r8 = new android.content.Intent;	 Catch:{ RemoteException -> 0x0093 }
        r0 = "android.settings.LOCATION_SOURCE_SETTINGS";
        r8.<init>(r0);	 Catch:{ RemoteException -> 0x0093 }
        r0 = 268435456; // 0x10000000 float:2.5243549E-29 double:1.32624737E-315;
        r8.setFlags(r0);	 Catch:{ RemoteException -> 0x0093 }
        r0 = 0;
        r1 = 0;
        r10 = android.app.PendingIntent.getActivity(r14, r0, r8, r1);	 Catch:{ RemoteException -> 0x0093 }
        r0 = new android.app.Notification$Builder;	 Catch:{ RemoteException -> 0x0093 }
        r1 = r13.mContext;	 Catch:{ RemoteException -> 0x0093 }
        r0.<init>(r1);	 Catch:{ RemoteException -> 0x0093 }
        r0 = r0.setSmallIcon(r9);	 Catch:{ RemoteException -> 0x0093 }
        r1 = r13.mContext;	 Catch:{ RemoteException -> 0x0093 }
        r1 = r1.getText(r11);	 Catch:{ RemoteException -> 0x0093 }
        r0 = r0.setContentTitle(r1);	 Catch:{ RemoteException -> 0x0093 }
        r1 = 1;
        r0 = r0.setOngoing(r1);	 Catch:{ RemoteException -> 0x0093 }
        r0 = r0.setContentIntent(r10);	 Catch:{ RemoteException -> 0x0093 }
        r4 = r0.getNotification();	 Catch:{ RemoteException -> 0x0093 }
        r0 = 0;
        r4.tickerView = r0;	 Catch:{ RemoteException -> 0x0093 }
        r0 = 0;
        r4.tickerText = r0;	 Catch:{ RemoteException -> 0x0093 }
        r0 = 1;
        r4.priority = r0;	 Catch:{ RemoteException -> 0x0093 }
        r0 = 1;
        r5 = new int[r0];	 Catch:{ RemoteException -> 0x0093 }
        r0 = r13.mNotificationService;	 Catch:{ RemoteException -> 0x0093 }
        r1 = r13.mContext;	 Catch:{ RemoteException -> 0x0093 }
        r1 = r1.getPackageName();	 Catch:{ RemoteException -> 0x0093 }
        r2 = 0;
        r3 = 252119; // 0x3d8d7 float:3.53294E-40 double:1.245633E-318;
        r0.enqueueNotificationWithTag(r1, r2, r3, r4, r5);	 Catch:{ RemoteException -> 0x0093 }
    L_0x006d:
        return;
    L_0x006e:
        r0 = "android.location.GPS_ENABLED_CHANGE";
        r0 = r6.equals(r0);
        if (r0 == 0) goto L_0x007c;
    L_0x0076:
        if (r7 != 0) goto L_0x007c;
    L_0x0078:
        r12 = 0;
        r11 = 0;
        r9 = r11;
        goto L_0x001c;
    L_0x007c:
        r9 = 2130837649; // 0x7f020091 float:1.7280258E38 double:1.052773679E-314;
        r11 = 2131296373; // 0x7f090075 float:1.821066E38 double:1.053000319E-314;
        r12 = 1;
        goto L_0x001c;
    L_0x0084:
        r0 = r13.mNotificationService;	 Catch:{ RemoteException -> 0x0093 }
        r1 = r13.mContext;	 Catch:{ RemoteException -> 0x0093 }
        r1 = r1.getPackageName();	 Catch:{ RemoteException -> 0x0093 }
        r2 = 252119; // 0x3d8d7 float:3.53294E-40 double:1.245633E-318;
        r0.cancelNotification(r1, r2);	 Catch:{ RemoteException -> 0x0093 }
        goto L_0x006d;
    L_0x0093:
        r0 = move-exception;
        goto L_0x006d;
        */
    }
}
