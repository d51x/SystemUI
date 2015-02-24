package com.android.systemui.statusbar;

import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class DelegateViewHelper {
    private BaseStatusBar mBar;
    private View mDelegateView;
    private float[] mDownPoint;
    RectF mInitialTouch;
    private boolean mPanelShowing;
    private View mSourceView;
    private boolean mStarted;
    private boolean mSwapXY;
    private int[] mTempPoint;
    private float mTriggerThreshhold;

    public DelegateViewHelper(View sourceView) {
        this.mTempPoint = new int[2];
        this.mDownPoint = new float[2];
        this.mInitialTouch = new RectF();
        this.mSwapXY = false;
        setSourceView(sourceView);
    }

    public void setDelegateView(View view) {
        this.mDelegateView = view;
    }

    public void setBar(BaseStatusBar phoneStatusBar) {
        this.mBar = phoneStatusBar;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mSourceView == null || this.mDelegateView == null || this.mBar.shouldDisableNavbarGestures() || this.mBar.inKeyguardRestrictedInputMode()) {
            return false;
        }
        this.mSourceView.getLocationOnScreen(this.mTempPoint);
        float sourceX = (float) this.mTempPoint[0];
        float sourceY = (float) this.mTempPoint[1];
        switch (event.getAction()) {
            case 0:
                this.mPanelShowing = this.mDelegateView.getVisibility() == 0;
                this.mDownPoint[0] = event.getX();
                this.mDownPoint[1] = event.getY();
                this.mStarted = this.mInitialTouch.contains(this.mDownPoint[0] + sourceX, this.mDownPoint[1] + sourceY);
                break;
        }
        if (!this.mStarted) {
            return false;
        }
        if (!this.mPanelShowing && event.getAction() == 2) {
            int historySize = event.getHistorySize();
            int k = 0;
            while (k < historySize + 1) {
                if ((this.mSwapXY ? this.mDownPoint[0] - (k < historySize ? event.getHistoricalX(k) : event.getX()) : this.mDownPoint[1] - (k < historySize ? event.getHistoricalY(k) : event.getY())) > this.mTriggerThreshhold) {
                    this.mBar.showSearchPanel();
                    this.mPanelShowing = true;
                } else {
                    k++;
                }
            }
        }
        this.mDelegateView.getLocationOnScreen(this.mTempPoint);
        float deltaX = sourceX - ((float) this.mTempPoint[0]);
        float deltaY = sourceY - ((float) this.mTempPoint[1]);
        event.offsetLocation(deltaX, deltaY);
        this.mDelegateView.dispatchTouchEvent(event);
        event.offsetLocation(-deltaX, -deltaY);
        return this.mPanelShowing;
    }

    public void setSourceView(View view) {
        this.mSourceView = view;
        if (this.mSourceView != null) {
            this.mTriggerThreshhold = this.mSourceView.getContext().getResources().getDimension(2131427367);
        }
    }

    public void setInitialTouchRegion(View... views) {
        RectF bounds = new RectF();
        int[] p = new int[2];
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            if (view != null) {
                view.getLocationOnScreen(p);
                if (i == 0) {
                    bounds.set((float) p[0], (float) p[1], (float) (p[0] + view.getWidth()), (float) (p[1] + view.getHeight()));
                } else {
                    bounds.union((float) p[0], (float) p[1], (float) (p[0] + view.getWidth()), (float) (p[1] + view.getHeight()));
                }
            }
        }
        this.mInitialTouch.set(bounds);
    }
}
