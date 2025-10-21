package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@Composable
fun BudgetCapScreen(
    budgetCap: Int?,
    onUpdate: (Int?) -> Unit,
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    var budgetText by remember { mutableStateOf(budgetCap?.toString() ?: "") }
    var hasError by remember { mutableStateOf(false) }

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
                title = "Budget Cap",
                subtitle = "Maximum amount per meal - we won't suggest anything above this"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            ConstraintWarningBadge(
                text = "Hard limit - meals above this won't be shown"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            PreferenceCard(title = "Set Your Maximum Budget (Optional)") {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { newValue ->
                        budgetText = newValue
                        val amount = newValue.toIntOrNull()
                        hasError = newValue.isNotBlank() && (amount == null || amount <= 0)
                        onUpdate(amount)
                    },
                    label = { Text("Maximum budget") },
                    placeholder = { Text("e.g., 20000") },
                    prefix = { Text("₩ ") },
                    suffix = { Text("per meal") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasError,
                    supportingText = {
                        if (hasError) {
                            Text("Please enter a valid positive amount")
                        } else if (budgetText.isBlank()) {
                            Text("Leave blank for no hard limit")
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

                // Quick preset buttons
                Text(
                    text = "Quick presets:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Dimensions.paddingSmall))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(10000, 15000, 20000, 30000).forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                budgetText = preset.toString()
                                onUpdate(preset)
                                hasError = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("₩${preset / 1000}k")
                        }
                    }
                }
            }
        }

        NavigationButtons(
            onPrevious = onPrevious,
            onNext = onNext,
            showPrevious = true,
            nextEnabled = !hasError,
            showSkip = budgetText.isBlank(),
            onSkip = { onNext() }
        )
    }
}
