package com.android.systemui.statusbar.tablet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class NotificationPeekPanel extends RelativeLayout implements StatusBarPanel {
    TabletStatusBar mBar;

    public NotificationPeekPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationPeekPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean isInContentArea(int x, int y) {
        return x >= getPaddingLeft() && x < getWidth() - getPaddingRight() && y >= getPaddingTop() && y < getHeight() - getPaddingBottom();
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        this.mBar.resetNotificationPeekFadeTimer();
        return false;
    }

    public boolean dispatchHoverEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        return (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) ? true : super.dispatchHoverEvent(event);
    }
}
