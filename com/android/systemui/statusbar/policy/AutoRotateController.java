package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.android.internal.view.RotationPolicy;
import com.android.internal.view.RotationPolicy.RotationPolicyListener;

public final class AutoRotateController implements OnCheckedChangeListener {
    private boolean mAutoRotation;
    private final RotationLockCallbacks mCallbacks;
    private final CompoundButton mCheckbox;
    private final Context mContext;
    private final RotationPolicyListener mRotationPolicyListener;

    public static interface RotationLockCallbacks {
        void setRotationLockControlVisibility(boolean z);
    }

    public AutoRotateController(Context context, CompoundButton checkbox, RotationLockCallbacks callbacks) {
        this.mRotationPolicyListener = new RotationPolicyListener() {
            public void onChange() {
                AutoRotateController.this.updateState();
            }
        };
        this.mContext = context;
        this.mCheckbox = checkbox;
        this.mCallbacks = callbacks;
        this.mCheckbox.setOnCheckedChangeListener(this);
        RotationPolicy.registerRotationPolicyListener(context, this.mRotationPolicyListener);
        updateState();
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        if (checked != this.mAutoRotation) {
            this.mAutoRotation = checked;
            RotationPolicy.setRotationLock(this.mContext, !checked);
        }
    }

    public void release() {
        RotationPolicy.unregisterRotationPolicyListener(this.mContext, this.mRotationPolicyListener);
    }

    private void updateState() {
        this.mAutoRotation = !RotationPolicy.isRotationLocked(this.mContext);
        this.mCheckbox.setChecked(this.mAutoRotation);
        boolean visible = RotationPolicy.isRotationLockToggleVisible(this.mContext);
        this.mCallbacks.setRotationLockControlVisibility(visible);
        this.mCheckbox.setEnabled(visible);
    }
}
