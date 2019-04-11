package com.google.ads.mediation.ironsource;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;
import com.ironsource.mediationsdk.utils.IronSourceUtils;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_APP_KEY;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.MEDIATION_NAME;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;

public class IronSourceMediationAdapter extends Adapter
        implements MediationRewardedAd, ISDemandOnlyRewardedVideoListener {

    private static AtomicBoolean mDidInitRewardedVideo = new AtomicBoolean(false);
    /**
     * Mediation rewarded ad listener used to forward rewarded ad events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationRewardedAdCallback mMediationRewardedAdCallback;
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mMediationAdLoadCallback;
    /**
     * This is the id of the rewarded video instance requested.
     */
    private String mInstanceID;

    /**
     * MediationRewardedAd implementation.
     */
    @Override
    public VersionInfo getSDKVersionInfo() {
        String sdkVersion = IronSourceUtils.getSDKVersion();
        String splits[] = sdkVersion.split("\\.");
        int major = 0;
        int minor = 0;
        int micro = 0;
        if (splits.length > 2) {
            major = Integer.parseInt(splits[0]);
            minor = Integer.parseInt(splits[1]);
            micro = Integer.parseInt(splits[2]);
        } else if (splits.length == 2) {
            major = Integer.parseInt(splits[0]);
            minor = Integer.parseInt(splits[1]);
        } else if (splits.length == 1) {
            major = Integer.parseInt(splits[0]);
        }
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        // Adapter versions have 2 patch versions. Multiply the first patch by 100.
        int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public void initialize(Context context,
                           InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {

        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            initializationCompleteCallback.onInitializationFailed("IronSource SDK requires " +
                    "an Activity context to initialize");
            return;
        }

        try {
            HashSet<String> appKeys = new HashSet<>();
            for (MediationConfiguration configuration : mediationConfigurations) {
                Bundle serverParameters = configuration.getServerParameters();
                String appKeyFromServer =
                        serverParameters.getString(IronSourceAdapterUtils.KEY_APP_KEY);

                if (!TextUtils.isEmpty(appKeyFromServer)) {
                    appKeys.add(appKeyFromServer);
                }
            }

            String appKey = "";
            int count = appKeys.size();
            if (count > 0) {
                appKey = appKeys.iterator().next();

                if (count > 1) {
                    String message = String.format("Multiple '%s' entries found: %s. " +
                                    "Using '%s' to initialize the IronSource SDK.",
                            IronSourceAdapterUtils.KEY_APP_KEY, appKeys.toString(), appKey);
                    Log.w(TAG, message);
                }
            }

            if (TextUtils.isEmpty(appKey)) {
                initializationCompleteCallback.onInitializationFailed(
                        "IronSource initialization Failed: Missing or Invalid App Key.");
                return;
            }

            IronSource.initISDemandOnly((Activity) context, appKey,
                    IronSource.AD_UNIT.REWARDED_VIDEO);
            initializationCompleteCallback.onInitializationSucceeded();
        } catch (Exception e) {
            initializationCompleteCallback.onInitializationFailed(
                    "IronSource initialization failed, " + e.getMessage());
        }
    }

    @Override
    public void loadRewardedAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
                               MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {

        mMediationAdLoadCallback = mediationAdLoadCallback;
        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        // Used for logging purposes
        String message;
        if (serverParameters == null) {
            message = "IronSource isn't initialized, server parameters are null";
            mMediationAdLoadCallback.onFailure(message);
            return;
        }

        this.mInstanceID = serverParameters.getString(IronSourceAdapterUtils.KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
        if (!mDidInitRewardedVideo.getAndSet(true)) {
            try {
                Context context = mediationRewardedAdConfiguration.getContext();
                if (!(context instanceof Activity)) {
                    // Context not an Activity context, log the reason for failure and fail the
                    // initialization.
                    mediationAdLoadCallback.onFailure("IronSource SDK requires an Activity context " +
                            "to initialize");
                    return;
                }

                String appKey = serverParameters.getString(KEY_APP_KEY);
                if (TextUtils.isEmpty(appKey)) {
                    message = "IronSource initialization failed, make sure that 'appKey'" +
                            " server parameter is added";
                    mediationAdLoadCallback.onFailure(message);
                    return;
                }

                IronSource.setISDemandOnlyRewardedVideoListener(this);
                IronSource.setMediationType(MEDIATION_NAME);
                Log.d(TAG, "IronSource Rewarded Video initialization called");
                IronSource.initISDemandOnly((Activity) context, appKey, IronSource.AD_UNIT.REWARDED_VIDEO);
                mMediationRewardedAdCallback = mMediationAdLoadCallback.onSuccess(IronSourceMediationAdapter.this);
                Log.d(TAG, String.format("IronSource loadRewardedVideo called with instanceId: %s", this.mInstanceID));
                IronSource.loadISDemandOnlyRewardedVideo(this.mInstanceID);

            } catch (Exception e) {
                Log.w(TAG, "IronSource Initialization failed for Rewarded Video", e);
                mMediationAdLoadCallback.onFailure(
                        "IronSource initialization failed for Rewarded Video: " + e.getMessage());
            }
        } else {
            Log.d(TAG, String.format("IronSource loadRewardedVideo called with instanceId: %s", this.mInstanceID));
            IronSource.loadISDemandOnlyRewardedVideo(this.mInstanceID);
        }

    }

    @Override
    public void showAd(Context context) {
        Log.d(TAG, String.format("IronSource showAd called with instanceId: %s", this.mInstanceID));
        IronSource.showISDemandOnlyRewardedVideo(this.mInstanceID);
    }

    /**
     * IronSource RewardedVideoListener implementation.
     */

    @Override
    public void onRewardedVideoAdLoadSuccess(String instanceId) {
        Log.d(TAG, String.format("IronSource load success for instanceId: %s  (current instance is: %s)", instanceId, this.mInstanceID));
        mMediationRewardedAdCallback = mMediationAdLoadCallback.onSuccess(IronSourceMediationAdapter.this);
    }

    @Override
    public void onRewardedVideoAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        final String message = String.format("IronSource Rewarded Video failed to load for instance %s (current instance is: %s) with Error: %s",
                instanceId, this.mInstanceID, ironSourceError.getErrorMessage());
        Log.d(TAG, message);
        mMediationAdLoadCallback.onFailure(message);
    }

    @Override
    public void onRewardedVideoAdOpened(final String instanceId) {
        Log.d(IronSourceAdapterUtils.TAG, String.format("IronSource Rewarded Video opened ad for instance %s (current instance is: %s)",
                instanceId, this.mInstanceID));

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.onAdOpened();
                    mMediationRewardedAdCallback.onVideoStart();
                    mMediationRewardedAdCallback.reportAdImpression();
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        Log.d(IronSourceAdapterUtils.TAG, String.format("IronSource Rewarded Video closed ad for instance %s  (current instance is: %s)",
                instanceId, this.mInstanceID));

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.onAdClosed();
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdRewarded(String instanceId) {
        final IronSourceReward reward = new IronSourceReward();
        Log.d(IronSourceAdapterUtils.TAG, String.format("IronSource Rewarded Video received reward: %d %s, for instance %s (current instance is: %s)",
                reward.getAmount(), reward.getType(), instanceId, this.mInstanceID));

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.onVideoComplete();
                    mMediationRewardedAdCallback.onUserEarnedReward(reward);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdShowFailed(final String instanceId, IronSourceError ironsourceError) {
        final String message = String.format("IronSource Rewarded Video failed to show for instance %s (current instance is: %s) with Error: %s",
                instanceId, this.mInstanceID, ironsourceError.getErrorMessage());
        Log.w(IronSourceAdapterUtils.TAG, message);

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.onAdFailedToShow(message);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdClicked(String instanceId) {
        Log.d(IronSourceAdapterUtils.TAG, String.format("IronSource Rewarded Video clicked for instance %s (current instance is: %s)",
                instanceId, this.mInstanceID));

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.reportAdClicked();
                }
            });
        }
    }

    /**
     * A {@link RewardItem} used to map IronSource reward to Google's reward.
     */
    class IronSourceReward implements RewardItem {

        @Override
        public String getType() {
            return "";
        }

        @Override
        public int getAmount() {
            return 1;
        }
    }

}
