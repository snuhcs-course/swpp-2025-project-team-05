package com.example.veato.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.veato.ui.theme.Dimensions

@Composable
fun NavigationButtons(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    showPrevious: Boolean = true,
    showNext: Boolean = true,
    showSkip: Boolean = false,
    onSkip: (() -> Unit)? = null,
    nextEnabled: Boolean = true,
    nextLabel: String = "Next",
    previousLabel: String = "Back"
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium)
    ) {
        // Previous button
        if (showPrevious) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f)
                    .height(Dimensions.buttonHeightMedium),
                contentPadding = PaddingValues(Dimensions.paddingMedium)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                )
                Spacer(modifier = Modifier.width(Dimensions.paddingSmall))
                Text(previousLabel)
            }
        }

        // Skip button (optional)
        if (showSkip && onSkip != null) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
        }

        // Next button
        if (showNext) {
            Button(
                onClick = onNext,
                enabled = nextEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(Dimensions.buttonHeightMedium),
                contentPadding = PaddingValues(Dimensions.paddingMedium)
            ) {
                Text(nextLabel)
                Spacer(modifier = Modifier.width(Dimensions.paddingSmall))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                )
            }
        }
    }
}
