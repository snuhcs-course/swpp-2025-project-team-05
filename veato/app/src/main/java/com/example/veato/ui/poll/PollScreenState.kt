package com.example.veato.ui.poll

import com.example.veato.data.model.Poll

data class PollScreenState(
    val poll: Poll? = null,
    val isBusy: Boolean = true,
    val voted: Boolean = false,
    val selectedIndices: Set<Int> = emptySet()
)
