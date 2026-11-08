package com.podlets.tgproxy.data.feed

import android.util.Log
import com.podlets.tgproxy.data.network.HttpDownloadHelper
import com.podlets.tgproxy.domain.ProxyEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Загрузка MTProto прокси из источников (TGStat, каналы). */
class ProxyFeedService {
    suspend fun fetchToList(
        output: MutableList<ProxyEntry>,
        sources: List<String>? = null,
        log: ((String) -> Unit)? = null,
        progress: ((CollectProgress) -> Unit)? = null
    ) {
        val targetUrls = sources ?: TGSTAT_URLS
        if (targetUrls.isEmpty()) {
            return
        }
        val seen = linkedSetOf<String>()
        val startMsg = "Загрузка источников прокси (${targetUrls.size})…"
        log?.invoke(startMsg)
        Log.i("PodletsTGProxy", startMsg)
        progress?.invoke(CollectProgress(proxiesFound = 0))

        var loadedAny = false
        for (url in targetUrls) {
            coroutineContext.ensureActive()
            try {
                val feedMsg = "Лента: $url…"
                log?.invoke(feedMsg)
                Log.i("PodletsTGProxy", feedMsg)

                val html = HttpDownloadHelper.download(url, timeoutMs = 20_000L, maxAttempts = 2)
                if (!ProxyLinkParser.looksLikeTelegramFeed(html)) {
                    val noPostsMsg = "Лента: ответ без постов, пробуем следующий URL…"
                    log?.invoke(noPostsMsg)
                    Log.w("PodletsTGProxy", noPostsMsg)
                    continue
                }
                val added = emitNewProxies(ProxyLinkParser.parseFeed(html), seen, output)
                val addedMsg = "Лента: $url, +$added прокси"
                log?.invoke(addedMsg)
                Log.i("PodletsTGProxy", addedMsg)

                progress?.invoke(CollectProgress(proxiesFound = seen.size))
                if (added > 0 || seen.isNotEmpty()) {
                    loadedAny = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = "Ошибка ленты $url: ${e.message}"
                log?.invoke(errMsg)
                Log.e("PodletsTGProxy", errMsg, e)
            }
        }

        if (!loadedAny && seen.isEmpty()) {
            throw IllegalStateException("Не удалось загрузить прокси из источников")
        }
        val doneMsg = "Сбор завершён: ${seen.size} прокси"
        log?.invoke(doneMsg)
        Log.i("PodletsTGProxy", doneMsg)
        progress?.invoke(CollectProgress(proxiesFound = seen.size))
    }

    private fun emitNewProxies(
        parsed: List<ProxyEntry>,
        seen: MutableSet<String>,
        output: MutableList<ProxyEntry>
    ): Int {
        var added = 0
        for (p in parsed) {
            if (!seen.add(p.key)) continue
            added++
            output.add(p)
        }
        return added
    }

    companion object {
        private val TGSTAT_URLS = listOf(
            "https://tgstat.com/channel/@ProxyMTProto",
            "https://tgstat.ru/channel/@ProxyMTProto"
        )
    }
}
