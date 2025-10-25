package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BudgetRange(
    val minPrice: Int,
    val maxPrice: Int
) {
    init {
        require(minPrice >= 0) { "Min price must be non-negative" }
        require(maxPrice >= minPrice) { "Max price must be greater than or equal to min price" }
    }

    companion object {
        // Pre-defined budget ranges in Korean Won
        val BUDGET = BudgetRange(3000, 8000)
        val MODERATE = BudgetRange(8000, 15000)
        val PREMIUM = BudgetRange(15000, 25000)
        val FINE_DINING = BudgetRange(25000, 50000)
    }
}
