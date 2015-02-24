package com.android.systemui.statusbar.tablet;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DelegateViewHelper;

public class TabletStatusBarView extends FrameLayout {
    private final int MAX_PANELS;
    private DelegateViewHelper mDelegateHelper;
    private Handler mHandler;
    private final View[] mIgnoreChildren;
    private final View[] mPanels;
    private final int[] mPos;

    public TabletStatusBarView(Context context) {
        this(context, null);
    }

    public TabletStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.MAX_PANELS = 5;
        this.mIgnoreChildren = new View[5];
        this.mPanels = new View[5];
        this.mPos = new int[2];
        this.mDelegateHelper = new DelegateViewHelper(this);
    }

    public void setDelegateView(View view) {
        this.mDelegateHelper.setDelegateView(view);
    }

    public void setBar(BaseStatusBar phoneStatusBar) {
        this.mDelegateHelper.setBar(phoneStatusBar);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mDelegateHelper != null) {
            this.mDelegateHelper.onInterceptTouchEvent(event);
        }
        return true;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        View view = findViewById(2131492963);
        if (view == null) {
            view = findViewById(2131492880);
        }
        this.mDelegateHelper.setSourceView(view);
        this.mDelegateHelper.setInitialTouchRegion(view);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mHandler.removeMessages(1001);
            this.mHandler.sendEmptyMessage(1001);
            this.mHandler.removeMessages(1041);
            this.mHandler.sendEmptyMessage(1041);
            this.mHandler.removeMessages(2000);
            this.mHandler.sendEmptyMessage(2000);
            int i = 0;
            while (i < this.mPanels.length) {
                if (this.mPanels[i] != null && this.mPanels[i].getVisibility() == 0 && eventInside(this.mIgnoreChildren[i], ev)) {
                    return true;
                }
                i++;
            }
        }
        return (this.mDelegateHelper == null || !this.mDelegateHelper.onInterceptTouchEvent(ev)) ? super.onInterceptTouchEvent(ev) : true;
    }

    private boolean eventInside(View v, MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        int[] p = this.mPos;
        v.getLocationInWindow(p);
        return x >= p[0] && x < p[0] + v.getWidth() && y >= p[1] && y < p[1] + v.getHeight();
    }

    public void setHandler(Handler h) {
        this.mHandler = h;
    }

    public void setIgnoreChildren(int index, View ignore, View panel) {
        this.mIgnoreChildren[index] = ignore;
        this.mPanels[index] = panel;
    }
}
