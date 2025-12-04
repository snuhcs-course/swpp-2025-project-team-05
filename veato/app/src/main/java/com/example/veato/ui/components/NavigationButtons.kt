package com.example.veato.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)
    ) {
        // Previous button
        if (showPrevious) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                contentPadding = PaddingValues(
                    horizontal = Dimensions.paddingSmall,
                    vertical = Dimensions.paddingSmall
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = previousLabel,
                    maxLines = 1
                )
            }
        }

        // Skip button (optional)
        if (showSkip && onSkip != null) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = "Skip",
                    maxLines = 1
                )
            }
        }

        // Next button
        if (showNext) {
            Button(
                onClick = onNext,
                enabled = nextEnabled,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                contentPadding = PaddingValues(
                    horizontal = Dimensions.paddingSmall,
                    vertical = Dimensions.paddingSmall
                )
            ) {
                Text(
                    text = nextLabel,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                )
            }
        }
    }
}
