package com.gridee.parking.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.gridee.parking.R
import com.gridee.parking.databinding.FragmentHomeBinding
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.RewardClaimRequest
import com.gridee.parking.data.model.TopUpRequest
import com.gridee.parking.ui.MainViewModel
import com.gridee.parking.ui.base.BaseTabFragment
import com.gridee.parking.ui.home.HomeParkingSpot
import com.gridee.parking.ui.home.HomeParkingSpotAdapter
import com.gridee.parking.ui.home.ParkingCategoryBottomSheet
import com.gridee.parking.ui.home.ParkingSelectionSummaryBottomSheet
import com.gridee.parking.ui.home.ParkingSpotBottomSheetDialogFragment
import com.gridee.parking.ui.home.ParkingSpotCategory
import com.gridee.parking.ui.home.ParkingTimeSelectionBottomSheet
import com.gridee.parking.ui.bottomsheet.VehiclesQuickActionBottomSheet
import com.gridee.parking.ui.search.SearchActivity
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.gridee.parking.ui.profile.ProfileViewModel
import com.gridee.parking.utils.JwtTokenManager
import kotlinx.coroutines.launch
import retrofit2.HttpException

class HomeFragment : BaseTabFragment<FragmentHomeBinding>() {

    companion object {
        private const val TAG = "HomeFragment"
        private const val KEY_SELECTED_CATEGORY = "home_selected_category"
        private const val KEY_CATEGORY_PROMPTED = "home_category_prompted"
        private const val KEY_CHECK_IN_TIME = "home_check_in_time"
        private const val KEY_CHECK_OUT_TIME = "home_check_out_time"
        private const val TAG_TIME_SELECTION = "ParkingTimeSelection"
        private val MIN_CHECKOUT_OFFSET = TimeUnit.HOURS.toMillis(1)
        private val DEFAULT_CHECKOUT_OFFSET = TimeUnit.HOURS.toMillis(2)

        fun newInstance(userName: String? = null): HomeFragment {
            return HomeFragment().apply {
                userName?.let {
                    arguments = android.os.Bundle().apply {
                        putString("USER_NAME", it)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val restoredCheckInMillis = savedInstanceState?.getLong(KEY_CHECK_IN_TIME)?.takeIf { it > 0 }
        val defaultCheckIn = createDefaultCheckInTime()
        selectedCheckInTime = restoredCheckInMillis?.let { Date(it) } ?: defaultCheckIn
        val restoredCheckOutMillis = savedInstanceState?.getLong(KEY_CHECK_OUT_TIME)?.takeIf { it > 0 }
        selectedCheckOutTime = restoredCheckOutMillis?.let { Date(it) } ?: createDefaultCheckOutTime(selectedCheckInTime)
        ensureValidTimeRange()

        savedInstanceState?.getString(KEY_SELECTED_CATEGORY)?.let { savedName ->
            val recovered = ParkingSpotCategory.values().firstOrNull { it.name == savedName }
            if (recovered != null) {
                selectedCategory = recovered
            }
        }
        hasPromptedCategorySheet = savedInstanceState?.getBoolean(KEY_CATEGORY_PROMPTED, false) ?: false
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var parkingSpotAdapter: HomeParkingSpotAdapter
    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false
    private var nativeAd: NativeAd? = null
    private var coinBottomSheet: CoinBottomSheetDialogFragment? = null
    private var heroBasePaddingTop = 0
    private var scrollBasePaddingBottom = 0
    private var isSticky = false
    private var searchBarTop = 0
    private var stickyThreshold = 0
    private var cachedParkingSpots: List<HomeParkingSpot> = emptyList()
    private var selectedCategory: ParkingSpotCategory = ParkingSpotCategory.default()
    private var hasPromptedCategorySheet = false
    private var selectedCheckInTime: Date = Date()
    private var selectedCheckOutTime: Date = Date()

    private val voiceSearchLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val query = matches?.firstOrNull()?.trim()
                if (!query.isNullOrEmpty()) {
                    openSearch(initialQuery = query)
                } else {
                    showToast(getString(R.string.voice_search_no_result))
                }
            }
        }

    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startVoiceRecognition()
            } else {
                showToast(getString(R.string.voice_search_permission_denied))
            }
        }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun getScrollableView(): View? {
        return try {
            binding.scrollContent
        } catch (e: IllegalStateException) {
            null
        }
    }

    override fun setupUI() {
        heroBasePaddingTop = binding.homeHeroContainer.paddingTop
        scrollBasePaddingBottom = binding.scrollContent.paddingBottom
        setupEdgeToEdgeInsets()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        profileViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        setupUserWelcome()
        setupVehiclePreview()
        setupClickListeners()
        setupStickySearchBar()
        setupParkingSpotSection()
        setupParkingCategoryFilter()
        setupAds()
    }

    private fun setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.homeHeroContainer.setPadding(
                binding.homeHeroContainer.paddingLeft,
                heroBasePaddingTop + statusBars.top,
                binding.homeHeroContainer.paddingRight,
                binding.homeHeroContainer.paddingBottom
            )

            binding.stickySearchContainer.setPadding(
                binding.stickySearchContainer.paddingLeft,
                statusBars.top,
                binding.stickySearchContainer.paddingRight,
                binding.stickySearchContainer.paddingBottom
            )

            binding.scrollContent.setPadding(
                binding.scrollContent.paddingLeft,
                binding.scrollContent.paddingTop,
                binding.scrollContent.paddingRight,
                scrollBasePaddingBottom + navBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupUserWelcome() {
        // Get user name from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        val userName = arguments?.getString("USER_NAME")
            ?: sharedPref.getString("user_name", "User")
            ?: "User"

        val greetingPrefix = getGreetingForCurrentTime()
        binding.tvAppTitle.text = getString(R.string.home_header_greeting, greetingPrefix, userName)
    }

    private fun setupVehiclePreview() {
        profileViewModel.userProfile.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                val count = user.vehicleNumbers.size
                if (count > 0) {
                    binding.tvVehiclesCount.text = "$count vehicles"
                    binding.tvVehiclesCount.visibility = View.VISIBLE
                    binding.tvVehiclesSubtitle.text = "Your everyday"
                } else {
                    binding.tvVehiclesCount.visibility = View.GONE
                    binding.tvVehiclesSubtitle.text = "Add your first"
                }
            }
        }

        // Load if needed
        if (profileViewModel.userProfile.value == null) {
             val jwtManager = JwtTokenManager(requireContext())
             val userId = jwtManager.getUserId()
             if (!userId.isNullOrEmpty()) {
                 profileViewModel.loadUserProfile(userId)
             }
        }
    }

