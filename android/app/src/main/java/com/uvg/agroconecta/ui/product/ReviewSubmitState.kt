package com.uvg.agroconecta.ui.product

sealed class ReviewSubmitState {
    object Idle    : ReviewSubmitState()
    object Loading : ReviewSubmitState()
    object Success : ReviewSubmitState()
    data class Error(val message: String) : ReviewSubmitState()
}