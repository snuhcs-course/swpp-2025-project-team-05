package com.example.veato.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for UserProfile data model
 *
 * Following AAA pattern (Arrange, Act, Assert) and test naming convention:
 * methodUnderTest_scenario_expectedResult
 *
 * Testing strategies used:
 * - Equivalence testing: Group inputs into valid/invalid classes
 * - Boundary testing: Test edge cases (empty strings, null, min/max values)
 */
class UserProfileTest {

    // ============================================================
    // Validation Tests (Equivalence + Boundary Testing)
    // ============================================================

    @Test
    fun validate_withValidProfile_returnsTrue() {
        // Arrange: Valid profile (equivalence class: valid inputs)
        val profile = UserProfile(
            userId = "user123",
            userName = "testuser",
            hardConstraints = HardConstraints.EMPTY,
            softPreferences = SoftPreferences.DEFAULT
        )

        // Act
        val result = profile.validate()

        // Assert
        assertTrue(result)
    }

    @Test
    fun validate_withEmptyUserId_returnsFalse() {
        // Arrange: Boundary test - empty userId
        val profile = UserProfile(
            userId = "",
            userName = "testuser",
            hardConstraints = HardConstraints.EMPTY
        )

        // Act
        val result = profile.validate()

        // Assert
        assertFalse(result)
    }

    @Test
    fun validate_withBlankUserId_returnsFalse() {
        // Arrange: Boundary test - blank userId (whitespace only)
        val profile = UserProfile(
            userId = "   ",
            userName = "testuser",
            hardConstraints = HardConstraints.EMPTY
        )

        // Act
        val result = profile.validate()

        // Assert
        assertFalse(result)
    }

    @Test
    fun validate_withValidUserIdAndConstraints_returnsTrue() {
        // Arrange: Valid userId with constraints
        val constraints = HardConstraints(
            dietaryRestrictions = listOf(DietaryType.VEGETARIAN),
            allergies = listOf(Allergen.PEANUTS),
            avoidIngredients = listOf("Cilantro")
        )
        val profile = UserProfile(
            userId = "user123",
            hardConstraints = constraints
        )

        // Act
        val result = profile.validate()

        // Assert
        // Note: HardConstraints.validate() currently always returns true
        assertTrue(result)
    }

    @Test
    fun validate_withMinimalValidData_returnsTrue() {
        // Arrange: Boundary test - minimum valid data
        val profile = UserProfile(
            userId = "a", // Single character userId
            hardConstraints = HardConstraints.EMPTY
        )

        // Act
        val result = profile.validate()

        // Assert
        assertTrue(result)
    }

    @Test
    fun validate_withLongUserId_returnsTrue() {
        // Arrange: Boundary test - very long userId
        val longUserId = "a".repeat(1000)
        val profile = UserProfile(
            userId = longUserId,
            hardConstraints = HardConstraints.EMPTY
        )

        // Act
        val result = profile.validate()

        // Assert
        assertTrue(result)
    }

    // ============================================================
    // JSON Serialization Tests
    // ============================================================

    @Test
    fun toJson_withValidProfile_returnsCorrectJson() {
        // Arrange
        val profile = UserProfile(
            userId = "user123",
            userName = "testuser",
            fullName = "Test User",
            hardConstraints = HardConstraints.EMPTY,
            softPreferences = SoftPreferences.DEFAULT,
            isOnboardingComplete = true
        )

        // Act
        val json = profile.toJson()

        // Assert
        assertTrue(json.contains("\"userId\":\"user123\""))
        assertTrue(json.contains("\"userName\":\"testuser\""))
        assertTrue(json.contains("\"fullName\":\"Test User\""))
        assertTrue(json.contains("\"isOnboardingComplete\":true"))
    }

    @Test
    fun fromJson_withValidJson_returnsCorrectProfile() {
        // Arrange
        val originalProfile = UserProfile(
            userId = "user123",
            userName = "testuser",
            fullName = "Test User",
            isOnboardingComplete = true
        )
        val json = originalProfile.toJson()

        // Act
        val deserializedProfile = UserProfile.fromJson(json)

        // Assert
        assertEquals(originalProfile.userId, deserializedProfile.userId)
        assertEquals(originalProfile.userName, deserializedProfile.userName)
        assertEquals(originalProfile.fullName, deserializedProfile.fullName)
        assertEquals(originalProfile.isOnboardingComplete, deserializedProfile.isOnboardingComplete)
    }

    @Test
    fun toJsonAndFromJson_roundTrip_preservesAllData() {
        // Arrange: Profile with complex data
        val originalProfile = UserProfile(
            userId = "user123",
            userName = "testuser",
            fullName = "Test User",
            hardConstraints = HardConstraints(
                dietaryRestrictions = listOf(DietaryType.VEGETARIAN, DietaryType.GLUTEN_FREE),
                allergies = listOf(Allergen.PEANUTS, Allergen.SHELLFISH),
                avoidIngredients = listOf("Cilantro", "Mushrooms")
            ),
            softPreferences = SoftPreferences(
                favoriteCuisines = listOf(CuisineType.KOREAN, CuisineType.JAPANESE),
                spiceTolerance = SpiceLevel.MEDIUM,
                mealTypePreferences = listOf(MealType.RICE_BASED),
                portionPreference = PortionSize.LARGE
            ),
            isOnboardingComplete = true
        )

        // Act: Serialize and deserialize
        val json = originalProfile.toJson()
        val deserializedProfile = UserProfile.fromJson(json)

        // Assert: All fields preserved
        assertEquals(originalProfile.userId, deserializedProfile.userId)
        assertEquals(originalProfile.hardConstraints, deserializedProfile.hardConstraints)
        assertEquals(originalProfile.softPreferences, deserializedProfile.softPreferences)
        assertEquals(originalProfile.isOnboardingComplete, deserializedProfile.isOnboardingComplete)
    }

