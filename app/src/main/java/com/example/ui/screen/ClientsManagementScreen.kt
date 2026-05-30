package com.example.ui.screen

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.ui.components.CampaignStatusBadge
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.HealthScoreGauge
import com.example.ui.viewmodel.SeoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsManagementScreen(
    viewModel: SeoViewModel,
    onClientClick: (Int) -> Unit,
    onAddClientClick: () -> Unit
) {
    val clients by viewModel.clients.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") } // "All", "Active", "Paused", "Review"
    var selectedSortOrder by remember { mutableStateOf("Score") } // "Score", "Name"

    val filteredSortedClients = remember(clients, searchQuery, selectedStatusFilter, selectedSortOrder) {
        var resultList = clients

        // 1. Filter by Search Query
        if (searchQuery.isNotBlank()) {
            resultList = resultList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.websiteUrl.contains(searchQuery, ignoreCase = true) ||
                        it.campaignType.contains(searchQuery, ignoreCase = true)
            }
        }

        // 2. Filter by status
        if (selectedStatusFilter != "All") {
            resultList = resultList.filter {
                it.status.equals(selectedStatusFilter, ignoreCase = true)
            }
        }

        // 3. Sort ordering
        resultList = when (selectedSortOrder) {
            "Score" -> resultList.sortedByDescending { it.healthScore }
            "Name" -> resultList.sortedBy { it.name.lowercase() }
            else -> resultList
        }

        resultList
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClientClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("onboard_client_fab")
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Onboard New Client Campaign",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // --- HEADER BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SEO Campaigns",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Search, filter and analyze tracking progress",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add shortcut link
                IconButton(
                    onClick = onAddClientClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Add Client",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SEARCH TEXT FIELD ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search domain name, brand, campaign type...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("management_search_bar"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- FILTER BADGES ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf("All", "Active", "Paused", "Review")
                    filters.forEach { filterName ->
                        val isSelected = selectedStatusFilter == filterName
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { selectedStatusFilter = filterName }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("status_filter_chip_$filterName"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filterName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // --- SORT OPTIONS DROPDOWN ---
                Box {
                    var expandedSortBox by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { expandedSortBox = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("sort_selector_box"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (selectedSortOrder == "Score") "By Score" else "By Name",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = expandedSortBox,
                        onDismissRequest = { expandedSortBox = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by SEO Score") },
                            onClick = {
                                selectedSortOrder = "Score"
                                expandedSortBox = false
                            },
                            modifier = Modifier.testTag("sort_option_score")
                        )
                        DropdownMenuItem(
                            text = { Text("Sort Alphabetically") },
                            onClick = {
                                selectedSortOrder = "Name"
                                expandedSortBox = false
                            },
                            modifier = Modifier.testTag("sort_option_name")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CLIENT LAZY LIST ---
            if (filteredSortedClients.isEmpty()) {
                EmptyPlaceholder(
                    title = "No Client Campaigns Listed",
                    description = if (searchQuery.isEmpty()) {
                        "Your SEO suite sandbox contains no active accounts. Log a new Client Campaign setup target or click synchronize."
                    } else {
                        "We couldn't resolve any campaigns matching '$searchQuery'. Keep optimizing your keyword searches!"
                    },
                    buttonText = if (searchQuery.isEmpty()) "Initiate Simulation Data" else null,
                    onButtonClick = if (searchQuery.isEmpty()) {
                        { viewModel.seedInitialSandboxData() }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSortedClients, key = { it.id }) { client ->
                        CampaignManagementCard(
                            client = client,
                            onClick = { onClientClick(client.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // clearance offset for Floating Action Button
                    }
                }
            }
        }
    }
}

@Composable
fun CampaignManagementCard(
    client: Client,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("management_client_card_${client.id}"),
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
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampaignStatusBadge(status = client.status)
                    Text(
                        text = client.campaignType.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = client.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = client.websiteUrl,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Divider vertical
            Box(
                modifier = Modifier
                    .height(54.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Score circular Gauge dial
            HealthScoreGauge(
                score = client.healthScore,
                size = 64.dp,
                strokeWidth = 5.dp
            )
        }
    }
}
