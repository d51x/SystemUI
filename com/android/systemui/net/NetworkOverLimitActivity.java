package com.android.systemui.net;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.net.INetworkPolicyManager.Stub;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

public class NetworkOverLimitActivity extends Activity {

    class AnonymousClass_1 implements OnClickListener {
        final /* synthetic */ NetworkTemplate val$template;

        AnonymousClass_1(NetworkTemplate networkTemplate) {
            this.val$template = networkTemplate;
        }

        public void onClick(DialogInterface dialog, int which) {
            NetworkOverLimitActivity.this.snoozePolicy(this.val$template);
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        NetworkTemplate template = (NetworkTemplate) getIntent().getParcelableExtra("android.net.NETWORK_TEMPLATE");
        Builder builder = new Builder(this);
        builder.setTitle(getLimitedDialogTitleForTemplate(template));
        builder.setMessage(2131296369);
        builder.setPositiveButton(17039370, null);
        builder.setNegativeButton(2131296370, new AnonymousClass_1(template));
        Dialog dialog = builder.create();
        dialog.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                NetworkOverLimitActivity.this.finish();
            }
        });
        dialog.getWindow().setType(2003);
        dialog.show();
    }

    private void snoozePolicy(NetworkTemplate template) {
        try {
            Stub.asInterface(ServiceManager.getService("netpolicy")).snoozeLimit(template);
        } catch (RemoteException e) {
            Slog.w("NetworkOverLimitActivity", "problem snoozing network policy", e);
        }
    }

    private static int getLimitedDialogTitleForTemplate(NetworkTemplate template) {
        switch (template.getMatchRule()) {
            case 1:
                return 2131296367;
            case 2:
                return 2131296365;
            case 3:
                return 2131296366;
            default:
                return 2131296368;
        }
    }
}
