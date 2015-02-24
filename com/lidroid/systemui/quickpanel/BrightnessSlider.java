package com.lidroid.systemui.quickpanel;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.ServiceManager;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class BrightnessSlider extends LinearLayout implements OnClickListener, OnSeekBarChangeListener {
    private Context mContext;
    private Handler mHandler;
    private View mMax;
    private View mMin;
    private BrightnessSettingsObserver mObserver;
    private SeekBar mSlider;

    private class BrightnessSettingsObserver extends ContentObserver {
        public BrightnessSettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = BrightnessSlider.this.mContext.getContentResolver();
            resolver.registerContentObserver(System.getUriFor("screen_brightness"), false, this);
            resolver.registerContentObserver(System.getUriFor("screen_brightness_mode"), false, this);
        }

        public void unobserve() {
            BrightnessSlider.this.mContext.getContentResolver().unregisterContentObserver(this);
        }

        public void onChange(boolean selfChange) {
            BrightnessSlider.this.updateState();
        }
    }

    public BrightnessSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mObserver = null;
        this.mContext = context;
        this.mHandler = new Handler();
        this.mObserver = new BrightnessSettingsObserver(this.mHandler);
    }

    public void updateState() {
        this.mSlider.setProgress(getCurrBrightness());
        if (System.getInt(getContext().getContentResolver(), "screen_brightness_mode", 0) == 0) {
            setVisibility(VISIBLE);
        } else {
            setVisibility(GONE);
        }
    }

    private int getCurrBrightness() {
        return System.getInt(this.mContext.getContentResolver(), "screen_brightness", 0);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setBrightness(progress);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        setDB(this.mSlider.getProgress());
    }

    public void setDB(int br) {
        try {
            System.putInt(this.mContext.getContentResolver(), "screen_brightness", br);
        } catch (Exception exc) {
            Log.e("BrightnessSlider", exc.getLocalizedMessage());
        }
    }

    private void setBrightness(int brightness) {
        try {
            IPowerManager power = Stub.asInterface(ServiceManager.getService("power"));
            if (power != null) {
                power.setBacklightBrightness(brightness);
            }
        } catch (Exception exc) {
            Log.e("BrightnessSlider", exc.getLocalizedMessage());
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSlider = (SeekBar) findViewWithTag("slider");
        this.mSlider.setOnSeekBarChangeListener(this);
        this.mSlider.setMax(255);
        this.mMin = findViewWithTag("br_min");
        this.mMin.setOnClickListener(this);
        this.mMax = findViewWithTag("br_max");
        this.mMax.setOnClickListener(this);
        updateState();
    }

    protected void onAttachedToWindow() {
        if (this.mObserver != null) {
            this.mObserver.observe();
        }
    }

    protected void onDetachedFromWindow() {
        if (this.mObserver != null) {
            this.mObserver.unobserve();
        }
    }

    public void onClick(View v) {
        if (v == this.mMin) {
            setDB(0);
        }
        if (v == this.mMax) {
            setDB(255);
        }
    }
}
