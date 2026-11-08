package com.podlets.tgproxy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.podlets.tgproxy.R
import com.podlets.tgproxy.data.TelegramLinkBuilder
import com.podlets.tgproxy.domain.ProxyEntry
import com.podlets.tgproxy.domain.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchProxyTab(
    viewModel: SearchViewModel,
    bottomPadding: Dp = 0.dp
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    var showCustomProxyDialog by remember { mutableStateOf(false) }
    var showOpenMultipleDialog by remember { mutableStateOf(false) }

    val copyAllProxies = {
        if (state.proxies.isNotEmpty()) {
            val allText = state.proxies.joinToString("\n") { TelegramLinkBuilder.buildTg(it) }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Telegram MTProto", allText))
            Toast.makeText(context, context.getString(R.string.copied_all_count, state.proxies.size), Toast.LENGTH_SHORT).show()
        }
    }

    val openProxiesInTelegram: (Int) -> Unit = { count ->
        val target = state.proxies.take(count)
        if (target.isNotEmpty()) {
            scope.launch {
                Toast.makeText(
                    context,
                    context.getString(R.string.opening_in_telegram_toast, target.size),
                    Toast.LENGTH_SHORT
                ).show()
                // Reverse order so that #1 (fastest) is opened last and stays on top of the stack in Telegram
                for (proxy in target.reversed()) {
                    TelegramLauncher.openProxy(context, proxy)
                    delay(150L)
                }
            }
        }
    }

    var lastSortMode by remember { mutableStateOf(state.sortMode) }
    LaunchedEffect(state.sortMode) {
        if (state.sortMode != lastSortMode) {
            lastSortMode = state.sortMode
            if (state.proxies.isNotEmpty()) {
                listState.scrollToItem(0)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Action to check own proxies
            FilledTonalButton(
                onClick = { showCustomProxyDialog = true },
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.check_custom_proxies),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Right: Count Badge & Action Buttons
            if (state.proxies.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = if (state.isCustomCheck) {
                                stringResource(R.string.verified_count, state.proxies.size)
                            } else {
                                stringResource(R.string.found_count, state.proxies.size)
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    FilledTonalIconButton(
                        onClick = copyAllProxies,
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy_all),
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { showOpenMultipleDialog = true },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_telegram_logo),
                            contentDescription = stringResource(R.string.open_all_in_telegram),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // Top: Sort bar if list has proxies (only for public feeds, not custom check)
        if (state.proxies.isNotEmpty() && !state.isCustomCheck) {
            SortBar(
                sortMode = state.sortMode,
                onSortMode = { viewModel.setSortMode(it) }
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.showPullHint && !state.isSearching && state.proxies.isNotEmpty()) {
            PullHintBanner(onDismiss = { viewModel.dismissPullHint() })
            Spacer(Modifier.height(8.dp))
        }

        // Center: List of proxies or Welcome / Error state
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            PullToRefreshBox(
                isRefreshing = state.isSearching,
                onRefresh = { viewModel.newSearch() },
                state = pullState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.proxies.isEmpty() && !state.isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.size(86.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TravelExplore,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (!state.hasSearched) {
                                        stringResource(R.string.search_welcome_title)
                                    } else {
                                        stringResource(R.string.feed_failed)
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (!state.hasSearched) {
                                        stringResource(R.string.search_welcome_desc)
                                    } else {
                                        stringResource(R.string.pull_hint)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 21.sp
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FeatureChip(text = "⚡ " + stringResource(R.string.feature_live_ping))
                                FeatureChip(text = "📡 " + stringResource(R.string.feature_tgstat))
                                FeatureChip(text = "✈ " + stringResource(R.string.feature_one_click))
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwipeDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.pull_hint),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp)
                    ) {
                        items(state.proxies, key = { it.key }) { entry ->
                            ProxyCard(
                                entry = entry,
                                isCustomCheck = state.isCustomCheck,
                                isRechecking = entry.key in state.recheckingKeys,
                                onOpen = { TelegramLauncher.openProxy(context, it) },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val link = TelegramLinkBuilder.buildTg(entry)
                                    val clip = ClipData.newPlainText("Telegram MTProto", link)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                                },
                                onRecheck = { viewModel.recheckProxy(it) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Bottom: Search Control Card
        AppSectionCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.isSearching) {
                            state.progressText
                        } else {
                            stringResource(R.string.pull_hint)
                        },
                        style = if (state.isSearching) {
                            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp)
                        },
                        color = if (state.isSearching) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(10.dp))

                    if (state.isSearching) {
                        Button(
                            onClick = { viewModel.cancelSearch() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.cancel_search), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.newSearch() },
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (state.proxies.isEmpty()) stringResource(R.string.start_search)
                                else stringResource(R.string.new_search),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (state.activityLine.isNotEmpty() && state.isSearching) {
                    Text(
                        text = state.activityLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.isSearching) {
                    LinearProgressIndicator(
                        progress = { state.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    state.feedError?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearFeedError() },
            title = { Text(stringResource(R.string.app_name)) },
            text = { Text(msg) },
            confirmButton = {
                if (state.sourcesEmpty) {
                    Button(onClick = { viewModel.restoreDefaultSources() }) {
                        Text(stringResource(R.string.use_default_sources))
                    }
                } else {
                    Button(onClick = { viewModel.clearFeedError() }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = if (state.sourcesEmpty) {
                {
                    TextButton(onClick = { viewModel.clearFeedError() }) {
                        Text(stringResource(R.string.close))
                    }
                }
            } else null
        )
    }

    if (showCustomProxyDialog) {
        var customText by remember { mutableStateOf("") }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        AlertDialog(
            onDismissRequest = { showCustomProxyDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.check_custom_proxies_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.check_custom_proxies_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp),
                        placeholder = {
                            Text(
                                "tg://proxy?server=...\nhttps://t.me/proxy?...\nServer: ... Port: ... Secret: ...",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val clipText = clipboard?.primaryClip?.let { clip ->
                                    if (clip.itemCount > 0) clip.getItemAt(0).coerceToText(context)?.toString() else null
                                }
                                if (!clipText.isNullOrBlank()) {
                                    customText = if (customText.isBlank()) clipText else "$customText\n$clipText"
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.paste_from_clipboard))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textToProcess = customText.trim()
                        if (textToProcess.isNotEmpty()) {
                            showCustomProxyDialog = false
                            viewModel.checkCustomProxies(textToProcess)
                        }
                    },
                    enabled = customText.isNotBlank()
                ) {
                    Text(stringResource(R.string.check_button), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomProxyDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showOpenMultipleDialog && state.proxies.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showOpenMultipleDialog = false },
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_telegram_logo),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.open_in_telegram_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.open_in_telegram_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))

                    val total = state.proxies.size

                    // Option 1: Native cards stack (or direct connect if single proxy)
                    FilledTonalButton(
                        onClick = {
                            showOpenMultipleDialog = false
                            openProxiesInTelegram(minOf(5, total))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = when {
                                total == 1 -> stringResource(R.string.connect_in_telegram)
                                total <= 5 -> stringResource(R.string.open_all_stack, total)
                                else -> stringResource(R.string.open_top_5_stack)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Option 2: Send to Telegram chat / Saved Messages
                    Button(
                        onClick = {
                            showOpenMultipleDialog = false
                            TelegramLauncher.shareProxies(context, state.proxies)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (total == 1) {
                                stringResource(R.string.send_to_telegram)
                            } else {
                                stringResource(R.string.send_all_to_telegram, total)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOpenMultipleDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PullHintBanner(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SwipeDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.pull_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.pull_hint_dismiss),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SortBar(
    sortMode: SortMode,
    onSortMode: (SortMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SortTabItem(
                label = stringResource(R.string.sort_ping),
                icon = "⚡",
                selected = sortMode == SortMode.Ping,
                onClick = { onSortMode(SortMode.Ping) },
                modifier = Modifier.weight(1f)
            )
            SortTabItem(
                label = stringResource(R.string.sort_rating),
                icon = "★",
                selected = sortMode == SortMode.Rating,
                onClick = { onSortMode(SortMode.Rating) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SortTabItem(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProxyCard(
    entry: ProxyEntry,
    isCustomCheck: Boolean,
    isRechecking: Boolean,
    onOpen: (ProxyEntry) -> Unit,
    onCopy: () -> Unit,
    onRecheck: (ProxyEntry) -> Unit
) {
    val accentColor = entry.pingAccentColor()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !isRechecking) { onOpen(entry) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.displayLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                // Ping Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = if (isRechecking) stringResource(R.string.rechecking) else entry.pingDisplay,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isCustomCheck && !isRechecking && entry.isAvailable && entry.reactionsCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PingColor.StarGold.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = entry.ratingDisplay,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PingColor.StarGold
                                )
                            }
                        }

                        entry.publishedCaption?.let { caption ->
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!isRechecking) {
                        Text(
                            text = stringResource(R.string.connect_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isRechecking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        FilledTonalIconButton(
                            onClick = { onRecheck(entry) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.recheck),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
