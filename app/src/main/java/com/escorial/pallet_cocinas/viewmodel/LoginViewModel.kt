package com.escorial.pallet_cocinas.viewmodel

import androidx.lifecycle.viewModelScope
import com.escorial.pallet_cocinas.data.repository.LoginRepository
import com.escorial.pallet_cocinas.data.repository.SessionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(val isLoading: Boolean = false)

sealed class LoginNavEvent {
    data class ToMain(val bypass: Boolean) : LoginNavEvent()
}

class LoginViewModel(
    private val loginRepository: LoginRepository,
    private val sessionRepository: SessionRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<LoginNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<LoginNavEvent> = _navEvents.receiveAsFlow()

    val initialNavEvent: LoginNavEvent? = run {
        if (!sessionRepository.isLoggedIn()) return@run null
        val loggedUser = sessionRepository.getUsername(default = "")
        LoginNavEvent.ToMain(bypass = loggedUser == "expedicion")
    }

    fun login(username: String, password: String) {
        if (username == "expedicion" && password == "expedicion") {
            sessionRepository.saveSession("expedicion", "Expedicion")
            viewModelScope.launch { _navEvents.send(LoginNavEvent.ToMain(bypass = true)) }
            return
        }
        safeLaunch(onLoadingChange = { loading -> _uiState.update { it.copy(isLoading = loading) } }) {
            val loginResult = loginRepository.login(username, password) ?: return@safeLaunch
            sessionRepository.saveSession(loginResult.usuario_sistema, loginResult.nombre)
            _navEvents.send(LoginNavEvent.ToMain(bypass = false))
        }
    }
}
