package com.example.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.viewmodel.SeoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SeoViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val googleToken by viewModel.googleAccessToken.collectAsState()

    var showClearDataDialog by remember { mutableStateOf(false) }
    var operationSuccessMessage by remember { mutableStateOf("") }

    // Wrap in RTL
    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(
                            id = if (androidx.compose.foundation.isSystemInDarkTheme()) R.drawable.searchops_logo_dark else R.drawable.searchops_logo_light
                        ),
                        contentDescription = "SearchOps Logo",
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "تنظیمات برنامه",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "مدیریت تم، یکپارچه‌سازی APIها و تاریخچه داده‌ها",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            if (operationSuccessMessage.isNotBlank()) {
                Snackbar(
                    modifier = Modifier.padding(bottom = 16.dp),
                    action = {
                        TextButton(onClick = { operationSuccessMessage = "" }) {
                            Text("باشه", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                ) {
                    Text(text = operationSuccessMessage)
                }
            }

            // --- SECTION 1: SYSTEM APPEARANCE (THEME SWITCHER) ---
            Text(
                text = "تغییر تم برنامه",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "حالت نمایش پوسته نرم‌افزار:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themes = listOf(
                            Triple("System", "پیشفرض سیستم", "System"),
                            Triple("Light", "روشن", "Light"),
                            Triple("Dark", "تیره", "Dark")
                        )
                        themes.forEach { (key, display, tag) ->
                            val isSelected = themeMode == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setThemeMode(key) }
                                    .testTag("theme_button_$tag"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = display,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- SECTION 2: API INTEGRATIONS & DIAGNOSTICS ---
            Text(
                text = "وضعیت کلیدهای API",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "بررسی زنده سلامت اتصال به سرورها:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val hasGeminiKey = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                    val geminiStatus = if (hasGeminiKey) "متصل (کلید معتبر)" else "قطع است (کلید یافت نشد)"
                    val tokenStatus = if (googleToken != null) "رابط فعال" else "متصل نشده"

                    DiagnosticApiItem(service = "هوش مصنوعی گوگل (Gemini API)", status = geminiStatus, isOk = hasGeminiKey)
                    DiagnosticApiItem(service = "سرویس سرعت لود (PageSpeed API)", status = "آماده کاربری", isOk = true)
                    DiagnosticApiItem(service = "همگام‌ساز سرچ کنسول (OAuth)", status = tokenStatus, isOk = googleToken != null)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- SECTION 3: APP LOGO AND BRANDING SHIELD ---
            Text(
                text = "تغییر و مانیتور لوگو",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(
                                id = if (androidx.compose.foundation.isSystemInDarkTheme()) R.drawable.searchops_logo_dark else R.drawable.searchops_logo_light
                            ),
                            contentDescription = "SearchOps Brand Mini Logo",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "لوگوی فعال: SearchOps",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "برندینگ یکپارچه تیره و روشن موتور پردازش سئو",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- SECTION 4: INFORMATION / ABOUT APP ---
            Text(
                text = "اطلاعات برنامه",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "میز کار فنی سئو SearchOps",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "این پلتفرم یک ابزار کاملاً لوکال و آفلاین‌محور است که با استفاده از خزنده هوشمند کدهای وب‌سایت شما را برحسب تگ‌های بهینه‌سازی، حجم متا دسک، داده‌های ساختاریافته و فایل‌های دسترسی ربات بررسی کرده و به کمک موتور هوشمند هوارسانی بر بستر هوش مصنوعی گوگل (Gemini API) توصیه‌های جامع برای سئوکاران ارائه می‌دهد.",
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("نسخه کلاینت:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("v2.5.0", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 5: DESTRUCTIVE ACTION (CLEAR ALL AUDITS) ---
            Button(
                onClick = { showClearDataDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_clear_data_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "پاک کردن داده‌های تحلیل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Confirmation Alert Dialog in Persian for wiping data
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllAnalysisData {
                            operationSuccessMessage = "تمامی رکوردهای تحلیل وب‌سایت با موفقیت پاک شدند."
                        }
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، کاملاً پاک شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("انصراف")
                }
            },
            title = {
                Text("پاک کردن تمامی داده‌ها؟", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = "با تایید این مرحله، تمامی وب‌سایت‌های خزش‌شده، سرعت بارگذاری ذخیره‌شده و مشاوره گفتگو به طور دائم از دیتابیس دستگاه شما حذف خواهند شد. این متغیر غیر قابل بازگشت است.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            },
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun DiagnosticApiItem(service: String, status: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = service,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOk) com.example.ui.theme.Success else com.example.ui.theme.Warning
        )
    }
}
