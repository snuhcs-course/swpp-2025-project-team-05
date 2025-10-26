package com.example.veato.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized dimensions for consistent spacing, sizing, etc.
 * Refined for a more elegant, spacious feel
 */
object Dimensions {
    // Padding - More generous spacing
    val paddingExtraSmall = 4.dp
    val paddingSmall = 12.dp       // Increased from 8dp
    val paddingMedium = 20.dp      // Increased from 16dp
    val paddingLarge = 28.dp       // Increased from 24dp
    val paddingExtraLarge = 36.dp  // Increased from 32dp

    // Corner Radius - More refined curves
    val cornerRadiusSmall = 12.dp      // Increased from 8dp
    val cornerRadiusMedium = 16.dp     // Increased from 12dp
    val cornerRadiusLarge = 20.dp      // Increased from 16dp
    val cornerRadiusExtraLarge = 28.dp // Increased from 24dp

    // Icon Sizes
    val iconSizeSmall = 18.dp      // Slightly larger
    val iconSizeMedium = 24.dp
    val iconSizeLarge = 32.dp
    val iconSizeExtraLarge = 48.dp

    // Button Heights - More prominent
    val buttonHeightSmall = 40.dp   // Increased from 36dp
    val buttonHeightMedium = 52.dp  // Increased from 48dp
    val buttonHeightLarge = 60.dp   // Increased from 56dp

    // Input Field Heights
    val inputFieldHeight = 56.dp

    // Card/Container - Subtle elevation
    val cardElevation = 1.dp        // Reduced from 2dp for subtlety
    val cardPadding = paddingLarge  // Changed to paddingLarge for more space
    val cardMinHeight = 120.dp

    // Progress Indicator
    val progressIndicatorHeight = 4.dp  // Thinner for elegance

    // Chip
    val chipHeight = 36.dp         // Slightly taller
    val chipPadding = paddingSmall

    // Divider
    val dividerThickness = 1.dp

    // Bottom Navigation
    val bottomNavHeight = 56.dp
}
