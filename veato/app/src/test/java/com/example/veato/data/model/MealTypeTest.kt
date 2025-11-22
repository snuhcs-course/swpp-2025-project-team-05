package com.example.veato.data.model

import org.junit.Assert.*
import org.junit.Test

class MealTypeTest {
    @Test
    fun enums_haveCorrectDisplayNamesAndKoreanNames() {
        assertEquals("Rice-Based", MealType.RICE_BASED.displayName)
        assertEquals("밥류", MealType.RICE_BASED.koreanName)

        assertEquals("Noodle-Based", MealType.NOODLE_BASED.displayName)
        assertEquals("면류", MealType.NOODLE_BASED.koreanName)

        assertEquals("Bread-Based", MealType.BREAD_BASED.displayName)
        assertEquals("빵류", MealType.BREAD_BASED.koreanName)

        assertEquals("Soup-Based", MealType.SOUP_BASED.displayName)
        assertEquals("국/찌개", MealType.SOUP_BASED.koreanName)
    }

    @Test
    fun mealType_enumHasFourValues() {
        assertEquals(4, MealType.entries.size)
    }

    @Test
    fun mealType_enumOrderIsCorrect() {
        val entries = MealType.entries

        assertEquals(MealType.RICE_BASED, entries[0])
        assertEquals(MealType.NOODLE_BASED, entries[1])
        assertEquals(MealType.BREAD_BASED, entries[2])
        assertEquals(MealType.SOUP_BASED, entries[3])
    }

    @Test
    fun mealType_enumNamesAreCorrect() {
        assertEquals("RICE_BASED", MealType.RICE_BASED.name)
        assertEquals("NOODLE_BASED", MealType.NOODLE_BASED.name)
        assertEquals("BREAD_BASED", MealType.BREAD_BASED.name)
        assertEquals("SOUP_BASED", MealType.SOUP_BASED.name)
    }

    @Test
    fun mealType_valueOfWorksCorrectly() {
        assertEquals(MealType.RICE_BASED, MealType.valueOf("RICE_BASED"))
        assertEquals(MealType.NOODLE_BASED, MealType.valueOf("NOODLE_BASED"))
        assertEquals(MealType.BREAD_BASED, MealType.valueOf("BREAD_BASED"))
        assertEquals(MealType.SOUP_BASED, MealType.valueOf("SOUP_BASED"))
    }

    @Test
    fun mealType_serializesToJsonCorrectly() {
        val json = kotlinx.serialization.json.Json

        val serialized = json.encodeToString(
            MealType.serializer(),
            MealType.NOODLE_BASED
        )

        assertEquals("\"NOODLE_BASED\"", serialized)
    }

    @Test
    fun mealType_deserializesFromJsonCorrectly() {
        val json = kotlinx.serialization.json.Json

        val result = json.decodeFromString(
            MealType.serializer(),
            "\"BREAD_BASED\""
        )

        assertEquals(MealType.BREAD_BASED, result)
    }

    @Test
    fun mealType_allEnumsHaveNonEmptyProperties() {
        MealType.entries.forEach { type ->
            assertTrue(type.displayName.isNotEmpty())
            assertTrue(type.koreanName.isNotEmpty())
        }
    }

    @Test
    fun mealType_noDuplicateDisplayNamesOrKoreanNames() {
        val displayNames = MealType.entries.map { it.displayName }
        val koreanNames = MealType.entries.map { it.koreanName }

        assertEquals(displayNames.size, displayNames.toSet().size)
        assertEquals(koreanNames.size, koreanNames.toSet().size)
    }
}
