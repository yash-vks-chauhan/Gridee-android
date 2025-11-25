package com.gridee.parking.ui.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.gridee.parking.R

private const val ENTRY_DURATION = 320L
private const val EXIT_DURATION = 220L
private const val ENTRY_START_DELAY = 40L
private const val ENTRY_STAGGER_DELAY = 70L
private const val EXIT_STAGGER_DELAY = 50L

class CoinBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private enum class SheetState {
        PROMPT,
        REWARD_GRANTED
    }

    var onPlayVideoRequested: (() -> Unit)? = null
    var onSeeWalletRequested: (() -> Unit)? = null
    var onSheetDismissed: (() -> Unit)? = null
    private val motionInterpolator = FastOutSlowInInterpolator()

    private var coinAnimationView: LottieAnimationView? = null
    private var descriptionView: TextView? = null
    private var playVideoButton: MaterialCardView? = null
    private var promptContainer: View? = null
    private var rewardSuccessContainer: View? = null
    private var rewardAmountView: TextView? = null
    private var viewWalletButton: MaterialButton? = null
    private var entryAnimator: AnimatorSet? = null
    private var exitAnimator: AnimatorSet? = null
    private var isExitAnimationRunning = false
    private var currentState = SheetState.PROMPT
    private var rewardAmountValue = 10.0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        dialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_coin_animation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coinAnimationView = view.findViewById<LottieAnimationView>(R.id.coin_animation)?.apply {
            playAnimation()
        }
        descriptionView = view.findViewById(R.id.tv_coin_sheet_copy)
        playVideoButton = view.findViewById(R.id.btn_play_video)
        promptContainer = view.findViewById(R.id.layout_coin_prompt)
        rewardSuccessContainer = view.findViewById(R.id.layout_reward_success)
        rewardAmountView = view.findViewById(R.id.tv_reward_amount)
        viewWalletButton = view.findViewById(R.id.btn_view_wallet)

        if (currentState == SheetState.PROMPT) {
            prepareEntryAnimationState()
        }
        updateStateUI()
        view.doOnPreDraw { animateCurrentStateEntry() }

        playVideoButton?.setOnClickListener {
            if (isExitAnimationRunning) return@setOnClickListener
            onPlayVideoRequested?.invoke()
        }
        viewWalletButton?.setOnClickListener {
            if (isExitAnimationRunning) return@setOnClickListener
            onSeeWalletRequested?.invoke()
            closeWithExitAnimation()
        }
        view.findViewById<ImageButton>(R.id.btn_close_sheet)?.apply {
            rotation = 0f
            setOnClickListener { button ->
                if (isExitAnimationRunning) return@setOnClickListener
                button.animate()
                    .rotationBy(180f)
                    .setDuration(220)
                    .withEndAction {
                        button.rotation = 0f
                        closeWithExitAnimation()
                    }
                    .start()
            }
        }
    }

    override fun onDestroyView() {
        coinAnimationView?.cancelAnimation()
        entryAnimator?.cancel()
        exitAnimator?.cancel()
        isExitAnimationRunning = false
        coinAnimationView = null
        descriptionView = null
        playVideoButton = null
        promptContainer = null
        rewardSuccessContainer = null
        rewardAmountView = null
        viewWalletButton = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onSheetDismissed?.invoke()
    }

    companion object {
        const val TAG = "CoinBottomSheet"
    }

    fun showRewardSuccess(amount: Double) {
        rewardAmountValue = amount
        currentState = SheetState.REWARD_GRANTED
        if (view == null) return
        showRewardState(animated = true)
    }

    private fun updateStateUI() {
        when (currentState) {
            SheetState.PROMPT -> {
                promptContainer?.isVisible = true
                rewardSuccessContainer?.isVisible = false
            }
            SheetState.REWARD_GRANTED -> showRewardState(animated = false)
        }
    }

    private fun showRewardState(animated: Boolean) {
        updateRewardTexts()
        if (!animated) {
            promptContainer?.isVisible = false
            rewardSuccessContainer?.apply {
                isVisible = true
                alpha = 1f
                translationY = 0f
                translationX = 0f
            }
            return
        }
        rewardSuccessContainer?.apply {
            isVisible = true
            alpha = 0f
        }
        promptContainer?.animate()
            ?.alpha(0f)
            ?.setDuration(180L)
            ?.withEndAction {
                promptContainer?.isVisible = false
                promptContainer?.alpha = 1f
            }
            ?.start()
        playRewardGrantedAnimation()
    }

    private fun animateCurrentStateEntry() {
        when (currentState) {
            SheetState.PROMPT -> playEntryAnimations()
            SheetState.REWARD_GRANTED -> playRewardGrantedAnimation()
        }
    }

    private fun playRewardGrantedAnimation() {
        val container = rewardSuccessContainer ?: return
        val offset = resources.getDimension(R.dimen.coin_sheet_slide_offset)
        container.alpha = 0f
        container.translationY = offset
        container.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ENTRY_DURATION)
            .setInterpolator(motionInterpolator)
            .start()
    }

    private fun updateRewardTexts() {
        val amountInt = rewardAmountValue.toInt()
        rewardAmountView?.text = getString(R.string.home_coin_reward_amount, amountInt)
    }

    private fun prepareEntryAnimationState() {
        if (currentState != SheetState.PROMPT) return
        val offset = resources.getDimension(R.dimen.coin_sheet_slide_offset)
        listOfNotNull(coinAnimationView, descriptionView, playVideoButton).forEach { view ->
            view.translationX = -offset
            view.alpha = 0f
        }
    }

    private fun playEntryAnimations() {
        if (currentState != SheetState.PROMPT) return
        val offset = resources.getDimension(R.dimen.coin_sheet_slide_offset)
        val pigAnimator = coinAnimationView?.let {
            createSlideAnimator(it, -offset, 0f, true).apply { duration = ENTRY_DURATION }
        }
        val textAnimator = descriptionView?.let {
            createSlideAnimator(it, -offset, 0f, true).apply { duration = ENTRY_DURATION }
        }
        val buttonAnimator = playVideoButton?.let {
            createSlideAnimator(it, -offset, 0f, true).apply { duration = ENTRY_DURATION }
        }
        val animators = listOfNotNull(pigAnimator, textAnimator, buttonAnimator)
        if (animators.isEmpty()) return

        pigAnimator?.startDelay = ENTRY_START_DELAY
        textAnimator?.startDelay = ENTRY_START_DELAY + ENTRY_STAGGER_DELAY
        buttonAnimator?.startDelay = ENTRY_START_DELAY + (ENTRY_STAGGER_DELAY * 2)

        entryAnimator?.cancel()
        entryAnimator = AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    private fun closeWithExitAnimation() {
        if (isExitAnimationRunning) return

        val offset = resources.getDimension(R.dimen.coin_sheet_slide_offset)
        val animators = if (currentState == SheetState.REWARD_GRANTED) {
            rewardSuccessContainer?.let {
                listOf(
                    createSlideAnimator(it, 0f, offset, false).apply { duration = EXIT_DURATION }
                )
            } ?: emptyList()
        } else {
            val buttonAnimator = playVideoButton?.let {
                createSlideAnimator(it, 0f, offset, false).apply { duration = EXIT_DURATION }
            }
            val textAnimator = descriptionView?.let {
                createSlideAnimator(it, 0f, offset, false).apply { duration = EXIT_DURATION }
            }
            val pigAnimator = coinAnimationView?.let {
                createSlideAnimator(it, 0f, offset, false).apply { duration = EXIT_DURATION }
            }
            buttonAnimator?.startDelay = 0L
            textAnimator?.startDelay = EXIT_STAGGER_DELAY
            pigAnimator?.startDelay = EXIT_STAGGER_DELAY * 2
            listOfNotNull(buttonAnimator, textAnimator, pigAnimator)
        }
        if (animators.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }

        entryAnimator?.cancel()
        isExitAnimationRunning = true
        exitAnimator?.cancel()
        exitAnimator = AnimatorSet().apply {
            playTogether(animators)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    isExitAnimationRunning = false
                    super@CoinBottomSheetDialogFragment.dismissAllowingStateLoss()
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    isExitAnimationRunning = false
                }
            })
            start()
        }
    }

    private fun createSlideAnimator(
        target: View,
        fromX: Float,
        toX: Float,
        fadeIn: Boolean
    ): AnimatorSet {
        target.translationX = fromX
        target.alpha = if (fadeIn) 0f else 1f

        val slide = ObjectAnimator.ofFloat(target, View.TRANSLATION_X, fromX, toX)
        val alphaValues = if (fadeIn) floatArrayOf(0f, 1f) else floatArrayOf(1f, 0f)
        val fade = ObjectAnimator.ofFloat(target, View.ALPHA, *alphaValues)

        return AnimatorSet().apply {
            interpolator = motionInterpolator
            playTogether(slide, fade)
        }
    }
}
