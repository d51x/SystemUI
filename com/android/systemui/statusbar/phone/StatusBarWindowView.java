package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import com.android.systemui.ExpandHelper;
import com.android.systemui.statusbar.policy.NotificationRowLayout;

public class StatusBarWindowView extends FrameLayout {
    private NotificationRowLayout latestItems;
    private ExpandHelper mExpandHelper;
    PhoneStatusBar mService;

    public StatusBarWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMotionEventSplittingEnabled(false);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.latestItems = (NotificationRowLayout) findViewById(2131492929);
        ScrollView scrollView = (ScrollView) findViewById(2131492928);
        this.mExpandHelper = new ExpandHelper(this.mContext, this.latestItems, getResources().getDimensionPixelSize(2131427345), getResources().getDimensionPixelSize(2131427346));
        this.mExpandHelper.setEventSource(this);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == 0;
        switch (event.getKeyCode()) {
            case 4:
                if (down) {
                    return true;
                }
                this.mService.animateCollapse();
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        MotionEvent cancellation = MotionEvent.obtain(ev);
        cancellation.setAction(3);
        boolean intercept = this.mExpandHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
        if (intercept) {
            this.latestItems.onInterceptTouchEvent(cancellation);
        }
        return intercept;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mExpandHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
    }
}
