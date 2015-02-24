package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class CloseDragHandle extends LinearLayout {
    PhoneStatusBar mService;

    public CloseDragHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != 0) {
            this.mService.interceptTouchEvent(event);
        }
        return true;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mService.interceptTouchEvent(event) ? true : super.onInterceptTouchEvent(event);
    }
}
