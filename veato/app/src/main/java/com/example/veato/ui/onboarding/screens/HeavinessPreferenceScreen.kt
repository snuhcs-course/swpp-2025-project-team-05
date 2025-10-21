package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.veato.data.model.HeavinessLevel
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeavinessPreferenceScreen(
    heavinessLevel: HeavinessLevel,
    onUpdate: (HeavinessLevel) -> Unit,
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
                title = "Meal Heaviness",
                subtitle = "Do you prefer light or filling meals?"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            PreferenceInfoCard(
                text = "Influences portion size and meal type recommendations"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            PreferenceCard(title = "Your Preference") {
                // Visual representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (heavinessLevel) {
                            HeavinessLevel.LIGHT -> "🥗"
                            HeavinessLevel.MEDIUM -> "🍱"
                            HeavinessLevel.HEAVY -> "🍔"
                        },
                        style = MaterialTheme.typography.displayMedium
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

                // Segmented button / Chip selection
                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)
                ) {
                    HeavinessLevel.entries.forEach { level ->
                        FilterChip(
                            selected = heavinessLevel == level,
                            onClick = { onUpdate(level) },
                            label = {
                                Column {
                                    Text(
                                        text = level.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = level.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        NavigationButtons(
            onPrevious = onPrevious,
            onNext = onNext,
            showPrevious = true
        )
    }
}
