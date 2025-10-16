package com.example.veato.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.veato.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MultiSelectChipGroup(
    items: List<T>,
    selectedItems: List<T>,
    onSelectionChange: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String,
    itemsPerRow: Int = 2
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)
    ) {
        items.chunked(itemsPerRow).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    val isSelected = selectedItems.contains(item)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newSelection = if (isSelected) {
                                selectedItems - item
                            } else {
                                selectedItems + item
                            }
                            onSelectionChange(newSelection)
                        },
                        label = {
                            Text(
                                text = itemLabel(item),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if row is not full
                repeat(itemsPerRow - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
