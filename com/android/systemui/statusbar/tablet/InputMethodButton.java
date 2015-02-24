package com.android.systemui.statusbar.tablet;

import android.content.Context;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.ImageView;
import java.util.List;

public class InputMethodButton extends ImageView {
    private boolean mHardKeyboardAvailable;
    private ImageView mIcon;
    private final int mId;
    private final InputMethodManager mImm;
    private boolean mScreenLocked;
    private boolean mShowButton;
    private IBinder mToken;

    public InputMethodButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowButton = false;
        this.mScreenLocked = false;
        this.mId = getId();
        this.mImm = (InputMethodManager) context.getSystemService("input_method");
    }

    protected void onAttachedToWindow() {
        this.mIcon = (ImageView) findViewById(this.mId);
        refreshStatusIcon();
    }

    private boolean needsToShowIMEButtonWhenVisibilityAuto() {
        List<InputMethodInfo> imis = this.mImm.getEnabledInputMethodList();
        int N = imis.size();
        if (N > 2) {
            return true;
        }
        if (N < 1) {
            return false;
        }
        int nonAuxCount = 0;
        int auxCount = 0;
        InputMethodSubtype nonAuxSubtype = null;
        InputMethodSubtype auxSubtype = null;
        for (int i = 0; i < N; i++) {
            List<InputMethodSubtype> subtypes = this.mImm.getEnabledInputMethodSubtypeList((InputMethodInfo) imis.get(i), true);
            int subtypeCount = subtypes.size();
            if (subtypeCount == 0) {
                nonAuxCount++;
            } else {
                for (int j = 0; j < subtypeCount; j++) {
                    InputMethodSubtype subtype = (InputMethodSubtype) subtypes.get(j);
                    if (subtype.isAuxiliary()) {
                        auxCount++;
                        auxSubtype = subtype;
                    } else {
                        nonAuxCount++;
                        nonAuxSubtype = subtype;
                    }
                }
            }
        }
        if (nonAuxCount > 1 || auxCount > 1) {
            return true;
        }
        if (nonAuxCount != 1 || auxCount != 1) {
            return false;
        }
        if (!(nonAuxSubtype == null || auxSubtype == null)) {
            if ((nonAuxSubtype.getLocale().equals(auxSubtype.getLocale()) || auxSubtype.overridesImplicitlyEnabledSubtype() || nonAuxSubtype.overridesImplicitlyEnabledSubtype()) && nonAuxSubtype.containsExtraValueKey("TrySuppressingImeSwitcher")) {
                return false;
            }
        }
        return true;
    }

    private boolean needsToShowIMEButton() {
        if (!this.mShowButton || this.mScreenLocked) {
            return false;
        }
        if (this.mHardKeyboardAvailable) {
            return true;
        }
        switch (loadInputMethodSelectorVisibility()) {
            case 0:
                return needsToShowIMEButtonWhenVisibilityAuto();
            case 1:
                return true;
            case 2:
                return false;
            default:
                return false;
        }
    }

    private void refreshStatusIcon() {
        if (this.mIcon != null) {
            if (needsToShowIMEButton()) {
                setVisibility(0);
                this.mIcon.setImageResource(2130837551);
                return;
            }
            setVisibility(8);
        }
    }

    private int loadInputMethodSelectorVisibility() {
        return Secure.getInt(getContext().getContentResolver(), "input_method_selector_visibility", 0);
    }

    public void setIconImage(int resId) {
        if (this.mIcon != null) {
            this.mIcon.setImageResource(resId);
        }
    }

    public void setImeWindowStatus(IBinder token, boolean showButton) {
        this.mToken = token;
        this.mShowButton = showButton;
        refreshStatusIcon();
    }

    public void setHardKeyboardStatus(boolean available) {
        if (this.mHardKeyboardAvailable != available) {
            this.mHardKeyboardAvailable = available;
            refreshStatusIcon();
        }
    }

    public void setScreenLocked(boolean locked) {
        this.mScreenLocked = locked;
        refreshStatusIcon();
    }
}
