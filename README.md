# DroidAdMob for Godot

A comprehensive AdMob plugin for Godot 4.x that supports Banner Ads, Interstitial Ads, and Rewarded Video Ads.

## Features

- ✅ Banner Ads (multiple sizes and positions)
- ✅ Interstitial Ads
- ✅ Rewarded Video Ads
- ✅ GDPR Consent Management (EU/UK compliance)
- ✅ Test mode for development
- ✅ Easy GDScript API
- ✅ Configurable AdMob App ID

---

## 🔧 Building the Plugin (For Plugin Developers)

### Prerequisites

- Java 17 (JDK 17)
- Android SDK
- Gradle 8.1+

### Quick Build

```bash
# Build the plugin
./gradlew clean assemble
```

The built plugin will be available at: `plugin/demo/addons/DroidAdMob/`

### Configure AdMob App ID (Optional)

By default, the plugin uses Google's test AdMob App ID. To use your own:

1. Edit `gradle.properties`
2. Update this line:
   ```properties
   admob.app.id=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
   ```
3. Rebuild: `./gradlew clean assemble`

Get your App ID from [AdMob Console](https://apps.admob.com/)

---

## 🎮 Using the Plugin in Your Godot Game

### 1. Installation

Copy the plugin to your Godot project:

```
YourGodotProject/
└── addons/
    └── DroidAdMob/
        ├── bin/
        │   ├── debug/DroidAdMob-debug.aar
        │   └── release/DroidAdMob-release.aar
        ├── plugin.cfg
        ├── export_plugin.gd
        └── admob.gd
```

### 2. Enable the Plugin

1. In Godot Editor: `Project` → `Project Settings` → `Plugins`
2. Enable **DroidAdMob for Godot**

### 3. Configure Your AdMob App ID

#### Method 1: AndroidManifest.xml (Recommended)

1. Install Android Build Template: `Project` → `Install Android Build Template...`
2. Edit `android/build/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application>
        <!-- Add your AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"
            tools:replace="android:value"/>
    </application>
</manifest>
```

**Important:** Include `xmlns:tools` and `tools:replace="android:value"`

#### Method 2: Use Test App ID (For Testing)

No configuration needed! The plugin comes with Google's test App ID pre-configured.

### 4. Usage in GDScript

```gdscript
extends Node2D

var admob

func _ready():
    # Initialize AdMob
    if Engine.has_singleton("DroidAdMob"):
        admob = Engine.get_singleton("DroidAdMob")
        
        # Connect signals
        admob.connect("ad_loaded", self, "_on_ad_loaded")
        admob.connect("ad_failed_to_load", self, "_on_ad_failed")
        admob.connect("rewarded", self, "_on_rewarded")
        admob.connect("consent_info_updated", self, "_on_consent_info_updated")
        admob.connect("consent_form_dismissed", self, "_on_consent_form_dismissed")
        
        # Request consent (important for GDPR compliance in EU/UK)
        # For testing: use true and your device ID
        # For production: use false and empty string
        admob.requestConsentInfoUpdate(false, "")
    else:
        print("AdMob plugin not found!")

func _on_consent_info_updated():
    print("Consent info updated")
    # Load consent form if required
    admob.loadConsentForm()

func _on_consent_form_dismissed():
    print("Consent form dismissed")
    # Now safe to initialize ads
    admob.initialize(true)  # true = test ads, false = real ads

# Banner Ads
func show_banner():
    var ad_unit_id = admob.getTestBannerAdUnit()
    admob.loadBanner(ad_unit_id, "bottom", "banner")

func hide_banner():
    admob.hideBanner()

func remove_banner():
    admob.removeBanner()

# Interstitial Ads
func load_interstitial():
    var ad_unit_id = admob.getTestInterstitialAdUnit()
    admob.loadInterstitial(ad_unit_id)

func show_interstitial():
    if admob.isInterstitialLoaded():
        admob.showInterstitial()

# Rewarded Ads
func load_rewarded():
    var ad_unit_id = admob.getTestRewardedAdUnit()
    admob.loadRewarded(ad_unit_id)

func show_rewarded():
    if admob.isRewardedLoaded():
        admob.showRewarded()

# Signal handlers
func _on_ad_loaded():
    print("Ad loaded!")

func _on_ad_failed(error):
    print("Ad failed: ", error)

func _on_rewarded(reward_type, reward_amount):
    print("Player earned: ", reward_amount, " ", reward_type)
```

### 5. Export Your Game

1. Go to `Project` → `Export...`
2. Select Android preset
3. Export your APK/AAB

---

## 📱 API Reference

### Initialization

```gdscript
admob.initialize(test_mode: bool)
```

### Test Ad Unit IDs

```gdscript
admob.getTestBannerAdUnit()        # Returns test banner ad unit ID
admob.getTestInterstitialAdUnit()  # Returns test interstitial ad unit ID
admob.getTestRewardedAdUnit()      # Returns test rewarded ad unit ID
```

### Banner Ads

```gdscript
admob.loadBanner(ad_unit_id: String, position: String, size: String)
# position: "top" or "bottom" (default: "bottom")
# size: "banner", "large_banner", "medium_rectangle", "full_banner", "leaderboard"

admob.showBanner()
admob.hideBanner()
admob.removeBanner()
```

### Interstitial Ads

```gdscript
admob.loadInterstitial(ad_unit_id: String)
admob.showInterstitial()
admob.isInterstitialLoaded()  # Returns bool
```

### Rewarded Ads

```gdscript
admob.loadRewarded(ad_unit_id: String)
admob.showRewarded()
admob.isRewardedLoaded()  # Returns bool
```

### Consent Management (GDPR/Privacy)

```gdscript
# Request consent information update (call before initializing ads)
admob.requestConsentInfoUpdate(is_test_mode: bool, test_device_id: String)
# is_test_mode: true to simulate EEA region for testing
# test_device_id: Your device ID for testing (empty string "" for production)

# Load and show consent form (call after consent_info_updated signal)
admob.loadConsentForm()

# Get current consent status
var status = admob.getConsentStatus()
# Returns: 0=UNKNOWN, 1=NOT_REQUIRED, 2=REQUIRED, 3=OBTAINED

# Check if privacy options should be shown to user
var required = admob.isPrivacyOptionsRequired()  # Returns bool

# Show privacy options form (lets user change consent preferences)
admob.showPrivacyOptionsForm()

# Reset consent (for testing only)
admob.resetConsentInformation()
```

### Consent Signals

```gdscript
consent_info_updated              # Consent info successfully updated
consent_info_update_failed(error) # Failed to update consent info
consent_form_dismissed            # User dismissed consent form
consent_form_failed(error)        # Consent form error
consent_status_changed(status)    # Consent status changed (int: 0-3)
```

### Ad Signals

```gdscript
ad_loaded                          # Ad successfully loaded
ad_failed_to_load(error: String)   # Ad failed to load
ad_opened                          # Ad opened/displayed
ad_closed                          # Ad closed
ad_impression                      # Ad impression recorded
ad_clicked                         # Ad was clicked
rewarded(type: String, amount: int) # User earned reward
interstitial_loaded                # Interstitial ad loaded
interstitial_failed_to_load(error: String)
rewarded_ad_loaded                 # Rewarded ad loaded
rewarded_ad_failed_to_load(error: String)
```

---

## 🚀 Production Checklist

Before publishing your game:

- [ ] **GDPR Compliance**: Implement consent management for EU/UK users
  - [ ] Call `requestConsentInfoUpdate()` before initializing ads
  - [ ] Handle consent form display when required
  - [ ] Show privacy options if `isPrivacyOptionsRequired()` returns true
  - [ ] Only initialize ads after consent is gathered
- [ ] Replace test App ID with your AdMob App ID
- [ ] Create real ad units in AdMob Console
- [ ] Replace test ad unit IDs with your real ad unit IDs
- [ ] Set `initialize(false)` to disable test mode
- [ ] Test ads on a real device
- [ ] Verify ad impressions in AdMob Console

---

## 🔐 GDPR Consent Implementation Guide

### Why Consent Matters

- **Legal Requirement**: GDPR requires explicit user consent before collecting personal data in EU/EEA countries
- **Google Policy**: AdMob requires consent collection in applicable regions
- **User Trust**: Transparent privacy practices build user confidence

### Complete Consent Flow Example

```gdscript
extends Node2D

var admob
var ads_initialized = false

func _ready():
    if Engine.has_singleton("DroidAdMob"):
        admob = Engine.get_singleton("DroidAdMob")
        setup_consent()

func setup_consent():
    # Connect all consent signals
    admob.connect("consent_info_updated", self, "_on_consent_info_updated")
    admob.connect("consent_info_update_failed", self, "_on_consent_info_failed")
    admob.connect("consent_form_dismissed", self, "_on_consent_form_dismissed")
    admob.connect("consent_form_failed", self, "_on_consent_form_failed")
    
    # Start consent flow
    # Production: use (false, "")
    # Testing: use (true, "YOUR_DEVICE_ID")
    admob.requestConsentInfoUpdate(false, "")

func _on_consent_info_updated():
    print("Consent info updated, status: ", admob.getConsentStatus())
    # Load consent form (will auto-show if required)
    admob.loadConsentForm()

func _on_consent_info_failed(error: String):
    print("Failed to update consent info: ", error)
    # Can still proceed but with limited ads
    initialize_ads()

func _on_consent_form_dismissed():
    print("Consent form dismissed")
    initialize_ads()

func _on_consent_form_failed(error: String):
    print("Consent form error: ", error)
    initialize_ads()

func initialize_ads():
    if ads_initialized:
        return
    
    ads_initialized = true
    admob.initialize(true)  # true = test mode
    
    # Connect ad signals and load ads
    admob.connect("ad_loaded", self, "_on_ad_loaded")
    admob.connect("ad_failed_to_load", self, "_on_ad_failed")
    
    var banner_id = admob.getTestBannerAdUnit()
    admob.loadBanner(banner_id, "bottom", "banner")

func _on_ad_loaded():
    print("Ad loaded successfully")

func _on_ad_failed(error: String):
    print("Ad failed: ", error)
```

### Testing Consent (Important!)

#### Get Your Test Device ID

1. Run your app on your device with test mode enabled:
   ```gdscript
   admob.requestConsentInfoUpdate(true, "")
   ```

2. Check Android logcat for a line like:
   ```
   Use new ConsentDebugSettings.Builder()
   .addTestDeviceHashedId("33BE2250B43518CCDA7DE426D04EE231")
   ```

3. Copy your device ID and use it:
   ```gdscript
   var test_device_id = "33BE2250B43518CCDA7DE426D04EE231"
   admob.requestConsentInfoUpdate(true, test_device_id)
   ```

This simulates EEA region behavior and lets you test the consent flow.

#### Reset Consent During Testing

```gdscript
# Only for testing - resets all consent
admob.resetConsentInformation()
# Then request again
admob.requestConsentInfoUpdate(true, "YOUR_DEVICE_ID")
```

### Privacy Options Button

If consent is required, provide a way for users to change preferences:

```gdscript
func add_privacy_settings_to_menu():
    # Check if privacy options are required
    if admob.isPrivacyOptionsRequired():
        var privacy_button = Button.new()
        privacy_button.text = "Privacy Settings"
        privacy_button.connect("pressed", self, "_on_privacy_settings_pressed")
        # Add button to your menu UI

func _on_privacy_settings_pressed():
    admob.showPrivacyOptionsForm()
```

### Consent Status Codes

| Code | Status | Meaning |
|------|--------|---------|
| 0 | UNKNOWN | Consent status not yet determined |
| 1 | NOT_REQUIRED | User is not in a region requiring consent |
| 2 | REQUIRED | User is in EEA/UK, consent required |
| 3 | OBTAINED | User has provided consent |

### Best Practices

1. ✅ **Always request consent before initializing ads** - Required by GDPR
2. ✅ **Handle all consent signals** - Don't leave users stuck if something fails
3. ✅ **Provide privacy options** - Users must be able to change consent
4. ✅ **Test in EEA mode** - Make sure the flow works correctly
5. ✅ **Don't block gameplay** - If consent fails, still let users play
6. ✅ **Production mode** - Disable test mode before release: `requestConsentInfoUpdate(false, "")`

---

## 🔍 Troubleshooting

### Ads not showing

- Make sure you called `initialize()` before loading ads
- Check that your App ID is correct
- For testing, use test ad unit IDs
- Verify internet connection
- Check logcat for error messages

### "Multiple entries with same key" error

Add `tools:replace="android:value"` to your AndroidManifest.xml meta-data entry:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-..."
    tools:replace="android:value"/>
```

### Plugin not found

- Verify the plugin is in `addons/DroidAdMob/`
- Make sure the plugin is enabled in Project Settings
- Restart Godot Editor

### Consent form not showing

- Check that you're testing in EEA mode: `requestConsentInfoUpdate(true, "YOUR_DEVICE_ID")`
- Or test in an actual EEA/UK region
- Verify you called `loadConsentForm()` after receiving `consent_info_updated` signal
- Check logcat for UMP SDK errors

### "Consent already gathered" during testing

- Use `resetConsentInformation()` to clear test consent
- Or clear app data on your device
- Make sure you're using test mode with a valid device ID

### Ads not showing after consent

- Verify you called `initialize()` AFTER the consent flow completes
- Check consent status: `admob.getConsentStatus()` should be 3 (OBTAINED) or 1 (NOT_REQUIRED)
- Check for ad load errors via the `ad_failed_to_load` signal
- In production, ensure test mode is disabled: `requestConsentInfoUpdate(false, "")`

---

## 📄 Requirements

- Godot 4.3.0 or later
- Android API 21+ (Android 5.0 Lollipop)
- Target Android API 34
- Google Mobile Ads SDK 22.6.0

---

## 📜 License

MIT License - See [LICENSE](LICENSE) file for details.

This plugin is based on the [Godot Android Plugin Template](https://github.com/m4gr3d/Godot-Android-Plugin-Template) by Fredia Huya-Kouadio.

---

## 🔗 Resources

- [AdMob Console](https://apps.admob.com/) - Manage your ads
- [AdMob Help](https://support.google.com/admob/) - Official support
- [Godot Documentation](https://docs.godotengine.org/) - Godot docs
- [Google UMP SDK Guide](https://developers.google.com/admob/ump/android/quick-start) - Consent management
- [GDPR Compliance](https://support.google.com/admob/answer/10113207) - AdMob GDPR guide
- [EU User Consent Policy](https://www.google.com/about/company/user-consent-policy/) - Google's policy
