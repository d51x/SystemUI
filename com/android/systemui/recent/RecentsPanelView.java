package com.android.systemui.recent;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.ActivityOptions.OnAnimationStartedListener;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IWindowManager.Stub;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.tablet.StatusBarPanel;
import java.util.ArrayList;

public class RecentsPanelView extends FrameLayout implements AnimatorListener, OnTouchListener, OnItemClickListener, RecentsCallback, StatusBarPanel {
    private BaseStatusBar mBar;
    private Choreographer mChoreo;
    private Context mContext;
    private boolean mFirstScreenful;
    private boolean mFitThumbnailToXY;
    boolean mHideRecentsAfterThumbnailScaleUpStarted;
    private boolean mHighEndGfx;
    private TaskDescriptionAdapter mListAdapter;
    private int mNumItemsWaitingForThumbnailsAndIcons;
    ImageView mPlaceholderThumbnail;
    private PopupMenu mPopup;
    private Runnable mPreloadTasksRunnable;
    private boolean mReadyToShow;
    private int mRecentItemLayoutId;
    private ArrayList<TaskDescription> mRecentTaskDescriptions;
    private boolean mRecentTasksDirty;
    private RecentTasksLoader mRecentTasksLoader;
    private ViewGroup mRecentsContainer;
    private View mRecentsNoApps;
    private View mRecentsScrim;
    private boolean mShowing;
    private StatusBarTouchProxy mStatusBarTouchProxy;
    boolean mThumbnailScaleUpStarted;
    private int mThumbnailWidth;
    View mTransitionBg;
    OnRecentsPanelVisibilityChangedListener mVisibilityChangedListener;
    private boolean mWaitingToShow;
    private boolean mWaitingToShowAnimated;

    public static interface RecentsScrollView {
        int numItemsInOneScreenful();

        void setAdapter(TaskDescriptionAdapter taskDescriptionAdapter);

        void setCallback(RecentsCallback recentsCallback);

        void setMinSwipeAlpha(float f);
    }

    class AnonymousClass_3 implements OnMenuItemClickListener {
        final /* synthetic */ View val$selectedView;

        AnonymousClass_3(View view) {
            this.val$selectedView = view;
        }

        public boolean onMenuItemClick(MenuItem item) {
            if (item.getItemId() == 2131493034) {
                RecentsPanelView.this.mRecentsContainer.removeViewInLayout(this.val$selectedView);
            } else if (item.getItemId() != 2131493035) {
                return false;
            } else {
                ViewHolder viewHolder = (ViewHolder) this.val$selectedView.getTag();
                if (viewHolder != null) {
                    RecentsPanelView.this.startApplicationDetailsActivity(viewHolder.taskDescription.packageName);
                    RecentsPanelView.this.mBar.animateCollapse(0);
                } else {
                    throw new IllegalStateException("Oops, no tag on view " + this.val$selectedView);
                }
            }
            return true;
        }
    }

    class AnonymousClass_4 implements OnDismissListener {
        final /* synthetic */ View val$thumbnailView;

        AnonymousClass_4(View view) {
            this.val$thumbnailView = view;
        }

        public void onDismiss(PopupMenu menu) {
            this.val$thumbnailView.setSelected(false);
            RecentsPanelView.this.mPopup = null;
        }
    }

    private final class OnLongClickDelegate implements OnLongClickListener {
        View mOtherView;

        OnLongClickDelegate(View other) {
            this.mOtherView = other;
        }

        public boolean onLongClick(View v) {
            return this.mOtherView.performLongClick();
        }
    }

    public static interface OnRecentsPanelVisibilityChangedListener {
        void onRecentsPanelVisibilityChanged(boolean z);
    }

