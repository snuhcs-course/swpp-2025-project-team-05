package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.veato.ui.components.*
import com.example.veato.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvoidIngredientsScreen(
    avoidList: List<String>,
    onUpdate: (List<String>) -> Unit,
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    var newIngredient by remember { mutableStateOf("") }

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
                title = "Ingredients to Avoid",
                subtitle = "Foods you absolutely refuse to eat"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            ConstraintWarningBadge(
                text = "These will be completely excluded from recommendations"
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

            // Input field to add ingredients
            OutlinedTextField(
                value = newIngredient,
                onValueChange = { newIngredient = it },
                label = { Text("Add ingredient") },
                placeholder = { Text("e.g., Mushrooms, Cilantro, Onions") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (newIngredient.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onUpdate(avoidList + newIngredient.trim())
                                newIngredient = ""
                            }
                        ) {
                            Icon(Icons.Default.Add, "Add ingredient")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            // Display selected ingredients as chips
            if (avoidList.isNotEmpty()) {
                PreferenceCard(title = "Avoiding (${avoidList.size})") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)
                    ) {
                        avoidList.chunked(2).forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { ingredient ->
                                    InputChip(
                                        selected = true,
                                        onClick = { },
                                        label = { Text(ingredient) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )
                                }
                                repeat(2 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        NavigationButtons(
            onPrevious = onPrevious,
            onNext = onNext,
            showPrevious = true,
            showSkip = avoidList.isEmpty(),
            onSkip = { onNext() }
        )
    }
}
