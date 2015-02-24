package com.android.systemui.statusbar.policy;

class WimaxIcons {
    static final int WIMAX_DISCONNECTED;
    static final int WIMAX_IDLE;
    static final int[][] WIMAX_SIGNAL_STRENGTH;

    WimaxIcons() {
    }

    static {
        WIMAX_SIGNAL_STRENGTH = TelephonyIcons.DATA_SIGNAL_STRENGTH;
        WIMAX_DISCONNECTED = WIMAX_SIGNAL_STRENGTH[0][0];
        WIMAX_IDLE = WIMAX_DISCONNECTED;
    }
}
