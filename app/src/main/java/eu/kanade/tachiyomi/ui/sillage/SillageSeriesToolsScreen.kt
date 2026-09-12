package eu.kanade.tachiyomi.ui.sillage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.util.chapter.getResumeChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.service.getChapterSort
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun SillageSeriesActions(manga: Manga) {
    val navigator = LocalNavigator.currentOrThrow
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { navigator.push(SillageSeriesToolsScreen(manga.id)) }, modifier = Modifier.weight(1f)) { Text("Similaires / sources") }
        TextButton(onClick = { navigator.push(SillageSeriesToolsScreen(manga.id, true)) }, modifier = Modifier.weight(1f)) { Text("Télécharger") }
    }
}

class SillageSeriesToolsScreen(private val mangaId: Long, private val trainFirst: Boolean = false) : Screen() {
    @Composable override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var manga by remember { mutableStateOf<Manga?>(null) }
        var rows by remember { mutableStateOf<List<CatalogueSeries>>(emptyList()) }
        var suggestions by remember { mutableStateOf<List<SimilarSeries>>(emptyList()) }
        var minimum by remember { mutableStateOf(false) }
        var travelCount by remember { mutableStateOf("25") }
        var message by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var loading by remember { mutableStateOf(true) }
        LaunchedEffect(mangaId) {
            try {
                manga = withContext(Dispatchers.IO) { Injekt.get<GetManga>().await(mangaId) }
                rows = SillageCatalogue.load(context).document.series
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (_: Exception) { message = "Catalogue indisponible. Reviens sur cette fiche pour réessayer." }
            finally { loading = false }
        }
        val current = manga
        val group = remember(rows, current) { rows.firstOrNull { row ->
            row.editions.ifEmpty { listOf(row) }.any { it.sourceId == current?.source.toString() && it.url == current?.url } || row.title.equals(current?.title, true)
        } }
        LaunchedEffect(rows, current, minimum) {
            suggestions = withContext(Dispatchers.Default) {
                similarSeries(current?.title.orEmpty(), current?.genre.orEmpty().ifEmpty { group?.genres.orEmpty() }, rows, if (minimum) 50 else 0)
            }
        }
        fun open(row: CatalogueSeries) {
            if (busy) return
            busy = true
            scope.launch {
                try {
                    val local = withContext(Dispatchers.IO) {
                        val sources = Injekt.get<tachiyomi.domain.source.service.SourceManager>()
                        val available = row.editions.ifEmpty { listOf(row) }.sortedByDescending { it.chapters ?: -1 }
                            .firstOrNull { sources.get(it.sourceId.toLong()) != null }
                            ?: error("Active une source de cette série dans Sources avant de lire.")
                        Injekt.get<NetworkToLocalManga>()(available.toManga())
                    }
                    navigator.push(MangaScreen(local.id))
                } catch (e: kotlinx.coroutines.CancellationException) { throw e
                } catch (e: Exception) { message = e.message ?: "Impossible d’ouvrir cette édition" }
                finally { busy = false }
            }
        }
        fun prepare(count: Int) {
            val selected = current ?: return
            if (busy) return
            busy = true
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val chapters = Injekt.get<GetChaptersByMangaId>().await(selected.id, true).sortedWith(getChapterSort(selected, sortDescending = false))
                        val resume = chapters.getResumeChapter(selected)
                        val manager = Injekt.get<DownloadManager>()
                        val next = travelChapters(chapters, resume?.id, count)
                            .filterNot { manager.isChapterDownloaded(it.name, it.scanlator, it.url, selected.ogTitle, selected.source) }
                        manager.downloadChapters(selected, next)
                        next.size
                    }
                    message = if (result == 0) "Aucun chapitre supplémentaire à télécharger : actualise la série si nécessaire."
                        else "$result chapitre(s) demandé(s). Consulte la file de téléchargements pour vérifier leur fin avant le départ."
                } catch (e: kotlinx.coroutines.CancellationException) { throw e
                } catch (_: Exception) { message = "Téléchargement impossible. Vérifie la source et le dossier de stockage." }
                finally { busy = false }
            }
        }
        Scaffold { padding ->
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    TextButton(onClick = { navigator.pop() }) { Text("Retour") }
                    Text(current?.title ?: "Chargement…", style = MaterialTheme.typography.headlineSmall)
                    if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (message.isNotBlank()) Text(message)
                }
                item {
                    Text("Télécharger pour le train", style = MaterialTheme.typography.titleLarge)
                    Text("Les prochains chapitres sont choisis depuis ta reprise de lecture. Ceux déjà téléchargés sont conservés. La taille exacte dépend des images et est inconnue avant téléchargement.")
                    Row {
                        Button(onClick = { prepare(5) }, enabled = current != null && !busy) { Text("5 chapitres") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { prepare(10) }, enabled = current != null && !busy) { Text("10 chapitres") }
                    }
                }
                item {
                    Text("Long trajet ou avion", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(25, 50, 100).forEach { count ->
                            OutlinedButton(onClick = { travelCount = count.toString() }, enabled = !busy) { Text(count.toString()) }
                        }
                    }
                    val count = travelCount.toIntOrNull()
                    OutlinedTextField(value = travelCount, onValueChange = { travelCount = it.filter(Char::isDigit).take(3) },
                        label = { Text("Nombre de chapitres (1 à 500)") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth())
                    Button(onClick = { prepare(count!!) }, enabled = current != null && !busy && count != null && count in 1..500) {
                        Text("Télécharger le nombre choisi")
                    }
                    Text("Pour un vol, termine les téléchargements avant le départ. Vérifie la place disponible sur ton téléphone ; plusieurs dizaines de chapitres peuvent occuper beaucoup d’espace.")
                }
                item { Text("Autres sources", style = MaterialTheme.typography.titleLarge)
                    Text("Les nombres connus peuvent dater de la dernière vérification. Pour transférer ta progression, utilise Migrer dans Mes lectures.") }
                items(group?.editions.orEmpty().sortedByDescending { it.chapters ?: -1 }, key = { "edition:${it.key}" }) { edition ->
                    OutlinedButton(onClick = { open(edition) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("${edition.sourceName} · ${edition.chapters?.toString() ?: "?"} chapitres" + if (edition.sourceId == current?.source.toString()) " · actuelle" else "")
                    }
                }
                item {
                    Text("Séries similaires", style = MaterialTheme.typography.titleLarge)
                    FilterChip(selected = minimum, onClick = { minimum = !minimum }, label = { Text("Au moins 50 chapitres") })
                    if (!loading && suggestions.isEmpty()) Text("Pas assez de genres communs connus pour proposer des titres fiables.")
                }
                items(suggestions, key = { "similar:${it.series.key}" }) { suggestion ->
                    Card(onClick = { open(suggestion.series) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(suggestion.series.title, style = MaterialTheme.typography.titleMedium)
                            Text(suggestion.sharedGenres.joinToString(" · "))
                            Text("${suggestion.series.chapters?.toString() ?: "?"} chapitres · ${suggestion.series.sourceName}")
                        }
                    }
                }
            }
        }
    }
}
