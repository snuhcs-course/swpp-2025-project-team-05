package com.example.veato.data.model

import org.junit.Assert.*
import org.junit.Test

class PortionSizeTest {
    @Test
    fun enums_haveCorrectDisplayNames() {
        assertEquals("Small", PortionSize.SMALL.displayName)
        assertEquals("Medium", PortionSize.MEDIUM.displayName)
        assertEquals("Large", PortionSize.LARGE.displayName)
    }

    @Test
    fun portionSize_enumHasThreeValues() {
        assertEquals(3, PortionSize.entries.size)
    }

    @Test
    fun portionSize_enumOrderIsCorrect() {
        val entries = PortionSize.entries

        assertEquals(PortionSize.SMALL, entries[0])
        assertEquals(PortionSize.MEDIUM, entries[1])
        assertEquals(PortionSize.LARGE, entries[2])
    }

    @Test
    fun portionSize_enumNamesAreCorrect() {
        assertEquals("SMALL", PortionSize.SMALL.name)
        assertEquals("MEDIUM", PortionSize.MEDIUM.name)
        assertEquals("LARGE", PortionSize.LARGE.name)
    }

    @Test
    fun portionSize_valueOfWorksCorrectly() {
        assertEquals(PortionSize.SMALL, PortionSize.valueOf("SMALL"))
        assertEquals(PortionSize.MEDIUM, PortionSize.valueOf("MEDIUM"))
        assertEquals(PortionSize.LARGE, PortionSize.valueOf("LARGE"))
    }

    @Test
    fun portionSize_serializesToJsonCorrectly() {
        val json = kotlinx.serialization.json.Json

        val serialized = json.encodeToString(
            PortionSize.serializer(),
            PortionSize.MEDIUM
        )

        assertEquals("\"MEDIUM\"", serialized)
    }

    @Test
    fun portionSize_deserializesFromJsonCorrectly() {
        val json = kotlinx.serialization.json.Json

        val deserialized = json.decodeFromString(
            PortionSize.serializer(),
            "\"LARGE\""
        )

        assertEquals(PortionSize.LARGE, deserialized)
    }

    @Test
    fun portionSize_displayNamesAreNotEmpty() {
        PortionSize.entries.forEach { size ->
            assertTrue(size.displayName.isNotEmpty())
        }
    }

    @Test
    fun portionSize_displayNamesAreUnique() {
        val names = PortionSize.entries.map { it.displayName }
        assertEquals(names.size, names.toSet().size)
    }
}
