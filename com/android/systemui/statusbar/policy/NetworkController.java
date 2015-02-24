package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings.System;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Slog;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.IccCard.State;
import com.android.internal.util.AsyncChannel;
import com.android.server.am.BatteryStatsService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class NetworkController extends BroadcastReceiver {
    int mAirplaneIconId;
    private boolean mAirplaneMode;
    boolean mAlwaysShowCdmaRssi;
    IBatteryStats mBatteryStats;
    private int mBluetoothTetherIconId;
    private boolean mBluetoothTethered;
    ArrayList<TextView> mCombinedLabelViews;
    ArrayList<ImageView> mCombinedSignalIconViews;
    private boolean mConnected;
    private int mConnectedNetworkType;
    private String mConnectedNetworkTypeName;
    String mContentDescriptionCombinedSignal;
    String mContentDescriptionDataType;
    String mContentDescriptionPhoneSignal;
    String mContentDescriptionWifi;
    String mContentDescriptionWimax;
    Context mContext;
    int mDataActivity;
    boolean mDataAndWifiStacked;
    boolean mDataConnected;
    int mDataDirectionIconId;
    ArrayList<ImageView> mDataDirectionIconViews;
    ArrayList<ImageView> mDataDirectionOverlayIconViews;
    int[] mDataIconList;
    int mDataNetType;
    int mDataSignalIconId;
    int mDataState;
    int mDataTypeIconId;
    ArrayList<ImageView> mDataTypeIconViews;
    private boolean mHasMobileDataFeature;
    boolean mHspaDataDistinguishable;
    private int mInetCondition;
    private boolean mIsWimaxEnabled;
    private boolean mLastAirplaneMode;
    String mLastCombinedLabel;
    int mLastCombinedSignalIconId;
    int mLastDataDirectionIconId;
    int mLastDataDirectionOverlayIconId;
    int mLastDataTypeIconId;
    int mLastPhoneSignalIconId;
    int mLastSignalLevel;
    int mLastWifiIconId;
    int mLastWimaxIconId;
    int mMobileActivityIconId;
    ArrayList<TextView> mMobileLabelViews;
    String mNetworkName;
    String mNetworkNameDefault;
    String mNetworkNameSeparator;
    final TelephonyManager mPhone;
    int mPhoneSignalIconId;
    ArrayList<ImageView> mPhoneSignalIconViews;
    int mPhoneState;
    PhoneStateListener mPhoneStateListener;
    ServiceState mServiceState;
    boolean mShowAtLeastThreeGees;
    boolean mShowPhoneRSSIForData;
    ArrayList<SignalCluster> mSignalClusters;
    SignalStrength mSignalStrength;
    State mSimState;
    int mWifiActivity;
    int mWifiActivityIconId;
    AsyncChannel mWifiChannel;
    boolean mWifiConnected;
    boolean mWifiEnabled;
    int mWifiIconId;
    ArrayList<ImageView> mWifiIconViews;
    ArrayList<TextView> mWifiLabelViews;
    int mWifiLevel;
    final WifiManager mWifiManager;
    int mWifiRssi;
    String mWifiSsid;
    private boolean mWimaxConnected;
    private int mWimaxExtraState;
    private int mWimaxIconId;
    ArrayList<ImageView> mWimaxIconViews;
    private boolean mWimaxIdle;
    private int mWimaxSignal;
    private int mWimaxState;
    private boolean mWimaxSupported;

    public static interface SignalCluster {
        void setIsAirplaneMode(boolean z, int i);

        void setMobileDataIndicators(boolean z, int i, int i2, int i3, String str, String str2);

        void setWifiIndicators(boolean z, int i, int i2, String str);
    }

    class WifiHandler extends Handler {
        WifiHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.arg1 != NetworkController.this.mWifiActivity) {
                        NetworkController.this.mWifiActivity = msg.arg1;
                        NetworkController.this.refreshViews();
                    }
                case 69632:
                    if (msg.arg1 == 0) {
                        NetworkController.this.mWifiChannel.sendMessage(Message.obtain(this, 69633));
                    } else {
                        Slog.e("StatusBar.NetworkController", "Failed to connect to wifi");
                    }
                default:
                    break;
            }
        }
    }

    public NetworkController(Context context) {
        this.mSimState = State.READY;
        this.mPhoneState = 0;
        this.mDataNetType = 0;
        this.mDataState = 0;
        this.mDataActivity = 0;
        this.mDataIconList = TelephonyIcons.DATA_G[0];
        this.mShowPhoneRSSIForData = false;
        this.mShowAtLeastThreeGees = false;
        this.mAlwaysShowCdmaRssi = false;
        this.mWifiIconId = 0;
        this.mWifiActivityIconId = 0;
        this.mWifiActivity = 0;
        this.mBluetoothTethered = false;
        this.mBluetoothTetherIconId = 17302860;
        this.mWimaxSupported = false;
        this.mIsWimaxEnabled = false;
        this.mWimaxConnected = false;
        this.mWimaxIdle = false;
        this.mWimaxIconId = 0;
        this.mWimaxSignal = 0;
        this.mWimaxState = 0;
        this.mWimaxExtraState = 0;
        this.mConnected = false;
        this.mConnectedNetworkType = -1;
        this.mInetCondition = 0;
        this.mAirplaneMode = false;
        this.mLastAirplaneMode = true;
        this.mPhoneSignalIconViews = new ArrayList();
        this.mDataDirectionIconViews = new ArrayList();
        this.mDataDirectionOverlayIconViews = new ArrayList();
        this.mWifiIconViews = new ArrayList();
        this.mWimaxIconViews = new ArrayList();
        this.mCombinedSignalIconViews = new ArrayList();
        this.mDataTypeIconViews = new ArrayList();
        this.mCombinedLabelViews = new ArrayList();
        this.mMobileLabelViews = new ArrayList();
        this.mWifiLabelViews = new ArrayList();
        this.mSignalClusters = new ArrayList();
        this.mLastPhoneSignalIconId = -1;
        this.mLastDataDirectionIconId = -1;
        this.mLastDataDirectionOverlayIconId = -1;
        this.mLastWifiIconId = -1;
        this.mLastWimaxIconId = -1;
        this.mLastCombinedSignalIconId = -1;
        this.mLastDataTypeIconId = -1;
        this.mLastCombinedLabel = "";
        this.mDataAndWifiStacked = false;
        this.mPhoneStateListener = new PhoneStateListener() {
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                NetworkController.this.mSignalStrength = signalStrength;
                NetworkController.this.updateTelephonySignalStrength();
                NetworkController.this.refreshViews();
            }

            public void onServiceStateChanged(ServiceState state) {
                NetworkController.this.mServiceState = state;
                NetworkController.this.updateTelephonySignalStrength();
                NetworkController.this.updateDataNetType();
                NetworkController.this.updateDataIcon();
                NetworkController.this.refreshViews();
            }

            public void onCallStateChanged(int state, String incomingNumber) {
                if (NetworkController.this.isCdma()) {
                    NetworkController.this.updateTelephonySignalStrength();
                    NetworkController.this.refreshViews();
                }
            }

            public void onDataConnectionStateChanged(int state, int networkType) {
                NetworkController.this.mDataState = state;
                NetworkController.this.mDataNetType = networkType;
                NetworkController.this.updateDataNetType();
                NetworkController.this.updateDataIcon();
                NetworkController.this.refreshViews();
            }

            public void onDataActivity(int direction) {
                NetworkController.this.mDataActivity = direction;
                NetworkController.this.updateDataIcon();
                NetworkController.this.refreshViews();
            }
        };
        this.mContext = context;
        Resources res = context.getResources();
        this.mHasMobileDataFeature = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).isNetworkSupported(0);
        this.mShowPhoneRSSIForData = res.getBoolean(2131230724);
        this.mShowAtLeastThreeGees = res.getBoolean(2131230725);
        this.mAlwaysShowCdmaRssi = res.getBoolean(17891378);
        updateWifiIcons();
        updateWimaxIcons();
        this.mPhone = (TelephonyManager) context.getSystemService("phone");
        this.mPhone.listen(this.mPhoneStateListener, 481);
        this.mHspaDataDistinguishable = this.mContext.getResources().getBoolean(2131230722);
        this.mNetworkNameSeparator = this.mContext.getString(2131296283);
        this.mNetworkNameDefault = this.mContext.getString(17040118);
        this.mNetworkName = this.mNetworkNameDefault;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        Handler handler = new WifiHandler();
        this.mWifiChannel = new AsyncChannel();
        Messenger wifiMessenger = this.mWifiManager.getWifiServiceMessenger();
        if (wifiMessenger != null) {
            this.mWifiChannel.connect(this.mContext, handler, wifiMessenger);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.RSSI_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.provider.Telephony.SPN_STRINGS_UPDATED");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.conn.INET_CONDITION_ACTION");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        this.mWimaxSupported = this.mContext.getResources().getBoolean(17891383);
        if (this.mWimaxSupported) {
            filter.addAction("android.net.fourG.wimax.WIMAX_NETWORK_STATE_CHANGED");
            filter.addAction("android.net.wimax.SIGNAL_LEVEL_CHANGED");
            filter.addAction("android.net.fourG.NET_4G_STATE_CHANGED");
        }
        context.registerReceiver(this, filter);
        updateAirplaneMode();
        this.mBatteryStats = BatteryStatsService.getService();
    }

    public boolean hasMobileDataFeature() {
        return this.mHasMobileDataFeature;
    }

    public void addPhoneSignalIconView(ImageView v) {
        this.mPhoneSignalIconViews.add(v);
    }

    public void addWifiIconView(ImageView v) {
        this.mWifiIconViews.add(v);
    }

    public void addDataTypeIconView(ImageView v) {
        this.mDataTypeIconViews.add(v);
    }

    public void addCombinedLabelView(TextView v) {
        this.mCombinedLabelViews.add(v);
    }

    public void addMobileLabelView(TextView v) {
        this.mMobileLabelViews.add(v);
    }

    public void addWifiLabelView(TextView v) {
        this.mWifiLabelViews.add(v);
    }

    public void addSignalCluster(SignalCluster cluster) {
        this.mSignalClusters.add(cluster);
        refreshSignalCluster(cluster);
    }

    public void refreshSignalCluster(SignalCluster cluster) {
        boolean z = this.mWifiEnabled && (this.mWifiConnected || !this.mHasMobileDataFeature);
        cluster.setWifiIndicators(z, this.mWifiIconId, this.mWifiActivityIconId, this.mContentDescriptionWifi);
        if (this.mIsWimaxEnabled && this.mWimaxConnected) {
            int i;
            if (this.mAlwaysShowCdmaRssi) {
                i = this.mPhoneSignalIconId;
            } else {
                i = this.mWimaxIconId;
            }
            cluster.setMobileDataIndicators(true, i, this.mMobileActivityIconId, this.mDataTypeIconId, this.mContentDescriptionWimax, this.mContentDescriptionDataType);
        } else {
            cluster.setMobileDataIndicators(this.mHasMobileDataFeature, this.mShowPhoneRSSIForData ? this.mPhoneSignalIconId : this.mDataSignalIconId, this.mMobileActivityIconId, this.mDataTypeIconId, this.mContentDescriptionPhoneSignal, this.mContentDescriptionDataType);
        }
        cluster.setIsAirplaneMode(this.mAirplaneMode, this.mAirplaneIconId);
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.net.wifi.RSSI_CHANGED") || action.equals("android.net.wifi.WIFI_STATE_CHANGED") || action.equals("android.net.wifi.STATE_CHANGE")) {
            updateWifiState(intent);
            refreshViews();
        } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
            updateSimState(intent);
            updateDataIcon();
            refreshViews();
        } else if (action.equals("android.provider.Telephony.SPN_STRINGS_UPDATED")) {
            updateNetworkName(intent.getBooleanExtra("showSpn", false), intent.getStringExtra("spn"), intent.getBooleanExtra("showPlmn", false), intent.getStringExtra("plmn"));
            refreshViews();
        } else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE") || action.equals("android.net.conn.INET_CONDITION_ACTION")) {
            updateConnectivity(intent);
            refreshViews();
        } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
            refreshViews();
        } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
            updateAirplaneMode();
            refreshViews();
        } else if (action.equals("android.net.fourG.NET_4G_STATE_CHANGED") || action.equals("android.net.wimax.SIGNAL_LEVEL_CHANGED") || action.equals("android.net.fourG.wimax.WIMAX_NETWORK_STATE_CHANGED")) {
            updateWimaxState(intent);
            refreshViews();
        }
    }

    private final void updateSimState(Intent intent) {
        String stateExtra = intent.getStringExtra("ss");
        if ("ABSENT".equals(stateExtra)) {
            this.mSimState = State.ABSENT;
        } else if ("READY".equals(stateExtra)) {
            this.mSimState = State.READY;
        } else if ("LOCKED".equals(stateExtra)) {
            String lockedReason = intent.getStringExtra("reason");
            if ("PIN".equals(lockedReason)) {
                this.mSimState = State.PIN_REQUIRED;
            } else if ("PUK".equals(lockedReason)) {
                this.mSimState = State.PUK_REQUIRED;
            } else {
                this.mSimState = State.NETWORK_LOCKED;
            }
        } else {
            this.mSimState = State.UNKNOWN;
        }
    }

    private boolean isCdma() {
        return (this.mSignalStrength == null || this.mSignalStrength.isGsm()) ? false : true;
    }

    private boolean hasService() {
        if (this.mServiceState == null) {
            return false;
        }
        switch (this.mServiceState.getState()) {
            case 1:
            case 3:
                return false;
            default:
                return true;
        }
    }

    private void updateAirplaneMode() {
        boolean z = true;
        if (System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 1) {
            z = false;
        }
        this.mAirplaneMode = z;
    }

    private final void updateTelephonySignalStrength() {
        if (!hasService()) {
            this.mPhoneSignalIconId = 2130837670;
            this.mDataSignalIconId = 2130837670;
        } else if (this.mSignalStrength == null) {
            this.mPhoneSignalIconId = 2130837670;
            this.mDataSignalIconId = 2130837670;
            this.mContentDescriptionPhoneSignal = this.mContext.getString(AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0]);
        } else {
            int iconLevel;
            int[] iconList;
            if (isCdma() && this.mAlwaysShowCdmaRssi) {
                iconLevel = this.mSignalStrength.getCdmaLevel();
                this.mLastSignalLevel = iconLevel;
            } else {
                iconLevel = this.mSignalStrength.getLevel();
                this.mLastSignalLevel = iconLevel;
            }
            if (isCdma()) {
                if (isCdmaEri()) {
                    iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_ROAMING[this.mInetCondition];
                } else {
                    iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[this.mInetCondition];
                }
            } else if (this.mPhone.isNetworkRoaming()) {
                iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_ROAMING[this.mInetCondition];
            } else {
                iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[this.mInetCondition];
            }
            this.mPhoneSignalIconId = iconList[iconLevel];
            this.mContentDescriptionPhoneSignal = this.mContext.getString(AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[iconLevel]);
            this.mDataSignalIconId = TelephonyIcons.DATA_SIGNAL_STRENGTH[this.mInetCondition][iconLevel];
        }
    }

    private final void updateDataNetType() {
        if (this.mIsWimaxEnabled && this.mWimaxConnected) {
            this.mDataIconList = TelephonyIcons.DATA_4G[this.mInetCondition];
            this.mDataTypeIconId = 2130837627;
            this.mContentDescriptionDataType = this.mContext.getString(2131296348);
        } else {
            switch (this.mDataNetType) {
                case 0:
                    if (!this.mShowAtLeastThreeGees) {
                        this.mDataIconList = TelephonyIcons.DATA_G[this.mInetCondition];
                        this.mDataTypeIconId = 0;
                        this.mContentDescriptionDataType = this.mContext.getString(2131296345);
                    }
                    if (!this.mShowAtLeastThreeGees) {
                        this.mDataIconList = TelephonyIcons.DATA_E[this.mInetCondition];
                        this.mDataTypeIconId = 2130837628;
                        this.mContentDescriptionDataType = this.mContext.getString(2131296350);
                    }
                    this.mDataIconList = TelephonyIcons.DATA_3G[this.mInetCondition];
                    this.mDataTypeIconId = 2130837626;
                    this.mContentDescriptionDataType = this.mContext.getString(2131296346);
                    break;
                case 2:
                    if (this.mShowAtLeastThreeGees) {
                        this.mDataIconList = TelephonyIcons.DATA_E[this.mInetCondition];
                        this.mDataTypeIconId = 2130837628;
                        this.mContentDescriptionDataType = this.mContext.getString(2131296350);
                    }
                    this.mDataIconList = TelephonyIcons.DATA_3G[this.mInetCondition];
                    this.mDataTypeIconId = 2130837626;
                    this.mContentDescriptionDataType = this.mContext.getString(2131296346);
                    break;
                case 3:
                    this.mDataIconList = TelephonyIcons.DATA_3G[this.mInetCondition];
                    this.mDataTypeIconId = 2130837626;
                    this.mContentDescriptionDataType = this.mContext.getString(2131296346);
                    break;
                case 4:
                    this.mDataIconList = TelephonyIcons.DATA_1X[this.mInetCondition];
                    this.mDataTypeIconId = 2130837625;
                    this.mContentDescriptionDataType = this.mContext.getString(2131296349);
                    break;
                case 5:
                case 6:
                case 12:
                case 14:
                    this.mDataIconList = TelephonyIcons.DATA_3G[this.mInetCondition];
                    this.mDataTypeIconId = 2130837626;
                    this.mContentDescriptionDataType = this.mContext.getString(2131296346);
                    break;
                case 7:
                    this.mDataIconList = TelephonyIcons.DATA_1X[this.mInetCondition];
                    this.mDataTypeIconId = 2130837625;
                    this.mContentDescriptionDataType = this.mContext.getString(2131296349);
                    break;
                case 8:
                case 9:
                case 10:
                case 15:
                    if (this.mHspaDataDistinguishable) {
                        this.mDataIconList = TelephonyIcons.DATA_H[this.mInetCondition];
                        this.mDataTypeIconId = 2130837630;
                        this.mContentDescriptionDataType = this.mContext.getString(2131296347);
                    } else {
                        this.mDataIconList = TelephonyIcons.DATA_3G[this.mInetCondition];
                        this.mDataTypeIconId = 2130837626;
                        this.mContentDescriptionDataType = this.mContext.getString(2131296346);
                    }
                    break;
                case 13:
                    this.mDataIconList = TelephonyIcons.DATA_4G[this.mInetCondition];
                    this.mDataTypeIconId = 2130837627;
                    this.mContentDescriptionDataType = this.mContext.getString(2131296348);
                    break;
                default:
                    if (this.mShowAtLeastThreeGees) {
                        this.mDataIconList = TelephonyIcons.DATA_3G[this.mInetCondition];
                        this.mDataTypeIconId = 2130837626;
                        this.mContentDescriptionDataType = this.mContext.getString(2131296346);
                    } else {
                        this.mDataIconList = TelephonyIcons.DATA_G[this.mInetCondition];
                        this.mDataTypeIconId = 2130837629;
                        this.mContentDescriptionDataType = this.mContext.getString(2131296345);
                    }
                    break;
            }
        }
        if (isCdma()) {
            if (isCdmaEri()) {
                this.mDataTypeIconId = 2130837631;
            }
        } else if (this.mPhone.isNetworkRoaming()) {
            this.mDataTypeIconId = 2130837631;
        }
    }

    boolean isCdmaEri() {
        if (!(this.mServiceState == null || this.mServiceState.getCdmaEriIconIndex() == 1)) {
            int iconMode = this.mServiceState.getCdmaEriIconMode();
            if (iconMode == 0 || iconMode == 1) {
                return true;
            }
        }
        return false;
    }

    private final void updateDataIcon() {
        int iconId;
        boolean visible = true;
        if (isCdma()) {
            if (hasService() && this.mDataState == 2) {
                switch (this.mDataActivity) {
                    case 1:
                        iconId = this.mDataIconList[1];
                        break;
                    case 2:
                        iconId = this.mDataIconList[2];
                        break;
                    case 3:
                        iconId = this.mDataIconList[3];
                        break;
                    default:
                        iconId = this.mDataIconList[0];
                        break;
                }
            } else {
                iconId = 0;
                visible = false;
            }
        } else if (this.mSimState != State.READY && this.mSimState != State.UNKNOWN) {
            iconId = 2130837650;
            visible = false;
        } else if (hasService() && this.mDataState == 2) {
            switch (this.mDataActivity) {
                case 1:
                    iconId = this.mDataIconList[1];
                    break;
                case 2:
                    iconId = this.mDataIconList[2];
                    break;
                case 3:
                    iconId = this.mDataIconList[3];
                    break;
                default:
                    iconId = this.mDataIconList[0];
                    break;
            }
            this.mDataDirectionIconId = iconId;
        } else {
            iconId = 0;
            visible = false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.notePhoneDataConnectionState(this.mPhone.getNetworkType(), visible);
            Binder.restoreCallingIdentity(ident);
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(ident);
        }
        this.mDataDirectionIconId = iconId;
        this.mDataConnected = visible;
    }

    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        StringBuilder str = new StringBuilder();
        boolean something = false;
        if (showPlmn && plmn != null) {
            str.append(plmn);
            something = true;
        }
        if (showSpn && spn != null) {
            if (something) {
                str.append(this.mNetworkNameSeparator);
            }
            str.append(spn);
            something = true;
        }
        if (something) {
            this.mNetworkName = str.toString();
        } else {
            this.mNetworkName = this.mNetworkNameDefault;
        }
    }

    private void updateWifiState(Intent intent) {
        boolean z = true;
        String action = intent.getAction();
        if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
            if (intent.getIntExtra("wifi_state", 4) != 3) {
                z = false;
            }
            this.mWifiEnabled = z;
        } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            boolean wasConnected = this.mWifiConnected;
            if (networkInfo == null || !networkInfo.isConnected()) {
                z = false;
            }
            this.mWifiConnected = z;
            if (this.mWifiConnected && !wasConnected) {
                WifiInfo info = (WifiInfo) intent.getParcelableExtra("wifiInfo");
                if (info == null) {
                    info = this.mWifiManager.getConnectionInfo();
                }
                if (info != null) {
                    this.mWifiSsid = huntForSsid(info);
                } else {
                    this.mWifiSsid = null;
                }
            } else if (!this.mWifiConnected) {
                this.mWifiSsid = null;
            }
        } else if (action.equals("android.net.wifi.RSSI_CHANGED")) {
            this.mWifiRssi = intent.getIntExtra("newRssi", -200);
            this.mWifiLevel = WifiManager.calculateSignalLevel(this.mWifiRssi, WifiIcons.WIFI_LEVEL_COUNT);
        }
        updateWifiIcons();
    }

    private void updateWifiIcons() {
        int i = 0;
        if (this.mWifiConnected) {
            this.mWifiIconId = WifiIcons.WIFI_SIGNAL_STRENGTH[this.mInetCondition][this.mWifiLevel];
            this.mContentDescriptionWifi = this.mContext.getString(AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH[this.mWifiLevel]);
            return;
        }
        if (this.mDataAndWifiStacked) {
            this.mWifiIconId = 0;
        } else {
            if (this.mWifiEnabled) {
                i = 2130837687;
            }
            this.mWifiIconId = i;
        }
        this.mContentDescriptionWifi = this.mContext.getString(2131296335);
    }

    private String huntForSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        for (WifiConfiguration net : this.mWifiManager.getConfiguredNetworks()) {
            if (net.networkId == info.getNetworkId()) {
                return net.SSID;
            }
        }
        return null;
    }

    private final void updateWimaxState(Intent intent) {
        boolean z = true;
        String action = intent.getAction();
        boolean z2 = this.mWimaxConnected;
        if (action.equals("android.net.fourG.NET_4G_STATE_CHANGED")) {
            if (intent.getIntExtra("4g_state", 4) != 3) {
                z = false;
            }
            this.mIsWimaxEnabled = z;
        } else if (action.equals("android.net.wimax.SIGNAL_LEVEL_CHANGED")) {
            this.mWimaxSignal = intent.getIntExtra("newSignalLevel", 0);
        } else if (action.equals("android.net.fourG.wimax.WIMAX_NETWORK_STATE_CHANGED")) {
            boolean z3;
            this.mWimaxState = intent.getIntExtra("WimaxState", 4);
            this.mWimaxExtraState = intent.getIntExtra("WimaxStateDetail", 4);
            if (this.mWimaxState == 7) {
                z3 = true;
            } else {
                z3 = false;
            }
            this.mWimaxConnected = z3;
            if (this.mWimaxExtraState != 6) {
                z = false;
            }
            this.mWimaxIdle = z;
        }
        updateDataNetType();
        updateWimaxIcons();
    }

    private void updateWimaxIcons() {
        if (!this.mIsWimaxEnabled) {
            this.mWimaxIconId = 0;
        } else if (this.mWimaxConnected) {
            if (this.mWimaxIdle) {
                this.mWimaxIconId = WimaxIcons.WIMAX_IDLE;
            } else {
                this.mWimaxIconId = WimaxIcons.WIMAX_SIGNAL_STRENGTH[this.mInetCondition][this.mWimaxSignal];
            }
            this.mContentDescriptionWimax = this.mContext.getString(AccessibilityContentDescriptions.WIMAX_CONNECTION_STRENGTH[this.mWimaxSignal]);
        } else {
            this.mWimaxIconId = WimaxIcons.WIMAX_DISCONNECTED;
            this.mContentDescriptionWimax = this.mContext.getString(2131296340);
        }
    }

    private void updateConnectivity(Intent intent) {
        boolean z;
        int i = 1;
        NetworkInfo info = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            z = false;
        } else {
            z = true;
        }
        this.mConnected = z;
        if (this.mConnected) {
            this.mConnectedNetworkType = info.getType();
            this.mConnectedNetworkTypeName = info.getTypeName();
        } else {
            this.mConnectedNetworkType = -1;
            this.mConnectedNetworkTypeName = null;
        }
        if (intent.getIntExtra("inetCondition", 0) <= 50) {
            i = 0;
        }
        this.mInetCondition = i;
        if (info == null || info.getType() != 7) {
            this.mBluetoothTethered = false;
        } else {
            this.mBluetoothTethered = info.isConnected();
        }
        updateDataNetType();
        updateWimaxIcons();
        updateDataIcon();
        updateTelephonySignalStrength();
        updateWifiIcons();
    }

    void refreshViews() {
        int i;
        int i2;
        int i3;
        String str;
        int size;
        int i4;
        ImageView imageView;
        Context context = this.mContext;
        String str2 = "";
        String str3 = "";
        str3 = "";
        if (this.mHasMobileDataFeature) {
            if (this.mDataConnected) {
                str3 = this.mNetworkName;
            } else if (!this.mConnected) {
                str3 = context.getString(2131296371);
            } else if (hasService()) {
                str3 = this.mNetworkName;
            } else {
                str3 = "";
            }
            if (this.mDataConnected) {
                int i5 = this.mDataSignalIconId;
                switch (this.mDataActivity) {
                    case 1:
                        this.mMobileActivityIconId = 2130837668;
                        break;
                    case 2:
                        this.mMobileActivityIconId = 2130837671;
                        break;
                    case 3:
                        this.mMobileActivityIconId = 2130837669;
                        break;
                    default:
                        this.mMobileActivityIconId = 0;
                        break;
                }
                i5 = this.mMobileActivityIconId;
                i = this.mDataSignalIconId;
                this.mContentDescriptionCombinedSignal = this.mContentDescriptionDataType;
                i2 = i;
                i = i5;
                str2 = str3;
            } else {
                i = 0;
                i2 = 0;
            }
        } else {
            this.mPhoneSignalIconId = 0;
            this.mDataSignalIconId = 0;
            str3 = "";
            i = 0;
            i2 = 0;
        }
        if (this.mWifiConnected) {
            if (this.mWifiSsid == null) {
                str2 = context.getString(2131296372);
                this.mWifiActivityIconId = 0;
            } else {
                str2 = this.mWifiSsid;
                switch (this.mWifiActivity) {
                    case 0:
                        this.mWifiActivityIconId = 0;
                        break;
                    case 1:
                        this.mWifiActivityIconId = 2130837675;
                        break;
                    case 2:
                        this.mWifiActivityIconId = 2130837677;
                        break;
                    case 3:
                        this.mWifiActivityIconId = 2130837676;
                        break;
                    default:
                        break;
                }
            }
            i = this.mWifiActivityIconId;
            i2 = this.mWifiIconId;
            this.mContentDescriptionCombinedSignal = this.mContentDescriptionWifi;
            i3 = i2;
            i2 = i;
            str = str2;
        } else if (this.mHasMobileDataFeature) {
            i3 = i2;
            i2 = i;
            str = str2;
            str2 = "";
        } else {
            i3 = i2;
            i2 = i;
            str = str2;
            str2 = context.getString(2131296371);
        }
        if (this.mBluetoothTethered) {
            str = this.mContext.getString(2131296284);
            i3 = this.mBluetoothTetherIconId;
            this.mContentDescriptionCombinedSignal = this.mContext.getString(2131296353);
        }
        if (this.mConnectedNetworkType == 9) {
            Object obj = 1;
        } else {
            int i6 = 0;
        }
        if (i6 != 0) {
            str = this.mConnectedNetworkTypeName;
        }
        String str4;
        if (this.mAirplaneMode && (this.mServiceState == null || (!hasService() && !this.mServiceState.isEmergencyOnly()))) {
            this.mContentDescriptionPhoneSignal = this.mContext.getString(2131296354);
            this.mAirplaneIconId = 2130837667;
            this.mDataTypeIconId = 0;
            this.mDataSignalIconId = 0;
            this.mPhoneSignalIconId = 0;
            if (this.mWifiConnected) {
                i6 = i3;
                str4 = str;
                str = str2;
                str2 = "";
            } else {
                if (this.mHasMobileDataFeature) {
                    str2 = "";
                } else {
                    str2 = context.getString(2131296371);
                    str = str2;
                }
                this.mContentDescriptionCombinedSignal = this.mContentDescriptionPhoneSignal;
                i6 = this.mDataSignalIconId;
                str4 = str;
                str = str2;
                str2 = str3;
            }
        } else if (this.mDataConnected || this.mWifiConnected || this.mBluetoothTethered || this.mWimaxConnected || i6 != 0) {
            i6 = i3;
            str4 = str;
            str = str2;
            str2 = str3;
        } else {
            String string = context.getString(2131296371);
            i = this.mHasMobileDataFeature ? this.mDataSignalIconId : this.mWifiIconId;
            this.mContentDescriptionCombinedSignal = this.mHasMobileDataFeature ? this.mContentDescriptionDataType : this.mContentDescriptionWifi;
            this.mDataTypeIconId = 0;
            if (isCdma()) {
                if (isCdmaEri()) {
                    this.mDataTypeIconId = 2130837631;
                    str4 = string;
                    i6 = i;
                    str = str2;
                    str2 = str3;
                }
            } else if (this.mPhone.isNetworkRoaming()) {
                this.mDataTypeIconId = 2130837631;
            }
            str4 = string;
            i6 = i;
            str = str2;
            str2 = str3;
        }
        if (!(this.mLastPhoneSignalIconId == this.mPhoneSignalIconId && this.mLastDataDirectionOverlayIconId == i2 && this.mLastWifiIconId == this.mWifiIconId && this.mLastWimaxIconId == this.mWimaxIconId && this.mLastDataTypeIconId == this.mDataTypeIconId && this.mLastAirplaneMode == this.mAirplaneMode)) {
            Iterator it = this.mSignalClusters.iterator();
            while (it.hasNext()) {
                refreshSignalCluster((SignalCluster) it.next());
            }
        }
        if (this.mLastAirplaneMode != this.mAirplaneMode) {
            this.mLastAirplaneMode = this.mAirplaneMode;
        }
        if (this.mLastPhoneSignalIconId != this.mPhoneSignalIconId) {
            this.mLastPhoneSignalIconId = this.mPhoneSignalIconId;
            size = this.mPhoneSignalIconViews.size();
            for (i4 = 0; i4 < size; i4++) {
                imageView = (ImageView) this.mPhoneSignalIconViews.get(i4);
                if (this.mPhoneSignalIconId == 0) {
                    imageView.setVisibility(8);
                } else {
                    imageView.setVisibility(0);
                    imageView.setImageResource(this.mPhoneSignalIconId);
                    imageView.setContentDescription(this.mContentDescriptionPhoneSignal);
                }
            }
        }
        if (this.mLastDataDirectionIconId != this.mDataDirectionIconId) {
            this.mLastDataDirectionIconId = this.mDataDirectionIconId;
            size = this.mDataDirectionIconViews.size();
            for (i4 = 0; i4 < size; i4++) {
                imageView = (ImageView) this.mDataDirectionIconViews.get(i4);
                imageView.setImageResource(this.mDataDirectionIconId);
                imageView.setContentDescription(this.mContentDescriptionDataType);
            }
        }
        if (this.mLastWifiIconId != this.mWifiIconId) {
            this.mLastWifiIconId = this.mWifiIconId;
            size = this.mWifiIconViews.size();
            for (i4 = 0; i4 < size; i4++) {
                imageView = (ImageView) this.mWifiIconViews.get(i4);
                if (this.mWifiIconId == 0) {
                    imageView.setVisibility(8);
                } else {
                    imageView.setVisibility(0);
                    imageView.setImageResource(this.mWifiIconId);
                    imageView.setContentDescription(this.mContentDescriptionWifi);
                }
            }
        }
        if (this.mLastWimaxIconId != this.mWimaxIconId) {
            this.mLastWimaxIconId = this.mWimaxIconId;
            size = this.mWimaxIconViews.size();
            for (i4 = 0; i4 < size; i4++) {
                imageView = (ImageView) this.mWimaxIconViews.get(i4);
                if (this.mWimaxIconId == 0) {
                    imageView.setVisibility(8);
                } else {
                    imageView.setVisibility(0);
                    imageView.setImageResource(this.mWimaxIconId);
                    imageView.setContentDescription(this.mContentDescriptionWimax);
                }
            }
        }
        if (this.mLastCombinedSignalIconId != i6) {
            this.mLastCombinedSignalIconId = i6;
            size = this.mCombinedSignalIconViews.size();
            for (i4 = 0; i4 < size; i4++) {
                imageView = (ImageView) this.mCombinedSignalIconViews.get(i4);
                imageView.setImageResource(i6);
                imageView.setContentDescription(this.mContentDescriptionCombinedSignal);
            }
        }
        if (this.mLastDataTypeIconId != this.mDataTypeIconId) {
            this.mLastDataTypeIconId = this.mDataTypeIconId;
            i4 = this.mDataTypeIconViews.size();
            for (i6 = 0; i6 < i4; i6++) {
                imageView = (ImageView) this.mDataTypeIconViews.get(i6);
                if (this.mDataTypeIconId == 0) {
                    imageView.setVisibility(8);
                } else {
                    imageView.setVisibility(0);
                    imageView.setImageResource(this.mDataTypeIconId);
                    imageView.setContentDescription(this.mContentDescriptionDataType);
                }
            }
        }
        if (this.mLastDataDirectionOverlayIconId != i2) {
            this.mLastDataDirectionOverlayIconId = i2;
            i4 = this.mDataDirectionOverlayIconViews.size();
            for (i6 = 0; i6 < i4; i6++) {
                imageView = (ImageView) this.mDataDirectionOverlayIconViews.get(i6);
                if (i2 == 0) {
                    imageView.setVisibility(8);
                } else {
                    imageView.setVisibility(0);
                    imageView.setImageResource(i2);
                    imageView.setContentDescription(this.mContentDescriptionDataType);
                }
            }
        }
        if (!this.mLastCombinedLabel.equals(r5)) {
            this.mLastCombinedLabel = r5;
            i6 = this.mCombinedLabelViews.size();
            for (i2 = 0; i2 < i6; i2++) {
                ((TextView) this.mCombinedLabelViews.get(i2)).setText(r5);
            }
        }
        i3 = this.mWifiLabelViews.size();
        for (i2 = 0; i2 < i3; i2++) {
            TextView textView = (TextView) this.mWifiLabelViews.get(i2);
            textView.setText(r2);
            if ("".equals(r2)) {
                textView.setVisibility(8);
            } else {
                textView.setVisibility(0);
            }
        }
        i2 = this.mMobileLabelViews.size();
        for (i = 0; i < i2; i++) {
            textView = (TextView) this.mMobileLabelViews.get(i);
            textView.setText(r1);
            if ("".equals(r1)) {
                textView.setVisibility(8);
            } else {
                textView.setVisibility(0);
            }
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NetworkController state:");
        String str = "  %s network type %d (%s)";
        Object[] objArr = new Object[3];
        objArr[0] = this.mConnected ? "CONNECTED" : "DISCONNECTED";
        objArr[1] = Integer.valueOf(this.mConnectedNetworkType);
        objArr[2] = this.mConnectedNetworkTypeName;
        printWriter.println(String.format(str, objArr));
        printWriter.println("  - telephony ------");
        printWriter.print("  hasService()=");
        printWriter.println(hasService());
        printWriter.print("  mHspaDataDistinguishable=");
        printWriter.println(this.mHspaDataDistinguishable);
        printWriter.print("  mDataConnected=");
        printWriter.println(this.mDataConnected);
        printWriter.print("  mSimState=");
        printWriter.println(this.mSimState);
        printWriter.print("  mPhoneState=");
        printWriter.println(this.mPhoneState);
        printWriter.print("  mDataState=");
        printWriter.println(this.mDataState);
        printWriter.print("  mDataActivity=");
        printWriter.println(this.mDataActivity);
        printWriter.print("  mDataNetType=");
        printWriter.print(this.mDataNetType);
        printWriter.print("/");
        printWriter.println(TelephonyManager.getNetworkTypeName(this.mDataNetType));
        printWriter.print("  mServiceState=");
        printWriter.println(this.mServiceState);
        printWriter.print("  mSignalStrength=");
        printWriter.println(this.mSignalStrength);
        printWriter.print("  mLastSignalLevel=");
        printWriter.println(this.mLastSignalLevel);
        printWriter.print("  mNetworkName=");
        printWriter.println(this.mNetworkName);
        printWriter.print("  mNetworkNameDefault=");
        printWriter.println(this.mNetworkNameDefault);
        printWriter.print("  mNetworkNameSeparator=");
        printWriter.println(this.mNetworkNameSeparator.replace("\n", "\\n"));
        printWriter.print("  mPhoneSignalIconId=0x");
        printWriter.print(Integer.toHexString(this.mPhoneSignalIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mPhoneSignalIconId));
        printWriter.print("  mDataDirectionIconId=");
        printWriter.print(Integer.toHexString(this.mDataDirectionIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mDataDirectionIconId));
        printWriter.print("  mDataSignalIconId=");
        printWriter.print(Integer.toHexString(this.mDataSignalIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mDataSignalIconId));
        printWriter.print("  mDataTypeIconId=");
        printWriter.print(Integer.toHexString(this.mDataTypeIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mDataTypeIconId));
        printWriter.println("  - wifi ------");
        printWriter.print("  mWifiEnabled=");
        printWriter.println(this.mWifiEnabled);
        printWriter.print("  mWifiConnected=");
        printWriter.println(this.mWifiConnected);
        printWriter.print("  mWifiRssi=");
        printWriter.println(this.mWifiRssi);
        printWriter.print("  mWifiLevel=");
        printWriter.println(this.mWifiLevel);
        printWriter.print("  mWifiSsid=");
        printWriter.println(this.mWifiSsid);
        printWriter.println(String.format("  mWifiIconId=0x%08x/%s", new Object[]{Integer.valueOf(this.mWifiIconId), getResourceName(this.mWifiIconId)}));
        printWriter.print("  mWifiActivity=");
        printWriter.println(this.mWifiActivity);
        if (this.mWimaxSupported) {
            printWriter.println("  - wimax ------");
            printWriter.print("  mIsWimaxEnabled=");
            printWriter.println(this.mIsWimaxEnabled);
            printWriter.print("  mWimaxConnected=");
            printWriter.println(this.mWimaxConnected);
            printWriter.print("  mWimaxIdle=");
            printWriter.println(this.mWimaxIdle);
            printWriter.println(String.format("  mWimaxIconId=0x%08x/%s", new Object[]{Integer.valueOf(this.mWimaxIconId), getResourceName(this.mWimaxIconId)}));
            printWriter.println(String.format("  mWimaxSignal=%d", new Object[]{Integer.valueOf(this.mWimaxSignal)}));
            printWriter.println(String.format("  mWimaxState=%d", new Object[]{Integer.valueOf(this.mWimaxState)}));
            printWriter.println(String.format("  mWimaxExtraState=%d", new Object[]{Integer.valueOf(this.mWimaxExtraState)}));
        }
        printWriter.println("  - Bluetooth ----");
        printWriter.print("  mBtReverseTethered=");
        printWriter.println(this.mBluetoothTethered);
        printWriter.println("  - connectivity ------");
        printWriter.print("  mInetCondition=");
        printWriter.println(this.mInetCondition);
        printWriter.println("  - icons ------");
        printWriter.print("  mLastPhoneSignalIconId=0x");
        printWriter.print(Integer.toHexString(this.mLastPhoneSignalIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mLastPhoneSignalIconId));
        printWriter.print("  mLastDataDirectionIconId=0x");
        printWriter.print(Integer.toHexString(this.mLastDataDirectionIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mLastDataDirectionIconId));
        printWriter.print("  mLastDataDirectionOverlayIconId=0x");
        printWriter.print(Integer.toHexString(this.mLastDataDirectionOverlayIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mLastDataDirectionOverlayIconId));
        printWriter.print("  mLastWifiIconId=0x");
        printWriter.print(Integer.toHexString(this.mLastWifiIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mLastWifiIconId));
        printWriter.print("  mLastCombinedSignalIconId=0x");
        printWriter.print(Integer.toHexString(this.mLastCombinedSignalIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mLastCombinedSignalIconId));
        printWriter.print("  mLastDataTypeIconId=0x");
        printWriter.print(Integer.toHexString(this.mLastDataTypeIconId));
        printWriter.print("/");
        printWriter.println(getResourceName(this.mLastDataTypeIconId));
        printWriter.print("  mLastCombinedLabel=");
        printWriter.print(this.mLastCombinedLabel);
        printWriter.println("");
    }

    private String getResourceName(int i) {
        if (i == 0) {
            return "(null)";
        }
        try {
            return this.mContext.getResources().getResourceName(i);
        } catch (NotFoundException e) {
            return "(unknown)";
        }
    }
}
