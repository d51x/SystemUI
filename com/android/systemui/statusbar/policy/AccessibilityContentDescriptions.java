package com.android.systemui.statusbar.policy;

public class AccessibilityContentDescriptions {
    static final int[] DATA_CONNECTION_STRENGTH;
    static final int[] PHONE_SIGNAL_STRENGTH;
    static final int[] WIFI_CONNECTION_STRENGTH;
    static final int[] WIMAX_CONNECTION_STRENGTH;

    private AccessibilityContentDescriptions() {
    }

    static {
        PHONE_SIGNAL_STRENGTH = new int[]{2131296325, 2131296326, 2131296327, 2131296328, 2131296329};
        DATA_CONNECTION_STRENGTH = new int[]{2131296330, 2131296331, 2131296332, 2131296333, 2131296334};
        WIFI_CONNECTION_STRENGTH = new int[]{2131296335, 2131296336, 2131296337, 2131296338, 2131296339};
        WIMAX_CONNECTION_STRENGTH = new int[]{2131296340, 2131296341, 2131296342, 2131296343, 2131296344};
    }
}
