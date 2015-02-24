package com.android.systemui.statusbar.policy;

class TelephonyIcons {
    static final int[][] DATA_1X;
    static final int[][] DATA_3G;
    static final int[][] DATA_4G;
    static final int[][] DATA_E;
    static final int[][] DATA_G;
    static final int[][] DATA_H;
    static final int[][] DATA_SIGNAL_STRENGTH;
    static final int[][] TELEPHONY_SIGNAL_STRENGTH;
    static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING;

    TelephonyIcons() {
    }

    static {
        TELEPHONY_SIGNAL_STRENGTH = new int[][]{new int[]{2130837657, 2130837659, 2130837661, 2130837663, 2130837665}, new int[]{2130837658, 2130837660, 2130837662, 2130837664, 2130837666}};
        TELEPHONY_SIGNAL_STRENGTH_ROAMING = new int[][]{new int[]{2130837657, 2130837659, 2130837661, 2130837663, 2130837665}, new int[]{2130837658, 2130837660, 2130837662, 2130837664, 2130837666}};
        DATA_SIGNAL_STRENGTH = TELEPHONY_SIGNAL_STRENGTH;
        DATA_G = new int[][]{new int[]{2130837629, 2130837629, 2130837629, 2130837629}, new int[]{2130837636, 2130837636, 2130837636, 2130837636}};
        DATA_3G = new int[][]{new int[]{2130837626, 2130837626, 2130837626, 2130837626}, new int[]{2130837633, 2130837633, 2130837633, 2130837633}};
        DATA_E = new int[][]{new int[]{2130837628, 2130837628, 2130837628, 2130837628}, new int[]{2130837635, 2130837635, 2130837635, 2130837635}};
        DATA_H = new int[][]{new int[]{2130837630, 2130837630, 2130837630, 2130837630}, new int[]{2130837637, 2130837637, 2130837637, 2130837637}};
        DATA_1X = new int[][]{new int[]{2130837625, 2130837625, 2130837625, 2130837625}, new int[]{2130837632, 2130837632, 2130837632, 2130837632}};
        DATA_4G = new int[][]{new int[]{2130837627, 2130837627, 2130837627, 2130837627}, new int[]{2130837634, 2130837634, 2130837634, 2130837634}};
    }
}
