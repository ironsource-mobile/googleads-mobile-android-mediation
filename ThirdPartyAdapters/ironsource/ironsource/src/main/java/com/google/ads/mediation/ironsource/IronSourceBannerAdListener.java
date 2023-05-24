package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN;
import static com.ironsource.mediationsdk.logger.IronSourceError.ERROR_DO_BN_LOAD_ALREADY_IN_PROGRESS;
import static com.ironsource.mediationsdk.logger.IronSourceError.ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerListener;
import com.ironsource.mediationsdk.logger.IronSourceError;

public class IronSourceBannerAdListener implements ISDemandOnlyBannerListener {

  public void onBannerAdLoaded(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource Banner ad loaded for instance ID: %s", instanceId));
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);

    if (ironSourceBannerAd != null) {
      if (ironSourceBannerAd.getIronSourceAdView() != null) {
        ironSourceBannerAd
            .getIronSourceAdView()
            .addView(ironSourceBannerAd.getIronSourceBannerLayout());
      }

      if (ironSourceBannerAd.getAdLoadCallback() != null) {
        ironSourceBannerAd.setBannerAdCallback(
            ironSourceBannerAd.getAdLoadCallback().onSuccess(ironSourceBannerAd));
      }
    }
  }

  public void onBannerAdLoadFailed(
      @NonNull final String instanceId, @NonNull final IronSourceError ironSourceError) {
    final AdError loadError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    String errorMessage =
        String.format(
            "IronSource failed to load Banner ad for instance ID: %s. Error: %s",
            instanceId, loadError.getMessage());
    Log.w(TAG, errorMessage);
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);

    if (ironSourceBannerAd != null) {
      MediationAdLoadCallback adLoadCallback = ironSourceBannerAd.getAdLoadCallback();
      if (adLoadCallback != null) {
        adLoadCallback.onFailure(loadError);
      }

      if (ironSourceError.getErrorCode() != ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS
          && ironSourceError.getErrorCode() != ERROR_DO_BN_LOAD_ALREADY_IN_PROGRESS) {
        IronSourceBannerAd.removeFromAvailableInstances(instanceId);
      }
    }
  }

  public void onBannerAdShown(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource Banner AdShown for instance ID: %s", instanceId));
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);

    if (ironSourceBannerAd != null) {
      MediationBannerAdCallback adCallback = ironSourceBannerAd.getBannerAdCallback();
      if (adCallback != null) {
        adCallback.reportAdImpression();
      }
    }

    // Remove from available instances and destroy all other instances
    IronSourceBannerAd.clearAllAvailableInstancesExceptOne(instanceId);
  }

  public void onBannerAdClicked(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource Banner ad clicked for instance ID: %s", instanceId));
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);

    if (ironSourceBannerAd != null) {
      MediationBannerAdCallback adCallback = ironSourceBannerAd.getBannerAdCallback();
      if (adCallback != null) {
        adCallback.onAdOpened();
        adCallback.reportAdClicked();
      }
    }
  }

  public void onBannerAdLeftApplication(@NonNull String instanceId) {
    Log.d(
        TAG,
        String.format(
            "IronSource Banner ad onBanner Ad Left Application for instance ID: %s", instanceId));
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);

    if (ironSourceBannerAd != null) {
      MediationBannerAdCallback adCallback = ironSourceBannerAd.getBannerAdCallback();
      if (adCallback != null) {
        adCallback.onAdLeftApplication();
      }
    }
  }
}
