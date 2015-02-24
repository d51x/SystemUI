package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;
import java.util.Date;

public final class DateView extends TextView {
    private boolean mAttachedToWindow;
    private BroadcastReceiver mIntentReceiver;
    private boolean mUpdating;
    private boolean mWindowVisible;

    public DateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.TIME_TICK".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                    DateView.this.updateClock();
                }
            }
        };
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttachedToWindow = true;
        setUpdates();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAttachedToWindow = false;
        setUpdates();
    }

    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        this.mWindowVisible = visibility == 0;
        setUpdates();
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        setUpdates();
    }

    protected int getSuggestedMinimumWidth() {
        return 0;
    }

    private final void updateClock() {
        Context context = getContext();
        Date now = new Date();
        CharSequence dow = DateFormat.format("EEEE", now);
        CharSequence date = DateFormat.getLongDateFormat(context).format(now);
        setText(context.getString(2131296258, new Object[]{dow, date}));
    }

    private boolean isVisible() {
        View v = this;
        while (v.getVisibility() == 0) {
            ViewParent parent = v.getParent();
            if (!(parent instanceof View)) {
                return true;
            }
            v = (View) parent;
        }
        return false;
    }

    private void setUpdates() {
        boolean update = this.mAttachedToWindow && this.mWindowVisible && isVisible();
        if (update != this.mUpdating) {
            this.mUpdating = update;
            if (update) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.TIME_TICK");
                filter.addAction("android.intent.action.TIME_SET");
                filter.addAction("android.intent.action.TIMEZONE_CHANGED");
                this.mContext.registerReceiver(this.mIntentReceiver, filter, null, null);
                updateClock();
                return;
            }
            this.mContext.unregisterReceiver(this.mIntentReceiver);
        }
    }
}
