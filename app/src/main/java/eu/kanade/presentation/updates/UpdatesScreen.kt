package eu.kanade.presentation.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.active
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

@Composable
fun UpdateScreen(
    state: UpdatesScreenModel.State,
    snackbarHostState: SnackbarHostState,
    lastUpdated: Long,
    // SY -->
    preserveReadingPosition: Boolean,
    // SY <--
    onClickCover: (UpdatesItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onCalendarClicked: () -> Unit,
    onUpdateLibrary: () -> Boolean,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onOpenChapter: (UpdatesItem) -> Unit,
    onFilterClicked: () -> Unit,
    hasActiveFilters: Boolean,
) {
    var grouped by remember { mutableStateOf(true) }
    BackHandler(enabled = state.selectionMode) {
        onSelectAll(false)
    }

    Scaffold(
        topBar = { scrollBehavior ->
            UpdatesAppBar(
                onCalendarClicked = { onCalendarClicked() },
                onUpdateLibrary = { onUpdateLibrary() },
                onFilterClicked = { onFilterClicked() },
                hasFilters = hasActiveFilters,
                actionModeCounter = state.selected.size,
                onSelectAll = { onSelectAll(true) },
                onInvertSelection = { onInvertSelection() },
                onCancelActionMode = { onSelectAll(false) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            UpdatesBottomBar(
                selected = state.selected,
                onDownloadChapter = onDownloadChapter,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMultiDeleteClicked = onMultiDeleteClicked,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.items.isEmpty() -> androidx.compose.foundation.layout.Column(
                Modifier.padding(contentPadding).padding(24.dp),
            ) {
                androidx.compose.material3.Text(if (hasActiveFilters) "Aucun chapitre pour ces filtres" else "Aucun nouveau chapitre détecté", style = MaterialTheme.typography.titleLarge)
                androidx.compose.material3.Text("Cet onglet suit les chapitres de tes favoris ajoutés avec le cœur. Leur vérification automatique a lieu tous les 7 jours ; tu peux aussi la lancer ici.")
                androidx.compose.material3.Text(if (lastUpdated > 0) "Dernière vérification : " + java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.FRANCE).format(java.util.Date(lastUpdated)) else "Pas encore de vérification des favoris enregistrée.")
                androidx.compose.material3.Button(onClick = { onUpdateLibrary() }) { androidx.compose.material3.Text("Vérifier mes favoris") }
                if (hasActiveFilters) androidx.compose.material3.TextButton(onClick = onFilterClicked) { androidx.compose.material3.Text("Modifier les filtres") }
            }
            else -> {
                val scope = rememberCoroutineScope()
                var isRefreshing by remember { mutableStateOf(false) }

                PullRefresh(
                    refreshing = isRefreshing,
                    onRefresh = {
                        val started = onUpdateLibrary()
                        if (!started) return@PullRefresh
                        scope.launch {
                            // Fake refresh status but hide it after a second as it's a long running task
                            isRefreshing = true
                            delay(1.seconds)
                            isRefreshing = false
                        }
                    },
                    enabled = !state.selectionMode,
                    indicatorPadding = contentPadding,
                ) {
                    FastScrollLazyColumn(
                        contentPadding = contentPadding,
                    ) {
                        updatesLastUpdatedItem(lastUpdated)

                        item {
                            androidx.compose.material3.TextButton(onClick = { grouped = !grouped }) {
                                androidx.compose.material3.Text(if (grouped) "Voir les chapitres individuellement" else "Regrouper par série")
                            }
                        }
                        if (grouped && !state.selectionMode) {
                            items(state.items.groupBy { it.update.mangaId }.values.toList(), key = { "series:${it.first().update.mangaId}" }) { chapters ->
                                androidx.compose.material3.TextButton(onClick = { onClickCover(chapters.first()) }, modifier = Modifier.fillMaxWidth()) {
                                    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                        androidx.compose.material3.Text(chapters.first().update.mangaTitle, style = MaterialTheme.typography.titleMedium)
                                        androidx.compose.material3.Text("${chapters.size} chapitres récents · ${chapters.count { !it.update.read }} non lus")
                                    }
                                }
                            }
                        } else updatesUiItems(
                            uiModels = state.getUiModel(),
                            selectionMode = state.selectionMode,
                            // SY -->
                            preserveReadingPosition = preserveReadingPosition,
                            // SY <--
                            onUpdateSelected = onUpdateSelected,
                            onClickCover = onClickCover,
                            onClickUpdate = onOpenChapter,
                            onDownloadChapter = onDownloadChapter,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdatesAppBar(
    onCalendarClicked: () -> Unit,
    onUpdateLibrary: () -> Unit,
    onFilterClicked: () -> Unit,
    hasFilters: Boolean,
    // For action mode
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancelActionMode: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    AppBar(
        modifier = modifier,
        title = "Nouveaux chapitres",
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = Icons.Outlined.FilterList,
                        iconTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current,
                        onClick = onFilterClicked,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_view_upcoming),
                        icon = Icons.Outlined.CalendarMonth,
                        onClick = onCalendarClicked,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_update_library),
                        icon = Icons.Outlined.Refresh,
                        onClick = onUpdateLibrary,
                    ),
                ),
            )
        },
        actionModeCounter = actionModeCounter,
        onCancelActionMode = onCancelActionMode,
        actionModeActions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        onClick = onSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
                        onClick = onInvertSelection,
                    ),
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun UpdatesBottomBar(
    selected: List<UpdatesItem>,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
) {
    MangaBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, true)
        }.takeIf { selected.fastAny { !it.update.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, false)
        }.takeIf { selected.fastAll { it.update.bookmark } },
        onMarkAsReadClicked = {
            onMultiMarkAsReadClicked(selected, true)
        }.takeIf { selected.fastAny { !it.update.read } },
        onMarkAsUnreadClicked = {
            onMultiMarkAsReadClicked(selected, false)
        }.takeIf { selected.fastAny { it.update.read || it.update.lastPageRead > 0L } },
        onDownloadClicked = {
            onDownloadChapter(selected, ChapterDownloadAction.START)
        }.takeIf {
            selected.fastAny { it.downloadStateProvider() != Download.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected)
        }.takeIf { selected.fastAny { it.downloadStateProvider() == Download.State.DOWNLOADED } },
    )
}

sealed interface UpdatesUiModel {
    data class Header(val date: LocalDate) : UpdatesUiModel
    data class Item(val item: UpdatesItem) : UpdatesUiModel
}
