# Wallet Card Gradient Options

## Current Active Gradient
**File:** `wallet_card_gradient.xml`
**Style:** Dark Grey to Black (Minimal Professional)
- Start: `#2D3436` (Dark grey)
- Center: `#1E272E` (Deeper grey)
- End: `#0A0E13` (Almost black)

This is the **default** gradient matching your minimal black/white/grey theme.

---

## Alternative Gradients (Optional)

### Option 1: Charcoal Black
**File:** `wallet_gradient_alt1.xml`
- Start: `#3A3A3A`
- Center: `#242424`
- End: `#0F0F0F`
**Look:** Most minimal, pure charcoal to black

### Option 2: Slate Grey
**File:** `wallet_gradient_alt2.xml`
- Start: `#424242`
- Center: `#2C2C2C`
- End: `#1A1A1A`
**Look:** Elegant slate grey, balanced

### Option 3: Light to Dark Grey
**File:** `wallet_gradient_alt3.xml`
- Start: `#5A5A5A`
- Center: `#3D3D3D`
- End: `#252525`
**Look:** Softer, lighter grey tone

---

## How to Switch Gradients

To change the wallet card gradient, edit line 80 in:
`fragment_wallet_new.xml`

Change:
```xml
android:background="@drawable/wallet_card_gradient"
```

To one of:
```xml
android:background="@drawable/wallet_gradient_alt1"
android:background="@drawable/wallet_gradient_alt2"
android:background="@drawable/wallet_gradient_alt3"
```

---

## Wallet Card Features

✅ Minimal black/grey gradient (matches app theme)
✅ Large prominent balance (48sp)
✅ Clean "Gridee Wallet" branding
✅ White "Add Money" button
✅ "Your Parking Wallet" tagline
✅ "Updated now" timestamp
✅ Professional spacing and typography
✅ 20dp rounded corners
✅ 12dp elevation for depth

---

## Perfect for:
- Minimal/modern design aesthetic
- Professional wallet appearance
- Easy readability with white text on dark background
- Consistent with black/white/grey app theme
