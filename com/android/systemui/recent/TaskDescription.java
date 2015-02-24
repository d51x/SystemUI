package com.android.systemui.recent;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public final class TaskDescription {
    final CharSequence description;
    final Intent intent;
    private Drawable mIcon;
    private CharSequence mLabel;
    private boolean mLoaded;
    private Bitmap mThumbnail;
    final String packageName;
    final int persistentTaskId;
    final ResolveInfo resolveInfo;
    final int taskId;

    public TaskDescription(int _taskId, int _persistentTaskId, ResolveInfo _resolveInfo, Intent _intent, String _packageName, CharSequence _description) {
        this.resolveInfo = _resolveInfo;
        this.intent = _intent;
        this.taskId = _taskId;
        this.persistentTaskId = _persistentTaskId;
        this.description = _description;
        this.packageName = _packageName;
    }

    public TaskDescription() {
        this.resolveInfo = null;
        this.intent = null;
        this.taskId = -1;
        this.persistentTaskId = -1;
        this.description = null;
        this.packageName = null;
    }

    public void setLoaded(boolean loaded) {
        this.mLoaded = loaded;
    }

    public boolean isLoaded() {
        return this.mLoaded;
    }

    public boolean isNull() {
        return this.resolveInfo == null;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public void setLabel(CharSequence label) {
        this.mLabel = label;
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.mThumbnail = thumbnail;
    }

    public Bitmap getThumbnail() {
        return this.mThumbnail;
    }
}
