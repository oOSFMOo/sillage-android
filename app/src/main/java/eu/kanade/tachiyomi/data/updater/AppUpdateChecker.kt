package eu.kanade.tachiyomi.data.updater

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.model.Release
import uy.kohesive.injekt.injectLazy

class AppUpdateChecker {

    private val network: NetworkHelper by injectLazy()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(context: Context, forceCheck: Boolean = false): GetApplicationRelease.Result {
        return withIOContext {
            val preferences = context.getSharedPreferences("sillage-app-update", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val last = preferences.getLong("last-success", 0)
            if (!forceCheck && now >= last && now - last < 86_400_000L) {
                return@withIOContext GetApplicationRelease.Result.NoNewUpdate
            }
            val releases = with(json) {
                network.client.newCall(GET("https://api.github.com/repos/$GITHUB_REPO/releases?per_page=30"))
                    .awaitSuccess().parseAs<List<SillageAppRelease>>()
            }
            val candidate = selectSillageUpdate(releases, BuildConfig.VERSION_NAME)
            preferences.edit().putLong("last-success", now).apply()
            if (candidate == null) return@withIOContext GetApplicationRelease.Result.NoNewUpdate
            GetApplicationRelease.Result.NewUpdate(Release(candidate.tag, candidate.body.orEmpty(),
                candidate.htmlUrl, listOf(candidate.apkUrl()!!)))
        }
    }
}

const val GITHUB_REPO = "oOSFMOo/sillage-android"
