package com.android.systemui.recent;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class StatusBarTouchProxy extends FrameLayout {
    private View mStatusBar;

    public StatusBarTouchProxy(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setStatusBar(View statusBar) {
        this.mStatusBar = statusBar;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return this.mStatusBar.dispatchTouchEvent(event);
    }
}
