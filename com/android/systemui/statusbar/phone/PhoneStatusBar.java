package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.wifi.WifiManager;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.tw.john.TWUtil;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.view.Choreographer;
import android.view.Display;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerImpl;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarNotification;
import com.android.systemui.recent.RecentTasksLoader;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.RotationToggle;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.DateView;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NotificationRowLayout;
import com.android.systemui.statusbar.policy.OnSizeChangedListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class PhoneStatusBar extends BaseStatusBar {
    int[] mAbsPos;
    float mAnimAccel;
    long mAnimLastTimeNanos;
    float mAnimVel;
    float mAnimY;
    boolean mAnimating;
    boolean mAnimatingReveal;
    final Runnable mAnimationCallback;
    BatteryController mBatteryController;
    private BroadcastReceiver mBroadcastReceiver;
    private TextView mCarrierLabel;
    private int mCarrierLabelHeight;
    private boolean mCarrierLabelVisible;
    Choreographer mChoreographer;
    View mClearButton;
    private OnClickListener mClearButtonListener;
    CloseDragHandle mCloseView;
    private int mCloseViewHeight;
    boolean mClosing;
    private float mCollapseAccelPx;
    private float mCollapseMinDisplayFraction;
    DateView mDateView;
    int mDisabled;
    Display mDisplay;
    DisplayMetrics mDisplayMetrics;
    int mEdgeBorder;
    int mEdgeBorder2;
    private float mExpandAccelPx;
    private float mExpandMinDisplayFraction;
    boolean mExpanded;
    View mExpandedContents;
    boolean mExpandedVisible;
    private float mFlingCollapseMinVelocityPx;
    private float mFlingExpandMinVelocityPx;
    private float mFlingGestureMaxOutputVelocityPx;
    private float mFlingGestureMaxXVelocityPx;
    float mFlingVelocity;
    int mFlingY;
    OnFocusChangeListener mFocusChangeListener;
    OnTouchListener mHomeSearchActionListener;
    int mIconHPadding;
    PhoneStatusBarPolicy mIconPolicy;
    int mIconSize;
    LinearLayout mIcons;
    private AnimatorSet mLightsOnAnimation;
    private AnimatorSet mLightsOutAnimation;
    LocationController mLocationController;
    private final AnimatorListener mMakeIconsInvisible;
    View mMoreIcon;
    int mNaturalBarHeight;
    private NavigationBarView mNavigationBarView;
    private int mNavigationIconHints;
    NetworkController mNetworkController;
    IconMerger mNotificationIcons;
    View mNotificationPanel;
    final Rect mNotificationPanelBackgroundPadding;
    int mNotificationPanelGravity;
    boolean mNotificationPanelIsFullScreenWidth;
    int mNotificationPanelMarginBottomPx;
    int mNotificationPanelMarginLeftPx;
    int mNotificationPanelMinHeight;
    private final Runnable mPerformFling;
    private final Runnable mPerformSelfExpandFling;
    int mPixelFormat;
    int[] mPositionTmp;
    Runnable mPostCollapseCleanup;
    Object mQueueLock;
    private OnClickListener mRecentsClickListener;
    final Runnable mRevealAnimationCallback;
    RotationToggle mRotationButton;
    ScrollView mScrollView;
    private float mSelfCollapseVelocityPx;
    private float mSelfExpandVelocityPx;
    View mSettingsButton;
    private OnClickListener mSettingsButtonListener;
    private int mShowSearchHoldoff;
    private Runnable mShowSearchPanel;
    private final Runnable mStartRevealAnimation;
    Runnable mStartTracing;
    PhoneStatusBarView mStatusBarView;
    StatusBarWindowView mStatusBarWindow;
    LinearLayout mStatusIcons;
    Runnable mStopTracing;
    int mSystemUiVisibility;
    private TWUtil mTWUtil;
    private Ticker mTicker;
    private View mTickerView;
    private boolean mTicking;
    AnimationListener mTickingDoneListener;
    boolean mTracking;
    int mTrackingPosition;
    VelocityTracker mVelocityTracker;
    int mViewDelta;
    private ImageView mVolumeIcon;
    boolean mVolumeMute;
    private SeekBar mVolumeSeekBar;
    private TextView mVolumeText;
    boolean mWifiEnabled;
    private ImageView mWifiIcon;
    IWindowManager mWindowManager;

    class AnonymousClass_16 extends AnimatorListenerAdapter {
        final /* synthetic */ View val$nlo;

        AnonymousClass_16(View view) {
            this.val$nlo = view;
        }

        public void onAnimationEnd(Animator _a) {
            this.val$nlo.setVisibility(8);
        }
    }

    class AnonymousClass_9 implements OnClickListener {
        final /* synthetic */ Context val$context;

        AnonymousClass_9(Context context) {
            this.val$context = context;
        }

        public void onClick(View v) {
            int i = 1;
            int progress;
            switch (v.getId()) {
                case 2131492919:
                    boolean z;
                    WifiManager wifiManager = (WifiManager) this.val$context.getSystemService("wifi");
                    if (PhoneStatusBar.this.mWifiEnabled) {
                        z = false;
                    }
                    wifiManager.setWifiEnabled(z);
                case 2131492920:
                    if (PhoneStatusBar.this.mTWUtil != null) {
                        PhoneStatusBar.this.mTWUtil.write(33281, 1, 26);
                    }
                case 2131492921:
                    try {
                        if (!PhoneStatusBar.this.mAnimating) {
                            PhoneStatusBar.this.animateCollapse();
                        }
                        Intent it = new Intent();
                        it.setClassName("com.android.settings", "com.android.settings.NaviActivity");
                        it.setFlags(268435456);
                        this.val$context.startActivity(it);
                    } catch (Exception e) {
                    }
                case 2131492922:
                    if (!PhoneStatusBar.this.mAnimating) {
                        PhoneStatusBar.this.animateCollapse();
                    }
                    if (PhoneStatusBar.this.mTWUtil != null) {
                        PhoneStatusBar.this.mTWUtil.write(33281, 1, 83);
                    }
                case 2131492923:
                    if (PhoneStatusBar.this.mTWUtil != null) {
                        TWUtil access$300 = PhoneStatusBar.this.mTWUtil;
                        if (PhoneStatusBar.this.mVolumeMute) {
                            i = 0;
                        }
                        access$300.write(515, 0, i);
                    }
                case 2131492924:
                    progress = PhoneStatusBar.this.mVolumeSeekBar.getProgress() - 1;
                    if (progress < 0) {
                        progress = 0;
                    }
                    if (PhoneStatusBar.this.mTWUtil != null) {
                        PhoneStatusBar.this.mTWUtil.write(515, 1, progress);
                    }
                case 2131492926:
                    progress = PhoneStatusBar.this.mVolumeSeekBar.getProgress() + 1;
                    int max = PhoneStatusBar.this.mVolumeSeekBar.getMax();
                    if (progress > max) {
                        progress = max;
                    }
                    if (PhoneStatusBar.this.mTWUtil != null) {
                        PhoneStatusBar.this.mTWUtil.write(515, 1, progress);
                    }
                default:
                    break;
            }
        }
    }

    private static class FastColorDrawable extends Drawable {
        private final int mColor;

        public FastColorDrawable(int color) {
            this.mColor = -16777216 | color;
        }

        public void draw(Canvas canvas) {
            canvas.drawColor(this.mColor, Mode.SRC);
        }

        public void setAlpha(int alpha) {
        }

        public void setColorFilter(ColorFilter cf) {
        }

        public int getOpacity() {
            return -1;
        }

        public void setBounds(int left, int top, int right, int bottom) {
        }

        public void setBounds(Rect bounds) {
        }
    }

    private class H extends H {
        private H() {
            super();
        }

        public void handleMessage(Message m) {
            super.handleMessage(m);
            switch (m.what) {
                case 1000:
                    PhoneStatusBar.this.animateExpand();
                case 1001:
                    PhoneStatusBar.this.animateCollapse();
                case 1026:
                    PhoneStatusBar.this.setIntruderAlertVisibility(true);
                case 1027:
                    PhoneStatusBar.this.setIntruderAlertVisibility(false);
                    PhoneStatusBar.this.mCurrentlyIntrudingNotification = null;
                default:
                    break;
            }
        }
    }

    private class MyTicker extends Ticker {
        MyTicker(Context context, View sb) {
            super(context, sb);
        }

        public void tickerStarting() {
            PhoneStatusBar.this.mTicking = true;
            PhoneStatusBar.this.mIcons.setVisibility(8);
            PhoneStatusBar.this.mTickerView.setVisibility(0);
            PhoneStatusBar.this.mTickerView.startAnimation(PhoneStatusBar.this.loadAnim(17432625, null));
            PhoneStatusBar.this.mIcons.startAnimation(PhoneStatusBar.this.loadAnim(17432626, null));
        }

        public void tickerDone() {
            PhoneStatusBar.this.mIcons.setVisibility(0);
            PhoneStatusBar.this.mTickerView.setVisibility(8);
            PhoneStatusBar.this.mIcons.startAnimation(PhoneStatusBar.this.loadAnim(17432621, null));
            PhoneStatusBar.this.mTickerView.startAnimation(PhoneStatusBar.this.loadAnim(17432623, PhoneStatusBar.this.mTickingDoneListener));
        }

        public void tickerHalting() {
            PhoneStatusBar.this.mIcons.setVisibility(0);
            PhoneStatusBar.this.mTickerView.setVisibility(8);
            PhoneStatusBar.this.mIcons.startAnimation(PhoneStatusBar.this.loadAnim(17432576, null));
            PhoneStatusBar.this.mTickerView.startAnimation(PhoneStatusBar.this.loadAnim(17432577, PhoneStatusBar.this.mTickingDoneListener));
        }
    }

    public PhoneStatusBar() {
        this.mNaturalBarHeight = -1;
        this.mIconSize = -1;
        this.mIconHPadding = -1;
        this.mQueueLock = new Object();
        this.mNotificationPanelBackgroundPadding = new Rect();
        this.mCarrierLabelVisible = false;
        this.mPositionTmp = new int[2];
        this.mNavigationBarView = null;
        this.mAnimatingReveal = false;
        this.mAbsPos = new int[2];
        this.mPostCollapseCleanup = null;
        this.mDisabled = 0;
        this.mSystemUiVisibility = 0;
        this.mDisplayMetrics = new DisplayMetrics();
        this.mNavigationIconHints = 0;
        this.mMakeIconsInvisible = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (PhoneStatusBar.this.mIcons.getAlpha() == 0.0f) {
                    Slog.d("PhoneStatusBar", "makeIconsInvisible");
                    PhoneStatusBar.this.mIcons.setVisibility(4);
                }
            }
        };
        this.mStartRevealAnimation = new Runnable() {
            public void run() {
                PhoneStatusBar.this.mAnimAccel = PhoneStatusBar.this.mExpandAccelPx;
                PhoneStatusBar.this.mAnimVel = PhoneStatusBar.this.mFlingExpandMinVelocityPx;
                PhoneStatusBar.this.mAnimY = (float) PhoneStatusBar.this.getStatusBarHeight();
                PhoneStatusBar.this.updateExpandedViewPos((int) PhoneStatusBar.this.mAnimY);
                PhoneStatusBar.this.mAnimating = true;
                PhoneStatusBar.this.mAnimatingReveal = true;
                PhoneStatusBar.this.resetLastAnimTime();
                PhoneStatusBar.this.mChoreographer.removeCallbacks(1, PhoneStatusBar.this.mAnimationCallback, null);
                PhoneStatusBar.this.mChoreographer.removeCallbacks(1, PhoneStatusBar.this.mRevealAnimationCallback, null);
                PhoneStatusBar.this.mChoreographer.postCallback(1, PhoneStatusBar.this.mRevealAnimationCallback, null);
            }
        };
        this.mPerformSelfExpandFling = new Runnable() {
            public void run() {
                PhoneStatusBar.this.performFling(0, PhoneStatusBar.this.mSelfExpandVelocityPx, true);
            }
        };
        this.mPerformFling = new Runnable() {
            public void run() {
                PhoneStatusBar.this.performFling(PhoneStatusBar.this.mFlingY + PhoneStatusBar.this.mViewDelta, PhoneStatusBar.this.mFlingVelocity, false);
            }
        };
        this.mTWUtil = null;
        this.mVolumeIcon = null;
        this.mVolumeMute = false;
        this.mVolumeSeekBar = null;
        this.mVolumeText = null;
        this.mWifiIcon = null;
        this.mWifiEnabled = false;
        this.mRecentsClickListener = new OnClickListener() {
            public void onClick(View v) {
                PhoneStatusBar.this.toggleRecentApps();
            }
        };
        this.mShowSearchHoldoff = 0;
        this.mShowSearchPanel = new Runnable() {
            public void run() {
                PhoneStatusBar.this.showSearchPanel();
            }
        };
        this.mHomeSearchActionListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case 0:
                        if (!(PhoneStatusBar.this.shouldDisableNavbarGestures() || PhoneStatusBar.this.inKeyguardRestrictedInputMode())) {
                            PhoneStatusBar.this.mHandler.removeCallbacks(PhoneStatusBar.this.mShowSearchPanel);
                            PhoneStatusBar.this.mHandler.postDelayed(PhoneStatusBar.this.mShowSearchPanel, (long) PhoneStatusBar.this.mShowSearchHoldoff);
                        }
                        break;
                    case 1:
                    case 3:
                        PhoneStatusBar.this.mHandler.removeCallbacks(PhoneStatusBar.this.mShowSearchPanel);
                        break;
                }
                return false;
            }
        };
        this.mAnimationCallback = new Runnable() {
            public void run() {
                PhoneStatusBar.this.doAnimation(PhoneStatusBar.this.mChoreographer.getFrameTimeNanos());
            }
        };
        this.mRevealAnimationCallback = new Runnable() {
            public void run() {
                PhoneStatusBar.this.doRevealAnimation(PhoneStatusBar.this.mChoreographer.getFrameTimeNanos());
            }
        };
        this.mFocusChangeListener = new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                v.setSelected(hasFocus);
            }
        };
        this.mTickingDoneListener = new AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                PhoneStatusBar.this.mTicking = false;
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        };
        this.mClearButtonListener = new OnClickListener() {

            class AnonymousClass_1 implements Runnable {
                final /* synthetic */ ArrayList val$snapshot;

                class AnonymousClass_2 implements Runnable {
                    final /* synthetic */ View val$_v;
                    final /* synthetic */ int val$velocity;

                    AnonymousClass_2(View view, int i) {
                        this.val$_v = view;
                        this.val$velocity = i;
                    }

                    public void run() {
                        PhoneStatusBar.this.mPile.dismissRowAnimated(this.val$_v, this.val$velocity);
                    }
                }

                AnonymousClass_1(ArrayList arrayList) {
                    this.val$snapshot = arrayList;
                }

                public void run() {
                    int currentDelay = 140;
                    int totalDelay = 0;
                    PhoneStatusBar.this.mPile.setViewRemoval(false);
                    PhoneStatusBar.this.mPostCollapseCleanup = new Runnable() {
                        public void run() {
                            try {
                                PhoneStatusBar.this.mPile.setViewRemoval(true);
                                PhoneStatusBar.this.mBarService.onClearAllNotifications();
                            } catch (Exception e) {
                            }
                        }
                    };
                    int velocity = ((View) this.val$snapshot.get(0)).getWidth() * 8;
                    Iterator i$ = this.val$snapshot.iterator();
                    while (i$.hasNext()) {
                        PhoneStatusBar.this.mHandler.postDelayed(new AnonymousClass_2((View) i$.next(), velocity), (long) totalDelay);
                        currentDelay = Math.max(50, currentDelay - 10);
                        totalDelay += currentDelay;
                    }
                    PhoneStatusBar.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            PhoneStatusBar.this.animateCollapse(0);
                        }
                    }, (long) (totalDelay + 225));
                }
            }

            public void onClick(View v) {
                synchronized (PhoneStatusBar.this.mNotificationData) {
                    int numChildren = PhoneStatusBar.this.mPile.getChildCount();
                    int scrollTop = PhoneStatusBar.this.mScrollView.getScrollY();
                    int scrollBottom = scrollTop + PhoneStatusBar.this.mScrollView.getHeight();
                    ArrayList<View> snapshot = new ArrayList(numChildren);
                    for (int i = 0; i < numChildren; i++) {
                        View child = PhoneStatusBar.this.mPile.getChildAt(i);
                        if (PhoneStatusBar.this.mPile.canChildBeDismissed(child) && child.getBottom() > scrollTop && child.getTop() < scrollBottom) {
                            snapshot.add(child);
                        }
                    }
                    if (snapshot.isEmpty()) {
                        PhoneStatusBar.this.animateCollapse(0);
                        return;
                    }
                    new Thread(new AnonymousClass_1(snapshot)).start();
                }
            }
        };
        this.mSettingsButtonListener = new OnClickListener() {
            public void onClick(View v) {
                if (PhoneStatusBar.this.isDeviceProvisioned()) {
                    try {
                        ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
                    } catch (RemoteException e) {
                    }
                    v.getContext().startActivity(new Intent("android.settings.SETTINGS").setFlags(268435456));
                    PhoneStatusBar.this.animateCollapse();
                }
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int i = 1;
                String action = intent.getAction();
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) || "android.intent.action.SCREEN_OFF".equals(action)) {
                    int flags = 0;
                    if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                        String reason = intent.getStringExtra("reason");
                        if (reason != null && reason.equals("recentapps")) {
                            flags = 0 | 2;
                        }
                    }
                    PhoneStatusBar.this.animateCollapse(flags);
                } else if ("android.intent.action.CONFIGURATION_CHANGED".equals(action)) {
                    PhoneStatusBar.this.updateResources();
                    PhoneStatusBar.this.repositionNavigationBar();
                    PhoneStatusBar.this.updateExpandedViewPos(-10000);
                } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    boolean z;
                    int i2;
                    PhoneStatusBar phoneStatusBar = PhoneStatusBar.this;
                    if (intent.getIntExtra("wifi_state", 4) == 3) {
                        z = true;
                    } else {
                        z = false;
                    }
                    phoneStatusBar.mWifiEnabled = z;
                    Drawable drawable = PhoneStatusBar.this.mWifiIcon.getDrawable();
                    if (PhoneStatusBar.this.mWifiEnabled) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    drawable.setLevel(i2);
                    Drawable background = PhoneStatusBar.this.mWifiIcon.getBackground();
                    if (!PhoneStatusBar.this.mWifiEnabled) {
                        i = 0;
                    }
                    background.setLevel(i);
                } else if ("com.android.internal.policy.statusbar.CMD".equals(action)) {
                    String key = intent.getStringExtra("key");
                    if ("recent_apps".equals(key)) {
                        PhoneStatusBar.this.toggleRecentApps();
                    } else if (!"home".equals(key) && !"back".equals(key) && "menu".equals(key)) {
                    }
                }
            }
        };
        this.mStartTracing = new Runnable() {
            public void run() {
                PhoneStatusBar.this.vibrate();
                SystemClock.sleep(250);
                Slog.d("PhoneStatusBar", "startTracing");
                Debug.startMethodTracing("/data/statusbar-traces/trace");
                PhoneStatusBar.this.mHandler.postDelayed(PhoneStatusBar.this.mStopTracing, 10000);
            }
        };
        this.mStopTracing = new Runnable() {
            public void run() {
                Debug.stopMethodTracing();
                Slog.d("PhoneStatusBar", "stopTracing");
                PhoneStatusBar.this.vibrate();
            }
        };
    }

    public void start() {
        this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        this.mWindowManager = Stub.asInterface(ServiceManager.getService("window"));
        super.start();
        addNavigationBar();
        this.mIconPolicy = new PhoneStatusBarPolicy(this.mContext);
    }

    protected PhoneStatusBarView makeStatusBarView() {
        Context context = this.mContext;
        Resources resources = context.getResources();
        updateDisplaySize();
        loadDimens();
        this.mIconSize = resources.getDimensionPixelSize(17104912);
        this.mStatusBarWindow = (StatusBarWindowView) View.inflate(context, 2130903057, null);
        this.mStatusBarWindow.mService = this;
        this.mStatusBarWindow.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == 0 && PhoneStatusBar.this.mExpanded && !PhoneStatusBar.this.mAnimating) {
                    PhoneStatusBar.this.animateCollapse();
                }
                return PhoneStatusBar.this.mStatusBarWindow.onTouchEvent(event);
            }
        });
        this.mStatusBarView = (PhoneStatusBarView) this.mStatusBarWindow.findViewById(2131492899);
        this.mNotificationPanel = this.mStatusBarWindow.findViewById(2131492917);
        this.mNotificationPanel.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        this.mStatusBarView.mRecentView.setOnClickListener(this.mRecentsClickListener);
        this.mStatusBarView.mRecentView.setOnTouchListener(this.mRecentsPanel);
        this.mNotificationPanelIsFullScreenWidth = this.mNotificationPanel.getLayoutParams().width == -1;
        this.mNotificationPanel.setSystemUiVisibility((this.mNotificationPanelIsFullScreenWidth ? 0 : 1048576) | 524288);
        if (!ActivityManager.isHighEndGfx(this.mDisplay)) {
            this.mStatusBarWindow.setBackground(null);
            this.mNotificationPanel.setBackground(new FastColorDrawable(context.getResources().getColor(2131165184)));
        }
        updateShowSearchHoldoff();
        this.mStatusBarView.mService = this;
        this.mChoreographer = Choreographer.getInstance();
        try {
            if (this.mWindowManager.hasNavigationBar()) {
                this.mNavigationBarView = (NavigationBarView) View.inflate(context, 2130903044, null);
                this.mNavigationBarView.setDisabledFlags(this.mDisabled);
                this.mNavigationBarView.setBar(this);
            }
        } catch (RemoteException e) {
        }
        this.mPixelFormat = -1;
        this.mStatusIcons = (LinearLayout) this.mStatusBarView.findViewById(2131492907);
        this.mNotificationIcons = (IconMerger) this.mStatusBarView.findViewById(2131492906);
        this.mNotificationIcons.setOverflowIndicator(this.mMoreIcon);
        this.mIcons = (LinearLayout) this.mStatusBarView.findViewById(2131492901);
        this.mTickerView = this.mStatusBarView.findViewById(2131492914);
        this.mPile = (NotificationRowLayout) this.mStatusBarWindow.findViewById(2131492929);
        this.mPile.setLayoutTransitionsEnabled(false);
        this.mPile.setLongPressListener(getNotificationLongClicker());
        this.mPile.setOnSizeChangedListener(new OnSizeChangedListener() {
            public void onSizeChanged(View view, int w, int h, int oldw, int oldh) {
                PhoneStatusBar.this.updateCarrierLabelVisibility(false);
            }
        });
        this.mExpandedContents = this.mPile;
        this.mClearButton = this.mStatusBarWindow.findViewById(2131492934);
        this.mClearButton.setOnClickListener(this.mClearButtonListener);
        this.mClearButton.setAlpha(0.0f);
        this.mClearButton.setVisibility(4);
        this.mClearButton.setEnabled(false);
        this.mDateView = (DateView) this.mStatusBarWindow.findViewById(2131492931);
        this.mSettingsButton = this.mStatusBarWindow.findViewById(2131492933);
        this.mSettingsButton.setOnClickListener(this.mSettingsButtonListener);
        this.mRotationButton = (RotationToggle) this.mStatusBarWindow.findViewById(2131492932);
        this.mCarrierLabel = (TextView) this.mStatusBarWindow.findViewById(2131492918);
        this.mCarrierLabel.setVisibility(this.mCarrierLabelVisible ? 0 : 4);
        this.mScrollView = (ScrollView) this.mStatusBarWindow.findViewById(2131492928);
        this.mScrollView.setVerticalScrollBarEnabled(false);
        this.mTicker = new MyTicker(context, this.mStatusBarView);
        ((TickerView) this.mStatusBarView.findViewById(2131492916)).mTicker = this.mTicker;
        this.mCloseView = (CloseDragHandle) this.mStatusBarWindow.findViewById(2131492930);
        this.mCloseView.mService = this;
        this.mCloseViewHeight = resources.getDimensionPixelSize(2131427369);
        this.mEdgeBorder = resources.getDimensionPixelSize(2131427328);
        this.mEdgeBorder2 = resources.getDimensionPixelSize(2131427329);
        setAreThereNotifications();
        this.mLocationController = new LocationController(this.mContext);
        this.mBatteryController = new BatteryController(this.mContext);
        this.mBatteryController.addIconView((ImageView) this.mStatusBarView.findViewById(2131492910));
        this.mNetworkController = new NetworkController(this.mContext);
        SignalClusterView signalClusterView = (SignalClusterView) this.mStatusBarView.findViewById(2131492909);
        this.mNetworkController.addSignalCluster(signalClusterView);
        signalClusterView.setNetworkController(this.mNetworkController);
        if (this.mNetworkController.hasMobileDataFeature()) {
            this.mNetworkController.addMobileLabelView(this.mCarrierLabel);
        } else {
            this.mNetworkController.addCombinedLabelView(this.mCarrierLabel);
        }
        this.mRecentTasksLoader = new RecentTasksLoader(context);
        updateRecentsPanel();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("com.android.internal.policy.statusbar.CMD");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mVolumeText = (TextView) this.mStatusBarWindow.findViewById(2131492927);
        this.mVolumeSeekBar = (SeekBar) this.mStatusBarWindow.findViewById(2131492925);
        this.mVolumeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && PhoneStatusBar.this.mTWUtil != null) {
                    PhoneStatusBar.this.mTWUtil.write(515, 1, progress);
                }
            }
        });
        OnClickListener anonymousClass_9 = new AnonymousClass_9(context);
        this.mVolumeIcon = (ImageView) this.mStatusBarWindow.findViewById(2131492923);
        this.mVolumeIcon.setOnClickListener(anonymousClass_9);
        this.mStatusBarWindow.findViewById(2131492924).setOnClickListener(anonymousClass_9);
        this.mStatusBarWindow.findViewById(2131492926).setOnClickListener(anonymousClass_9);
        this.mWifiIcon = (ImageView) this.mStatusBarWindow.findViewById(2131492919);
        this.mWifiIcon.setOnClickListener(anonymousClass_9);
        this.mStatusBarWindow.findViewById(2131492920).setOnClickListener(anonymousClass_9);
        this.mStatusBarWindow.findViewById(2131492921).setOnClickListener(anonymousClass_9);
        this.mStatusBarWindow.findViewById(2131492922).setOnClickListener(anonymousClass_9);
        this.mTWUtil = new TWUtil();
        if (this.mTWUtil.open(new short[]{(short) 515}) == 0) {
            this.mTWUtil.start();
            this.mTWUtil.addHandler("PhoneStatusBar", new Handler() {
                public void handleMessage(Message msg) {
                    int i = 1;
                    try {
                        switch (msg.what) {
                            case 515:
                                PhoneStatusBar.this.mVolumeMute = (msg.arg1 & Integer.MIN_VALUE) == Integer.MIN_VALUE;
                                Drawable drawable = PhoneStatusBar.this.mVolumeIcon.getDrawable();
                                if (!PhoneStatusBar.this.mVolumeMute) {
                                    i = 0;
                                }
                                drawable.setLevel(i);
                                PhoneStatusBar.this.mVolumeSeekBar.setMax(msg.arg2 & Integer.MAX_VALUE);
                                PhoneStatusBar.this.mVolumeSeekBar.setProgress(msg.arg1 & Integer.MAX_VALUE);
                                PhoneStatusBar.this.mVolumeText.setText(String.format(Locale.US, "%d", new Object[]{Integer.valueOf(level)}));
                            default:
                                break;
                        }
                    } catch (Exception e) {
                    }
                }
            });
            this.mTWUtil.write(515, 255);
        } else {
            this.mTWUtil.close();
            this.mTWUtil = null;
        }
        return this.mStatusBarView;
    }

    protected LayoutParams getRecentsLayoutParams(ViewGroup.LayoutParams layoutParams) {
        LayoutParams layoutParams2 = new LayoutParams(layoutParams.width, layoutParams.height, 2014, 8519936, -3);
        if (ActivityManager.isHighEndGfx(this.mDisplay)) {
            layoutParams2.flags |= 16777216;
        } else {
            layoutParams2.flags |= 2;
            layoutParams2.dimAmount = 0.75f;
        }
        layoutParams2.gravity = 83;
        layoutParams2.setTitle("RecentsPanel");
        layoutParams2.windowAnimations = 16974313;
        layoutParams2.softInputMode = 49;
        return layoutParams2;
    }

    protected LayoutParams getSearchLayoutParams(ViewGroup.LayoutParams layoutParams) {
        LayoutParams layoutParams2 = new LayoutParams(-1, -1, 2024, 8519936, -3);
        if (ActivityManager.isHighEndGfx(this.mDisplay)) {
            layoutParams2.flags |= 16777216;
        }
        layoutParams2.gravity = 83;
        layoutParams2.setTitle("SearchPanel");
        layoutParams2.windowAnimations = 16974313;
        layoutParams2.softInputMode = 49;
        return layoutParams2;
    }

    protected void updateRecentsPanel() {
        super.updateRecentsPanel(2130903054);
        this.mRecentsPanel.setMinSwipeAlpha(0.03f);
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.getRecentsButton().setOnTouchListener(this.mRecentsPanel);
        }
    }

    protected void updateSearchPanel() {
        super.updateSearchPanel();
        this.mSearchPanelView.setStatusBarView(this.mNavigationBarView);
        this.mNavigationBarView.setDelegateView(this.mSearchPanelView);
    }

    public void showSearchPanel() {
        super.showSearchPanel();
        LayoutParams lp = (LayoutParams) this.mNavigationBarView.getLayoutParams();
        lp.flags &= -33;
        WindowManagerImpl.getDefault().updateViewLayout(this.mNavigationBarView, lp);
    }

    public void hideSearchPanel() {
        super.hideSearchPanel();
        LayoutParams lp = (LayoutParams) this.mNavigationBarView.getLayoutParams();
        lp.flags |= 32;
        WindowManagerImpl.getDefault().updateViewLayout(this.mNavigationBarView, lp);
    }

    protected int getStatusBarGravity() {
        return 55;
    }

    public int getStatusBarHeight() {
        if (this.mNaturalBarHeight < 0) {
            this.mNaturalBarHeight = this.mContext.getResources().getDimensionPixelSize(17104906);
        }
        return this.mNaturalBarHeight;
    }

    private int getCloseViewHeight() {
        return this.mCloseViewHeight;
    }

    private void prepareNavigationBarView() {
        this.mNavigationBarView.reorient();
        this.mNavigationBarView.getRecentsButton().setOnClickListener(this.mRecentsClickListener);
        this.mNavigationBarView.getRecentsButton().setOnTouchListener(this.mRecentsPanel);
        this.mNavigationBarView.getHomeButton().setOnTouchListener(this.mHomeSearchActionListener);
        updateSearchPanel();
    }

    private void addNavigationBar() {
        if (this.mNavigationBarView != null) {
            prepareNavigationBarView();
            WindowManagerImpl.getDefault().addView(this.mNavigationBarView, getNavigationBarLayoutParams());
        }
    }

    private void repositionNavigationBar() {
        if (this.mNavigationBarView != null) {
            prepareNavigationBarView();
            WindowManagerImpl.getDefault().updateViewLayout(this.mNavigationBarView, getNavigationBarLayoutParams());
        }
    }

    private LayoutParams getNavigationBarLayoutParams() {
        LayoutParams layoutParams = new LayoutParams(-1, -1, 2019, 8388712, -3);
        if (ActivityManager.isHighEndGfx(this.mDisplay)) {
            layoutParams.flags |= 16777216;
        }
        layoutParams.setTitle("NavigationBar");
        layoutParams.windowAnimations = 0;
        return layoutParams;
    }

    public void addIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
        StatusBarIconView view = new StatusBarIconView(this.mContext, slot, null);
        view.set(icon);
        this.mStatusIcons.addView(view, viewIndex, new LinearLayout.LayoutParams(this.mIconSize, this.mIconSize));
    }

    public void updateIcon(String slot, int index, int viewIndex, StatusBarIcon old, StatusBarIcon icon) {
        ((StatusBarIconView) this.mStatusIcons.getChildAt(viewIndex)).set(icon);
    }

    public void removeIcon(String slot, int index, int viewIndex) {
        this.mStatusIcons.removeViewAt(viewIndex);
    }

    public void addNotification(IBinder iBinder, StatusBarNotification statusBarNotification) {
        Slog.d("PhoneStatusBar", "addNotification score=" + statusBarNotification.score);
        if (addNotificationViews(iBinder, statusBarNotification) != null) {
            try {
                ActivityManagerNative.getDefault().isTopActivityImmersive();
            } catch (RemoteException e) {
            }
            if (statusBarNotification.notification.fullScreenIntent != null) {
                Slog.d("PhoneStatusBar", "Notification has fullScreenIntent; sending fullScreenIntent");
                try {
                    statusBarNotification.notification.fullScreenIntent.send();
                } catch (CanceledException e2) {
                }
            } else if (this.mCurrentlyIntrudingNotification == null) {
                tick(null, statusBarNotification, true);
            }
            setAreThereNotifications();
            updateExpandedViewPos(-10000);
        }
    }

    public void removeNotification(IBinder key) {
        StatusBarNotification old = removeNotificationViews(key);
        if (old != null) {
            this.mTicker.removeEntry(old);
            updateExpandedViewPos(-10000);
            if (this.mNotificationData.size() == 0 && !this.mAnimating) {
                animateCollapse();
            }
        }
        setAreThereNotifications();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        updateRecentsPanel();
        updateShowSearchHoldoff();
    }

    private void updateShowSearchHoldoff() {
        this.mShowSearchHoldoff = this.mContext.getResources().getInteger(2131361793);
    }

    private void loadNotificationShade() {
        if (this.mPile != null) {
            int i;
            int N = this.mNotificationData.size();
            ArrayList<View> toShow = new ArrayList();
            boolean provisioned = isDeviceProvisioned();
            for (i = 0; i < N; i++) {
                Entry ent = this.mNotificationData.get((N - i) - 1);
                if (provisioned || showNotificationEvenIfUnprovisioned(ent.notification)) {
                    toShow.add(ent.row);
                }
            }
            ArrayList<View> toRemove = new ArrayList();
            for (i = 0; i < this.mPile.getChildCount(); i++) {
                View child = this.mPile.getChildAt(i);
                if (!toShow.contains(child)) {
                    toRemove.add(child);
                }
            }
            Iterator i$ = toRemove.iterator();
            while (i$.hasNext()) {
                this.mPile.removeView((View) i$.next());
            }
            for (i = 0; i < toShow.size(); i++) {
                View v = (View) toShow.get(i);
                if (v.getParent() == null) {
                    this.mPile.addView(v, i);
                }
            }
            this.mSettingsButton.setEnabled(isDeviceProvisioned());
        }
    }

    protected void updateNotificationIcons() {
        if (this.mNotificationIcons != null) {
            int i;
            loadNotificationShade();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(this.mIconSize + (this.mIconHPadding * 2), this.mNaturalBarHeight);
            int N = this.mNotificationData.size();
            ArrayList<View> toShow = new ArrayList();
            boolean provisioned = isDeviceProvisioned();
            for (i = 0; i < N; i++) {
                Entry ent = this.mNotificationData.get((N - i) - 1);
                if ((provisioned && ent.notification.score >= -10) || showNotificationEvenIfUnprovisioned(ent.notification)) {
                    toShow.add(ent.icon);
                }
            }
            ArrayList<View> toRemove = new ArrayList();
            for (i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
                View child = this.mNotificationIcons.getChildAt(i);
                if (!toShow.contains(child)) {
                    toRemove.add(child);
                }
            }
            Iterator i$ = toRemove.iterator();
            while (i$.hasNext()) {
                this.mNotificationIcons.removeView((View) i$.next());
            }
            for (i = 0; i < toShow.size(); i++) {
                View v = (View) toShow.get(i);
                if (v.getParent() == null) {
                    this.mNotificationIcons.addView(v, i, params);
                }
            }
        }
    }

    protected void updateCarrierLabelVisibility(boolean force) {
        boolean makeVisible;
        if (this.mPile.getHeight() < this.mScrollView.getHeight() - this.mCarrierLabelHeight) {
            makeVisible = true;
        } else {
            makeVisible = false;
        }
        if (force || this.mCarrierLabelVisible != makeVisible) {
            float f;
            AnimatorListener animatorListener;
            this.mCarrierLabelVisible = makeVisible;
            this.mCarrierLabel.animate().cancel();
            if (makeVisible) {
                this.mCarrierLabel.setVisibility(0);
            }
            ViewPropertyAnimator animate = this.mCarrierLabel.animate();
            if (makeVisible) {
                f = 1.0f;
            } else {
                f = 0.0f;
            }
            animate = animate.alpha(f).setDuration(150);
            if (makeVisible) {
                animatorListener = null;
            } else {
                animatorListener = new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        if (!PhoneStatusBar.this.mCarrierLabelVisible) {
                            PhoneStatusBar.this.mCarrierLabel.setVisibility(4);
                            PhoneStatusBar.this.mCarrierLabel.setAlpha(0.0f);
                        }
                    }
                };
            }
            animate.setListener(animatorListener).start();
        }
    }

    protected void setAreThereNotifications() {
        boolean z;
        View view;
        float f = 1.0f;
        int i = 1;
        if (this.mNotificationData.size() > 0) {
            int i2 = 1;
        } else {
            boolean z2 = false;
        }
        if (i2 == 0 || !this.mNotificationData.hasClearableItems()) {
            z = false;
        } else {
            z = true;
        }
        float f2;
        if (this.mClearButton.isShown()) {
            boolean z3;
            if (this.mClearButton.getAlpha() == 1.0f) {
                int i3 = 1;
            } else {
                z3 = false;
            }
            if (z != z3) {
                view = this.mClearButton;
                String str = "alpha";
                float[] fArr = new float[1];
                if (z) {
                    f2 = 1.0f;
                } else {
                    f2 = 0.0f;
                }
                fArr[0] = f2;
                ObjectAnimator duration = ObjectAnimator.ofFloat(view, str, fArr).setDuration(250);
                duration.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        if (PhoneStatusBar.this.mClearButton.getAlpha() <= 0.0f) {
                            PhoneStatusBar.this.mClearButton.setVisibility(4);
                        }
                    }

                    public void onAnimationStart(Animator animation) {
                        if (PhoneStatusBar.this.mClearButton.getAlpha() <= 0.0f) {
                            PhoneStatusBar.this.mClearButton.setVisibility(0);
                        }
                    }
                });
                duration.start();
            }
        } else {
            view = this.mClearButton;
            if (z) {
                f2 = 1.0f;
            } else {
                f2 = 0.0f;
            }
            view.setAlpha(f2);
            this.mClearButton.setVisibility(z ? 0 : 4);
        }
        this.mClearButton.setEnabled(z);
        view = this.mStatusBarView.findViewById(2131492900);
        if (i2 == 0 || areLightsOn()) {
            z = false;
        } else {
            int i4 = 1;
        }
        if (view.getAlpha() != 1.0f) {
            i = 0;
        }
        if (i4 != i) {
            long j;
            AnimatorListener animatorListener;
            if (i4 != 0) {
                view.setAlpha(0.0f);
                view.setVisibility(0);
            }
            ViewPropertyAnimator animate = view.animate();
            if (i4 == 0) {
                f = 0.0f;
            }
            animate = animate.alpha(f);
            if (i4 != 0) {
                j = 750;
            } else {
                j = 250;
            }
            animate = animate.setDuration(j).setInterpolator(new AccelerateInterpolator(2.0f));
            if (i4 != 0) {
                animatorListener = null;
            } else {
                animatorListener = new AnonymousClass_16(view);
            }
            animate.setListener(animatorListener).start();
        }
        updateCarrierLabelVisibility(false);
    }

    public void showClock(boolean z) {
        if (this.mStatusBarView != null) {
            View findViewById = this.mStatusBarView.findViewById(2131492911);
            if (findViewById != null) {
                findViewById.setVisibility(z ? 0 : 8);
            }
        }
    }

    public void disable(int i) {
        int i2 = i ^ this.mDisabled;
        this.mDisabled = i;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disable: < ");
        stringBuilder.append((i & 65536) != 0 ? "EXPAND" : "expand");
        stringBuilder.append((i2 & 65536) != 0 ? "* " : " ");
        stringBuilder.append((i & 131072) != 0 ? "ICONS" : "icons");
        stringBuilder.append((i2 & 131072) != 0 ? "* " : " ");
        stringBuilder.append((262144 & i) != 0 ? "ALERTS" : "alerts");
        stringBuilder.append((262144 & i2) != 0 ? "* " : " ");
        stringBuilder.append((i & 524288) != 0 ? "TICKER" : "ticker");
        stringBuilder.append((i2 & 524288) != 0 ? "* " : " ");
        stringBuilder.append((i & 1048576) != 0 ? "SYSTEM_INFO" : "system_info");
        stringBuilder.append((i2 & 1048576) != 0 ? "* " : " ");
        stringBuilder.append((4194304 & i) != 0 ? "BACK" : "back");
        stringBuilder.append((4194304 & i2) != 0 ? "* " : " ");
        stringBuilder.append((2097152 & i) != 0 ? "HOME" : "home");
        stringBuilder.append((2097152 & i2) != 0 ? "* " : " ");
        stringBuilder.append((16777216 & i) != 0 ? "RECENT" : "recent");
        stringBuilder.append((16777216 & i2) != 0 ? "* " : " ");
        stringBuilder.append((i & 8388608) != 0 ? "CLOCK" : "clock");
        stringBuilder.append((i2 & 8388608) != 0 ? "* " : " ");
        stringBuilder.append(">");
        Slog.d("PhoneStatusBar", stringBuilder.toString());
        if ((i2 & 1048576) != 0) {
            this.mIcons.animate().cancel();
            if ((i & 1048576) != 0) {
                if (this.mTicking) {
                    this.mTicker.halt();
                }
                this.mIcons.animate().alpha(0.0f).translationY(((float) this.mNaturalBarHeight) * 0.5f).setDuration(175).setInterpolator(new DecelerateInterpolator(1.5f)).setListener(this.mMakeIconsInvisible).start();
            } else {
                this.mIcons.setVisibility(0);
                this.mIcons.animate().alpha(1.0f).translationY(0.0f).setStartDelay(0).setInterpolator(new DecelerateInterpolator(1.5f)).setDuration(175).start();
            }
        }
        if ((i2 & 8388608) != 0) {
            showClock((i & 8388608) == 0);
        }
        if (!((i2 & 65536) == 0 || (i & 65536) == 0)) {
            animateCollapse();
        }
        if ((23068672 & i2) != 0) {
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setDisabledFlags(i);
            }
            if ((16777216 & i) != 0) {
                this.mHandler.removeMessages(1021);
                this.mHandler.sendEmptyMessage(1021);
            }
        }
        if ((i2 & 131072) != 0) {
            if ((i & 131072) != 0) {
                if (this.mTicking) {
                    this.mTicker.halt();
                } else {
                    setNotificationIconVisibility(false, 17432577);
                }
            } else if (!this.mExpandedVisible) {
                setNotificationIconVisibility(true, 17432576);
            }
        } else if ((i2 & 524288) != 0 && this.mTicking && (i & 524288) != 0) {
            this.mTicker.halt();
        }
    }

    protected H createHandler() {
        return new H();
    }

    private void makeExpandedVisible(boolean z) {
        if (!this.mExpandedVisible) {
            this.mExpandedVisible = true;
            this.mPile.setLayoutTransitionsEnabled(true);
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setSlippery(true);
            }
            updateCarrierLabelVisibility(true);
            updateExpandedViewPos(-10000);
            LayoutParams layoutParams = (LayoutParams) this.mStatusBarWindow.getLayoutParams();
            layoutParams.flags &= -9;
            layoutParams.flags |= 131072;
            layoutParams.height = -1;
            WindowManagerImpl.getDefault().updateViewLayout(this.mStatusBarWindow, layoutParams);
            if (z) {
                this.mHandler.post(this.mStartRevealAnimation);
            }
            visibilityChanged(true);
        }
    }

    public void animateExpand() {
        if ((this.mDisabled & 65536) == 0 && !this.mExpanded) {
            prepareTracking(0, true);
            this.mHandler.post(this.mPerformSelfExpandFling);
        }
    }

    public void animateCollapse() {
        animateCollapse(0);
    }

    public void animateCollapse(int flags) {
        animateCollapse(flags, 1.0f);
    }

    public void animateCollapse(int flags, float velocityMultiplier) {
        if ((flags & 2) == 0) {
            this.mHandler.removeMessages(1021);
            this.mHandler.sendEmptyMessage(1021);
        }
        if ((flags & 1) == 0) {
            this.mHandler.removeMessages(1025);
            this.mHandler.sendEmptyMessage(1025);
        }
        if (this.mExpandedVisible) {
            int y;
            if (this.mAnimating) {
                y = (int) this.mAnimY;
            } else {
                y = getExpandedViewMaxHeight() - 1;
            }
            this.mExpanded = true;
            prepareTracking(y, false);
            performFling(y, (-this.mSelfCollapseVelocityPx) * velocityMultiplier, true);
        }
    }

    void performExpand() {
        if ((this.mDisabled & 65536) == 0 && !this.mExpanded) {
            this.mExpanded = true;
            makeExpandedVisible(false);
            updateExpandedViewPos(-10001);
        }
    }

    void performCollapse() {
        if (this.mExpandedVisible) {
            this.mExpandedVisible = false;
            this.mPile.setLayoutTransitionsEnabled(false);
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setSlippery(false);
            }
            visibilityChanged(false);
            LayoutParams layoutParams = (LayoutParams) this.mStatusBarWindow.getLayoutParams();
            layoutParams.height = getStatusBarHeight();
            layoutParams.flags |= 8;
            layoutParams.flags &= -131073;
            WindowManagerImpl.getDefault().updateViewLayout(this.mStatusBarWindow, layoutParams);
            if ((this.mDisabled & 131072) == 0) {
                setNotificationIconVisibility(true, 17432576);
            }
            if (this.mExpanded) {
                this.mExpanded = false;
                dismissPopups();
                if (this.mPostCollapseCleanup != null) {
                    this.mPostCollapseCleanup.run();
                    this.mPostCollapseCleanup = null;
                }
            }
        }
    }

    void resetLastAnimTime() {
        this.mAnimLastTimeNanos = System.nanoTime();
    }

    void doAnimation(long frameTimeNanos) {
        if (this.mAnimating) {
            incrementAnim(frameTimeNanos);
            if (this.mAnimY >= ((float) (getExpandedViewMaxHeight() - 1)) && !this.mClosing) {
                this.mAnimating = false;
                updateExpandedViewPos(-10001);
                performExpand();
            } else if (this.mAnimY == 0.0f && this.mAnimAccel == 0.0f && this.mClosing) {
                this.mAnimating = false;
                performCollapse();
            } else {
                if (this.mAnimY < ((float) getStatusBarHeight()) && this.mClosing) {
                    this.mAnimY = 0.0f;
                    this.mAnimAccel = 0.0f;
                    this.mAnimVel = 0.0f;
                }
                updateExpandedViewPos((int) this.mAnimY);
                this.mChoreographer.postCallback(1, this.mAnimationCallback, null);
            }
        }
    }

    void stopTracking() {
        if (this.mTracking) {
            this.mTracking = false;
            setPileLayers(0);
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
            this.mCloseView.setPressed(false);
        }
    }

    private void setPileLayers(int layerType) {
        int count = this.mPile.getChildCount();
        int i;
        switch (layerType) {
            case 0:
                for (i = 0; i < count; i++) {
                    this.mPile.getChildAt(i).setLayerType(layerType, null);
                }
            case 2:
                int[] location = new int[2];
                this.mNotificationPanel.getLocationInWindow(location);
                int left = location[0];
                int top = location[1];
                int right = left + this.mNotificationPanel.getWidth();
                int bottom = top + getExpandedViewMaxHeight();
                Rect childBounds = new Rect();
                for (i = 0; i < count; i++) {
                    View view = this.mPile.getChildAt(i);
                    view.getLocationInWindow(location);
                    childBounds.set(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
                    if (childBounds.intersects(left, top, right, bottom)) {
                        view.setLayerType(layerType, null);
                    }
                }
            default:
                break;
        }
    }

    void incrementAnim(long j) {
        float max = ((float) Math.max(j - this.mAnimLastTimeNanos, 0)) * 1.0E-9f;
        float f = this.mAnimY;
        float f2 = this.mAnimVel;
        float f3 = this.mAnimAccel;
        this.mAnimY = (f + (f2 * max)) + (((0.5f * f3) * max) * max);
        this.mAnimVel = (max * f3) + f2;
        this.mAnimLastTimeNanos = j;
    }

    void doRevealAnimation(long frameTimeNanos) {
        int h = this.mNotificationPanelMinHeight;
        if (this.mAnimatingReveal && this.mAnimating && this.mAnimY < ((float) h)) {
            incrementAnim(frameTimeNanos);
            if (this.mAnimY >= ((float) h)) {
                this.mAnimY = (float) h;
                updateExpandedViewPos((int) this.mAnimY);
                return;
            }
            updateExpandedViewPos((int) this.mAnimY);
            this.mChoreographer.postCallback(1, this.mRevealAnimationCallback, null);
        }
    }

    void prepareTracking(int y, boolean opening) {
        this.mCloseView.setPressed(true);
        this.mTracking = true;
        setPileLayers(2);
        this.mVelocityTracker = VelocityTracker.obtain();
        if (opening) {
            makeExpandedVisible(true);
            return;
        }
        if (this.mAnimating) {
            this.mAnimating = false;
            this.mChoreographer.removeCallbacks(1, this.mAnimationCallback, null);
        }
        updateExpandedViewPos(this.mViewDelta + y);
    }

    void performFling(int y, float vel, boolean always) {
        boolean z = false;
        this.mAnimatingReveal = false;
        this.mAnimY = (float) y;
        this.mAnimVel = vel;
        if (this.mExpanded) {
            if (always || (vel <= this.mFlingCollapseMinVelocityPx && (((float) y) <= ((float) getExpandedViewMaxHeight()) * (1.0f - this.mCollapseMinDisplayFraction) || vel <= (-this.mFlingExpandMinVelocityPx)))) {
                this.mAnimAccel = -this.mCollapseAccelPx;
                if (vel > 0.0f) {
                    this.mAnimVel = 0.0f;
                }
            } else {
                this.mAnimAccel = this.mExpandAccelPx;
                if (vel < 0.0f) {
                    this.mAnimVel = 0.0f;
                }
            }
        } else if (always || vel > this.mFlingExpandMinVelocityPx || (((float) y) > ((float) getExpandedViewMaxHeight()) * (1.0f - this.mExpandMinDisplayFraction) && vel > (-this.mFlingCollapseMinVelocityPx))) {
            this.mAnimAccel = this.mExpandAccelPx;
            if (vel < 0.0f) {
                this.mAnimVel = 0.0f;
            }
        } else {
            this.mAnimAccel = -this.mCollapseAccelPx;
            if (vel > 0.0f) {
                this.mAnimVel = 0.0f;
            }
        }
        resetLastAnimTime();
        this.mAnimating = true;
        if (this.mAnimAccel < 0.0f) {
            z = true;
        }
        this.mClosing = z;
        this.mChoreographer.removeCallbacks(1, this.mAnimationCallback, null);
        this.mChoreographer.removeCallbacks(1, this.mRevealAnimationCallback, null);
        this.mChoreographer.postCallback(1, this.mAnimationCallback, null);
        stopTracking();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean interceptTouchEvent(android.view.MotionEvent r9) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.PhoneStatusBar.interceptTouchEvent(android.view.MotionEvent):boolean");
        /*
        this = this;
        r6 = 0;
        r0 = 1;
        r1 = 0;
        r2 = r8.mDisabled;
        r3 = 65536; // 0x10000 float:9.18355E-41 double:3.2379E-319;
        r2 = r2 & r3;
        if (r2 == 0) goto L_0x000b;
    L_0x000a:
        return r1;
    L_0x000b:
        r2 = r9.getAction();
        r3 = r8.getStatusBarHeight();
        r4 = r3 * 2;
        r5 = r9.getRawY();
        r5 = (int) r5;
        if (r2 != 0) goto L_0x0093;
    L_0x001c:
        r2 = r8.areLightsOn();
        if (r2 != 0) goto L_0x0025;
    L_0x0022:
        r8.setLightsOn(r0);
    L_0x0025:
        r2 = r8.mExpanded;
        if (r2 != 0) goto L_0x003e;
    L_0x0029:
        if (r5 >= r4) goto L_0x003e;
    L_0x002b:
        r2 = r9.getRawX();
        r2 = (int) r2;
        r6 = r8.mEdgeBorder2;
        r6 = r6 * 2;
        if (r2 < r6) goto L_0x000a;
    L_0x0036:
        r7 = r8.mDisplayMetrics;
        r7 = r7.widthPixels;
        r6 = r7 - r6;
        if (r2 > r6) goto L_0x000a;
    L_0x003e:
        r2 = r8.mExpanded;
        if (r2 != 0) goto L_0x0073;
    L_0x0042:
        r2 = r3 - r5;
        r8.mViewDelta = r2;
    L_0x0046:
        r2 = r8.mExpanded;
        if (r2 != 0) goto L_0x004c;
    L_0x004a:
        if (r5 < r4) goto L_0x0057;
    L_0x004c:
        r2 = r8.mExpanded;
        if (r2 == 0) goto L_0x000a;
    L_0x0050:
        r2 = r8.getExpandedViewMaxHeight();
        r2 = r2 - r4;
        if (r5 <= r2) goto L_0x000a;
    L_0x0057:
        r2 = r9.getRawX();
        r2 = (int) r2;
        r3 = r8.mEdgeBorder;
        if (r2 < r3) goto L_0x000a;
    L_0x0060:
        r4 = r8.mDisplayMetrics;
        r4 = r4.widthPixels;
        r3 = r4 - r3;
        if (r2 >= r3) goto L_0x000a;
    L_0x0068:
        r2 = r8.mExpanded;
        if (r2 != 0) goto L_0x0091;
    L_0x006c:
        r8.prepareTracking(r5, r0);
        r8.trackMovement(r9);
        goto L_0x000a;
    L_0x0073:
        r2 = r8.mCloseView;
        r3 = r8.mAbsPos;
        r2.getLocationOnScreen(r3);
        r2 = r8.mAbsPos;
        r2 = r2[r0];
        r3 = r8.getCloseViewHeight();
        r2 = r2 + r3;
        r3 = r8.mNotificationPanelBackgroundPadding;
        r3 = r3.top;
        r2 = r2 + r3;
        r3 = r8.mNotificationPanelBackgroundPadding;
        r3 = r3.bottom;
        r2 = r2 + r3;
        r2 = r2 - r5;
        r8.mViewDelta = r2;
        goto L_0x0046;
    L_0x0091:
        r0 = r1;
        goto L_0x006c;
    L_0x0093:
        r3 = r8.mTracking;
        if (r3 == 0) goto L_0x000a;
    L_0x0097:
        r8.trackMovement(r9);
        r3 = 2;
        if (r2 != r3) goto L_0x00b2;
    L_0x009d:
        r0 = r8.mAnimatingReveal;
        if (r0 == 0) goto L_0x00a8;
    L_0x00a1:
        r0 = r8.mViewDelta;
        r0 = r0 + r5;
        r2 = r8.mNotificationPanelMinHeight;
        if (r0 < r2) goto L_0x000a;
    L_0x00a8:
        r8.mAnimatingReveal = r1;
        r0 = r8.mViewDelta;
        r0 = r0 + r5;
        r8.updateExpandedViewPos(r0);
        goto L_0x000a;
    L_0x00b2:
        if (r2 == r0) goto L_0x00b7;
    L_0x00b4:
        r3 = 3;
        if (r2 != r3) goto L_0x000a;
    L_0x00b7:
        r2 = r8.mVelocityTracker;
        r3 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r2.computeCurrentVelocity(r3);
        r2 = r8.mVelocityTracker;
        r3 = r2.getYVelocity();
        r2 = (r3 > r6 ? 1 : (r3 == r6 ? 0 : -1));
        if (r2 >= 0) goto L_0x0104;
    L_0x00c8:
        r2 = r8.mVelocityTracker;
        r2 = r2.getXVelocity();
        r4 = (r2 > r6 ? 1 : (r2 == r6 ? 0 : -1));
        if (r4 >= 0) goto L_0x00d3;
    L_0x00d2:
        r2 = -r2;
    L_0x00d3:
        r4 = r8.mFlingGestureMaxXVelocityPx;
        r4 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1));
        if (r4 <= 0) goto L_0x00db;
    L_0x00d9:
        r2 = r8.mFlingGestureMaxXVelocityPx;
    L_0x00db:
        r3 = (double) r3;
        r6 = (double) r2;
        r2 = java.lang.Math.hypot(r3, r6);
        r2 = (float) r2;
        r3 = r8.mFlingGestureMaxOutputVelocityPx;
        r3 = (r2 > r3 ? 1 : (r2 == r3 ? 0 : -1));
        if (r3 <= 0) goto L_0x00ea;
    L_0x00e8:
        r2 = r8.mFlingGestureMaxOutputVelocityPx;
    L_0x00ea:
        if (r0 == 0) goto L_0x0109;
    L_0x00ec:
        r0 = -r2;
    L_0x00ed:
        r2 = r8.mTrackingPosition;
        r3 = r8.mNotificationPanelMinHeight;
        if (r2 != r3) goto L_0x0106;
    L_0x00f3:
        r2 = r8.mTrackingPosition;
        r8.mFlingY = r2;
        r8.mViewDelta = r1;
    L_0x00f9:
        r8.mFlingVelocity = r0;
        r0 = r8.mHandler;
        r2 = r8.mPerformFling;
        r0.post(r2);
        goto L_0x000a;
    L_0x0104:
        r0 = r1;
        goto L_0x00c8;
    L_0x0106:
        r8.mFlingY = r5;
        goto L_0x00f9;
    L_0x0109:
        r0 = r2;
        goto L_0x00ed;
        */
    }

    private void trackMovement(MotionEvent event) {
        float deltaX = event.getRawX() - event.getX();
        float deltaY = event.getRawY() - event.getY();
        event.offsetLocation(deltaX, deltaY);
        this.mVelocityTracker.addMovement(event);
        event.offsetLocation(-deltaX, -deltaY);
    }

    public void setNavigationIconHints(int hints) {
        if (hints != this.mNavigationIconHints) {
            this.mNavigationIconHints = hints;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setNavigationIconHints(hints);
            }
        }
    }

    public void setSystemUiVisibility(int vis, int mask) {
        int oldVal = this.mSystemUiVisibility;
        int newVal = ((mask ^ -1) & oldVal) | (vis & mask);
        int diff = newVal ^ oldVal;
        if (diff != 0) {
            this.mSystemUiVisibility = newVal;
            if ((diff & 1) != 0) {
                boolean lightsOut = (vis & 1) != 0;
                if (lightsOut) {
                    animateCollapse();
                    if (this.mTicking) {
                        this.mTicker.halt();
                    }
                }
                if (this.mNavigationBarView != null) {
                    this.mNavigationBarView.setLowProfile(lightsOut);
                }
                setStatusBarLowProfile(lightsOut);
            }
            notifyUiVisibilityChanged();
        }
    }

    private void setStatusBarLowProfile(boolean z) {
        if (this.mLightsOutAnimation == null) {
            View findViewById = this.mStatusBarView.findViewById(2131492904);
            View findViewById2 = this.mStatusBarView.findViewById(2131492907);
            View findViewById3 = this.mStatusBarView.findViewById(2131492909);
            View findViewById4 = this.mStatusBarView.findViewById(2131492910);
            View findViewById5 = this.mStatusBarView.findViewById(2131492911);
            this.mLightsOutAnimation = new AnimatorSet();
            AnimatorSet animatorSet = this.mLightsOutAnimation;
            Animator[] animatorArr = new Animator[5];
            animatorArr[0] = ObjectAnimator.ofFloat(findViewById, View.ALPHA, new float[]{0.0f});
            animatorArr[1] = ObjectAnimator.ofFloat(findViewById2, View.ALPHA, new float[]{0.0f});
            animatorArr[2] = ObjectAnimator.ofFloat(findViewById3, View.ALPHA, new float[]{0.0f});
            animatorArr[3] = ObjectAnimator.ofFloat(findViewById4, View.ALPHA, new float[]{0.5f});
            animatorArr[4] = ObjectAnimator.ofFloat(findViewById5, View.ALPHA, new float[]{0.5f});
            animatorSet.playTogether(animatorArr);
            this.mLightsOutAnimation.setDuration(750);
            this.mLightsOnAnimation = new AnimatorSet();
            animatorSet = this.mLightsOnAnimation;
            animatorArr = new Animator[5];
            animatorArr[0] = ObjectAnimator.ofFloat(findViewById, View.ALPHA, new float[]{1.0f});
            animatorArr[1] = ObjectAnimator.ofFloat(findViewById2, View.ALPHA, new float[]{1.0f});
            animatorArr[2] = ObjectAnimator.ofFloat(findViewById3, View.ALPHA, new float[]{1.0f});
            animatorArr[3] = ObjectAnimator.ofFloat(findViewById4, View.ALPHA, new float[]{1.0f});
            animatorArr[4] = ObjectAnimator.ofFloat(findViewById5, View.ALPHA, new float[]{1.0f});
            animatorSet.playTogether(animatorArr);
            this.mLightsOnAnimation.setDuration(250);
        }
        this.mLightsOutAnimation.cancel();
        this.mLightsOnAnimation.cancel();
        (z ? this.mLightsOutAnimation : this.mLightsOnAnimation).start();
        setAreThereNotifications();
    }

    private boolean areLightsOn() {
        return (this.mSystemUiVisibility & 1) == 0;
    }

    public void setLightsOn(boolean z) {
        Log.v("PhoneStatusBar", "setLightsOn(" + z + ")");
        if (z) {
            setSystemUiVisibility(0, 1);
        } else {
            setSystemUiVisibility(1, 1);
        }
    }

    private void notifyUiVisibilityChanged() {
        try {
            this.mWindowManager.statusBarVisibilityChanged(this.mSystemUiVisibility);
        } catch (RemoteException e) {
        }
    }

    public void topAppWindowChanged(boolean showMenu) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setMenuVisibility(showMenu);
        }
        if (showMenu) {
            setLightsOn(true);
        }
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition) {
        boolean altBack = backDisposition == 2 || (vis & 2) != 0;
        this.mCommandQueue.setNavigationIconHints(altBack ? this.mNavigationIconHints | 8 : this.mNavigationIconHints & -9);
    }

    public void setHardKeyboardStatus(boolean available, boolean enabled) {
    }

    protected void tick(IBinder iBinder, StatusBarNotification statusBarNotification, boolean z) {
        if (areLightsOn() && isDeviceProvisioned() && statusBarNotification.notification.tickerText != null && this.mStatusBarWindow.getWindowToken() != null && (this.mDisabled & 655360) == 0) {
            this.mTicker.addEntry(statusBarNotification);
        }
    }

    private Animation loadAnim(int id, AnimationListener listener) {
        Animation anim = AnimationUtils.loadAnimation(this.mContext, id);
        if (listener != null) {
            anim.setAnimationListener(listener);
        }
        return anim;
    }

    public static String viewInfo(View view) {
        return "[(" + view.getLeft() + "," + view.getTop() + ")(" + view.getRight() + "," + view.getBottom() + ") " + view.getWidth() + "x" + view.getHeight() + "]";
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mQueueLock) {
            String str;
            printWriter.println("Current Status Bar state:");
            printWriter.println("  mExpanded=" + this.mExpanded + ", mExpandedVisible=" + this.mExpandedVisible + ", mTrackingPosition=" + this.mTrackingPosition);
            printWriter.println("  mTicking=" + this.mTicking);
            printWriter.println("  mTracking=" + this.mTracking);
            StringBuilder append = new StringBuilder().append("  mNotificationPanel=");
            if (this.mNotificationPanel == null) {
                str = "null";
            } else {
                str = this.mNotificationPanel + " params=" + this.mNotificationPanel.getLayoutParams().debug("");
            }
            printWriter.println(append.append(str).toString());
            printWriter.println("  mAnimating=" + this.mAnimating + ", mAnimY=" + this.mAnimY + ", mAnimVel=" + this.mAnimVel + ", mAnimAccel=" + this.mAnimAccel);
            printWriter.println("  mAnimLastTimeNanos=" + this.mAnimLastTimeNanos);
            printWriter.println("  mAnimatingReveal=" + this.mAnimatingReveal + " mViewDelta=" + this.mViewDelta);
            printWriter.println("  mDisplayMetrics=" + this.mDisplayMetrics);
            printWriter.println("  mPile: " + viewInfo(this.mPile));
            printWriter.println("  mCloseView: " + viewInfo(this.mCloseView));
            printWriter.println("  mTickerView: " + viewInfo(this.mTickerView));
            printWriter.println("  mScrollView: " + viewInfo(this.mScrollView) + " scroll " + this.mScrollView.getScrollX() + "," + this.mScrollView.getScrollY());
        }
        printWriter.print("  mNavigationBarView=");
        if (this.mNavigationBarView == null) {
            printWriter.println("null");
        } else {
            this.mNavigationBarView.dump(fileDescriptor, printWriter, strArr);
        }
        synchronized (this.mNotificationData) {
            int i;
            int size = this.mNotificationData.size();
            printWriter.println("  notification icons: " + size);
            for (i = 0; i < size; i++) {
                Entry entry = this.mNotificationData.get(i);
                printWriter.println("    [" + i + "] key=" + entry.key + " icon=" + entry.icon);
                StatusBarNotification statusBarNotification = entry.notification;
                printWriter.println("         pkg=" + statusBarNotification.pkg + " id=" + statusBarNotification.id + " score=" + statusBarNotification.score);
                printWriter.println("         notification=" + statusBarNotification.notification);
                printWriter.println("         tickerText=\"" + statusBarNotification.notification.tickerText + "\"");
            }
        }
        int childCount = this.mStatusIcons.getChildCount();
        printWriter.println("  system icons: " + childCount);
        for (i = 0; i < childCount; i++) {
            printWriter.println("    [" + i + "] icon=" + ((StatusBarIconView) this.mStatusIcons.getChildAt(i)));
        }
        this.mNetworkController.dump(fileDescriptor, printWriter, strArr);
    }

    public void createAndAddWindows() {
        addStatusBarWindow();
    }

    private void addStatusBarWindow() {
        ViewGroup.LayoutParams layoutParams = new LayoutParams(-1, getStatusBarHeight(), 2000, 8388680, -3);
        layoutParams.flags |= 16777216;
        layoutParams.gravity = getStatusBarGravity();
        layoutParams.setTitle("StatusBar");
        layoutParams.packageName = this.mContext.getPackageName();
        makeStatusBarView();
        WindowManagerImpl.getDefault().addView(this.mStatusBarWindow, layoutParams);
    }

    void setNotificationIconVisibility(boolean visible, int anim) {
        int old = this.mNotificationIcons.getVisibility();
        int v = visible ? 0 : 4;
        if (old != v) {
            this.mNotificationIcons.setVisibility(v);
            this.mNotificationIcons.startAnimation(loadAnim(anim, null));
        }
    }

    void updateExpandedInvisiblePosition() {
        this.mTrackingPosition = -this.mDisplayMetrics.heightPixels;
    }

    static final float saturate(float a) {
        if (a < 0.0f) {
            return 0.0f;
        }
        return a > 1.0f ? 1.0f : a;
    }

    protected int getExpandedViewMaxHeight() {
        return this.mDisplayMetrics.heightPixels - this.mNotificationPanelMarginBottomPx;
    }

    protected void updateExpandedViewPos(int i) {
        int expandedViewMaxHeight = getExpandedViewMaxHeight();
        if (this.mExpandedVisible) {
            int i2;
            int i3;
            if (i == -10001) {
                i2 = expandedViewMaxHeight;
            } else if (i == -10000) {
                i2 = this.mTrackingPosition;
            } else if (i <= expandedViewMaxHeight) {
                i2 = i;
            } else {
                i2 = expandedViewMaxHeight;
            }
            if (i2 > expandedViewMaxHeight || !(i2 >= expandedViewMaxHeight || this.mTracking || this.mAnimating)) {
                i3 = expandedViewMaxHeight;
            } else if (i2 < 0) {
                i3 = 0;
            } else {
                i3 = i2;
            }
            if (i3 != this.mTrackingPosition) {
                this.mTrackingPosition = i3;
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mNotificationPanel.getLayoutParams();
                layoutParams.height = i3;
                layoutParams.gravity = this.mNotificationPanelGravity;
                layoutParams.leftMargin = this.mNotificationPanelMarginLeftPx;
                this.mNotificationPanel.setLayoutParams(layoutParams);
                i2 = getCloseViewHeight() + getStatusBarHeight();
                float saturate = saturate(((float) (i3 - i2)) / ((float) (expandedViewMaxHeight - i2)));
                if (ActivityManager.isHighEndGfx(this.mDisplay)) {
                    this.mStatusBarWindow.setBackgroundColor(((int) (((float) (1.0d - ((1.0d - Math.cos(Math.pow((double) (1.0f - saturate), 2.200000047683716d) * 3.141590118408203d)) * 0.5d))) * 176.0f)) << 24);
                }
                updateCarrierLabelVisibility(false);
                return;
            }
            return;
        }
        updateExpandedInvisiblePosition();
    }

    void updateDisplaySize() {
        this.mDisplay.getMetrics(this.mDisplayMetrics);
    }

    private void setIntruderAlertVisibility(boolean vis) {
    }

    public void dismissIntruder() {
        if (this.mCurrentlyIntrudingNotification != null) {
            try {
                this.mBarService.onNotificationClear(this.mCurrentlyIntrudingNotification.pkg, this.mCurrentlyIntrudingNotification.tag, this.mCurrentlyIntrudingNotification.id);
            } catch (RemoteException e) {
            }
        }
    }

    void updateResources() {
        Context context = this.mContext;
        context.getResources();
        if (this.mClearButton instanceof TextView) {
            ((TextView) this.mClearButton).setText(context.getText(2131296260));
        }
        loadDimens();
    }

    protected void loadDimens() {
        Resources resources = this.mContext.getResources();
        this.mNaturalBarHeight = resources.getDimensionPixelSize(17104906);
        int dimensionPixelSize = resources.getDimensionPixelSize(17104912);
        int dimensionPixelSize2 = resources.getDimensionPixelSize(2131427349);
        if (!(dimensionPixelSize2 == this.mIconHPadding && dimensionPixelSize == this.mIconSize)) {
            this.mIconHPadding = dimensionPixelSize2;
            this.mIconSize = dimensionPixelSize;
        }
        this.mEdgeBorder = resources.getDimensionPixelSize(2131427328);
        this.mEdgeBorder2 = resources.getDimensionPixelSize(2131427329);
        this.mSelfExpandVelocityPx = resources.getDimension(2131427351);
        this.mSelfCollapseVelocityPx = resources.getDimension(2131427352);
        this.mFlingExpandMinVelocityPx = resources.getDimension(2131427353);
        this.mFlingCollapseMinVelocityPx = resources.getDimension(2131427354);
        this.mCollapseMinDisplayFraction = resources.getFraction(2131427357, 1, 1);
        this.mExpandMinDisplayFraction = resources.getFraction(2131427358, 1, 1);
        this.mExpandAccelPx = resources.getDimension(2131427359);
        this.mCollapseAccelPx = resources.getDimension(2131427360);
        this.mFlingGestureMaxXVelocityPx = resources.getDimension(2131427355);
        this.mFlingGestureMaxOutputVelocityPx = resources.getDimension(2131427356);
        this.mNotificationPanelMarginBottomPx = (int) resources.getDimension(2131427374);
        this.mNotificationPanelMarginLeftPx = (int) resources.getDimension(2131427375);
        this.mNotificationPanelGravity = resources.getInteger(2131361797);
        if (this.mNotificationPanelGravity <= 0) {
            this.mNotificationPanelGravity = 48;
        }
        getNinePatchPadding(resources.getDrawable(2130837581), this.mNotificationPanelBackgroundPadding);
        this.mNotificationPanelMinHeight = (((resources.getDimensionPixelSize(2131427372) + resources.getDimensionPixelSize(2131427371)) + this.mNotificationPanelBackgroundPadding.top) + this.mNotificationPanelBackgroundPadding.bottom) + resources.getDimensionPixelSize(2131427370);
        this.mCarrierLabelHeight = resources.getDimensionPixelSize(2131427376);
    }

    private static void getNinePatchPadding(Drawable d, Rect outPadding) {
        if (d instanceof NinePatchDrawable) {
            ((NinePatchDrawable) d).getPadding(outPadding);
        }
    }

    void vibrate() {
        ((Vibrator) this.mContext.getSystemService("vibrator")).vibrate(250);
    }

    protected void haltTicker() {
        this.mTicker.halt();
    }

    protected boolean shouldDisableNavbarGestures() {
        return this.mExpanded || (this.mDisabled & 2097152) != 0;
    }
}