    final class TaskDescriptionAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public TaskDescriptionAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return RecentsPanelView.this.mRecentTaskDescriptions != null ? RecentsPanelView.this.mRecentTaskDescriptions.size() : 0;
        }

        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View createView(ViewGroup parent) {
            View convertView = this.mInflater.inflate(RecentsPanelView.this.mRecentItemLayoutId, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.thumbnailView = convertView.findViewById(2131492942);
            holder.thumbnailViewImage = (ImageView) convertView.findViewById(2131492943);
            if (RecentsPanelView.this.mRecentTasksLoader != null) {
                RecentsPanelView.this.updateThumbnail(holder, RecentsPanelView.this.mRecentTasksLoader.getDefaultThumbnail(), false, false);
            }
            holder.iconView = (ImageView) convertView.findViewById(2131492944);
            if (RecentsPanelView.this.mRecentTasksLoader != null) {
                holder.iconView.setImageBitmap(RecentsPanelView.this.mRecentTasksLoader.getDefaultIcon());
            }
            holder.labelView = (TextView) convertView.findViewById(2131492945);
            holder.descriptionView = (TextView) convertView.findViewById(2131492946);
            convertView.setTag(holder);
            return convertView;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createView(parent);
                if (convertView.getParent() != null) {
                    throw new RuntimeException("Recycled child has parent");
                }
            } else if (convertView.getParent() != null) {
                throw new RuntimeException("Recycled child has parent");
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            TaskDescription td = (TaskDescription) RecentsPanelView.this.mRecentTaskDescriptions.get((RecentsPanelView.this.mRecentTaskDescriptions.size() - position) - 1);
            holder.labelView.setText(td.getLabel());
            holder.thumbnailView.setContentDescription(td.getLabel());
            holder.loadedThumbnailAndIcon = td.isLoaded();
            if (td.isLoaded()) {
                RecentsPanelView.this.updateThumbnail(holder, td.getThumbnail(), true, false);
                RecentsPanelView.this.updateIcon(holder, td.getIcon(), true, false);
                RecentsPanelView.access$510(RecentsPanelView.this);
            }
            holder.thumbnailView.setTag(td);
            holder.thumbnailView.setOnLongClickListener(new OnLongClickDelegate(convertView));
            holder.taskDescription = td;
            return convertView;
        }

        public void recycleView(View v) {
            ViewHolder holder = (ViewHolder) v.getTag();
            RecentsPanelView.this.updateThumbnail(holder, RecentsPanelView.this.mRecentTasksLoader.getDefaultThumbnail(), false, false);
            holder.iconView.setImageBitmap(RecentsPanelView.this.mRecentTasksLoader.getDefaultIcon());
            holder.iconView.setVisibility(4);
            holder.labelView.setText(null);
            holder.thumbnailView.setContentDescription(null);
            holder.thumbnailView.setTag(null);
            holder.thumbnailView.setOnLongClickListener(null);
            holder.thumbnailView.setVisibility(4);
            holder.taskDescription = null;
            holder.loadedThumbnailAndIcon = false;
        }
    }

    static final class ViewHolder {
        TextView descriptionView;
        ImageView iconView;
        TextView labelView;
        boolean loadedThumbnailAndIcon;
        TaskDescription taskDescription;
        View thumbnailView;
        ImageView thumbnailViewImage;
        Bitmap thumbnailViewImageBitmap;

        ViewHolder() {
        }
    }

    static /* synthetic */ int access$510(RecentsPanelView x0) {
        int i = x0.mNumItemsWaitingForThumbnailsAndIcons;
        x0.mNumItemsWaitingForThumbnailsAndIcons = i - 1;
        return i;
    }

    public RecentsPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mRecentTasksDirty = true;
        this.mFirstScreenful = true;
        this.mContext = context;
        updateValuesFromResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecentsPanelView, defStyle, 0);
        this.mRecentItemLayoutId = a.getResourceId(0, 0);
        a.recycle();
    }

    public int numItemsInOneScreenful() {
        if (this.mRecentsContainer instanceof RecentsScrollView) {
            return ((RecentsScrollView) this.mRecentsContainer).numItemsInOneScreenful();
        }
        throw new IllegalArgumentException("missing Recents[Horizontal]ScrollView");
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != 4 || event.isCanceled()) {
            return super.onKeyUp(keyCode, event);
        }
        show(false, false);
        return true;
    }

    private boolean pointInside(int x, int y, View v) {
        return x >= v.getLeft() && x < v.getRight() && y >= v.getTop() && y < v.getBottom();
    }

    public boolean isInContentArea(int x, int y) {
        if (pointInside(x, y, this.mRecentsContainer)) {
            return true;
        }
        return this.mStatusBarTouchProxy != null && pointInside(x, y, this.mStatusBarTouchProxy);
    }

    public void show(boolean show, boolean animate) {
        if (show) {
            refreshRecentTasksList(null, true);
            this.mWaitingToShow = true;
            this.mWaitingToShowAnimated = animate;
            showIfReady();
            return;
        }
        show(show, animate, null, false);
    }

    private void showIfReady() {
        if (this.mWaitingToShow && this.mReadyToShow) {
            show(true, this.mWaitingToShowAnimated, null, false);
        }
    }

    static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    public void show(boolean show, boolean animate, ArrayList<TaskDescription> recentTaskDescriptions, boolean firstScreenful) {
        int i = 0;
        sendCloseSystemWindows(this.mContext, "recentapps");
        if (show) {
            refreshRecentTasksList(recentTaskDescriptions, firstScreenful);
            boolean noApps = !this.mFirstScreenful && this.mRecentTaskDescriptions.size() == 0;
            if (this.mRecentsNoApps != null) {
                int i2;
                this.mRecentsNoApps.setAlpha(1.0f);
                View view = this.mRecentsNoApps;
                if (noApps) {
                    i2 = 0;
                } else {
                    i2 = 4;
                }
                view.setVisibility(i2);
            } else if (noApps) {
                this.mRecentTasksLoader.cancelLoadingThumbnailsAndIcons();
                this.mRecentTasksDirty = true;
                this.mWaitingToShow = false;
                this.mReadyToShow = false;
                return;
            }
        }
        this.mRecentTasksLoader.cancelLoadingThumbnailsAndIcons();
        this.mRecentTasksDirty = true;
        this.mWaitingToShow = false;
        this.mReadyToShow = false;
        if (!animate) {
            this.mShowing = show;
            if (!show) {
                i = 8;
            }
            setVisibility(i);
            this.mChoreo.jumpTo(show);
            onAnimationEnd(null);
        } else if (this.mShowing != show) {
            this.mShowing = show;
            if (show) {
                setVisibility(0);
            }
            this.mChoreo.startAnimation(show);
        }
        if (show) {
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        } else if (this.mPopup != null) {
            this.mPopup.dismiss();
        }
    }

    public void dismiss() {
        hide(true);
    }

    public void hide(boolean animate) {
        if (!animate) {
            setVisibility(8);
        }
        if (this.mBar != null) {
            this.mBar.animateCollapse(0);
        }
    }

    public void onAnimationCancel(Animator animation) {
    }

    public void onAnimationEnd(Animator animation) {
        if (this.mShowing) {
            LayoutTransition transitioner = new LayoutTransition();
            this.mRecentsContainer.setLayoutTransition(transitioner);
            createCustomAnimations(transitioner);
            return;
        }
        this.mRecentsContainer.setLayoutTransition(null);
        clearRecentTasksList();
    }

    public void onAnimationRepeat(Animator animation) {
    }

    public void onAnimationStart(Animator animation) {
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.mChoreo.setPanelHeight(this.mRecentsContainer.getHeight());
    }

    public boolean dispatchHoverEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        return (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) ? true : super.dispatchHoverEvent(event);
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public void setBar(BaseStatusBar bar) {
        this.mBar = bar;
    }

    public void setStatusBarView(View statusBarView) {
        if (this.mStatusBarTouchProxy != null) {
            this.mStatusBarTouchProxy.setStatusBar(statusBarView);
        }
    }

    public void setRecentTasksLoader(RecentTasksLoader loader) {
        this.mRecentTasksLoader = loader;
    }

    public void setVisibility(int visibility) {
        if (this.mVisibilityChangedListener != null) {
            this.mVisibilityChangedListener.onRecentsPanelVisibilityChanged(visibility == 0);
        }
        super.setVisibility(visibility);
    }

    public void updateValuesFromResources() {
        Resources res = this.mContext.getResources();
        this.mThumbnailWidth = Math.round(res.getDimension(2131427332));
        this.mFitThumbnailToXY = res.getBoolean(2131230721);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContext.getSystemService("layout_inflater");
        this.mRecentsContainer = (ViewGroup) findViewById(2131492952);
        this.mStatusBarTouchProxy = (StatusBarTouchProxy) findViewById(2131493009);
        this.mListAdapter = new TaskDescriptionAdapter(this.mContext);
        if (this.mRecentsContainer instanceof RecentsScrollView) {
            RecentsScrollView scrollView = (RecentsScrollView) this.mRecentsContainer;
            scrollView.setAdapter(this.mListAdapter);
            scrollView.setCallback(this);
            this.mRecentsScrim = findViewById(2131492950);
            this.mRecentsNoApps = findViewById(2131492954);
            this.mChoreo = new Choreographer(this, this.mRecentsScrim, this.mRecentsContainer, this.mRecentsNoApps, this);
            if (this.mRecentsScrim != null) {
                this.mHighEndGfx = ActivityManager.isHighEndGfx(((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay());
                if (!this.mHighEndGfx) {
                    this.mRecentsScrim.setBackground(null);
                } else if (this.mRecentsScrim.getBackground() instanceof BitmapDrawable) {
                    ((BitmapDrawable) this.mRecentsScrim.getBackground()).setTileModeY(TileMode.REPEAT);
                }
            }
            this.mPreloadTasksRunnable = new Runnable() {
                public void run() {
                    if (!RecentsPanelView.this.mShowing) {
                        RecentsPanelView.this.setVisibility(4);
                        RecentsPanelView.this.refreshRecentTasksList();
                    }
                }
            };
            return;
        }
        throw new IllegalArgumentException("missing Recents[Horizontal]ScrollView");
    }

    public void setMinSwipeAlpha(float minAlpha) {
        if (this.mRecentsContainer instanceof RecentsScrollView) {
            ((RecentsScrollView) this.mRecentsContainer).setMinSwipeAlpha(minAlpha);
        }
    }

    private void createCustomAnimations(LayoutTransition transitioner) {
        transitioner.setDuration(200);
        transitioner.setStartDelay(1, 0);
        transitioner.setAnimator(3, null);
    }

    private void updateIcon(ViewHolder h, Drawable icon, boolean show, boolean anim) {
        if (icon != null) {
            h.iconView.setImageDrawable(icon);
            if (show && h.iconView.getVisibility() != 0) {
                if (anim) {
                    h.iconView.setAnimation(AnimationUtils.loadAnimation(this.mContext, 2130968589));
                }
                h.iconView.setVisibility(0);
            }
        }
    }

    private void updateThumbnail(ViewHolder h, Bitmap thumbnail, boolean show, boolean anim) {
        if (thumbnail != null) {
            h.thumbnailViewImage.setImageBitmap(thumbnail);
            if (!(h.thumbnailViewImageBitmap != null && h.thumbnailViewImageBitmap.getWidth() == thumbnail.getWidth() && h.thumbnailViewImageBitmap.getHeight() == thumbnail.getHeight())) {
                if (this.mFitThumbnailToXY) {
                    h.thumbnailViewImage.setScaleType(ScaleType.FIT_XY);
                } else {
                    Matrix scaleMatrix = new Matrix();
                    float scale = ((float) this.mThumbnailWidth) / ((float) thumbnail.getWidth());
                    scaleMatrix.setScale(scale, scale);
                    h.thumbnailViewImage.setScaleType(ScaleType.MATRIX);
                    h.thumbnailViewImage.setImageMatrix(scaleMatrix);
                }
            }
            if (show && h.thumbnailView.getVisibility() != 0) {
                if (anim) {
                    h.thumbnailView.setAnimation(AnimationUtils.loadAnimation(this.mContext, 2130968589));
                }
                h.thumbnailView.setVisibility(0);
            }
            h.thumbnailViewImageBitmap = thumbnail;
        }
    }

    void onTaskThumbnailLoaded(TaskDescription td) {
        synchronized (td) {
            if (this.mRecentsContainer != null) {
                ViewGroup container = this.mRecentsContainer;
                if (container instanceof RecentsScrollView) {
                    container = container.findViewById(2131492953);
                }
                for (int i = 0; i < container.getChildCount(); i++) {
                    View v = container.getChildAt(i);
                    if (v.getTag() instanceof ViewHolder) {
                        ViewHolder h = (ViewHolder) v.getTag();
                        if (!h.loadedThumbnailAndIcon && h.taskDescription == td) {
                            updateIcon(h, td.getIcon(), true, false);
                            updateThumbnail(h, td.getThumbnail(), true, false);
                            h.loadedThumbnailAndIcon = true;
                            this.mNumItemsWaitingForThumbnailsAndIcons--;
                        }
                    }
                }
            }
        }
        showIfReady();
    }

    public boolean onTouch(View v, MotionEvent ev) {
        if (!this.mShowing) {
            int action = ev.getAction() & 255;
            if (action == 0) {
                post(this.mPreloadTasksRunnable);
            } else if (action == 3) {
                setVisibility(8);
                clearRecentTasksList();
                removeCallbacks(this.mPreloadTasksRunnable);
            } else if (action == 1) {
                removeCallbacks(this.mPreloadTasksRunnable);
                if (!v.isPressed()) {
                    setVisibility(8);
                    clearRecentTasksList();
                }
            }
        }
        return false;
    }

    public void preloadRecentTasksList() {
        if (!this.mShowing) {
            this.mPreloadTasksRunnable.run();
        }
    }

    public void clearRecentTasksList() {
        if (!this.mShowing && this.mRecentTaskDescriptions != null) {
            this.mRecentTasksLoader.cancelLoadingThumbnailsAndIcons();
            this.mRecentTaskDescriptions.clear();
            this.mListAdapter.notifyDataSetInvalidated();
            this.mRecentTasksDirty = true;
        }
    }

    public void refreshRecentTasksList() {
        refreshRecentTasksList(null, false);
    }

    private void refreshRecentTasksList(ArrayList<TaskDescription> recentTasksList, boolean firstScreenful) {
        if (this.mRecentTasksDirty) {
            if (recentTasksList != null) {
                this.mFirstScreenful = true;
                onTasksLoaded(recentTasksList);
            } else {
                this.mFirstScreenful = true;
                this.mRecentTasksLoader.loadTasksInBackground();
            }
            this.mRecentTasksDirty = false;
        }
    }

    public void onTasksLoaded(ArrayList<TaskDescription> tasks) {
        if (this.mFirstScreenful || tasks.size() != 0) {
            int size = this.mFirstScreenful ? tasks.size() : this.mRecentTaskDescriptions == null ? 0 : this.mRecentTaskDescriptions.size();
            this.mNumItemsWaitingForThumbnailsAndIcons = size;
            if (this.mRecentTaskDescriptions == null) {
                this.mRecentTaskDescriptions = new ArrayList(tasks);
            } else {
                this.mRecentTaskDescriptions.addAll(tasks);
            }
            this.mListAdapter.notifyDataSetInvalidated();
            updateUiElements(getResources().getConfiguration());
            this.mReadyToShow = true;
            this.mFirstScreenful = false;
            showIfReady();
        }
    }

    public ArrayList<TaskDescription> getRecentTasksList() {
        return this.mRecentTaskDescriptions;
    }

    public boolean getFirstScreenful() {
        return this.mFirstScreenful;
    }

    private void updateUiElements(Configuration config) {
        String recentAppsAccessibilityDescription;
        this.mRecentsContainer.setVisibility(this.mRecentTaskDescriptions.size() > 0 ? 0 : 8);
        int numRecentApps = this.mRecentTaskDescriptions.size();
        if (numRecentApps == 0) {
            recentAppsAccessibilityDescription = getResources().getString(2131296265);
        } else {
            recentAppsAccessibilityDescription = getResources().getQuantityString(2131558400, numRecentApps, new Object[]{Integer.valueOf(numRecentApps)});
        }
        setContentDescription(recentAppsAccessibilityDescription);
    }

    public void handleOnClick(View view) {
        boolean usingDrawingCache;
        ViewHolder holder = (ViewHolder) view.getTag();
        TaskDescription ad = holder.taskDescription;
        Context context = view.getContext();
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        Bitmap bm = holder.thumbnailViewImageBitmap;
        if (bm.getWidth() == holder.thumbnailViewImage.getWidth() && bm.getHeight() == holder.thumbnailViewImage.getHeight()) {
            usingDrawingCache = false;
        } else {
            holder.thumbnailViewImage.setDrawingCacheEnabled(true);
            bm = holder.thumbnailViewImage.getDrawingCache();
            usingDrawingCache = true;
        }
        if (this.mPlaceholderThumbnail == null) {
            this.mPlaceholderThumbnail = (ImageView) findViewById(2131492951);
        }
        if (this.mTransitionBg == null) {
            this.mTransitionBg = findViewById(2131492949);
            try {
                if (!Stub.asInterface(ServiceManager.getService("window")).hasSystemNavBar()) {
                    LayoutParams lp = (LayoutParams) this.mTransitionBg.getLayoutParams();
                    lp.setMargins(0, getResources().getDimensionPixelSize(17104906), 0, 0);
                    this.mTransitionBg.setLayoutParams(lp);
                }
            } catch (RemoteException e) {
                Log.w("RecentsPanelView", "Failing checking whether status bar is visible", e);
            }
        }
        ImageView placeholderThumbnail = this.mPlaceholderThumbnail;
        this.mHideRecentsAfterThumbnailScaleUpStarted = false;
        placeholderThumbnail.setVisibility(0);
        if (usingDrawingCache) {
            placeholderThumbnail.setImageBitmap(bm.copy(bm.getConfig(), true));
        } else {
            placeholderThumbnail.setImageBitmap(bm);
        }
        Rect r = new Rect();
        holder.thumbnailViewImage.getGlobalVisibleRect(r);
        placeholderThumbnail.setTranslationX((float) r.left);
        placeholderThumbnail.setTranslationY((float) r.top);
        show(false, true);
        this.mThumbnailScaleUpStarted = false;
        ActivityOptions opts = ActivityOptions.makeDelayedThumbnailScaleUpAnimation(holder.thumbnailViewImage, bm, 0, 0, new OnAnimationStartedListener() {
            public void onAnimationStarted() {
                RecentsPanelView.this.mThumbnailScaleUpStarted = true;
                if (!RecentsPanelView.this.mHighEndGfx) {
                    RecentsPanelView.this.mPlaceholderThumbnail.setVisibility(4);
                }
                if (RecentsPanelView.this.mHideRecentsAfterThumbnailScaleUpStarted) {
                    RecentsPanelView.this.hideWindow();
                }
            }
        });
        if (ad.taskId >= 0) {
            am.moveTaskToFront(ad.taskId, 1, opts.toBundle());
        } else {
            Intent intent = ad.intent;
            intent.addFlags(269500416);
            context.startActivity(intent, opts.toBundle());
        }
        if (usingDrawingCache) {
            holder.thumbnailViewImage.setDrawingCacheEnabled(false);
        }
    }

    public void hideWindow() {
        if (this.mThumbnailScaleUpStarted) {
            setVisibility(8);
            this.mTransitionBg.setVisibility(4);
            this.mPlaceholderThumbnail.setVisibility(4);
            this.mHideRecentsAfterThumbnailScaleUpStarted = false;
            return;
        }
        this.mHideRecentsAfterThumbnailScaleUpStarted = true;
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        handleOnClick(view);
    }

    public void handleSwipe(View view) {
        TaskDescription ad = ((ViewHolder) view.getTag()).taskDescription;
        if (ad == null) {
            Log.v("RecentsPanelView", "Not able to find activity description for swiped task; view=" + view + " tag=" + view.getTag());
            return;
        }
        this.mRecentTaskDescriptions.remove(ad);
        if (this.mRecentTaskDescriptions.size() == 0) {
            hide(false);
        }
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        if (am != null) {
            am.removeTask(ad.persistentTaskId, 1);
            setContentDescription(this.mContext.getString(2131296364, new Object[]{ad.getLabel()}));
            sendAccessibilityEvent(4);
            setContentDescription(null);
        }
    }

    private void startApplicationDetailsActivity(String packageName) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", packageName, null));
        intent.setFlags(268435456);
        getContext().startActivity(intent);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mPopup != null ? true : super.onInterceptTouchEvent(ev);
    }

    public void handleLongPress(View view, View view2, View view3) {
        view3.setSelected(true);
        Context context = this.mContext;
        if (view2 == null) {
            view2 = view;
        }
        PopupMenu popupMenu = new PopupMenu(context, view2);
        this.mPopup = popupMenu;
        popupMenu.getMenuInflater().inflate(2131689473, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new AnonymousClass_3(view));
        popupMenu.setOnDismissListener(new AnonymousClass_4(view3));
        popupMenu.show();
    }
}
