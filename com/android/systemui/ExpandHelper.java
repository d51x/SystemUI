package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class ExpandHelper implements OnClickListener {
    private Callback mCallback;
    private Context mContext;
    private View mCurrView;
    private View mCurrViewBottomGlow;
    private View mCurrViewTopGlow;
    private ScaleGestureDetector mDetector;
    private View mEventSource;
    private AnimatorSet mGlowAnimationSet;
    private ObjectAnimator mGlowBottomAnimation;
    private ObjectAnimator mGlowTopAnimation;
    private int mGravity;
    private float mInitialTouchFocusY;
    private float mInitialTouchSpan;
    private int mLargeSize;
    private float mMaximumStretch;
    private float mNaturalHeight;
    private float mOldHeight;
    private ObjectAnimator mScaleAnimation;
    private ViewScaler mScaler;
    private int mSmallSize;
    private boolean mStretching;

    public static interface Callback {
        boolean canChildBeExpanded(View view);

        View getChildAtPosition(float f, float f2);

        View getChildAtRawPosition(float f, float f2);

        boolean setUserExpandedChild(View view, boolean z);
    }

    private class ViewScaler {
        View mView;

        public void setView(View v) {
            this.mView = v;
        }

        public void setHeight(float h) {
            LayoutParams lp = this.mView.getLayoutParams();
            lp.height = (int) h;
            this.mView.setLayoutParams(lp);
            this.mView.requestLayout();
        }

        public float getHeight() {
            int height = this.mView.getLayoutParams().height;
            if (height < 0) {
                height = this.mView.getMeasuredHeight();
            }
            return (float) height;
        }

        public int getNaturalHeight(int maximum) {
            LayoutParams lp = this.mView.getLayoutParams();
            int oldHeight = lp.height;
            lp.height = -2;
            this.mView.setLayoutParams(lp);
            this.mView.measure(MeasureSpec.makeMeasureSpec(this.mView.getMeasuredWidth(), 1073741824), MeasureSpec.makeMeasureSpec(maximum, Integer.MIN_VALUE));
            lp.height = oldHeight;
            this.mView.setLayoutParams(lp);
            return this.mView.getMeasuredHeight();
        }
    }

    public ExpandHelper(Context context, Callback callback, int small, int large) {
        this.mSmallSize = small;
        this.mMaximumStretch = ((float) this.mSmallSize) * 2.0f;
        this.mLargeSize = large;
        this.mContext = context;
        this.mCallback = callback;
        this.mScaler = new ViewScaler();
        this.mGravity = 48;
        this.mScaleAnimation = ObjectAnimator.ofFloat(this.mScaler, "height", new float[]{0.0f});
        this.mScaleAnimation.setDuration(250);
        AnimatorListenerAdapter glowVisibilityController = new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                View target = (View) ((ObjectAnimator) animation).getTarget();
                if (target.getAlpha() <= 0.0f) {
                    target.setVisibility(0);
                }
            }

            public void onAnimationEnd(Animator animation) {
                View target = (View) ((ObjectAnimator) animation).getTarget();
                if (target.getAlpha() <= 0.0f) {
                    target.setVisibility(4);
                }
            }
        };
        this.mGlowTopAnimation = ObjectAnimator.ofFloat(null, "alpha", new float[]{0.0f});
        this.mGlowTopAnimation.addListener(glowVisibilityController);
        this.mGlowBottomAnimation = ObjectAnimator.ofFloat(null, "alpha", new float[]{0.0f});
        this.mGlowBottomAnimation.addListener(glowVisibilityController);
        this.mGlowAnimationSet = new AnimatorSet();
        this.mGlowAnimationSet.play(this.mGlowTopAnimation).with(this.mGlowBottomAnimation);
        this.mGlowAnimationSet.setDuration(150);
        this.mDetector = new ScaleGestureDetector(context, new SimpleOnScaleGestureListener() {
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                View v;
                float x = detector.getFocusX();
                float y = detector.getFocusY();
                if (ExpandHelper.this.mEventSource != null) {
                    int[] location = new int[2];
                    ExpandHelper.this.mEventSource.getLocationOnScreen(location);
                    v = ExpandHelper.this.mCallback.getChildAtRawPosition(x + ((float) location[0]), y + ((float) location[1]));
                } else {
                    v = ExpandHelper.this.mCallback.getChildAtPosition(x, y);
                }
                ExpandHelper.this.mInitialTouchFocusY = detector.getFocusY();
                ExpandHelper.this.mInitialTouchSpan = Math.abs(detector.getCurrentSpan());
                ExpandHelper.this.mStretching = ExpandHelper.this.initScale(v);
                return ExpandHelper.this.mStretching;
            }

            public boolean onScale(ScaleGestureDetector detector) {
                float f;
                float span = (Math.abs(detector.getCurrentSpan()) - ExpandHelper.this.mInitialTouchSpan) * 1.0f;
                float drag = (detector.getFocusY() - ExpandHelper.this.mInitialTouchFocusY) * 1.0f;
                if (ExpandHelper.this.mGravity == 80) {
                    f = -1.0f;
                } else {
                    f = 1.0f;
                }
                drag *= f;
                float pull = (Math.abs(drag) + Math.abs(span)) + 1.0f;
                float hand = (((Math.abs(drag) * drag) / pull) + ((Math.abs(span) * span) / pull)) + ExpandHelper.this.mOldHeight;
                float target = hand;
                if (hand < ((float) ExpandHelper.this.mSmallSize)) {
                    hand = (float) ExpandHelper.this.mSmallSize;
                } else if (hand > ((float) ExpandHelper.this.mLargeSize)) {
                    hand = (float) ExpandHelper.this.mLargeSize;
                }
                if (hand > ExpandHelper.this.mNaturalHeight) {
                    hand = ExpandHelper.this.mNaturalHeight;
                }
                ExpandHelper.this.mScaler.setHeight(hand);
                ExpandHelper.this.setGlow(((1.0f / (((float) Math.pow(2.718281828459045d, (double) (((8.0f * Math.abs((target - hand) / ExpandHelper.this.mMaximumStretch)) - 5.0f) * -1.0f))) + 1.0f)) * 0.5f) + 0.5f);
                return true;
            }

            public void onScaleEnd(ScaleGestureDetector detector) {
                ExpandHelper.this.finishScale(false);
            }
        });
    }

    public void setEventSource(View eventSource) {
        this.mEventSource = eventSource;
    }

    public void setGravity(int gravity) {
        this.mGravity = gravity;
    }

    public void setGlow(float glow) {
        if (!this.mGlowAnimationSet.isRunning() || glow == 0.0f) {
            if (this.mGlowAnimationSet.isRunning()) {
                this.mGlowAnimationSet.cancel();
            }
            if (this.mCurrViewTopGlow != null && this.mCurrViewBottomGlow != null) {
                if (glow == 0.0f || this.mCurrViewTopGlow.getAlpha() == 0.0f) {
                    this.mGlowTopAnimation.setTarget(this.mCurrViewTopGlow);
                    this.mGlowBottomAnimation.setTarget(this.mCurrViewBottomGlow);
                    this.mGlowTopAnimation.setFloatValues(new float[]{glow});
                    this.mGlowBottomAnimation.setFloatValues(new float[]{glow});
                    this.mGlowAnimationSet.setupStartValues();
                    this.mGlowAnimationSet.start();
                    return;
                }
                this.mCurrViewTopGlow.setAlpha(glow);
                this.mCurrViewBottomGlow.setAlpha(glow);
                handleGlowVisibility();
            }
        }
    }

    private void handleGlowVisibility() {
        int i;
        int i2 = 4;
        View view = this.mCurrViewTopGlow;
        if (this.mCurrViewTopGlow.getAlpha() <= 0.0f) {
            i = 4;
        } else {
            i = 0;
        }
        view.setVisibility(i);
        View view2 = this.mCurrViewBottomGlow;
        if (this.mCurrViewBottomGlow.getAlpha() > 0.0f) {
            i2 = 0;
        }
        view2.setVisibility(i2);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        this.mDetector.onTouchEvent(ev);
        return this.mStretching;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (this.mStretching) {
            this.mDetector.onTouchEvent(ev);
        }
        switch (action) {
            case 1:
            case 3:
                this.mStretching = false;
                clearView();
                break;
        }
        return true;
    }

    private boolean initScale(View v) {
        if (v != null) {
            this.mStretching = true;
            setView(v);
            setGlow(0.5f);
            this.mScaler.setView(v);
            this.mOldHeight = this.mScaler.getHeight();
            if (this.mCallback.canChildBeExpanded(v)) {
                this.mNaturalHeight = (float) this.mScaler.getNaturalHeight(this.mLargeSize);
            } else {
                this.mNaturalHeight = this.mOldHeight;
            }
            v.getParent().requestDisallowInterceptTouchEvent(true);
        }
        return this.mStretching;
    }

    private void finishScale(boolean force) {
        boolean wasClosed;
        boolean z = true;
        float h = this.mScaler.getHeight();
        if (this.mOldHeight == ((float) this.mSmallSize)) {
            wasClosed = true;
        } else {
            wasClosed = false;
        }
        h = wasClosed ? (force || h > ((float) this.mSmallSize)) ? this.mNaturalHeight : (float) this.mSmallSize : (force || h < this.mNaturalHeight) ? (float) this.mSmallSize : this.mNaturalHeight;
        if (this.mScaleAnimation.isRunning()) {
            this.mScaleAnimation.cancel();
        }
        this.mScaleAnimation.setFloatValues(new float[]{h});
        this.mScaleAnimation.setupStartValues();
        this.mScaleAnimation.start();
        this.mStretching = false;
        setGlow(0.0f);
        Callback callback = this.mCallback;
        View view = this.mCurrView;
        if (h != this.mNaturalHeight) {
            z = false;
        }
        callback.setUserExpandedChild(view, z);
        clearView();
    }

    private void clearView() {
        this.mCurrView = null;
        this.mCurrViewTopGlow = null;
        this.mCurrViewBottomGlow = null;
    }

    private void setView(View v) {
        this.mCurrView = v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            this.mCurrViewTopGlow = g.findViewById(2131492936);
            this.mCurrViewBottomGlow = g.findViewById(2131492940);
        }
    }

    public void onClick(View v) {
        initScale(v);
        finishScale(true);
    }
}
