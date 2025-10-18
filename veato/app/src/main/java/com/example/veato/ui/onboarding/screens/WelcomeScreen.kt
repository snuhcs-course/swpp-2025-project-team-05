package com.example.veato.ui.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.veato.ui.components.NavigationButtons
import com.example.veato.ui.theme.Dimensions

@Composable
fun WelcomeScreen(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(Dimensions.paddingExtraLarge))

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingLarge)
        ) {
            // Logo/Brand
            Text(
                text = "Veato",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            // Title
            Text(
                text = "Welcome to Veato!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            // Description
            Text(
                text = "Let's personalize your food recommendations.\n\nWe'll help you and your group find the perfect meal every time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimensions.paddingMedium)
            )

            Spacer(modifier = Modifier.height(Dimensions.paddingMedium))

            // Info cards
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.paddingMedium)
                ) {
                    Text(
                        text = "This will take about 2 minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(Dimensions.paddingSmall))
                    Text(
                        text = "• Set your food constraints and preferences\n• Help us understand what you like\n• Get personalized recommendations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Navigation
        NavigationButtons(
            onPrevious = {},
            onNext = onNext,
            showPrevious = false,
            nextLabel = "Get Started"
        )
    }
}
