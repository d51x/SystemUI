package com.android.systemui.statusbar;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.ViewDebug.ExportedProperty;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView.ScaleType;
import com.android.internal.statusbar.StatusBarIcon;
import java.text.NumberFormat;

public class StatusBarIconView extends AnimatedImageView {
    private StatusBarIcon mIcon;
    private Notification mNotification;
    private Drawable mNumberBackground;
    private Paint mNumberPain;
    private String mNumberText;
    private int mNumberX;
    private int mNumberY;
    @ExportedProperty
    private String mSlot;

    public StatusBarIconView(Context context, String slot, Notification notification) {
        super(context);
        Resources res = context.getResources();
        this.mSlot = slot;
        this.mNumberPain = new Paint();
        this.mNumberPain.setTextAlign(Align.CENTER);
        this.mNumberPain.setColor(res.getColor(2130837713));
        this.mNumberPain.setAntiAlias(true);
        this.mNotification = notification;
        setContentDescription(notification);
        if (notification != null) {
            float scale = ((float) res.getDimensionPixelSize(2131427347)) / ((float) res.getDimensionPixelSize(2131427342));
            setScaleX(scale);
            setScaleY(scale);
            setAlpha(res.getFraction(2131427348, 1, 1));
        }
        setScaleType(ScaleType.CENTER);
    }

    public StatusBarIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        float scale = ((float) res.getDimensionPixelSize(2131427347)) / ((float) res.getDimensionPixelSize(2131427342));
        setScaleX(scale);
        setScaleY(scale);
        setAlpha(res.getFraction(2131427348, 1, 1));
    }

    private static boolean streq(String a, String b) {
        if (a == b) {
            return true;
        }
        if (a != null || b == null) {
            return (a == null || b != null) ? a.equals(b) : false;
        } else {
            return false;
        }
    }

    public boolean set(StatusBarIcon icon) {
        boolean iconEquals;
        boolean levelEquals;
        boolean visibilityEquals;
        boolean numberEquals;
        int i = 0;
        if (this.mIcon != null && streq(this.mIcon.iconPackage, icon.iconPackage) && this.mIcon.iconId == icon.iconId) {
            iconEquals = true;
        } else {
            iconEquals = false;
        }
        if (iconEquals && this.mIcon.iconLevel == icon.iconLevel) {
            levelEquals = true;
        } else {
            levelEquals = false;
        }
        if (this.mIcon == null || this.mIcon.visible != icon.visible) {
            visibilityEquals = false;
        } else {
            visibilityEquals = true;
        }
        if (this.mIcon == null || this.mIcon.number != icon.number) {
            numberEquals = false;
        } else {
            numberEquals = true;
        }
        this.mIcon = icon.clone();
        setContentDescription(icon.contentDescription);
        if (!iconEquals) {
            Drawable drawable = getIcon(icon);
            if (drawable == null) {
                Slog.w("StatusBarIconView", "No icon for slot " + this.mSlot);
                return false;
            }
            setImageDrawable(drawable);
        }
        if (!levelEquals) {
            setImageLevel(icon.iconLevel);
        }
        if (!numberEquals) {
            if (icon.number <= 0 || !this.mContext.getResources().getBoolean(2131230723)) {
                this.mNumberBackground = null;
                this.mNumberText = null;
            } else {
                if (this.mNumberBackground == null) {
                    this.mNumberBackground = getContext().getResources().getDrawable(2130837530);
                }
                placeNumber();
            }
            invalidate();
        }
        if (!visibilityEquals) {
            if (!icon.visible) {
                i = 8;
            }
            setVisibility(i);
        }
        return true;
    }

    private Drawable getIcon(StatusBarIcon icon) {
        return getIcon(getContext(), icon);
    }

    public static Drawable getIcon(Context context, StatusBarIcon icon) {
        if (icon.iconPackage != null) {
            try {
                Resources r = context.getPackageManager().getResourcesForApplication(icon.iconPackage);
            } catch (NameNotFoundException e) {
                Slog.e("StatusBarIconView", "Icon package not found: " + icon.iconPackage);
                return null;
            }
        }
        r = context.getResources();
        if (icon.iconId == 0) {
            return null;
        }
        try {
            return r.getDrawable(icon.iconId);
        } catch (RuntimeException e2) {
            Slog.w("StatusBarIconView", "Icon not found in " + (icon.iconPackage != null ? Integer.valueOf(icon.iconId) : "<system>") + ": " + Integer.toHexString(icon.iconId));
            return null;
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (this.mNotification != null) {
            event.setParcelableData(this.mNotification);
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mNumberBackground != null) {
            placeNumber();
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mNumberBackground != null) {
            this.mNumberBackground.draw(canvas);
            canvas.drawText(this.mNumberText, (float) this.mNumberX, (float) this.mNumberY, this.mNumberPain);
        }
    }

    protected void debug(int depth) {
        super.debug(depth);
        Log.d("View", debugIndent(depth) + "slot=" + this.mSlot);
        Log.d("View", debugIndent(depth) + "icon=" + this.mIcon);
    }

    void placeNumber() {
        String str;
        if (this.mIcon.number > this.mContext.getResources().getInteger(17694723)) {
            str = this.mContext.getResources().getString(17039383);
        } else {
            str = NumberFormat.getIntegerInstance().format((long) this.mIcon.number);
        }
        this.mNumberText = str;
        int w = getWidth();
        int h = getHeight();
        Rect r = new Rect();
        this.mNumberPain.getTextBounds(str, 0, str.length(), r);
        int tw = r.right - r.left;
        int th = r.bottom - r.top;
        this.mNumberBackground.getPadding(r);
        int dw = (r.left + tw) + r.right;
        if (dw < this.mNumberBackground.getMinimumWidth()) {
            dw = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberX = (w - r.right) - (((dw - r.right) - r.left) / 2);
        int dh = (r.top + th) + r.bottom;
        if (dh < this.mNumberBackground.getMinimumWidth()) {
            dh = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberY = (h - r.bottom) - ((((dh - r.top) - th) - r.bottom) / 2);
        this.mNumberBackground.setBounds(w - dw, h - dh, w, h);
    }

    private void setContentDescription(Notification notification) {
        if (notification != null) {
            CharSequence tickerText = notification.tickerText;
            if (!TextUtils.isEmpty(tickerText)) {
                setContentDescription(tickerText);
            }
        }
    }

    public String toString() {
        return "StatusBarIconView(slot=" + this.mSlot + " icon=" + this.mIcon + " notification=" + this.mNotification + ")";
    }
}
