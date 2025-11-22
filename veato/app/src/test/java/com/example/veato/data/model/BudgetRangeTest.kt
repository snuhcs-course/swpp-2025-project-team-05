package com.example.veato.data.model

import org.junit.Assert.*
import org.junit.Test

class BudgetRangeTest {
    @Test
    fun constructor_validRange_createsInstance() {
        val range = BudgetRange(3000, 8000)
        assertEquals(3000, range.minPrice)
        assertEquals(8000, range.maxPrice)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_negativeMinPrice_throwsException() {
        BudgetRange(-1, 1000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_maxLessThanMin_throwsException() {
        BudgetRange(5000, 1000)
    }

    @Test
    fun predefinedRanges_areValid() {
        assertEquals(3000, BudgetRange.BUDGET.minPrice)
        assertEquals(8000, BudgetRange.BUDGET.maxPrice)

        assertEquals(8000, BudgetRange.MODERATE.minPrice)
        assertEquals(15000, BudgetRange.MODERATE.maxPrice)

        assertEquals(15000, BudgetRange.PREMIUM.minPrice)
        assertEquals(25000, BudgetRange.PREMIUM.maxPrice)

        assertEquals(25000, BudgetRange.FINE_DINING.minPrice)
        assertEquals(50000, BudgetRange.FINE_DINING.maxPrice)
    }

    @Test
    fun dataClassFunctions_areCovered() {
        val r1 = BudgetRange(3000, 8000)
        val r2 = BudgetRange(3000, 8000)

        // equals + hashCode
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())

        // copy()
        val copy = r1.copy(maxPrice = 9000)
        assertEquals(9000, copy.maxPrice)
        assertEquals(3000, copy.minPrice)

        // toString()
        val text = r1.toString()
        assertTrue(text.contains("minPrice=3000"))
        assertTrue(text.contains("maxPrice=8000"))
    }

    @Test
    fun componentFunctions_returnCorrectValues() {
        val range = BudgetRange(3000, 8000)

        assertEquals(3000, range.component1())
        assertEquals(8000, range.component2())
    }
}
