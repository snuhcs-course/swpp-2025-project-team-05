package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.veato.data.model.MealType
import com.example.veato.data.model.PortionSize
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedPreferencesScreen(
    mealTypes: List<MealType>,
    portionSize: PortionSize?,
    onUpdateMealTypes: (List<MealType>) -> Unit,
    onUpdatePortionSize: (PortionSize?) -> Unit,
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
                title = "Advanced Preferences",
                subtitle = "Optional: Fine-tune your meal recommendations"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            PreferenceInfoCard(
                text = "All optional - skip if you have no preference"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            // Meal Type Preferences
            PreferenceCard(title = "Preferred Meal Types") {
                MultiSelectChipGroup(
                    items = MealType.entries,
                    selectedItems = mealTypes,
                    onSelectionChange = onUpdateMealTypes,
                    itemLabel = { "${it.koreanName} ${it.displayName}" },
                    itemsPerRow = 2
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            // Portion Size Preference
            PreferenceCard(title = "Preferred Portion Size") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PortionSize.entries.forEach { size ->
                        FilterChip(
                            selected = portionSize == size,
                            onClick = {
                                onUpdatePortionSize(if (portionSize == size) null else size)
                            },
                            label = { Text(size.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (portionSize == null) {
                    Spacer(modifier = Modifier.height(Dimensions.paddingSmall))
                    Text(
                        text = "No preference selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        NavigationButtons(
            onPrevious = onPrevious,
            onNext = onNext,
            showPrevious = true,
            showSkip = mealTypes.isEmpty() && portionSize == null,
            onSkip = { onNext() },
            nextLabel = "Continue"
        )
    }
}
