package com.google.ads.mediation.imobile;

import static com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import java.util.ArrayList;
import jp.co.imobile.sdkads.android.AdMobMediationSupportAdSize;
import jp.co.imobile.sdkads.android.FailNotificationReason;
import jp.co.imobile.sdkads.android.ImobileSdkAd;
import jp.co.imobile.sdkads.android.ImobileSdkAdListener;

/**
 * i-mobile mediation adapter for AdMob banner and interstitial ads.
 */
public final class IMobileAdapter implements MediationBannerAdapter, MediationInterstitialAdapter {

  // region - Fields for log.
  /**
   * Tag for log.
   */
  private static final String TAG = IMobileAdapter.class.getSimpleName();
  // endregion

  // region - Fields for banner ads.
  /**
   * Listener for banner ads.
   */
  private MediationBannerListener mediationBannerListener;

  /**
   * View to display banner ads.
   */
  private ViewGroup bannerView;

  /**
   * Supported ad sizes.
   */
  private static final ArrayList<AdSize> supportedSizes;

  static {
    // Initialize static fields.
    AdMobMediationSupportAdSize[] iMobileAdSizes = AdMobMediationSupportAdSize.values();
    supportedSizes = new ArrayList<>();
    for (AdMobMediationSupportAdSize adSize : iMobileAdSizes) {
      supportedSizes.add(new AdSize(adSize.getWidth(), adSize.getHeight()));
    }
  }
  // endregion

  // region - Methods for banner ads.
  @Override
  public void requestBannerAd(@NonNull Context context, @NonNull MediationBannerListener listener,
      @NonNull Bundle serverParameters, @NonNull AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {

    // Validate Context.
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, "Context is not an Activity.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(this, error);
      return;
    }

    // Validate AdSize.
    AdSize supportedAdSize = MediationUtils.findClosestSize(context, adSize, supportedSizes);
    if (supportedAdSize == null) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Ad size " + adSize + " is not supported.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(this, error);
      return;
    }

    // Initialize fields.
    mediationBannerListener = listener;

    // Get parameters for i-mobile SDK.
    String publisherId = serverParameters.getString(Constants.KEY_PUBLISHER_ID);
    String mediaId = serverParameters.getString(Constants.KEY_MEDIA_ID);
    String spotId = serverParameters.getString(Constants.KEY_SPOT_ID);

    Activity activity = (Activity) context;

    // Call i-mobile SDK.
    Log.d(TAG, "Requesting banner with ad size: " + adSize);
    ImobileSdkAd.registerSpotInline(activity, publisherId, mediaId, spotId);
    ImobileSdkAd.start(spotId);
    ImobileSdkAd.setImobileSdkAdListener(
        spotId,
        new ImobileSdkAdListener() {
          @Override
          public void onAdReadyCompleted() {
            if (mediationBannerListener != null) {
              mediationBannerListener.onAdLoaded(IMobileAdapter.this);
            }
          }

          @Override
          public void onAdCliclkCompleted() {
            if (mediationBannerListener != null) {
              mediationBannerListener.onAdClicked(IMobileAdapter.this);
              mediationBannerListener.onAdOpened(IMobileAdapter.this);
              mediationBannerListener.onAdLeftApplication(IMobileAdapter.this);
            }
          }

          @Override
          public void onDismissAdScreen() {
            if (mediationBannerListener != null) {
              mediationBannerListener.onAdClosed(IMobileAdapter.this);
            }
          }

          @Override
          public void onFailed(FailNotificationReason reason) {
            AdError error = AdapterHelper.getAdError(reason);
            Log.w(TAG, error.getMessage());
            if (mediationBannerListener != null) {
              mediationBannerListener.onAdFailedToLoad(IMobileAdapter.this, error);
            }
          }
        });

