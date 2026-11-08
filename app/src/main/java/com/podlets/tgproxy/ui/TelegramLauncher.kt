package com.podlets.tgproxy.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.podlets.tgproxy.R
import com.podlets.tgproxy.data.TelegramLinkBuilder
import com.podlets.tgproxy.domain.ProxyEntry

object TelegramLauncher {
    fun openProxy(context: Context, entry: ProxyEntry) {
        val tg = TelegramLinkBuilder.buildTg(entry)
        val https = TelegramLinkBuilder.buildHttps(entry)
        openProxyUrl(context, tg, https)
    }

    fun openProxyUrl(context: Context, url: String, fallbackUrl: String? = null) {
        val tgUrl = when {
            url.startsWith("https://t.me/proxy") -> url.replace("https://t.me/proxy", "tg://proxy")
            url.startsWith("https://t.me/") -> {
                val path = url.removePrefix("https://t.me/")
                val domain = path.substringBefore("?").substringBefore("/")
                val query = if (path.contains("?")) path.substringAfter("?") else ""
                if (query.isNotEmpty()) "tg://resolve?domain=$domain&$query" else "tg://resolve?domain=$domain"
            }
            else -> url
        }

        val webUrl = fallbackUrl ?: if (tgUrl.startsWith("tg://proxy")) {
            tgUrl.replace("tg://proxy", "https://t.me/proxy")
        } else if (tgUrl.startsWith("tg://resolve")) {
            val uri = Uri.parse(tgUrl)
            val domain = uri.getQueryParameter("domain") ?: ""
            val start = uri.getQueryParameter("start")
            if (start != null) "https://t.me/$domain?start=$start" else "https://t.me/$domain"
        } else url

        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tgUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_telegram, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareProxies(context: Context, proxies: List<ProxyEntry>) {
        if (proxies.isEmpty()) return
        val text = proxies.joinToString("\n\n") { TelegramLinkBuilder.buildTg(it) }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pm = context.packageManager
        val tgPackages = listOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.messenger.beta",
            "org.thunderdog.challegram",
            "org.telegram.plus"
        )
        val resolved = tgPackages.firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
        if (resolved != null) {
            shareIntent.setPackage(resolved)
        }
        try {
            context.startActivity(shareIntent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                context.getString(R.string.share_proxies_title)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}
