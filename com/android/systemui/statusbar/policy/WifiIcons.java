package com.android.systemui.statusbar.policy;

class WifiIcons {
    static final int WIFI_LEVEL_COUNT;
    static final int[][] WIFI_SIGNAL_STRENGTH;

    WifiIcons() {
    }

    static {
        WIFI_SIGNAL_STRENGTH = new int[][]{new int[]{2130837678, 2130837679, 2130837681, 2130837683, 2130837685}, new int[]{2130837678, 2130837680, 2130837682, 2130837684, 2130837686}};
        WIFI_LEVEL_COUNT = WIFI_SIGNAL_STRENGTH[0].length;
    }
}
