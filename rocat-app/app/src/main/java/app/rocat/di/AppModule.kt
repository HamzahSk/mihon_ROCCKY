package app.rocat.di

import android.app.Application
import android.content.Context
import app.rocat.core.common.injekt.InjektModule
import app.rocat.core.common.injekt.Registrar
import app.rocat.core.common.network.NetworkHelper
import app.rocat.data.script.ScriptManager
import app.rocat.data.script.ScriptRepositoryImpl
import app.rocat.data.script.ScriptSourceFetcher
import app.rocat.domain.script.DeleteScript
import app.rocat.domain.script.ExecuteScript
import app.rocat.domain.script.GetScripts
import app.rocat.domain.script.ImportScript
import app.rocat.domain.script.ScriptRepository
import app.rocat.domain.script.SetScriptEnabled
import app.rocat.domain.script.UpsertScript

/**
 * Application-level dependency graph, mirroring mihon's `AppModule`/`PreferenceModule`.
 * Registered from [RoApp.onCreate].
 */
class AppModule(val app: Application) : InjektModule {

    override fun registerInjectables(registrar: Registrar) {
        registrar.add(app)
        registrar.addSingleton(app as Context)

        val networkHelper = NetworkHelper(app.cacheDir)
        registrar.addSingleton(networkHelper)

        // Wires the Rhino engine + network-backed environment into a single manager.
        val scriptManager = ScriptManager(networkHelper)
        registrar.addSingleton(scriptManager)

        val scriptsDir = java.io.File(app.filesDir, "scripts")
        val scriptRepository: ScriptRepository = ScriptRepositoryImpl(scriptsDir)
        registrar.addSingleton(scriptRepository)
        registrar.addSingletonFactory { GetScripts(scriptRepository) }
        registrar.addSingletonFactory { UpsertScript(scriptRepository) }
        registrar.addSingletonFactory { ImportScript(scriptRepository) }
        registrar.addSingletonFactory { DeleteScript(scriptRepository) }
        registrar.addSingletonFactory { SetScriptEnabled(scriptRepository) }
        registrar.addSingletonFactory { ScriptSourceFetcher(networkHelper.client) }
        registrar.addSingletonFactory {
            ExecuteScript(
                engine = scriptManager.engine,
                environment = scriptManager.environment,
            )
        }
    }
}