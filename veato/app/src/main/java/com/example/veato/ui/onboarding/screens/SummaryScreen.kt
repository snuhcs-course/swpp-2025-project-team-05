package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.veato.data.model.UserProfile
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@Composable
fun SummaryScreen(
    profile: UserProfile,
    onSave: () -> Unit,
    onEdit: (Int) -> Unit,
    currentStep: Int,
    totalSteps: Int,
    isSaving: Boolean,
    saveError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.paddingLarge)
    ) {
        OnboardingProgressIndicator(
            currentStep = currentStep,
            totalSteps = totalSteps
        )

        Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

        SectionHeader(
            title = "Review & Finish",
            subtitle = "Review your preferences before completing setup"
        )

        Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Hard Constraints Section
            if (profile.hardConstraints.hasConstraints()) {
                SummaryCard(
                    title = "Hard Constraints",
                    subtitle = "These will never appear in recommendations"
                ) {
                    if (profile.hardConstraints.dietaryRestrictions.isNotEmpty()) {
                        SummaryItem(
                            label = "Dietary",
                            value = profile.hardConstraints.dietaryRestrictions.joinToString { it.displayName }
                        )
                    }
                    if (profile.hardConstraints.allergies.isNotEmpty()) {
                        SummaryItem(
                            label = "Allergies",
                            value = profile.hardConstraints.allergies.joinToString { it.displayName }
                        )
                    }
                    if (profile.hardConstraints.avoidIngredients.isNotEmpty()) {
                        SummaryItem(
                            label = "Avoid",
                            value = profile.hardConstraints.avoidIngredients.joinToString()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.paddingMedium))
            }

            // Soft Preferences Section
            SummaryCard(
                title = "Preferences",
                subtitle = "Used to rank and prioritize recommendations"
            ) {
                if (profile.softPreferences.favoriteCuisines.isNotEmpty()) {
                    SummaryItem(
                        label = "Cuisines",
                        value = profile.softPreferences.favoriteCuisines.joinToString { it.displayName }
                    )
                }
                SummaryItem(
                    label = "Spice",
                    value = profile.softPreferences.spiceTolerance.displayName
                )
            }

            if (saveError != null) {
                Spacer(modifier = Modifier.height(Dimensions.paddingMedium))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Error: $saveError",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Dimensions.paddingMedium)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

        Button(
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(Dimensions.paddingSmall))
                Text("Saving...")
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(Dimensions.paddingSmall))
                Text("Complete Setup")
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.paddingMedium)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimensions.paddingSmall))
            content()
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.7f)
        )
    }
}
