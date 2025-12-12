extends Node2D

# AdMob plugin instance
var admob: AdMob

# Ad unit IDs - using test IDs by default
var banner_ad_unit: String
var interstitial_ad_unit: String
var rewarded_ad_unit: String

func _ready():
	# Create AdMob instance
	admob = AdMob.new()

	# Connect signals
	admob.ad_loaded.connect(_on_ad_loaded)
	admob.ad_failed_to_load.connect(_on_ad_failed_to_load)
	admob.ad_opened.connect(_on_ad_opened)
	admob.ad_closed.connect(_on_ad_closed)
	admob.ad_clicked.connect(_on_ad_clicked)
	admob.interstitial_loaded.connect(_on_interstitial_loaded)
	admob.interstitial_failed_to_load.connect(_on_interstitial_failed_to_load)
	admob.rewarded_ad_loaded.connect(_on_rewarded_ad_loaded)
	admob.rewarded_ad_failed_to_load.connect(_on_rewarded_ad_failed_to_load)
	admob.rewarded.connect(_on_rewarded)

	# Initialize AdMob with test mode enabled
	admob.initialize(true)

	# Get test ad unit IDs
	banner_ad_unit = admob.get_test_banner_ad_unit()
	interstitial_ad_unit = admob.get_test_interstitial_ad_unit()
	rewarded_ad_unit = admob.get_test_rewarded_ad_unit()

	print("AdMob Demo Ready!")
	print("Banner Ad Unit: ", banner_ad_unit)
	print("Interstitial Ad Unit: ", interstitial_ad_unit)
	print("Rewarded Ad Unit: ", rewarded_ad_unit)

# Button callbacks
func _on_load_banner_pressed():
	print("Loading banner ad...")
	admob.load_banner(banner_ad_unit, "bottom", "banner")

func _on_hide_banner_pressed():
	print("Hiding banner ad...")
	admob.hide_banner()

func _on_show_banner_pressed():
	print("Showing banner ad...")
	admob.show_banner()

func _on_remove_banner_pressed():
	print("Removing banner ad...")
	admob.remove_banner()

func _on_load_interstitial_pressed():
	print("Loading interstitial ad...")
	admob.load_interstitial(interstitial_ad_unit)

func _on_show_interstitial_pressed():
	if admob.is_interstitial_loaded():
		print("Showing interstitial ad...")
		admob.show_interstitial()
	else:
		print("Interstitial ad not loaded yet!")

func _on_load_rewarded_pressed():
	print("Loading rewarded ad...")
	admob.load_rewarded(rewarded_ad_unit)

func _on_show_rewarded_pressed():
	if admob.is_rewarded_loaded():
		print("Showing rewarded ad...")
		admob.show_rewarded()
	else:
		print("Rewarded ad not loaded yet!")

# AdMob signal handlers
func _on_ad_loaded():
	print("Ad loaded successfully")

func _on_ad_failed_to_load(error_message: String):
	print("Ad failed to load: ", error_message)

func _on_ad_opened():
	print("Ad opened")

func _on_ad_closed():
	print("Ad closed")
	# Reload ads after they're closed
	if not admob.is_interstitial_loaded():
		admob.load_interstitial(interstitial_ad_unit)
	if not admob.is_rewarded_loaded():
		admob.load_rewarded(rewarded_ad_unit)

func _on_ad_clicked():
	print("Ad clicked")

func _on_interstitial_loaded():
	print("Interstitial ad loaded and ready to show")

func _on_interstitial_failed_to_load(error_message: String):
	print("Interstitial ad failed to load: ", error_message)

func _on_rewarded_ad_loaded():
	print("Rewarded ad loaded and ready to show")

func _on_rewarded_ad_failed_to_load(error_message: String):
	print("Rewarded ad failed to load: ", error_message)

func _on_rewarded(type: String, amount: int):
	print("User earned reward: ", amount, " ", type)
	# Give the player their reward here

