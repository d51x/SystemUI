package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.SignalCluster;

public class SignalClusterView extends LinearLayout implements SignalCluster {
    ImageView mAirplane;
    private int mAirplaneIconId;
    private boolean mIsAirplaneMode;
    ImageView mMobile;
    ImageView mMobileActivity;
    private int mMobileActivityId;
    private String mMobileDescription;
    ViewGroup mMobileGroup;
    private int mMobileStrengthId;
    ImageView mMobileType;
    private String mMobileTypeDescription;
    private int mMobileTypeId;
    private boolean mMobileVisible;
    NetworkController mNC;
    View mSpacer;
    ImageView mWifi;
    ImageView mWifiActivity;
    private int mWifiActivityId;
    private String mWifiDescription;
    ViewGroup mWifiGroup;
    private int mWifiStrengthId;
    private boolean mWifiVisible;

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mWifiVisible = false;
        this.mWifiStrengthId = 0;
        this.mWifiActivityId = 0;
        this.mMobileVisible = false;
        this.mMobileStrengthId = 0;
        this.mMobileActivityId = 0;
        this.mMobileTypeId = 0;
        this.mIsAirplaneMode = false;
        this.mAirplaneIconId = 0;
    }

    public void setNetworkController(NetworkController nc) {
        this.mNC = nc;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mWifiGroup = (ViewGroup) findViewById(2131492890);
        this.mWifi = (ImageView) findViewById(2131492891);
        this.mWifiActivity = (ImageView) findViewById(2131492892);
        this.mMobileGroup = (ViewGroup) findViewById(2131492894);
        this.mMobile = (ImageView) findViewById(2131492895);
        this.mMobileActivity = (ImageView) findViewById(2131492897);
        this.mMobileType = (ImageView) findViewById(2131492896);
        this.mSpacer = findViewById(2131492893);
        this.mAirplane = (ImageView) findViewById(2131492898);
        apply();
    }

    protected void onDetachedFromWindow() {
        this.mWifiGroup = null;
        this.mWifi = null;
        this.mWifiActivity = null;
        this.mMobileGroup = null;
        this.mMobile = null;
        this.mMobileActivity = null;
        this.mMobileType = null;
        this.mSpacer = null;
        this.mAirplane = null;
        super.onDetachedFromWindow();
    }

    public void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon, String contentDescription) {
        this.mWifiVisible = visible;
        this.mWifiStrengthId = strengthIcon;
        this.mWifiActivityId = activityIcon;
        this.mWifiDescription = contentDescription;
        apply();
    }

    public void setMobileDataIndicators(boolean visible, int strengthIcon, int activityIcon, int typeIcon, String contentDescription, String typeContentDescription) {
        this.mMobileVisible = visible;
        this.mMobileStrengthId = strengthIcon;
        this.mMobileActivityId = activityIcon;
        this.mMobileTypeId = typeIcon;
        this.mMobileDescription = contentDescription;
        this.mMobileTypeDescription = typeContentDescription;
        apply();
    }

    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        this.mIsAirplaneMode = is;
        this.mAirplaneIconId = airplaneIconId;
        apply();
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (this.mWifiVisible && this.mWifiGroup.getContentDescription() != null) {
            event.getText().add(this.mWifiGroup.getContentDescription());
        }
        if (this.mMobileVisible && this.mMobileGroup.getContentDescription() != null) {
            event.getText().add(this.mMobileGroup.getContentDescription());
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    private void apply() {
        int i = 0;
        if (this.mWifiGroup != null) {
            if (this.mWifiVisible) {
                this.mWifiGroup.setVisibility(0);
                this.mWifi.setImageResource(this.mWifiStrengthId);
                this.mWifiActivity.setImageResource(this.mWifiActivityId);
                this.mWifiGroup.setContentDescription(this.mWifiDescription);
            } else {
                this.mWifiGroup.setVisibility(8);
            }
            if (!this.mMobileVisible || this.mIsAirplaneMode) {
                this.mMobileGroup.setVisibility(8);
            } else {
                this.mMobileGroup.setVisibility(0);
                this.mMobile.setImageResource(this.mMobileStrengthId);
                this.mMobileActivity.setImageResource(this.mMobileActivityId);
                this.mMobileType.setImageResource(this.mMobileTypeId);
                this.mMobileGroup.setContentDescription(this.mMobileTypeDescription + " " + this.mMobileDescription);
            }
            if (this.mIsAirplaneMode) {
                this.mAirplane.setVisibility(0);
                this.mAirplane.setImageResource(this.mAirplaneIconId);
            } else {
                this.mAirplane.setVisibility(8);
            }
            if (this.mMobileVisible && this.mWifiVisible && this.mIsAirplaneMode) {
                this.mSpacer.setVisibility(4);
            } else {
                this.mSpacer.setVisibility(8);
            }
            ImageView imageView = this.mMobileType;
            if (this.mWifiVisible) {
                i = 8;
            }
            imageView.setVisibility(i);
        }
    }
}
