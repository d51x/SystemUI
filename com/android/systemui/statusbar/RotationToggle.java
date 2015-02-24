package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import com.android.systemui.statusbar.policy.AutoRotateController;
import com.android.systemui.statusbar.policy.AutoRotateController.RotationLockCallbacks;

public class RotationToggle extends CompoundButton implements RotationLockCallbacks {
    private AutoRotateController mRotater;

    public RotationToggle(Context context) {
        super(context);
    }

    public RotationToggle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RotationToggle(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mRotater = new AutoRotateController(getContext(), this, this);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mRotater != null) {
            this.mRotater.release();
            this.mRotater = null;
        }
    }

    public void setRotationLockControlVisibility(boolean show) {
        setVisibility(show ? 0 : 8);
    }
}
