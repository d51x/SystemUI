package com.android.systemui.statusbar.tablet;

import android.app.ActivityManager;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.RadioButton;

public class CompatModePanel extends FrameLayout implements OnClickListener, StatusBarPanel {
    private ActivityManager mAM;
    private boolean mAttached;
    private Context mContext;
    private RadioButton mOffButton;
    private RadioButton mOnButton;
    private View mTrigger;

    public CompatModePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAttached = false;
        this.mContext = context;
        this.mAM = (ActivityManager) context.getSystemService("activity");
    }

    public void onFinishInflate() {
        this.mOnButton = (RadioButton) findViewById(2131492974);
        this.mOffButton = (RadioButton) findViewById(2131492973);
        this.mOnButton.setOnClickListener(this);
        this.mOffButton.setOnClickListener(this);
        refresh();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            this.mAttached = false;
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mAttached) {
            this.mAttached = true;
        }
    }

    public void onClick(View v) {
        if (v == this.mOnButton) {
            this.mAM.setFrontActivityScreenCompatMode(1);
        } else if (v == this.mOffButton) {
            this.mAM.setFrontActivityScreenCompatMode(0);
        }
    }

    public boolean isInContentArea(int x, int y) {
        return false;
    }

    public boolean dispatchHoverEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        return (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) ? true : super.dispatchHoverEvent(event);
    }

    public void setTrigger(View v) {
        this.mTrigger = v;
    }

    public void openPanel() {
        setVisibility(0);
        if (this.mTrigger != null) {
            this.mTrigger.setSelected(true);
        }
        refresh();
    }

    public void closePanel() {
        setVisibility(8);
        if (this.mTrigger != null) {
            this.mTrigger.setSelected(false);
        }
    }

    private void refresh() {
        boolean z = true;
        int mode = this.mAM.getFrontActivityScreenCompatMode();
        if (mode == -1 || mode == -2) {
            closePanel();
            return;
        }
        boolean on;
        if (mode == 1) {
            on = true;
        } else {
            on = false;
        }
        this.mOnButton.setChecked(on);
        RadioButton radioButton = this.mOffButton;
        if (on) {
            z = false;
        }
        radioButton.setChecked(z);
    }
}
