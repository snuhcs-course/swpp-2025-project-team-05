package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.veato.data.model.BudgetRange
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@Composable
fun BudgetPreferenceScreen(
    budgetRange: BudgetRange,
    onUpdate: (BudgetRange) -> Unit,
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    var currentRange by remember { mutableStateOf(budgetRange.minPrice..budgetRange.maxPrice) }

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
                title = "Typical Budget",
                subtitle = "What's your usual budget range?"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            PreferenceInfoCard(
                text = "Meals in this range ranked higher (different from hard cap)"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            PreferenceCard(title = "Your Preferred Price Range") {
                IntRangeSlider(
                    value = currentRange,
                    onValueChange = { newRange ->
                        currentRange = newRange
                        onUpdate(BudgetRange(newRange.first, newRange.last))
                    },
                    valueRange = 3000f..50000f,
                    label = "Budget Range",
                    formatValue = { "₩${it / 1000}k" }
                )

                Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

                // Preset ranges
                Text(
                    text = "Or choose a preset:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Dimensions.paddingSmall))

                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)
                ) {
                    listOf(
                        "Budget" to BudgetRange.BUDGET,
                        "Moderate" to BudgetRange.MODERATE,
                        "Premium" to BudgetRange.PREMIUM,
                        "Fine Dining" to BudgetRange.FINE_DINING
                    ).forEach { (name, preset) ->
                        OutlinedButton(
                            onClick = {
                                currentRange = preset.minPrice..preset.maxPrice
                                onUpdate(preset)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name)
                                Text("₩${preset.minPrice / 1000}k - ₩${preset.maxPrice / 1000}k")
                            }
                        }
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
