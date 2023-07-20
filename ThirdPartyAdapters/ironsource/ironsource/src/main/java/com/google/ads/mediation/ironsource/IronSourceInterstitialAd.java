// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceConstants.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceConstants.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceConstants.TAG;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_REQUIRES_ACTIVITY_CONTEXT;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.ironsource.mediationsdk.IronSource;
import java.util.concurrent.ConcurrentHashMap;

public class IronSourceInterstitialAd implements MediationInterstitialAd {

  private static final ConcurrentHashMap<String, IronSourceInterstitialAd>
      availableInterstitialInstances = new ConcurrentHashMap<>();

  private static final IronSourceInterstitialAdListener ironSourceInterstitialListener =
      new IronSourceInterstitialAdListener();

  /**
   * Mediation listener used to forward interstitial ad events from IronSource SDK to Google Mobile
   * Ads SDK while ad is presented.
   */
  private MediationInterstitialAdCallback interstitialAdCallback;

  /**
   * Mediation listener used to forward interstitial ad events from IronSource SDK to Google Mobile
   * Ads SDK for loading phases of the ad.
   */
  public MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      mediationInterstitialAdLoadCallback;

  /** IronSource interstitial context. */
  private final Context context;

  /** IronSource interstitial instance ID. */
  private final String instanceID;

  public IronSourceInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration interstitialAdConfig,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationInterstitialAdLoadCallback) {
    Bundle serverParameters = interstitialAdConfig.getServerParameters();
    instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
    context = interstitialAdConfig.getContext();
    this.mediationInterstitialAdLoadCallback = mediationInterstitialAdLoadCallback;
  }

  /** Getters and Setters. */
  public static IronSourceInterstitialAd getFromAvailableInstances(String instanceId) {
    return availableInterstitialInstances.get(instanceId);
  }

  public static void removeFromAvailableInstances(String instanceId) {
    availableInterstitialInstances.remove(instanceId);
  }

  public static IronSourceInterstitialAdListener getIronSourceInterstitialListener() {
    return ironSourceInterstitialListener;
  }

  public MediationInterstitialAdCallback getInterstitialAdCallback() {
    return interstitialAdCallback;
  }

  public void setInterstitialAdCallback(MediationInterstitialAdCallback adCallback) {
    interstitialAdCallback = adCallback;
  }
  
  public void loadInterstitial() {
    if (!isParamsValid()) {
      return;
    }

    Activity activity = (Activity) context;
    availableInterstitialInstances.put(instanceID, this);
    Log.d(
        TAG, String.format("Loading IronSource interstitial ad with instance ID: %s", instanceID));
    IronSource.loadISDemandOnlyInterstitial(activity, instanceID);
  }

  /** Checks if the parameters for loading this instance are valid. */
  private boolean isParamsValid() {
    // Check that the context is an Activity.
    AdError loadError = IronSourceAdapterUtils.checkContextIsActivity(context);
    if (loadError != null) {
      onAdFailedToLoad(loadError);
      return false;
    }

    // Check that the instance ID is valid.
    loadError = IronSourceAdapterUtils.checkInstanceId(instanceID);
    if (loadError != null) {
      onAdFailedToLoad(loadError);
      return false;
    }

    // Check that an Ad for this instance ID is not already loading.
    if (!canLoadInterstitialInstance(instanceID)) {
      String errorMessage =
          String.format(
              "An IronSource interstitial ad is already loading for instance ID: %s", instanceID);
      AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ERROR_DOMAIN);
      onAdFailedToLoad(concurrentError);
      return false;
    }

    return true;
  }

  private boolean canLoadInterstitialInstance(@NonNull String instanceId) {
    IronSourceInterstitialAd ironSourceInterstitialAd =
        availableInterstitialInstances.get(instanceId);
    return (ironSourceInterstitialAd == null);
  }

  /** Interstitial show Ad. */
  @Override
  public void showAd(@NonNull Context context) {
    IronSource.showISDemandOnlyInterstitial(instanceID);
  }

  /** Pass Load Fail from IronSource SDK to Google Mobile Ads. */
  private void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.e(TAG, loadError.getMessage());
    if (mediationInterstitialAdLoadCallback != null) {
      mediationInterstitialAdLoadCallback.onFailure(loadError);
    }
  }
}