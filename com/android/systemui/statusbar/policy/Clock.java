package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class Clock extends TextView {
    private boolean mAttached;
    private Calendar mCalendar;
    private SimpleDateFormat mClockFormat;
    private String mClockFormatString;
    private final BroadcastReceiver mIntentReceiver;

    public Clock(Context context) {
        this(context, null);
    }

    public Clock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Clock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")) {
                    Clock.this.mCalendar = Calendar.getInstance(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
                    if (Clock.this.mClockFormat != null) {
                        Clock.this.mClockFormat.setTimeZone(Clock.this.mCalendar.getTimeZone());
                    }
                }
                Clock.this.updateClock();
            }
        };
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mAttached) {
            this.mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_TICK");
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            getContext().registerReceiver(this.mIntentReceiver, filter, null, getHandler());
        }
        this.mCalendar = Calendar.getInstance(TimeZone.getDefault());
        updateClock();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            getContext().unregisterReceiver(this.mIntentReceiver);
            this.mAttached = false;
        }
    }

    final void updateClock() {
        this.mCalendar.setTimeInMillis(System.currentTimeMillis());
        setText(getSmallTime());
    }

    private final CharSequence getSmallTime() {
        int res;
        SimpleDateFormat sdf;
        Context context = getContext();
        if (DateFormat.is24HourFormat(context)) {
            res = 17039490;
        } else {
            res = 17039489;
        }
        String format = context.getString(res);
        if (format.equals(this.mClockFormatString)) {
            sdf = this.mClockFormat;
        } else {
            int a = -1;
            boolean quoted = false;
            for (int i = 0; i < format.length(); i++) {
                char c = format.charAt(i);
                if (c == '\'') {
                    quoted = !quoted;
                }
                if (!quoted && c == 'a') {
                    a = i;
                    break;
                }
            }
            if (a >= 0) {
                int b = a;
                while (a > 0 && Character.isWhitespace(format.charAt(a - 1))) {
                    a--;
                }
                format = format.substring(0, a) + '\uef00' + format.substring(a, b) + "a" + '\uef01' + format.substring(b + 1);
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
            this.mClockFormat = simpleDateFormat;
            this.mClockFormatString = format;
        }
        String result = sdf.format(this.mCalendar.getTime());
        int magic1 = result.indexOf(61184);
        int magic2 = result.indexOf(61185);
        if (magic1 < 0 || magic2 <= magic1) {
            return result;
        }
        SpannableStringBuilder formatted = new SpannableStringBuilder(result);
        formatted.delete(magic1, magic2 + 1);
        return formatted;
    }
}
