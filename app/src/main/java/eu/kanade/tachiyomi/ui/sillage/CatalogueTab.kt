package eu.kanade.tachiyomi.ui.sillage

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat
import java.util.Locale

data object CatalogueTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 2u,
            title = "Catalogue",
            icon = rememberVectorPainter(Icons.Outlined.AutoStories),
        )

    @Composable
    override fun Content() {
        val context = LocalContext.current.applicationContext
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val gridState = rememberLazyGridState()
        var index by remember { mutableStateOf<CatalogueIndex?>(null) }
        var loadError by remember { mutableStateOf(false) }
        var loadAttempt by remember { mutableStateOf(0) }
        var query by rememberSaveable { mutableStateOf("") }
        var genre by rememberSaveable { mutableStateOf("") }
        var minimum by rememberSaveable { mutableStateOf(0) }
        var sortName by rememberSaveable { mutableStateOf(CatalogueSort.CHAPTERS.name) }
        var showFilters by rememberSaveable { mutableStateOf(false) }
        var showSources by rememberSaveable { mutableStateOf(false) }
        var filtered by remember { mutableStateOf<List<CatalogueSeries>>(emptyList()) }
        var filtering by remember { mutableStateOf(true) }
        var opening by remember { mutableStateOf<String?>(null) }
        var selectedSeries by remember { mutableStateOf<CatalogueSeries?>(null) }
        val revision by SillageCatalogue.revision.collectAsState()
        val extensions by Injekt.get<eu.kanade.tachiyomi.extension.ExtensionManager>().installedExtensionsFlow.collectAsState()
        val enabledLanguages = Injekt.get<eu.kanade.domain.source.service.SourcePreferences>().enabledLanguages.get() + setOf("fr", "en", "all")
        val availableSources = extensions.flatMap { it.sources }.filter { it.lang in enabledLanguages }
        var sourceStatus by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
        val sort = CatalogueSort.valueOf(sortName)
        val countFormat = remember { NumberFormat.getIntegerInstance(Locale.FRANCE) }

        LaunchedEffect(loadAttempt, revision) {
            loadError = false
            try {
                index = SillageCatalogue.load(context)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                loadError = true
            }
        }
        LaunchedEffect(showSources, extensions) {
            while (showSources) {
                sourceStatus = withContext(Dispatchers.IO) {
                    CatalogueStore(context).use { store -> availableSources.associate { it.id to store.state(it.id).message } }
                }
                delay(1500)
            }
        }
        LaunchedEffect(index, query, genre, minimum, sortName) {
            val currentIndex = index ?: return@LaunchedEffect
            filtering = true
            delay(220)
            filtered = SillageCatalogue.filter(currentIndex, query, genre, minimum, sort)
            filtering = false
        }
        LaunchedEffect(query, genre, minimum, sortName) { gridState.scrollToItem(0) }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SILLAGE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Ta prochaine lecture", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { navigator.push(HistoryTab) }) {
                            Icon(Icons.Outlined.History, contentDescription = "Historique et reprise de lecture")
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        placeholder = { Text("Rechercher dans le catalogue") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Effacer la recherche")
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("", "Murim", "Isekai", "Arts martiaux", "Aventure").forEach { item ->
                            FilterChip(selected = genre == item, onClick = { genre = item }, label = { Text(item.ifBlank { "Tous" }) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (index == null) "Ouverture du catalogue…" else "${countFormat.format(filtered.size)} séries",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { showFilters = true }) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (minimum == 0) "Trier et filtrer" else "$minimum+ chap. · Trier")
                        }
                    }
                    if (filtering && index != null) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
        ) { padding ->
            when {
                loadError -> Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Le catalogue n’a pas pu être ouvert.")
                    Button(onClick = { loadAttempt++ }) { Text("Réessayer") }
                    TextButton(onClick = { scope.launch { HomeScreen.openTab(HomeScreen.Tab.Browse()) } }) { Text("Parcourir les sources") }
                }
                index == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(144.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CatalogueIntro(
                            generatedAt = index!!.document.generatedAt,
                            onSources = { showSources = true },
                            onUpdate = {
                                availableSources.forEach { CatalogueImportWorker.enqueue(context, it, latest = true) }
                                showSources = true
                            },
                        )
                    }
                    if (filtered.isEmpty() && !filtering) item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aucune série pour ces filtres", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { query = ""; genre = ""; minimum = 0 }) { Text("Effacer les filtres") }
                        }
                    }
                    items(filtered, key = { it.key }) { series ->
                        CatalogueCard(series = series, opening = opening == series.key) {
                            if (series.editions.size > 1) {
                                selectedSeries = series
                                return@CatalogueCard
                            }
                            if (opening != null) return@CatalogueCard
                            opening = series.key
                            scope.launch {
                                try {
                                    val manga = withContext(Dispatchers.IO) {
                                        Injekt.get<NetworkToLocalManga>()(series.toManga())
                                    }
                                    navigator.push(MangaScreen(manga.id))
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    snackbar.showSnackbar("Impossible d’ouvrir cette série. Réessaie dans un instant.")
                                } finally {
                                    opening = null
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFilters) CatalogueFilters(
            genres = index?.document?.series?.flatMap { it.genres }?.distinct()?.sorted().orEmpty(),
            genre = genre,
            onGenre = { genre = it },
            minimum = minimum,
            sort = sort,
            onMinimum = { minimum = it },
            onSort = { sortName = it.name },
            onDismiss = { showFilters = false },
        )
        if (showSources) AlertDialog(
            onDismissRequest = { showSources = false },
            title = { Text("Sources et nouveautés") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Une nouvelle source importe son catalogue automatiquement. Ensuite, Actualiser ne consulte que son flux de nouveautés. Une source sans ce flux est signalée ici.", style = MaterialTheme.typography.bodyMedium)
                    availableSources.sortedBy { it.name }.forEach { source ->
                        Text(source.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Text(sourceStatus[source.id] ?: "En attente", style = MaterialTheme.typography.bodySmall)
                        TextButton(
                            onClick = {
                                CatalogueImportWorker.enqueue(context, source, latest = true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Actualiser / Réessayer") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSources = false
                    scope.launch { HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true)) }
                }) { Text("Gérer les extensions") }
            },
            dismissButton = { TextButton(onClick = { showSources = false }) { Text("Fermer") } },
        )
        selectedSeries?.let { series ->
            AlertDialog(
                onDismissRequest = { selectedSeries = null },
                title = { Text(series.title) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text("Choisis une source. La plus complète est affichée en premier. La progression est conservée séparément par source ; utilise Migrer dans Mes lectures pour la transférer.")
                        series.editions.forEach { edition ->
                            TextButton(onClick = {
                                selectedSeries = null
                                scope.launch {
                                    try {
                                        val manga = withContext(Dispatchers.IO) { Injekt.get<NetworkToLocalManga>()(edition.toManga()) }
                                        navigator.push(MangaScreen(manga.id))
                                    } catch (e: CancellationException) { throw e
                                    } catch (_: Exception) { snackbar.showSnackbar("Impossible d’ouvrir cette source.") }
                                }
                            }) { Text("${edition.sourceName} · ${edition.chapters?.toString() ?: "?"} chapitres") }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { selectedSeries = null }) { Text("Fermer") } },
            )
        }
    }
}

@Composable
private fun CatalogueIntro(generatedAt: String, onSources: () -> Unit, onUpdate: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Explore ici. Retrouve tes favoris dans Mes lectures.", style = MaterialTheme.typography.titleSmall)
            Text(
                "Le suivi des nouveaux chapitres concerne tes favoris. Ce catalogue est un instantané du ${generatedAt.take(10).split('-').reversed().joinToString(".")}. Notes et nombres de chapitres sont indicatifs ; les valeurs absentes restent inconnues.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onSources) { Text("Sources et nouveautés →") }
            Button(onClick = onUpdate) { Text("Actualiser les nouveautés") }
        }
    }
}

@Composable
private fun CatalogueCard(series: CatalogueSeries, opening: Boolean, onClick: () -> Unit) {
    val manga = remember(series.key) { series.toManga() }
    var localManga by remember(series.key) { mutableStateOf<Manga?>(null) }
    LaunchedEffect(series.key) {
        // Insert only the cards that are actually on screen. This gives Coil a
        // real Android manga ID so it can repair a missing desktop cover, while
        // keeping the complete catalogue out of the user's followed library.
        localManga = withContext(Dispatchers.IO) {
            try { Injekt.get<NetworkToLocalManga>()(manga) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
        }
    }
    val accent = remember(series.sourceId) {
        listOf(Color(0xFF35446B), Color(0xFF513B65), Color(0xFF275C58), Color(0xFF614238), Color(0xFF3D4C68))[
            (series.sourceId.hashCode() and Int.MAX_VALUE) % 5
        ]
    }
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(Brush.verticalGradient(listOf(accent, Color(0xFF171A27))))) {
            Column(Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                Text(series.title, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleSmall, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
            }
            if (localManga != null) MangaCover.Book(
                data = localManga!!,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = series.title,
                shape = RoundedCornerShape(0.dp),
            )
            if (series.rating != null) Text(
                "★ ${String.format(Locale.FRANCE, "%.1f", series.rating)}",
                color = Color(0xFFFFDE8B),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color(0xDD161923), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 4.dp),
            )
            if (opening) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
        Column(Modifier.padding(10.dp)) {
            Text(series.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                series.chapters?.let { "$it chapitres" } ?: "Chapitres à vérifier",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(series.sourceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CatalogueFilters(genres: List<String>, genre: String, onGenre: (String) -> Unit, minimum: Int, sort: CatalogueSort, onMinimum: (Int) -> Unit, onSort: (CatalogueSort) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trouver ta prochaine série") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Genre", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("") + genres).forEach { item ->
                        FilterChip(selected = genre == item, onClick = { onGenre(item) }, label = { Text(item.ifBlank { "Tous" }) })
                    }
                }
                Text("Classer par", style = MaterialTheme.typography.titleSmall)
                CatalogueSort.entries.forEach { option ->
                    FilterChip(selected = sort == option, onClick = { onSort(option) }, label = { Text(option.label) })
                }
                Text("Chapitres minimum", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 20, 50, 100, 200, 500).forEach { number ->
                        FilterChip(selected = minimum == number, onClick = { onMinimum(number) }, label = { Text(if (number == 0) "Tous" else "$number+") })
                    }
                }
                Text("Le filtre exclut les séries dont le nombre de chapitres est encore inconnu.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Voir les séries") } },
    )
}
