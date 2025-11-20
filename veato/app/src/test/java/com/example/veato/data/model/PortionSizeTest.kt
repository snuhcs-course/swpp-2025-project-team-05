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
}
