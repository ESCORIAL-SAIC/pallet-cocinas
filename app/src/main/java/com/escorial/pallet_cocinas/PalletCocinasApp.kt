package com.escorial.pallet_cocinas

import android.app.Application
import com.escorial.pallet_cocinas.di.AppContainer

class PalletCocinasApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
