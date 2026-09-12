package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File

internal object SillageApkVerifier {
    @Suppress("DEPRECATION")
    fun verify(context: Context, apk: File) {
        check(apk.isFile) { "Aucune mise à jour téléchargée" }
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val candidate = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: error("Le fichier téléchargé n’est pas une application Android valide")
        check(candidate.packageName == installed.packageName) { "Cette mise à jour ne correspond pas à Sillage" }
        check(PackageInfoCompat.getLongVersionCode(candidate) > PackageInfoCompat.getLongVersionCode(installed)) {
            "Cette version est déjà installée ou plus ancienne"
        }
        check(signatures(installed).isNotEmpty() && signatures(candidate) == signatures(installed)) {
            "La signature de cette mise à jour ne correspond pas à Sillage"
        }
    }

    @Suppress("DEPRECATION")
    private fun signatures(info: PackageInfo): Set<String> =
        (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
            .orEmpty().map { it.toCharsString() }.toSet()
}
