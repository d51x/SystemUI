package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

public class BatteryController extends BroadcastReceiver {
    private Context mContext;
    private ArrayList<ImageView> mIconViews;
    private ArrayList<TextView> mLabelViews;

    public BatteryController(Context context) {
        this.mIconViews = new ArrayList();
        this.mLabelViews = new ArrayList();
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        context.registerReceiver(this, filter);
    }

    public void addIconView(ImageView v) {
        this.mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        this.mLabelViews.add(v);
    }

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BATTERY_CHANGED")) {
            boolean plugged;
            int i;
            int level = intent.getIntExtra("level", 0);
            if (intent.getIntExtra("plugged", 0) != 0) {
                plugged = true;
            } else {
                plugged = false;
            }
            int icon = plugged ? 2130837614 : 2130837605;
            int N = this.mIconViews.size();
            for (i = 0; i < N; i++) {
                ImageView v = (ImageView) this.mIconViews.get(i);
                v.setImageResource(icon);
                v.setImageLevel(level);
                v.setContentDescription(this.mContext.getString(2131296355, new Object[]{Integer.valueOf(level)}));
            }
            N = this.mLabelViews.size();
            for (i = 0; i < N; i++) {
                ((TextView) this.mLabelViews.get(i)).setText(this.mContext.getString(2131296282, new Object[]{Integer.valueOf(level)}));
            }
        }
    }
}
