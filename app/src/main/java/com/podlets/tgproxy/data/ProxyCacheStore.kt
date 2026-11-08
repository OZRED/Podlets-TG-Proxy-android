package com.podlets.tgproxy.data

import android.content.Context
import com.podlets.tgproxy.domain.ProxyEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant

data class CachedProxyData(
    val proxies: List<ProxyEntry>,
    val isCustomCheck: Boolean = false
)

object ProxyCacheStore {
    private const val FILE_NAME = "cached_proxies.json"

    fun save(context: Context, proxies: List<ProxyEntry>, isCustomCheck: Boolean = false) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonArray = JSONArray()
            for (p in proxies) {
                val obj = JSONObject().apply {
                    put("server", p.server)
                    put("port", p.port)
                    put("secret", p.secret)
                    if (p.pingMs != null) put("pingMs", p.pingMs)
                    put("isAvailable", p.isAvailable)
                    p.publishedAt?.let { put("publishedAt", it.toEpochMilli()) }
                    put("reactionsCount", p.reactionsCount)
                }
                jsonArray.put(obj)
            }
            val root = JSONObject().apply {
                put("isCustomCheck", isCustomCheck)
                put("proxies", jsonArray)
            }
            file.writeText(root.toString())
        } catch (_: Exception) {
            // Ignore cache write errors
        }
    }

    fun load(context: Context): CachedProxyData {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return CachedProxyData(emptyList())
            val text = file.readText().trim()
            if (text.isBlank()) return CachedProxyData(emptyList())

            val (jsonArray, isCustomCheck) = if (text.startsWith("{")) {
                val root = JSONObject(text)
                Pair(root.optJSONArray("proxies") ?: JSONArray(), root.optBoolean("isCustomCheck", false))
            } else {
                Pair(JSONArray(text), false)
            }

            val result = ArrayList<ProxyEntry>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val server = obj.optString("server")
                val port = obj.optInt("port")
                val secret = obj.optString("secret")
                if (server.isBlank() || port <= 0 || secret.isBlank()) continue
                val pingMs = if (obj.has("pingMs")) obj.optInt("pingMs") else null
                val isAvailable = obj.optBoolean("isAvailable", true)
                val publishedAtMilli = if (obj.has("publishedAt")) obj.optLong("publishedAt") else 0L
                val publishedAt = if (publishedAtMilli > 0L) Instant.ofEpochMilli(publishedAtMilli) else null
                val reactionsCount = obj.optInt("reactionsCount", 0)

                result.add(
                    ProxyEntry(
                        server = server,
                        port = port,
                        secret = secret,
                        pingMs = pingMs,
                        isAvailable = isAvailable,
                        publishedAt = publishedAt,
                        reactionsCount = reactionsCount
                    )
                )
            }
            CachedProxyData(result, isCustomCheck)
        } catch (_: Exception) {
            CachedProxyData(emptyList())
        }
    }
}
