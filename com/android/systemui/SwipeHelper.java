package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;

public class SwipeHelper {
    public static float ALPHA_FADE_START;
    private static LinearInterpolator sLinearInterpolator;
    private int DEFAULT_ESCAPE_ANIMATION_DURATION;
    private int MAX_DISMISS_VELOCITY;
    private int MAX_ESCAPE_ANIMATION_DURATION;
    private float SWIPE_ESCAPE_VELOCITY;
    private Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    private View mCurrAnimView;
    private View mCurrView;
    private float mDensityScale;
    private boolean mDragging;
    private Handler mHandler;
    private float mInitialTouchPos;
    private OnLongClickListener mLongPressListener;
    private boolean mLongPressSent;
    private long mLongPressTimeout;
    private float mMinAlpha;
    private float mPagingTouchSlop;
    private int mSwipeDirection;
    private VelocityTracker mVelocityTracker;
    private Runnable mWatchLongPress;

    class AnonymousClass_2 extends AnimatorListenerAdapter {
        final /* synthetic */ View val$animView;
        final /* synthetic */ View val$view;

        AnonymousClass_2(View view, View view2) {
            this.val$view = view;
            this.val$animView = view2;
        }

        public void onAnimationEnd(Animator animation) {
            SwipeHelper.this.mCallback.onChildDismissed(this.val$view);
            this.val$animView.setLayerType(0, null);
        }
    }

    class AnonymousClass_3 implements AnimatorUpdateListener {
        final /* synthetic */ View val$animView;
        final /* synthetic */ boolean val$canAnimViewBeDismissed;

        AnonymousClass_3(boolean z, View view) {
            this.val$canAnimViewBeDismissed = z;
            this.val$animView = view;
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            if (this.val$canAnimViewBeDismissed) {
                this.val$animView.setAlpha(SwipeHelper.this.getAlphaForOffset(this.val$animView));
            }
            SwipeHelper.invalidateGlobalRegion(this.val$animView);
        }
    }

    class AnonymousClass_4 implements AnimatorUpdateListener {
        final /* synthetic */ View val$animView;
        final /* synthetic */ boolean val$canAnimViewBeDismissed;

        AnonymousClass_4(boolean z, View view) {
            this.val$canAnimViewBeDismissed = z;
            this.val$animView = view;
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            if (this.val$canAnimViewBeDismissed) {
                this.val$animView.setAlpha(SwipeHelper.this.getAlphaForOffset(this.val$animView));
            }
            SwipeHelper.invalidateGlobalRegion(this.val$animView);
        }
    }

    public static interface Callback {
        boolean canChildBeDismissed(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        View getChildContentView(View view);

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onDragCancelled(View view);
    }

    static {
        sLinearInterpolator = new LinearInterpolator();
        ALPHA_FADE_START = 0.0f;
    }

    public SwipeHelper(int swipeDirection, Callback callback, float densityScale, float pagingTouchSlop) {
        this.SWIPE_ESCAPE_VELOCITY = 100.0f;
        this.DEFAULT_ESCAPE_ANIMATION_DURATION = 200;
        this.MAX_ESCAPE_ANIMATION_DURATION = 400;
        this.MAX_DISMISS_VELOCITY = 2000;
        this.mMinAlpha = 0.0f;
        this.mCallback = callback;
        this.mHandler = new Handler();
        this.mSwipeDirection = swipeDirection;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mDensityScale = densityScale;
        this.mPagingTouchSlop = pagingTouchSlop;
        this.mLongPressTimeout = (long) (((float) ViewConfiguration.getLongPressTimeout()) * 1.5f);
    }

    public void setLongPressListener(OnLongClickListener listener) {
        this.mLongPressListener = listener;
    }

    public void setDensityScale(float densityScale) {
        this.mDensityScale = densityScale;
    }

    public void setPagingTouchSlop(float pagingTouchSlop) {
        this.mPagingTouchSlop = pagingTouchSlop;
    }

    private float getPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getX() : ev.getY();
    }

    private float getTranslation(View v) {
        return this.mSwipeDirection == 0 ? v.getTranslationX() : v.getTranslationY();
    }

    private float getVelocity(VelocityTracker vt) {
        return this.mSwipeDirection == 0 ? vt.getXVelocity() : vt.getYVelocity();
    }

    private ObjectAnimator createTranslationAnimation(View v, float newPos) {
        return ObjectAnimator.ofFloat(v, this.mSwipeDirection == 0 ? "translationX" : "translationY", new float[]{newPos});
    }

    private float getPerpendicularVelocity(VelocityTracker vt) {
        return this.mSwipeDirection == 0 ? vt.getYVelocity() : vt.getXVelocity();
    }

