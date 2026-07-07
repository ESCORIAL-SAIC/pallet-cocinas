package com.escorial.pallet_cocinas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escorial.pallet_cocinas.utils.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val _errorEvents = Channel<String>(Channel.BUFFERED)
    val errorEvents: Flow<String> = _errorEvents.receiveAsFlow()

    protected fun safeLaunch(
        onLoadingChange: (Boolean) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ): Job = viewModelScope.launch {
        onLoadingChange(true)
        try {
            block()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            _errorEvents.send(t.toUserMessage())
        } finally {
            onLoadingChange(false)
        }
    }
}
