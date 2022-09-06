package com.google.ads.mediation.mintegral;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.mbridge.msdk.MBridgeConstans;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MintegralConstants {
  public static final String APP_ID = "app_id";
  public static final String APP_KEY = "app_key";
  public static final String AD_UNIT_ID = "ad_unit_id";
  public static final String PLACEMENT_ID = "placement_id";
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.mintegral";
  public static final String MINTEGRAL_SDK_ERROR_DOMAIN = "com.mbridge.msdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
          value = {STATUS_GDPR_OR_CCPA_CONSENTS,
                  STATUS_GDPR_OR_CCPA_NOT_CONSENTS,
          })
  public @interface MBridgeConstants {

  }

  /**
   * user consents GDPR
   */
  public static final int STATUS_GDPR_OR_CCPA_CONSENTS = MBridgeConstans.IS_SWITCH_ON;

  /**
   * user not consents GDPR
   */
  public static final int STATUS_GDPR_OR_CCPA_NOT_CONSENTS = MBridgeConstans.IS_SWITCH_OFF;


  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
          value = {ERROR_INVALID_SERVER_PARAMETERS,
                  ERROR_BANNER_SIZE_MISMATCH,
                  ERROR_INVALID_BID_RESPONSE,
                  ERROR_MINTEGRAL_SDK,
                  ERROR_CODE_NO_FILL,
          })
  public @interface AdapterError {

  }


  /**
   * The Mintegral SDK returned a failure callback.
   */
  public static final int ERROR_MINTEGRAL_SDK = 100;

  /**
   * Invalid server parameters (e.g. Missing app ID or placement ID).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Mintegral supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Missing or invalid bid response.
   */
  public static final int ERROR_INVALID_BID_RESPONSE = 103;


  /**
   * Mintegral sdk ad no fill
   */
  public static final int ERROR_CODE_NO_FILL = 104;


  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode,
                                           @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
  }

  @NonNull
  public static AdError createSdkError(@NonNull String errorMessage) {
    return new AdError(ERROR_MINTEGRAL_SDK, errorMessage, MINTEGRAL_SDK_ERROR_DOMAIN);
  }
}