    private void setTranslation(View v, float translate) {
        if (this.mSwipeDirection == 0) {
            v.setTranslationX(translate);
        } else {
            v.setTranslationY(translate);
        }
    }

    private float getSize(View v) {
        return this.mSwipeDirection == 0 ? (float) v.getMeasuredWidth() : (float) v.getMeasuredHeight();
    }

    public void setMinAlpha(float minAlpha) {
        this.mMinAlpha = minAlpha;
    }

    private float getAlphaForOffset(View view) {
        float viewSize = getSize(view);
        float fadeSize = 0.5f * viewSize;
        float result = 1.0f;
        float pos = getTranslation(view);
        if (pos >= ALPHA_FADE_START * viewSize) {
            result = 1.0f - ((pos - (ALPHA_FADE_START * viewSize)) / fadeSize);
        } else if (pos < (1.0f - ALPHA_FADE_START) * viewSize) {
            result = 1.0f + (((ALPHA_FADE_START * viewSize) + pos) / fadeSize);
        }
        return Math.max(this.mMinAlpha, result);
    }

    public static void invalidateGlobalRegion(View view) {
        invalidateGlobalRegion(view, new RectF((float) view.getLeft(), (float) view.getTop(), (float) view.getRight(), (float) view.getBottom()));
    }

    public static void invalidateGlobalRegion(View view, RectF childBounds) {
        while (view.getParent() != null && (view.getParent() instanceof View)) {
            view = view.getParent();
            view.getMatrix().mapRect(childBounds);
            view.invalidate((int) Math.floor((double) childBounds.left), (int) Math.floor((double) childBounds.top), (int) Math.ceil((double) childBounds.right), (int) Math.ceil((double) childBounds.bottom));
        }
    }

    public void removeLongPressCallback() {
        if (this.mWatchLongPress != null) {
            this.mHandler.removeCallbacks(this.mWatchLongPress);
            this.mWatchLongPress = null;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case 0:
                this.mDragging = false;
                this.mLongPressSent = false;
                this.mCurrView = this.mCallback.getChildAtPosition(ev);
                this.mVelocityTracker.clear();
                if (this.mCurrView != null) {
                    this.mCurrAnimView = this.mCallback.getChildContentView(this.mCurrView);
                    this.mCanCurrViewBeDimissed = this.mCallback.canChildBeDismissed(this.mCurrView);
                    this.mVelocityTracker.addMovement(ev);
                    this.mInitialTouchPos = getPos(ev);
                    if (this.mLongPressListener != null) {
                        if (this.mWatchLongPress == null) {
                            this.mWatchLongPress = new Runnable() {
                                public void run() {
                                    if (SwipeHelper.this.mCurrView != null && !SwipeHelper.this.mLongPressSent) {
                                        SwipeHelper.this.mLongPressSent = true;
                                        SwipeHelper.this.mCurrView.sendAccessibilityEvent(2);
                                        SwipeHelper.this.mLongPressListener.onLongClick(SwipeHelper.this.mCurrView);
                                    }
                                }
                            };
                        }
                        this.mHandler.postDelayed(this.mWatchLongPress, this.mLongPressTimeout);
                    }
                }
                break;
            case 1:
            case 3:
                this.mDragging = false;
                this.mCurrView = null;
                this.mCurrAnimView = null;
                this.mLongPressSent = false;
                removeLongPressCallback();
                break;
            case 2:
                if (!(this.mCurrView == null || this.mLongPressSent)) {
                    this.mVelocityTracker.addMovement(ev);
                    if (Math.abs(getPos(ev) - this.mInitialTouchPos) > this.mPagingTouchSlop) {
                        this.mCallback.onBeginDrag(this.mCurrView);
                        this.mDragging = true;
                        this.mInitialTouchPos = getPos(ev) - getTranslation(this.mCurrAnimView);
                        removeLongPressCallback();
                    }
                }
                break;
        }
        return this.mDragging;
    }

