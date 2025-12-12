package org.godotengine.plugin.android.admob;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DroidAdMob extends GodotPlugin {

    private static final String TAG = "DroidAdMob";

    // Test Ad Unit IDs
    public static final String TEST_BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111";
    public static final String TEST_INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/1033173712";
    public static final String TEST_REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917";

    private AdView bannerAd;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private boolean isInitialized = false;
    private boolean testMode = false;

    // Consent management
    private ConsentInformation consentInformation;
    private ConsentForm consentForm;
    private boolean consentGathered = false;

    public DroidAdMob(Godot godot) {
        super(godot);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return BuildConfig.GODOT_PLUGIN_NAME;
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new HashSet<>();
        signals.add(new SignalInfo("ad_loaded"));
        signals.add(new SignalInfo("ad_failed_to_load", String.class));
        signals.add(new SignalInfo("ad_opened"));
        signals.add(new SignalInfo("ad_closed"));
        signals.add(new SignalInfo("ad_impression"));
        signals.add(new SignalInfo("ad_clicked"));
        signals.add(new SignalInfo("rewarded", String.class, Integer.class));
        signals.add(new SignalInfo("interstitial_loaded"));
        signals.add(new SignalInfo("interstitial_failed_to_load", String.class));
        signals.add(new SignalInfo("rewarded_ad_loaded"));
        signals.add(new SignalInfo("rewarded_ad_failed_to_load", String.class));
        signals.add(new SignalInfo("consent_info_updated"));
        signals.add(new SignalInfo("consent_info_update_failed", String.class));
        signals.add(new SignalInfo("consent_form_dismissed"));
        signals.add(new SignalInfo("consent_form_failed", String.class));
        signals.add(new SignalInfo("consent_status_changed", Integer.class));
        return signals;
    }

    /**
     * Request consent information update
     * This should be called before initializing ads, especially in EU/UK regions
     * @param isTestMode Whether to use test/debug mode for consent
     * @param testDeviceId Optional test device ID for consent debugging (empty string for production)
     */
    @UsedByGodot
    public void requestConsentInfoUpdate(final boolean isTestMode, final String testDeviceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) {
                    Log.e(TAG, "Activity is null during consent request");
                    emitSignal("consent_info_update_failed", "Activity is null");
                    return;
                }

                ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder();

                if (isTestMode && testDeviceId != null && !testDeviceId.isEmpty()) {
                    ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                            .addTestDeviceHashedId(testDeviceId)
                            .build();
                    paramsBuilder.setConsentDebugSettings(debugSettings);
                    Log.d(TAG, "Consent debug mode enabled for device: " + testDeviceId);
                }

                ConsentRequestParameters params = paramsBuilder.build();
                consentInformation = UserMessagingPlatform.getConsentInformation(activity);

                consentInformation.requestConsentInfoUpdate(
                    activity,
                    params,
                    new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                        @Override
                        public void onConsentInfoUpdateSuccess() {
                            Log.d(TAG, "Consent information updated. Status: " + consentInformation.getConsentStatus());
                            emitSignal("consent_info_updated");
                            emitSignal("consent_status_changed", consentInformation.getConsentStatus());
                        }
                    },
                    new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                        @Override
                        public void onConsentInfoUpdateFailure(@NonNull FormError formError) {
                            Log.e(TAG, "Consent info update failed: " + formError.getMessage());
                            emitSignal("consent_info_update_failed", formError.getMessage());
                        }
                    }
                );
            }
        });
    }

    /**
     * Load and show consent form if required
     * Should be called after requestConsentInfoUpdate succeeds
     */
    @UsedByGodot
    public void loadConsentForm() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) {
                    Log.e(TAG, "Activity is null during consent form load");
                    emitSignal("consent_form_failed", "Activity is null");
                    return;
                }

                if (consentInformation == null) {
                    Log.e(TAG, "Consent information not initialized. Call requestConsentInfoUpdate first");
                    emitSignal("consent_form_failed", "Consent information not initialized");
                    return;
                }

                UserMessagingPlatform.loadConsentForm(
                    activity,
                    new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
                        @Override
                        public void onConsentFormLoadSuccess(@NonNull ConsentForm form) {
                            consentForm = form;
                            Log.d(TAG, "Consent form loaded successfully");

                            // Show form if consent is required
                            if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                                showConsentForm();
                            } else {
                                Log.d(TAG, "Consent not required, status: " + consentInformation.getConsentStatus());
                                consentGathered = true;
                                emitSignal("consent_form_dismissed");
                            }
                        }
                    },
                    new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
                        @Override
                        public void onConsentFormLoadFailure(@NonNull FormError formError) {
                            Log.e(TAG, "Consent form load failed: " + formError.getMessage());
                            emitSignal("consent_form_failed", formError.getMessage());
                        }
                    }
                );
            }
        });
    }

    /**
     * Show the consent form to the user
     */
    private void showConsentForm() {
        Activity activity = getActivity();
        if (activity == null || consentForm == null) {
            return;
        }

        consentForm.show(activity, new ConsentForm.OnConsentFormDismissedListener() {
            @Override
            public void onConsentFormDismissed(FormError formError) {
                if (formError != null) {
                    Log.e(TAG, "Consent form dismissed with error: " + formError.getMessage());
                    emitSignal("consent_form_failed", formError.getMessage());
                } else {
                    Log.d(TAG, "Consent form dismissed by user");
                    consentGathered = true;
                    emitSignal("consent_form_dismissed");
                    emitSignal("consent_status_changed", consentInformation.getConsentStatus());
                }

                // Load a new form for next time
                consentForm = null;
            }
        });
    }

    /**
     * Get current consent status
     * @return Consent status: 0=UNKNOWN, 1=NOT_REQUIRED, 2=REQUIRED, 3=OBTAINED
     */
    @UsedByGodot
    public int getConsentStatus() {
        if (consentInformation == null) {
            return ConsentInformation.ConsentStatus.UNKNOWN;
        }
        return consentInformation.getConsentStatus();
    }

    /**
     * Check if privacy options are required (user can change consent)
     * @return true if privacy options entry point should be shown
     */
    @UsedByGodot
    public boolean isPrivacyOptionsRequired() {
        if (consentInformation == null) {
            return false;
        }
        return consentInformation.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    /**
     * Show privacy options form (allows user to update consent preferences)
     */
    @UsedByGodot
    public void showPrivacyOptionsForm() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) {
                    Log.e(TAG, "Activity is null");
                    emitSignal("consent_form_failed", "Activity is null");
                    return;
                }

                if (consentInformation == null) {
                    Log.e(TAG, "Consent information not initialized");
                    emitSignal("consent_form_failed", "Consent information not initialized");
                    return;
                }

                UserMessagingPlatform.showPrivacyOptionsForm(activity, new ConsentForm.OnConsentFormDismissedListener() {
                    @Override
                    public void onConsentFormDismissed(FormError formError) {
                        if (formError != null) {
                            Log.e(TAG, "Privacy options form error: " + formError.getMessage());
                            emitSignal("consent_form_failed", formError.getMessage());
                        } else {
                            Log.d(TAG, "Privacy options form dismissed");
                            emitSignal("consent_form_dismissed");
                            emitSignal("consent_status_changed", consentInformation.getConsentStatus());
                        }
                    }
                });
            }
        });
    }

    /**
     * Reset consent information (for testing purposes)
     */
    @UsedByGodot
    public void resetConsentInformation() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (consentInformation != null) {
                    consentInformation.reset();
                    consentGathered = false;
                    Log.d(TAG, "Consent information reset");
                }
            }
        });
    }

    /**
     * Initialize the Mobile Ads SDK
     * @param isTestMode Whether to use test ads or not
     */
    @UsedByGodot
    public void initialize(final boolean isTestMode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isInitialized) {
                    Log.w(TAG, "AdMob already initialized");
                    return;
                }

                testMode = isTestMode;

                if (testMode) {
                    // Set test device IDs for testing
                    RequestConfiguration configuration = new RequestConfiguration.Builder()
                            .setTestDeviceIds(Collections.singletonList("EMULATOR"))
                            .build();
                    MobileAds.setRequestConfiguration(configuration);
                    Log.d(TAG, "AdMob initialized in TEST mode");
                }

                Activity activity = getActivity();
                if (activity != null) {
                    MobileAds.initialize(activity, initializationStatus -> {
                        isInitialized = true;
                        Log.d(TAG, "AdMob initialized: " + initializationStatus.getAdapterStatusMap());
                    });
                } else {
                    Log.e(TAG, "Activity is null during MobileAds initialization");
                }
            }
        });
    }

    @UsedByGodot
    public String getTestBannerAdUnit() {
        return TEST_BANNER_AD_UNIT;
    }

    @UsedByGodot
    public String getTestInterstitialAdUnit() {
        return TEST_INTERSTITIAL_AD_UNIT;
    }

    @UsedByGodot
    public String getTestRewardedAdUnit() {
        return TEST_REWARDED_AD_UNIT;
    }

    @UsedByGodot
    public void loadBanner(final String adUnitId, final String position, final String size) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isInitialized) {
                    Log.e(TAG, "AdMob not initialized. Call initialize() first");
                    emitSignal("ad_failed_to_load", "AdMob not initialized");
                    return;
                }

                // Remove existing banner if any
                removeBanner();

                Activity activity = getActivity();
                if (activity == null) {
                    Log.e(TAG, "Activity is null during banner load");
                    return;
                }

                // Create new banner
                bannerAd = new AdView(activity);
                bannerAd.setAdUnitId(adUnitId);
                bannerAd.setAdSize(getAdSize(size));

                // Set up ad listener
                bannerAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        Log.d(TAG, "Banner ad loaded");
                        emitSignal("ad_loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Banner ad failed to load: " + loadAdError.getMessage());
                        emitSignal("ad_failed_to_load", loadAdError.getMessage());
                    }

                    @Override
                    public void onAdOpened() {
                        Log.d(TAG, "Banner ad opened");
                        emitSignal("ad_opened");
                    }

                    @Override
                    public void onAdClosed() {
                        Log.d(TAG, "Banner ad closed");
                        emitSignal("ad_closed");
                    }

                    @Override
                    public void onAdImpression() {
                        Log.d(TAG, "Banner ad impression recorded");
                        emitSignal("ad_impression");
                    }

                    @Override
                    public void onAdClicked() {
                        Log.d(TAG, "Banner ad clicked");
                        emitSignal("ad_clicked");
                    }
                });

                // Add banner to layout
                FrameLayout layout = activity.findViewById(android.R.id.content);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
                params.gravity = position.toLowerCase().equals("top") ? Gravity.TOP : Gravity.BOTTOM;
                layout.addView(bannerAd, params);

                // Load the ad
                bannerAd.loadAd(new AdRequest.Builder().build());
                Log.d(TAG, "Loading banner ad with ID: " + adUnitId);
            }
        });
    }

    @UsedByGodot
    public void removeBanner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bannerAd != null) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        FrameLayout layout = activity.findViewById(android.R.id.content);
                        layout.removeView(bannerAd);
                    }
                    bannerAd.destroy();
                    bannerAd = null;
                    Log.d(TAG, "Banner ad removed");
                }
            }
        });
    }

    @UsedByGodot
    public void hideBanner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bannerAd != null) {
                    bannerAd.setVisibility(View.GONE);
                    Log.d(TAG, "Banner ad hidden");
                }
            }
        });
    }

    @UsedByGodot
    public void showBanner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bannerAd != null) {
                    bannerAd.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Banner ad shown");
                }
            }
        });
    }

    @UsedByGodot
    public void loadInterstitial(final String adUnitId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isInitialized) {
                    Log.e(TAG, "AdMob not initialized. Call initialize() first");
                    emitSignal("interstitial_failed_to_load", "AdMob not initialized");
                    return;
                }

                Activity activity = getActivity();
                if (activity == null) {
                    Log.e(TAG, "Activity is null during interstitial load");
                    return;
                }

                AdRequest adRequest = new AdRequest.Builder().build();

                InterstitialAd.load(activity, adUnitId, adRequest, new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        Log.d(TAG, "Interstitial ad loaded");
                        emitSignal("interstitial_loaded");

                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Interstitial ad dismissed");
                                interstitialAd = null;
                                emitSignal("ad_closed");
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                                interstitialAd = null;
                                emitSignal("ad_failed_to_load", adError.getMessage());
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                Log.d(TAG, "Interstitial ad showed");
                                emitSignal("ad_opened");
                            }

                            @Override
                            public void onAdImpression() {
                                Log.d(TAG, "Interstitial ad impression recorded");
                                emitSignal("ad_impression");
                            }

                            @Override
                            public void onAdClicked() {
                                Log.d(TAG, "Interstitial ad clicked");
                                emitSignal("ad_clicked");
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        interstitialAd = null;
                        emitSignal("interstitial_failed_to_load", loadAdError.getMessage());
                    }
                });

                Log.d(TAG, "Loading interstitial ad with ID: " + adUnitId);
            }
        });
    }

    @UsedByGodot
    public void showInterstitial() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) {
                    Log.e(TAG, "Activity is null during interstitial show");
                    return;
                }

                if (interstitialAd != null) {
                    interstitialAd.show(activity);
                    Log.d(TAG, "Showing interstitial ad");
                } else {
                    Log.e(TAG, "Interstitial ad not ready");
                    emitSignal("ad_failed_to_load", "Interstitial ad not loaded");
                }
            }
        });
    }

    @UsedByGodot
    public boolean isInterstitialLoaded() {
        return interstitialAd != null;
    }

    @UsedByGodot
    public void loadRewarded(final String adUnitId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isInitialized) {
                    Log.e(TAG, "AdMob not initialized. Call initialize() first");
                    emitSignal("rewarded_ad_failed_to_load", "AdMob not initialized");
                    return;
                }

                Activity activity = getActivity();
                if (activity == null) {
                    Log.e(TAG, "Activity is null during rewarded load");
                    return;
                }

                AdRequest adRequest = new AdRequest.Builder().build();

                RewardedAd.load(activity, adUnitId, adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d(TAG, "Rewarded ad loaded");
                        emitSignal("rewarded_ad_loaded");

                        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Rewarded ad dismissed");
                                rewardedAd = null;
                                emitSignal("ad_closed");
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                Log.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                                rewardedAd = null;
                                emitSignal("ad_failed_to_load", adError.getMessage());
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                Log.d(TAG, "Rewarded ad showed");
                                emitSignal("ad_opened");
                            }

                            @Override
                            public void onAdImpression() {
                                Log.d(TAG, "Rewarded ad impression recorded");
                                emitSignal("ad_impression");
                            }

                            @Override
                            public void onAdClicked() {
                                Log.d(TAG, "Rewarded ad clicked");
                                emitSignal("ad_clicked");
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                        rewardedAd = null;
                        emitSignal("rewarded_ad_failed_to_load", loadAdError.getMessage());
                    }
                });

                Log.d(TAG, "Loading rewarded ad with ID: " + adUnitId);
            }
        });
    }

    @UsedByGodot
    public void showRewarded() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) {
                    Log.e(TAG, "Activity is null during rewarded show");
                    return;
                }

                if (rewardedAd != null) {
                    rewardedAd.show(activity, rewardItem -> {
                        int rewardAmount = rewardItem.getAmount();
                        String rewardType = rewardItem.getType();
                        Log.d(TAG, "User earned reward: " + rewardAmount + " " + rewardType);
                        emitSignal("rewarded", rewardType, rewardAmount);
                    });
                    Log.d(TAG, "Showing rewarded ad");
                } else {
                    Log.e(TAG, "Rewarded ad not ready");
                    emitSignal("ad_failed_to_load", "Rewarded ad not loaded");
                }
            }
        });
    }

    @UsedByGodot
    public boolean isRewardedLoaded() {
        return rewardedAd != null;
    }

    private AdSize getAdSize(String size) {
        switch (size.toLowerCase()) {
            case "large_banner":
                return AdSize.LARGE_BANNER;
            case "medium_rectangle":
                return AdSize.MEDIUM_RECTANGLE;
            case "full_banner":
                return AdSize.FULL_BANNER;
            case "leaderboard":
                return AdSize.LEADERBOARD;
            default:
                return AdSize.BANNER;
        }
    }

    @Override
    public void onMainDestroy() {
        super.onMainDestroy();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bannerAd != null) {
                    bannerAd.destroy();
                    bannerAd = null;
                }
                interstitialAd = null;
                rewardedAd = null;
            }
        });
    }
}

