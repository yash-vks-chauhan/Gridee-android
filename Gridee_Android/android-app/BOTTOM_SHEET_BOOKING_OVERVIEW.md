## Booking Overview Bottom Sheet Notes

### Overview
- Entry point is `BookingsFragmentNew.showBookingDetails(...)`.
- Inflates `bottom_sheet_booking_overview.xml` via `BottomSheetBookingOverviewBinding`.
- Uses `BottomSheetDialog` with `R.style.BottomSheetDialogTheme`.

### Blur + Frosted Overlay
| Behaviour | OS Support | Details |
|-----------|------------|---------|
| True blur | Android 12L+ (API 31+) | `RenderEffect.createBlurEffect` applied to the fragment root. |
| Legacy fallback | Android 11 and below | Semi-transparent overlay tint (`@color/booking_sheet_overlay`). |

- Overlay view is injected via `toggleBackgroundBlur(true)` and removed when the sheet is dismissed.
- Overlay consumes clicks to keep the sheet modal.

### Layout Structure
```
bottom_sheet_booking_overview.xml
├── FrameLayout (close button floating at top right)
├── ScrollView (full-height, padding 16dp)
│   └── MaterialCardView (booking summary, large radius)
│       └── LinearLayout (header, detail rows, footer actions)
└── Buttons (Cancel/Close) live inside card footer for iOS-style layout
```

### Styling Tokens
- Overlay tint: `booking_sheet_overlay`
- Card surface: `booking_sheet_card_surface`
- Divider: `booking_sheet_divider`
- Icon tint: `booking_sheet_icon_tint`
- Chip backgrounds reuse `status_outlined_*` drawables.

### Notable Behaviours
- Sheet forces expanded state (`BottomSheetBehavior#STATE_EXPANDED`) and skips collapsed.
- Window blur/dim is cleared to keep only the custom frosted overlay.
- Cancel button currently posts a `showToast` placeholder; wire it to backend when ready.

### Troubleshooting
- **Blur not visible on Android 12+**: check `RenderEffect` hardware acceleration (should be `true` by default). Ensure device/emu is API 31+.
- **Overlay sticks after dismiss**: verify `toggleBackgroundBlur(false)` runs (see `setOnDismissListener` and `onDestroyView()` cleanup).
- **Buttons clipped on small screens**: layout is inside `ScrollView` with `fillViewport="true"`; adjust margins/padding if content overflows.

### Future Enhancements
- Make overlay tap dismiss the sheet if UX requires it.
- Add motion spec for card elevation/translation on open (currently default Material animation).
- Extract `toggleBackgroundBlur` into reusable helper if other screens need the same effect.