    public void dismissChild(View view, float velocity) {
        float newPos;
        int duration;
        ObjectAnimator anim;
        View animView = this.mCallback.getChildContentView(view);
        boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(view);
        if (velocity >= 0.0f) {
            if ((velocity != 0.0f || getTranslation(animView) >= 0.0f) && !(velocity == 0.0f && getTranslation(animView) == 0.0f && this.mSwipeDirection == 1)) {
                newPos = getSize(animView);
                duration = this.MAX_ESCAPE_ANIMATION_DURATION;
                if (velocity == 0.0f) {
                    duration = Math.min(duration, (int) ((Math.abs(newPos - getTranslation(animView)) * 1000.0f) / Math.abs(velocity)));
                } else {
                    duration = this.DEFAULT_ESCAPE_ANIMATION_DURATION;
                }
                animView.setLayerType(2, null);
                anim = createTranslationAnimation(animView, newPos);
                anim.setInterpolator(sLinearInterpolator);
                anim.setDuration((long) duration);
                anim.addListener(new AnonymousClass_2(view, animView));
                anim.addUpdateListener(new AnonymousClass_3(canAnimViewBeDismissed, animView));
                anim.start();
            }
        }
        newPos = -getSize(animView);
        duration = this.MAX_ESCAPE_ANIMATION_DURATION;
        if (velocity == 0.0f) {
            duration = this.DEFAULT_ESCAPE_ANIMATION_DURATION;
        } else {
            duration = Math.min(duration, (int) ((Math.abs(newPos - getTranslation(animView)) * 1000.0f) / Math.abs(velocity)));
        }
        animView.setLayerType(2, null);
        anim = createTranslationAnimation(animView, newPos);
        anim.setInterpolator(sLinearInterpolator);
        anim.setDuration((long) duration);
        anim.addListener(new AnonymousClass_2(view, animView));
        anim.addUpdateListener(new AnonymousClass_3(canAnimViewBeDismissed, animView));
        anim.start();
    }

    public void snapChild(View view, float velocity) {
        View animView = this.mCallback.getChildContentView(view);
        boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(animView);
        ObjectAnimator anim = createTranslationAnimation(animView, 0.0f);
        anim.setDuration((long) 150);
        anim.addUpdateListener(new AnonymousClass_4(canAnimViewBeDismissed, animView));
        anim.start();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mLongPressSent) {
            return true;
        }
        if (this.mDragging) {
            this.mVelocityTracker.addMovement(ev);
            switch (ev.getAction()) {
                case 1:
                case 3:
                    if (this.mCurrView != null) {
                        boolean childSwipedFastEnough;
                        boolean dismissChild;
                        View view;
                        this.mVelocityTracker.computeCurrentVelocity(1000, ((float) this.MAX_DISMISS_VELOCITY) * this.mDensityScale);
                        float escapeVelocity = this.SWIPE_ESCAPE_VELOCITY * this.mDensityScale;
                        float velocity = getVelocity(this.mVelocityTracker);
                        float perpendicularVelocity = getPerpendicularVelocity(this.mVelocityTracker);
                        boolean childSwipedFarEnough = ((double) Math.abs(getTranslation(this.mCurrAnimView))) > 0.4d * ((double) getSize(this.mCurrAnimView));
                        if (Math.abs(velocity) > escapeVelocity && Math.abs(velocity) > Math.abs(perpendicularVelocity)) {
                            if ((velocity > 0.0f ? 1 : null) == (getTranslation(this.mCurrAnimView) > 0.0f ? 1 : null)) {
                                childSwipedFastEnough = true;
                                dismissChild = this.mCallback.canChildBeDismissed(this.mCurrView) && (childSwipedFastEnough || childSwipedFarEnough);
                                if (dismissChild) {
                                    this.mCallback.onDragCancelled(this.mCurrView);
                                    snapChild(this.mCurrView, velocity);
                                } else {
                                    view = this.mCurrView;
                                    if (!childSwipedFastEnough) {
                                        velocity = 0.0f;
                                    }
                                    dismissChild(view, velocity);
                                }
                            }
                        }
                        childSwipedFastEnough = false;
                        if (!this.mCallback.canChildBeDismissed(this.mCurrView)) {
                        }
                        if (dismissChild) {
                            this.mCallback.onDragCancelled(this.mCurrView);
                            snapChild(this.mCurrView, velocity);
                        } else {
                            view = this.mCurrView;
                            if (childSwipedFastEnough) {
                                velocity = 0.0f;
                            }
                            dismissChild(view, velocity);
                        }
                    }
                    break;
                case 2:
                case 4:
                    if (this.mCurrView != null) {
                        float delta = getPos(ev) - this.mInitialTouchPos;
                        if (!this.mCallback.canChildBeDismissed(this.mCurrView)) {
                            float size = getSize(this.mCurrAnimView);
                            float maxScrollDistance = 0.15f * size;
                            delta = Math.abs(delta) >= size ? delta > 0.0f ? maxScrollDistance : -maxScrollDistance : maxScrollDistance * ((float) Math.sin(((double) (delta / size)) * 1.5707963267948966d));
                        }
                        setTranslation(this.mCurrAnimView, delta);
                        if (this.mCanCurrViewBeDimissed) {
                            this.mCurrAnimView.setAlpha(getAlphaForOffset(this.mCurrAnimView));
                        }
                        invalidateGlobalRegion(this.mCurrView);
                    }
                    break;
            }
            return true;
        }
        removeLongPressCallback();
        return false;
    }
}
