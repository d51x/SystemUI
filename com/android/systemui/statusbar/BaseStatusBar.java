package com.android.systemui.statusbar;

import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerImpl;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RemoteViews;
import android.widget.RemoteViews.OnClickHandler;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarIconList;
import com.android.internal.statusbar.StatusBarNotification;
import com.android.internal.widget.SizeAdaptiveLayout;
import com.android.systemui.SearchPanelView;
import com.android.systemui.SystemUI;
import com.android.systemui.recent.RecentTasksLoader;
import com.android.systemui.recent.RecentsPanelView;
import com.android.systemui.recent.RecentsPanelView.OnRecentsPanelVisibilityChangedListener;
import com.android.systemui.recent.TaskDescription;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.policy.NotificationRowLayout;
import com.android.systemui.statusbar.tablet.StatusBarPanel;
import java.util.ArrayList;

public abstract class BaseStatusBar extends SystemUI implements OnRecentsPanelVisibilityChangedListener, Callbacks {
    protected IStatusBarService mBarService;
    protected CommandQueue mCommandQueue;
    protected StatusBarNotification mCurrentlyIntrudingNotification;
    private boolean mDeviceProvisioned;
    protected Display mDisplay;
    protected H mHandler;
    protected PopupMenu mNotificationBlamePopup;
    protected NotificationData mNotificationData;
    private OnClickHandler mOnClickHandler;
    protected boolean mPanelSlightlyVisible;
    protected NotificationRowLayout mPile;
    private ContentObserver mProvisioningObserver;
    protected RecentTasksLoader mRecentTasksLoader;
    protected RecentsPanelView mRecentsPanel;
    protected SearchPanelView mSearchPanelView;
    private IWindowManager mWindowManager;

    class AnonymousClass_1 extends ContentObserver {
        AnonymousClass_1(Handler x0) {
            super(x0);
        }

        public void onChange(boolean selfChange) {
            boolean provisioned = false;
            if (Secure.getInt(BaseStatusBar.this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
                provisioned = true;
            }
            if (provisioned != BaseStatusBar.this.mDeviceProvisioned) {
                BaseStatusBar.this.mDeviceProvisioned = provisioned;
                BaseStatusBar.this.updateNotificationIcons();
            }
        }
    }

    class AnonymousClass_3 implements OnClickListener {
        final /* synthetic */ int val$_id;
        final /* synthetic */ String val$_pkg;
        final /* synthetic */ String val$_tag;

        AnonymousClass_3(String str, String str2, int i) {
            this.val$_pkg = str;
            this.val$_tag = str2;
            this.val$_id = i;
        }

        public void onClick(View v) {
            try {
                BaseStatusBar.this.mBarService.onNotificationClear(this.val$_pkg, this.val$_tag, this.val$_id);
            } catch (RemoteException e) {
            }
        }
    }

    protected class H extends Handler {
        protected H() {
        }

        public void handleMessage(Message m) {
            switch (m.what) {
                case 1020:
                    if (BaseStatusBar.this.mRecentsPanel != null) {
                        BaseStatusBar.this.mRecentsPanel.show(true, false);
                    }
                case 1021:
                    if (BaseStatusBar.this.mRecentsPanel != null && BaseStatusBar.this.mRecentsPanel.isShowing()) {
                        BaseStatusBar.this.mRecentsPanel.show(false, false);
                    }
                case 1022:
                    BaseStatusBar.this.mRecentsPanel.preloadRecentTasksList();
                case 1023:
                    BaseStatusBar.this.mRecentsPanel.clearRecentTasksList();
                case 1024:
                    if (BaseStatusBar.this.mSearchPanelView != null && BaseStatusBar.this.mSearchPanelView.isAssistantAvailable()) {
                        BaseStatusBar.this.mSearchPanelView.show(true, true);
                    }
                case 1025:
                    if (BaseStatusBar.this.mSearchPanelView != null && BaseStatusBar.this.mSearchPanelView.isShowing()) {
                        BaseStatusBar.this.mSearchPanelView.show(false, true);
                    }
                default:
                    break;
            }
        }
    }

    private class NotificationClicker implements OnClickListener {
        private int mId;
        private PendingIntent mIntent;
        private String mPkg;
        private String mTag;

