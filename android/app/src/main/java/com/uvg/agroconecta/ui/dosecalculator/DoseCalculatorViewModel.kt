package com.uvg.agroconecta.ui.dosecalculator

import androidx.lifecycle.ViewModel
import com.uvg.agroconecta.data.dosecalculator.DoseCalculationResult
import com.uvg.agroconecta.data.dosecalculator.DoseCalculator
import com.uvg.agroconecta.data.dosecalculator.DoseReferenceData
import com.uvg.agroconecta.data.dosecalculator.LandUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DoseCalculatorUiState(
    val products: List<String> = DoseReferenceData.getProducts(),
    val availableCrops: List<String> = emptyList(),
    val selectedProduct: String = "",
    val selectedCrop: String = "",
    val selectedLandUnit: LandUnit = LandUnit.HECTAREAS,
    val landAreaInput: String = "",
    val result: DoseCalculationResult? = null,
    val errorMessage: String? = null
)

class DoseCalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DoseCalculatorUiState())
    val uiState: StateFlow<DoseCalculatorUiState> = _uiState

    fun onProductSelected(productName: String) {
        val crops = DoseReferenceData.getCropsForProduct(productName)

        _uiState.value = _uiState.value.copy(
            selectedProduct = productName,
            availableCrops = crops,
            selectedCrop = "",
            result = null,
            errorMessage = null
        )
    }

    fun onCropSelected(cropName: String) {
        _uiState.value = _uiState.value.copy(
            selectedCrop = cropName,
            result = null,
            errorMessage = null
        )
    }

    fun onLandUnitSelected(landUnit: LandUnit) {
        _uiState.value = _uiState.value.copy(
            selectedLandUnit = landUnit,
            result = null,
            errorMessage = null
        )
    }

    fun onLandAreaChanged(value: String) {
        val normalizedValue = value.replace(',', '.')

        val isValidInput = normalizedValue.isEmpty() ||
            normalizedValue.matches(Regex("""\d*\.?\d*"""))

        if (!isValidInput) {
            return
        }

        _uiState.value = _uiState.value.copy(
            landAreaInput = normalizedValue,
            result = null,
            errorMessage = null
        )
    }

    fun calculateDose() {
        val currentState = _uiState.value

        if (currentState.selectedProduct.isBlank()) {
            showError("Selecciona un producto.")
            return
        }

        if (currentState.selectedCrop.isBlank()) {
            showError("Selecciona un cultivo.")
            return
        }

        val landArea = currentState.landAreaInput.toDoubleOrNull()

        if (landArea == null || landArea <= 0) {
            showError("Ingresa un tamaño de terreno válido.")
            return
        }

        val reference = DoseReferenceData.findReference(
            productName = currentState.selectedProduct,
            cropName = currentState.selectedCrop
        )

        if (reference == null) {
            showError("No existe una dosis de referencia para esta selección.")
            return
        }

        val calculationResult = try {
            DoseCalculator.calculate(
                reference = reference,
                landArea = landArea,
                landUnit = currentState.selectedLandUnit
            )
        } catch (exception: IllegalArgumentException) {
            showError(
                exception.message
                    ?: "No fue posible calcular la dosis."
            )
            return
        }

        _uiState.value = currentState.copy(
            result = calculationResult,
            errorMessage = null
        )
    }

    fun resetCalculator() {
        _uiState.value = DoseCalculatorUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }

    private fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            result = null,
            errorMessage = message
        )
    }
}
