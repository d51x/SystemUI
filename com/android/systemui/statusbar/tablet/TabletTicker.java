package com.android.systemui.statusbar.tablet;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerImpl;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarNotification;
import com.android.systemui.statusbar.StatusBarIconView;

public class TabletTicker extends Handler implements TransitionListener {
    private TabletStatusBar mBar;
    private Context mContext;
    private IBinder mCurrentKey;
    private StatusBarNotification mCurrentNotification;
    private View mCurrentView;
    private IBinder[] mKeys;
    private final int mLargeIconHeight;
    private LayoutTransition mLayoutTransition;
    private StatusBarNotification[] mQueue;
    private int mQueuePos;
    private ViewGroup mWindow;
    private boolean mWindowShouldClose;

    class AnonymousClass_1 implements OnClickListener {
        final /* synthetic */ OnClickListener val$clicker;

        AnonymousClass_1(OnClickListener onClickListener) {
            this.val$clicker = onClickListener;
        }

        public void onClick(View v) {
            TabletTicker.this.halt();
            this.val$clicker.onClick(v);
        }
    }

    public TabletTicker(TabletStatusBar bar) {
        this.mKeys = new IBinder[3];
        this.mQueue = new StatusBarNotification[3];
        this.mBar = bar;
        this.mContext = bar.getContext();
        this.mLargeIconHeight = this.mContext.getResources().getDimensionPixelSize(17104902);
    }

    public void add(IBinder key, StatusBarNotification notification) {
        remove(key, false);
        this.mKeys[this.mQueuePos] = key;
        this.mQueue[this.mQueuePos] = notification;
        if (this.mQueuePos == 0 && this.mCurrentNotification == null) {
            sendEmptyMessage(1);
        }
        if (this.mQueuePos < 2) {
            this.mQueuePos++;
        }
    }

    public void remove(IBinder key) {
        remove(key, true);
    }

    public void remove(IBinder key, boolean advance) {
        if (this.mCurrentKey != key) {
            int i = 0;
            while (i < 3) {
                if (this.mKeys[i] == key) {
                    while (i < 2) {
                        this.mKeys[i] = this.mKeys[i + 1];
                        this.mQueue[i] = this.mQueue[i + 1];
                        i++;
                    }
                    this.mKeys[2] = null;
                    this.mQueue[2] = null;
                    if (this.mQueuePos > 0) {
                        this.mQueuePos--;
                        return;
                    }
                    return;
                }
                i++;
            }
        } else if (advance) {
            removeMessages(1);
            sendEmptyMessage(1);
        }
    }

    public void halt() {
        removeMessages(1);
        if (this.mCurrentView != null || this.mQueuePos != 0) {
            for (int i = 0; i < 3; i++) {
                this.mKeys[i] = null;
                this.mQueue[i] = null;
            }
            this.mQueuePos = 0;
            sendEmptyMessage(1);
        }
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                advance();
            default:
                break;
        }
    }

    private void advance() {
        boolean z = true;
        if (this.mCurrentView != null) {
            if (this.mWindow != null) {
                this.mWindow.removeView(this.mCurrentView);
            }
            this.mCurrentView = null;
            this.mCurrentKey = null;
            this.mCurrentNotification = null;
        }
        dequeue();
        while (this.mCurrentNotification != null) {
            this.mCurrentView = makeTickerView(this.mCurrentNotification);
            if (this.mCurrentView != null) {
                if (this.mWindow == null) {
                    this.mWindow = makeWindow();
                    WindowManagerImpl.getDefault().addView(this.mWindow, this.mWindow.getLayoutParams());
                }
                this.mWindow.addView(this.mCurrentView);
                sendEmptyMessageDelayed(1, 5000);
                if (this.mCurrentView != null || this.mWindow == null) {
                    z = false;
                }
                this.mWindowShouldClose = z;
            }
            dequeue();
        }
        z = false;
        this.mWindowShouldClose = z;
    }

    private void dequeue() {
        this.mCurrentKey = this.mKeys[0];
        this.mCurrentNotification = this.mQueue[0];
        int N = this.mQueuePos;
        for (int i = 0; i < N; i++) {
            this.mKeys[i] = this.mKeys[i + 1];
            this.mQueue[i] = this.mQueue[i + 1];
        }
        this.mKeys[N] = null;
        this.mQueue[N] = null;
        if (this.mQueuePos > 0) {
            this.mQueuePos--;
        }
    }

    private ViewGroup makeWindow() {
        Resources res = this.mContext.getResources();
        FrameLayout view = new FrameLayout(this.mContext);
        LayoutParams lp = new LayoutParams(res.getDimensionPixelSize(2131427386), this.mLargeIconHeight, 2024, 776 | 32, -3);
        lp.gravity = 85;
        this.mLayoutTransition = new LayoutTransition();
        this.mLayoutTransition.addTransitionListener(this);
        view.setLayoutTransition(this.mLayoutTransition);
        lp.setTitle("NotificationTicker");
        view.setLayoutParams(lp);
        return view;
    }

    public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
    }

    public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
        if (this.mWindowShouldClose) {
            WindowManagerImpl.getDefault().removeView(this.mWindow);
            this.mWindow = null;
            this.mWindowShouldClose = false;
            this.mBar.doneTicking();
        }
    }

    private View makeTickerView(StatusBarNotification notification) {
        int iconId;
        Notification n = notification.notification;
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        if (n.largeIcon != null) {
            iconId = 2131493031;
        } else {
            iconId = 2131493029;
        }
        ViewGroup group;
        if (n.tickerView != null) {
            group = (ViewGroup) inflater.inflate(2130903073, null, false);
            FrameLayout content = (FrameLayout) group.findViewById(2131493032);
            View expanded = null;
            Exception exception = null;
            try {
                expanded = n.tickerView.apply(this.mContext, content);
            } catch (Exception e) {
                exception = e;
            }
            if (expanded == null) {
                Slog.e("StatusBar.TabletTicker", "couldn't inflate view for notification " + (notification.pkg + "/0x" + Integer.toHexString(notification.id)), exception);
                return null;
            }
            content.addView(expanded, new FrameLayout.LayoutParams(-1, -1));
        } else if (n.tickerText != null) {
            group = (ViewGroup) inflater.inflate(2130903072, this.mWindow, false);
            ImageView iv = (ImageView) group.findViewById(iconId);
            iv.setImageDrawable(StatusBarIconView.getIcon(this.mContext, new StatusBarIcon(notification.pkg, n.icon, n.iconLevel, 0, n.tickerText)));
            iv.setVisibility(0);
            ((TextView) group.findViewById(2131493030)).setText(n.tickerText);
        } else {
            throw new RuntimeException("tickerView==null && tickerText==null");
        }
        ImageView largeIcon = (ImageView) group.findViewById(2131493028);
        if (n.largeIcon != null) {
            largeIcon.setImageBitmap(n.largeIcon);
            largeIcon.setVisibility(0);
            ViewGroup.LayoutParams lp = largeIcon.getLayoutParams();
            int statusBarHeight = this.mBar.getStatusBarHeight();
            if (n.largeIcon.getHeight() <= statusBarHeight) {
                lp.height = statusBarHeight;
            } else {
                lp.height = this.mLargeIconHeight;
            }
            largeIcon.setLayoutParams(lp);
        }
        PendingIntent contentIntent = notification.notification.contentIntent;
        if (contentIntent != null) {
            group.setOnClickListener(new AnonymousClass_1(this.mBar.makeClicker(contentIntent, notification.pkg, notification.tag, notification.id)));
        } else {
            group.setOnClickListener(null);
        }
        return group;
    }
}
