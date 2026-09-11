package eu.kanade.tachiyomi.data.backup.restore

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SillageBackupMigrationTest {
    private fun imported(index: Int) = BackupManga(
        source = 6084907896154116083L,
        url = "/title/$index",
        title = "Series $index",
        dateAdded = 1789073830638L,
        thumbnailUrl = "/api/v1/manga/$index/thumbnail",
        initialized = true,
    )

    private fun catalogue() = MutableList(1000, ::imported)

    @Test
    fun `an unrelated large personal library is never converted`() {
        val entries = catalogue().onEach { it.dateAdded = 1789073830639L }
        val backup = Backup(entries)
        val prepared = SillageBackupMigration.prepare(backup)

        assertFalse(prepared.isLegacyCatalogue)
        assertSame(backup, prepared.backup)
        assertEquals(1000, prepared.backup.backupManga.size)
    }

    @Test
    fun `untouched catalogue entries do not become followed series`() {
        val prepared = SillageBackupMigration.prepare(Backup(catalogue()))

        assertTrue(prepared.isLegacyCatalogue)
        assertEquals(1000, prepared.movedToCatalogue)
        assertTrue(prepared.backup.backupManga.isEmpty())
    }

    @Test
    fun `reading position and bookmarks survive with lazy cover repair`() {
        val entries = catalogue()
        val chapter = BackupChapter(
            url = "/chapter/1",
            name = "Chapter 1",
            read = true,
            lastPageRead = 17L,
            bookmark = true,
        )
        entries[0].chapters = listOf(chapter)
        val prepared = SillageBackupMigration.prepare(Backup(entries))
        val restored = prepared.backup.backupManga.single()

        assertEquals(999, prepared.movedToCatalogue)
        assertSame(chapter, restored.chapters.single())
        assertEquals(17L, restored.chapters.single().lastPageRead)
        assertTrue(restored.favorite)
        assertNull(restored.thumbnailUrl)
        assertFalse(restored.initialized)
    }

    @Test
    fun `new favourites and a custom reading category are preserved`() {
        val entries = catalogue()
        entries[0].categories = listOf(50)
        entries.add(imported(1001).apply { dateAdded += 1 })
        val prepared = SillageBackupMigration.prepare(
            Backup(entries, backupCategories = listOf(BackupCategory("Mes lectures", order = 50))),
        )

        assertEquals(listOf("/title/0", "/title/1001"), prepared.backup.backupManga.map { it.url })
        assertEquals(listOf(50L), prepared.backup.backupManga.first().categories)
    }

    @Test
    fun `wrong imported category IDs are repaired using the original genres`() {
        val entries = catalogue()
        entries[0].genre = listOf("Action")
        entries[0].categories = listOf(1)
        entries[0].notes = "À poursuivre"
        val prepared = SillageBackupMigration.prepare(
            Backup(
                entries,
                backupCategories = listOf(
                    BackupCategory("Action", order = 0, id = 1),
                    BackupCategory("Adulte", order = 1, id = 2),
                ),
            ),
        )

        assertEquals(listOf(0L), prepared.backup.backupManga.single().categories)
        assertEquals("À poursuivre", prepared.backup.backupManga.single().notes)
    }

    @Test
    fun `valid remote and personal file covers are left intact`() {
        assertTrue(SillageBackupMigration.isDesktopCover("/api/v1/manga/25/thumbnail"))
        assertFalse(SillageBackupMigration.isDesktopCover("https://example.org/cover.jpg"))
        assertFalse(SillageBackupMigration.isDesktopCover("/storage/emulated/0/cover.jpg"))
        assertFalse(SillageBackupMigration.isDesktopCover(null))
    }
}
