package com.android.systemui.statusbar.tablet;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;
import com.android.systemui.ExpandHelper;
import com.android.systemui.statusbar.policy.NotificationRowLayout;

public class NotificationPanel extends RelativeLayout implements OnClickListener, StatusBarPanel {
    static Interpolator sAccelerateInterpolator;
    static Interpolator sDecelerateInterpolator;
    private NotificationRowLayout latestItems;
    TabletStatusBar mBar;
    Choreographer mChoreo;
    View mClearButton;
    private OnClickListener mClearButtonListener;
    Rect mContentArea;
    ViewGroup mContentFrame;
    float mContentFrameMissingTranslation;
    ViewGroup mContentParent;
    private ExpandHelper mExpandHelper;
    boolean mHasClearableNotifications;
    View mNotificationButton;
    int mNotificationCount;
    View mNotificationScroller;
    private OnPreDrawListener mPreDrawListener;
    View mSettingsButton;
    View mSettingsView;
    boolean mShowing;
    NotificationPanelTitle mTitleArea;

    class AnonymousClass_3 extends AnimatorListenerAdapter {
        final /* synthetic */ View val$toHide;
        final /* synthetic */ View val$toShow;

        AnonymousClass_3(View view, View view2) {
            this.val$toHide = view;
            this.val$toShow = view2;
        }

        public void onAnimationEnd(Animator _a) {
            this.val$toHide.setVisibility(8);
            if (this.val$toShow != null) {
                this.val$toShow.setVisibility(0);
                if (this.val$toShow == NotificationPanel.this.mSettingsView || NotificationPanel.this.mNotificationCount > 0) {
                    ObjectAnimator.ofFloat(this.val$toShow, "alpha", new float[]{0.0f, 1.0f}).setDuration(150).start();
                }
                if (this.val$toHide == NotificationPanel.this.mSettingsView) {
                    NotificationPanel.this.removeSettingsView();
                }
            }
            NotificationPanel.this.updateClearButton();
            NotificationPanel.this.updatePanelModeButtons();
        }
    }

    private class Choreographer implements AnimatorListener {
        final int HYPERSPACE_OFFRAMP;
        AnimatorSet mContentAnim;
        boolean mVisible;

        Choreographer() {
            this.HYPERSPACE_OFFRAMP = 200;
        }

        void createAnimation(boolean appearing) {
            float end;
            float start;
            float y = NotificationPanel.this.mContentParent.getTranslationY();
            if (appearing) {
                end = 0.0f;
                if (NotificationPanel.this.mNotificationCount == 0) {
                    end = 0.0f + NotificationPanel.this.mContentFrameMissingTranslation;
                }
                start = 200.0f + end;
            } else {
                start = y;
                end = y + 200.0f;
            }
            Animator posAnim = ObjectAnimator.ofFloat(NotificationPanel.this.mContentParent, "translationY", new float[]{start, end});
            posAnim.setInterpolator(appearing ? sDecelerateInterpolator : sAccelerateInterpolator);
            if (this.mContentAnim != null && this.mContentAnim.isRunning()) {
                this.mContentAnim.cancel();
            }
            ViewGroup viewGroup = NotificationPanel.this.mContentParent;
            String str = "alpha";
            float[] fArr = new float[1];
            fArr[0] = appearing ? 1065353216 : null;
            Animator fadeAnim = ObjectAnimator.ofFloat(viewGroup, str, fArr);
            fadeAnim.setInterpolator(appearing ? sAccelerateInterpolator : sDecelerateInterpolator);
            this.mContentAnim = new AnimatorSet();
            this.mContentAnim.play(fadeAnim).with(posAnim);
            AnimatorSet animatorSet = this.mContentAnim;
            if (appearing) {
                animatorSet.setDuration((long) 250);
                this.mContentAnim.addListener(this);
            } else {
                animatorSet.setDuration((long) 250);
                this.mContentAnim.addListener(this);
            }
        }

        void startAnimation(boolean appearing) {
            createAnimation(appearing);
            this.mContentAnim.start();
            this.mVisible = appearing;
            if (!this.mVisible) {
                NotificationPanel.this.updateClearButton();
            }
        }

        public void onAnimationCancel(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            if (!this.mVisible) {
                NotificationPanel.this.setVisibility(8);
            }
            NotificationPanel.this.mContentParent.setLayerType(0, null);
            this.mContentAnim = null;
            if (this.mVisible) {
                NotificationPanel.this.updateClearButton();
            }
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationStart(Animator animation) {
        }
    }

    static {
        sAccelerateInterpolator = new AccelerateInterpolator();
        sDecelerateInterpolator = new DecelerateInterpolator();
    }

    public NotificationPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHasClearableNotifications = false;
        this.mNotificationCount = 0;
        this.mContentArea = new Rect();
        this.mChoreo = new Choreographer();
        this.mClearButtonListener = new OnClickListener() {
            public void onClick(View v) {
                NotificationPanel.this.mBar.clearAll();
            }
        };
        this.mPreDrawListener = new OnPreDrawListener() {
            public boolean onPreDraw() {
                NotificationPanel.this.getViewTreeObserver().removeOnPreDrawListener(this);
                NotificationPanel.this.mChoreo.startAnimation(true);
                return false;
            }
        };
    }

