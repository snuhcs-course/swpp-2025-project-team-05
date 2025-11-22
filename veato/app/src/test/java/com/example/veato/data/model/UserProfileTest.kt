package com.example.veato.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for UserProfile data model
 *
 * Testing strategies used:
 * - Equivalence testing: Group inputs into valid/invalid classes
 * - Boundary testing: Test edge cases (empty strings, null, min/max values)
 */
class UserProfileTest {
    @Test
    fun validate_withValidProfile_returnsTrue() {
        val profile = UserProfile(
            userId = "user123",
            userName = "testuser",
            hardConstraints = HardConstraints.EMPTY,
            softPreferences = SoftPreferences.DEFAULT
        )

        val result = profile.validate()

        assertTrue(result)
    }

    @Test
    fun validate_withEmptyUserId_returnsFalse() {
        val profile = UserProfile(
            userId = "",
            userName = "testuser",
            hardConstraints = HardConstraints.EMPTY
        )

        val result = profile.validate()

        assertFalse(result)
    }

    @Test
    fun validate_withBlankUserId_returnsFalse() {
        val profile = UserProfile(
            userId = "   ",
            userName = "testuser",
            hardConstraints = HardConstraints.EMPTY
        )

        val result = profile.validate()

        assertFalse(result)
    }

    @Test
    fun validate_withValidUserIdAndConstraints_returnsTrue() {
        val constraints = HardConstraints(
            dietaryRestrictions = listOf(DietaryType.VEGETARIAN),
            allergies = listOf(Allergen.PEANUTS),
            avoidIngredients = listOf("Cilantro")
        )
        val profile = UserProfile(
            userId = "user123",
            hardConstraints = constraints
        )

        val result = profile.validate()

        assertTrue(result)
    }

    @Test
    fun validate_withMinimalValidData_returnsTrue() {
        val profile = UserProfile(
            userId = "a", // Single character userId
            hardConstraints = HardConstraints.EMPTY
        )

        val result = profile.validate()

        assertTrue(result)
    }

    @Test
    fun validate_withLongUserId_returnsTrue() {
        val longUserId = "a".repeat(1000)
        val profile = UserProfile(
            userId = longUserId,
            hardConstraints = HardConstraints.EMPTY
        )

        val result = profile.validate()

        assertTrue(result)
    }

    @Test
    fun toJson_withValidProfile_returnsCorrectJson() {
        val profile = UserProfile(
            userId = "user123",
            userName = "testuser",
            fullName = "Test User",
            hardConstraints = HardConstraints.EMPTY,
            softPreferences = SoftPreferences.DEFAULT,
            isOnboardingComplete = true
        )

        val json = profile.toJson()

        assertTrue(json.contains("\"userId\":\"user123\""))
        assertTrue(json.contains("\"userName\":\"testuser\""))
        assertTrue(json.contains("\"fullName\":\"Test User\""))
        assertTrue(json.contains("\"isOnboardingComplete\":true"))
    }

    @Test
    fun fromJson_withValidJson_returnsCorrectProfile() {
        val originalProfile = UserProfile(
            userId = "user123",
            userName = "testuser",
            fullName = "Test User",
            isOnboardingComplete = true
        )
        val json = originalProfile.toJson()

        val deserializedProfile = UserProfile.fromJson(json)

        assertEquals(originalProfile.userId, deserializedProfile.userId)
        assertEquals(originalProfile.userName, deserializedProfile.userName)
        assertEquals(originalProfile.fullName, deserializedProfile.fullName)
        assertEquals(originalProfile.isOnboardingComplete, deserializedProfile.isOnboardingComplete)
    }

