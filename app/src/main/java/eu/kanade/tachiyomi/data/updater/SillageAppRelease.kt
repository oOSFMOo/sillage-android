package eu.kanade.tachiyomi.data.updater

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SillageAppRelease(
    @SerialName("tag_name") val tag: String,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val draft: Boolean = false,
    val assets: List<SillageAppAsset> = emptyList(),
) {
    fun apkUrl(): String? = assets.firstOrNull {
        it.name.matches(Regex("Sillage-Android-\\d+\\.\\d+(?:\\.\\d+)?\\.apk")) &&
            it.url == "https://github.com/$GITHUB_REPO/releases/download/$tag/${it.name}"
    }?.url
}

@Serializable
internal data class SillageAppAsset(val name: String, @SerialName("browser_download_url") val url: String)

internal fun sillageVersion(value: String): List<Int>? {
    if (!value.matches(Regex("v?\\d+\\.\\d+(?:\\.\\d+)?"))) return null
    val parts = value.removePrefix("v").split('.').map { it.toIntOrNull() ?: return null }
    return parts + List(3 - parts.size) { 0 }
}

internal fun selectSillageUpdate(releases: List<SillageAppRelease>, installed: String): SillageAppRelease? {
    val current = sillageVersion(installed) ?: return null
    val comparator = compareBy<List<Int>>({ it[0] }, { it[1] }, { it[2] })
    return releases.filter { !it.draft && it.apkUrl() != null &&
        it.htmlUrl == "https://github.com/$GITHUB_REPO/releases/tag/${it.tag}" }
        .mapNotNull { release -> sillageVersion(release.tag)?.let { release to it } }
        .filter { comparator.compare(it.second, current) > 0 }
        .maxWithOrNull { a, b -> comparator.compare(a.second, b.second) }?.first
}
