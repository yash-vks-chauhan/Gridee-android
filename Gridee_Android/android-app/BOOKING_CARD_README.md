# Booking Card Implementation Guide

## Overview
Clean and minimal booking card design with a focus on essential information using black, white, and grey color scheme.

## Design Goals
- Essential information first
- Clear visual hierarchy
- Efficient space utilization
- Professional appearance
- Minimalistic design

## Features
- Nested card structure (white outer, grey inner)
- Status indicators
- Icon-based information display
- Interactive elements
- Smooth transitions

## Color Palette
```
Primary Colors:
- White (#FFFFFF) - Main card background
- Light Grey (#F5F5F5) - Inner card background
- Dark Grey (#424242) - Primary text
- Medium Grey (#757575) - Secondary text and icons

Status Colors (Grayscale):
- Pending: #E0E0E0
- Active: #9E9E9E
- Completed: #616161
```

## Components Structure
```
BookingCard/
├── BookingCardView.kt
├── BookingStatusBar.kt
├── BookingInnerCard.kt
├── BookingDetailsSection.kt
└── layouts/
    ├── booking_card_layout.xml
    ├── booking_status_bar.xml
    ├── booking_inner_card.xml
    └── booking_details_section.xml
```

## Usage Example
```kotlin
// In your Fragment or Activity
binding.bookingCardContainer.apply {
    bookingCard.setBookingData(bookingData)
    bookingCard.setOnActionClickListener { action ->
        // Handle actions
    }
}
```

## Implementation Checklist
- [ ] Base card layout
- [ ] Inner grey card
- [ ] Status indicators
- [ ] Icon integration
- [ ] Typography styles
- [ ] Animations
- [ ] Accessibility
- [ ] Testing

## Important Notes
1. Maintain 8dp grid system
2. Use consistent padding/margin
3. Follow Material Design guidelines
4. Implement proper state handling
5. Ensure smooth transitions

## Dependencies
```gradle
implementation 'com.google.android.material:material:1.9.0'
```

## Accessibility
- Proper content descriptions
- Touch target sizes (48dp)
- Sufficient contrast ratios
- State change announcements
