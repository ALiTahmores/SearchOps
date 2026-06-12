package com.example.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.SeoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabContainer(
    viewModel: SeoViewModel,
    onClientClick: (Int) -> Unit,
    onAddClientClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    // Tab 0: Dashboard (داشبورد)
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "داشبورد") },
                        label = { Text("داشبورد", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_tab_metrics")
                    )

                    // Tab 1: Website Analysis Screen (تحلیل سایت)
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(imageVector = Icons.Default.Analytics, contentDescription = "تحلیل سایت") },
                        label = { Text("تحلیل سایت", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_tab_campaigns")
                    )

                    // Tab 2: AI Assistant Screen (دستیار هوش مصنوعی)
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "دستیار هوش مصنوعی") },
                        label = { Text("دستیار هوش مصنوعی", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_tab_ai")
                    )

                    // Tab 3: Settings Screen (تنظیمات)
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "تنظیمات") },
                        label = { Text("تنظیمات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onStartAnalysisClick = { selectedTab = 1 }
                    )
                    1 -> WebsiteAnalysisScreen(
                        viewModel = viewModel
                    )
                    2 -> EnterpriseAiAssistantTab(
                        viewModel = viewModel
                    )
                    3 -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
