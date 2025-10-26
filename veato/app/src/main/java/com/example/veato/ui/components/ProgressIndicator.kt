package com.example.veato.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.veato.ui.theme.Dimensions

@Composable
fun OnboardingProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress text
        Text(
            text = "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

        // Progress bar with rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.progressIndicatorHeight)
                .clip(RoundedCornerShape(Dimensions.progressIndicatorHeight / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(currentStep.toFloat() / totalSteps.toFloat())
                    .clip(RoundedCornerShape(Dimensions.progressIndicatorHeight / 2))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
