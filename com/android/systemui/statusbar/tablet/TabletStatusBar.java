package com.android.systemui.statusbar.tablet;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Slog;
import android.view.Display;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerImpl;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarNotification;
import com.android.systemui.recent.RecentTasksLoader;
import com.android.systemui.recent.RecentsPanelView.OnRecentsPanelVisibilityChangedListener;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.BaseStatusBar.TouchOutsideListener;
import com.android.systemui.statusbar.DoNotDisturb;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CompatModeButton;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NotificationRowLayout;
import com.android.systemui.statusbar.policy.Prefs;
import com.android.systemui.statusbar.tablet.InputMethodsPanel.OnHardKeyboardEnabledChangeListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class TabletStatusBar extends BaseStatusBar implements OnRecentsPanelVisibilityChangedListener, OnHardKeyboardEnabledChangeListener {
    private boolean mAltBackButtonEnabledForIme;
    ImageView mBackButton;
    ViewGroup mBarContents;
    BatteryController mBatteryController;
    BluetoothController mBluetoothController;
    private BroadcastReceiver mBroadcastReceiver;
    CompatModeButton mCompatModeButton;
    private CompatModePanel mCompatModePanel;
    View mCompatibilityHelpDialog;
    int mDisabled;
    DoNotDisturb mDoNotDisturb;
    View mFakeSpaceBar;
    ViewGroup mFeedbackIconArea;
    View mHomeButton;
    private OnTouchListener mHomeSearchActionListener;
    int mIconHPadding;
    IconLayout mIconLayout;
    int mIconSize;
    InputMethodButton mInputMethodSwitchButton;
    private InputMethodsPanel mInputMethodsPanel;
    LocationController mLocationController;
    private int mMaxNotificationIcons;
    View mMenuButton;
    int mMenuNavIconWidth;
    int mNaturalBarHeight;
    int mNavIconWidth;
    ViewGroup mNavigationArea;
    private int mNavigationIconHints;
    NetworkController mNetworkController;
    View mNotificationArea;
    Entry mNotificationDNDDummyEntry;
    boolean mNotificationDNDMode;
    int mNotificationFlingVelocity;
    NotificationIconArea mNotificationIconArea;
    NotificationPanel mNotificationPanel;
    LayoutParams mNotificationPanelParams;
    int mNotificationPeekIndex;
    IBinder mNotificationPeekKey;
    ViewGroup mNotificationPeekRow;
    int mNotificationPeekTapDuration;
    NotificationPeekPanel mNotificationPeekWindow;
    View mNotificationTrigger;
    private OnClickListener mOnClickListener;
    View mRecentButton;
    View mShadow;
    private int mShowSearchHoldoff;
    private Runnable mShowSearchPanel;
    KeyEvent mSpaceBarKeyEvent;
    TabletStatusBarView mStatusBarView;
    private int mSystemUiVisibility;
    TabletTicker mTicker;
    IWindowManager mWindowManager;

    private class H extends H {
        private H() {
            super();
        }

        public void handleMessage(Message m) {
            super.handleMessage(m);
            int N;
            Entry entry;
            switch (m.what) {
                case 1000:
                    if (!TabletStatusBar.this.mNotificationPanel.isShowing()) {
                        TabletStatusBar.this.mNotificationPanel.show(true, true);
                        TabletStatusBar.this.mNotificationArea.setVisibility(4);
                        TabletStatusBar.this.mTicker.halt();
                    }
                case 1001:
                    if (TabletStatusBar.this.mNotificationPanel.isShowing()) {
                        TabletStatusBar.this.mNotificationPanel.show(false, true);
                        TabletStatusBar.this.mNotificationArea.setVisibility(0);
                    }
                case 1002:
                    if (m.arg1 >= 0) {
                        N = TabletStatusBar.this.mNotificationData.size();
                        if (!TabletStatusBar.this.mNotificationDNDMode && TabletStatusBar.this.mNotificationPeekIndex >= 0 && TabletStatusBar.this.mNotificationPeekIndex < N) {
                            TabletStatusBar.this.mNotificationData.get((N - 1) - TabletStatusBar.this.mNotificationPeekIndex).icon.setBackgroundColor(0);
                            TabletStatusBar.this.mNotificationPeekIndex = -1;
                            TabletStatusBar.this.mNotificationPeekKey = null;
                        }
                        int peekIndex = m.arg1;
                        if (peekIndex < N) {
                            if (TabletStatusBar.this.mNotificationDNDMode) {
                                entry = TabletStatusBar.this.mNotificationDNDDummyEntry;
                            } else {
                                entry = TabletStatusBar.this.mNotificationData.get((N - 1) - peekIndex);
                            }
                            Entry copy = new Entry(entry.key, entry.notification, entry.icon);
                            TabletStatusBar.this.inflateViews(copy, TabletStatusBar.this.mNotificationPeekRow);
                            if (TabletStatusBar.this.mNotificationDNDMode) {
                                copy.content.setOnClickListener(new OnClickListener() {
                                    public void onClick(View v) {
                                        Editor editor = Prefs.edit(TabletStatusBar.this.mContext);
                                        editor.putBoolean("do_not_disturb", false);
                                        editor.apply();
                                        TabletStatusBar.this.animateCollapse();
                                        TabletStatusBar.this.visibilityChanged(false);
                                    }
                                });
                            }
                            entry.icon.setBackgroundColor(553648127);
                            TabletStatusBar.this.mNotificationPeekRow.removeAllViews();
                            TabletStatusBar.this.mNotificationPeekRow.addView(copy.row);
                            TabletStatusBar.this.mNotificationPeekWindow.setVisibility(0);
                            TabletStatusBar.this.mNotificationPanel.show(false, true);
                            TabletStatusBar.this.mNotificationPeekIndex = peekIndex;
                            TabletStatusBar.this.mNotificationPeekKey = entry.key;
                        }
                    }
                case 1003:
                    TabletStatusBar.this.mNotificationPeekWindow.setVisibility(8);
                    TabletStatusBar.this.mNotificationPeekRow.removeAllViews();
                    N = TabletStatusBar.this.mNotificationData.size();
                    if (TabletStatusBar.this.mNotificationPeekIndex >= 0 && TabletStatusBar.this.mNotificationPeekIndex < N) {
                        if (TabletStatusBar.this.mNotificationDNDMode) {
                            entry = TabletStatusBar.this.mNotificationDNDDummyEntry;
                        } else {
                            entry = TabletStatusBar.this.mNotificationData.get((N - 1) - TabletStatusBar.this.mNotificationPeekIndex);
                        }
                        entry.icon.setBackgroundColor(0);
                    }
                    TabletStatusBar.this.mNotificationPeekIndex = -1;
                    TabletStatusBar.this.mNotificationPeekKey = null;
                case 1030:
                    TabletStatusBar.this.mBarContents.setVisibility(0);
                    TabletStatusBar.this.mShadow.setVisibility(8);
                    TabletStatusBar.access$1672(TabletStatusBar.this, -2);
                    TabletStatusBar.this.notifyUiVisibilityChanged();
                case 1031:
                    TabletStatusBar.this.animateCollapse();
                    TabletStatusBar.this.visibilityChanged(false);
                    TabletStatusBar.this.mBarContents.setVisibility(8);
                    TabletStatusBar.this.mShadow.setVisibility(0);
                    TabletStatusBar.access$1676(TabletStatusBar.this, 1);
                    TabletStatusBar.this.notifyUiVisibilityChanged();
                case 1040:
                    if (TabletStatusBar.this.mInputMethodsPanel != null) {
                        TabletStatusBar.this.mInputMethodsPanel.openPanel();
                    }
                case 1041:
                    if (TabletStatusBar.this.mInputMethodsPanel != null) {
                        TabletStatusBar.this.mInputMethodsPanel.closePanel(false);
                    }
                case 1050:
                    if (TabletStatusBar.this.mCompatModePanel != null) {
                        TabletStatusBar.this.mCompatModePanel.openPanel();
                    }
                case 1051:
                    if (TabletStatusBar.this.mCompatModePanel != null) {
                        TabletStatusBar.this.mCompatModePanel.closePanel();
                    }
                case 2000:
                    TabletStatusBar.this.mTicker.halt();
                default:
                    break;
            }
        }
    }

    private class NotificationTriggerTouchListener implements OnTouchListener {
        private Runnable mHiliteOnR;
        float mInitialTouchX;
        float mInitialTouchY;
        int mTouchSlop;
        VelocityTracker mVT;

        public NotificationTriggerTouchListener() {
            this.mHiliteOnR = new Runnable() {
                public void run() {
                    TabletStatusBar.this.mNotificationArea.setBackgroundResource(17302500);
                }
            };
            this.mTouchSlop = ViewConfiguration.get(TabletStatusBar.this.getContext()).getScaledTouchSlop();
        }

        public void hilite(boolean on) {
            if (on) {
                TabletStatusBar.this.mNotificationArea.postDelayed(this.mHiliteOnR, 100);
                return;
            }
            TabletStatusBar.this.mNotificationArea.removeCallbacks(this.mHiliteOnR);
            TabletStatusBar.this.mNotificationArea.setBackgroundDrawable(null);
        }

        public boolean onTouch(View v, MotionEvent event) {
            if ((TabletStatusBar.this.mDisabled & 65536) != 0) {
                return true;
            }
            int action = event.getAction();
            switch (action) {
                case 0:
                    this.mVT = VelocityTracker.obtain();
                    this.mInitialTouchX = event.getX();
                    this.mInitialTouchY = event.getY();
                    hilite(true);
                    if (this.mVT != null) {
                        return true;
                    }
                    this.mVT.addMovement(event);
                    this.mVT.computeCurrentVelocity(1000);
                    if (this.mVT.getYVelocity() < ((float) (-TabletStatusBar.this.mNotificationFlingVelocity))) {
                        return true;
                    }
                    TabletStatusBar.this.animateExpand();
                    TabletStatusBar.this.visibilityChanged(true);
                    hilite(false);
                    this.mVT.recycle();
                    this.mVT = null;
                    return true;
                case 1:
                case 3:
                    hilite(false);
                    if (this.mVT != null) {
                        if (action == 1 && Math.abs(event.getX() - this.mInitialTouchX) < ((float) this.mTouchSlop) && Math.abs(event.getY() - this.mInitialTouchY) < ((float) (this.mTouchSlop / 3)) && ((int) event.getY()) < v.getBottom()) {
                            TabletStatusBar.this.animateExpand();
                            TabletStatusBar.this.visibilityChanged(true);
                            v.sendAccessibilityEvent(1);
                            v.playSoundEffect(0);
                        }
                        this.mVT.recycle();
                        this.mVT = null;
                        return true;
                    }
                    return false;
                case 2:
                case 4:
                    if (this.mVT != null) {
                        return true;
                    }
                    this.mVT.addMovement(event);
                    this.mVT.computeCurrentVelocity(1000);
                    if (this.mVT.getYVelocity() < ((float) (-TabletStatusBar.this.mNotificationFlingVelocity))) {
                        return true;
                    }
                    TabletStatusBar.this.animateExpand();
                    TabletStatusBar.this.visibilityChanged(true);
                    hilite(false);
                    this.mVT.recycle();
                    this.mVT = null;
                    return true;
                default:
                    return false;
            }
        }
    }

    public TabletStatusBar() {
        this.mNaturalBarHeight = -1;
        this.mIconSize = -1;
        this.mIconHPadding = -1;
        this.mNavIconWidth = -1;
        this.mMenuNavIconWidth = -1;
        this.mMaxNotificationIcons = 5;
        this.mSpaceBarKeyEvent = null;
        this.mCompatibilityHelpDialog = null;
        this.mDisabled = 0;
        this.mSystemUiVisibility = 0;
        this.mNavigationIconHints = 0;
        this.mShowSearchHoldoff = 0;
        this.mShowSearchPanel = new Runnable() {
            public void run() {
                TabletStatusBar.this.showSearchPanel();
            }
        };
        this.mHomeSearchActionListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case 0:
                        if (!(TabletStatusBar.this.shouldDisableNavbarGestures() || TabletStatusBar.this.inKeyguardRestrictedInputMode())) {
                            TabletStatusBar.this.mHandler.removeCallbacks(TabletStatusBar.this.mShowSearchPanel);
                            TabletStatusBar.this.mHandler.postDelayed(TabletStatusBar.this.mShowSearchPanel, (long) TabletStatusBar.this.mShowSearchHoldoff);
                        }
                        break;
                    case 1:
                    case 3:
                        TabletStatusBar.this.mHandler.removeCallbacks(TabletStatusBar.this.mShowSearchPanel);
                        break;
                }
                return false;
            }
        };
        this.mOnClickListener = new OnClickListener() {
            public void onClick(View v) {
                if (v == TabletStatusBar.this.mRecentButton) {
                    TabletStatusBar.this.onClickRecentButton();
                } else if (v == TabletStatusBar.this.mInputMethodSwitchButton) {
                    TabletStatusBar.this.onClickInputMethodSwitchButton();
                } else if (v == TabletStatusBar.this.mCompatModeButton) {
                    TabletStatusBar.this.onClickCompatModeButton();
                }
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) || "android.intent.action.SCREEN_OFF".equals(action)) {
                    int flags = 0;
                    if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                        String reason = intent.getStringExtra("reason");
                        if (reason != null && reason.equals("recentapps")) {
                            flags = 0 | 2;
                        }
                    }
                    if ("android.intent.action.SCREEN_OFF".equals(action)) {
                        TabletStatusBar.this.mRecentsPanel.show(false, false);
                        flags |= 2;
                    }
                    TabletStatusBar.this.animateCollapse(flags);
                }
            }
        };
    }

    static /* synthetic */ int access$1672(TabletStatusBar x0, int x1) {
        int i = x0.mSystemUiVisibility & x1;
        x0.mSystemUiVisibility = i;
        return i;
    }

    static /* synthetic */ int access$1676(TabletStatusBar x0, int x1) {
        int i = x0.mSystemUiVisibility | x1;
        x0.mSystemUiVisibility = i;
        return i;
    }

    public Context getContext() {
        return this.mContext;
    }

    protected void createAndAddWindows() {
        addStatusBarWindow();
        addPanelWindows();
    }

    private void addStatusBarWindow() {
        View sb = makeStatusBarView();
        LayoutParams lp = new LayoutParams(-1, -1, 2019, 8388680, 4);
        lp.gravity = getStatusBarGravity();
        lp.setTitle("SystemBar");
        lp.packageName = this.mContext.getPackageName();
        WindowManagerImpl.getDefault().addView(sb, lp);
    }

    protected void addPanelWindows() {
        Context context = this.mContext;
        Resources res = this.mContext.getResources();
        this.mNotificationPanel = (NotificationPanel) View.inflate(context, 2130903064, null);
        this.mNotificationPanel.setBar(this);
        this.mNotificationPanel.show(false, false);
        this.mNotificationPanel.setOnTouchListener(new TouchOutsideListener(1001, this.mNotificationPanel));
        this.mBatteryController.addIconView((ImageView) this.mNotificationPanel.findViewById(2131492910));
        this.mBatteryController.addLabelView((TextView) this.mNotificationPanel.findViewById(2131493004));
        this.mBluetoothController.addIconView((ImageView) this.mNotificationPanel.findViewById(2131492993));
        ImageView mobileRSSI = (ImageView) this.mNotificationPanel.findViewById(2131492895);
        if (mobileRSSI != null) {
            this.mNetworkController.addPhoneSignalIconView(mobileRSSI);
        }
        ImageView wifiRSSI = (ImageView) this.mNotificationPanel.findViewById(2131492891);
        if (wifiRSSI != null) {
            this.mNetworkController.addWifiIconView(wifiRSSI);
        }
        this.mNetworkController.addWifiLabelView((TextView) this.mNotificationPanel.findViewById(2131493003));
        this.mNetworkController.addDataTypeIconView((ImageView) this.mNotificationPanel.findViewById(2131492896));
        this.mNetworkController.addMobileLabelView((TextView) this.mNotificationPanel.findViewById(2131493000));
        this.mNetworkController.addCombinedLabelView((TextView) this.mBarContents.findViewById(2131492992));
        this.mStatusBarView.setIgnoreChildren(0, this.mNotificationTrigger, this.mNotificationPanel);
        LayoutParams lp = new LayoutParams(res.getDimensionPixelSize(2131427385), getNotificationPanelHeight(), 2024, 25297664, -3);
        this.mNotificationPanelParams = lp;
        lp.gravity = 85;
        lp.setTitle("NotificationPanel");
        lp.softInputMode = 49;
        lp.windowAnimations = 16973824;
        WindowManagerImpl.getDefault().addView(this.mNotificationPanel, lp);
        this.mRecentTasksLoader = new RecentTasksLoader(context);
        updateRecentsPanel();
        this.mStatusBarView.setBar(this);
        this.mHomeButton.setOnTouchListener(this.mHomeSearchActionListener);
        updateSearchPanel();
        this.mInputMethodsPanel = (InputMethodsPanel) View.inflate(context, 2130903061, null);
        this.mInputMethodsPanel.setHardKeyboardEnabledChangeListener(this);
        this.mInputMethodsPanel.setOnTouchListener(new TouchOutsideListener(1041, this.mInputMethodsPanel));
        this.mInputMethodsPanel.setImeSwitchButton(this.mInputMethodSwitchButton);
        this.mStatusBarView.setIgnoreChildren(2, this.mInputMethodSwitchButton, this.mInputMethodsPanel);
        lp = new LayoutParams(-2, -2, 2014, 25297152, -3);
        lp.gravity = 85;
        lp.setTitle("InputMethodsPanel");
        lp.windowAnimations = 2131623947;
        WindowManagerImpl.getDefault().addView(this.mInputMethodsPanel, lp);
        this.mCompatModePanel = (CompatModePanel) View.inflate(context, 2130903059, null);
        this.mCompatModePanel.setOnTouchListener(new TouchOutsideListener(1051, this.mCompatModePanel));
        this.mCompatModePanel.setTrigger(this.mCompatModeButton);
        this.mCompatModePanel.setVisibility(8);
        this.mStatusBarView.setIgnoreChildren(3, this.mCompatModeButton, this.mCompatModePanel);
        lp = new LayoutParams(250, -2, 2014, 25297152, -3);
        lp.gravity = 85;
        lp.setTitle("CompatModePanel");
        lp.windowAnimations = 16973826;
        WindowManagerImpl.getDefault().addView(this.mCompatModePanel, lp);
        this.mRecentButton.setOnTouchListener(this.mRecentsPanel);
        this.mPile = (NotificationRowLayout) this.mNotificationPanel.findViewById(2131492938);
        this.mPile.removeAllViews();
        this.mPile.setLongPressListener(getNotificationLongClicker());
        ((ScrollView) this.mPile.getParent()).setFillViewport(true);
    }

    private int getNotificationPanelHeight() {
        Resources res = this.mContext.getResources();
        Display d = WindowManagerImpl.getDefault().getDefaultDisplay();
        Point size = new Point();
        d.getRealSize(size);
        return Math.max(res.getDimensionPixelSize(2131427388), size.y);
    }

    public void start() {
        super.start();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        loadDimens();
        this.mNotificationPanelParams.height = getNotificationPanelHeight();
        WindowManagerImpl.getDefault().updateViewLayout(this.mNotificationPanel, this.mNotificationPanelParams);
        this.mRecentsPanel.updateValuesFromResources();
        this.mShowSearchHoldoff = this.mContext.getResources().getInteger(2131361793);
        updateSearchPanel();
    }

    protected void loadDimens() {
        Resources res = this.mContext.getResources();
        this.mNaturalBarHeight = res.getDimensionPixelSize(17104909);
        int newIconSize = res.getDimensionPixelSize(17104915);
        int newIconHPadding = res.getDimensionPixelSize(2131427349);
        int newNavIconWidth = res.getDimensionPixelSize(2131427362);
        int newMenuNavIconWidth = res.getDimensionPixelSize(2131427363);
        if (!(this.mNavigationArea == null || newNavIconWidth == this.mNavIconWidth)) {
            this.mNavIconWidth = newNavIconWidth;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(this.mNavIconWidth, -1);
            this.mBackButton.setLayoutParams(lp);
            this.mHomeButton.setLayoutParams(lp);
            this.mRecentButton.setLayoutParams(lp);
        }
        if (!(this.mNavigationArea == null || newMenuNavIconWidth == this.mMenuNavIconWidth)) {
            this.mMenuNavIconWidth = newMenuNavIconWidth;
            this.mMenuButton.setLayoutParams(new LinearLayout.LayoutParams(this.mMenuNavIconWidth, -1));
        }
        if (!(newIconHPadding == this.mIconHPadding && newIconSize == this.mIconSize)) {
            this.mIconHPadding = newIconHPadding;
            this.mIconSize = newIconSize;
            reloadAllNotificationIcons();
        }
        int numIcons = res.getInteger(2131361792);
        if (numIcons != this.mMaxNotificationIcons) {
            this.mMaxNotificationIcons = numIcons;
            reloadAllNotificationIcons();
        }
    }

    protected View makeStatusBarView() {
        Context context = this.mContext;
        this.mWindowManager = Stub.asInterface(ServiceManager.getService("window"));
        loadDimens();
        TabletStatusBarView tabletStatusBarView = (TabletStatusBarView) View.inflate(context, 2130903058, null);
        this.mStatusBarView = tabletStatusBarView;
        tabletStatusBarView.setHandler(this.mHandler);
        try {
            if (this.mWindowManager.hasNavigationBar()) {
                Slog.e("TabletStatusBar", "Tablet device cannot show navigation bar and system bar");
            }
        } catch (RemoteException e) {
        }
        this.mBarContents = (ViewGroup) tabletStatusBarView.findViewById(2131492962);
        this.mNotificationArea = tabletStatusBarView.findViewById(2131492965);
        this.mNotificationArea.setOnTouchListener(new NotificationTriggerTouchListener());
        this.mNotificationTrigger = tabletStatusBarView.findViewById(2131492991);
        this.mNotificationIconArea = (NotificationIconArea) tabletStatusBarView.findViewById(2131492906);
        this.mIconLayout = (IconLayout) tabletStatusBarView.findViewById(2131492901);
        ViewConfiguration.get(context);
        this.mNotificationPeekTapDuration = ViewConfiguration.getTapTimeout();
        this.mNotificationFlingVelocity = 300;
        this.mTicker = new TabletTicker(this);
        this.mLocationController = new LocationController(this.mContext);
        this.mDoNotDisturb = new DoNotDisturb(this.mContext);
        this.mBatteryController = new BatteryController(this.mContext);
        this.mBatteryController.addIconView((ImageView) tabletStatusBarView.findViewById(2131492910));
        this.mBluetoothController = new BluetoothController(this.mContext);
        this.mBluetoothController.addIconView((ImageView) tabletStatusBarView.findViewById(2131492993));
        this.mNetworkController = new NetworkController(this.mContext);
        this.mNetworkController.addSignalCluster((SignalClusterView) tabletStatusBarView.findViewById(2131492909));
        this.mBackButton = (ImageView) tabletStatusBarView.findViewById(2131492881);
        this.mNavigationArea = (ViewGroup) tabletStatusBarView.findViewById(2131492963);
        this.mHomeButton = this.mNavigationArea.findViewById(2131492882);
        this.mMenuButton = this.mNavigationArea.findViewById(2131492884);
        this.mRecentButton = this.mNavigationArea.findViewById(2131492883);
        this.mRecentButton.setOnClickListener(this.mOnClickListener);
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setDuration(250);
        layoutTransition.setDuration(0, 0);
        layoutTransition.setDuration(1, 0);
        layoutTransition.addTransitionListener(new TransitionListener() {
            public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                TabletStatusBar.this.mBarContents.invalidate();
            }

            public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            }
        });
        this.mNavigationArea.setLayoutTransition(layoutTransition);
        this.mNavigationArea.setMotionEventSplittingEnabled(false);
        this.mFeedbackIconArea = (ViewGroup) tabletStatusBarView.findViewById(2131492988);
        this.mInputMethodSwitchButton = (InputMethodButton) tabletStatusBarView.findViewById(2131492989);
        this.mInputMethodSwitchButton.setOnClickListener(this.mOnClickListener);
        this.mCompatModeButton = (CompatModeButton) tabletStatusBarView.findViewById(2131492990);
        this.mCompatModeButton.setOnClickListener(this.mOnClickListener);
        this.mCompatModeButton.setVisibility(8);
        this.mFakeSpaceBar = tabletStatusBarView.findViewById(2131492964);
        this.mShadow = tabletStatusBarView.findViewById(2131492967);
        this.mShadow.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent ev) {
                if (ev.getAction() == 0) {
                    TabletStatusBar.this.mShadow.setVisibility(8);
                    TabletStatusBar.this.mBarContents.setVisibility(0);
                    try {
                        TabletStatusBar.this.mBarService.setSystemUiVisibility(0, 1);
                    } catch (RemoteException e) {
                    }
                }
                return false;
            }
        });
        LayoutTransition layoutTransition2 = new LayoutTransition();
        layoutTransition2.setAnimator(2, ObjectAnimator.ofFloat(null, "alpha", new float[]{0.5f, 1.0f}));
        layoutTransition2.setDuration(2, 200);
        layoutTransition2.setStartDelay(2, 0);
        layoutTransition2.setAnimator(3, ObjectAnimator.ofFloat(null, "alpha", new float[]{1.0f, 0.0f}));
        layoutTransition2.setDuration(3, 750);
        layoutTransition2.setStartDelay(3, 0);
        ((ViewGroup) tabletStatusBarView.findViewById(2131492961)).setLayoutTransition(layoutTransition2);
        layoutTransition2 = new LayoutTransition();
        layoutTransition2.setAnimator(2, ObjectAnimator.ofFloat(null, "alpha", new float[]{0.0f, 1.0f}));
        layoutTransition2.setDuration(2, 750);
        layoutTransition2.setStartDelay(2, 0);
        layoutTransition2.setAnimator(3, ObjectAnimator.ofFloat(null, "alpha", new float[]{1.0f, 0.0f}));
        layoutTransition2.setDuration(3, 0);
        layoutTransition2.setStartDelay(3, 0);
        ((ViewGroup) tabletStatusBarView.findViewById(2131492966)).setLayoutTransition(layoutTransition2);
        setAreThereNotifications();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        return tabletStatusBarView;
    }

    protected LayoutParams getRecentsLayoutParams(ViewGroup.LayoutParams layoutParams) {
        LayoutParams layoutParams2 = new LayoutParams((int) this.mContext.getResources().getDimension(2131427390), -1, 2024, 25297152, -3);
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
        } else {
            layoutParams2.flags |= 2;
            layoutParams2.dimAmount = 0.7f;
        }
        layoutParams2.gravity = 83;
        layoutParams2.setTitle("SearchPanel");
        layoutParams2.windowAnimations = 16974313;
        layoutParams2.softInputMode = 49;
        return layoutParams2;
    }

    protected void updateRecentsPanel() {
        super.updateRecentsPanel(2130903069);
        this.mRecentsPanel.setStatusBarView(this.mStatusBarView);
    }

    protected void updateSearchPanel() {
        super.updateSearchPanel();
        this.mSearchPanelView.setStatusBarView(this.mStatusBarView);
        this.mStatusBarView.setDelegateView(this.mSearchPanelView);
    }

    public void showSearchPanel() {
        super.showSearchPanel();
        LayoutParams lp = (LayoutParams) this.mStatusBarView.getLayoutParams();
        lp.flags &= -33;
        WindowManagerImpl.getDefault().updateViewLayout(this.mStatusBarView, lp);
    }

    public void hideSearchPanel() {
        super.hideSearchPanel();
        LayoutParams lp = (LayoutParams) this.mStatusBarView.getLayoutParams();
        lp.flags |= 32;
        WindowManagerImpl.getDefault().updateViewLayout(this.mStatusBarView, lp);
    }

    public int getStatusBarHeight() {
        return this.mStatusBarView != null ? this.mStatusBarView.getHeight() : this.mContext.getResources().getDimensionPixelSize(17104909);
    }

    protected int getStatusBarGravity() {
        return 87;
    }

    protected H createHandler() {
        return new H();
    }

    public void addIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
    }

    public void updateIcon(String slot, int index, int viewIndex, StatusBarIcon old, StatusBarIcon icon) {
    }

    public void removeIcon(String slot, int index, int viewIndex) {
    }

    public void addNotification(IBinder iBinder, StatusBarNotification statusBarNotification) {
        addNotificationViews(iBinder, statusBarNotification);
        isImmersive();
        if (statusBarNotification.notification.fullScreenIntent != null) {
            Slog.w("TabletStatusBar", "Notification has fullScreenIntent and activity is not immersive; sending fullScreenIntent");
            try {
                statusBarNotification.notification.fullScreenIntent.send();
            } catch (CanceledException e) {
            }
        } else {
            tick(iBinder, statusBarNotification, true);
        }
        setAreThereNotifications();
    }

    public void removeNotification(IBinder key) {
        removeNotificationViews(key);
        this.mTicker.remove(key);
        setAreThereNotifications();
    }

    public void showClock(boolean z) {
        int i = 0;
        View findViewById = this.mBarContents.findViewById(2131492911);
        View findViewById2 = this.mBarContents.findViewById(2131492992);
        if (findViewById != null) {
            findViewById.setVisibility(z ? 0 : 8);
        }
        if (findViewById2 != null) {
            if (z) {
                i = 8;
            }
            findViewById2.setVisibility(i);
        }
    }

    public void disable(int i) {
        Object obj = 1;
        int i2 = i ^ this.mDisabled;
        this.mDisabled = i;
        if ((8388608 & i2) != 0) {
            boolean z = (8388608 & i) == 0;
            Slog.i("TabletStatusBar", "DISABLE_CLOCK: " + (z ? "no" : "yes"));
            showClock(z);
        }
        if ((1048576 & i2) != 0) {
            boolean z2;
            if ((1048576 & i) != 0) {
                z2 = false;
            }
            Slog.i("TabletStatusBar", "DISABLE_SYSTEM_INFO: " + (z2 ? "no" : "yes"));
            this.mNotificationTrigger.setVisibility(z2 ? 0 : 8);
        }
        if (!((i2 & 65536) == 0 || (i & 65536) == 0)) {
            Slog.i("TabletStatusBar", "DISABLE_EXPAND: yes");
            animateCollapse();
            visibilityChanged(false);
        }
        if ((i2 & 131072) != 0) {
            this.mNotificationDNDMode = Prefs.read(this.mContext).getBoolean("do_not_disturb", false);
            if ((i & 131072) != 0) {
                Slog.i("TabletStatusBar", "DISABLE_NOTIFICATION_ICONS: yes" + (this.mNotificationDNDMode ? " (DND)" : ""));
                this.mTicker.halt();
            } else {
                Slog.i("TabletStatusBar", "DISABLE_NOTIFICATION_ICONS: no" + (this.mNotificationDNDMode ? " (DND)" : ""));
            }
            reloadAllNotificationIcons();
        } else if (!((524288 & i2) == 0 || (524288 & i) == 0)) {
            this.mTicker.halt();
        }
        if ((23068672 & i2) != 0) {
            setNavigationVisibility(i);
            if ((16777216 & i) != 0) {
                this.mHandler.removeMessages(1021);
                this.mHandler.sendEmptyMessage(1021);
            }
        }
    }

    private void setNavigationVisibility(int i) {
        int i2;
        int i3 = 4;
        boolean z = true;
        if ((2097152 & i) != 0) {
            boolean z2 = true;
        } else {
            Object obj = null;
        }
        if ((16777216 & i) != 0) {
            boolean z3 = true;
        } else {
            Object obj2 = null;
        }
        if ((4194304 & i) != 0) {
            boolean z4 = true;
        } else {
            Object obj3 = null;
        }
        ImageView imageView = this.mBackButton;
        if (z4) {
            i2 = 4;
        } else {
            i2 = 0;
        }
        imageView.setVisibility(i2);
        View view = this.mHomeButton;
        if (z2) {
            i2 = 4;
        } else {
            i2 = 0;
        }
        view.setVisibility(i2);
        View view2 = this.mRecentButton;
        if (!z3) {
            i3 = 0;
        }
        view2.setVisibility(i3);
        InputMethodButton inputMethodButton = this.mInputMethodSwitchButton;
        if ((1048576 & i) == 0) {
            z = false;
        }
        inputMethodButton.setScreenLocked(z);
    }

    private boolean hasTicker(Notification n) {
        return (n.tickerView == null && TextUtils.isEmpty(n.tickerText)) ? false : true;
    }

    protected void tick(IBinder iBinder, StatusBarNotification statusBarNotification, boolean z) {
        if (!this.mNotificationPanel.isShowing()) {
            if ((z || (statusBarNotification.notification.flags & 8) == 0) && hasTicker(statusBarNotification.notification) && this.mStatusBarView.getWindowToken() != null && (this.mDisabled & 655360) == 0) {
                this.mTicker.add(iBinder, statusBarNotification);
                this.mFeedbackIconArea.setVisibility(8);
            }
        }
    }

    public void doneTicking() {
        this.mFeedbackIconArea.setVisibility(0);
    }

    public void animateExpand() {
        this.mHandler.removeMessages(1000);
        this.mHandler.sendEmptyMessage(1000);
    }

    public void animateCollapse() {
        animateCollapse(0);
    }

    public void animateCollapse(int flags) {
        if ((flags & 4) == 0) {
            this.mHandler.removeMessages(1001);
            this.mHandler.sendEmptyMessage(1001);
        }
        if ((flags & 2) == 0) {
            this.mHandler.removeMessages(1021);
            this.mHandler.sendEmptyMessage(1021);
        }
        if ((flags & 1) == 0) {
            this.mHandler.removeMessages(1025);
            this.mHandler.sendEmptyMessage(1025);
        }
        if ((flags & 8) == 0) {
            this.mHandler.removeMessages(1041);
            this.mHandler.sendEmptyMessage(1041);
        }
        if ((flags & 16) == 0) {
            this.mHandler.removeMessages(1051);
            this.mHandler.sendEmptyMessage(1051);
        }
    }

    public void setNavigationIconHints(int i) {
        float f = 0.5f;
        if (i != this.mNavigationIconHints) {
            float f2;
            int i2;
            this.mNavigationIconHints = i;
            this.mBackButton.setAlpha((i & 1) != 0 ? 0.5f : 1.0f);
            View view = this.mHomeButton;
            if ((i & 2) != 0) {
                f2 = 0.5f;
            } else {
                f2 = 1.0f;
            }
            view.setAlpha(f2);
            View view2 = this.mRecentButton;
            if ((i & 4) == 0) {
                f = 1.0f;
            }
            view2.setAlpha(f);
            ImageView imageView = this.mBackButton;
            if ((i & 8) != 0) {
                i2 = 2130837544;
            } else {
                i2 = 2130837543;
            }
            imageView.setImageResource(i2);
        }
    }

    private void notifyUiVisibilityChanged() {
        try {
            this.mWindowManager.statusBarVisibilityChanged(this.mSystemUiVisibility);
        } catch (RemoteException e) {
        }
    }

    public void setSystemUiVisibility(int vis, int mask) {
        int i = 1030;
        int oldVal = this.mSystemUiVisibility;
        int newVal = ((mask ^ -1) & oldVal) | (vis & mask);
        int diff = newVal ^ oldVal;
        if (diff != 0) {
            this.mSystemUiVisibility = newVal;
            if ((diff & 1) != 0) {
                this.mHandler.removeMessages(1031);
                this.mHandler.removeMessages(1030);
                H h = this.mHandler;
                if ((vis & 1) != 0) {
                    i = 1031;
                }
                h.sendEmptyMessage(i);
            }
            notifyUiVisibilityChanged();
        }
    }

    public void setLightsOn(boolean z) {
        if (this.mMenuButton.getVisibility() == 0) {
            z = true;
        }
        Slog.v("TabletStatusBar", "setLightsOn(" + z + ")");
        if (z) {
            setSystemUiVisibility(0, 1);
        } else {
            setSystemUiVisibility(1, 1);
        }
    }

    public void topAppWindowChanged(boolean z) {
        this.mMenuButton.setVisibility(z ? 0 : 8);
        if (z) {
            setLightsOn(true);
        }
        this.mCompatModeButton.refresh();
        if (this.mCompatModeButton.getVisibility() != 0) {
            hideCompatibilityHelp();
            this.mCompatModePanel.closePanel();
        } else if (!Prefs.read(this.mContext).getBoolean("shown_compat_mode_help", false)) {
            showCompatibilityHelp();
        }
    }

    private void showCompatibilityHelp() {
        if (this.mCompatibilityHelpDialog == null) {
            this.mCompatibilityHelpDialog = View.inflate(this.mContext, 2130903041, null);
            this.mCompatibilityHelpDialog.findViewById(2131492874).setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    TabletStatusBar.this.hideCompatibilityHelp();
                    Editor editor = Prefs.edit(TabletStatusBar.this.mContext);
                    editor.putBoolean("shown_compat_mode_help", true);
                    editor.apply();
                }
            });
            ViewGroup.LayoutParams layoutParams = new LayoutParams(-1, -1, 2008, 131840, -3);
            layoutParams.setTitle("CompatibilityModeDialog");
            layoutParams.softInputMode = 49;
            layoutParams.windowAnimations = 16974311;
            WindowManagerImpl.getDefault().addView(this.mCompatibilityHelpDialog, layoutParams);
        }
    }

    private void hideCompatibilityHelp() {
        if (this.mCompatibilityHelpDialog != null) {
            WindowManagerImpl.getDefault().removeView(this.mCompatibilityHelpDialog);
            this.mCompatibilityHelpDialog = null;
        }
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition) {
        boolean z;
        boolean altBack;
        int i = 0;
        InputMethodButton inputMethodButton = this.mInputMethodSwitchButton;
        if ((vis & 1) != 0) {
            z = true;
        } else {
            z = false;
        }
        inputMethodButton.setImeWindowStatus(token, z);
        updateNotificationIcons();
        this.mInputMethodsPanel.setImeToken(token);
        if (backDisposition == 2 || (vis & 2) != 0) {
            altBack = true;
        } else {
            altBack = false;
        }
        this.mAltBackButtonEnabledForIme = altBack;
        this.mCommandQueue.setNavigationIconHints(altBack ? this.mNavigationIconHints | 8 : this.mNavigationIconHints & -9);
        View view = this.mFakeSpaceBar;
        if ((vis & 2) == 0) {
            i = 8;
        }
        view.setVisibility(i);
    }

    public void onRecentsPanelVisibilityChanged(boolean visible) {
        boolean altBack = visible || this.mAltBackButtonEnabledForIme;
        this.mCommandQueue.setNavigationIconHints(altBack ? this.mNavigationIconHints | 8 : this.mNavigationIconHints & -9);
    }

    public void setHardKeyboardStatus(boolean available, boolean enabled) {
        this.mInputMethodSwitchButton.setHardKeyboardStatus(available);
        updateNotificationIcons();
        this.mInputMethodsPanel.setHardKeyboardStatus(available, enabled);
    }

    public void onHardKeyboardEnabledChange(boolean enabled) {
        try {
            this.mBarService.setHardKeyboardEnabled(enabled);
        } catch (RemoteException e) {
        }
    }

    private boolean isImmersive() {
        try {
            return ActivityManagerNative.getDefault().isTopActivityImmersive();
        } catch (RemoteException e) {
            return false;
        }
    }

    protected void setAreThereNotifications() {
        if (this.mNotificationPanel != null) {
            NotificationPanel notificationPanel = this.mNotificationPanel;
            boolean z = isDeviceProvisioned() && this.mNotificationData.hasClearableItems();
            notificationPanel.setClearable(z);
        }
    }

    public void onClickRecentButton() {
        if ((this.mDisabled & 65536) == 0) {
            int i = this.mRecentsPanel.getVisibility() == 0 ? 1021 : 1020;
            this.mHandler.removeMessages(i);
            this.mHandler.sendEmptyMessage(i);
        }
    }

    public void onClickInputMethodSwitchButton() {
        int msg = this.mInputMethodsPanel.getVisibility() == 8 ? 1040 : 1041;
        this.mHandler.removeMessages(msg);
        this.mHandler.sendEmptyMessage(msg);
    }

    public void onClickCompatModeButton() {
        int msg = this.mCompatModePanel.getVisibility() == 8 ? 1050 : 1051;
        this.mHandler.removeMessages(msg);
        this.mHandler.sendEmptyMessage(msg);
    }

    public void resetNotificationPeekFadeTimer() {
        this.mHandler.removeMessages(1003);
        this.mHandler.sendEmptyMessageDelayed(1003, 3000);
    }

    private void reloadAllNotificationIcons() {
        if (this.mIconLayout != null) {
            this.mIconLayout.removeAllViews();
            updateNotificationIcons();
        }
    }

    protected void updateNotificationIcons() {
        if (this.mIconLayout != null) {
            loadNotificationPanel();
            ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(this.mIconSize + (this.mIconHPadding * 2), this.mNaturalBarHeight);
            if (this.mNotificationDNDMode) {
                if (this.mIconLayout.getChildCount() == 0) {
                    Notification notification = new Builder(this.mContext).setContentTitle(this.mContext.getText(2131296378)).setContentText(this.mContext.getText(2131296379)).setSmallIcon(2130837528).setOngoing(true).getNotification();
                    View statusBarIconView = new StatusBarIconView(this.mContext, "_dnd", notification);
                    statusBarIconView.setImageResource(2130837528);
                    statusBarIconView.setScaleType(ScaleType.CENTER_INSIDE);
                    statusBarIconView.setPadding(this.mIconHPadding, 0, this.mIconHPadding, 0);
                    this.mNotificationDNDDummyEntry = new Entry(null, new StatusBarNotification("", 0, "", 0, 0, 2, notification), statusBarIconView);
                    this.mIconLayout.addView(statusBarIconView, layoutParams);
                }
            } else if ((this.mDisabled & 131072) == 0) {
                int size = this.mNotificationData.size();
                ArrayList arrayList = new ArrayList();
                int i = this.mMaxNotificationIcons;
                if (this.mInputMethodSwitchButton.getVisibility() != 8) {
                    i--;
                }
                if (this.mCompatModeButton.getVisibility() != 8) {
                    i--;
                }
                boolean isDeviceProvisioned = isDeviceProvisioned();
                int i2 = 0;
                while (arrayList.size() < i && i2 < size) {
                    Entry entry = this.mNotificationData.get((size - i2) - 1);
                    if ((isDeviceProvisioned && entry.notification.score >= -10) || showNotificationEvenIfUnprovisioned(entry.notification)) {
                        arrayList.add(entry.icon);
                    }
                    i2++;
                }
                ArrayList arrayList2 = new ArrayList();
                for (i = 0; i < this.mIconLayout.getChildCount(); i++) {
                    View childAt = this.mIconLayout.getChildAt(i);
                    if (!arrayList.contains(childAt)) {
                        arrayList2.add(childAt);
                    }
                }
                Iterator it = arrayList2.iterator();
                while (it.hasNext()) {
                    this.mIconLayout.removeView((View) it.next());
                }
                for (i2 = 0; i2 < arrayList.size(); i2++) {
                    View view = (View) arrayList.get(i2);
                    view.setPadding(this.mIconHPadding, 0, this.mIconHPadding, 0);
                    if (view.getParent() == null) {
                        this.mIconLayout.addView(view, i2, layoutParams);
                    }
                }
            }
        }
    }

    private void loadNotificationPanel() {
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
                this.mPile.addView(v, Math.min((toShow.size() - 1) - i, this.mPile.getChildCount()));
            }
        }
        this.mNotificationPanel.setNotificationCount(toShow.size());
        this.mNotificationPanel.setSettingsEnabled(isDeviceProvisioned());
    }

    protected void workAroundBadLayerDrawableOpacity(View v) {
        Drawable bgd = v.getBackground();
        if (bgd instanceof LayerDrawable) {
            LayerDrawable d = (LayerDrawable) bgd;
            v.setBackgroundDrawable(null);
            d.setOpacity(-3);
            v.setBackgroundDrawable(d);
        }
    }

    public void clearAll() {
        try {
            this.mBarService.onClearAllNotifications();
        } catch (RemoteException e) {
        }
        animateCollapse();
        visibilityChanged(false);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("mDisabled=0x");
        printWriter.println(Integer.toHexString(this.mDisabled));
        printWriter.println("mNetworkController:");
        this.mNetworkController.dump(fileDescriptor, printWriter, strArr);
    }

    protected boolean isTopNotification(ViewGroup parent, Entry entry) {
        return (parent == null || entry == null || parent.indexOfChild(entry.row) != parent.getChildCount() - 1) ? false : true;
    }

    protected void haltTicker() {
        this.mTicker.halt();
    }

    protected void updateExpandedViewPos(int expandedPosition) {
    }

    protected boolean shouldDisableNavbarGestures() {
        return this.mNotificationPanel.getVisibility() == 0 || (this.mDisabled & 2097152) != 0;
    }
}
