package eu.kanade.tachiyomi.data.backup.restore

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupManga

/** Adapts only the old Sillage bulk export, never an arbitrary large personal library. */
internal object SillageBackupMigration {
    // The export wrote this identical dateAdded to every catalogue entry.
    private const val LEGACY_EXPORT_TIMESTAMP = 1789073830638L
    private const val MIN_LEGACY_ENTRIES = 1000
    private val legacySources = setOf(
        6084907896154116083L,
        2559570015828016575L,
        9153097368891994905L,
        1903782575226230108L,
        3390167717659783669L,
    )

    private val originalCategories = setOf(
        "Action", "Adulte", "Apocalypse", "Arts martiaux", "Aventure", "Changement de genre",
        "Comédie", "Cuisine", "Cultivation", "Démons", "Donjons", "Drame", "Ecchi", "En couleur",
        "Fantasy", "Harem", "Harem inversé", "Historique", "Horreur", "Isekai", "Josei", "Magie",
        "Mecha", "Médecine", "Militaire", "Murim", "Musique", "Mystère", "Policier", "Psychologique",
        "Public averti", "Régression", "Réincarnation", "Romance", "Romance BL", "Romance GL",
        "Science-fiction", "Seinen", "Shōjo", "Shōnen", "Sport", "Super-héros", "Surnaturel",
        "Survie", "Système", "Thriller", "Tragédie", "Tranche de vie", "Vie scolaire", "Webtoon",
    )

    data class Prepared(val backup: Backup, val movedToCatalogue: Int, val isLegacyCatalogue: Boolean)

    fun prepare(backup: Backup): Prepared {
        val isLegacyCatalogue = backup.backupManga.count(::isLegacyEntry) >= MIN_LEGACY_ENTRIES
        if (!isLegacyCatalogue) return Prepared(backup, 0, false)

        val customCategoryOrders = backup.backupCategories
            .filterNot { it.name in originalCategories }
            .map { it.order }
            .toSet()
        val categoriesByName = backup.backupCategories.associateBy { it.name }
        val retained = backup.backupManga.filter { manga ->
            !isLegacyEntry(manga) || hasPersonalState(manga, customCategoryOrders)
        }
        retained.filter(::isLegacyEntry).forEach { manga ->
            // Source details are fetched lazily when the user opens the series.
            manga.thumbnailUrl = manga.thumbnailUrl?.takeUnless(::isDesktopCover)
            manga.initialized = false
            // The old export confused category IDs with category orders.
            manga.categories = (
                manga.genre.mapNotNull { categoriesByName[it]?.order } +
                    manga.categories.filter { it in customCategoryOrders }
                ).distinct()
        }
        return Prepared(
            backup = backup.copy(backupManga = retained),
            movedToCatalogue = backup.backupManga.size - retained.size,
            isLegacyCatalogue = true,
        )
    }

    fun isDesktopCover(url: String?): Boolean =
        url?.matches(Regex("^/api/v1/manga/[0-9]+/thumbnail(?:\\?.*)?$")) == true

    private fun isLegacyEntry(manga: BackupManga): Boolean =
        manga.dateAdded == LEGACY_EXPORT_TIMESTAMP && manga.source in legacySources

    private fun hasPersonalState(manga: BackupManga, customCategoryOrders: Set<Long>): Boolean =
        manga.history.isNotEmpty() ||
            manga.chapters.any { it.read || it.bookmark || it.lastPageRead > 0L } ||
            manga.tracking.isNotEmpty() ||
            manga.notes.isNotBlank() ||
            manga.categories.any { it in customCategoryOrders } ||
            manga.customTitle != null || manga.customAuthor != null || manga.customArtist != null ||
            manga.customDescription != null || manga.customGenre != null || manga.customThumbnailUrl != null ||
            manga.customStatus != 0 || manga.mergedMangaReferences.isNotEmpty() ||
            manga.viewer != 0 || (manga.viewer_flags ?: 0) != 0 || manga.chapterFlags != 0
}
