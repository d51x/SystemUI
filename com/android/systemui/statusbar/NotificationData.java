package com.android.systemui.statusbar;

import android.os.IBinder;
import android.view.View;
import com.android.internal.statusbar.StatusBarNotification;
import com.android.systemui.statusbar.NotificationData.Entry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

public class NotificationData {
    private final ArrayList<Entry> mEntries;
    private final Comparator<Entry> mEntryCmp;

    public static final class Entry {
        public View content;
        public View expanded;
        protected View expandedLarge;
        public StatusBarIconView icon;
        public IBinder key;
        public StatusBarNotification notification;
        public View row;

        public Entry(IBinder key, StatusBarNotification n, StatusBarIconView ic) {
            this.key = key;
            this.notification = n;
            this.icon = ic;
        }

        public void setLargeView(View expandedLarge) {
            this.expandedLarge = expandedLarge;
            NotificationData.writeBooleanTag(this.row, 2131492864, expandedLarge != null);
        }

        public View getLargeView() {
            return this.expandedLarge;
        }

        public boolean expandable() {
            return NotificationData.getIsExpandable(this.row);
        }

        public boolean userExpanded() {
            return NotificationData.getUserExpanded(this.row);
        }

        public boolean setUserExpanded(boolean userExpanded) {
            return NotificationData.setUserExpanded(this.row, userExpanded);
        }
    }

    public NotificationData() {
        this.mEntries = new ArrayList();
        this.mEntryCmp = new Comparator<Entry>() {
            public int compare(Entry a, Entry b) {
                StatusBarNotification na = a.notification;
                StatusBarNotification nb = b.notification;
                int d = na.score - nb.score;
                return d != 0 ? d : (int) (na.notification.when - nb.notification.when);
            }
        };
    }

    public int size() {
        return this.mEntries.size();
    }

    public Entry get(int i) {
        return (Entry) this.mEntries.get(i);
    }

    public Entry findByKey(IBinder key) {
        Iterator i$ = this.mEntries.iterator();
        while (i$.hasNext()) {
            Entry e = (Entry) i$.next();
            if (e.key == key) {
                return e;
            }
        }
        return null;
    }

    public int add(Entry entry) {
        int N = this.mEntries.size();
        int i = 0;
        while (i < N && this.mEntryCmp.compare(this.mEntries.get(i), entry) <= 0) {
            i++;
        }
        this.mEntries.add(i, entry);
        return i;
    }

    public Entry remove(IBinder key) {
        Entry e = findByKey(key);
        if (e != null) {
            this.mEntries.remove(e);
        }
        return e;
    }

    public boolean hasClearableItems() {
        Iterator i$ = this.mEntries.iterator();
        while (i$.hasNext()) {
            Entry e = (Entry) i$.next();
            if (e.expanded != null && e.notification.isClearable()) {
                return true;
            }
        }
        return false;
    }

    protected static boolean readBooleanTag(View view, int id) {
        if (view == null) {
            return false;
        }
        Object value = view.getTag(id);
        return value != null && (value instanceof Boolean) && ((Boolean) value).booleanValue();
    }

    protected static boolean writeBooleanTag(View view, int id, boolean value) {
        if (view == null) {
            return false;
        }
        view.setTag(id, Boolean.valueOf(value));
        return value;
    }

    public static boolean getIsExpandable(View row) {
        return readBooleanTag(row, 2131492864);
    }

    public static boolean getUserExpanded(View row) {
        return readBooleanTag(row, 2131492865);
    }

    public static boolean setUserExpanded(View row, boolean userExpanded) {
        return writeBooleanTag(row, 2131492865, userExpanded);
    }
}
