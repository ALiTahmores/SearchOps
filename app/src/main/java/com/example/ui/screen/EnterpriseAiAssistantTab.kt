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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Auto-select first client if none selected
    LaunchedEffect(clients) {
        if (activeClient == null && clients.isNotEmpty()) {
            viewModel.selectClient(clients.first().id)
        }
    }

    // Auto scroll to latest reply
    LaunchedEffect(chatHistory.size, isChatLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // --- Header Title ---
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
                        text = "دستیار هوش مصنوعی سئو",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "تدوین و برنامه‌ریزی تخصصی استراتژی‌های سئو و چیدمان محتوا با هوش مصنوعی",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (clients.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "وب‌سایتی برای تحلیل یافت نشد",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "برای گفتگو با دستیار هوش مصنوعی و دریافت توصیه‌ها، ابتدا وب‌سایتی را در تب 'تحلیل سایت' تحلیل کنید.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // --- Active Client Selection Dropdown ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(com.example.ui.theme.Success)
                                )
                                Column {
                                    Text(
                                        text = "انتخاب سایت هدف جهت مشاور هوشمند:",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = activeClient?.name ?: "یک سایت را انتخاب کنید...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = clientsDropdownExpanded,
                        onDismissRequest = { clientsDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        clients.forEach { c ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

                // --- Strategy Preset Chips ---
                Text(
                    text = "پیشنهادهای آماده برای گفتگو با هوش مصنوعی:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetStrategyChip(
                            label = "بهینه‌سازی سئو محتوا",
                            prompt = "لطفاً با توجه به ساختار سایت من، یک استراتژی محتوای غنی برای پوشش موضوعی کلمات کلیدی هدف با رعایت تگ‌های هدینگ و فواصل لینک‌سازی داخلی پیشنهاد دهید.",
                            onClick = { chatMessage = it },
                            modifier = Modifier.weight(1f)
                        )
                        PresetStrategyChip(
                            label = "رفع خطاهای ابزارهای خزش",
                            prompt = "بر اساس خزش صفحات سئو و کدهای متا وب‌سایت من، چه مواردی را جهت جلب رضایت ربات‌های الگریتم رتبه‌بندی گوگل اصلاح کنم؟",
                            onClick = { chatMessage = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetStrategyChip(
                            label = "ایجاد داده‌های اسکیما",
                            prompt = "من نیاز به کدهای اسکیما (Structured JSON-LD Schema Markup) استاندارد برای صفحه درباره ما و صفحه خدمات برچسب گزاری دارم. نمونه کد بنویس.",
                            onClick = { chatMessage = it },
                            modifier = Modifier.weight(1f)
                        )
                        PresetStrategyChip(
                            label = "استراتژی سرعت و لود",
                            prompt = "راه‌کارهای فنی برای رفع تاخیر کلیک و ورودی کاربر (INP) و بهبود ثبات طرح بارگذاری (CLS) در وردپرس چیست؟",
                            onClick = { chatMessage = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // --- Conversation Log Area ---
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
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "صندوق مشاور هوشمند سئو",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "یکی از گزینه‌های آماده بالا را انتخاب کنید یا پرسش سفارشی سئو خود را در کادر زیر بنویسید تا تحلیل کدهای شما آغاز شود.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(chatHistory, key = { it.id }) { chat ->
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // User Prompt (Left aligned in LTR, Right aligned in standard RTL layout)
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

                                    // AI Response bubble
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
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "پاسخ ساختاریافته استراتژیک:",
                                                    fontSize = 11.sp,
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

                            if (isChatLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.75f)
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
                                                text = "در حال فراخوانی مدل هوشمند...",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Message Input field ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatMessage,
                        onValueChange = { chatMessage = it },
                        placeholder = { Text("سوال خود از هوش مصنوعی سئو بپرسید...", fontSize = 12.sp) },
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
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("ai_assistant_send_button"),
                        enabled = chatMessage.isNotBlank() && !isChatLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Transmit Message",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetStrategyChip(
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
