package app.rocat.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import app.rocat.core.common.injekt.InjektModule
import app.rocat.core.common.injekt.Registrar
import app.rocat.core.common.network.NetworkHelper
import app.rocat.data.db.AppDatabase
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
import app.rocat.i18n.I18nProvider
import app.rocat.settings.SettingsRepository
import app.rocat.storage.StorageManager
import app.rocat.ui.import.ImportScriptViewModel
import app.rocat.ui.settings.SettingsViewModel
import app.rocat.ui.scripts.ScriptsViewModel

/**
 * Application-level dependency graph, mirroring mihon's `AppModule`/`PreferenceModule`.
 * Registered from [RoApp.onCreate].
 */
class AppModule(val app: Application) : InjektModule {

    override fun registerInjectables(registrar: Registrar) {
        registrar.add(app)
        registrar.addSingleton(app as Context)

        val networkHelper = NetworkHelper(app)
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

        // Tahap 15: Settings / storage / i18n / Room.
        val settingsRepository = SettingsRepository(app)
        registrar.addSingleton(settingsRepository)

        registrar.addSingleton(I18nProvider(settingsRepository))
        registrar.addSingleton(StorageManager(app, settingsRepository))

        // Tahap 15.3: Room database singleton + DAOs.
        val database = Room.databaseBuilder(app, AppDatabase::class.java, DATABASE_NAME).build()
        registrar.addSingleton(database)
        registrar.addSingleton(database.cookieDao())
        registrar.addSingleton(database.historyDao())

        // ViewModels. Registered as Injekt factories so the Compose screens can build
        // them without the reflection-based default factory (which only supports
        // no-arg constructors and crashes for these constructor-injected ViewModels).
        registrar.addSingletonFactory { ScriptsViewModel() }
        registrar.addSingletonFactory { ImportScriptViewModel() }
        registrar.addSingletonFactory { SettingsViewModel() }
        registrar.addSingleton(AppViewModelFactory)
    }

    companion object {
        private const val DATABASE_NAME = "rocat.db"
    }
}