        NotificationClicker(PendingIntent intent, String pkg, String tag, int id) {
            this.mIntent = intent;
            this.mPkg = pkg;
            this.mTag = tag;
            this.mId = id;
        }

        public void onClick(View v) {
            try {
                ActivityManagerNative.getDefault().resumeAppSwitches();
                ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
            } catch (RemoteException e) {
            }
            if (this.mIntent != null) {
                int[] pos = new int[2];
                v.getLocationOnScreen(pos);
                Intent overlay = new Intent();
                overlay.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));
                try {
                    this.mIntent.send(BaseStatusBar.this.mContext, 0, overlay);
                } catch (CanceledException e2) {
                    Slog.w("StatusBar", "Sending contentIntent failed: " + e2);
                }
                KeyguardManager kgm = (KeyguardManager) BaseStatusBar.this.mContext.getSystemService("keyguard");
                if (kgm != null) {
                    kgm.exitKeyguardSecurely(null);
                }
            }
            try {
                BaseStatusBar.this.mBarService.onNotificationClick(this.mPkg, this.mTag, this.mId);
            } catch (RemoteException e3) {
            }
            BaseStatusBar.this.animateCollapse(0);
            BaseStatusBar.this.visibilityChanged(false);
        }
    }

    public class TouchOutsideListener implements OnTouchListener {
        private int mMsg;
        private StatusBarPanel mPanel;

        public TouchOutsideListener(int msg, StatusBarPanel panel) {
            this.mMsg = msg;
            this.mPanel = panel;
        }

        public boolean onTouch(View v, MotionEvent ev) {
            int action = ev.getAction();
            if (action != 4 && (action != 0 || this.mPanel.isInContentArea((int) ev.getX(), (int) ev.getY()))) {
                return false;
            }
            BaseStatusBar.this.mHandler.removeMessages(this.mMsg);
            BaseStatusBar.this.mHandler.sendEmptyMessage(this.mMsg);
            return true;
        }
    }

    protected abstract void createAndAddWindows();

    protected abstract LayoutParams getRecentsLayoutParams(ViewGroup.LayoutParams layoutParams);

    protected abstract LayoutParams getSearchLayoutParams(ViewGroup.LayoutParams layoutParams);

    protected abstract void haltTicker();

    protected abstract void setAreThereNotifications();

    protected abstract boolean shouldDisableNavbarGestures();

    protected abstract void tick(IBinder iBinder, StatusBarNotification statusBarNotification, boolean z);

    protected abstract void updateExpandedViewPos(int i);

    protected abstract void updateNotificationIcons();

    public BaseStatusBar() {
        this.mHandler = createHandler();
        this.mNotificationData = new NotificationData();
        this.mDeviceProvisioned = false;
        this.mProvisioningObserver = new AnonymousClass_1(new Handler());
        this.mOnClickHandler = new OnClickHandler() {
            public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
                boolean isActivity = pendingIntent.isActivity();
                if (isActivity) {
                    try {
                        ActivityManagerNative.getDefault().resumeAppSwitches();
                        ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
                    } catch (RemoteException e) {
                    }
                }
                boolean handled = super.onClickHandler(view, pendingIntent, fillInIntent);
                if (isActivity && handled) {
                    BaseStatusBar.this.animateCollapse(0);
                    BaseStatusBar.this.visibilityChanged(false);
                }
                return handled;
            }
        };
    }

    protected boolean isDeviceProvisioned() {
        return this.mDeviceProvisioned;
    }

    public void start() {
        boolean z;
        int i;
        boolean z2 = true;
        this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        this.mProvisioningObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("device_provisioned"), true, this.mProvisioningObserver);
        this.mWindowManager = Stub.asInterface(ServiceManager.getService("window"));
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        StatusBarIconList iconList = new StatusBarIconList();
        ArrayList<IBinder> notificationKeys = new ArrayList();
        ArrayList<StatusBarNotification> notifications = new ArrayList();
        this.mCommandQueue = new CommandQueue(this, iconList);
        int[] switches = new int[7];
        ArrayList<IBinder> binders = new ArrayList();
        try {
            this.mBarService.registerStatusBar(this.mCommandQueue, iconList, notificationKeys, notifications, switches, binders);
        } catch (RemoteException e) {
        }
        createAndAddWindows();
        disable(switches[0]);
        setSystemUiVisibility(switches[1], -1);
        if (switches[2] != 0) {
            z = true;
        } else {
            z = false;
        }
        topAppWindowChanged(z);
        setImeWindowStatus((IBinder) binders.get(0), switches[3], switches[4]);
        if (switches[5] != 0) {
            z = true;
        } else {
            z = false;
        }
        if (switches[6] == 0) {
            z2 = false;
        }
        setHardKeyboardStatus(z, z2);
        int N = iconList.size();
        int viewIndex = 0;
        for (i = 0; i < N; i++) {
            StatusBarIcon icon = iconList.getIcon(i);
            if (icon != null) {
                addIcon(iconList.getSlot(i), i, viewIndex, icon);
                viewIndex++;
            }
        }
        N = notificationKeys.size();
        if (N == notifications.size()) {
            for (i = 0; i < N; i++) {
                addNotification((IBinder) notificationKeys.get(i), (StatusBarNotification) notifications.get(i));
            }
            return;
        }
        Log.wtf("StatusBar", "Notification list length mismatch: keys=" + N + " notifications=" + notifications.size());
    }

    protected View updateNotificationVetoButton(View row, StatusBarNotification n) {
        View vetoButton = row.findViewById(2131492937);
        if (n.isClearable()) {
            vetoButton.setOnClickListener(new AnonymousClass_3(n.pkg, n.tag, n.id));
            vetoButton.setVisibility(0);
        } else {
            vetoButton.setVisibility(8);
        }
        return vetoButton;
    }

    protected void applyLegacyRowBackground(StatusBarNotification sbn, View content) {
        if (sbn.notification.contentView.getLayoutId() != 17367153) {
            int version = 0;
            try {
                version = this.mContext.getPackageManager().getApplicationInfo(sbn.pkg, 0).targetSdkVersion;
            } catch (NameNotFoundException ex) {
                Slog.e("StatusBar", "Failed looking up ApplicationInfo for " + sbn.pkg, ex);
            }
            if (version <= 0 || version >= 9) {
                content.setBackgroundResource(17302534);
            } else {
                content.setBackgroundResource(2130837582);
            }
        }
    }

    private void startApplicationDetailsActivity(String packageName) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", packageName, null));
        intent.setFlags(268435456);
        this.mContext.startActivity(intent);
    }

    protected OnLongClickListener getNotificationLongClicker() {
        return new OnLongClickListener() {

            class AnonymousClass_1 implements OnMenuItemClickListener {
                final /* synthetic */ String val$packageNameF;

                AnonymousClass_1(String str) {
                    this.val$packageNameF = str;
                }

                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() != 2131493033) {
                        return false;
                    }
                    BaseStatusBar.this.startApplicationDetailsActivity(this.val$packageNameF);
                    BaseStatusBar.this.animateCollapse(0);
                    return true;
                }
            }

            public boolean onLongClick(View v) {
                String packageNameF = (String) v.getTag();
                if (packageNameF == null || v.getWindowToken() == null) {
                    return false;
                }
                BaseStatusBar.this.mNotificationBlamePopup = new PopupMenu(BaseStatusBar.this.mContext, v);
                BaseStatusBar.this.mNotificationBlamePopup.getMenuInflater().inflate(2131689472, BaseStatusBar.this.mNotificationBlamePopup.getMenu());
                BaseStatusBar.this.mNotificationBlamePopup.setOnMenuItemClickListener(new AnonymousClass_1(packageNameF));
                BaseStatusBar.this.mNotificationBlamePopup.show();
                return true;
            }
        };
    }

    public void dismissPopups() {
        if (this.mNotificationBlamePopup != null) {
            this.mNotificationBlamePopup.dismiss();
            this.mNotificationBlamePopup = null;
        }
    }

    public void dismissIntruder() {
    }

    public void toggleRecentApps() {
        int msg = this.mRecentsPanel.getVisibility() == 0 ? 1021 : 1020;
        this.mHandler.removeMessages(msg);
        this.mHandler.sendEmptyMessage(msg);
    }

    public void preloadRecentApps() {
        this.mHandler.removeMessages(1022);
        this.mHandler.sendEmptyMessage(1022);
    }

    public void cancelPreloadRecentApps() {
        this.mHandler.removeMessages(1023);
        this.mHandler.sendEmptyMessage(1023);
    }

    public void showSearchPanel() {
        this.mHandler.removeMessages(1024);
        this.mHandler.sendEmptyMessage(1024);
    }

    public void hideSearchPanel() {
        this.mHandler.removeMessages(1025);
        this.mHandler.sendEmptyMessage(1025);
    }

    public void onRecentsPanelVisibilityChanged(boolean visible) {
    }

    protected void updateRecentsPanel(int recentsResId) {
        boolean visible = false;
        ArrayList<TaskDescription> recentTasksList = null;
        boolean firstScreenful = false;
        if (this.mRecentsPanel != null) {
            visible = this.mRecentsPanel.isShowing();
            WindowManagerImpl.getDefault().removeView(this.mRecentsPanel);
            if (visible) {
                recentTasksList = this.mRecentsPanel.getRecentTasksList();
                firstScreenful = this.mRecentsPanel.getFirstScreenful();
            }
        }
        this.mRecentsPanel = (RecentsPanelView) LayoutInflater.from(this.mContext).inflate(recentsResId, new LinearLayout(this.mContext), false);
        this.mRecentsPanel.setRecentTasksLoader(this.mRecentTasksLoader);
        this.mRecentTasksLoader.setRecentsPanel(this.mRecentsPanel);
        this.mRecentsPanel.setOnTouchListener(new TouchOutsideListener(1021, this.mRecentsPanel));
        this.mRecentsPanel.setVisibility(8);
        WindowManagerImpl.getDefault().addView(this.mRecentsPanel, getRecentsLayoutParams(this.mRecentsPanel.getLayoutParams()));
        this.mRecentsPanel.setBar(this);
        if (visible) {
            this.mRecentsPanel.show(true, false, recentTasksList, firstScreenful);
        }
    }

    protected void updateSearchPanel() {
        boolean visible = false;
        if (this.mSearchPanelView != null) {
            visible = this.mSearchPanelView.isShowing();
            WindowManagerImpl.getDefault().removeView(this.mSearchPanelView);
        }
        this.mSearchPanelView = (SearchPanelView) LayoutInflater.from(this.mContext).inflate(2130903055, new LinearLayout(this.mContext), false);
        this.mSearchPanelView.setOnTouchListener(new TouchOutsideListener(1025, this.mSearchPanelView));
        this.mSearchPanelView.setVisibility(8);
        WindowManagerImpl.getDefault().addView(this.mSearchPanelView, getSearchLayoutParams(this.mSearchPanelView.getLayoutParams()));
        this.mSearchPanelView.setBar(this);
        if (visible) {
            this.mSearchPanelView.show(true, false);
        }
    }

    protected H createHandler() {
        return new H();
    }

    protected void workAroundBadLayerDrawableOpacity(View v) {
    }

    protected boolean inflateViews(Entry entry, ViewGroup parent) {
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(2131427345);
        int minHeight = this.mContext.getResources().getDimensionPixelSize(2131427343);
        int maxHeight = this.mContext.getResources().getDimensionPixelSize(2131427344);
        StatusBarNotification sbn = entry.notification;
        RemoteViews oneU = sbn.notification.contentView;
        RemoteViews large = sbn.notification.bigContentView;
        if (oneU == null) {
            return false;
        }
        View row = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(2130903052, parent, false);
        row.setTag(sbn.pkg);
        workAroundBadLayerDrawableOpacity(row);
        updateNotificationVetoButton(row, sbn).setContentDescription(this.mContext.getString(2131296358));
        ViewGroup content = (ViewGroup) row.findViewById(2131492938);
        ViewGroup adaptive = (ViewGroup) row.findViewById(2131492939);
        content.setDescendantFocusability(393216);
        PendingIntent contentIntent = sbn.notification.contentIntent;
        if (contentIntent != null) {
            content.setOnClickListener(new NotificationClicker(contentIntent, sbn.pkg, sbn.tag, sbn.id));
        } else {
            content.setOnClickListener(null);
        }
        View expandedLarge = null;
        try {
            View expandedOneU = oneU.apply(this.mContext, adaptive, this.mOnClickHandler);
            if (large != null) {
                expandedLarge = large.apply(this.mContext, adaptive, this.mOnClickHandler);
            }
            if (expandedOneU != null) {
                ViewGroup.LayoutParams layoutParams = new SizeAdaptiveLayout.LayoutParams(expandedOneU.getLayoutParams());
                layoutParams.minHeight = minHeight;
                layoutParams.maxHeight = minHeight;
                adaptive.addView(expandedOneU, layoutParams);
            }
            if (expandedLarge != null) {
                layoutParams = new SizeAdaptiveLayout.LayoutParams(expandedLarge.getLayoutParams());
                layoutParams.minHeight = minHeight + 1;
                layoutParams.maxHeight = maxHeight;
                adaptive.addView(expandedLarge, layoutParams);
            }
            row.setDrawingCacheEnabled(true);
            applyLegacyRowBackground(sbn, content);
            row.setTag(2131492864, Boolean.valueOf(large != null));
            entry.row = row;
            entry.content = content;
            entry.expanded = expandedOneU;
            entry.setLargeView(expandedLarge);
            return true;
        } catch (RuntimeException e) {
            Slog.e("StatusBar", "couldn't inflate view for notification " + (sbn.pkg + "/0x" + Integer.toHexString(sbn.id)), e);
            return false;
        }
    }

    public NotificationClicker makeClicker(PendingIntent intent, String pkg, String tag, int id) {
        return new NotificationClicker(intent, pkg, tag, id);
    }

    protected void visibilityChanged(boolean visible) {
        if (this.mPanelSlightlyVisible != visible) {
            this.mPanelSlightlyVisible = visible;
            try {
                this.mBarService.onPanelRevealed();
            } catch (RemoteException e) {
            }
        }
    }

    void handleNotificationError(IBinder key, StatusBarNotification n, String message) {
        removeNotification(key);
        try {
            this.mBarService.onNotificationError(n.pkg, n.tag, n.id, n.uid, n.initialPid, message);
        } catch (RemoteException e) {
        }
    }

    protected StatusBarNotification removeNotificationViews(IBinder key) {
        Entry entry = this.mNotificationData.remove(key);
        if (entry == null) {
            Slog.w("StatusBar", "removeNotification for unknown key: " + key);
            return null;
        }
        ViewGroup rowParent = (ViewGroup) entry.row.getParent();
        if (rowParent != null) {
            rowParent.removeView(entry.row);
        }
        updateExpansionStates();
        updateNotificationIcons();
        return entry.notification;
    }

    protected StatusBarIconView addNotificationViews(IBinder key, StatusBarNotification notification) {
        StatusBarIconView iconView = new StatusBarIconView(this.mContext, notification.pkg + "/0x" + Integer.toHexString(notification.id), notification.notification);
        iconView.setScaleType(ScaleType.CENTER_INSIDE);
        StatusBarIcon ic = new StatusBarIcon(notification.pkg, notification.notification.icon, notification.notification.iconLevel, notification.notification.number, notification.notification.tickerText);
        if (iconView.set(ic)) {
            Entry entry = new Entry(key, notification, iconView);
            if (inflateViews(entry, this.mPile)) {
                int add = this.mNotificationData.add(entry);
                updateExpansionStates();
                updateNotificationIcons();
                return iconView;
            }
            handleNotificationError(key, notification, "Couldn't expand RemoteViews for: " + notification);
            return null;
        }
        handleNotificationError(key, notification, "Couldn't create icon: " + ic);
        return null;
    }

    protected boolean expandView(Entry entry, boolean expand) {
        int rowHeight = this.mContext.getResources().getDimensionPixelSize(2131427345);
        ViewGroup.LayoutParams lp = entry.row.getLayoutParams();
        if (entry.expandable() && expand) {
            lp.height = -2;
        } else {
            lp.height = rowHeight;
        }
        entry.row.setLayoutParams(lp);
        return expand;
    }

    protected void updateExpansionStates() {
        int N = this.mNotificationData.size();
        for (int i = 0; i < N; i++) {
            Entry entry = this.mNotificationData.get(i);
            if (i == N - 1) {
                expandView(entry, true);
            } else if (!entry.userExpanded()) {
                expandView(entry, false);
            }
        }
    }

    protected boolean isTopNotification(ViewGroup parent, Entry entry) {
        return parent != null && parent.indexOfChild(entry.row) == 0;
    }

    public void updateNotification(IBinder key, StatusBarNotification notification) {
        Entry oldEntry = this.mNotificationData.findByKey(key);
        if (oldEntry == null) {
            Slog.w("StatusBar", "updateNotification for unknown key: " + key);
            return;
        }
        StatusBarNotification oldNotification = oldEntry.notification;
        RemoteViews oldContentView = oldNotification.notification.contentView;
        RemoteViews contentView = notification.notification.contentView;
        RemoteViews oldBigContentView = oldNotification.notification.bigContentView;
        RemoteViews bigContentView = notification.notification.bigContentView;
        boolean contentsUnchanged = (oldEntry.expanded == null || contentView.getPackage() == null || oldContentView.getPackage() == null || !oldContentView.getPackage().equals(contentView.getPackage()) || oldContentView.getLayoutId() != contentView.getLayoutId()) ? false : true;
        boolean bigContentsUnchanged = (oldEntry.getLargeView() == null && bigContentView == null) || !(oldEntry.getLargeView() == null || bigContentView == null || bigContentView.getPackage() == null || oldBigContentView.getPackage() == null || !oldBigContentView.getPackage().equals(bigContentView.getPackage()) || oldBigContentView.getLayoutId() != bigContentView.getLayoutId());
        ViewGroup rowParent = (ViewGroup) oldEntry.row.getParent();
        boolean orderUnchanged = notification.notification.when == oldNotification.notification.when && notification.score == oldNotification.score;
        boolean updateTicker = (notification.notification.tickerText == null || TextUtils.equals(notification.notification.tickerText, oldEntry.notification.notification.tickerText)) ? false : true;
        boolean isTopAnyway = isTopNotification(rowParent, oldEntry);
        if (contentsUnchanged && bigContentsUnchanged && (orderUnchanged || isTopAnyway)) {
            oldEntry.notification = notification;
            try {
                contentView.reapply(this.mContext, oldEntry.expanded, this.mOnClickHandler);
                if (!(bigContentView == null || oldEntry.getLargeView() == null)) {
                    bigContentView.reapply(this.mContext, oldEntry.getLargeView(), this.mOnClickHandler);
                }
                PendingIntent contentIntent = notification.notification.contentIntent;
                if (contentIntent != null) {
                    oldEntry.content.setOnClickListener(makeClicker(contentIntent, notification.pkg, notification.tag, notification.id));
                } else {
                    oldEntry.content.setOnClickListener(null);
                }
                StatusBarIcon ic = new StatusBarIcon(notification.pkg, notification.notification.icon, notification.notification.iconLevel, notification.notification.number, notification.notification.tickerText);
                if (oldEntry.icon.set(ic)) {
                    updateExpansionStates();
                } else {
                    handleNotificationError(key, notification, "Couldn't update icon: " + ic);
                    return;
                }
            } catch (RuntimeException e) {
                Slog.w("StatusBar", "Couldn't reapply views for package " + contentView.getPackage(), e);
                removeNotificationViews(key);
                addNotificationViews(key, notification);
            }
        } else {
            boolean wasExpanded = oldEntry.userExpanded();
            removeNotificationViews(key);
            addNotificationViews(key, notification);
            if (wasExpanded) {
                Entry newEntry = this.mNotificationData.findByKey(key);
                expandView(newEntry, true);
                newEntry.setUserExpanded(true);
            }
        }
        updateNotificationVetoButton(oldEntry.row, notification);
        if (updateTicker) {
            haltTicker();
            tick(key, notification, false);
        }
        setAreThereNotifications();
        updateExpandedViewPos(-10000);
    }

    protected boolean showNotificationEvenIfUnprovisioned(StatusBarNotification sbn) {
        if ("android".equals(sbn.pkg) && sbn.notification.kind != null) {
            for (String aKind : sbn.notification.kind) {
                if ("android.system.imeswitcher".equals(aKind) || "android.system.update".equals(aKind)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean inKeyguardRestrictedInputMode() {
        return ((KeyguardManager) this.mContext.getSystemService("keyguard")).inKeyguardRestrictedInputMode();
    }
}
