package com.pebblentn.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the latest published app version string (e.g. "0.0.14") from a release feed. Abstracted so
 * the update logic can be tested with a fake, and so the network detail lives in one place.
 */
fun interface ReleaseFetcher {
    /** The latest version tag, or null if it could not be determined (offline, error, throttled). */
    suspend fun latestVersion(): String?
}

/**
 * Reads the latest release tag from the GitHub Releases API (REQ-ANDROID-013). This is the app's
 * only network call: a plain unauthenticated GET that sends no user or notification data and only
 * reads the public release metadata. Best-effort — any failure returns null and the caller carries
 * on. GitHub requires a User-Agent on API requests.
 */
class GitHubReleaseFetcher(
    private val apiUrl: String = LATEST_RELEASE_URL,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ReleaseFetcher {

    override suspend fun latestVersion(): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "PebbleNTN-Android")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.parseToJsonElement(body).jsonObject["tag_name"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Timber.d(e, "Update check failed")
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/optio/PebbleNTN/releases/latest"
        private const val TIMEOUT_MS = 8_000
    }
}
