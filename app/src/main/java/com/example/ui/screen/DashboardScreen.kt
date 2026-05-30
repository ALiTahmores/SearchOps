package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CampaignStatusBadge
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.HealthScoreGauge
import com.example.ui.theme.*
import com.example.ui.viewmodel.GlobalStats
import com.example.ui.viewmodel.SeoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SeoViewModel,
    onClientClick: (Int) -> Unit,
    onAddClientClick: () -> Unit
) {
    val clients by viewModel.clients.collectAsState()
    val globalStats by viewModel.globalStats.collectAsState()
    var searchKeyword by remember { mutableStateOf("") }

    val filteredClients = remember(clients, searchKeyword) {
        if (searchKeyword.isBlank()) clients else {
            clients.filter {
                it.name.contains(searchKeyword, ignoreCase = true) ||
                        it.websiteUrl.contains(searchKeyword, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SEOPulse Agency",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                fontSize = 26.sp
                            )
                        )
                        Text(
                            text = "Enterprise SEO Command Center",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        )
                    }
                    IconButton(
                        onClick = { viewModel.seedInitialSandboxData() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = CardSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Trigger seed reload",
                            tint = PrimaryDark
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClientClick,
                containerColor = PrimaryDark,
                contentColor = OnPrimaryDark,
                modifier = Modifier
                    .testTag("add_client_fab")
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Client",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Global Statistics Dashboard Panel
            item {
                GlobalStatsPanel(stats = globalStats)
            }

            // Search Filter Row
            item {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    placeholder = { Text("Search clients or websites...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_search_input"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchKeyword.isNotEmpty()) {
                            IconButton(onClick = { searchKeyword = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Client Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAMPAIGNS (${filteredClients.size})",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // List of Client Campaign Cards
            if (filteredClients.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        title = "No campaigns found",
                        description = if (searchKeyword.isEmpty()) {
                            "You haven't onboarded any clients yet. Click '+' bottom-right to create one."
                        } else {
                            "No clients match '$searchKeyword'. Try checking the spelling."
                        },
                        buttonText = if (searchKeyword.isEmpty()) "Seed Sandbox Data" else null,
                        onButtonClick = if (searchKeyword.isEmpty()) {
                            { viewModel.seedInitialSandboxData() }
                        } else null
                    )
                }
            } else {
                items(filteredClients, key = { it.id }) { client ->
                    ClientCampaignCard(
                        client = client,
                        onClick = { onClientClick(client.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Offset height of FAB + padding
            }
        }
    }
}

// --- Global Stats Grid Panel ---
@Composable
fun GlobalStatsPanel(stats: GlobalStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PORTFOLIO METRICS",
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Large Score Circular Chart representation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    HealthScoreGauge(score = stats.averageHealthScore, size = 80.dp, strokeWidth = 6.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Portfolio Avg",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(90.dp)
                        .width(1.dp)
                        .background(BorderColor)
                )

                // Stats Breakdown items
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricRow(
                        label = "Total Clients",
                        value = stats.totalClients.toString(),
                        icon = Icons.Default.BusinessCenter,
                        color = PrimaryDark
                    )
                    StatMetricRow(
                        label = "Active Campaigns",
                        value = stats.activeCampaignsCount.toString(),
                        icon = Icons.Default.Speed,
                        color = AccentGreen
                    )
                    StatMetricRow(
                        label = "Pending Tasks",
                        value = stats.pendingTasksCount.toString(),
                        icon = Icons.Default.TaskAlt,
                        color = SoftYellow
                    )
                }
            }
        }
    }
}

@Composable
fun StatMetricRow(label: String, value: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = value, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            Text(text = label, fontSize = 11.sp, color = TextSecondary, lineHeight = 12.sp)
        }
    }
}

// --- Individual Client Campaign card ---
@Composable
fun ClientCampaignCard(
    client: com.example.data.model.Client,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("client_card_${client.id}"),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CampaignStatusBadge(status = client.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = client.campaignType,
                        color = PrimaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = client.name,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = client.websiteUrl,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Simple visual separation
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
                    .background(BorderColor)
                    .padding(horizontal = 12.dp)
            )
            // Health gauge representing SEO score
            HealthScoreGauge(
                score = client.healthScore,
                size = 64.dp,
                strokeWidth = 5.dp
            )
        }
    }
}
