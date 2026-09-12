package eu.kanade.tachiyomi.data.updater

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SillageAppReleaseTest {
    private fun release(version: String) = SillageAppRelease("v$version",
        htmlUrl = "https://github.com/$GITHUB_REPO/releases/tag/v$version",
        assets = listOf(SillageAppAsset("Sillage-Android-$version.apk",
            "https://github.com/$GITHUB_REPO/releases/download/v$version/Sillage-Android-$version.apk")))

    @Test fun `newest version wins regardless of publication ordering`() {
        assertEquals("v1.10.0", selectSillageUpdate(listOf(release("1.3.0"), release("1.10.0")), "1.2.0")?.tag)
    }
    @Test fun `an older major version cannot downgrade the app`() {
        assertNull(selectSillageUpdate(listOf(release("1.99.0")), "2.0.0"))
    }
    @Test fun `current version is not offered again`() {
        assertNull(selectSillageUpdate(listOf(release("1.2.0")), "1.2.0"))
    }
    @Test fun `drafts and releases without an apk are ignored`() {
        assertNull(selectSillageUpdate(listOf(release("1.3.0").copy(draft = true), release("1.4.0").copy(assets = emptyList())), "1.2.0"))
    }
    @Test fun `source archive cannot be selected as installer`() {
        val candidate = release("1.3.0")
        assertEquals(candidate.assets.single().url, candidate.copy(assets = listOf(SillageAppAsset("source.zip", "https://example.org/source.zip")) + candidate.assets).apkUrl())
    }
    @Test fun `foreign download host is rejected`() {
        val candidate = release("1.3.0")
        assertNull(candidate.copy(assets = listOf(candidate.assets.single().copy(url = "https://example.org/update.apk"))).apkUrl())
    }
    @Test fun `published prereleases and null release notes are supported`() {
        val json = """{"tag_name":"v1.3.0","body":null,"html_url":"https://github.com/$GITHUB_REPO/releases/tag/v1.3.0","prerelease":true,"draft":false,"assets":[{"name":"Sillage-Android-1.3.apk","browser_download_url":"https://github.com/$GITHUB_REPO/releases/download/v1.3.0/Sillage-Android-1.3.apk"}]}"""
        val candidate = Json { ignoreUnknownKeys = true }.decodeFromString<SillageAppRelease>(json)
        assertNotNull(selectSillageUpdate(listOf(candidate), "1.2.0"))
    }
    @Test fun `invalid versions are ignored and short versions are normalized`() {
        assertNull(sillageVersion("v1.3.0-beta"))
        assertEquals(listOf(1, 3, 0), sillageVersion("v1.3"))
        assertNull(selectSillageUpdate(listOf(release("999999999999.3.0")), "1.2.0"))
    }
}
