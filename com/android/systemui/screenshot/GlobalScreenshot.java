package com.android.systemui.screenshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.MediaActionSound;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Interpolator;
import android.widget.ImageView;

class GlobalScreenshot {
    private ImageView mBackgroundView;
    private float mBgPadding;
    private float mBgPaddingScale;
    private MediaActionSound mCameraSound;
    private Context mContext;
    private Display mDisplay;
    private Matrix mDisplayMatrix;
    private DisplayMetrics mDisplayMetrics;
    private int mNotificationIconSize;
    private NotificationManager mNotificationManager;
    private Bitmap mScreenBitmap;
    private AnimatorSet mScreenshotAnimation;
    private ImageView mScreenshotFlash;
    private View mScreenshotLayout;
    private ImageView mScreenshotView;
    private LayoutParams mWindowLayoutParams;
    private WindowManager mWindowManager;

    class AnonymousClass_11 implements AnimatorUpdateListener {
        final /* synthetic */ PointF val$finalPos;
        final /* synthetic */ Interpolator val$scaleInterpolator;

        AnonymousClass_11(Interpolator interpolator, PointF pointF) {
            this.val$scaleInterpolator = interpolator;
            this.val$finalPos = pointF;
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            float t = ((Float) animation.getAnimatedValue()).floatValue();
            float scaleT = (0.725f + GlobalScreenshot.this.mBgPaddingScale) - (this.val$scaleInterpolator.getInterpolation(t) * 0.27500004f);
            GlobalScreenshot.this.mBackgroundView.setAlpha((1.0f - t) * 0.5f);
            GlobalScreenshot.this.mScreenshotView.setAlpha(1.0f - this.val$scaleInterpolator.getInterpolation(t));
            GlobalScreenshot.this.mScreenshotView.setScaleX(scaleT);
            GlobalScreenshot.this.mScreenshotView.setScaleY(scaleT);
            GlobalScreenshot.this.mScreenshotView.setTranslationX(this.val$finalPos.x * t);
            GlobalScreenshot.this.mScreenshotView.setTranslationY(this.val$finalPos.y * t);
        }
    }

    class AnonymousClass_2 extends AnimatorListenerAdapter {
        final /* synthetic */ Runnable val$finisher;

        AnonymousClass_2(Runnable runnable) {
            this.val$finisher = runnable;
        }

        public void onAnimationEnd(Animator animation) {
            GlobalScreenshot.this.saveScreenshotInWorkerThread(this.val$finisher);
            GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
        }
    }

    class AnonymousClass_7 implements AnimatorUpdateListener {
        final /* synthetic */ Interpolator val$flashAlphaInterpolator;
        final /* synthetic */ Interpolator val$scaleInterpolator;

        AnonymousClass_7(Interpolator interpolator, Interpolator interpolator2) {
            this.val$scaleInterpolator = interpolator;
            this.val$flashAlphaInterpolator = interpolator2;
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            float t = ((Float) animation.getAnimatedValue()).floatValue();
            float scaleT = (1.0f + GlobalScreenshot.this.mBgPaddingScale) - (this.val$scaleInterpolator.getInterpolation(t) * 0.27499998f);
            GlobalScreenshot.this.mBackgroundView.setAlpha(this.val$scaleInterpolator.getInterpolation(t) * 0.5f);
            GlobalScreenshot.this.mScreenshotView.setAlpha(t);
            GlobalScreenshot.this.mScreenshotView.setScaleX(scaleT);
            GlobalScreenshot.this.mScreenshotView.setScaleY(scaleT);
            GlobalScreenshot.this.mScreenshotFlash.setAlpha(this.val$flashAlphaInterpolator.getInterpolation(t));
        }
    }

