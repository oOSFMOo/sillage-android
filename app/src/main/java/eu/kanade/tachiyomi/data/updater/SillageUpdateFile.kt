package eu.kanade.tachiyomi.data.updater

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

/** Installation only sees the final file, never a partial or failed candidate. */
internal object SillageUpdateFile {
    fun ready(cacheDir: File) = File(cacheDir, "sillage-update/ready.apk")

    fun download(cacheDir: File, input: InputStream, expectedLength: Long, checkActive: () -> Unit, verify: (File) -> Unit): File {
        val destination = ready(cacheDir)
        destination.parentFile!!.mkdirs()
        val partial = File.createTempFile("incoming-", ".apk", destination.parentFile)
        try {
            input.use { source ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        checkActive()
                        val size = source.read(buffer)
                        if (size < 0) break
                        output.write(buffer, 0, size)
                    }
                    output.fd.sync()
                }
            }
            check(expectedLength < 0 || partial.length() == expectedLength) { "Téléchargement incomplet. Relance le téléchargement de la mise à jour." }
            try {
                ZipFile(partial).use { archive -> check(archive.getEntry("AndroidManifest.xml") != null) }
            } catch (e: Exception) {
                throw IllegalStateException("Le téléchargement est incomplet ou ne contient pas un APK. Relance-le ou utilise le lien GitHub.", e)
            }
            verify(partial)
            checkActive()
            Files.move(partial.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            return destination
        } finally { partial.delete() }
    }
}
