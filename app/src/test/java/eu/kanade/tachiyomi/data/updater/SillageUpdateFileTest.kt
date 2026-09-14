package eu.kanade.tachiyomi.data.updater

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SillageUpdateFileTest {
    @TempDir lateinit var directory: File
    private fun archive(): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
        }
    }.toByteArray()
    @Test fun `only fully verified file becomes installable`() {
        val bytes = archive()
        val ready = SillageUpdateFile.download(directory, bytes.inputStream(), bytes.size.toLong(), {}, {
            assertFalse(SillageUpdateFile.ready(directory).exists())
            assertArrayEquals(bytes, it.readBytes())
        })
        assertArrayEquals(bytes, ready.readBytes())
        assertEquals(listOf("ready.apk"), ready.parentFile!!.list()!!.toList())
    }
    @Test fun `truncation is rejected and staging is removed`() {
        val bytes = archive()
        assertThrows(IllegalStateException::class.java) {
            SillageUpdateFile.download(directory, bytes.inputStream(), bytes.size + 1L, {}, {})
        }
        assertFalse(SillageUpdateFile.ready(directory).exists())
        assertTrue(SillageUpdateFile.ready(directory).parentFile!!.list()!!.isEmpty())
    }
    @Test fun `html error page cannot be offered as apk`() {
        assertThrows(IllegalStateException::class.java) {
            SillageUpdateFile.download(directory, "<html>Error</html>".byteInputStream(), -1, {}, {})
        }
        assertFalse(SillageUpdateFile.ready(directory).exists())
    }
    @Test fun `signature failure preserves previous verified candidate`() {
        val bytes = archive()
        val ready = SillageUpdateFile.download(directory, bytes.inputStream(), -1, {}, {})
        assertThrows(IllegalStateException::class.java) {
            SillageUpdateFile.download(directory, bytes.inputStream(), -1, {}, { error("Invalid signature") })
        }
        assertArrayEquals(bytes, ready.readBytes())
    }
    @Test fun `cancellation never promotes the incoming file`() {
        assertThrows(java.util.concurrent.CancellationException::class.java) {
            SillageUpdateFile.download(directory, archive().inputStream(), -1, { throw java.util.concurrent.CancellationException() }, {})
        }
        assertFalse(SillageUpdateFile.ready(directory).exists())
    }
}
