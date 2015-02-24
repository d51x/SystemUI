package com.android.systemui.statusbar.tablet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;
import java.util.ArrayList;
import java.util.Iterator;

public class NotificationPanelTitle extends RelativeLayout implements OnClickListener {
    private ArrayList<View> buttons;
    private NotificationPanel mPanel;
    private View mSettingsButton;

    public NotificationPanelTitle(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.buttons = new ArrayList();
        setOnClickListener(this);
    }

    public void setPanel(NotificationPanel p) {
        this.mPanel = p;
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        ArrayList arrayList = this.buttons;
        View findViewById = findViewById(2131492933);
        this.mSettingsButton = findViewById;
        arrayList.add(findViewById);
        this.buttons.add(findViewById(2131493005));
    }

    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        Iterator i$ = this.buttons.iterator();
        while (i$.hasNext()) {
            View button = (View) i$.next();
            if (button != null) {
                button.setPressed(pressed);
            }
        }
    }

    public boolean onTouchEvent(MotionEvent e) {
        boolean z = false;
        if (!this.mSettingsButton.isEnabled()) {
            return false;
        }
        switch (e.getAction()) {
            case 0:
                setPressed(true);
                break;
            case 1:
                if (isPressed()) {
                    playSoundEffect(0);
                    this.mPanel.swapPanels();
                    setPressed(false);
                }
                break;
            case 2:
                int x = (int) e.getX();
                int y = (int) e.getY();
                if (x > 0 && x < getWidth() && y > 0 && y < getHeight()) {
                    z = true;
                }
                setPressed(z);
                break;
            case 3:
                setPressed(false);
                break;
        }
        return true;
    }

    public void onClick(View v) {
        if (this.mSettingsButton.isEnabled() && v == this) {
            this.mPanel.swapPanels();
        }
    }

    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        if (!super.onRequestSendAccessibilityEvent(child, event)) {
            return false;
        }
        AccessibilityEvent record = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(record);
        dispatchPopulateAccessibilityEvent(record);
        event.appendRecord(record);
        return true;
    }
}
