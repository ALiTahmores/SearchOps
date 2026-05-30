package com.example.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.viewmodel.SeoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseAiAssistantTab(
    viewModel: SeoViewModel
) {
    val clients by viewModel.clients.collectAsState()
    val activeClient by viewModel.selectedClient.collectAsState()
    val chatHistory by viewModel.selectedClientChat.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()

    var chatMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var clientsDropdownExpanded by remember { mutableStateOf(false) }

    // On load, if there are clients and no active client is selected, auto-select the first one so the chat is primed!
    LaunchedEffect(clients) {
        if (activeClient == null && clients.isNotEmpty()) {
            viewModel.selectClient(clients.first().id)
        }
    }

    // Auto scroll down to latest reply message
    LaunchedEffect(chatHistory.size, isChatLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        // --- 1. HEADER TITLE BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "AI Strategy Generator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Generate professional, search-optimized advisory reports",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- 2. CLIENT SELECTION FIELD ---
        if (clients.isEmpty()) {
            EmptyPlaceholder(
                title = "No SEO Campaigns Configured",
                description = "Register an active client campaign in the console to utilize our AI strategist workspace.",
                buttonText = "Register Diagnostic Client",
                onButtonClick = { /* User can navigate to campaigns screen */ },
                modifier = Modifier.weight(1f)
            )
        } else {
            // Dropdown Selector Row
            Box(
                modifier = Modifier
                    .fillModifierCompactWidth()
                    .padding(bottom = 14.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { clientsDropdownExpanded = true }
                        .testTag("ai_client_selector_box"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.Success)
                            )
                            Column {
                                Text(
                                    text = "ACTIVE AUDIT TARGET",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = activeClient?.name ?: "Select Campaign Target...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Selection Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = clientsDropdownExpanded,
                    onDismissRequest = { clientsDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    clients.forEach { c ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(text = c.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = c.websiteUrl, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                viewModel.selectClient(c.id)
                                clientsDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("ai_select_client_option_${c.id}")
                        )
                    }
                }
            }

            // --- 3. PROMPT SUITE SHORTCUT CHIPS ---
            Text(
                text = "SUGGESTED STRATEGY INSTRUCTIONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Column(
                modifier = Modifier.padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PromptTabChip(
                        label = "Schema XML-LD Code",
                        prompt = "Generate detailed Structured Schema markup in JSON-LD syntax for local businesses, matching the crawl variables in our SEO dashboard parameters.",
                        onClick = { chatMessage = it },
                        modifier = Modifier.weight(1f)
                    )
                    PromptTabChip(
                        label = "Crawl Redesigns",
                        prompt = "Analyze tracking metadata failures. Model alternative Title Tags (under 55 characters) and click-friendly Meta Descriptions mapped to the keywords list.",
                        onClick = { chatMessage = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PromptTabChip(
                        label = "Informational Gaps",
                        prompt = "Extract user queries. Outline a premium content strategy mapping relevant search intents and title headers optimized to build trust.",
                        onClick = { chatMessage = it },
                        modifier = Modifier.weight(1f)
                    )
                    PromptTabChip(
                        label = "Backlink Outreach",
                        prompt = "Code up a professional, warm email proposal to industry media sites proposing resource listing additions with our audit target links.",
                        onClick = { chatMessage = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 4. CHAT MESSAGE LOG HISTORY ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .padding(10.dp)
            ) {
                if (chatHistory.isEmpty() && !isChatLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "SearchOps Strategy Sandbox",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Pick an instruction preset or query your custom SEO request below. We utilize live crawl configurations for detailed advisory results.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 2.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(chatHistory, key = { it.id }) { chat ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // User query (Right alignment bubble, filled accent background)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("chat_bubble_user")
                                ) {
                                    Text(
                                        text = chat.prompt,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // AI Intelligence reply (Left alignment, modern card outline background)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Start)
                                        .fillMaxWidth(0.9f)
                                        .clip(RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp))
                                        .padding(12.dp)
                                        .testTag("chat_bubble_ai")
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "SearchOps Intelligence",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = chat.response,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Appended Loading indicators
                        if (isChatLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .clip(RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Reviewing crawler logs...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 5. INTERACTIVE MESSAGE FIELD ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = chatMessage,
                    onValueChange = { chatMessage = it },
                    placeholder = { Text("Ask about meta markup or search keywords...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_assistant_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (chatMessage.isNotBlank()) {
                            viewModel.sendAssistantMessage(chatMessage)
                            chatMessage = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("ai_assistant_send_button"),
                    enabled = chatMessage.isNotBlank() && !isChatLoading
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Transmit Message",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Fixed width helper layout
@Composable
fun Modifier.fillModifierCompactWidth(): Modifier {
    return this.fillMaxWidth()
}

@Composable
fun PromptTabChip(
    label: String,
    prompt: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clickable { onClick(prompt) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
