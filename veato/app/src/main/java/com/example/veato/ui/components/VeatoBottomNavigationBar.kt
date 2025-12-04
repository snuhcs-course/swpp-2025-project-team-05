package com.example.veato.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veato.MyPreferencesActivity
import com.example.veato.MyTeamsActivity
import com.example.veato.ProfileActivity
import com.example.veato.R
import com.example.veato.ui.theme.VeatoNavBar

enum class NavigationScreen(val index: Int) {
    TEAMS(0),
    PREFERENCES(1),
    PROFILE(2)
}

const val EXTRA_FROM_TAB_INDEX = "from_tab_index"

/**
 * Shared bottom navigation bar with icon + label tabs
 * Tab order: My Teams → My Preferences → My Profile
 *
 * @param currentScreen The currently active screen
 */
@Composable
fun VeatoBottomNavigationBar(currentScreen: NavigationScreen) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(VeatoNavBar)
            .border(1.dp, Color(0xFFE0E0E0))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 1: My Teams (left)
        NavigationTab(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_nav_teams,
            label = "My Teams",
            isSelected = currentScreen == NavigationScreen.TEAMS,
            contentDesc = "My Teams",
            onClick = {
                if (currentScreen != NavigationScreen.TEAMS) {
                    val intent = Intent(context, MyTeamsActivity::class.java)
                    intent.putExtra(EXTRA_FROM_TAB_INDEX, currentScreen.index)
                    context.startActivity(intent)
                }
            }
        )

        // Tab 2: My Preferences (center)
        NavigationTab(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_nav_preferences,
            label = "My Preferences",
            isSelected = currentScreen == NavigationScreen.PREFERENCES,
            contentDesc = "My Preferences",
            onClick = {
                if (currentScreen != NavigationScreen.PREFERENCES) {
                    val intent = Intent(context, MyPreferencesActivity::class.java)
                    intent.putExtra(EXTRA_FROM_TAB_INDEX, currentScreen.index)
                    context.startActivity(intent)
                }
            }
        )

        // Tab 3: My Profile (right)
        NavigationTab(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_nav_profile,
            label = "My Profile",
            isSelected = currentScreen == NavigationScreen.PROFILE,
            contentDesc = "My Profile",
            onClick = {
                if (currentScreen != NavigationScreen.PROFILE) {
                    val intent = Intent(context, ProfileActivity::class.java)
                    intent.putExtra(EXTRA_FROM_TAB_INDEX, currentScreen.index)
                    context.startActivity(intent)
                }
            }
        )
    }
}

@Composable
private fun NavigationTab(
    modifier: Modifier = Modifier,
    icon: Int,
    label: String,
    isSelected: Boolean,
    contentDesc: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Active tab: primary color, Inactive: onSurfaceVariant with reduced opacity
    val tintColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true),
                onClick = onClick
            )
            .semantics {
                contentDescription = if (isSelected) {
                    "$contentDesc, selected"
                } else {
                    contentDesc
                }
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon (24dp inside ≥48dp touch target)
        Image(
            painter = painterResource(id = icon),
            contentDescription = null, // Description on parent
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(tintColor)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Label
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = fontWeight,
            color = tintColor,
            maxLines = 1
        )

        // Selected indicator (underline)
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(3.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}