    private fun getGreetingForCurrentTime(): String {
        val istCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val hourOfDay = istCalendar.get(Calendar.HOUR_OF_DAY)
        return when (hourOfDay) {
            in 6..11 -> getString(R.string.home_greeting_morning)
            in 12..16 -> getString(R.string.home_greeting_afternoon)
            in 17..20 -> getString(R.string.home_greeting_evening)
            else -> getString(R.string.home_greeting_welcome_back)
        }
    }

    private fun setupClickListeners() {
        binding.greetingAnimation.setOnClickListener {
            binding.greetingAnimation.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(90)
                .withEndAction {
                    binding.greetingAnimation.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(140)
                        .start()
                }
                .start()

            showCoinSheet()
        }

        binding.cardSearch.setOnClickListener {
            openSearch()
        }

        binding.ivSearchMic.setOnClickListener {
            handleMicClick()
        }

        // Sticky Search Listeners
        binding.stickyCardSearch.setOnClickListener {
            openSearch()
        }

        binding.stickyIvSearchMic.setOnClickListener {
            handleMicClick()
        }

        // Quick Actions Grid Listeners
        binding.cardActionFindParking.setOnClickListener {
            openSearch()
        }

        binding.cardActionHistory.setOnClickListener {
            try {
                // Navigate to Bookings Tab in MainContainerActivity
                val intent = Intent(requireContext(), com.gridee.parking.ui.main.MainContainerActivity::class.java).apply {
                    putExtra(com.gridee.parking.ui.main.MainContainerActivity.EXTRA_TARGET_TAB, com.gridee.parking.ui.components.CustomBottomNavigation.TAB_BOOKINGS)
                    putExtra(com.gridee.parking.ui.main.MainContainerActivity.EXTRA_SHOW_COMPLETED, true)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback
                showToast("Opening Booking History...")
            }
        }

        binding.cardActionWallet.setOnClickListener {
            openWalletTab()
        }

        binding.cardActionVehicles.setOnClickListener {
            // Navigate to Vehicles
            VehiclesQuickActionBottomSheet().show(childFragmentManager, VehiclesQuickActionBottomSheet.TAG)
        }
    }

    private fun setupStickySearchBar() {
        binding.scrollContent.setOnScrollChangeListener { _: View?, _: Int, scrollY: Int, _: Int, _: Int ->
            // Calculate positions if not already done
            if (searchBarTop == 0) {
                val location = IntArray(2)
                binding.searchContainer.getLocationOnScreen(location)
            }

            val searchLocation = IntArray(2)
            binding.searchContainer.getLocationOnScreen(searchLocation)
            val searchY = searchLocation[1]

            val stickyContainerLocation = IntArray(2)
            binding.stickySearchContainer.getLocationOnScreen(stickyContainerLocation)
            val stickyY = stickyContainerLocation[1] + binding.stickySearchContainer.paddingTop

            // Adjust this offset to fine tune the "Soft Lock" feel
            val lockOffset = resources.getDimensionPixelSize(R.dimen.home_search_corner_radius) / 2 

            if (searchY <= stickyY + lockOffset) {
                if (!isSticky) {
                    isSticky = true
                    
                    // Physics-based Entry: "Magnetic Snap"
                    binding.stickySearchContainer.visibility = View.VISIBLE
                    binding.stickySearchContainer.alpha = 0f
                    
                    // Start slightly "down" (following momentum) and slightly smaller
                    binding.stickySearchContainer.translationY = 40f 
                    binding.stickySearchContainer.scaleX = 0.96f
                    binding.stickySearchContainer.scaleY = 0.96f

                    // Animate with Overshoot for a natural "settle" effect
                    binding.stickySearchContainer.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(450) // Elegant duration
                        .setInterpolator(android.view.animation.OvershootInterpolator(1.1f)) // The "Soft Lock" physics
                        .start()
                    
                    // Smoothly fade out original
                    binding.searchContainer.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .start()
                        
                    // Haptic feedback for the "Lock"
                    binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                }
            } else {
                if (isSticky) {
                    isSticky = false
                    
                    // Physics-based Exit: "Release Tension"
                    binding.stickySearchContainer.animate()
                        .alpha(0f)
                        .translationY(40f) // Slide back down matching scroll direction
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .setDuration(300)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .withEndAction {
                            binding.stickySearchContainer.visibility = View.INVISIBLE
                        }
                        .start()

                    // Fade in original
                    binding.searchContainer.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
            }
        }
    }

    private fun setupParkingSpotSection() {
        parkingSpotAdapter = HomeParkingSpotAdapter().apply {
            onSpotClick = { spot, view ->
                val rect = Rect()
                view.getGlobalVisibleRect(rect)
                
                ParkingSpotBottomSheetDialogFragment
                    .newInstance(spot, rect, selectedCheckInTime, selectedCheckOutTime)
                    .show(childFragmentManager, ParkingSpotBottomSheetDialogFragment.TAG)
            }
        }

        val snapHelper = PagerSnapHelper()
        binding.rvParkingSpots.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = parkingSpotAdapter
            if (itemDecorationCount == 0) {
                val spacing = resources.getDimensionPixelSize(R.dimen.home_parking_spot_spacing)
                addItemDecoration(HorizontalSpaceItemDecoration(spacing))
            }
            snapHelper.attachToRecyclerView(this)
        }

        binding.btnParkingRefresh.setOnClickListener {
            viewModel.refreshParkingSnapshot()
        }

        viewModel.parkingSpots.observe(viewLifecycleOwner) { spots ->
            cachedParkingSpots = spots.orEmpty()
            applyParkingCategoryFilter()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.parkingLoadingIndicator.isVisible = isLoading
            binding.btnParkingRefresh.isEnabled = !isLoading
            binding.btnParkingRefresh.alpha = if (isLoading) 0.5f else 1f
            binding.tvParkingEmptyState.isVisible = !isLoading && parkingSpotAdapter.itemCount == 0
        }
    }

    private fun setupParkingCategoryFilter() {
        updateSelectedCategoryUI()
        binding.btnParkingCategory.setOnClickListener {
            openSelectionSummarySheet()
        }
        maybePromptCategorySelection()
    }

    private fun maybePromptCategorySelection() {
        if (hasPromptedCategorySheet) return
        binding.root.post {
            if (!isAdded || hasPromptedCategorySheet) return@post
            openCategoryBottomSheet()
        }
    }

    private fun openCategoryBottomSheet() {
        if (childFragmentManager.findFragmentByTag(ParkingCategoryBottomSheet.TAG) != null) {
            return
        }
        hasPromptedCategorySheet = true
        val sheet = ParkingCategoryBottomSheet.newInstance(selectedCategory)
        sheet.onCategorySelected = { category ->
            selectedCategory = category
            updateSelectedCategoryUI()
            applyParkingCategoryFilter()
            openTimeSelectionBottomSheet()
        }
        sheet.show(childFragmentManager, ParkingCategoryBottomSheet.TAG)
    }

    private fun openTimeSelectionBottomSheet() {
        if (childFragmentManager.findFragmentByTag(TAG_TIME_SELECTION) != null) {
            return
        }
        val sheet = ParkingTimeSelectionBottomSheet.newInstance(
            selectedCheckInTime.time,
            selectedCheckOutTime.time
        )
        sheet.onTimesConfirmed = { checkIn, checkOut ->
            selectedCheckInTime = checkIn
            selectedCheckOutTime = checkOut
            ensureValidTimeRange()
            updateSelectedCategoryUI()
            openSelectionSummarySheet()
        }
        sheet.show(childFragmentManager, TAG_TIME_SELECTION)
    }

    private fun openSelectionSummarySheet() {
        if (childFragmentManager.findFragmentByTag(ParkingSelectionSummaryBottomSheet.TAG) != null) {
            return
        }
        val categoryTitle = getString(selectedCategory.titleRes)
        val categorySubtitle = getString(selectedCategory.descriptionRes)
        val sheet = ParkingSelectionSummaryBottomSheet.newInstance(
            categoryTitle,
            categorySubtitle,
            selectedCheckInTime.time,
            selectedCheckOutTime.time
        )
        sheet.onApplySelection = {
            showToast(getString(R.string.home_parking_selection_applied))
        }
        sheet.onModifyTimer = {
            openTimeSelectionBottomSheet()
        }
        sheet.onModifyCategory = {
            openCategoryBottomSheet()
        }
        sheet.show(childFragmentManager, ParkingSelectionSummaryBottomSheet.TAG)
    }

    private fun updateSelectedCategoryUI() {
        binding.btnParkingCategory.text = getString(selectedCategory.titleRes)
    }

    private fun applyParkingCategoryFilter() {
        updateSelectedCategoryUI()
        val filtered = selectedCategory.filter(cachedParkingSpots)
        parkingSpotAdapter.submitSpots(filtered)
        val hasItems = filtered.isNotEmpty()
        val isLoading = binding.parkingLoadingIndicator.isVisible
        binding.rvParkingSpots.isVisible = hasItems
        binding.tvParkingEmptyState.isVisible = !hasItems && !isLoading
    }

    private fun createDefaultCheckInTime(): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 30)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun createDefaultCheckOutTime(start: Date): Date {
        return Date(start.time + DEFAULT_CHECKOUT_OFFSET)
    }

