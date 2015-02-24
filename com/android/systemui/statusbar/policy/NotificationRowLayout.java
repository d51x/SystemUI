package com.android.systemui.statusbar.policy;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import com.android.systemui.ExpandHelper.Callback;
import com.android.systemui.SwipeHelper;
import com.android.systemui.statusbar.NotificationData;
import java.util.HashMap;

public class NotificationRowLayout extends LinearLayout implements Callback, SwipeHelper.Callback {
    boolean mAnimateBounds;
    HashMap<View, ValueAnimator> mAppearingViews;
    HashMap<View, ValueAnimator> mDisappearingViews;
    private OnSizeChangedListener mOnSizeChangedListener;
    private LayoutTransition mRealLayoutTransition;
    boolean mRemoveViews;
    private SwipeHelper mSwipeHelper;
    Rect mTmpRect;

    public NotificationRowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationRowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAnimateBounds = true;
        this.mTmpRect = new Rect();
        this.mAppearingViews = new HashMap();
        this.mDisappearingViews = new HashMap();
        this.mRemoveViews = true;
        this.mRealLayoutTransition = new LayoutTransition();
        setLayoutTransitionsEnabled(true);
        setOrientation(1);
        this.mSwipeHelper = new SwipeHelper(0, this, getResources().getDisplayMetrics().density, (float) ViewConfiguration.get(this.mContext).getScaledPagingTouchSlop());
    }

    public void setLongPressListener(OnLongClickListener listener) {
        this.mSwipeHelper.setLongPressListener(listener);
    }

    public void setOnSizeChangedListener(OnSizeChangedListener l) {
        this.mOnSizeChangedListener = l;
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            this.mSwipeHelper.removeLongPressCallback();
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mSwipeHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    public boolean canChildBeDismissed(View v) {
        View veto = v.findViewById(2131492937);
        return (veto == null || veto.getVisibility() == 8) ? false : true;
    }

    public boolean canChildBeExpanded(View v) {
        return NotificationData.getIsExpandable(v);
    }

    public boolean setUserExpandedChild(View v, boolean userExpanded) {
        return NotificationData.setUserExpanded(v, userExpanded);
    }

    public void onChildDismissed(View v) {
        View veto = v.findViewById(2131492937);
        if (veto != null && veto.getVisibility() != 8 && this.mRemoveViews) {
            veto.performClick();
        }
    }

    public void onBeginDrag(View v) {
        requestDisallowInterceptTouchEvent(true);
    }

    public void onDragCancelled(View v) {
    }

    public View getChildAtPosition(MotionEvent ev) {
        return getChildAtPosition(ev.getX(), ev.getY());
    }

    public View getChildAtRawPosition(float touchX, float touchY) {
        int[] location = new int[2];
        getLocationOnScreen(location);
        return getChildAtPosition(touchX - ((float) location[0]), touchY - ((float) location[1]));
    }

    public View getChildAtPosition(float touchX, float touchY) {
        int count = getChildCount();
        int y = 0;
        for (int childIdx = 0; childIdx < count; childIdx++) {
            View slidingChild = getChildAt(childIdx);
            if (slidingChild.getVisibility() != 8) {
                y += slidingChild.getMeasuredHeight();
                if (touchY < ((float) y)) {
                    return slidingChild;
                }
            }
        }
        return null;
    }

    public View getChildContentView(View v) {
        return v;
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mSwipeHelper.setDensityScale(getResources().getDisplayMetrics().density);
        this.mSwipeHelper.setPagingTouchSlop((float) ViewConfiguration.get(this.mContext).getScaledPagingTouchSlop());
    }

    public void setViewRemoval(boolean removeViews) {
        this.mRemoveViews = removeViews;
    }

    public void setLayoutTransitionsEnabled(boolean b) {
        if (b) {
            setLayoutTransition(this.mRealLayoutTransition);
            return;
        }
        if (this.mRealLayoutTransition.isRunning()) {
            this.mRealLayoutTransition.cancel();
        }
        setLayoutTransition(null);
    }

    public void dismissRowAnimated(View child, int vel) {
        this.mSwipeHelper.dismissChild(child, (float) vel);
    }

    public void onFinishInflate() {
        super.onFinishInflate();
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (this.mOnSizeChangedListener != null) {
            this.mOnSizeChangedListener.onSizeChanged(this, w, h, oldw, oldh);
        }
    }
}