    // Create view to display banner ads.
    bannerView = new FrameLayout(activity);
    float scaleRatio =
        canScale(supportedAdSize) ? calcScaleRatio(activity, adSize, supportedAdSize) : 1.0f;
    bannerView.setLayoutParams(
        new FrameLayout.LayoutParams(
            (int) (supportedAdSize.getWidthInPixels(activity) * scaleRatio),
            (int) (supportedAdSize.getHeightInPixels(activity) * scaleRatio)));
    ImobileSdkAd.showAdForAdMobMediation(activity, spotId, bannerView, scaleRatio);
  }

  private boolean canScale(AdSize iMobileAdSize) {
    return iMobileAdSize.getWidth() == 320
        && (iMobileAdSize.getHeight() == 50 || iMobileAdSize.getHeight() == 100);
  }

  private float calcScaleRatio(Context context, AdSize requestedAdSize, AdSize iMobileAdSize) {
    return Math.min(
        ((float) requestedAdSize.getWidthInPixels(context)
            / iMobileAdSize.getWidthInPixels(context)),
        ((float) requestedAdSize.getHeightInPixels(context)
            / iMobileAdSize.getHeightInPixels(context)));
  }

  @NonNull
  @Override
  public View getBannerView() {
    return bannerView;
  }
  // endregion

  // region - Fields for interstitial ads.
  /**
   * Listener for interstitial ads.
   */
  private MediationInterstitialListener mediationInterstitialListener;

  /**
   * Activity to display interstitial ads.
   */
  private Activity interstitialActivity;

  /**
   * i-mobile spot ID.
   */
  private String interstitialSpotId;
  // endregion

  // region - Methods for interstitial ads.
  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener listener, @NonNull Bundle serverParameters,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {

    // Validate Context.
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Context is not an Activity.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(this, error);
      return;
    }
    interstitialActivity = (Activity) context;

    // Initialize fields.
    mediationInterstitialListener = listener;

    // Get parameters for i-mobile SDK.
    String publisherId = serverParameters.getString(Constants.KEY_PUBLISHER_ID);
    String mediaId = serverParameters.getString(Constants.KEY_MEDIA_ID);
    interstitialSpotId = serverParameters.getString(Constants.KEY_SPOT_ID);

    // Call i-mobile SDK.
    ImobileSdkAd.registerSpotFullScreen(
        interstitialActivity, publisherId, mediaId, interstitialSpotId);
    ImobileSdkAd.setImobileSdkAdListener(
        interstitialSpotId,
        new ImobileSdkAdListener() {
          @Override
          public void onAdReadyCompleted() {
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdLoaded(IMobileAdapter.this);
            }
          }

          @Override
          public void onAdShowCompleted() {
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdOpened(IMobileAdapter.this);
            }
          }

          @Override
          public void onAdCliclkCompleted() {
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdClicked(IMobileAdapter.this);
              mediationInterstitialListener.onAdLeftApplication(IMobileAdapter.this);
            }
          }

          @Override
          public void onAdCloseCompleted() {
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdClosed(IMobileAdapter.this);
            }
          }

          @Override
          public void onFailed(FailNotificationReason reason) {
            AdError error = AdapterHelper.getAdError(reason);
            Log.w(TAG, error.getMessage());
            if (mediationBannerListener != null) {
              mediationBannerListener.onAdFailedToLoad(
                  IMobileAdapter.this, error);
            }
          }
        });

    // Start getting ads.
    if (ImobileSdkAd.isShowAd(interstitialSpotId)) {
      mediationInterstitialListener.onAdLoaded(IMobileAdapter.this);
    } else {
      ImobileSdkAd.start(interstitialSpotId);
    }
  }

  @Override
  public void showInterstitial() {
    // Show ad.
    if (interstitialActivity != null
        && interstitialActivity.hasWindowFocus()
        && interstitialSpotId != null) {
      ImobileSdkAd.showAdforce(interstitialActivity, interstitialSpotId);
    }
  }
  // endregion

  // region - Methods of life cycle.
  @Override
  public void onDestroy() {
    // Release objects.
    mediationBannerListener = null;
    bannerView = null;
    mediationInterstitialListener = null;
    interstitialActivity = null;
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }
  // endregion

}