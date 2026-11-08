package com.podlets.tgproxy

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first

data class ProxyLaunchConfig(
    val bindIp: String,
    val port: Int,
    val ips: String,
    val poolSize: Int,
    val cfEnabled: Boolean,
    val cfPriority: Boolean,
    val cfDomain: String,
    val secretKey: String
)

object ProxyController {

    private const val TAG = "ProxyController"

    /**
     * Synchronous fast start targeting ProxyService with ACTION_START.
     * Guaranteed to execute within the immediate user click interaction window
     * (e.g. from Quick Settings Tile) to prevent ForegroundServiceStartNotAllowedException on Android 12+.
     */
    fun startSync(context: Context): Boolean {
        return try {
            val appContext = context.applicationContext
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, ProxyService::class.java).apply {
                    action = ProxyService.ACTION_START
                }
            )
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start foreground service synchronously", e)
            false
        }
    }

    suspend fun loadConfig(context: Context): ProxyLaunchConfig? {
        val settingsStore = SettingsStore(context)
        settingsStore.migrateLegacyDefaults()
        val portText = settingsStore.port.first()
        val port = portText.toIntOrNull() ?: return null

        val bindIp = settingsStore.bindIp.first()
        val isExperimental = settingsStore.isExperimentalMode.first()
        val isDcAuto = settingsStore.isDcAuto.first()
        val poolSize = settingsStore.poolSize.first()
        val cfEnabled = settingsStore.cfproxyEnabled.first()
        val customCfDomainEnabled = settingsStore.customCfDomainEnabled.first()
        val customCfDomain = settingsStore.customCfDomain.first().trim()
        val secretKey = ensureSecretKey(settingsStore)

        val parsedIps = buildList {
            if (!isDcAuto) {
                appendDc(1, settingsStore.dc1.first())
                appendDc(2, settingsStore.dc2.first())
                appendDc(3, settingsStore.dc3.first())
                appendDc(4, settingsStore.dc4.first())

                if (isExperimental) {
                    appendDc(5, settingsStore.dc5.first())
                    appendDc(203, settingsStore.dc203.first())
                    appendDc(-1, settingsStore.dc1m.first())
                    appendDc(-2, settingsStore.dc2m.first())
                    appendDc(-3, settingsStore.dc3m.first())
                    appendDc(-4, settingsStore.dc4m.first())
                    appendDc(-5, settingsStore.dc5m.first())
                    appendDc(-203, settingsStore.dc203m.first())
                }
            }
        }.joinToString(",")

        return ProxyLaunchConfig(
            bindIp = bindIp,
            port = port,
            ips = parsedIps,
            poolSize = poolSize,
            cfEnabled = cfEnabled,
            cfPriority = true,
            cfDomain = if (customCfDomainEnabled && cfEnabled) customCfDomain else "",
            secretKey = secretKey
        )
    }

    suspend fun startFromSavedSettings(
        context: Context,
        showInvalidPortToast: Boolean = false
    ): Boolean {
        val config = loadConfig(context)
        if (config == null) {
            if (showInvalidPortToast) {
                Toast.makeText(context, context.getString(R.string.invalid_port), Toast.LENGTH_SHORT).show()
            }
            ProxyTileService.requestSync(context)
            return false
        }

        return try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ProxyService::class.java).apply {
                    action = ProxyService.ACTION_START
                    putExtra(ProxyService.EXTRA_BIND_IP, config.bindIp)
                    putExtra(ProxyService.EXTRA_PORT, config.port)
                    putExtra(ProxyService.EXTRA_IPS, config.ips)
                    putExtra(ProxyService.EXTRA_POOL_SIZE, config.poolSize)
                    putExtra(ProxyService.EXTRA_CFPROXY_ENABLED, config.cfEnabled)
                    putExtra(ProxyService.EXTRA_CFPROXY_PRIORITY, config.cfPriority)
                    putExtra(ProxyService.EXTRA_CFPROXY_DOMAIN, config.cfDomain)
                    putExtra(ProxyService.EXTRA_SECRET_KEY, config.secretKey)
                }
            )
            ProxyTileService.requestSync(context)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start foreground service from saved settings", e)
            false
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        try {
            appContext.startService(
                Intent(appContext, ProxyService::class.java).apply {
                    action = ProxyService.ACTION_STOP
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to stop proxy service", e)
        }
        ProxyTileService.requestSync(appContext)
    }

    internal suspend fun ensureSecretKey(settingsStore: SettingsStore): String {
        val current = settingsStore.secretKey.first().trim()
        if (isValidSecret(current)) {
            return current
        }

        val generated = generateRandomSecret()
        settingsStore.saveSecretKey(generated)
        return generated
    }

    private fun MutableList<String>.appendDc(dc: Int, value: String) {
        val ip = value.trim()
        if (ip.isNotBlank()) {
            add("$dc:$ip")
        }
    }

    private fun generateRandomSecret(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun isValidSecret(value: String): Boolean {
        return value.length == 32 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    }
}
