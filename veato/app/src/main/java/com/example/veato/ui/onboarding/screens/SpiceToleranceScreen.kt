package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.veato.data.model.SpiceLevel
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@Composable
fun SpiceToleranceScreen(
    spiceLevel: SpiceLevel,
    onUpdate: (SpiceLevel) -> Unit,
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.paddingLarge),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            OnboardingProgressIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            SectionHeader(
                title = "Spice Preference",
                subtitle = "How spicy do you like your food?"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            PreferenceInfoCard(
                text = "Helps rank menu options - doesn't exclude mild food"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            PreferenceCard(title = "Your Spice Tolerance") {
                PreferenceSlider(
                    value = spiceLevel.level.toFloat(),
                    onValueChange = { newValue ->
                        onUpdate(SpiceLevel.fromLevel(newValue.toInt()))
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    label = spiceLevel.displayName,
                    startLabel = "No Spice",
                    endLabel = "Extra Spicy"
                )

                Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

                // Description
                Text(
                    text = when (spiceLevel) {
                        SpiceLevel.NONE -> "I prefer meals with no spice at all"
                        SpiceLevel.LOW -> "A little kick is okay, but keep it mild"
                        SpiceLevel.MEDIUM -> "I enjoy moderately spicy food"
                        SpiceLevel.HIGH -> "I love spicy food"
                        SpiceLevel.EXTRA -> "The spicier the better"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        NavigationButtons(
            onPrevious = onPrevious,
            onNext = onNext,
            showPrevious = true
        )
    }
}
