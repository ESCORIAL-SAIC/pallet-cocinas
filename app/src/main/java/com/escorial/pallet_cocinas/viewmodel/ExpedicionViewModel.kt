package com.escorial.pallet_cocinas.viewmodel

import androidx.lifecycle.ViewModel
import com.escorial.pallet_cocinas.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExpedicionUiState(
    val username: String?,
    val fullName: String?
)

class ExpedicionViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ExpedicionUiState(
            username = sessionRepository.getUsername(),
            fullName = sessionRepository.getFullName()
        )
    )
    val uiState: StateFlow<ExpedicionUiState> = _uiState.asStateFlow()

    fun logout() = sessionRepository.logout()
}
