package com.android.systemui.recent;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.TaskThumbnails;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RecentTasksLoader {
    private Context mContext;
    private Bitmap mDefaultIconBackground;
    private Bitmap mDefaultThumbnailBackground;
    private final Handler mHandler;
    private int mIconDpi;
    private int mNumTasksInFirstScreenful;
    private RecentsPanelView mRecentsPanel;
    private AsyncTask<Void, ArrayList<TaskDescription>, Void> mTaskLoader;
    private AsyncTask<Void, TaskDescription, Void> mThumbnailLoader;

    class AnonymousClass_1 extends AsyncTask<Void, ArrayList<TaskDescription>, Void> {
        final /* synthetic */ LinkedBlockingQueue val$tasksWaitingForThumbnails;

        AnonymousClass_1(LinkedBlockingQueue linkedBlockingQueue) {
            this.val$tasksWaitingForThumbnails = linkedBlockingQueue;
        }

        protected void onProgressUpdate(ArrayList<TaskDescription>... values) {
            if (!isCancelled()) {
                RecentTasksLoader.this.mRecentsPanel.onTasksLoaded(values[0]);
            }
        }

        protected Void doInBackground(Void... params) {
            int origPri = Process.getThreadPriority(Process.myTid());
            Process.setThreadPriority(10);
            PackageManager pm = RecentTasksLoader.this.mContext.getPackageManager();
            List<RecentTaskInfo> recentTasks = ((ActivityManager) RecentTasksLoader.this.mContext.getSystemService("activity")).getRecentTasks(21, 2);
            int numTasks = recentTasks.size();
            ActivityInfo homeInfo = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").resolveActivityInfo(pm, 0);
            boolean firstScreenful = true;
            ArrayList arrayList = new ArrayList();
            int index = 0;
            for (int i = 1; i < numTasks && index < 21 && !isCancelled(); i++) {
                RecentTaskInfo recentInfo = (RecentTaskInfo) recentTasks.get(i);
                TaskDescription item = RecentTasksLoader.this.createTaskDescription(recentInfo.id, recentInfo.persistentId, recentInfo.baseIntent, recentInfo.origActivity, recentInfo.description, homeInfo);
                if (item != null) {
                    while (true) {
                        try {
                            this.val$tasksWaitingForThumbnails.put(item);
                            arrayList.add(item);
                            break;
                        } catch (InterruptedException e) {
                        }
                    }
                    if (firstScreenful && arrayList.size() == RecentTasksLoader.this.mNumTasksInFirstScreenful) {
                        publishProgress(new ArrayList[]{arrayList});
                        arrayList = new ArrayList();
                        firstScreenful = false;
                    }
                    index++;
                }
            }
            if (!isCancelled()) {
                publishProgress(new ArrayList[]{arrayList});
                if (firstScreenful) {
                    publishProgress(new ArrayList[]{new ArrayList()});
                }
            }
            while (true) {
                try {
                    continue;
                    this.val$tasksWaitingForThumbnails.put(new TaskDescription());
                    Process.setThreadPriority(origPri);
                    break;
                } catch (InterruptedException e2) {
                }
            }
            return null;
        }
    }

    class AnonymousClass_2 extends AsyncTask<Void, TaskDescription, Void> {
        final /* synthetic */ BlockingQueue val$tasksWaitingForThumbnails;

        AnonymousClass_2(BlockingQueue blockingQueue) {
            this.val$tasksWaitingForThumbnails = blockingQueue;
        }

        protected void onProgressUpdate(TaskDescription... values) {
            if (!isCancelled()) {
                RecentTasksLoader.this.mRecentsPanel.onTaskThumbnailLoaded(values[0]);
            }
        }

        protected Void doInBackground(Void... params) {
            int origPri = Process.getThreadPriority(Process.myTid());
            Process.setThreadPriority(10);
            while (!isCancelled()) {
                TaskDescription td = null;
                while (td == null) {
                    try {
                        td = (TaskDescription) this.val$tasksWaitingForThumbnails.take();
                    } catch (InterruptedException e) {
                    }
                }
                if (td.isNull()) {
                    break;
                }
                RecentTasksLoader.this.loadThumbnailAndIcon(td);
                synchronized (td) {
                    publishProgress(new TaskDescription[]{td});
                }
            }
            Process.setThreadPriority(origPri);
            return null;
        }
    }

    public RecentTasksLoader(Context context) {
        this.mContext = context;
        Resources res = context.getResources();
        if (res.getBoolean(2131230720)) {
            this.mIconDpi = ((ActivityManager) context.getSystemService("activity")).getLauncherLargeIconDensity();
        } else {
            this.mIconDpi = res.getDisplayMetrics().densityDpi;
        }
        int iconSize = (this.mIconDpi * res.getDimensionPixelSize(17104896)) / res.getDisplayMetrics().densityDpi;
        this.mDefaultIconBackground = Bitmap.createBitmap(iconSize, iconSize, Config.ARGB_8888);
        int thumbnailWidth = res.getDimensionPixelSize(17104898);
        int thumbnailHeight = res.getDimensionPixelSize(17104897);
        int color = res.getColor(2130837716);
        this.mDefaultThumbnailBackground = Bitmap.createBitmap(thumbnailWidth, thumbnailHeight, Config.ARGB_8888);
        new Canvas(this.mDefaultThumbnailBackground).drawColor(color);
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mHandler = new Handler();
    }

    public void setRecentsPanel(RecentsPanelView recentsPanel) {
        this.mRecentsPanel = recentsPanel;
        this.mNumTasksInFirstScreenful = this.mRecentsPanel.numItemsInOneScreenful();
    }

    public Bitmap getDefaultThumbnail() {
        return this.mDefaultThumbnailBackground;
    }

    public Bitmap getDefaultIcon() {
        return this.mDefaultIconBackground;
    }

    TaskDescription createTaskDescription(int taskId, int persistentTaskId, Intent baseIntent, ComponentName origActivity, CharSequence description, ActivityInfo homeInfo) {
        Intent intent = new Intent(baseIntent);
        if (origActivity != null) {
            intent.setComponent(origActivity);
        }
        PackageManager pm = this.mContext.getPackageManager();
        if (homeInfo == null) {
            homeInfo = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").resolveActivityInfo(pm, 0);
        }
        if ("com.tw.service".equals(intent.getComponent().getPackageName()) || "com.tw.recorder".equals(intent.getComponent().getPackageName()) || (homeInfo != null && homeInfo.packageName.equals(intent.getComponent().getPackageName()) && homeInfo.name.equals(intent.getComponent().getClassName()))) {
            return null;
        }
        intent.setFlags((intent.getFlags() & -2097153) | 268435456);
        ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        if (resolveInfo != null) {
            ActivityInfo info = resolveInfo.activityInfo;
            String title = info.loadLabel(pm).toString();
            if (title != null && title.length() > 0) {
                TaskDescription item = new TaskDescription(taskId, persistentTaskId, resolveInfo, baseIntent, info.packageName, description);
                item.setLabel(title);
                return item;
            }
        }
        return null;
    }

    void loadThumbnailAndIcon(TaskDescription td) {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        PackageManager pm = this.mContext.getPackageManager();
        TaskThumbnails thumbs = am.getTaskThumbnails(td.persistentTaskId);
        Drawable icon = getFullResIcon(td.resolveInfo, pm);
        synchronized (td) {
            if (thumbs != null) {
                if (thumbs.mainThumbnail != null) {
                    td.setThumbnail(thumbs.mainThumbnail);
                    if (icon != null) {
                        td.setIcon(icon);
                    }
                    td.setLoaded(true);
                }
            }
            td.setThumbnail(this.mDefaultThumbnailBackground);
            if (icon != null) {
                td.setIcon(icon);
            }
            td.setLoaded(true);
        }
    }

    Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), 17629184);
    }

    Drawable getFullResIcon(Resources resources, int iconId) {
        try {
            return resources.getDrawableForDensity(iconId, this.mIconDpi);
        } catch (NotFoundException e) {
            return getFullResDefaultActivityIcon();
        }
    }

    private Drawable getFullResIcon(ResolveInfo info, PackageManager packageManager) {
        try {
            Resources resources = packageManager.getResourcesForApplication(info.activityInfo.applicationInfo);
        } catch (NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.activityInfo.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    public void cancelLoadingThumbnailsAndIcons() {
        if (this.mTaskLoader != null) {
            this.mTaskLoader.cancel(false);
            this.mTaskLoader = null;
        }
        if (this.mThumbnailLoader != null) {
            this.mThumbnailLoader.cancel(false);
            this.mThumbnailLoader = null;
        }
    }

    public void loadTasksInBackground() {
        cancelLoadingThumbnailsAndIcons();
        LinkedBlockingQueue<TaskDescription> tasksWaitingForThumbnails = new LinkedBlockingQueue();
        ArrayList arrayList = new ArrayList();
        this.mTaskLoader = new AnonymousClass_1(tasksWaitingForThumbnails);
        this.mTaskLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        loadThumbnailsAndIconsInBackground(tasksWaitingForThumbnails);
    }

    private void loadThumbnailsAndIconsInBackground(BlockingQueue<TaskDescription> tasksWaitingForThumbnails) {
        this.mThumbnailLoader = new AnonymousClass_2(tasksWaitingForThumbnails);
        this.mThumbnailLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }
}