    private fun ensureValidTimeRange() {
        if (selectedCheckOutTime.time - selectedCheckInTime.time < MIN_CHECKOUT_OFFSET) {
            selectedCheckOutTime = Date(selectedCheckInTime.time + MIN_CHECKOUT_OFFSET)
        }
    }

    private fun setupAds() {
        MobileAds.initialize(requireContext())
        loadNativeAd()
        loadRewardedAd()
    }

    private fun loadNativeAd() {
        if (!isAdded) return

        binding.homeNativeAdView.visibility = View.GONE
        binding.ivAdPlaceholder.visibility = View.VISIBLE

        val adLoader = AdLoader.Builder(requireContext(), getString(R.string.admob_native_unit_id))
            .forNativeAd { ad ->
                if (!isAdded || view == null) {
                    ad.destroy()
                    return@forNativeAd
                }
                nativeAd?.destroy()
                nativeAd = ad
                populateNativeAdView(ad)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed: ${error.message} (${error.code})")
                    binding.homeNativeAdView.visibility = View.GONE
                    binding.ivAdPlaceholder.visibility = View.VISIBLE
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(ad: NativeAd) {
        val adView = binding.homeNativeAdView
        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
        val iconView = adView.findViewById<ImageView>(R.id.ad_icon)
        val callToActionView = adView.findViewById<TextView>(R.id.ad_call_to_action)
        val adChoicesView = adView.findViewById<AdChoicesView>(R.id.ad_choices_container)

        adView.mediaView = mediaView
        adView.headlineView = headlineView
        adView.bodyView = bodyView
        adView.advertiserView = advertiserView
        adView.iconView = iconView
        adView.callToActionView = callToActionView
        adView.adChoicesView = adChoicesView

        headlineView.text = ad.headline

        val mediaContent = ad.mediaContent
        if (mediaContent != null) {
            mediaView.visibility = View.VISIBLE
            mediaView.setMediaContent(mediaContent)
        } else {
            mediaView.visibility = View.GONE
        }

        val body = ad.body
        if (body.isNullOrEmpty()) {
            bodyView.visibility = View.GONE
        } else {
            bodyView.visibility = View.VISIBLE
            bodyView.text = body
        }

        val advertiser = ad.advertiser
        if (advertiser.isNullOrEmpty()) {
            advertiserView.visibility = View.GONE
        } else {
            advertiserView.visibility = View.VISIBLE
            advertiserView.text = advertiser
        }

        val icon = ad.icon
        if (icon == null) {
            iconView.visibility = View.GONE
        } else {
            iconView.visibility = View.VISIBLE
            iconView.setImageDrawable(icon.drawable)
        }

        val callToAction = ad.callToAction
        if (callToAction.isNullOrEmpty()) {
            callToActionView.visibility = View.GONE
        } else {
            callToActionView.visibility = View.VISIBLE
            callToActionView.text = callToAction
        }

        adChoicesView?.visibility = View.VISIBLE
        adView.setNativeAd(ad)

        binding.ivAdPlaceholder.visibility = View.GONE
        adView.visibility = View.VISIBLE
    }

    private fun loadRewardedAd() {
        if (isRewardedLoading) return
        isRewardedLoading = true
        RewardedAd.load(
            requireContext(),
            getString(R.string.admob_rewarded_unit_id),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    showToast(getString(R.string.home_ad_failed_to_load, adError.message))
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    attachFullScreenCallbacks(ad)
                }
            }
        )
    }

