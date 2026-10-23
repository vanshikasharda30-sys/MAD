package com.example.clinicai

import android.app.Application

class ClinicAIApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.auth.initialize(this)
    }
}
