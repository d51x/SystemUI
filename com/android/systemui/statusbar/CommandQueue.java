package com.android.systemui.statusbar;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import com.android.internal.statusbar.IStatusBar.Stub;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarIconList;
import com.android.internal.statusbar.StatusBarNotification;
import com.android.systemui.statusbar.CommandQueue.Callbacks;

public class CommandQueue extends Stub {
    private Callbacks mCallbacks;
    private Handler mHandler;
    private StatusBarIconList mList;

    public static interface Callbacks {
        void addIcon(String str, int i, int i2, StatusBarIcon statusBarIcon);

        void addNotification(IBinder iBinder, StatusBarNotification statusBarNotification);

        void animateCollapse(int i);

        void animateExpand();

        void cancelPreloadRecentApps();

        void disable(int i);

        void preloadRecentApps();

        void removeIcon(String str, int i, int i2);

        void removeNotification(IBinder iBinder);

        void setHardKeyboardStatus(boolean z, boolean z2);

        void setImeWindowStatus(IBinder iBinder, int i, int i2);

        void setNavigationIconHints(int i);

        void setSystemUiVisibility(int i, int i2);

        void toggleRecentApps();

        void topAppWindowChanged(boolean z);

        void updateIcon(String str, int i, int i2, StatusBarIcon statusBarIcon, StatusBarIcon statusBarIcon2);

        void updateNotification(IBinder iBinder, StatusBarNotification statusBarNotification);
    }

