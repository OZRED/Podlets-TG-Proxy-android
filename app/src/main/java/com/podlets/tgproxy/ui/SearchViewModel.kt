package com.podlets.tgproxy.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.podlets.tgproxy.R
import com.podlets.tgproxy.SettingsStore
import com.podlets.tgproxy.data.ProxyCacheStore
import com.podlets.tgproxy.data.feed.ProxyFeedService
import com.podlets.tgproxy.data.feed.ProxyLinkParser
import com.podlets.tgproxy.data.health.ProxyHealthService
import com.podlets.tgproxy.domain.ProxyEntry
import com.podlets.tgproxy.domain.SortMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class SearchUiState(
    val hasSearched: Boolean = false,
    val isSearching: Boolean = false,
    val searchCancelled: Boolean = false,
    val fetchComplete: Boolean = false,
    val refiningPings: Boolean = false,
    val progressPercent: Int = 0,
    val progressText: String = "",
    val statusText: String = "",
    val activityLine: String = "",
    val foundCount: Int = 0,
    val showFoundCount: Boolean = false,
    val showPullHint: Boolean = false,
    val proxies: List<ProxyEntry> = emptyList(),
    val recheckingKeys: Set<String> = emptySet(),
    val feedError: String? = null,
    val sourcesEmpty: Boolean = false,
    val isCustomCheck: Boolean = false,
    val sortMode: SortMode = SortMode.Ping
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val feedService = ProxyFeedService()
    private val sortedList = SortedProxyList()

    private var searchJob: Job? = null
    private var backgroundRecheckJob: Job? = null
    private var appInForeground = true
    private var collectFound = 0
    private var finalDiscovered = 0
    private var checkedCount = 0
    private var refineTotal = 0
    private var refineDone = 0
    private var lastCollectLog: String? = null
    private var refiningPings = false

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedModeStr = settingsStore.searchSortMode.first()
            val mode = if (savedModeStr.equals("RATING", ignoreCase = true)) {
                SortMode.Rating
            } else {
                SortMode.Ping
            }
            sortedList.setSortMode(mode)

            val cachedData = withContext(Dispatchers.IO) {
                ProxyCacheStore.load(getApplication())
            }
            if (cachedData.proxies.isNotEmpty()) {
                sortedList.initFrom(cachedData.proxies)
                val snapshot = sortedList.snapshot()
                val status = if (cachedData.isCustomCheck) {
                    getApplication<Application>().getString(R.string.custom_check_done, snapshot.size)
                } else {
                    getApplication<Application>().getString(R.string.search_done, snapshot.size)
                }
                _uiState.update {
                    it.copy(
                        hasSearched = true,
                        isSearching = false,
                        showPullHint = true,
                        showFoundCount = snapshot.isNotEmpty(),
                        foundCount = snapshot.size,
                        sortMode = mode,
                        isCustomCheck = cachedData.isCustomCheck,
                        proxies = snapshot,
                        statusText = status
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        sortMode = mode,
                        proxies = sortedList.snapshot()
                    )
                }
            }
        }
    }

    fun onAppForeground() {
        appInForeground = true
        maybeStartBackgroundRecheck()
    }

    fun onAppBackground() {
        appInForeground = false
        stopBackgroundRecheck()
    }

    fun dismissPullHint() {
        _uiState.update { it.copy(showPullHint = false) }
    }

    fun setSortMode(mode: SortMode) {
        sortedList.setSortMode(mode)
        _uiState.update {
            it.copy(
                sortMode = mode,
                proxies = sortedList.snapshot()
            )
        }
        viewModelScope.launch {
            settingsStore.saveSearchSortMode(mode.name)
        }
    }

    fun startSearch() {
        stopBackgroundRecheck()
        searchJob?.cancel()
        sortedList.clear()
        collectFound = 0
        checkedCount = 0
        finalDiscovered = 0
        refineTotal = 0
        refineDone = 0
        refiningPings = false
        val sortMode = _uiState.value.sortMode
        sortedList.setSortMode(sortMode)

        val connectingText = getApplication<Application>().getString(R.string.connecting)
        _uiState.update {
            SearchUiState(
                hasSearched = true,
                isSearching = true,
                showPullHint = false,
                isCustomCheck = false,
                progressText = connectingText,
                statusText = connectingText,
                activityLine = connectingText,
                sortMode = sortMode
            )
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            val collected = mutableListOf<ProxyEntry>()
            lastCollectLog = null
            try {
                val sources = settingsStore.proxyFeedSources.first()
                if (sources.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                sourcesEmpty = true,
                                feedError = getApplication<Application>().getString(R.string.sources_empty)
                            )
                        }
                    }
                    return@launch
                }
                val timeoutMs = settingsStore.proxyCheckTimeoutMs.first()
                val healthService = ProxyHealthService(mtProxyTimeoutMs = timeoutMs)

                feedService.fetchToList(
                    output = collected,
                    sources = sources,
                    log = { line -> postActivityLine(line) },
                    progress = { p ->
                        collectFound = p.proxiesFound
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            updateOverallProgress()
                        }
                    }
                )

                finalDiscovered = collected.size
                collectFound = collected.size
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            fetchComplete = true,
                            showFoundCount = collected.isNotEmpty(),
                            activityLine = ""
                        )
                    }
                    updateOverallProgress()
                }

                if (collected.isEmpty()) {
                    val base = getApplication<Application>().getString(R.string.feed_failed)
                    val detail = lastCollectLog?.let { "\n\n$it" } ?: ""
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                feedError = base + detail
                            )
                        }
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(activityLine = "") }
                }

                // Phase 1: fast availability scan
                healthService.scanAvailability(
                    proxies = collected,
                    onProgress = { p ->
                        checkedCount = p.completed
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            updateOverallProgress()
                        }
                    },
                    onChecked = { result ->
                        if (result.isAvailable) {
                            sortedList.upsertAvailable(result.entry, result.pingMs)
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                _uiState.update {
                                    it.copy(
                                        proxies = sortedList.snapshot(),
                                        foundCount = sortedList.count,
                                        showFoundCount = true
                                    )
                                }
                            }
                        }
                    }
                )

                if (_uiState.value.searchCancelled) return@launch

                // Phase 2: accurate ping calculation
                val toRefine = sortedList.snapshot()
                if (toRefine.isNotEmpty()) {
                    refineTotal = toRefine.size
                    refineDone = 0
                    refiningPings = true
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(refiningPings = true) }
                        updateOverallProgress()
                    }

                    healthService.refinePings(
                        proxies = toRefine,
                        onProgress = { p ->
                            refineDone = p.completed
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                updateOverallProgress()
                            }
                        },
                        onChecking = { entry ->
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                _uiState.update {
                                    it.copy(recheckingKeys = it.recheckingKeys + entry.key)
                                }
                            }
                        },
                        onChecked = { result ->
                            if (result.isAvailable) {
                                sortedList.upsertAvailable(result.entry, result.pingMs)
                            } else {
                                result.entry.isAvailable = false
                                result.entry.pingMs = null
                                sortedList.remove(result.entry.key)
                            }
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                _uiState.update {
                                    it.copy(
                                        recheckingKeys = it.recheckingKeys - result.entry.key,
                                        proxies = sortedList.snapshot(),
                                        foundCount = sortedList.count,
                                        showFoundCount = sortedList.count > 0
                                    )
                                }
                            }
                        }
                    )
                }

                refiningPings = false
                if (!_uiState.value.searchCancelled) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                refiningPings = false,
                                progressPercent = 100,
                                activityLine = "",
                                recheckingKeys = emptySet(),
                                showPullHint = sortedList.count > 0,
                                statusText = getApplication<Application>().getString(
                                    R.string.search_done,
                                    sortedList.count
                                )
                            )
                        }
                    }
                    saveProxyCache()
                    maybeStartBackgroundRecheck()
                }
            } catch (_: CancellationException) {
                refiningPings = false
                withContext(Dispatchers.Main) { showPaused() }
                saveProxyCache()
            } catch (e: Exception) {
                refiningPings = false
                Log.e("PodletsTGProxy", "Search error", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            refiningPings = false,
                            feedError = e.message ?: e.toString()
                        )
                    }
                }
                saveProxyCache()
            } finally {
                refiningPings = false
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            refiningPings = false,
                            recheckingKeys = emptySet()
                        )
                    }
                }
            }
        }
    }

    fun cancelSearch() {
        _uiState.update { it.copy(searchCancelled = true) }
        searchJob?.cancel()
        showPaused()
        saveProxyCache()
        maybeStartBackgroundRecheck()
    }

    fun newSearch() {
        searchJob?.cancel()
        startSearch()
    }

    fun checkCustomProxies(rawText: String) {
        val parsed = ProxyLinkParser.parse(rawText).distinctBy { it.key }
        if (parsed.isEmpty()) {
            _uiState.update {
                it.copy(
                    feedError = getApplication<Application>().getString(R.string.no_proxies_found)
                )
            }
            return
        }

        stopBackgroundRecheck()
        searchJob?.cancel()
        sortedList.clear()
        collectFound = parsed.size
        checkedCount = 0
        finalDiscovered = parsed.size
        refineTotal = 0
        refineDone = 0
        refiningPings = false
        val sortMode = _uiState.value.sortMode
        sortedList.setSortMode(sortMode)

        val checkingText = getApplication<Application>().getString(R.string.check_progress, 0, parsed.size)
        _uiState.update {
            SearchUiState(
                hasSearched = true,
                isSearching = true,
                showPullHint = false,
                fetchComplete = true,
                isCustomCheck = true,
                progressText = checkingText,
                statusText = checkingText,
                activityLine = "",
                sortMode = sortMode
            )
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val timeoutMs = settingsStore.proxyCheckTimeoutMs.first()
                val healthService = ProxyHealthService(mtProxyTimeoutMs = timeoutMs)

                // Phase 1: availability scan
                healthService.scanAvailability(
                    proxies = parsed,
                    onProgress = { p ->
                        checkedCount = p.completed
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            updateOverallProgress()
                        }
                    },
                    onChecked = { result ->
                        if (result.isAvailable) {
                            sortedList.upsertAvailable(result.entry, result.pingMs)
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                _uiState.update {
                                    it.copy(
                                        proxies = sortedList.snapshot(),
                                        foundCount = sortedList.count,
                                        showFoundCount = true
                                    )
                                }
                            }
                        }
                    }
                )

                if (_uiState.value.searchCancelled) return@launch

                // Phase 2: accurate ping calculation
                val toRefine = sortedList.snapshot()
                if (toRefine.isNotEmpty()) {
                    refineTotal = toRefine.size
                    refineDone = 0
                    refiningPings = true
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(refiningPings = true) }
                        updateOverallProgress()
                    }

                    healthService.refinePings(
                        proxies = toRefine,
                        onProgress = { p ->
                            refineDone = p.completed
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                updateOverallProgress()
                            }
                        },
                        onChecking = { entry ->
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                _uiState.update {
                                    it.copy(recheckingKeys = it.recheckingKeys + entry.key)
                                }
                            }
                        },
                        onChecked = { result ->
                            if (result.isAvailable) {
                                sortedList.upsertAvailable(result.entry, result.pingMs)
                            } else {
                                result.entry.isAvailable = false
                                result.entry.pingMs = null
                                sortedList.remove(result.entry.key)
                            }
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                _uiState.update {
                                    it.copy(
                                        recheckingKeys = it.recheckingKeys - result.entry.key,
                                        proxies = sortedList.snapshot(),
                                        foundCount = sortedList.count,
                                        showFoundCount = sortedList.count > 0
                                    )
                                }
                            }
                        }
                    )
                }

                refiningPings = false
                if (!_uiState.value.searchCancelled) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                refiningPings = false,
                                progressPercent = 100,
                                activityLine = "",
                                recheckingKeys = emptySet(),
                                showPullHint = sortedList.count > 0,
                                statusText = getApplication<Application>().getString(
                                    R.string.custom_check_done,
                                    sortedList.count
                                )
                            )
                        }
                    }
                    saveProxyCache()
                    maybeStartBackgroundRecheck()
                }
            } catch (_: CancellationException) {
                refiningPings = false
                withContext(Dispatchers.Main) { showPaused() }
                saveProxyCache()
            } catch (e: Exception) {
                refiningPings = false
                Log.e("PodletsTGProxy", "Custom check error", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            refiningPings = false,
                            feedError = e.message ?: e.toString()
                        )
                    }
                }
                saveProxyCache()
            } finally {
                refiningPings = false
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            refiningPings = false,
                            recheckingKeys = emptySet()
                        )
                    }
                }
            }
        }
    }

    fun recheckProxy(entry: ProxyEntry) {
        viewModelScope.launch {
            _uiState.update { it.copy(recheckingKeys = it.recheckingKeys + entry.key) }
            val timeoutMs = settingsStore.proxyCheckTimeoutMs.first()
            val healthService = ProxyHealthService(mtProxyTimeoutMs = timeoutMs)
            val result = withContext(Dispatchers.IO) { healthService.checkOne(entry) }
            if (result.isAvailable) {
                sortedList.upsertAvailable(entry, result.pingMs)
            } else {
                entry.isAvailable = false
                entry.pingMs = null
                sortedList.remove(entry.key)
            }
            val logLine = "${entry.displayLabel} — " + (if (result.isAvailable) "OK (${result.pingMs}ms)" else "FAIL")
            Log.i("PodletsTGProxy", "Rechecked: $logLine")
            _uiState.update {
                it.copy(
                    recheckingKeys = it.recheckingKeys - entry.key,
                    proxies = sortedList.snapshot(),
                    foundCount = sortedList.count,
                    showFoundCount = sortedList.count > 0,
                    activityLine = logLine
                )
            }
            saveProxyCache()
        }
    }

    private fun maybeStartBackgroundRecheck() {
        stopBackgroundRecheck()
        val state = _uiState.value
        if (!appInForeground) return
        if (state.isSearching) return
        if (sortedList.count == 0) return

        backgroundRecheckJob = viewModelScope.launch(Dispatchers.IO) {
            val healthService = ProxyHealthService()
            while (coroutineContext.isActive) {
                delay(BACKGROUND_RECHECK_INTERVAL_MS)
                if (!appInForeground) continue
                val ui = _uiState.value
                if (ui.isSearching) continue

                val top = sortedList.snapshot()
                    .sortedWith(SortedProxyList.comparatorFor(SortMode.Rating))
                    .take(BACKGROUND_RECHECK_TOP_N)
                if (top.isEmpty()) continue

                for (entry in top) {
                    if (!coroutineContext.isActive) break
                    if (!appInForeground || _uiState.value.isSearching) break

                    withContext(Dispatchers.Main.immediate) {
                        _uiState.update { it.copy(recheckingKeys = it.recheckingKeys + entry.key) }
                    }
                    try {
                        val result = healthService.checkLight(entry)
                        if (result.isAvailable) {
                            sortedList.upsertAvailable(entry, result.pingMs)
                        } else {
                            entry.isAvailable = false
                            entry.pingMs = null
                            sortedList.remove(entry.key)
                        }
                        withContext(Dispatchers.Main.immediate) {
                            _uiState.update {
                                it.copy(
                                    proxies = sortedList.snapshot(),
                                    foundCount = sortedList.count,
                                    showFoundCount = sortedList.count > 0
                                )
                            }
                        }
                    } catch (_: CancellationException) {
                        throw CancellationException()
                    } catch (_: Exception) {
                    } finally {
                        withContext(Dispatchers.Main.immediate) {
                            _uiState.update { it.copy(recheckingKeys = it.recheckingKeys - entry.key) }
                        }
                    }
                }
                saveProxyCache()
            }
        }
    }

    private fun saveProxyCache() {
        val snapshot = sortedList.snapshot()
        val isCustom = _uiState.value.isCustomCheck
        viewModelScope.launch(Dispatchers.IO) {
            ProxyCacheStore.save(getApplication(), snapshot, isCustom)
        }
    }

    private fun stopBackgroundRecheck() {
        backgroundRecheckJob?.cancel()
        backgroundRecheckJob = null
    }

    private fun postActivityLine(line: String) {
        if (line.isBlank()) return
        lastCollectLog = line
        Log.i("PodletsTGProxy", line)
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _uiState.update {
                it.copy(activityLine = line, statusText = line)
            }
        }
    }

    fun clearFeedError() {
        _uiState.update { it.copy(feedError = null, sourcesEmpty = false) }
    }

    fun restoreDefaultSources() {
        viewModelScope.launch {
            settingsStore.saveProxyFeedSources(
                listOf(
                    "https://tgstat.com/channel/@ProxyMTProto",
                    "https://tgstat.ru/channel/@ProxyMTProto"
                )
            )
            _uiState.update { it.copy(sourcesEmpty = false, feedError = null) }
            startSearch()
        }
    }

    private fun updateOverallProgress() {
        val state = _uiState.value
        if (state.searchCancelled || !state.isSearching) return

        val percent = when {
            !state.fetchComplete -> if (collectFound == 0) 5 else 35
            refiningPings -> {
                val total = maxOf(1, refineTotal)
                70 + minOf(30, (30.0 * refineDone / total).toInt())
            }
            else -> {
                val total = maxOf(1, finalDiscovered)
                35 + minOf(35, (35.0 * checkedCount / total).toInt())
            }
        }

        val progressText = formatProgressText(state)
        _uiState.update {
            it.copy(
                progressPercent = percent.coerceIn(0, 100),
                progressText = progressText,
                foundCount = sortedList.count,
                showFoundCount = state.fetchComplete && sortedList.count > 0
            )
        }
    }

    private fun formatProgressText(state: SearchUiState): String {
        val app = getApplication<Application>()
        if (state.searchCancelled) {
            return app.getString(R.string.search_paused)
        }
        if (!state.fetchComplete) {
            if (collectFound == 0) return app.getString(R.string.connecting)
            return app.getString(R.string.collect_progress, collectFound)
        }
        if (refiningPings) {
            val total = maxOf(1, refineTotal)
            return if (refineDone >= total) {
                app.getString(R.string.refine_done, refineDone, total)
            } else {
                app.getString(R.string.refine_progress, refineDone, total)
            }
        }
        val total = maxOf(1, finalDiscovered)
        return if (checkedCount >= total) {
            app.getString(R.string.check_done, checkedCount, total)
        } else {
            app.getString(R.string.check_progress, checkedCount, total)
        }
    }

    private fun showPaused() {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                isSearching = false,
                refiningPings = false,
                progressPercent = 0,
                progressText = app.getString(R.string.search_paused),
                statusText = app.getString(R.string.search_paused),
                showFoundCount = sortedList.count > 0,
                recheckingKeys = emptySet(),
                showPullHint = sortedList.count > 0
            )
        }
    }

    companion object {
        private const val BACKGROUND_RECHECK_INTERVAL_MS = 3 * 60_000L
        private const val BACKGROUND_RECHECK_TOP_N = 5
    }
}