    public void setBar(TabletStatusBar b) {
        this.mBar = b;
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        setWillNotDraw(false);
        this.mContentParent = (ViewGroup) findViewById(2131492994);
        this.mContentParent.bringToFront();
        this.mTitleArea = (NotificationPanelTitle) findViewById(2131492998);
        this.mTitleArea.setPanel(this);
        this.mSettingsButton = findViewById(2131492933);
        this.mNotificationButton = findViewById(2131493005);
        this.mNotificationScroller = findViewById(2131492997);
        this.mContentFrame = (ViewGroup) findViewById(2131492996);
        this.mContentFrameMissingTranslation = 0.0f;
        this.mClearButton = findViewById(2131492934);
        this.mClearButton.setOnClickListener(this.mClearButtonListener);
        this.mShowing = false;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.latestItems = (NotificationRowLayout) findViewById(2131492938);
        this.mExpandHelper = new ExpandHelper(this.mContext, this.latestItems, getResources().getDimensionPixelSize(2131427345), getResources().getDimensionPixelSize(2131427346));
        this.mExpandHelper.setEventSource(this);
        this.mExpandHelper.setGravity(80);
    }

    public View getClearButton() {
        return this.mClearButton;
    }

    public void show(boolean show, boolean animate) {
        int i = 0;
        if (!animate) {
            this.mShowing = show;
            if (!show) {
                i = 8;
            }
            setVisibility(i);
        } else if (this.mShowing != show) {
            this.mShowing = show;
            if (show) {
                setVisibility(0);
                this.mContentParent.setLayerType(2, null);
                getViewTreeObserver().addOnPreDrawListener(this.mPreDrawListener);
                return;
            }
            this.mChoreo.startAnimation(show);
        }
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public void onVisibilityChanged(View v, int vis) {
        super.onVisibilityChanged(v, vis);
        if (vis != 0) {
            if (this.mSettingsView != null) {
                removeSettingsView();
            }
            this.mNotificationScroller.setVisibility(0);
            this.mNotificationScroller.setAlpha(1.0f);
            this.mNotificationScroller.scrollTo(0, 0);
            updatePanelModeButtons();
        }
    }

    public boolean dispatchHoverEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        return (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) ? true : super.dispatchHoverEvent(event);
    }

    public void onClick(View v) {
        if (this.mSettingsButton.isEnabled() && v == this.mTitleArea) {
            swapPanels();
        }
    }

    public void setNotificationCount(int n) {
        this.mNotificationCount = n;
    }

    public void swapPanels() {
        View toShow;
        View toHide;
        if (this.mSettingsView == null) {
            addSettingsView();
            toShow = this.mSettingsView;
            toHide = this.mNotificationScroller;
        } else {
            toShow = this.mNotificationScroller;
            toHide = this.mSettingsView;
        }
        Animator a = ObjectAnimator.ofFloat(toHide, "alpha", new float[]{1.0f, 0.0f}).setDuration(150);
        a.addListener(new AnonymousClass_3(toHide, toShow));
        a.start();
    }

    public void updateClearButton() {
        int i = 0;
        if (this.mBar != null) {
            boolean showX = isShowing() && this.mHasClearableNotifications && this.mNotificationScroller.getVisibility() == 0;
            View clearButton = getClearButton();
            if (!showX) {
                i = 4;
            }
            clearButton.setVisibility(i);
        }
    }

    public void setClearable(boolean clearable) {
        this.mHasClearableNotifications = clearable;
    }

    public void updatePanelModeButtons() {
        boolean settingsVisible;
        int i;
        int i2 = 0;
        if (this.mSettingsView != null) {
            settingsVisible = true;
        } else {
            settingsVisible = false;
        }
        View view = this.mSettingsButton;
        if (settingsVisible || !this.mSettingsButton.isEnabled()) {
            i = 8;
        } else {
            i = 0;
        }
        view.setVisibility(i);
        View view2 = this.mNotificationButton;
        if (!settingsVisible) {
            i2 = 8;
        }
        view2.setVisibility(i2);
    }

    public boolean isInContentArea(int x, int y) {
        this.mContentArea.left = this.mContentFrame.getLeft() + this.mContentFrame.getPaddingLeft();
        this.mContentArea.top = (this.mContentFrame.getTop() + this.mContentFrame.getPaddingTop()) + ((int) this.mContentParent.getTranslationY());
        this.mContentArea.right = this.mContentFrame.getRight() - this.mContentFrame.getPaddingRight();
        this.mContentArea.bottom = this.mContentFrame.getBottom() - this.mContentFrame.getPaddingBottom();
        offsetDescendantRectToMyCoords(this.mContentParent, this.mContentArea);
        return this.mContentArea.contains(x, y);
    }

    void removeSettingsView() {
        if (this.mSettingsView != null) {
            this.mContentFrame.removeView(this.mSettingsView);
            this.mSettingsView = null;
        }
    }

    void addSettingsView() {
        this.mSettingsView = LayoutInflater.from(getContext()).inflate(2130903071, this.mContentFrame, false);
        this.mSettingsView.setVisibility(8);
        this.mContentFrame.addView(this.mSettingsView);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        MotionEvent cancellation = MotionEvent.obtain(ev);
        cancellation.setAction(3);
        boolean intercept = this.mExpandHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
        if (intercept) {
            this.latestItems.onInterceptTouchEvent(cancellation);
        }
        return intercept;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mExpandHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    public void setSettingsEnabled(boolean settingsEnabled) {
        if (this.mSettingsButton != null) {
            this.mSettingsButton.setEnabled(settingsEnabled);
            this.mSettingsButton.setVisibility(settingsEnabled ? 0 : 8);
        }
    }
}
