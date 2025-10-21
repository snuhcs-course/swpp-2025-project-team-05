package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.veato.data.model.CuisineType
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@Composable
fun FavoriteCuisinesScreen(
    selectedCuisines: List<CuisineType>,
    onUpdate: (List<CuisineType>) -> Unit,
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
                title = "Favorite Cuisines",
                subtitle = "What do you usually enjoy?"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            PreferenceInfoCard(
                text = "Helps us rank recommendations - doesn't exclude others"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            PreferenceCard(
                title = "Select Your Favorites"
            ) {
                MultiSelectChipGroup(
                    items = CuisineType.entries,
                    selectedItems = selectedCuisines,
                    onSelectionChange = onUpdate,
                    itemLabel = { "${it.koreanName} ${it.displayName}" },
                    itemsPerRow = 2
                )
            }

            if (selectedCuisines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimensions.paddingMedium))
                Text(
                    text = "Selected ${selectedCuisines.size} cuisine${if (selectedCuisines.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        NavigationButtons(
            onPrevious = onPrevious,
            onNext = onNext,
            showPrevious = true,
            showSkip = selectedCuisines.isEmpty(),
            onSkip = { onNext() }
        )
    }
}
