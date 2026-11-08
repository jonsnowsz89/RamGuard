package com.ramguard.app

import android.app.Application
import com.ramguard.app.data.AppRepository
import com.ramguard.app.data.CleanerEngine
import com.ramguard.app.data.SettingsRepository

/** Manual DI container (keeps the sample free of Hilt/KSP setup). */
class AppContainer(app: Application) {
    val settings = SettingsRepository(app)
    val apps = AppRepository(app)
    val engine = CleanerEngine(app, settings, apps)
}

class CleanerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
