package com.example.veato.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.veato.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientsChipInput(
    ingredients: List<String>,
    onIngredientsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Add Ingredient"
) {
    var textFieldValue by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)
    ) {
        // Text field with add button
        if (enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val trimmedText = textFieldValue.trim()
                            if (trimmedText.isNotEmpty() && !ingredients.contains(trimmedText)) {
                                onIngredientsChange(ingredients + trimmedText)
                                textFieldValue = ""
                            }
                        }
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        val trimmedText = textFieldValue.trim()
                        if (trimmedText.isNotEmpty() && !ingredients.contains(trimmedText)) {
                            onIngredientsChange(ingredients + trimmedText)
                            textFieldValue = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add ingredient"
                    )
                }
            }
        }

        // Display existing ingredients as chips (chunked for wrapping)
        if (ingredients.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ingredients.chunked(2).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { ingredient ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    if (enabled) {
                                        onIngredientsChange(ingredients - ingredient)
                                    }
                                },
                                label = {
                                    Text(
                                        text = ingredient,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                trailingIcon = if (enabled) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove $ingredient",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if row is not full
                        repeat(2 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else if (!enabled) {
            Text(
                text = "No ingredients to avoid",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
