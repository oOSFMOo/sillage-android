package eu.kanade.tachiyomi.ui.sillage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.chapter.getResumeChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun SillageReadingHome() {
    val history by remember { Injekt.get<GetHistory>().subscribe("") }.collectAsState(emptyList())
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick = { scope.launch { HomeScreen.openTab(HomeScreen.Tab.Library()) } }) { Text("Mes lectures") }
            TextButton(onClick = { scope.launch { HomeScreen.openTab(HomeScreen.Tab.Updates) } }) { Text("Nouveaux chapitres") }
            TextButton(onClick = { scope.launch { HomeScreen.openTab(HomeScreen.Tab.More(toDownloads = true)) } }) { Text("Téléchargements") }
        }
        if (history.isNotEmpty()) {
            Text("Reprendre", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                history.distinctBy { it.mangaId }.take(3).forEach { entry ->
                    OutlinedCard(Modifier.width(210.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Dernière lecture · ch. ${entry.chapterNumber}", style = MaterialTheme.typography.bodySmall)
                            Row {
                                TextButton(enabled = !busy, onClick = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            val chapter = withContext(Dispatchers.IO) {
                                                val manga = Injekt.get<GetManga>().await(entry.mangaId) ?: return@withContext null
                                                Injekt.get<GetChaptersByMangaId>().await(manga.id, true).getResumeChapter(manga)
                                            }
                                            if (chapter != null) context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
                                            else navigator.push(MangaScreen(entry.mangaId))
                                        } catch (e: kotlinx.coroutines.CancellationException) { throw e
                                        } catch (_: Exception) { message = "Ouvre la fiche de la série pour reprendre." }
                                        finally { busy = false }
                                    }
                                }) { Text("Lire") }
                                TextButton(onClick = { navigator.push(SillageSeriesToolsScreen(entry.mangaId, true)) }) { Text("Train") }
                            }
                        }
                    }
                }
            }
        } else Text("Ajoute des favoris avec le cœur. Tes lectures récentes apparaîtront ici.", style = MaterialTheme.typography.bodySmall)
        if (message.isNotBlank()) Text(message)
    }
}