    @Test
    fun toJsonAndFromJson_roundTrip_preservesAllData() {
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
                spiceTolerance = SpiceLevel.MEDIUM
            ),
            isOnboardingComplete = true
        )

        val json = originalProfile.toJson()
        val deserializedProfile = UserProfile.fromJson(json)

        assertEquals(originalProfile.userId, deserializedProfile.userId)
        assertEquals(originalProfile.hardConstraints, deserializedProfile.hardConstraints)
        assertEquals(originalProfile.softPreferences, deserializedProfile.softPreferences)
        assertEquals(originalProfile.isOnboardingComplete, deserializedProfile.isOnboardingComplete)
    }

    @Test
    fun fromJson_withEmptyConstraints_deserializesCorrectly() {
        val originalProfile = UserProfile(
            userId = "user123",
            hardConstraints = HardConstraints.EMPTY,
            softPreferences = SoftPreferences.DEFAULT
        )
        val json = originalProfile.toJson()

        val deserializedProfile = UserProfile.fromJson(json)

        assertFalse(deserializedProfile.hardConstraints.hasConstraints())
        assertEquals(SoftPreferences.DEFAULT, deserializedProfile.softPreferences)
    }

    @Test
    fun createNew_withValidUserId_createsProfileWithCorrectDefaults() {
        val userId = "newUser123"

        val profile = UserProfile.createNew(userId)

        assertEquals(userId, profile.userId)
        assertEquals(HardConstraints.EMPTY, profile.hardConstraints)
        assertEquals(SoftPreferences.DEFAULT, profile.softPreferences)
        assertFalse(profile.isOnboardingComplete)
    }

    @Test
    fun createNew_withEmptyUserId_createsProfileWithEmptyUserId() {
        val userId = ""

        val profile = UserProfile.createNew(userId)

        assertEquals("", profile.userId)
        assertFalse(profile.validate()) // Should fail validation
    }

    @Test
    fun createNew_multipleInvocations_createsDifferentObjects() {
        val userId = "user123"

        val profile1 = UserProfile.createNew(userId)
        val profile2 = UserProfile.createNew(userId)

        assertTrue(profile1 !== profile2) // Different object references
        assertEquals(profile1.userId, profile2.userId)
    }

    @Test
    fun withUpdatedTimestamp_updatesTimestamp() {
        val originalProfile = UserProfile(
            userId = "user123",
            updatedAt = 1000L
        )
        Thread.sleep(10) // Ensure time passes

        val updatedProfile = originalProfile.withUpdatedTimestamp()

        assertTrue(updatedProfile.updatedAt > originalProfile.updatedAt)
        assertEquals(originalProfile.userId, updatedProfile.userId)
    }

    @Test
    fun withUpdatedTimestamp_preservesOtherFields() {
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

        val updatedProfile = originalProfile.withUpdatedTimestamp()

        assertEquals(originalProfile.userId, updatedProfile.userId)
        assertEquals(originalProfile.userName, updatedProfile.userName)
        assertEquals(originalProfile.fullName, updatedProfile.fullName)
        assertEquals(originalProfile.hardConstraints, updatedProfile.hardConstraints)
        assertEquals(originalProfile.isOnboardingComplete, updatedProfile.isOnboardingComplete)
        assertNotEquals(originalProfile.updatedAt, updatedProfile.updatedAt)
    }

    @Test
    fun copy_withModifiedField_createsNewInstanceWithChanges() {
        val originalProfile = UserProfile(
            userId = "user123",
            userName = "oldname"
        )

        val copiedProfile = originalProfile.copy(userName = "newname")

        assertEquals("user123", copiedProfile.userId)
        assertEquals("newname", copiedProfile.userName)
        assertEquals("oldname", originalProfile.userName) // Original unchanged
    }

    @Test
    fun equals_withSameData_returnsTrue() {
        val profile1 = UserProfile(
            userId = "user123",
            userName = "testuser"
        )
        val profile2 = UserProfile(
            userId = "user123",
            userName = "testuser"
        )

        assertEquals(profile1, profile2)
    }

    @Test
    fun equals_withDifferentData_returnsFalse() {
        val profile1 = UserProfile(
            userId = "user123",
            userName = "testuser1"
        )
        val profile2 = UserProfile(
            userId = "user123",
            userName = "testuser2"
        )

        assertNotEquals(profile1, profile2)
    }

    @Test
    fun validate_withSpecialCharactersInUserId_returnsTrue() {
        val profile = UserProfile(
            userId = "user-123_@email.com",
            hardConstraints = HardConstraints.EMPTY
        )

        val result = profile.validate()

        assertTrue(result)
    }

    @Test
    fun validate_withUnicodeInUserId_returnsTrue() {
        val profile = UserProfile(
            userId = "사용자123",
            hardConstraints = HardConstraints.EMPTY
        )

        val result = profile.validate()

        assertTrue(result)
    }
}
