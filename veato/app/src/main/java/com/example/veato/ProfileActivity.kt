package com.example.veato

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veato.data.local.ProfileDataStoreImpl
import com.example.veato.data.model.Allergen
import com.example.veato.data.model.CuisineType
import com.example.veato.data.model.DietaryType
import com.example.veato.data.model.SpiceLevel
import com.example.veato.data.model.UserProfile
import com.example.veato.data.repository.UserProfileRepositoryImpl
import com.example.veato.ui.components.MultiSelectChipGroup
import com.example.veato.ui.components.PreferenceSlider
import com.example.veato.ui.profile.ProfileViewModel
import com.example.veato.ui.profile.ProfileViewModelFactory
import com.example.veato.ui.theme.VeatoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


private val db by lazy { Firebase.firestore }


class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VeatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background)
                {
                    ProfileScreen()
                }
            }
        }
    }

}


@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"


    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            repository = UserProfileRepositoryImpl(
                ProfileDataStoreImpl(context)
            ),
            userId = userId
        )
    )
    val state by viewModel.state.collectAsState()

    // picture and name
    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Spacer(Modifier.height(24.dp))


        NameIdPictureBox("TODO NAME", state.userProfile?.userId, state.userProfile != null)
        // name id picture


        Spacer(Modifier.height(24.dp))


        // tab select button
        TabRow(selectedTabIndex = state.tab) {
            Tab(selected = (state.tab == 0), onClick = { viewModel.changeTab(0) }, text = { Text("Preferences") })
            Tab(selected = (state.tab == 1), onClick = { if (!state.isEditing) viewModel.changeTab(1) else Toast.makeText(context, "Save your preferences first", Toast.LENGTH_SHORT).show() }, text = { Text("History") })
        }

        Spacer(Modifier.height(16.dp))

        // actual tab show
        when {
            state.isBusy -> LoadingCircle()
            state.userProfile == null -> Text("No profile found.")
            state.tab == 0 -> PreferencesTab(
                userProfile = state.userProfile!!,
                updateDietRestriction = viewModel::updateDietaryRestrictions,
                updateAllergies = viewModel::updateAllergies,
                updateFavoriteCuisines = viewModel::updateFavoriteCuisines,
                updateSpiceTolerance = viewModel::updateSpiceTolerance,
                updateProfile = viewModel::updateProfile,
                isEditing = state.isEditing,
                toggleMode = viewModel::toggleEditing
            )
            state.tab == 1 -> HistoryTab()
        }
    }
}

@Composable
private fun NameIdPictureBox(name: String? = "", userId: String? = "", available:Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Column {
            Text( if (available) name ?: "USER_NAME" else "NOT AVAILABLE", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(if (available) "@${userId ?: "USER_ID"}" else userId ?: "NOT AVAILABLE", color = Color.Gray)
        }
    }
}

@Composable
private fun LoadingCircle() {
    Box(Modifier.fillMaxSize()) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}


@Composable
private fun PreferencesTab(
    userProfile: UserProfile,
    updateDietRestriction: (List<DietaryType>) -> Unit,
    updateAllergies: (List<Allergen>) -> Unit,
    updateFavoriteCuisines: (List<CuisineType>) -> Unit,
    updateSpiceTolerance: (SpiceLevel) -> Unit,
    isEditing: Boolean,
    toggleMode: () -> Unit,


    updateProfile: () -> Unit
) {


Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.verticalScroll(rememberScrollState())
) {
        EditPreferenceButton(
            isEditMode = isEditing,
            onClick = {
                if (isEditing) {
                    updateProfile()
                }
                toggleMode()
            }
        )
        Text("Hard Constraints", style = MaterialTheme.typography.titleLarge)
        DietRestrictionBox(
            enabled = isEditing,
            selectedItems = userProfile.hardConstraints.dietaryRestrictions,
            onUpdate = updateDietRestriction
        )
         AllergiesBox(
             enabled = isEditing,
             selectedItems = userProfile.hardConstraints.allergies,
             onUpdate = updateAllergies
         )
         Text("Soft Constraints", style = MaterialTheme.typography.titleLarge)
         FavoriteCuisinesBox(
             enabled = isEditing,
             selectedItems = userProfile.softPreferences.favoriteCuisines,
             onUpdate = updateFavoriteCuisines
         )
         SpiceToleranceBox(
             enabled = isEditing,
             value = userProfile.softPreferences.spiceTolerance,
             onUpdate = updateSpiceTolerance
         )
    }
}

@Composable
fun DietRestrictionBox(
    enabled: Boolean,
    selectedItems: List<DietaryType> = emptyList(),
    onUpdate: (List<DietaryType>) -> Unit
) {
    Column {
        Text("① Dietary Restrictions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        MultiSelectChipGroup(
            items = DietaryType.entries.filter { it != DietaryType.CUSTOM },
            selectedItems = selectedItems,
            onSelectionChange = onUpdate,
            itemLabel = { it.displayName },
            itemsPerRow = 2,
            enabled = enabled
        )
    }
}

@Composable
fun AllergiesBox(
    enabled: Boolean,
    selectedItems: List<Allergen> = emptyList(),
    onUpdate: (List<Allergen>) -> Unit
) {
    Column {
        Text("② Food Allergies", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        MultiSelectChipGroup(
            items = Allergen.entries.filter { it != Allergen.CUSTOM },
            selectedItems = selectedItems,
            onSelectionChange = onUpdate,
            itemLabel = { it.displayName },
            itemsPerRow = 2,
            enabled = enabled
        )
    }
}


@Composable
fun FavoriteCuisinesBox(
    enabled: Boolean,
    selectedItems: List<CuisineType> = emptyList(),
    onUpdate: (List<CuisineType>) -> Unit
) {
    Column {
        Text("① Favorite Cuisines", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        MultiSelectChipGroup(
            items = CuisineType.entries,
            selectedItems = selectedItems,
            onSelectionChange = onUpdate,
            itemLabel = { it.displayName },
            itemsPerRow = 2,
            enabled = enabled
        )
    }
}

@Composable
fun SpiceToleranceBox(
    enabled: Boolean,
    value: SpiceLevel,
    onUpdate: (SpiceLevel) -> Unit
) {
    Column {
        Text("② Spice Tolerance", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        PreferenceSlider(
            value = value.level.toFloat(),
            onValueChange = { onUpdate(SpiceLevel.fromLevel(it.toInt())) },
            valueRange = 1f..5f,
            enabled = enabled,
            steps = 3,
            label = "",
            startLabel = "No Spice",
            endLabel = "Extra Spicy"
        )
    }
}





@Composable
private fun EditPreferenceButton(
    isEditMode: Boolean,
    onClick: () -> Unit
) {
    Button(onClick = onClick) {
        Text(if (isEditMode) "Save Changes" else "Edit Preference", style = MaterialTheme.typography.labelSmall)
    }
}


@Composable
private fun HistoryTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("to be added later")
    }
}

