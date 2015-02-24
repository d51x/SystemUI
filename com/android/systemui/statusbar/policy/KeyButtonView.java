package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.SwipeHelper;

public class KeyButtonView extends ImageView {
    final float BUTTON_QUIESCENT_ALPHA;
    final float GLOW_MAX_SCALE_FACTOR;
    Runnable mCheckLongPress;
    int mCode;
    long mDownTime;
    float mDrawingAlpha;
    float mGlowAlpha;
    Drawable mGlowBG;
    int mGlowHeight;
    float mGlowScale;
    int mGlowWidth;
    AnimatorSet mPressedAnim;
    RectF mRect;
    boolean mSupportsLongpress;
    int mTouchSlop;

    public KeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.GLOW_MAX_SCALE_FACTOR = 1.8f;
        this.BUTTON_QUIESCENT_ALPHA = 0.7f;
        this.mGlowAlpha = 0.0f;
        this.mGlowScale = 1.0f;
        this.mDrawingAlpha = 1.0f;
        this.mSupportsLongpress = true;
        this.mRect = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
        this.mCheckLongPress = new Runnable() {
            public void run() {
                if (!KeyButtonView.this.isPressed()) {
                    return;
                }
                if (KeyButtonView.this.mCode != 0) {
                    KeyButtonView.this.sendEvent(0, 128);
                    KeyButtonView.this.sendAccessibilityEvent(2);
                    return;
                }
                KeyButtonView.this.performLongClick();
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView, defStyle, 0);
        this.mCode = a.getInteger(0, 0);
        this.mSupportsLongpress = a.getBoolean(1, true);
        this.mGlowBG = a.getDrawable(2);
        if (this.mGlowBG != null) {
            setDrawingAlpha(0.7f);
            this.mGlowWidth = this.mGlowBG.getIntrinsicWidth();
            this.mGlowHeight = this.mGlowBG.getIntrinsicHeight();
        }
        a.recycle();
        setClickable(true);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    protected void onDraw(Canvas canvas) {
        if (this.mGlowBG != null) {
            canvas.save();
            int w = getWidth();
            int h = getHeight();
            int drawW = (int) (((float) h) * (((float) this.mGlowWidth) / ((float) this.mGlowHeight)));
            int drawH = h;
            int margin = (drawW - w) / 2;
            canvas.scale(this.mGlowScale, this.mGlowScale, ((float) w) * 0.5f, ((float) h) * 0.5f);
            this.mGlowBG.setBounds(-margin, 0, drawW - margin, drawH);
            this.mGlowBG.setAlpha((int) ((this.mDrawingAlpha * this.mGlowAlpha) * 255.0f));
            this.mGlowBG.draw(canvas);
            canvas.restore();
            this.mRect.right = (float) w;
            this.mRect.bottom = (float) h;
        }
        super.onDraw(canvas);
    }

    public float getDrawingAlpha() {
        return this.mGlowBG == null ? 0.0f : this.mDrawingAlpha;
    }

    public void setDrawingAlpha(float x) {
        if (this.mGlowBG != null) {
            setAlpha((int) (255.0f * x));
            this.mDrawingAlpha = x;
        }
    }

    public float getGlowAlpha() {
        return this.mGlowBG == null ? 0.0f : this.mGlowAlpha;
    }

    public void setGlowAlpha(float x) {
        if (this.mGlowBG != null) {
            this.mGlowAlpha = x;
            invalidate();
        }
    }

    public float getGlowScale() {
        return this.mGlowBG == null ? 0.0f : this.mGlowScale;
    }

    public void setGlowScale(float x) {
        if (this.mGlowBG != null) {
            this.mGlowScale = x;
            float rx = ((((float) getWidth()) * 0.79999995f) / 2.0f) + 1.0f;
            float ry = ((((float) getHeight()) * 0.79999995f) / 2.0f) + 1.0f;
            SwipeHelper.invalidateGlobalRegion(this, new RectF(((float) getLeft()) - rx, ((float) getTop()) - ry, ((float) getRight()) + rx, ((float) getBottom()) + ry));
            ((View) getParent()).invalidate();
        }
    }

    public void setPressed(boolean pressed) {
        if (!(this.mGlowBG == null || pressed == isPressed())) {
            if (this.mPressedAnim != null && this.mPressedAnim.isRunning()) {
                this.mPressedAnim.cancel();
            }
            AnimatorSet as = new AnimatorSet();
            this.mPressedAnim = as;
            Animator[] animatorArr;
            if (pressed) {
                if (this.mGlowScale < 1.8f) {
                    this.mGlowScale = 1.8f;
                }
                if (this.mGlowAlpha < 0.7f) {
                    this.mGlowAlpha = 0.7f;
                }
                setDrawingAlpha(1.0f);
                animatorArr = new Animator[2];
                animatorArr[0] = ObjectAnimator.ofFloat(this, "glowAlpha", new float[]{1.0f});
                animatorArr[1] = ObjectAnimator.ofFloat(this, "glowScale", new float[]{1.8f});
                as.playTogether(animatorArr);
                as.setDuration(50);
            } else {
                animatorArr = new Animator[3];
                animatorArr[0] = ObjectAnimator.ofFloat(this, "glowAlpha", new float[]{0.0f});
                animatorArr[1] = ObjectAnimator.ofFloat(this, "glowScale", new float[]{1.0f});
                animatorArr[2] = ObjectAnimator.ofFloat(this, "drawingAlpha", new float[]{0.7f});
                as.playTogether(animatorArr);
                as.setDuration(500);
            }
            as.start();
        }
        super.setPressed(pressed);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean z = false;
        switch (ev.getAction()) {
            case 0:
                this.mDownTime = SystemClock.uptimeMillis();
                setPressed(true);
                if (this.mCode != 0) {
                    sendEvent(0, 0, this.mDownTime);
                } else {
                    performHapticFeedback(1);
                }
                if (this.mSupportsLongpress) {
                    removeCallbacks(this.mCheckLongPress);
                    postDelayed(this.mCheckLongPress, (long) ViewConfiguration.getLongPressTimeout());
                }
                break;
            case 1:
                boolean doIt = isPressed();
                setPressed(false);
                if (this.mCode != 0) {
                    if (doIt) {
                        sendEvent(1, 0);
                        sendAccessibilityEvent(1);
                        playSoundEffect(0);
                    } else {
                        sendEvent(1, 32);
                    }
                } else if (doIt) {
                    performClick();
                }
                if (this.mSupportsLongpress) {
                    removeCallbacks(this.mCheckLongPress);
                }
                break;
            case 2:
                int x = (int) ev.getX();
                int y = (int) ev.getY();
                if (x >= (-this.mTouchSlop) && x < getWidth() + this.mTouchSlop && y >= (-this.mTouchSlop) && y < getHeight() + this.mTouchSlop) {
                    z = true;
                }
                setPressed(z);
                break;
            case 3:
                setPressed(false);
                if (this.mCode != 0) {
                    sendEvent(1, 32);
                }
                if (this.mSupportsLongpress) {
                    removeCallbacks(this.mCheckLongPress);
                }
                break;
        }
        return true;
    }

    void sendEvent(int action, int flags) {
        sendEvent(action, flags, SystemClock.uptimeMillis());
    }

    void sendEvent(int action, int flags, long when) {
        InputManager.getInstance().injectInputEvent(new KeyEvent(this.mDownTime, when, action, this.mCode, (flags & 128) != 0 ? 1 : 0, 0, -1, 0, (flags | 8) | 64, 257), 0);
    }
}
