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
}
