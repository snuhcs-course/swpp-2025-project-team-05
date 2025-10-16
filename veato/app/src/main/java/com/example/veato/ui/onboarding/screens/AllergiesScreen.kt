package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.veato.data.model.Allergen
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@Composable
fun AllergiesScreen(
    selectedAllergies: List<Allergen>,
    onUpdate: (List<Allergen>) -> Unit,
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
                title = "Food Allergies",
                subtitle = "Critical for your safety - these will be completely excluded"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(Dimensions.paddingMedium),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)
                ) {
                    Text(text = "⚠️", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Select ALL allergens to ensure your safety",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            PreferenceCard(
                title = "Common Allergens"
            ) {
                MultiSelectChipGroup(
                    items = Allergen.entries.filter { it != Allergen.CUSTOM },
                    selectedItems = selectedAllergies,
                    onSelectionChange = onUpdate,
                    itemLabel = { it.displayName },
                    itemsPerRow = 2
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
