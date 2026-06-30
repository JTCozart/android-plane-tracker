package com.jtcozart.planetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Result of a single poll of the adsb.lol feed. */
sealed interface FetchResult {
    data class Success(val response: JSONObject, val httpCode: Int) : FetchResult
    data class Failure(val httpCode: Int, val reason: String) : FetchResult
}

/**
 * Fetches aircraft from the free, key-less adsb.lol feed.
 * Port of the firmware AdsbLolSource — same endpoint and contract.
 *
 *   GET https://api.adsb.lol/v2/lat/{lat}/lon/{lon}/dist/{radius_nm}
 */
class AdsbLolSource {
    private val client = OkHttpClient.Builder()
        .callTimeout(HTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()

    var lastResponseCode: Int = 0
        private set
    var consecutiveFailures: Int = 0
        private set

    suspend fun fetch(lat: Double, lon: Double, radiusNm: Float): FetchResult =
        withContext(Dispatchers.IO) {
            val url = String.format(Locale.US, URL_FORMAT, lat, lon, radiusNm.toDouble())
            val request = Request.Builder().url(url).build()
            try {
                client.newCall(request).execute().use { resp ->
                    lastResponseCode = resp.code
                    if (!resp.isSuccessful) {
                        consecutiveFailures++
                        return@withContext FetchResult.Failure(resp.code, "HTTP ${resp.code}")
                    }
                    val body = resp.body?.string()
                        ?: run {
                            consecutiveFailures++
                            return@withContext FetchResult.Failure(resp.code, "empty body")
                        }
                    val json = JSONObject(body)
                    consecutiveFailures = 0
                    FetchResult.Success(json, resp.code)
                }
            } catch (e: Exception) {
                consecutiveFailures++
                FetchResult.Failure(lastResponseCode, e.message ?: "network error")
            }
        }

    private companion object {
        const val HTTP_TIMEOUT_SEC = 8L
        const val URL_FORMAT = "https://api.adsb.lol/v2/lat/%.6f/lon/%.6f/dist/%.1f"
    }
}
