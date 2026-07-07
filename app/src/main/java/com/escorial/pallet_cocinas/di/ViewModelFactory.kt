package com.escorial.pallet_cocinas.di

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.escorial.pallet_cocinas.PalletCocinasApp

class ViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}

val ComponentActivity.appContainer: AppContainer
    get() = (application as PalletCocinasApp).container
