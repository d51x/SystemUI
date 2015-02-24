package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.android.systemui.R;

public class ToggleSlider extends RelativeLayout implements OnCheckedChangeListener, OnSeekBarChangeListener {
    private TextView mLabel;
    private Listener mListener;
    private SeekBar mSlider;
    private CompoundButton mToggle;
    private boolean mTracking;

    public static interface Listener {
        void onChanged(ToggleSlider toggleSlider, boolean z, boolean z2, int i);
    }

    public ToggleSlider(Context context) {
        this(context, null);
    }

    public ToggleSlider(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToggleSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View.inflate(context, 2130903056, this);
        Resources res = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ToggleSlider, defStyle, 0);
        this.mToggle = (CompoundButton) findViewById(2131492958);
        this.mToggle.setOnCheckedChangeListener(this);
        this.mToggle.setBackgroundDrawable(res.getDrawable(2130837701));
        this.mSlider = (SeekBar) findViewById(2131492959);
        this.mSlider.setOnSeekBarChangeListener(this);
        this.mLabel = (TextView) findViewById(2131492960);
        this.mLabel.setText(a.getString(0));
        a.recycle();
    }

    public void onCheckedChanged(CompoundButton toggle, boolean checked) {
        Drawable thumb;
        Drawable slider;
        Resources res = getContext().getResources();
        if (checked) {
            thumb = res.getDrawable(17302724);
            slider = res.getDrawable(2130837699);
        } else {
            thumb = res.getDrawable(17302728);
            slider = res.getDrawable(17302730);
        }
        this.mSlider.setThumb(thumb);
        this.mSlider.setProgressDrawable(slider);
        if (this.mListener != null) {
            this.mListener.onChanged(this, this.mTracking, checked, this.mSlider.getProgress());
        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (this.mListener != null) {
            this.mListener.onChanged(this, this.mTracking, this.mToggle.isChecked(), progress);
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        this.mTracking = true;
        if (this.mListener != null) {
            this.mListener.onChanged(this, this.mTracking, this.mToggle.isChecked(), this.mSlider.getProgress());
        }
        this.mToggle.setChecked(false);
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        this.mTracking = false;
        if (this.mListener != null) {
            this.mListener.onChanged(this, this.mTracking, this.mToggle.isChecked(), this.mSlider.getProgress());
        }
    }

    public void setOnChangedListener(Listener l) {
        this.mListener = l;
    }

    public void setChecked(boolean checked) {
        this.mToggle.setChecked(checked);
    }

    public void setMax(int max) {
        this.mSlider.setMax(max);
    }

    public void setValue(int value) {
        this.mSlider.setProgress(value);
    }
}
