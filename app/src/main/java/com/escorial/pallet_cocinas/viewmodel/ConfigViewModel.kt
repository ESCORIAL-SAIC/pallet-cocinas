package com.escorial.pallet_cocinas.viewmodel

import androidx.lifecycle.ViewModel
import com.escorial.pallet_cocinas.data.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ConfigUiState(
    val isUnlocked: Boolean = false,
    val apiUrl: String = ""
)

class ConfigViewModel(private val configRepository: ConfigRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    fun validatePassword(password: String): Boolean {
        if (password != CONTRASEÑA_CORRECTA) return false
        _uiState.update { it.copy(isUnlocked = true, apiUrl = configRepository.getApiUrl()) }
        return true
    }

    fun saveApiUrl(url: String) {
        configRepository.setApiUrl(url)
    }

    private companion object {
        const val CONTRASEÑA_CORRECTA = "Aria9278"
    }
}
