package com.android.systemui.statusbar.tablet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class NotificationIconArea extends RelativeLayout {
    IconLayout mIconLayout;

    static class IconLayout extends LinearLayout {
        public IconLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public boolean onInterceptTouchEvent(MotionEvent e) {
            return true;
        }
    }

    public NotificationIconArea(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIconLayout = (IconLayout) findViewById(2131492901);
    }
}
