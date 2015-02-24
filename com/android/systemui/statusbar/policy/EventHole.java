package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.graphics.Region.Op;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.ViewTreeObserver.OnComputeInternalInsetsListener;

public class EventHole extends View implements OnComputeInternalInsetsListener {
    private int[] mLoc;
    private boolean mWindowVis;

    public EventHole(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EventHole(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.mLoc = new int[2];
    }

    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        this.mWindowVis = visibility == 0;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnComputeInternalInsetsListener(this);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnComputeInternalInsetsListener(this);
    }

    public void onComputeInternalInsets(InternalInsetsInfo info) {
        boolean visible;
        if (!isShown() || !this.mWindowVis || getWidth() <= 0 || getHeight() <= 0) {
            visible = false;
        } else {
            visible = true;
        }
        int[] loc = this.mLoc;
        getLocationInWindow(loc);
        int l = loc[0];
        int r = l + getWidth();
        int t = loc[1];
        int b = t + getHeight();
        View top = this;
        while (top.getParent() instanceof View) {
            top = top.getParent();
        }
        if (visible) {
            info.setTouchableInsets(3);
            info.touchableRegion.set(0, 0, top.getWidth(), top.getHeight());
            info.touchableRegion.op(l, t, r, b, Op.DIFFERENCE);
            return;
        }
        info.setTouchableInsets(0);
    }
}