    @Test
    fun fromJson_withEmptyConstraints_deserializesCorrectly() {
        // Arrange: Boundary test - empty constraints
        val originalProfile = UserProfile(
            userId = "user123",
            hardConstraints = HardConstraints.EMPTY,
            softPreferences = SoftPreferences.DEFAULT
        )
        val json = originalProfile.toJson()

        // Act
        val deserializedProfile = UserProfile.fromJson(json)

        // Assert
        assertFalse(deserializedProfile.hardConstraints.hasConstraints())
        assertEquals(SoftPreferences.DEFAULT, deserializedProfile.softPreferences)
    }

    // ============================================================
    // Factory Method Tests (createNew)
    // ============================================================

    @Test
    fun createNew_withValidUserId_createsProfileWithCorrectDefaults() {
        // Arrange
        val userId = "newUser123"

        // Act
        val profile = UserProfile.createNew(userId)

        // Assert
        assertEquals(userId, profile.userId)
        assertEquals(HardConstraints.EMPTY, profile.hardConstraints)
        assertEquals(SoftPreferences.DEFAULT, profile.softPreferences)
        assertFalse(profile.isOnboardingComplete)
    }

    @Test
    fun createNew_withEmptyUserId_createsProfileWithEmptyUserId() {
        // Arrange: Boundary test - empty userId
        val userId = ""

        // Act
        val profile = UserProfile.createNew(userId)

        // Assert
        assertEquals("", profile.userId)
        assertFalse(profile.validate()) // Should fail validation
    }

    @Test
    fun createNew_multipleInvocations_createsDifferentObjects() {
        // Arrange
        val userId = "user123"

        // Act
        val profile1 = UserProfile.createNew(userId)
        val profile2 = UserProfile.createNew(userId)

        // Assert: Different instances but same values
        assertTrue(profile1 !== profile2) // Different object references
        assertEquals(profile1.userId, profile2.userId)
    }

    // ============================================================
    // Timestamp Tests
    // ============================================================

    @Test
    fun withUpdatedTimestamp_updatesTimestamp() {
        // Arrange
        val originalProfile = UserProfile(
            userId = "user123",
            updatedAt = 1000L
        )
        Thread.sleep(10) // Ensure time passes

        // Act
        val updatedProfile = originalProfile.withUpdatedTimestamp()

        // Assert
        assertTrue(updatedProfile.updatedAt > originalProfile.updatedAt)
        assertEquals(originalProfile.userId, updatedProfile.userId)
    }

    @Test
    fun withUpdatedTimestamp_preservesOtherFields() {
        // Arrange
        val originalProfile = UserProfile(
            userId = "user123",
            userName = "testuser",
            fullName = "Test User",
            hardConstraints = HardConstraints(
                dietaryRestrictions = listOf(DietaryType.VEGETARIAN)
            ),
            isOnboardingComplete = true,
            updatedAt = 1000L
        )

        // Act
        val updatedProfile = originalProfile.withUpdatedTimestamp()

        // Assert
        assertEquals(originalProfile.userId, updatedProfile.userId)
        assertEquals(originalProfile.userName, updatedProfile.userName)
        assertEquals(originalProfile.fullName, updatedProfile.fullName)
        assertEquals(originalProfile.hardConstraints, updatedProfile.hardConstraints)
        assertEquals(originalProfile.isOnboardingComplete, updatedProfile.isOnboardingComplete)
        assertNotEquals(originalProfile.updatedAt, updatedProfile.updatedAt)
    }

    // ============================================================
    // Data Class Properties Tests
    // ============================================================

    @Test
    fun copy_withModifiedField_createsNewInstanceWithChanges() {
        // Arrange
        val originalProfile = UserProfile(
            userId = "user123",
            userName = "oldname"
        )

        // Act
        val copiedProfile = originalProfile.copy(userName = "newname")

        // Assert
        assertEquals("user123", copiedProfile.userId)
        assertEquals("newname", copiedProfile.userName)
        assertEquals("oldname", originalProfile.userName) // Original unchanged
    }

    @Test
    fun equals_withSameData_returnsTrue() {
        // Arrange
        val profile1 = UserProfile(
            userId = "user123",
            userName = "testuser"
        )
        val profile2 = UserProfile(
            userId = "user123",
            userName = "testuser"
        )

        // Act & Assert
        assertEquals(profile1, profile2)
    }

    @Test
    fun equals_withDifferentData_returnsFalse() {
        // Arrange
        val profile1 = UserProfile(
            userId = "user123",
            userName = "testuser1"
        )
        val profile2 = UserProfile(
            userId = "user123",
            userName = "testuser2"
        )

        // Act & Assert
        assertNotEquals(profile1, profile2)
    }

    // ============================================================
    // Edge Case Tests
    // ============================================================

    @Test
    fun validate_withSpecialCharactersInUserId_returnsTrue() {
        // Arrange: Equivalence test - special characters
        val profile = UserProfile(
            userId = "user-123_@email.com",
            hardConstraints = HardConstraints.EMPTY
        )

        // Act
        val result = profile.validate()

        // Assert
        assertTrue(result)
    }

    @Test
    fun validate_withUnicodeInUserId_returnsTrue() {
        // Arrange: Equivalence test - Unicode characters
        val profile = UserProfile(
            userId = "사용자123",
            hardConstraints = HardConstraints.EMPTY
        )

        // Act
        val result = profile.validate()

        // Assert
        assertTrue(result)
    }
}