    public GlobalScreenshot(Context context) {
        Resources r = context.getResources();
        this.mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mDisplayMatrix = new Matrix();
        this.mScreenshotLayout = layoutInflater.inflate(2130903042, null);
        this.mBackgroundView = (ImageView) this.mScreenshotLayout.findViewById(2131492875);
        this.mScreenshotView = (ImageView) this.mScreenshotLayout.findViewById(2131492876);
        this.mScreenshotFlash = (ImageView) this.mScreenshotLayout.findViewById(2131492877);
        this.mScreenshotLayout.setFocusable(true);
        this.mScreenshotLayout.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        this.mWindowLayoutParams = new LayoutParams(-1, -1, 0, 0, 2015, 17302784, -3);
        this.mWindowLayoutParams.setTitle("ScreenshotAnimation");
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplayMetrics = new DisplayMetrics();
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        this.mNotificationIconSize = r.getDimensionPixelSize(17104902);
        this.mBgPadding = (float) r.getDimensionPixelSize(2131427361);
        this.mBgPaddingScale = this.mBgPadding / ((float) this.mDisplayMetrics.widthPixels);
        this.mCameraSound = new MediaActionSound();
        this.mCameraSound.load(0);
    }

    private void saveScreenshotInWorkerThread(Runnable finisher) {
        SaveImageInBackgroundData data = new SaveImageInBackgroundData();
        data.context = this.mContext;
        data.image = this.mScreenBitmap;
        data.iconSize = this.mNotificationIconSize;
        data.finisher = finisher;
        new SaveImageInBackgroundTask(this.mContext, data, this.mNotificationManager, 789).execute(new SaveImageInBackgroundData[]{data});
    }

    private float getDegreesForRotation(int value) {
        switch (value) {
            case 1:
                return 270.0f;
            case 2:
                return 180.0f;
            case 3:
                return 90.0f;
            default:
                return 0.0f;
        }
    }

    void takeScreenshot(Runnable finisher, boolean statusBarVisible, boolean navBarVisible) {
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        float[] dims = new float[]{(float) this.mDisplayMetrics.widthPixels, (float) this.mDisplayMetrics.heightPixels};
        float degrees = getDegreesForRotation(this.mDisplay.getRotation());
        boolean requiresRotation = degrees > 0.0f;
        if (requiresRotation) {
            this.mDisplayMatrix.reset();
            this.mDisplayMatrix.preRotate(-degrees);
            this.mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }
        this.mScreenBitmap = Surface.screenshot((int) dims[0], (int) dims[1]);
        if (this.mScreenBitmap == null) {
            notifyScreenshotError(this.mContext, this.mNotificationManager);
            finisher.run();
            return;
        }
        if (requiresRotation) {
            Bitmap ss = Bitmap.createBitmap(this.mDisplayMetrics.widthPixels, this.mDisplayMetrics.heightPixels, Config.ARGB_8888);
            Canvas c = new Canvas(ss);
            c.translate((float) (ss.getWidth() / 2), (float) (ss.getHeight() / 2));
            c.rotate(degrees);
            c.translate((-dims[0]) / 2.0f, (-dims[1]) / 2.0f);
            c.drawBitmap(this.mScreenBitmap, 0.0f, 0.0f, null);
            c.setBitmap(null);
            this.mScreenBitmap = ss;
        }
        this.mScreenBitmap.setHasAlpha(false);
        this.mScreenBitmap.prepareToDraw();
        startAnimation(finisher, this.mDisplayMetrics.widthPixels, this.mDisplayMetrics.heightPixels, statusBarVisible, navBarVisible);
    }

    private void startAnimation(Runnable finisher, int w, int h, boolean statusBarVisible, boolean navBarVisible) {
        this.mScreenshotView.setImageBitmap(this.mScreenBitmap);
        this.mScreenshotLayout.requestFocus();
        if (this.mScreenshotAnimation != null) {
            this.mScreenshotAnimation.end();
        }
        this.mWindowManager.addView(this.mScreenshotLayout, this.mWindowLayoutParams);
        ValueAnimator screenshotDropInAnim = createScreenshotDropInAnimation();
        ValueAnimator screenshotFadeOutAnim = createScreenshotDropOutAnimation(w, h, statusBarVisible, navBarVisible);
        this.mScreenshotAnimation = new AnimatorSet();
        this.mScreenshotAnimation.playSequentially(new Animator[]{screenshotDropInAnim, screenshotFadeOutAnim});
        this.mScreenshotAnimation.addListener(new AnonymousClass_2(finisher));
        this.mScreenshotLayout.post(new Runnable() {
            public void run() {
                GlobalScreenshot.this.mCameraSound.play(0);
                GlobalScreenshot.this.mScreenshotView.setLayerType(2, null);
                GlobalScreenshot.this.mScreenshotView.buildLayer();
                GlobalScreenshot.this.mScreenshotAnimation.start();
            }
        });
    }

