package com.example.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.Keyword
import com.example.data.model.Task
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SeoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    viewModel: SeoViewModel,
    clientId: Int,
    onBackClick: () -> Unit,
    onAskAiClick: () -> Unit,
    onClientDeleted: () -> Unit
) {
    // Select active client context
    LaunchedEffect(clientId) {
        viewModel.selectClient(clientId)
    }

    val client by viewModel.selectedClient.collectAsState()
    val isAuditing by viewModel.isAuditing.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("SERP KEYWORDS", "METRICS & GSC", "PSI SPEED LAB", "TECHNICAL CRAWLER", "ACTION TASKS")

    if (client == null) {
        FullScreenLoader(message = "Resolving campaign details...")
    } else {
        val clientData = client!!

        if (isAuditing) {
            FullScreenLoader(message = "Crawling technical properties & invoking Google Lighthouse on website ${clientData.websiteUrl}...")
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = clientData.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = clientData.websiteUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { /* Could launch browser */ }
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onAskAiClick,
                                modifier = Modifier
                                    .testTag("ai_assistant_shortcut_button")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartButton,
                                    contentDescription = "Ask AI Strategy",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                viewModel.removeClient(clientId) {
                                    onClientDeleted()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Campaign",
                                    tint = Error
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    // Segmented Tabs Header
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = activeTab == index,
                                onClick = { activeTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = if (activeTab == index) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.testTag("detail_tab_$index")
                            )
                        }
                    }

                    // Workspace Content switching
                    Box(modifier = Modifier.weight(1f)) {
                        when (activeTab) {
                            0 -> KeywordsWorkspace(viewModel)
                            1 -> MetricsPerformanceWorkspace(viewModel, clientData)
                            2 -> PageSpeedLabWorkspace(viewModel, clientData)
                            3 -> TechnicalCrawlerWorkspace(viewModel, clientData)
                            4 -> ActionTasksWorkspace(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// ==================== WORKSPACE 1: KEYWORDS ====================
@Composable
fun KeywordsWorkspace(viewModel: SeoViewModel) {
    val keywords by viewModel.selectedClientKeywords.collectAsState()
    val isGrounding by viewModel.isGroundingKeyword.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var entryModeGrounded by remember { mutableStateOf(true) } // default is grounding search!
    
    var phrase by remember { mutableStateOf("") }
    var vol by remember { mutableStateOf("") }
    var rank by remember { mutableStateOf("") }
    var diff by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SEO SERP KEYWORD TABLE",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Track real-time organic rankings using Google AI Grounding",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showForm = !showForm },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (showForm) Error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        ) {
                            Icon(
                                imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Toggle add form",
                                tint = if (showForm) Error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Add Keyword Drawer
                    AnimatedVisibility(
                        visible = showForm,
                        enter = expandVertically(tween(300)),
                        exit = shrinkVertically(tween(300))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Selector for Grounding vs Manual
                            SegmentedGroundedSelector(
                                isGrounded = entryModeGrounded,
                                onModeSelect = { entryModeGrounded = it }
                            )

                            OutlinedTextField(
                                value = phrase,
                                onValueChange = { phrase = it },
                                placeholder = { Text("Search term, e.g. custom logistics software") },
                                label = { Text("Keyword Phrase") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("keyword_phrase_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            if (!entryModeGrounded) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = vol,
                                        onValueChange = { vol = it },
                                        placeholder = { Text("1500") },
                                        label = { Text("Volume") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("keyword_volume_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = rank,
                                        onValueChange = { rank = it },
                                        placeholder = { Text("12") },
                                        label = { Text("Current Rank") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("keyword_rank_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = diff,
                                        onValueChange = { diff = it },
                                        placeholder = { Text("55") },
                                        label = { Text("Difficulty %") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("keyword_difficulty_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (phrase.isNotBlank()) {
                                        if (entryModeGrounded) {
                                            viewModel.addKeywordWithGrounding(phrase)
                                        } else {
                                            val volumeNum = vol.toIntOrNull() ?: 500
                                            val rankNum = rank.toIntOrNull() ?: 99
                                            val diffNum = diff.toIntOrNull() ?: 50
                                            viewModel.addKeyword(phrase, volumeNum, rankNum, diffNum)
                                        }
                                        phrase = ""
                                        vol = ""
                                        rank = ""
                                        diff = ""
                                        showForm = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (entryModeGrounded) Success else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_keyword_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (entryModeGrounded) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = "Grounding", modifier = Modifier.size(18.dp))
                                        Text("ANALYZE & TRACK VIA LIVE GOOGLE SEARCH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    } else {
                                        Text("SAVE STATIC MONITOR KEYWORD", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Keyword Table
            if (keywords.isEmpty()) {
                EmptyPlaceholder(
                    title = "No Tracked Keywords",
                    description = "Add target optimization phrases. Use Live Search Grounding to automatically retrieve keyword volume and ranking metrics straight from current Google results.",
                    buttonText = "Add First Keyword",
                    onButtonClick = { showForm = true }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PHRASE", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("GOOGLE RANK", modifier = Modifier.weight(1.0f), fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("EST. VOLUME", modifier = Modifier.weight(0.9f), fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("DIFFICULTY", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ACTIONS", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(keywords, key = { it.id }) { kw ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("keyword_item_${kw.id}"),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Phrase cell
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(
                                        text = kw.phrase,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Search Grounded",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Normal
                                    )
                                }

                                // Google rank cell
                                Row(
                                    modifier = Modifier.weight(1.0f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    KeywordRankChangeIndicator(
                                        current = kw.currentRank,
                                        change = kw.rankChange
                                    )
                                }

                                // Volume cell
                                Text(
                                    text = if (kw.searchVolume > 0) String.format("%,d", kw.searchVolume) else "N/A",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.9f)
                                )

                                // Difficulty cell
                                Box(
                                    modifier = Modifier.weight(0.8f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val color = if (kw.difficulty > 70) Error else if (kw.difficulty > 40) Warning else Success
                                    Text(
                                        text = "${kw.difficulty}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                }

                                // Actions cell
                                Row(
                                    modifier = Modifier.weight(0.7f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.refreshKeywordWithGrounding(kw) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh live check",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = { viewModel.removeKeyword(kw) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Keyword",
                                            tint = Error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Live Grounding Agent Loading Overlay
        if (isGrounding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {}, // absorb clicks to prevent any double interact
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "INITIALIZING REAL-TIME SEARCH AGENT",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Our search grounding AI is performing live Google queries, indexing target keyword configurations, analyzing organic search listings, and matching domain visibility data. This takes standard crawler indexation time.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedGroundedSelector(
    isGrounded: Boolean,
    onModeSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isGrounded) Success.copy(alpha = 0.15f) else Color.Transparent)
                .clickable { onModeSelect(true) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isGrounded) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "AI Search Grounding",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGrounded) Success else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (!isGrounded) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                .clickable { onModeSelect(false) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (!isGrounded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Manual Input",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!isGrounded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== WORKSPACE 2: SITE AUDIT ====================
@Composable
fun AuditWorkspace(viewModel: SeoViewModel, client: Client) {
    val audit by viewModel.selectedClientAudit.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (audit == null) {
            EmptyPlaceholder(
                title = "No audits executed on website",
                description = "Run a crawler diagnosis. We will analyze metadata tags, loading times and execute an automated Gemini diagnostics report.",
                buttonText = "CRAWL & AUDIT NOW",
                onButtonClick = { viewModel.runTechnicalAudit(client.id) }
            )
        } else {
            val auditData = audit!!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Score meter
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("audit_score_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AUDIT ANALYSIS REPORT",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Status: Crawled via Gemini Bot", fontSize = 13.sp, color = Success, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Column {
                                        Text(text = "${auditData.criticalIssues}", fontSize = 18.sp, color = Error, fontWeight = FontWeight.Black)
                                        Text(text = "Critical", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column {
                                        Text(text = "${auditData.warnings}", fontSize = 18.sp, color = Warning, fontWeight = FontWeight.Black)
                                        Text(text = "Warnings", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column {
                                        Text(text = "${auditData.passedChecks}", fontSize = 18.sp, color = Success, fontWeight = FontWeight.Black)
                                        Text(text = "Passed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            HealthScoreGauge(score = auditData.score, size = 86.dp, strokeWidth = 7.dp)
                        }
                    }
                }

                // Crawl Button
                item {
                    Button(
                        onClick = { viewModel.runTechnicalAudit(client.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("run_audit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "RE-LAUNCH SEO CRAWLER / AUDIT", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // Recommendation listings
                item {
                    Text(
                        text = "DETAILED CRAWL AUDIT BREAKDOWN",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }

                // Parse the issues list line by line cleanly with visual markers!
                val lines = auditData.issuesJson.split("\n").filter { it.isNotBlank() }
                items(lines) { line ->
                    val (icon, color) = when {
                        line.trim().startsWith("[CRITICAL]") -> Pair(Icons.Default.Cancel, Error)
                        line.trim().startsWith("[WARNING]") -> Pair(Icons.Default.Error, Warning)
                        else -> Pair(Icons.Default.CheckCircle, Success)
                    }
                    val displayText = line
                        .replace("[CRITICAL]", "")
                        .replace("[WARNING]", "")
                        .replace("[PASSED]", "")
                        .trim()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp,
                            if (color == Error) Error.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp).padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = displayText,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (color == Error) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ==================== WORKSPACE 3: ACTION TASKS ====================
@Composable
fun ActionTasksWorkspace(viewModel: SeoViewModel) {
    val tasks by viewModel.selectedClientTasks.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var taskDue by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("Medium") }

    val priorities = listOf("High", "Medium", "Low")

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CAMPAIGN ACTION TASKS",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Optimize parameters dynamically to fix crawler errors",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showForm = !showForm },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showForm) Error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    ) {
                        Icon(
                            imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Toggle add form",
                            tint = if (showForm) Error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Add Task Inline Drawer
                AnimatedVisibility(
                    visible = showForm,
                    enter = expandVertically(tween(300)),
                    exit = shrinkVertically(tween(300))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            placeholder = { Text("Task Title, e.g. Add Schema structural labels") },
                            label = { Text("Task Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_title_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = taskDesc,
                            onValueChange = { taskDesc = it },
                            placeholder = { Text("Outline details of technical changes needed.") },
                            label = { Text("Task Description") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_desc_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = taskDue,
                                onValueChange = { taskDue = it },
                                placeholder = { Text("e.g. June 15, 2026") },
                                label = { Text("Due Date") },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("task_due_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Priority", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val nextIndex = (priorities.indexOf(taskPriority) + 1) % priorities.size
                                            taskPriority = priorities[nextIndex]
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = taskPriority.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        Button(
                            onClick = {
                                if (taskTitle.isNotBlank()) {
                                    viewModel.addTask(taskTitle, taskDesc, taskDue.ifBlank { "Within 7 days" }, taskPriority)
                                    taskTitle = ""
                                    taskDesc = ""
                                    taskDue = ""
                                    taskPriority = "Medium"
                                    showForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_task_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SAVE CAMPAIGN TASK", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Action tasks checklist
        if (tasks.isEmpty()) {
            EmptyPlaceholder(
                title = "No Actions Required",
                description = "All tasks have been fully debugged or crawls didn't identify critical warnings for this client.",
                buttonText = "Create Manual Action",
                onButtonClick = { showForm = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_item_${task.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Success,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("task_check_${task.id}")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (task.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = task.description,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Due: ${task.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    PriorityBadge(priority = task.priority)
                                }
                            }
                            IconButton(onClick = { viewModel.removeTask(task.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Task",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ==================== WORKSPACE 4: GOOGLE ANALYTICS & SEARCH CONSOLE ====================
@Composable
fun MetricsPerformanceWorkspace(viewModel: SeoViewModel, client: Client) {
    val googleToken by viewModel.googleAccessToken.collectAsState()
    val ga4PropConfigId by viewModel.ga4PropertyConfigurationId.collectAsState()

    var gscReport by remember { mutableStateOf<com.example.data.remote.GscReport?>(null) }
    var ga4Report by remember { mutableStateOf<com.example.data.remote.Ga4Report?>(null) }
    var isLoadingMetrics by remember { mutableStateOf(false) }
    var metricsError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(googleToken, client.websiteUrl, client.gscSiteUrl) {
        if (!googleToken.isNullOrEmpty()) {
            isLoadingMetrics = true
            metricsError = null
            try {
                val siteUrl = if (!client.gscSiteUrl.isNullOrEmpty()) client.gscSiteUrl else client.websiteUrl
                val ga4Id = if (!client.ga4PropertyId.isNullOrEmpty()) client.ga4PropertyId else ga4PropConfigId

                gscReport = com.example.data.remote.SeoIntegrationService.fetchSearchConsoleReport(siteUrl, googleToken)
                ga4Report = com.example.data.remote.SeoIntegrationService.fetchAnalyticsReport(ga4Id, googleToken)
            } catch (e: Exception) {
                metricsError = e.localizedMessage
            } finally {
                isLoadingMetrics = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (googleToken.isNullOrEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("gsc_offline_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                            Text(text = "Google Analytics & GSC Integrations Offline", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = "To fetch live Search Console clicks and Google Analytics 4 active audiences, configure your Google OAuth Connection Hub in System Settings.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Configured Mapping Context:\n• Target Host: ${client.websiteUrl}\n• Search Console URI: ${client.gscSiteUrl?.ifEmpty { "Not set" } ?: "sc-domain:" + client.websiteUrl}\n• GA4 Property ID: $ga4PropConfigId",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        } else if (isLoadingMetrics) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (metricsError != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("gsc_error_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Authentication Handshake Failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        Text(text = metricsError ?: "Verification Timeout", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "The Google OAuth access token might have expired. Generate a new token in System Settings.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            val gsc = gscReport
            val ga4 = ga4Report

            if (gsc != null && ga4 != null) {
                item {
                    Text(text = "GOOGLE SEARCH CONSOLE REPORT", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricWidget(title = "Total Clicks", value = "${gsc.clicks}", subtitle = "Organic click sessions", modifier = Modifier.weight(1f))
                        MetricWidget(title = "Impressions", value = "${gsc.impressions}", subtitle = "Google appearances", modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricWidget(title = "Average CTR", value = "${(gsc.ctr * 100).coerceIn(0.0, 100.0).toString().take(4)}%", subtitle = "Click-Through Ratio", modifier = Modifier.weight(1f))
                        MetricWidget(title = "Average Position", value = "${gsc.averagePosition.toString().take(4)}", subtitle = "Average SERP Rank", modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("gsc_queries_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Connected Search Analytics", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            gsc.queries.forEach { qr ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = qr.query, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End), modifier = Modifier.weight(0.7f)) {
                                        Text(text = "C: ${qr.clicks}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "R: ${qr.position.toString().take(4)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(text = "GOOGLE ANALYTICS 4 AUDIENCE TELEMETRY", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricWidget(title = "Active Users", value = "${ga4.activeUsers}", subtitle = "Unique users count", modifier = Modifier.weight(1f))
                        MetricWidget(title = "Total Sessions", value = "${ga4.sessions}", subtitle = "Organic traffic sessions", modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricWidget(title = "Engagement Rate", value = "${(ga4.engagementRate * 100).toString().take(4)}%", subtitle = "User interaction index", modifier = Modifier.weight(1f))
                        MetricWidget(title = "Goal Conversions", value = "${ga4.conversions}", subtitle = "Total events count", modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("ga4_channels_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Traffic Channels Mapping", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ga4.trafficSources.forEach { ts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = ts.first, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Text(text = "${ts.second} clicks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(text = "Synthesizing credentials...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MetricWidget(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==================== WORKSPACE 5: PAGESPEED LABS ====================
@Composable
fun PageSpeedLabWorkspace(viewModel: SeoViewModel, client: Client) {
    val audit by viewModel.selectedClientAudit.collectAsState()
    var selectedStrategy by remember { mutableStateOf("MOBILE") }
    val isAuditing by viewModel.isAuditing.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("psi_strategy_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Lighthouse Emulated Agent", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MOBILE", "DESKTOP").forEach { str ->
                            val isSel = selectedStrategy == str
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .clickable { selectedStrategy = str }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = str, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.runTechnicalAudit(client.id, selectedStrategy) },
                enabled = !isAuditing,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("run_lighthouse_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isAuditing) "AUDITING SPEED LAB..." else "RUN GOOGLE PAGESPEED AUDIT", fontWeight = FontWeight.Bold)
            }
        }

        if (audit == null) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No speed audit records detected. Click button above to query.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val ad = audit!!
            item {
                Text(text = "LIGHTHOUSE SPEED METRICS (${ad.strategy.uppercase()})", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth())
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    GaugeWidget(label = "Performance", score = ad.performanceScore, modifier = Modifier.weight(1f))
                    GaugeWidget(label = "Accessibility", score = ad.accessibilityScore, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    GaugeWidget(label = "Best Practices", score = ad.bestPracticesScore, modifier = Modifier.weight(1f))
                    GaugeWidget(label = "SEO Score", score = ad.seoScore, modifier = Modifier.weight(1f))
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("psi_telemetry_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "PageSpeed Web Core Vitals", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        MetricRow(name = "Largest Contentful Paint (LCP)", value = ad.lcp.ifEmpty { "N/A" })
                        MetricRow(name = "Cumulative Layout Shift (CLS)", value = ad.cls.ifEmpty { "N/A" })
                        MetricRow(name = "First Contentful Paint (FCP)", value = ad.fcp.ifEmpty { "N/A" })
                        MetricRow(name = "Interaction to Next Paint (INP)", value = ad.inp.ifEmpty { "N/A" })
                        MetricRow(name = "Time to First Byte (TTFB)", value = ad.ttfb.ifEmpty { "N/A" })
                    }
                }
            }

            item {
                Text(text = "SPEED OPTIMIZATION OPPORTUNITIES", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }

            val lines = ad.issuesJson.split("\n").filter { it.contains("Speed recommendation:") || it.contains("speed recommendation:") || it.contains("Speed Suggestion:") }
            if (lines.isEmpty()) {
                item {
                    Text(text = "No critical rendering bottlenecks flagged. Layouts rendering optimally.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(lines) { rawLine ->
                    val cl = rawLine.replace("[WARNING] Speed recommendation:", "").replace("[WARNING] Speed Suggestion:", "").trim()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = Warning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = cl, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GaugeWidget(label: String, score: Int, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(8.dp)) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.size(54.dp),
                color = if (score >= 90) Color(0xFF2E7D32) else if (score >= 50) Color(0xFFEF6C00) else Color(0xFFC62828),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 5.dp
            )
            Text(text = "$score", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MetricRow(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ==================== WORKSPACE 6: JSOUP TECHNICAL CRAWLER ====================
@Composable
fun TechnicalCrawlerWorkspace(viewModel: SeoViewModel, client: Client) {
    val audit by viewModel.selectedClientAudit.collectAsState()
    val isAuditing by viewModel.isAuditing.collectAsState()

    val isGeneratingStrategy by viewModel.isGeneratingStrategy.collectAsState()
    val aiStrategyRes by viewModel.aiStrategyResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Button(
                onClick = { viewModel.runTechnicalAudit(client.id) },
                enabled = !isAuditing,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("run_crawler_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.TravelExplore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isAuditing) "SPIDERING HTML..." else "LAUNCH JSOUP TECHNICAL CRAWLER", fontWeight = FontWeight.Bold)
            }
        }

        if (audit == null) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No crawl diagnostic logs found. Trigger Jsoup parser above.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val ad = audit!!

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("crawler_summary_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "ON-PAGE CRITICAL METRIC LOGS", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Parsing: Robots.txt Rules, Structured Data", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = "Schema tags: ${ad.structuredDataCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = "Robots: ${if (ad.hasRobotsTxt) "YES" else "NO"}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (ad.hasRobotsTxt) Success else Warning)
                                Text(text = "Sitemaps: ${ad.sitemapsCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HealthScoreGauge(score = ad.score, size = 68.dp, strokeWidth = 5.dp)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("gemini_strategy_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(text = "Gemini Campaign Strategy Center", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = "Binds site technical crawled attributes, PageSpeed scores, GSC queries click curves, and GA4 views directly into Gemini model. Produces complete promotional-free technical blueprints.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isGeneratingStrategy) {
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            Button(
                                onClick = { viewModel.generateStrategyReport(client.id) },
                                modifier = Modifier.fillMaxWidth().height(38.dp).testTag("generate_strategy_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("COMPILE & RE-LAUNCH AI PLANNERS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!aiStrategyRes.isNullOrEmpty()) {
                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            Text(text = "COMPILED ROADMAP STRATEGY FINDINGS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = aiStrategyRes!!,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(text = "CRAWL VERIFIED EXECUTABLE STATEMENTS", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth())
            }

            val lines = ad.issuesJson.split("\n").filter { !it.contains("recommendation:") && !it.contains("Metrics:") && !it.contains("Strategy:") && !it.contains("Vitals") && it.isNotBlank() }
            items(lines) { line ->
                val (ic, col) = when {
                    line.trim().startsWith("[CRITICAL]") -> Pair(Icons.Default.Cancel, Error)
                    line.trim().startsWith("[WARNING]") -> Pair(Icons.Default.Error, Warning)
                    else -> Pair(Icons.Default.CheckCircle, Success)
                }
                val txt = line.replace("[CRITICAL]", "").replace("[WARNING]", "").replace("[PASSED]", "").trim()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(imageVector = ic, contentDescription = null, tint = col, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = txt, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
