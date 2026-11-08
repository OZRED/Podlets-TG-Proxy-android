package com.podlets.tgproxy

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProxyTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listenJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        listenJob?.cancel()
        listenJob = scope.launch {
            ProxyService.isRunning.collectLatest { isRunning ->
                renderTile(
                    if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                )
            }
        }
        renderTile()
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        val wasRunning = ProxyService.isRunning.value
        if (wasRunning) {
            renderTile(Tile.STATE_INACTIVE)
            ProxyController.stop(applicationContext)
        } else {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_START_PROXY
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    override fun onDestroy() {
        listenJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun renderTile(overrideState: Int? = null) {
        qsTile?.apply {
            label = "Telegram WS Proxy"
            icon = Icon.createWithResource(this@ProxyTileService, R.drawable.ic_qs_proxy_t)
            state = overrideState ?: if (ProxyService.isRunning.value) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (state == Tile.STATE_ACTIVE) getString(R.string.tile_connected) else getString(R.string.tile_disconnected)
            }
            contentDescription = label
            updateTile()
        }
    }

    companion object {
        fun requestSync(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, ProxyTileService::class.java))
            }
        }
    }
}
