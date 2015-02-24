package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import com.android.systemui.SwipeHelper;
import com.android.systemui.SwipeHelper.Callback;
import com.android.systemui.statusbar.BaseStatusBar;

public class IntruderAlertView extends LinearLayout implements Callback {
    BaseStatusBar mBar;
    private ViewGroup mContentHolder;
    private RemoteViews mIntruderRemoteViews;
    private OnClickListener mOnClickListener;
    private SwipeHelper mSwipeHelper;
    Rect mTmpRect;

    public IntruderAlertView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IntruderAlertView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mTmpRect = new Rect();
        setOrientation(1);
    }

    public void onAttachedToWindow() {
        this.mSwipeHelper = new SwipeHelper(0, this, getResources().getDisplayMetrics().density, (float) ViewConfiguration.get(getContext()).getScaledPagingTouchSlop());
        this.mContentHolder = (ViewGroup) findViewById(2131492878);
        if (this.mIntruderRemoteViews != null) {
            applyIntruderContent(this.mIntruderRemoteViews, this.mOnClickListener);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mSwipeHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    public boolean canChildBeDismissed(View v) {
        return true;
    }

    public void onChildDismissed(View v) {
        Slog.v("IntruderAlertView", "User swiped intruder to dismiss");
        this.mBar.dismissIntruder();
    }

    public void onBeginDrag(View v) {
    }

    public void onDragCancelled(View v) {
        this.mContentHolder.setAlpha(1.0f);
    }

    public View getChildAtPosition(MotionEvent ev) {
        return this.mContentHolder;
    }

    public View getChildContentView(View v) {
        return v;
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mSwipeHelper.setDensityScale(getResources().getDisplayMetrics().density);
        this.mSwipeHelper.setPagingTouchSlop((float) ViewConfiguration.get(getContext()).getScaledPagingTouchSlop());
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
    }

    public void applyIntruderContent(RemoteViews intruderView, OnClickListener listener) {
        this.mIntruderRemoteViews = intruderView;
        this.mOnClickListener = listener;
        if (this.mContentHolder != null) {
            this.mContentHolder.setX(0.0f);
            this.mContentHolder.setVisibility(0);
            this.mContentHolder.setAlpha(1.0f);
            this.mContentHolder.removeAllViews();
            View content = intruderView.apply(getContext(), this.mContentHolder);
            if (listener != null) {
                content.setOnClickListener(listener);
                Drawable bg = getResources().getDrawable(2130837574);
                if (bg == null) {
                    Log.e("IntruderAlertView", String.format("Can't find background drawable id=0x%08x", new Object[]{Integer.valueOf(2130837574)}));
                } else {
                    content.setBackgroundDrawable(bg);
                }
            }
            this.mContentHolder.addView(content);
        }
    }
}
