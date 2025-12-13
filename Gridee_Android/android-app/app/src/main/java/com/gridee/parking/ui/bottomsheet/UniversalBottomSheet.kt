package com.gridee.parking.ui.bottomsheet

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gridee.parking.data.repository.WalletRepository
import com.gridee.parking.databinding.BottomSheetUniversalBinding
import com.gridee.parking.ui.components.CustomBottomNavigation
import com.gridee.parking.ui.main.MainContainerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UniversalBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetUniversalBinding? = null
    private val binding get() = _binding!!

    private var lottieFileName: String? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewardedAd: Boolean = false
    private val rewardAmountRupees = 20.0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.gridee.parking.R.style.BottomSheetDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as com.google.android.material.bottomsheet.BottomSheetDialog
            
            // Fix for Edge-to-Edge: Ensure the container extends behind nav bar
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                // Remove default background to use ours
                sheet.background = null 
                
                // Force the sheet to extend to the edge
                sheet.fitsSystemWindows = false
                
                // Remove any margins that might lift the sheet up
                val params = sheet.layoutParams as? ViewGroup.MarginLayoutParams
                params?.setMargins(0, 0, 0, 0)
                sheet.layoutParams = params
                
                // Prevent system from padding the sheet automatically
                ViewCompat.setOnApplyWindowInsetsListener(sheet) { view, insets ->
                    view.setPadding(0, 0, 0, 0)
                    insets
                }
            }
            
            // Ensure behavior ignores gesture insets
            bottomSheetDialog.behavior.isGestureInsetBottomIgnored = true
            
            bottomSheetDialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val navBarColor = ContextCompat.getColor(requireContext(), com.gridee.parking.R.color.white)
                window.navigationBarColor = navBarColor
                window.isNavigationBarContrastEnforced = false
                
                // Ensure light nav bar (dark icons) since background is white
                val wic = WindowCompat.getInsetsController(window, window.decorView)
                wic.isAppearanceLightNavigationBars = true
                
                // Remove the black divider line above the nav bar (Android 9+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }

                // Glassmorphism: Blur the screen behind the sheet (Android 12+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.attributes.blurBehindRadius = 50 // 50px blur for frosted glass effect
                    window.attributes = window.attributes // Apply changes
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUniversalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupBehaviors()
        setupInsets()
        
        // Ambient Shadow: Colored glow (Android 9+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            binding.root.outlineAmbientShadowColor = android.graphics.Color.parseColor("#40000000") // Soft black/blue glow
            binding.root.outlineSpotShadowColor = android.graphics.Color.parseColor("#40000000")
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 32dp initial padding + navigation bar height
            val initialPadding = (32 * view.context.resources.displayMetrics.density).toInt()
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, initialPadding + bars.bottom)
            insets
        }
    }

    private fun setupBehaviors() {
        val bottomSheetBehavior = (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.behavior
        bottomSheetBehavior?.addBottomSheetCallback(object : com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Haptic Feedback
                if (newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED ||
                    newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
                ) {
                    bottomSheet.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                }
                
                // Handle Animation: Reset when settled
                if (newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED ||
                    newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED) {
                    animateHandle(32) // Reset to default width
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Micro-Interaction: Widen handle when dragging
                // We use a threshold to detect active dragging
                animateHandle(48) // Widen to 48dp
            }
        })
    }

    private fun animateHandle(targetWidthDp: Int) {
        val targetWidthPx = (targetWidthDp * resources.displayMetrics.density).toInt()
        if (binding.dragHandle.layoutParams.width != targetWidthPx) {
            val params = binding.dragHandle.layoutParams
            // Simple layout update (TransitionManager could be smoother but this is responsive)
            params.width = targetWidthPx
            binding.dragHandle.layoutParams = params
        }
    }

    private fun setupUI() {
        // Close Button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Set Lottie Animation
        if (lottieFileName != null) {
            binding.lottieIcon.setAnimation(lottieFileName)
            binding.lottieIcon.playAnimation()
            binding.lottieIcon.isVisible = true
        } else {
            binding.lottieIcon.isVisible = false
        }

        // Background Pattern Animation (Continuous Rotation)
        val rotateAnim = android.animation.ObjectAnimator.ofFloat(binding.ivRewardBg, "rotation", 0f, 360f)
        rotateAnim.duration = 40000 // 40 seconds for a full rotation (very slow and subtle)
        rotateAnim.repeatCount = android.animation.ObjectAnimator.INFINITE
        rotateAnim.interpolator = android.view.animation.LinearInterpolator()
        rotateAnim.start()

        // Icon Pop Animation
        binding.lottieIcon.scaleX = 0f
        binding.lottieIcon.scaleY = 0f
        binding.lottieIcon.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .setStartDelay(200)
            .start()
            
        // Background Fade In
        binding.ivRewardBg.alpha = 0f
        binding.ivRewardBg.animate()
            .alpha(0.6f)
            .setDuration(800)
            .setStartDelay(100)
            .start()

        // Primary button: show rewarded video for coin reward
        binding.btnPrimary.setOnClickListener {
            showRewardVideo()
        }

        // Preload rewarded ad so it's ready when user taps
        preloadRewardedAd()
    }

    fun setLottieFile(fileName: String) {
        lottieFileName = fileName
        if (_binding != null) {
            binding.lottieIcon.setAnimation(fileName)
            binding.lottieIcon.playAnimation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        rewardedAd = null
    }

    companion object {
        const val TAG = "UniversalBottomSheet"

        fun newInstance(
            lottieFileName: String? = null
        ): UniversalBottomSheet {
            val fragment = UniversalBottomSheet()
            fragment.lottieFileName = lottieFileName
            return fragment
        }
    }

    private fun preloadRewardedAd() {
        if (isLoadingRewardedAd || rewardedAd != null) return

        val adUnitId = "ca-app-pub-3940256099942544/5224354917"
        isLoadingRewardedAd = true

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            requireContext(),
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoadingRewardedAd = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingRewardedAd = false
                    rewardedAd = null
                    Toast.makeText(
                        requireContext(),
                        "Unable to load reward video right now.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun showRewardVideo() {
        val ad = rewardedAd
        if (ad == null) {
            // Try loading again and inform user
            Toast.makeText(
                requireContext(),
                "Loading reward video, please try again in a moment.",
                Toast.LENGTH_SHORT
            ).show()
            preloadRewardedAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                // Close the bottom sheet after video is closed
                dismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                Toast.makeText(
                    requireContext(),
                    "Failed to show reward video.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onAdShowedFullScreenContent() {
                rewardedAd = null
            }
        }

        ad.show(requireActivity()) { rewardItem ->
            // Video watched fully; credit the wallet with the configured reward
            creditRewardToWallet(rewardAmountRupees)
            Toast.makeText(
                requireContext(),
                "Reward earned: ₹${String.format("%.0f", rewardAmountRupees)}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun creditRewardToWallet(amount: Double) {
        if (!isAdded) return
        val ctx = requireContext()

        viewLifecycleOwner.lifecycleScope.launch {
            setRewardLoading(true)
            try {
                val result = withContext(Dispatchers.IO) {
                    WalletRepository(ctx).topUpWallet(amount)
                }
                result.fold(
                    onSuccess = { payload ->
                        val newBalance = (payload["balance"] as? Double)
                        showRewardDialog(amount, newBalance)
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            ctx,
                            "Reward earned but could not be added: ${error.message ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    ctx,
                    "Reward earned but could not be added: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setRewardLoading(false)
            }
        }
    }

    private fun setRewardLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.btnPrimary.isEnabled = !isLoading
        binding.btnPrimary.alpha = if (isLoading) 0.6f else 1f
    }

    private fun showRewardDialog(amount: Double, newBalance: Double?) {
        if (!isAdded) return
        val balanceLine = newBalance?.let { "\nNew balance: ₹${String.format("%.2f", it)}" } ?: ""
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reward Added")
            .setMessage("₹${String.format("%.0f", amount)} has been added to your wallet.$balanceLine")
            .setCancelable(true)
            .setPositiveButton("View Wallet") { _, _ ->
                openWalletPage()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun openWalletPage() {
        val ctx = context ?: return
        val intent = Intent(ctx, MainContainerActivity::class.java).apply {
            putExtra(MainContainerActivity.EXTRA_TARGET_TAB, CustomBottomNavigation.TAB_WALLET)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
