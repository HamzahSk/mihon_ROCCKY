package app.rocat

import android.app.Application
import app.rocat.core.common.injekt.Injekt
import app.rocat.di.AppModule
import logcat.LogcatLogger

class RoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        LogcatLogger.install()

        // Mirror mihon's App.onCreate() DI bootstrap.
        Injekt.importModule(AppModule(this))
    }
}