    private final class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            NotificationQueueEntry ne;
            switch (msg.what & -65536) {
                case 65536:
                    int index = msg.what & 65535;
                    int viewIndex = CommandQueue.this.mList.getViewIndex(index);
                    switch (msg.arg1) {
                        case 1:
                            StatusBarIcon icon = (StatusBarIcon) msg.obj;
                            StatusBarIcon old = CommandQueue.this.mList.getIcon(index);
                            if (old == null) {
                                CommandQueue.this.mList.setIcon(index, icon);
                                CommandQueue.this.mCallbacks.addIcon(CommandQueue.this.mList.getSlot(index), index, viewIndex, icon);
                                return;
                            }
                            CommandQueue.this.mList.setIcon(index, icon);
                            CommandQueue.this.mCallbacks.updateIcon(CommandQueue.this.mList.getSlot(index), index, viewIndex, old, icon);
                        case 2:
                            if (CommandQueue.this.mList.getIcon(index) != null) {
                                CommandQueue.this.mList.removeIcon(index);
                                CommandQueue.this.mCallbacks.removeIcon(CommandQueue.this.mList.getSlot(index), index, viewIndex);
                            }
                        default:
                            break;
                    }
                case 131072:
                    ne = (NotificationQueueEntry) msg.obj;
                    CommandQueue.this.mCallbacks.addNotification(ne.key, ne.notification);
                case 196608:
                    ne = (NotificationQueueEntry) msg.obj;
                    CommandQueue.this.mCallbacks.updateNotification(ne.key, ne.notification);
                case 262144:
                    CommandQueue.this.mCallbacks.removeNotification((IBinder) msg.obj);
                case 327680:
                    CommandQueue.this.mCallbacks.disable(msg.arg1);
                case 393216:
                    if (msg.arg1 == 1) {
                        CommandQueue.this.mCallbacks.animateExpand();
                    } else {
                        CommandQueue.this.mCallbacks.animateCollapse(msg.arg2);
                    }
                case 458752:
                    CommandQueue.this.mCallbacks.setSystemUiVisibility(msg.arg1, msg.arg2);
                case 524288:
                    Callbacks access$300 = CommandQueue.this.mCallbacks;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    access$300.topAppWindowChanged(z);
                case 589824:
                    CommandQueue.this.mCallbacks.setImeWindowStatus((IBinder) msg.obj, msg.arg1, msg.arg2);
                case 655360:
                    Callbacks access$3002 = CommandQueue.this.mCallbacks;
                    boolean z2 = msg.arg1 != 0;
                    if (msg.arg2 == 0) {
                        z = false;
                    }
                    access$3002.setHardKeyboardStatus(z2, z);
                case 720896:
                    CommandQueue.this.mCallbacks.toggleRecentApps();
                case 786432:
                    CommandQueue.this.mCallbacks.preloadRecentApps();
                case 851968:
                    CommandQueue.this.mCallbacks.cancelPreloadRecentApps();
                case 917504:
                    CommandQueue.this.mCallbacks.setNavigationIconHints(msg.arg1);
                default:
                    break;
            }
        }
    }

    private class NotificationQueueEntry {
        IBinder key;
        StatusBarNotification notification;

        private NotificationQueueEntry() {
        }
    }

    public CommandQueue(Callbacks callbacks, StatusBarIconList list) {
        this.mHandler = new H();
        this.mCallbacks = callbacks;
        this.mList = list;
    }

    public void setIcon(int index, StatusBarIcon icon) {
        synchronized (this.mList) {
            int what = 65536 | index;
            this.mHandler.removeMessages(what);
            this.mHandler.obtainMessage(what, 1, 0, icon.clone()).sendToTarget();
        }
    }

    public void removeIcon(int index) {
        synchronized (this.mList) {
            int what = 65536 | index;
            this.mHandler.removeMessages(what);
            this.mHandler.obtainMessage(what, 2, 0, null).sendToTarget();
        }
    }

    public void addNotification(IBinder key, StatusBarNotification notification) {
        synchronized (this.mList) {
            NotificationQueueEntry ne = new NotificationQueueEntry();
            ne.key = key;
            ne.notification = notification;
            this.mHandler.obtainMessage(131072, 0, 0, ne).sendToTarget();
        }
    }

    public void updateNotification(IBinder key, StatusBarNotification notification) {
        synchronized (this.mList) {
            NotificationQueueEntry ne = new NotificationQueueEntry();
            ne.key = key;
            ne.notification = notification;
            this.mHandler.obtainMessage(196608, 0, 0, ne).sendToTarget();
        }
    }

    public void removeNotification(IBinder key) {
        synchronized (this.mList) {
            this.mHandler.obtainMessage(262144, 0, 0, key).sendToTarget();
        }
    }

    public void disable(int state) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(327680);
            this.mHandler.obtainMessage(327680, state, 0, null).sendToTarget();
        }
    }

    public void animateExpand() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(393216);
            this.mHandler.obtainMessage(393216, 1, 0, null).sendToTarget();
        }
    }

    public void animateCollapse() {
        animateCollapse(0);
    }

    public void animateCollapse(int flags) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(393216);
            this.mHandler.obtainMessage(393216, 2, flags, null).sendToTarget();
        }
    }

    public void setSystemUiVisibility(int vis, int mask) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(458752);
            this.mHandler.obtainMessage(458752, vis, mask, null).sendToTarget();
        }
    }

    public void topAppWindowChanged(boolean menuVisible) {
        int i = 0;
        synchronized (this.mList) {
            this.mHandler.removeMessages(524288);
            Handler handler = this.mHandler;
            if (menuVisible) {
                i = 1;
            }
            handler.obtainMessage(524288, i, 0, null).sendToTarget();
        }
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(589824);
            this.mHandler.obtainMessage(589824, vis, backDisposition, token).sendToTarget();
        }
    }

    public void setHardKeyboardStatus(boolean available, boolean enabled) {
        int i = 1;
        synchronized (this.mList) {
            int i2;
            this.mHandler.removeMessages(655360);
            Handler handler = this.mHandler;
            if (available) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            if (!enabled) {
                i = 0;
            }
            handler.obtainMessage(655360, i2, i).sendToTarget();
        }
    }

    public void toggleRecentApps() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(720896);
            this.mHandler.obtainMessage(720896, 0, 0, null).sendToTarget();
        }
    }

    public void preloadRecentApps() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(786432);
            this.mHandler.obtainMessage(786432, 0, 0, null).sendToTarget();
        }
    }

    public void cancelPreloadRecentApps() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(851968);
            this.mHandler.obtainMessage(851968, 0, 0, null).sendToTarget();
        }
    }

    public void setNavigationIconHints(int hints) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(917504);
            this.mHandler.obtainMessage(917504, hints, 0, null).sendToTarget();
        }
    }
}
