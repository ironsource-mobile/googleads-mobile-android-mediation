package com.google.ads.mediation.ironsource;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyInterstitialListener;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.ADAPTER_VERSION_NAME;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.MEDIATION_NAME;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;

/**
 * A {@link MediationInterstitialAdapter} to load and show IronSource interstitial ads using Google
 * Mobile Ads SDK mediation.
 */
public class IronSourceAdapter
        implements MediationInterstitialAdapter, ISDemandOnlyInterstitialListener {

    /**
     * Mediation interstitial ad listener used to forward interstitial events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mInterstitialListener;

    /**
     * This is the id of the instance to be shown.
     */
    private String mInstanceID;

    private static AtomicBoolean mDidInitInterstitial = new AtomicBoolean(false);

    //region MediationInterstitialAdapter implementation.
    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {

        mInterstitialListener = listener;

        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            Log.e(TAG, "IronSource SDK requires an Activity context to initialize");
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        try {
            String appKey = serverParameters.getString(IronSourceAdapterUtils.KEY_APP_KEY);
            if (TextUtils.isEmpty(appKey)) {
                Log.e(TAG, String.format("IronSource initialization failed, make sure that the '%s' server parameter is added"
                        ,IronSourceAdapterUtils.KEY_APP_KEY));
                onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }

            this.mInstanceID = serverParameters.getString(IronSourceAdapterUtils.KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);

            IronSource.setISDemandOnlyInterstitialListener(this);
            if (!mDidInitInterstitial.getAndSet(true)) {
                IronSource.setMediationType(MEDIATION_NAME + ADAPTER_VERSION_NAME);
                Log.d(TAG,String.format("Init IronSource interstitial ad for instance: %s", this.mInstanceID));
                IronSource.initISDemandOnly((Activity) context, appKey, IronSource.AD_UNIT.INTERSTITIAL);
            }

            Log.d(TAG, String.format("Load IronSource interstitial ad for instance: %s", this.mInstanceID));
            IronSource.loadISDemandOnlyInterstitial(this.mInstanceID);

        } catch (Exception e) {
            Log.e(TAG, String.format("IronSource initialization failed with Error: %s", e.getMessage()));
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public void showInterstitial() {
        Log.d(TAG, String.format("IronSource showInterstitial for instance: %s", this.mInstanceID));
        IronSource.showISDemandOnlyInterstitial(this.mInstanceID);
    }
    //endregion

    @Override
    public void onDestroy() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    /**
     * Private IronSource methods
     */
    private void onISAdFailedToLoad(final int errorCode) {
        Log.e(TAG, String.format("IronSource Interstitial failed to load for instance %s, Error: %d"
                , this.mInstanceID, errorCode));

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdFailedToLoad(IronSourceAdapter.this, errorCode);
                }
            });
        }
    }

    //region ISDemandOnlyInterstitialListener implementation.
    @Override
    public void onInterstitialAdReady(String instanceId) {
        Log.d(TAG, String.format("IronSource Interstitial loaded successfully for instance %s (current instance is %s )"
                ,instanceId ,this.mInstanceID));

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdLoaded(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        Log.e(TAG, String.format("IronSource Interstitial failed to load for instance %s (current instance is %s ) with Error: %s"
                , instanceId, this.mInstanceID, ironSourceError.getErrorMessage()));

        // We only listen to a registered instance.
        if (!this.mInstanceID.equals(instanceId))
            return;

        onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
    }

    @Override
    public void onInterstitialAdOpened(String instanceId) {
        Log.d(TAG, String.format("IronSource Interstitial opened ad for instance %s (current instance is %s )",
                instanceId, this.mInstanceID));

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdOpened(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdClosed(String instanceId) {
        Log.d(TAG, String.format("IronSource Interstitial closed ad for instance %s (current instance is %s )",
                instanceId, this.mInstanceID));

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdClosed(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        Log.e(TAG, String.format("IronSource Interstitial failed to show " +
                "for instance %s(current instance is %s ) with Error: %s",instanceId, this.mInstanceID, ironSourceError.getErrorMessage()));
        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onAdOpened(IronSourceAdapter.this);
                    mInterstitialListener.onAdClosed(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdClicked(String instanceId) {
        Log.d(TAG, String.format("IronSource Interstitial ad clicked for instance %s (current instance is %s )",
                instanceId, this.mInstanceID));

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdClicked(IronSourceAdapter.this);
                    mInterstitialListener.onAdLeftApplication(IronSourceAdapter.this);
                }
            });
        }
    }
    //endregion
}
