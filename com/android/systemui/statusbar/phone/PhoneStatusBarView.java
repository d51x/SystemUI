package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Iterator;

public class PhoneStatusBarView extends FrameLayout {
    ArrayList<Rect> mButtonBounds;
    boolean mCapturingEvents;
    int mEndAlpha;
    long mEndTime;
    boolean mNightMode;
    ViewGroup mNotificationIcons;
    View mRecentView;
    PhoneStatusBar mService;
    int mStartAlpha;
    ViewGroup mStatusIcons;

    public PhoneStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mNightMode = false;
        this.mStartAlpha = 0;
        this.mEndAlpha = 0;
        this.mEndTime = 0;
        this.mButtonBounds = null;
        this.mCapturingEvents = true;
    }

    private void addButtonBounds(View v) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        Rect bounds = new Rect();
        bounds.set(location[0], location[1], location[0] + v.getWidth(), location[1] + v.getHeight());
        this.mButtonBounds.add(bounds);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mNotificationIcons = (ViewGroup) findViewById(2131492906);
        this.mStatusIcons = (ViewGroup) findViewById(2131492907);
        if (this.mButtonBounds == null) {
            this.mButtonBounds = new ArrayList();
        }
        this.mButtonBounds.clear();
        addButtonBounds(findViewById(2131492902));
        addButtonBounds(findViewById(2131492903));
        this.mRecentView = findViewById(2131492912);
        addButtonBounds(this.mRecentView);
        addButtonBounds(findViewById(2131492913));
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        boolean nightMode;
        int i = 0;
        super.onConfigurationChanged(newConfig);
        this.mService.updateDisplaySize();
        if ((newConfig.uiMode & 48) == 32) {
            nightMode = true;
        } else {
            nightMode = false;
        }
        if (this.mNightMode != nightMode) {
            this.mNightMode = nightMode;
            this.mStartAlpha = getCurAlpha();
            if (this.mNightMode) {
                i = 128;
            }
            this.mEndAlpha = i;
            this.mEndTime = SystemClock.uptimeMillis() + 400;
            invalidate();
        }
    }

    int getCurAlpha() {
        long time = SystemClock.uptimeMillis();
        return time > this.mEndTime ? this.mEndAlpha : this.mEndAlpha - ((int) ((((long) (this.mEndAlpha - this.mStartAlpha)) * (this.mEndTime - time)) / 400));
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mService.updateExpandedViewPos(-10000);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int alpha = getCurAlpha();
        if (alpha != 0) {
            canvas.drawARGB(alpha, 0, 0, 0);
        }
        if (alpha != this.mEndAlpha) {
            invalidate();
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mCapturingEvents) {
            return false;
        }
        if (event.getAction() != 0) {
            this.mService.interceptTouchEvent(event);
        }
        return true;
    }

    private boolean containsButtonBounds(int x, int y) {
        Iterator i$ = this.mButtonBounds.iterator();
        while (i$.hasNext()) {
            if (((Rect) i$.next()).contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getAction() == 0 && this.mButtonBounds != null && containsButtonBounds((int) event.getX(), (int) event.getY())) {
            this.mCapturingEvents = false;
            return false;
        }
        this.mCapturingEvents = true;
        return !this.mService.interceptTouchEvent(event) ? super.onInterceptTouchEvent(event) : true;
    }

    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        if (!super.onRequestSendAccessibilityEvent(child, event)) {
            return false;
        }
        AccessibilityEvent record = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(record);
        dispatchPopulateAccessibilityEvent(record);
        event.appendRecord(record);
        return true;
    }
}
