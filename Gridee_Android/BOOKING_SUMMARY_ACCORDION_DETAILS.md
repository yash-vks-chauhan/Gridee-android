# Booking Summary Accordion: Implementation Details

This document outlines the advanced UI/UX techniques, animations, and design principles used to create the premium "Booking Summary" accordion in the Parking Spot Bottom Sheet.

## 1. Core Interaction Design
The accordion is designed to be more than just a collapsible view; it is a tactile, physical object within the UI.

*   **Trigger**: The entire `MaterialCardView` acts as the trigger, not just the arrow.
*   **Tactile Feedback**:
    *   **Haptics**: We utilize `HapticFeedbackConstants.CONTEXT_CLICK` (or `KEYBOARD_TAP` on older devices) to provide a subtle physical "tick" when the user toggles the summary. This mimics the feel of a mechanical switch.
*   **Visual Feedback**: The chevron icon rotates 180 degrees smoothly, synchronized exactly with the expansion animation.

## 2. Motion & Transitions (The "Engine")
We moved away from standard `AutoTransition` to a bespoke **Custom Transition Set** to achieve the "slow and smooth" feel.

*   **Transition Set**:
    *   **Ordering**: `ORDERING_TOGETHER`. This is crucial. Standard transitions often expand *then* fade in. We do both simultaneously for a seamless "reveal".
    *   **Duration**: `400ms`. Slightly slower than the default (300ms) to give the animation a sense of weight and luxury.
    *   **Interpolator**: `FastOutSlowInInterpolator`. This mimics real-world physics—starting quickly and settling gently, rather than moving at a robotic constant speed.
*   **Components**:
    *   `ChangeBounds`: Handles the physical expansion of the card height.
    *   `Fade (IN/OUT)`: Ensures the content doesn't just "pop" into existence but materializes smoothly.

## 3. The "Waterfall" Staggered Reveal
To avoid the content appearing as a single static block, we implemented a staggered animation sequence for the inner rows (Check-in, Check-out, Rate).

*   **Sequence**:
    1.  **Row 1 (Check-in)**: Starts immediately.
    2.  **Row 2 (Check-out)**: Starts after 70ms.
    3.  **Row 3 (Rate)**: Starts after 140ms.
*   **Motion**: Each row slides down (`translationY`) by 20 pixels while fading in (`alpha` 0 -> 1). This creates a cascading "waterfall" effect that feels organic.

## 4. Advanced Visual Effects

### A. Real-Time Blur (Focus Effect)
*   **Target**: API 31+ (Android 12 and above).
*   **Effect**: We use `RenderEffect.createBlurEffect` to simulate a camera lens coming into focus.
*   **Implementation**: As the rows slide in, they start with a blur radius of `10f` and animate down to `0f`. This makes the text appear to "sharpen" into readability, adding a high-end cinematic touch.

### B. Premium Shimmer "Gleam"
*   **Purpose**: To draw attention to the new information without being distracting.
*   **Timing**: The shimmer triggers **600ms** after the toggle—exactly when the expansion finishes. It acts as a final "polish" or "settling" animation.
*   **Technique**:
    *   We use a `LinearGradient` shader on the `TextView` paint.
    *   **One-Shot**: Unlike standard loading shimmers that loop, this plays exactly **once**.
    *   **Color Preservation**: The shimmer uses the text's original color but adds a white/bright center, creating a metallic "gleam" rather than washing out the text.

## 5. Closing Animation ("The Zip-Up")
The closing experience is choreographed to be the perfect mechanical inverse of the opening, creating a satisfying "close" loop.

*   **Concept**: "The Zip-Up". Instead of disappearing all at once, the content rolls up from bottom to top.
*   **Sequence (Reverse Stagger)**:
    1.  **Row 3 (Rate)**: Starts disappearing immediately.
    2.  **Row 2 (Check-out)**: Starts after 40ms.
    3.  **Row 1 (Check-in)**: Starts after 80ms.
*   **Motion**:
    *   **Slide Up**: Rows slide *up* (`translationY` -15f) while fading out. This reinforces the visual metaphor of the accordion folding upwards.
    *   **Speed**: Fast (150ms). It feels responsive and snappy, respecting the user's intent to move on.
*   **Delayed Collapse**:
    *   The physical container waits **120ms** before shrinking.
    *   **Why?**: This allows the bottom rows to partially vanish before the box closes, preventing any visual "crushing" of the text. It ensures the content leaves the stage gracefully before the curtain falls.

---

### Summary of Tech Stack
*   **Kotlin**: Logic and animation orchestration.
*   **Android Transitions API**: `TransitionSet`, `ChangeBounds`, `Fade`.
*   **ViewPropertyAnimator**: For the lightweight row animations (`animate().translationY...`).
*   **RenderEffect (Android 12+)**: For the blur/focus simulation.
*   **Shader & Paint**: For the custom text shimmer.