    private ValueAnimator createScreenshotDropInAnimation() {
        Interpolator flashAlphaInterpolator = new Interpolator() {
            public float getInterpolation(float x) {
                return x <= 0.60465115f ? (float) Math.sin(3.141592653589793d * ((double) (x / 0.60465115f))) : 0.0f;
            }
        };
        Interpolator scaleInterpolator = new Interpolator() {
            public float getInterpolation(float x) {
                return x < 0.30232558f ? 0.0f : (x - 0.60465115f) / 0.39534885f;
            }
        };
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        anim.setDuration(430);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                GlobalScreenshot.this.mBackgroundView.setAlpha(0.0f);
                GlobalScreenshot.this.mBackgroundView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotView.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationX(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationY(0.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleX(GlobalScreenshot.this.mBgPaddingScale + 1.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleY(GlobalScreenshot.this.mBgPaddingScale + 1.0f);
                GlobalScreenshot.this.mScreenshotView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotFlash.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(0);
            }

            public void onAnimationEnd(Animator animation) {
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(8);
            }
        });
        anim.addUpdateListener(new AnonymousClass_7(scaleInterpolator, flashAlphaInterpolator));
        return anim;
    }

    private ValueAnimator createScreenshotDropOutAnimation(int w, int h, boolean statusBarVisible, boolean navBarVisible) {
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        anim.setStartDelay(500);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                GlobalScreenshot.this.mBackgroundView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setLayerType(0, null);
            }
        });
        if (statusBarVisible && navBarVisible) {
            Interpolator scaleInterpolator = new Interpolator() {
                public float getInterpolation(float x) {
                    return x < 0.8604651f ? (float) (1.0d - Math.pow((double) (1.0f - (x / 0.8604651f)), 2.0d)) : 1.0f;
                }
            };
            float halfScreenWidth = (((float) w) - (this.mBgPadding * 2.0f)) / 2.0f;
            float halfScreenHeight = (((float) h) - (this.mBgPadding * 2.0f)) / 2.0f;
            PointF finalPos = new PointF((-halfScreenWidth) + (0.45f * halfScreenWidth), (-halfScreenHeight) + (0.45f * halfScreenHeight));
            anim.setDuration(430);
            anim.addUpdateListener(new AnonymousClass_11(scaleInterpolator, finalPos));
        } else {
            anim.setDuration(320);
            anim.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = ((Float) animation.getAnimatedValue()).floatValue();
                    float scaleT = (0.725f + GlobalScreenshot.this.mBgPaddingScale) - (0.125f * t);
                    GlobalScreenshot.this.mBackgroundView.setAlpha((1.0f - t) * 0.5f);
                    GlobalScreenshot.this.mScreenshotView.setAlpha(1.0f - t);
                    GlobalScreenshot.this.mScreenshotView.setScaleX(scaleT);
                    GlobalScreenshot.this.mScreenshotView.setScaleY(scaleT);
                }
            });
        }
        return anim;
    }

    static void notifyScreenshotError(Context context, NotificationManager nManager) {
        Resources r = context.getResources();
        nManager.notify(789, new Builder(context).setTicker(r.getString(2131296305)).setContentTitle(r.getString(2131296305)).setContentText(r.getString(2131296306)).setSmallIcon(2130837602).setWhen(System.currentTimeMillis()).setAutoCancel(true).getNotification());
    }
}
