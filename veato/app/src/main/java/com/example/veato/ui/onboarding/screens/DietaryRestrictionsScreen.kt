package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.veato.data.model.DietaryType
import com.example.veato.data.model.getIconResource
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@Composable
fun DietaryRestrictionsScreen(
    selectedRestrictions: List<DietaryType>,
    onUpdate: (List<DietaryType>) -> Unit,
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
                title = "Dietary Restrictions",
                subtitle = "Select all that apply to you"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            ConstraintWarningBadge(
                text = "We'll NEVER recommend meals that violate these"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            PreferenceCard(
                title = "Your Restrictions"
            ) {
                MultiSelectChipGroup(
                    items = DietaryType.entries,
                    selectedItems = selectedRestrictions,
                    onSelectionChange = onUpdate,
                    itemLabel = { it.displayName },
                    itemIcon = { it.getIconResource() },
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
