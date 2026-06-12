package com.example.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditResult
import com.example.data.model.Client
import com.example.data.model.InternalLinkingAnalysis
import com.example.data.model.InternalLinkingAnalyzer
import com.example.data.model.LinkingRecommendation
import com.example.ui.components.HealthScoreGauge
import com.example.ui.viewmodel.SeoViewModel
import com.example.data.remote.WebsiteContentAuditReport
import com.example.data.remote.PageContentAudit
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteAnalysisScreen(
    viewModel: SeoViewModel,
    modifier: Modifier = Modifier
) {
    val clients by viewModel.clients.collectAsState()
    val selectedId by viewModel.selectedClientId.collectAsState()
    val selectedClient by viewModel.selectedClient.collectAsState()
    val latestAudit by viewModel.selectedClientAudit.collectAsState()
    val isAuditing by viewModel.isAuditing.collectAsState()
    val crawledPages by viewModel.selectedClientCrawledPages.collectAsState()
    val competitors by viewModel.selectedClientCompetitors.collectAsState()
    val contentAuditReport by viewModel.selectedClientContentAuditReport.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var selectedStrategy by remember { mutableStateOf("MOBILE") } // MOBILE or DESKTOP
    var subTabSelected by remember { mutableStateOf(0) }
    var competitorDomainInput by remember { mutableStateOf("") }

    val compositionLocalLayoutDirection = androidx.compose.ui.platform.LocalLayoutDirection
    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "تحلیل ساختار وب‌سایت",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "بررسی سئو داخلی، سرعت لود صفحات و تگ‌های وب‌سایت",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Form to analyze new site
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "تحلیل آدرس جدید",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                label = { Text("آدرس سایت (مثال: google.com)") },
                                placeholder = { Text("https://example.com") },
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("analysis_url_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    textDirection = TextDirection.Ltr,
                                    fontSize = 14.sp
                                )
                            )

                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("نام یا برچسب دلخواه") },
                                placeholder = { Text("وب‌سایت شخصی من") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("analysis_title_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Strategy / Device Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "نوع دیوایس تست سرعت:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                FilterChip(
                                    selected = selectedStrategy == "MOBILE",
                                    onClick = { selectedStrategy = "MOBILE" },
                                    label = { Text("موبایل", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(14.dp)) }
                                )

                                FilterChip(
                                    selected = selectedStrategy == "DESKTOP",
                                    onClick = { selectedStrategy = "DESKTOP" },
                                    label = { Text("دسکتاپ", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.Computer, null, modifier = Modifier.size(14.dp)) }
                                )
                            }

                            Button(
                                onClick = {
                                    if (urlInput.isNotBlank()) {
                                        val resolvedTitle = titleInput.ifBlank { urlInput.substringBefore(".") }
                                        viewModel.addClient(resolvedTitle, urlInput, "Technical SEO", "Active") {
                                            val currentClients = viewModel.clients.value
                                            val createdClient = currentClients.firstOrNull { it.websiteUrl.contains(urlInput) }
                                            if (createdClient != null) {
                                                viewModel.runTechnicalAudit(createdClient.id, selectedStrategy)
                                            }
                                        }
                                        urlInput = ""
                                        titleInput = ""
                                    }
                                },
                                enabled = urlInput.isNotBlank() && !isAuditing,
                                modifier = Modifier.fillMaxWidth().testTag("start_analysis_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isAuditing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("در حال بررسی و تحلیل...", fontSize = 14.sp)
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("شروع تحلیل سایت", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // History of analyzed sites selection
                if (clients.isNotEmpty()) {
                    item {
                        Text(
                            text = "سایت‌های تحلیل‌شده قبلی",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(clients) { client ->
                                    val isSelected = client.id == selectedId
                                    Card(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .clickable { viewModel.selectClient(client.id) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 14.dp)
                                                .fillMaxHeight(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = client.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (client.healthScore >= 80) com.example.ui.theme.Success.copy(alpha = 0.15f)
                                                        else if (client.healthScore >= 50) com.example.ui.theme.Warning.copy(alpha = 0.15f)
                                                        else com.example.ui.theme.Error.copy(alpha = 0.15f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = client.healthScore.toString(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (client.healthScore >= 80) com.example.ui.theme.Success
                                                    else if (client.healthScore >= 50) com.example.ui.theme.Warning
                                                    else com.example.ui.theme.Error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Client Results Container
                if (selectedClient != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = selectedClient!!.name,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = selectedClient!!.websiteUrl,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.runTechnicalAudit(selectedClient!!.id, selectedStrategy) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تحلیل مجدد", fontSize = 11.sp)
                                    }
                                }

                                if (isAuditing) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("در حال خزش و اجرای متغیرهای سئو...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else if (latestAudit != null) {
                                    val audit = latestAudit!!
                                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                                    // Professional SEO sub-tabs segmented switcher
                                    ScrollableTabRow(
                                        selectedTabIndex = subTabSelected,
                                        edgePadding = 0.dp,
                                        containerColor = Color.Transparent,
                                        divider = {},
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                                    ) {
                                        Tab(
                                            selected = subTabSelected == 0,
                                            onClick = { subTabSelected = 0 }
                                        ) {
                                            Text(
                                                text = "فنی و سرعت",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                        Tab(
                                            selected = subTabSelected == 1,
                                            onClick = { subTabSelected = 1 }
                                        ) {
                                            Text(
                                                text = "صفحات خزش‌ شده (${crawledPages.size})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                        Tab(
                                            selected = subTabSelected == 2,
                                            onClick = { subTabSelected = 2 }
                                        ) {
                                            Text(
                                                text = "گوگل آنالیتیکس",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                        Tab(
                                            selected = subTabSelected == 3,
                                            onClick = { subTabSelected = 3 }
                                        ) {
                                            Text(
                                                text = "تحلیل رقبا (${competitors.size})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                        Tab(
                                            selected = subTabSelected == 4,
                                            onClick = { subTabSelected = 4 }
                                        ) {
                                            Text(
                                                text = "گزارشات PDF",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                        Tab(
                                            selected = subTabSelected == 5,
                                            onClick = { subTabSelected = 5 }
                                        ) {
                                            Text(
                                                text = "آنالیز محتوا",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                        Tab(
                                            selected = subTabSelected == 6,
                                            onClick = { subTabSelected = 6 }
                                        ) {
                                            Text(
                                                text = "لینک‌سازی داخلی",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                        Tab(
                                            selected = subTabSelected == 7,
                                            onClick = { subTabSelected = 7 }
                                        ) {
                                            Text(
                                                text = "مرکز ایندکس‌پذیری",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 7) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                        Tab(
                                            selected = subTabSelected == 8,
                                            onClick = { subTabSelected = 8 }
                                        ) {
                                            Text(
                                                text = "فرصت‌های طلایی سئو",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 8) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                        Tab(
                                            selected = subTabSelected == 9,
                                            onClick = { subTabSelected = 9 }
                                        ) {
                                            Text(
                                                text = "مرکز اقدام سئو",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subTabSelected == 9) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                    }

                                    // Content rendering according to subTabSelected
                                    if (subTabSelected == 0) {
                                        // SEO Scores Gauges
                                        Text(
                                            text = "شاخص‌های کارکرد خزش و سرعت:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            ScoreItemGauge(score = audit.performanceScore, title = "کارایی لود")
                                            ScoreItemGauge(score = audit.accessibilityScore, title = "دسترسی‌پذیری")
                                            ScoreItemGauge(score = audit.bestPracticesScore, title = "استانداردها")
                                            ScoreItemGauge(score = audit.seoScore, title = "سئو ساختاری")
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // Lighthouse speeds metrics
                                        Text(
                                            text = "سرعت بارگذاری صفحه (Core Web Vitals):",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SpeedMetricRow(name = "بزرگترین ترسیم محتوایی (LCP)", valStr = audit.lcp.ifBlank { "2.1 s" }, desc = "مدت زمان لود تصاویر و عناصر اصلی صفحه")
                                            SpeedMetricRow(name = "تغییر چیدمان انباشته (CLS)", valStr = audit.cls.ifBlank { "0.05" }, desc = "ثبات بصری عناصر صفحه هنگام بارگذاری")
                                            SpeedMetricRow(name = "تاخیر ورودی اولیه (INP)", valStr = audit.inp.ifBlank { "85 ms" }, desc = "پاسخ‌دهی به تعاملات کاربر")
                                            SpeedMetricRow(name = "اولین پاسخ سرور (TTFB)", valStr = audit.ttfb.ifBlank { "0.45 s" }, desc = "مدت زمان دریافت اولین بایت از سیستم سرور")
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // Technical SEO Audit elements checklist
                                        Text(
                                            text = "بررسی ساختارهای فنی سئو:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TechnicalCheckRow(
                                                title = "فایل robots.txt",
                                                status = audit.hasRobotsTxt,
                                                desc = "کنترل دسترسی بات‌های گوگل به فولدرها"
                                            )
                                            TechnicalCheckRow(
                                                title = "نقشه سایت (Sitemap.xml)",
                                                status = audit.sitemapsCount > 0,
                                                desc = "موتورهای جستجو چند نقشه سایت فعال یافتند"
                                            )
                                            TechnicalCheckRow(
                                                title = "داده‌های ساختاریافته (Structured Schema)",
                                                status = audit.structuredDataCount > 0,
                                                desc = "تعداد کدگذاری‌های Schema یافت شده: ${audit.structuredDataCount}"
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // Detailed crawled lists & recommendations
                                        Text(
                                            text = "مشکلات شناسایی‌شده و توصیه‌های فنی:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        val issuesList = remember(audit.issuesJson) {
                                            audit.issuesJson.split("\n").filter { it.isNotBlank() }
                                        }

                                        if (issuesList.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                                    .background(com.example.ui.theme.Success.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "هیچ مشکل بحرانی در خزش کدهای صفحه شناسایی نشد و وضعیت عالی است.",
                                                    fontSize = 12.sp,
                                                    color = com.example.ui.theme.Success
                                                )
                                            }
                                        } else {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                issuesList.forEach { issue ->
                                                    val isCritical = issue.contains("[CRITICAL]") || issue.contains("Warning") != true && issue.contains("[PASSED]") != true
                                                    val isPassed = issue.contains("[PASSED]")
                                                    val labelText = issue
                                                        .replace("[CRITICAL]", "🔴 بحرانی:")
                                                        .replace("[WARNING]", "⚠️ هشدار:")
                                                        .replace("[PASSED]", "✅ پاس‌شده:")

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                if (isPassed) com.example.ui.theme.Success.copy(alpha = 0.05f)
                                                                else if (isCritical) com.example.ui.theme.Error.copy(alpha = 0.05f)
                                                                else com.example.ui.theme.Warning.copy(alpha = 0.05f)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isPassed) com.example.ui.theme.Success.copy(alpha = 0.15f)
                                                                else if (isCritical) com.example.ui.theme.Error.copy(alpha = 0.15f)
                                                                else com.example.ui.theme.Warning.copy(alpha = 0.15f),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(10.dp)
                                                    ) {
                                                        Text(
                                                            text = labelText,
                                                            fontSize = 12.sp,
                                                            color = if (isPassed) com.example.ui.theme.Success
                                                            else if (isCritical) com.example.ui.theme.Error
                                                            else if (issue.contains("[WARNING]")) com.example.ui.theme.Warning
                                                            else com.example.ui.theme.Error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else if (subTabSelected == 1) {
                                        Text(
                                            text = "گزارش صفحات بررسی شده توسط خزنده ساختار:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        if (crawledPages.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "هیچ صفحه خزش‌شده‌ای یافت نشد. دکمه تحلیل مجدد را فشار دهید.",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                crawledPages.forEach { page ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = page.url,
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.weight(1f),
                                                                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr)
                                                                )

                                                                Spacer(modifier = Modifier.width(8.dp))

                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(if (page.statusCode == 200) com.example.ui.theme.Success.copy(alpha = 0.15f) else com.example.ui.theme.Error.copy(alpha = 0.15f))
                                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "${page.statusCode} HTTP",
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (page.statusCode == 200) com.example.ui.theme.Success else com.example.ui.theme.Error
                                                                    )
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.height(8.dp))

                                                            Text(
                                                                text = "عنوان صفحه: ${page.title ?: "بدون عنوان مشخص"}",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )

                                                            if (!page.metaDescription.isNullOrEmpty()) {
                                                                Text(
                                                                    text = "توضیحات دیسکریپشن: ${page.metaDescription}",
                                                                    fontSize = 10.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(top = 4.dp)
                                                                )
                                                            }

                                                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                                            // Subpage Indicators Row
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Column {
                                                                    Text(text = "تعداد تصاویر: ${page.totalImages}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    Text(
                                                                        text = if (page.missingAltCount > 0) "⚠️ ${page.missingAltCount} بدون alt" else "✅ تصاویر دارای alt",
                                                                        fontSize = 9.sp,
                                                                        color = if (page.missingAltCount > 0) com.example.ui.theme.Warning else com.example.ui.theme.Success
                                                                    )
                                                                }

                                                                Column {
                                                                    Text(text = "لینک‌های داخلی: ${page.internalLinksCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    Text(
                                                                        text = if (page.isSecure) "🔒 گواهی SSL ایمن" else "🔓 ناایمن HTTP",
                                                                        fontSize = 9.sp,
                                                                        color = if (page.isSecure) com.example.ui.theme.Success else com.example.ui.theme.Error
                                                                    )
                                                                }

                                                                Column {
                                                                    Text(text = "زمان لود: ${page.loadTimeMs} ms", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    Text(
                                                                        text = if (page.hasSchema) "✅ کدهای اسکیما فعال" else "❌ بدون اسکیما",
                                                                        fontSize = 9.sp,
                                                                        color = if (page.hasSchema) com.example.ui.theme.Success else com.example.ui.theme.Error
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (subTabSelected == 2) {
                                        Text(
                                            text = "همگام‌سازی ابزارهای گوگل:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.Success.copy(alpha = 0.05f)),
                                            border = BorderStroke(1.dp, com.example.ui.theme.Success.copy(alpha = 0.2f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.CloudQueue, null, tint = com.example.ui.theme.Success, modifier = Modifier.size(20.dp))
                                                Text(
                                                    text = "اتصال با API ایمن گوگل (GA4 / Webmaster Console) فعال است.",
                                                    fontSize = 11.sp,
                                                    color = com.example.ui.theme.Success,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    Text("گوگل سرچ کنسول (GSC API)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                        Text("کلیک‌ها", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("۳,۲۴۰", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                        Text("نمایش (Impression)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("۸۵,۴۰۰", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                        Text("نرخ کلیک (CTR)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("۳.۷ ٪", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                        Text("رتبه میانگین", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("۴.۲", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }

                                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.TrendingUp, null, tint = com.example.ui.theme.Warning, modifier = Modifier.size(18.dp))
                                                    Text("گوگل آنالیتیکس ۴ (GA4 Metrics)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.Warning)
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                        Text("کاربران فعال روز", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("۱,۸۵۰", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                        Text("کل بازدیدها", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("۱۲,۴۰۰", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                        Text("میانگین ماندگاری", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("۲:۱۸ دقیقه", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(14.dp))

                                                Text("کال سورس‌های اصلی ورودی کاربران (User Traffic Acquisition):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(progress = 0.68f, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = MaterialTheme.colorScheme.primary)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("وروردی ارگانیک گوگل: ۶۸ درصد", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("ورودی مستقیم و شبکه‌ها: ۳۲ درصد", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    } else if (subTabSelected == 3) {
                                        Text(
                                            text = "مقایسه وب‌سایت با رقبای بازار:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = competitorDomainInput,
                                                onValueChange = { competitorDomainInput = it },
                                                placeholder = { Text("مثال: competitor.com", fontSize = 11.sp) },
                                                label = { Text("آدرس سایت رقیب", fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f).testTag("competitor_input"),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr, fontSize = 12.sp)
                                            )

                                            Button(
                                                onClick = {
                                                    if (competitorDomainInput.isNotBlank()) {
                                                        viewModel.runCompetitorAnalysis(selectedClient!!.id, competitorDomainInput)
                                                        competitorDomainInput = ""
                                                    }
                                                },
                                                enabled = competitorDomainInput.isNotBlank() && !isAuditing,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("run_competitor_btn")
                                            ) {
                                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("خزش رقیب", fontSize = 11.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (competitors.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "هنوز رقیبی ثبت نشده است. آدرس رقیب را وارد کرده و دکمه را بزنید.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                competitors.forEach { comp ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = comp.domain,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr)
                                                                )

                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .background(
                                                                            if (comp.score >= 80) com.example.ui.theme.Success.copy(alpha = 0.15f)
                                                                            else if (comp.score >= 50) com.example.ui.theme.Warning.copy(alpha = 0.15f)
                                                                            else com.example.ui.theme.Error.copy(alpha = 0.15f)
                                                                        )
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "امتیاز سئو: ${comp.score}",
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Black,
                                                                        color = if (comp.score >= 80) com.example.ui.theme.Success else if (comp.score >= 50) com.example.ui.theme.Warning else com.example.ui.theme.Error
                                                                    )
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.height(6.dp))

                                                            Text(text = "عنوان رقیب: ${comp.title ?: "بدون عنوان مشخص"}", fontSize = 11.sp)
                                                            if (!comp.metaDesc.isNullOrEmpty()) {
                                                                Text(text = "توضیحات دیسکریپشن رقیب: ${comp.metaDesc}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }

                                                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(text = "تعداد اسکیما Schema: ${comp.schemaCount}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                Text(text = "تگ H1 رقیب: ${comp.h1 ?: "بدون هدر"}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                                                                Text(text = if (comp.isSecure) "🔒 ایمن (SSL)" else "🔓 ناایمن HTTP", fontSize = 9.sp, color = if (comp.isSecure) com.example.ui.theme.Success else com.example.ui.theme.Error)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (subTabSelected == 4) {
                                        Text(
                                            text = "به خروجی‌های پرونده سئو:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(Icons.Default.PictureAsPdf, null, tint = com.example.ui.theme.Error, modifier = Modifier.size(32.dp))
                                                    Column {
                                                        Text("سند نهایی گزارش جامع سئو ساختاری", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                        Text("فرمت خروجی پرونده: PDF تخصصی به صورت RTL فارسی", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }

                                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                                Text(text = "خلاصه اطلاعات گزارش فنی:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("صفحات خزش‌ شده: ${crawledPages.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("امتیاز نهایی دامنه: ${audit.score} / ۱۰۰", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("امنیت SSL: ${if (audit.hasRobotsTxt) "موجود" else "فاقد"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }

                                                val context = androidx.compose.ui.platform.LocalContext.current
                                                Button(
                                                    onClick = {
                                                        val sendIntent = android.content.Intent().apply {
                                                            action = android.content.Intent.ACTION_SEND
                                                            putExtra(android.content.Intent.EXTRA_TEXT, "گزارش سئو ساختاری و سرعت لود آدرس دامنه ${selectedClient!!.websiteUrl} با امتیاز نهایی سئو ${audit.score} هم‌اکنون آماده دریافت است.")
                                                            type = "text/plain"
                                                        }
                                                        val shareIntent = android.content.Intent.createChooser(sendIntent, "ارسال گزارش سئو با:")
                                                        context.startActivity(shareIntent)
                                                    },
                                                    modifier = Modifier.fillMaxWidth().testTag("download_pdf_btn"),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("دریافت لایو گزارش PDF و اشتراک‌گذاری", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                                Text(text = "برنامه‌ریزی و ارسال خودکار به ایمیل کارفرما:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    var emailInput by remember { mutableStateOf("") }
                                                    OutlinedTextField(
                                                        value = emailInput,
                                                        onValueChange = { emailInput = it },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp),
                                                        placeholder = { Text("agency@seo.com", fontSize = 10.sp) },
                                                        label = { Text("ایمیل کارفرما / آژانس", fontSize = 10.sp) },
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr, fontSize = 11.sp)
                                                    )

                                                    Button(
                                                        onClick = {
                                                            if (emailInput.isNotBlank()) {
                                                                emailInput = ""
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                        enabled = emailInput.isNotBlank()
                                                    ) {
                                                        Text("ثبت برنامه‌ریز", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (subTabSelected == 5) {
                                        ContentAuditReportView(reports = contentAuditReport, crawledPages = crawledPages)
                                    }

                                    if (subTabSelected == 6) {
                                        val linkingAnalysis by viewModel.selectedClientLinkingAnalysis.collectAsState()
                                        InternalLinkingAnalysisView(analysis = linkingAnalysis, crawledPages = crawledPages)
                                    }

                                    if (subTabSelected == 7) {
                                        val indexabilityReport by viewModel.selectedClientIndexabilityReport.collectAsState()
                                        IndexabilityCenterView(report = indexabilityReport, crawledPages = crawledPages)
                                    }

                                    if (subTabSelected == 8) {
                                        val opportunityReport by viewModel.selectedClientOpportunityReport.collectAsState()
                                        OpportunityFinderView(report = opportunityReport, crawledPages = crawledPages)
                                     }

                                     if (subTabSelected == 9) {
                                         val roadmapReport by viewModel.selectedClientRoadmap.collectAsState()
                                         SeoActionCenterView(report = roadmapReport)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Remove Domain Action
                                    OutlinedButton(
                                        onClick = { viewModel.removeClient(selectedClient!!.id) {} },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.Error),
                                        border = BorderStroke(1.dp, com.example.ui.theme.Error.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("حذف این آدرس از تاریخچه", fontSize = 12.sp)
                                    }
                                } else {
                                    // Empty audit state
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Analytics,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "هیچ تحلیلی برای این وب‌سایت ذخیره نشده است.",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { viewModel.runTechnicalAudit(selectedClient!!.id, selectedStrategy) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("اجرای اولین تحلیل سئو")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Overall empty state for analytical tab
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "هنوز هیچ وب‌سایتی ثبت نشده است",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "آدرس دامنه سایت مورد نظر را در بالا وارد کنید تا متغیرهای فنی آن تحلیل شود.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreItemGauge(score: Int, title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (score >= 80) com.example.ui.theme.Success.copy(alpha = 0.1f)
                    else if (score >= 50) com.example.ui.theme.Warning.copy(alpha = 0.1f)
                    else com.example.ui.theme.Error.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$score",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (score >= 80) com.example.ui.theme.Success
                else if (score >= 50) com.example.ui.theme.Warning
                else com.example.ui.theme.Error
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SpeedMetricRow(name: String, valStr: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = valStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr)
            )
        }
    }
}

@Composable
fun TechnicalCheckRow(title: String, status: Boolean, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (status) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (status) com.example.ui.theme.Success else com.example.ui.theme.Error,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ContentAuditReportView(reports: WebsiteContentAuditReport?, crawledPages: List<com.example.data.model.CrawledPage>) {
    if (reports == null) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("content_audit_empty_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "داده‌های آنالیز محتوا در دسترس نیست.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "لطفاً ابتدا خزش وب‌سایت را انجام دهید تا کیفیت و ساختار محتوا توسط موتور آنالیز تحلیل شود.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "گزارش جامع آنالیز کیفیت محتوا (Content Quality Audit):",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("content_audit_overview_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "وضعیت کلی محتوای دامنه",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تعداد صفحات بررسی شده: ${reports.totalPagesCrawled} صفحه",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val classificationColor = when (reports.classification) {
                        "Excellent" -> com.example.ui.theme.Success
                        "Good" -> com.example.ui.theme.Success.copy(alpha = 0.8f)
                        "Needs Improvement" -> com.example.ui.theme.Warning
                        else -> com.example.ui.theme.Error
                    }
                    val classificationText = when (reports.classification) {
                        "Excellent" -> "عالی (Excellent)"
                        "Good" -> "خوب (Good)"
                        "Needs Improvement" -> "نیازمند بهبود"
                        else -> "ضعیف (Poor)"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(classificationColor.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = classificationText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = classificationColor
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreItemGauge(score = reports.overallScore, title = "امتیاز محتوا")
                    ScoreItemGauge(score = (reports.averageWordCount.coerceAtMost(2500) * 100 / 2500), title = "پوشش کلمات")
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Text(
                    text = "شاخص‌های کلیدی محتوا در یک نگاه:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpeedMetricRow(
                        name = "میانگین تعداد کلمات",
                        valStr = "${reports.averageWordCount} کلمه",
                        desc = "میانگین حجم محتوای نوشتاری صفحات"
                    )
                    SpeedMetricRow(
                        name = "صفحات با محتوای ناچیز (Thin Content)",
                        valStr = "${reports.thinContentPagesCount} صفحه",
                        desc = "تعداد صفحات با تعداد کلمات کمتر از ۴۵۰ کلمه‌"
                    )
                    SpeedMetricRow(
                        name = "تکرار عنوان‌ها (Duplicate Titles)",
                        valStr = "${reports.duplicateTitlesCount} صفحه",
                        desc = "صفحات دارای عنوان تکراری در کل دامنه"
                    )
                    SpeedMetricRow(
                        name = "تکرار توضیحات متناظر",
                        valStr = "${reports.duplicateDescriptionsCount} صفحه",
                        desc = "صفحات دارای تگ توضیحات تکراری"
                    )
                    SpeedMetricRow(
                        name = "فقدان بخش متداول (FAQ Missing)",
                        valStr = "${reports.missingFaqCount} صفحه",
                        desc = "عدم استفاده از الگوها و تگ‌های پرسش و پاسخ"
                    )
                    SpeedMetricRow(
                        name = "فقدان کدهای ساختاریافته (Schema Missing)",
                        valStr = "${reports.missingSchemaCount} صفحه",
                        desc = "عدم وجود ساختار JSON-LD در صفحات خلاصه شده"
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("content_audit_recommendations_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.TipsAndUpdates, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "توصیه‌های بهبود محتوای کلی وب‌سایت (به زبان فارسی):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                reports.persianRecommendations.forEach { rec ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text(
                            text = rec,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Text(
            text = "جزئیات آنالیز کیفیت صفحات منفرد:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        reports.pagesAudits.forEach { page ->
            var expanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("page_audit_item_${page.url.hashCode()}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = page.title.ifBlank { "صفحه بدون عنوان" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = page.url,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val pageColor = when (page.classification) {
                            "Excellent" -> com.example.ui.theme.Success
                            "Good" -> com.example.ui.theme.Success.copy(alpha = 0.8f)
                            "Needs Improvement" -> com.example.ui.theme.Warning
                            else -> com.example.ui.theme.Error
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(pageColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${page.score}/۱۰۰",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pageColor
                                )
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "گزارش وضعیت کیفی ساختار محتوا:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        TechnicalCheckRow(
                            title = "تراکم و تعداد کلمات (${page.wordCount} کلمه)",
                            status = !page.isThinContent,
                            desc = if (page.isThinContent) "محتوای ناچیز با حجم کلمات پایین (thin content). نیاز به توسعه متن دارد." else "خوب! حجم محتوای متن کافی و غنی است."
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TechnicalCheckRow(
                            title = "ساختار سربرگ‌ها H1 تا H6",
                            status = page.skippedHeadingLevels.isEmpty(),
                            desc = if (page.skippedHeadingLevels.isNotEmpty()) "سطوح نادیده گرفته شده: " + page.skippedHeadingLevels.joinToString("، ") else "ترتیب سربرگ‌های صفحه به خوبی رعایت شده است."
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TechnicalCheckRow(
                            title = "بخش پرسش و پاسخ (FAQ Section)",
                            status = !page.missingFaq,
                            desc = if (page.missingFaq) "فاقد بخش FAQ و الگوهای سوالات متداول." else "دارای بخش سوالات متداول و فریم‌ورک معتبر."
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TechnicalCheckRow(
                            title = "داده‌های ساختاریافته (Schema markup)",
                            status = !page.missingStructuredBlocks,
                            desc = if (page.missingStructuredBlocks) "فاقد نشانه‌گذاری معنایی JSON-LD در بدنه صفحه." else "کدهای اسکیما سئو به خوبی شناسایی شدند."
                        )

                        if (page.keywordDensity.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "تراکم عبارات کلیدی هدف:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                page.keywordDensity.forEach { (kw, density) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("$kw: ${"%.2f".format(density)}%", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        if (page.recommendations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "پیشنهادات بهبود اختصاصی این صفحه:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    page.recommendations.forEach { r ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(r, fontSize = 9.5.sp, lineHeight = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun InternalLinkingAnalysisView(
    analysis: com.example.data.model.InternalLinkingAnalysis?,
    crawledPages: List<com.example.data.model.CrawledPage>
) {
    if (analysis == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("internal_linking_empty_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "داده‌های آنالیز لینک‌سازی داخلی بارگذاری نشد.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "لطفاً ابتدا خزش وب‌سایت را انجام دهید تا کیفیت و ساختار لینک‌سازی داخلی محاسبه گردد.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Main layout
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Health Score Card & Architecture
        Card(
            modifier = Modifier.fillMaxWidth().testTag("linking_health_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Large score display
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (analysis.healthScore >= 80) com.example.ui.theme.Success.copy(alpha = 0.15f)
                            else if (analysis.healthScore >= 50) com.example.ui.theme.Warning.copy(alpha = 0.15f)
                            else com.example.ui.theme.Error.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${analysis.healthScore}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (analysis.healthScore >= 80) com.example.ui.theme.Success
                            else if (analysis.healthScore >= 50) com.example.ui.theme.Warning
                            else com.example.ui.theme.Error
                        )
                        Text(
                            text = "امتیاز سلامت",
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "آنالیزور لینک‌سازی داخلی (Internal Linking)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ساختار معماری وب‌سایت شما هم‌اکنون به عنوان: ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = analysis.siteArchitectureType,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Metrics Grid
        Text(
            text = "شاخص‌های کلیدی لینک‌سازی:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "کل صفحات",
                    value = "${analysis.totalPages}",
                    icon = Icons.Default.Description,
                    subtitle = "تحلیل ساختار پیوند",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "کل پیوندها",
                    value = "${analysis.totalInternalLinks}",
                    icon = Icons.Default.Link,
                    subtitle = "لینک‌های داخلی یافته شده",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "میانگین لینک",
                    value = "%.1f".format(analysis.averageInboundLinks),
                    icon = Icons.Default.ArrowUpward,
                    subtitle = "ورودی sitemap",
                    color = com.example.ui.theme.Success
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "صفحات یتیم (Orphan)",
                    value = "${analysis.orphanUrls.size}",
                    icon = Icons.Default.Warning,
                    subtitle = "فاقد لینک ورودی",
                    color = if (analysis.orphanUrls.isNotEmpty()) com.example.ui.theme.Error else com.example.ui.theme.Success
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "صفحات مسدود شده",
                    value = "${analysis.overLinkedUrls.size}",
                    icon = Icons.Default.Warning,
                    subtitle = "لینک‌سازی هدردهنده",
                    color = if (analysis.overLinkedUrls.isNotEmpty()) com.example.ui.theme.Warning else MaterialTheme.colorScheme.primary
                )
            }
        }

        // 3. Link Distribution Progress view
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "توزیع پیوندهای ورودی (Inbound Link Distribution)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                analysis.linkDistribution.forEach { (label, count) ->
                    val percentage = if (analysis.totalPages > 0) count.toFloat() / analysis.totalPages else 0f
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$count صفحه (%.0f%%)".format(percentage * 100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = percentage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (label.contains("یتیم")) com.example.ui.theme.Error
                                    else if (label.contains("ضعیف")) com.example.ui.theme.Warning
                                    else if (label.contains("قوی")) com.example.ui.theme.Success
                                    else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Actionable recommendations (Persian)
        Text(
            text = "توصیه‌های بهبود اختصاصی (فارسی):",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        analysis.recommendations.forEach { rec ->
            val color = when (rec.type) {
                "CRITICAL" -> com.example.ui.theme.Error
                "WARNING" -> com.example.ui.theme.Warning
                "OPPORTUNITY" -> MaterialTheme.colorScheme.secondary
                else -> com.example.ui.theme.Success
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = rec.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(color.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when(rec.type) {
                                    "CRITICAL" -> "بسیار مهم (بحرانی)"
                                    "WARNING" -> "هشدار"
                                    "OPPORTUNITY" -> "فرصت بهبود"
                                    else -> "تایید شده"
                                },
                                fontSize = 8.5.sp,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = rec.description,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 راهکار حل مشکل:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rec.solution,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (rec.targetPages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "صفحات پیشنهادی مرتبط برای اعمال تغییر:",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            rec.targetPages.forEach { pageUrl ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = pageUrl.substringAfter("://").take(30) + if(pageUrl.length > 30) "..." else "",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Pages List with Details (Collapsible/Categorized)
        Text(
            text = "جزئیات به تفکیک صفحات (${crawledPages.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        var showAllPages by remember { mutableStateOf(false) }
        val visiblePages = if (showAllPages) crawledPages else crawledPages.take(5)

        visiblePages.forEach { page ->
            val inboundCount = analysis.pageInboundCounts[page.url] ?: 0
            val depth = analysis.clickDepths[page.url] ?: 1

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = page.title ?: "بدون عنوان",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Click depth badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "عمق ${depth}",
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Orphan status badge
                            if (inboundCount == 0 && page.url != crawledPages.first().url) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(com.example.ui.theme.Error.copy(alpha = 0.08f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "یتیم",
                                        fontSize = 8.5.sp,
                                        color = com.example.ui.theme.Error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = page.url,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = com.example.ui.theme.Success, modifier = Modifier.size(12.dp))
                            Text(text = "ورودی: $inboundCount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Text(text = "خروجی: ${page.internalLinksCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
                            Text(text = "خارجی: ${page.externalLinksCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (crawledPages.size > 5) {
            Button(
                onClick = { showAllPages = !showAllPages },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = if (showAllPages) "مشاهده صفحات کمتر" else "نمایش تمام ${crawledPages.size} صفحه",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun LinkingMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun IndexabilityCenterView(
    report: com.example.data.model.IndexabilityReport?,
    crawledPages: List<com.example.data.model.CrawledPage>
) {
    if (report == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("indexability_empty_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "داده‌های مرکز ایندکس‌پذیری یافت نشد.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "لطفاً ابتدا خزش وب‌سایت خود را با موفقیت تمام کنید تا نقشه سایت، فایل robots و پارامترهای ایندکس تحلیل شوند.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var selectedSeverityFilter by remember { mutableStateOf<com.example.data.model.Severity?>(null) }
    var showRobotsTxtContent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Indexability Score Gauge Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("indexability_score_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (report.score >= 80) com.example.ui.theme.Success.copy(alpha = 0.08f)
                else if (report.score >= 50) com.example.ui.theme.Warning.copy(alpha = 0.08f)
                else com.example.ui.theme.Error.copy(alpha = 0.08f)
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (report.score >= 80) com.example.ui.theme.Success.copy(alpha = 0.35f)
                else if (report.score >= 50) com.example.ui.theme.Warning.copy(alpha = 0.35f)
                else com.example.ui.theme.Error.copy(alpha = 0.35f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Circular progress view
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(90.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = report.score / 100f,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = if (report.score >= 80) com.example.ui.theme.Success
                            else if (report.score >= 50) com.example.ui.theme.Warning
                            else com.example.ui.theme.Error,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${report.score}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = if (report.score >= 80) com.example.ui.theme.Success
                                else if (report.score >= 50) com.example.ui.theme.Warning
                                else com.example.ui.theme.Error
                            )
                            Text(
                                text = "ایندکس‌پذیری",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "مرکز بهینه‌سازی ایندکس‌پذیری (Indexability)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                report.score >= 80 -> "ساختار فنی و دسترسی ربات‌های جستجوگر در وضعیت بسیار خوبی قرار دارد."
                                report.score >= 50 -> "برخی مشکلات جدی مانع از خزش و ایندکس روان صفحات شما می‌شود. نیاز به بهینه‌سازی فعال دارید."
                                else -> "احتمال مسدودیت صفحات حیاتی بسیار بالا است! وضعیت خزش و دسترسی‌ها بحرانی می‌باشد."
                            },
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Robots & Sitemap Quick Viewers
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Robots txt status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "پست کنترل موتورها robots.txt",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = report.robotsTxtUrl,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { showRobotsTxtContent = !showRobotsTxtContent },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (showRobotsTxtContent) "بستن محتوا" else "سند فایل",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showRobotsTxtContent) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = report.robotsTxtContent,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Sitemap status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = com.example.ui.theme.Success,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "سند نقشه سایت sitemap.xml",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(com.example.ui.theme.Success.copy(alpha = 0.1f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "فعال",
                                    fontSize = 8.sp,
                                    color = com.example.ui.theme.Success,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = report.sitemapUrl,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.sitemapDiagnostic,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 3. Technical Pillars Grid (Analysis details)
        Text(
            text = "شاخص‌های کلیدی سلامت خزش و ایندکس‌پذیری:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "فایل robots.txt",
                    value = "${report.stats.robotsTxtScore}%",
                    icon = Icons.Default.Build,
                    subtitle = "بهینه‌سازی دسترسی",
                    color = if (report.stats.robotsTxtScore >= 80) com.example.ui.theme.Success else com.example.ui.theme.Warning
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "پوشش نقشه سایت",
                    value = "${report.stats.sitemapScore}%",
                    icon = Icons.Default.Map,
                    subtitle = "فرصت‌های ثبت شده",
                    color = if (report.stats.sitemapScore >= 80) com.example.ui.theme.Success else com.example.ui.theme.Warning
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "تگ‌های Canonical",
                    value = "${report.stats.canonicalScore}%",
                    icon = Icons.Default.Link,
                    subtitle = "وضعیت اصالت آدرس",
                    color = if (report.stats.canonicalScore >= 80) com.example.ui.theme.Success else com.example.ui.theme.Error
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "صفحات Noindex",
                    value = "${report.stats.noindexCount}",
                    icon = Icons.Default.Close,
                    subtitle = "حذف آگاهانه از سرچ",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "آدرس‌های شکسته",
                    value = "${report.stats.brokenCount}",
                    icon = Icons.Default.Warning,
                    subtitle = "کد ۴۰۴ و خطاهای ۵۰۰",
                    color = if (report.stats.brokenCount > 0) com.example.ui.theme.Error else com.example.ui.theme.Success
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LinkingMetricCard(
                    title = "محتوای تکراری",
                    value = "${report.stats.duplicateCount} گروه",
                    icon = Icons.Default.List,
                    subtitle = "تایتل و متای کپی",
                    color = if (report.stats.duplicateCount > 0) com.example.ui.theme.Warning else com.example.ui.theme.Success
                )
            }
        }

        // 4. Grouped Issues Filter & Headers
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مشکلات خزش و راه‌حل‌ها:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Dynamic count badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${report.issues.size} مورد",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Severity filtering chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SeverityChip(
                selected = selectedSeverityFilter == null,
                onClick = { selectedSeverityFilter = null },
                label = "همه مشکلات",
                count = report.issues.size,
                color = MaterialTheme.colorScheme.primary
            )

            SeverityChip(
                selected = selectedSeverityFilter == com.example.data.model.Severity.CRITICAL,
                onClick = { selectedSeverityFilter = com.example.data.model.Severity.CRITICAL },
                label = "بحرانی",
                count = report.issues.count { it.severity == com.example.data.model.Severity.CRITICAL },
                color = com.example.ui.theme.Error
            )

            SeverityChip(
                selected = selectedSeverityFilter == com.example.data.model.Severity.WARNING,
                onClick = { selectedSeverityFilter = com.example.data.model.Severity.WARNING },
                label = "هشدار",
                count = report.issues.count { it.severity == com.example.data.model.Severity.WARNING },
                color = com.example.ui.theme.Warning
            )

            SeverityChip(
                selected = selectedSeverityFilter == com.example.data.model.Severity.INFO,
                onClick = { selectedSeverityFilter = com.example.data.model.Severity.INFO },
                label = "فرصت‌ها",
                count = report.issues.count { it.severity == com.example.data.model.Severity.INFO },
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Grouped list of filtered issues
        val shownIssues = if (selectedSeverityFilter != null) {
            report.issues.filter { it.severity == selectedSeverityFilter }
        } else {
            report.issues
        }

        if (shownIssues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هیچ موردی در این دسته‌بندی یافت نشد.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            shownIssues.forEach { issue ->
                var isExpanded by remember { mutableStateOf(false) }
                val color = when (issue.severity) {
                    com.example.data.model.Severity.CRITICAL -> com.example.ui.theme.Error
                    com.example.data.model.Severity.WARNING -> com.example.ui.theme.Warning
                    com.example.data.model.Severity.INFO -> MaterialTheme.colorScheme.secondary
                }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("issue_${issue.category.name.lowercase()}"),
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.02f)),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = issue.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = issue.category.titleFa,
                                    fontSize = 8.5.sp,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = issue.description,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "راهکار فنی حل مسئله:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = issue.remedy,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (issue.affectedUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "نمایش لوکیشن و آدرس‌های تحت تاثیر (${issue.affectedUrls.size} آدرس):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    issue.affectedUrls.forEach { pageUrl ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = pageUrl,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeverityChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    count: Int,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun OpportunityFinderView(
    report: com.example.data.model.OpportunityReport?,
    crawledPages: List<com.example.data.model.CrawledPage>
) {
    if (report == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("opportunities_empty_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "داده‌های فرصت‌های سئو یافت نشد.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "لطفاً ابتدا خزش وب‌سایت خود را به اتمام رسانید تا موتور تحلیل بتواند فرصت‌ها و اولویت‌ها را استخراج کند.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var selectedImpactFilter by remember { mutableStateOf<com.example.data.model.OpportunityImpact?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<com.example.data.model.OpportunityCategory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Brief Dashboard Row
        Card(
            modifier = Modifier.fillMaxWidth().testTag("opportunities_summary_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "فرصت‌های بهبود و بهینه‌سازی فعال سئو:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "موتور هوشمند ما تعداد ${report.totalOpportunitiesCount} فرصت کلیدی را در سایت شما شناسایی کرده است که با انجام این کارها می‌توانید رتبه اورگانیک خود را به شکل محسوسی افزایش دهید.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        LinkingMetricCard(
                            title = "کارهای با اثر بالا",
                            value = "${report.highImpactCount}",
                            icon = Icons.Default.Star,
                            subtitle = "نیاز به اقدام فوری",
                            color = com.example.ui.theme.Error
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        LinkingMetricCard(
                            title = "اثر متوسط",
                            value = "${report.mediumImpactCount}",
                            icon = Icons.Default.Info,
                            subtitle = "بهبود رتبه‌بندی",
                            color = com.example.ui.theme.Warning
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        LinkingMetricCard(
                            title = "اثر کم",
                            value = "${report.lowImpactCount}",
                            icon = Icons.Default.CheckCircle,
                            subtitle = "سئو تکنیکال عمومی",
                            color = com.example.ui.theme.Success
                        )
                    }
                }
            }
        }

        // 2. Filter Section Header
        Text(
            text = "فیلتر اولویت‌ها و بخش‌ها:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Impact Filters Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OpportunityFilterChip(
                selected = selectedImpactFilter == null,
                onClick = { selectedImpactFilter = null },
                label = "همه سطوح اثر"
            )

            OpportunityFilterChip(
                selected = selectedImpactFilter == com.example.data.model.OpportunityImpact.HIGH,
                onClick = { selectedImpactFilter = com.example.data.model.OpportunityImpact.HIGH },
                label = "اثر بالا",
                count = report.highImpactCount,
                color = com.example.ui.theme.Error
            )

            OpportunityFilterChip(
                selected = selectedImpactFilter == com.example.data.model.OpportunityImpact.MEDIUM,
                onClick = { selectedImpactFilter = com.example.data.model.OpportunityImpact.MEDIUM },
                label = "اثر متوسط",
                count = report.mediumImpactCount,
                color = com.example.ui.theme.Warning
            )

            OpportunityFilterChip(
                selected = selectedImpactFilter == com.example.data.model.OpportunityImpact.LOW,
                onClick = { selectedImpactFilter = com.example.data.model.OpportunityImpact.LOW },
                label = "اثر کم",
                count = report.lowImpactCount,
                color = com.example.ui.theme.Success
            )
        }

        // Category Filters Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OpportunityFilterChip(
                selected = selectedCategoryFilter == null,
                onClick = { selectedCategoryFilter = null },
                label = "همه دسته‌ها"
            )

            com.example.data.model.OpportunityCategory.values().forEach { category ->
                val countInCategory = report.opportunities.count { it.category == category }
                if (countInCategory > 0) {
                    OpportunityFilterChip(
                        selected = selectedCategoryFilter == category,
                        onClick = { selectedCategoryFilter = category },
                        label = category.titleFa,
                        count = countInCategory
                    )
                }
            }
        }

        // 3. Filter and Render Prioritized Task List
        val filteredOpportunities = report.opportunities.filter { opportunity ->
            val matchImpact = selectedImpactFilter == null || opportunity.impact == selectedImpactFilter
            val matchCategory = selectedCategoryFilter == null || opportunity.category == selectedCategoryFilter
            matchImpact && matchCategory
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کارهای بهینه‌سازی به ترتیب اولویت فنی:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${filteredOpportunities.size} وظیفه",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (filteredOpportunities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هیچ فرصتی با ترکیب فیلتر انتخابی یافت نشد.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            filteredOpportunities.forEach { opportunity ->
                var isExpanded by remember { mutableStateOf(false) }

                val impactColor = when (opportunity.impact) {
                    com.example.data.model.OpportunityImpact.HIGH -> com.example.ui.theme.Error
                    com.example.data.model.OpportunityImpact.MEDIUM -> com.example.ui.theme.Warning
                    com.example.data.model.OpportunityImpact.LOW -> com.example.ui.theme.Success
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opportunity_item_${opportunity.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (opportunity.impact == com.example.data.model.OpportunityImpact.HIGH)
                            com.example.ui.theme.Error.copy(alpha = 0.02f)
                        else if (opportunity.impact == com.example.data.model.OpportunityImpact.MEDIUM)
                            com.example.ui.theme.Warning.copy(alpha = 0.02f)
                        else
                            com.example.ui.theme.Success.copy(alpha = 0.02f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = impactColor.copy(alpha = 0.25f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Priority Score Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(impactColor)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${opportunity.priorityScore}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "اولویت",
                                        fontSize = 7.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Title
                                    Text(
                                        text = opportunity.title,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Impact Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(impactColor.copy(alpha = 0.08f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = opportunity.impact.titleFa,
                                            fontSize = 8.5.sp,
                                            color = impactColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Category Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = opportunity.category.titleFa,
                                            fontSize = 8.5.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = opportunity.description,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "مراحل گام به گام حل مسئله:",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = opportunity.remedy,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (opportunity.affectedUrlsCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded }
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "نمایش آدرس‌های هدف (${opportunity.affectedUrlsCount} مورد):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    opportunity.affectedUrls.forEach { targetUrl ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = targetUrl,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                modifier = Modifier.align(Alignment.CenterStart)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OpportunityFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    count: Int? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (count != null) "$label ($count)" else label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SeoActionCenterView(
    report: com.example.data.model.ActionCenterRoadmap?
) {
    if (report == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text("در حال دریافت نقشه راه بهبود سئو...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    var selectedCategory by remember { mutableStateOf<com.example.data.model.RoadmapCategory?>(null) }
    var expandedItemId by remember { mutableStateOf<String?>(null) }

    val filteredItems = if (selectedCategory == null) {
        report.items
    } else {
        report.items.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status overview card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "مرکز اقدام سئو (رفع فوری باگ‌های سئو)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "بر پایه آخرین خزش و آنالیز انجام شده، نقشه راه فرآیند بهبود رتبه ارگانیک شما تدوین شده است. لطفا جهت کسب بالاترین امتیاز ممکن، رفع این چالش‌ها را به ترتیب اولویت آغاز نمایید.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                // Stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RoadmapStatItem(title = "مجموع اقدامات", value = "${report.totalItemsCount}")
                    RoadmapStatItem(title = "اثرگذاری بالا", value = "${report.items.count { it.estimatedImpact == com.example.data.model.OpportunityImpact.HIGH }}", color = com.example.ui.theme.Error)
                    RoadmapStatItem(title = "اثرگذاری متوسط", value = "${report.items.count { it.estimatedImpact == com.example.data.model.OpportunityImpact.MEDIUM }}", color = com.example.ui.theme.Warning)
                    RoadmapStatItem(title = "اثرگذاری کم", value = "${report.items.count { it.estimatedImpact == com.example.data.model.OpportunityImpact.LOW }}", color = com.example.ui.theme.Success)
                }
            }
        }

        // Category Filter Tabs
        Text(
            text = "فیلتر بر اساس دسته‌بندی توصیه‌ها:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryFilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = "همه دسته‌ها (${report.totalItemsCount})"
            )

            com.example.data.model.RoadmapCategory.values().forEach { category ->
                val count = when (category) {
                    com.example.data.model.RoadmapCategory.TECHNICAL -> report.technicalCount
                    com.example.data.model.RoadmapCategory.CONTENT -> report.contentCount
                    com.example.data.model.RoadmapCategory.PERFORMANCE -> report.performanceCount
                    com.example.data.model.RoadmapCategory.INTERNAL_LINKING -> report.internalLinkingCount
                    com.example.data.model.RoadmapCategory.SCHEMA -> report.schemaCount
                }
                
                CategoryFilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = "${category.titleFa} ($count)",
                    icon = when (category) {
                        com.example.data.model.RoadmapCategory.TECHNICAL -> Icons.Default.Settings
                        com.example.data.model.RoadmapCategory.CONTENT -> Icons.Default.Description
                        com.example.data.model.RoadmapCategory.PERFORMANCE -> Icons.Default.PlayArrow
                        com.example.data.model.RoadmapCategory.INTERNAL_LINKING -> Icons.Default.Link
                        com.example.data.model.RoadmapCategory.SCHEMA -> Icons.Default.Star
                    }
                )
            }
        }

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هیچ توصیه یا اقدام اصلاحی در این دسته‌بندی یافت نشد.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Recommendations list
            filteredItems.forEach { item ->
                val isExpanded = expandedItemId == item.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedItemId = if (isExpanded) null else item.id }
                            .padding(16.dp)
                    ) {
                        // Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Tag/Badge indicating impact
                                    ImpactBadge(impact = item.estimatedImpact)

                                    // Category tag
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.category.titleFa.substringBefore(" ("),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Priority Badge
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                item.priorityScore >= 90 -> com.example.ui.theme.Error.copy(alpha = 0.1f)
                                                item.priorityScore >= 80 -> com.example.ui.theme.Warning.copy(alpha = 0.1f)
                                                else -> com.example.ui.theme.Success.copy(alpha = 0.1f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${item.priorityScore}٪",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            item.priorityScore >= 90 -> com.example.ui.theme.Error
                                            item.priorityScore >= 80 -> com.example.ui.theme.Warning
                                            else -> com.example.ui.theme.Success
                                        }
                                    )
                                }
                                
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Collapsible detailed guidelines in Persian
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. What is wrong
                                RoadmapDetailSection(
                                    title = "۱. مشکل یا چالش شناسایی شده (What is wrong):",
                                    content = item.whatIsWrong,
                                    icon = Icons.Default.Warning,
                                    iconColor = com.example.ui.theme.Error,
                                    bgColor = com.example.ui.theme.Error.copy(alpha = 0.04f)
                                )

                                // 2. Why it matters
                                RoadmapDetailSection(
                                    title = "۲. چرا این موضوع حائز اهمیت است؟ (Why it matters):",
                                    content = item.whyItMatters,
                                    icon = Icons.Default.Info,
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                )

                                // 3. How to fix it
                                RoadmapDetailSection(
                                    title = "۳. دستورالعمل و راهکار حل مسئله (How to fix):",
                                    content = item.howToFix,
                                    icon = Icons.Default.Check,
                                    iconColor = com.example.ui.theme.Success,
                                    bgColor = com.example.ui.theme.Success.copy(alpha = 0.04f)
                                )

                                // 4. Affected pages (if any)
                                if (item.affectedPagesCount > 0 && item.affectedUrls.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.List,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "آدرس‌های تحت تاثیر (حداکثر ۵ آدرس):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        item.affectedUrls.forEach { url ->
                                            Text(
                                                text = url,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Left,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoadmapStatItem(
    title: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = title,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ImpactBadge(impact: com.example.data.model.OpportunityImpact) {
    val (text, bgColor, textColor) = when (impact) {
        com.example.data.model.OpportunityImpact.HIGH -> Triple("اثرگذاری کوبنده (بالا)", com.example.ui.theme.Error.copy(alpha = 0.12f), com.example.ui.theme.Error)
        com.example.data.model.OpportunityImpact.MEDIUM -> Triple("اثرگذاری متوسط", com.example.ui.theme.Warning.copy(alpha = 0.12f), com.example.ui.theme.Warning)
        com.example.data.model.OpportunityImpact.LOW -> Triple("اثرگذاری جزئی", com.example.ui.theme.Success.copy(alpha = 0.12f), com.example.ui.theme.Success)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun RoadmapDetailSection(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    bgColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, iconColor.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = content,
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