    private fun attachFullScreenCallbacks(ad: RewardedAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd()
            }

        }
    }

    private fun handlePlayAdClick() {
        val readyAd = rewardedAd
        if (readyAd != null) {
            readyAd.show(requireActivity()) { rewardItem ->
                val earnedAmount = rewardItem.amount.takeIf { it > 0 } ?: 10
                showToast(getString(R.string.home_ad_thank_you))
                creditRewardToWallet(earnedAmount)
            }
        } else {
            showToast(getString(R.string.home_ad_loading_message))
            loadRewardedAd()
        }
    }

    private fun showCoinSheet(showReward: Boolean = false, rewardAmount: Double = 0.0) {
        val existing = coinBottomSheet
        if (existing?.isAdded == true) {
            if (showReward) existing.showRewardSuccess(rewardAmount)
            return
        }
        val sheet = CoinBottomSheetDialogFragment().apply {
            onPlayVideoRequested = { handlePlayAdClick() }
            onSeeWalletRequested = { openWalletTab() }
            onSheetDismissed = { coinBottomSheet = null }
        }
        if (showReward) {
            sheet.showRewardSuccess(rewardAmount)
        }
        coinBottomSheet = sheet
        sheet.show(childFragmentManager, CoinBottomSheetDialogFragment.TAG)
    }

    private fun creditRewardToWallet(rewardAmount: Int) {
        val userId = JwtTokenManager(requireContext()).getUserId()
        if (userId.isNullOrEmpty()) {
            showToast(getString(R.string.home_wallet_reward_login))
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = attemptRewardCredit(userId, rewardAmount)) {
                RewardCreditResult.SUCCESS -> {
                    showToast(getString(R.string.home_wallet_reward_success, rewardAmount))
                    presentRewardSuccessState(rewardAmount.toDouble())
                }
                RewardCreditResult.UNAUTHORIZED -> {
                    showToast(getString(R.string.home_wallet_reward_login))
                }
                RewardCreditResult.FAILURE -> {
                    showToast(getString(R.string.home_wallet_reward_failure))
                }
            }
        }
    }

    private fun presentRewardSuccessState(amount: Double) {
        val sheet = coinBottomSheet
        if (sheet?.isAdded == true) {
            sheet.showRewardSuccess(amount)
        } else {
            showCoinSheet(showReward = true, rewardAmount = amount)
        }
    }

    private suspend fun attemptRewardCredit(userId: String, amount: Int): RewardCreditResult {
        return when (val endpointResult = callRewardEndpoint(userId, amount)) {
            RewardEndpointResult.SUCCESS -> RewardCreditResult.SUCCESS
            RewardEndpointResult.UNAUTHORIZED -> RewardCreditResult.UNAUTHORIZED
            RewardEndpointResult.FAILURE -> {
                if (tryLegacyRewardCredit(userId, amount)) {
                    RewardCreditResult.SUCCESS
                } else {
                    RewardCreditResult.FAILURE
                }
            }
        }
    }

    private suspend fun callRewardEndpoint(userId: String, amount: Int): RewardEndpointResult {
        return try {
            val response = ApiClient.apiService.claimWalletReward(
                userId,
                RewardClaimRequest(
                    amount = amount.toDouble(),
                    source = "rewarded_video",
                    rewardId = UUID.randomUUID().toString()
                )
            )
            when {
                response.isSuccessful -> RewardEndpointResult.SUCCESS
                response.code() == 401 -> RewardEndpointResult.UNAUTHORIZED
                else -> RewardEndpointResult.FAILURE
            }
        } catch (http: HttpException) {
            when {
                http.code() == 401 -> RewardEndpointResult.UNAUTHORIZED
                else -> RewardEndpointResult.FAILURE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Reward API call failed", e)
            RewardEndpointResult.FAILURE
        }
    }

    private suspend fun tryLegacyRewardCredit(userId: String, amount: Int): Boolean {
        return try {
            val response = ApiClient.apiService.topUpWallet(
                userId,
                TopUpRequest(
                    amount = amount.toDouble(),
                    type = "reward_bonus",
                    source = "rewarded_video"
                )
            )
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Legacy reward fallback failed", e)
            false
        }
    }

    private fun openSearch(initialQuery: String? = null) {
        val context = requireContext()
        val intent = Intent(context, SearchActivity::class.java).apply {
            initialQuery?.let {
                putExtra(SearchActivity.EXTRA_INITIAL_QUERY, it)
            }
        }

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            binding.cardSearch,
            getString(R.string.transition_search_bar)
        )

        startActivity(intent, options.toBundle())
    }

    private fun openWalletTab() {
        try {
            val intent = Intent(requireContext(), com.gridee.parking.ui.main.MainContainerActivity::class.java).apply {
                putExtra(com.gridee.parking.ui.main.MainContainerActivity.EXTRA_TARGET_TAB, com.gridee.parking.ui.components.CustomBottomNavigation.TAB_WALLET)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Opening Wallet...")
        }
    }

    private fun handleMicClick() {
        val context = requireContext()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.voice_search_prompt)
            )
        }

        if (speechIntent.resolveActivity(context.packageManager) == null) {
            showToast(getString(R.string.voice_search_not_supported))
            return
        }

        if (hasPermission) {
            voiceSearchLauncher.launch(speechIntent)
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceRecognition() {
        val context = requireContext()
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.voice_search_prompt)
            )
        }

        if (speechIntent.resolveActivity(context.packageManager) == null) {
            showToast(getString(R.string.voice_search_not_supported))
            return
        }

        voiceSearchLauncher.launch(speechIntent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_CHECK_IN_TIME, selectedCheckInTime.time)
        outState.putLong(KEY_CHECK_OUT_TIME, selectedCheckOutTime.time)
        outState.putString(KEY_SELECTED_CATEGORY, selectedCategory.name)
        outState.putBoolean(KEY_CATEGORY_PROMPTED, hasPromptedCategorySheet)
    }

    override fun onResume() {
        super.onResume()
        binding.greetingAnimation.playAnimation()
    }

    override fun onPause() {
        binding.greetingAnimation.pauseAnimation()
        super.onPause()
    }

    override fun onDestroyView() {
        nativeAd?.destroy()
        nativeAd = null
        binding.greetingAnimation.cancelAnimation()
        rewardedAd = null
        coinBottomSheet = null
        super.onDestroyView()
    }

    private enum class RewardCreditResult {
        SUCCESS,
        FAILURE,
        UNAUTHORIZED
    }

    private enum class RewardEndpointResult {
        SUCCESS,
        FAILURE,
        UNAUTHORIZED
    }

    private class HorizontalSpaceItemDecoration(private val spacing: Int) :
        RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val total = parent.adapter?.itemCount ?: 0
            val half = spacing / 2
            
            // First item: no left spacing (relies on RecyclerView padding)
            // Middle items: half spacing on both sides
            // Last item: no right spacing (relies on RecyclerView padding)
            
            outRect.left = if (position == 0) 0 else half
            outRect.right = if (position == total - 1) 0 else half
        }
    }

}
