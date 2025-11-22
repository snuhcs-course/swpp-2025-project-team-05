package com.example.veato.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HeavinessLevelTest {
    @Test
    fun enums_haveCorrectDisplayNamesAndDescriptions() {
        assertEquals("Light", HeavinessLevel.LIGHT.displayName)
        assertEquals("Salads, soups, light meals", HeavinessLevel.LIGHT.description)

        assertEquals("Medium", HeavinessLevel.MEDIUM.displayName)
        assertEquals("Balanced meals", HeavinessLevel.MEDIUM.description)

        assertEquals("Heavy", HeavinessLevel.HEAVY.displayName)
        assertEquals("Fried, rich, filling meals", HeavinessLevel.HEAVY.description)
    }

    @Test
    fun heavinessLevel_enumHasThreeValues() {
        assertEquals(3, HeavinessLevel.entries.size)
    }

    @Test
    fun heavinessLevels_haveCorrectOrder() {
        val entries = HeavinessLevel.entries

        assertEquals(HeavinessLevel.LIGHT, entries[0])
        assertEquals(HeavinessLevel.MEDIUM, entries[1])
        assertEquals(HeavinessLevel.HEAVY, entries[2])
    }

    @Test
    fun heavinessLevels_haveCorrectEnumNames() {
        assertEquals("LIGHT", HeavinessLevel.LIGHT.name)
        assertEquals("MEDIUM", HeavinessLevel.MEDIUM.name)
        assertEquals("HEAVY", HeavinessLevel.HEAVY.name)
    }

    @Test
    fun valueOf_returnsCorrectEnum() {
        assertEquals(HeavinessLevel.LIGHT, HeavinessLevel.valueOf("LIGHT"))
        assertEquals(HeavinessLevel.MEDIUM, HeavinessLevel.valueOf("MEDIUM"))
        assertEquals(HeavinessLevel.HEAVY, HeavinessLevel.valueOf("HEAVY"))
    }

    @Test
    fun enum_serializesToCorrectJson() {
        val json = kotlinx.serialization.json.Json

        val serialized = json.encodeToString(HeavinessLevel.serializer(), HeavinessLevel.MEDIUM)
        assertEquals("\"MEDIUM\"", serialized)
    }

    @Test
    fun enum_deserializesFromJsonCorrectly() {
        val json = kotlinx.serialization.json.Json

        val deserialized = json.decodeFromString(
            HeavinessLevel.serializer(),
            "\"HEAVY\""
        )

        assertEquals(HeavinessLevel.HEAVY, deserialized)
    }
}
