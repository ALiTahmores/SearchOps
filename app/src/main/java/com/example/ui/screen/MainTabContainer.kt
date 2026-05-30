package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Tab 0: Dashboard
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Metrics", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_metrics")
                )

                // Tab 1: Clients / Campaigns
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.BusinessCenter, contentDescription = "Campaigns") },
                    label = { Text("Campaigns", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_campaigns")
                )

                // Tab 2: AI Strategy Chat
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Assistant") },
                    label = { Text("AI Strategist", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_ai")
                )

                // Tab 3: Workspace Settings
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp) },
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
                0 -> PortfolioDashboardScreen(
                    viewModel = viewModel,
                    onClientClick = onClientClick,
                    onAddClientClick = onAddClientClick
                )
                1 -> ClientsManagementScreen(
                    viewModel = viewModel,
                    onClientClick = onClientClick,
                    onAddClientClick = onAddClientClick
                )
                2 -> EnterpriseAiAssistantTab(
                    viewModel = viewModel
                )
                3 -> SettingsScreen(
                    viewModel = viewModel,
                    onLogoutClick = onLogoutClick
                )
            }
        }
    }
}
