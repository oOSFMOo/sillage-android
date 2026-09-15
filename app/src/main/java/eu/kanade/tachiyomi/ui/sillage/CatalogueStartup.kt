package eu.kanade.tachiyomi.ui.sillage

import android.content.Context
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tachiyomi.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal object CatalogueStartup {
    fun start(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            // Runs once even after restoring preferences from another reader.
            val preferences = context.getSharedPreferences("sillage", Context.MODE_PRIVATE)
            if (!preferences.getBoolean("skip-duplicates-v1", false)) {
                Injekt.get<eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences>().skipDupe.set(true)
                preferences.edit().putBoolean("skip-duplicates-v1", true).apply()
            }
            if (!preferences.getBoolean("weekly-favourites-v2", false)) {
                Injekt.get<LibraryPreferences>().autoUpdateInterval.set(168)
                LibraryUpdateJob.setupTask(context)
                preferences.edit().putBoolean("weekly-favourites-v2", true).apply()
            }
            val seedSources = SillageCatalogue.load(context).document.series
                .flatMap { listOf(it) + it.editions }.map { it.sourceId }.toSet()
            val sourceManager = Injekt.get<tachiyomi.domain.source.service.SourceManager>()
            sourceManager.isInitialized.first { it }
            CatalogueStore(context).use { store ->
                if (store.state(AsuraSource.ID).message == "En attente") {
                    sourceManager.get(AsuraSource.ID)?.let { CatalogueImportWorker.enqueue(context, it) }
                }
            }
            Injekt.get<ExtensionManager>().installedExtensionsFlow.collect { extensions ->
                val languages = Injekt.get<SourcePreferences>().enabledLanguages.get() + setOf("fr", "en", "all")
                CatalogueStore(context).use { store ->
                    extensions.flatMap { it.sources }.filter { it.lang in languages }.forEach { source ->
                        val state = store.state(source.id)
                        if (source.id.toString() in seedSources && state.message == "En attente") {
                            store.state(source.id, state.copy(complete = true, message = "Catalogue préchargé · Utilise Actualiser les nouveautés"))
                        } else if (!state.complete && state.message == "En attente") {
                            CatalogueImportWorker.enqueue(context, source)
                        }
                    }
                }
            }
        }
    }
}
