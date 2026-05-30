package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screen.*
import com.example.ui.viewmodel.SeoViewModel

object NavRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING_WELCOME = "welcome"
    const val ONBOARDING_SIGNIN = "signin"
    const val ONBOARDING_SIGNUP = "signup"
    const val ONBOARDING_FORGOT = "forgot"
    
    const val MAIN_CONTAINER = "main_container"
    const val ADD_CLIENT = "add_client"
    const val CLIENT_DETAIL = "client_detail"
    const val AI_CHAT = "ai_chat"
}

@Composable
fun AppNavGraph(viewModel: SeoViewModel) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH
    ) {
        // --- SPLASH SCREEN ---
        composable(route = NavRoutes.SPLASH) {
            SplashScreen(
                isLoggedIn = isLoggedIn,
                onNavigateNext = { route ->
                    navController.navigate(route) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // --- ONBOARDING FLOW ---
        
        // 1. Welcome Screen
        composable(route = NavRoutes.ONBOARDING_WELCOME) {
            WelcomeScreen(
                viewModel = viewModel,
                onContinueEmailClick = {
                    navController.navigate(NavRoutes.ONBOARDING_SIGNIN)
                },
                onGoogleSignInClick = {
                    navController.navigate(NavRoutes.MAIN_CONTAINER) {
                        popUpTo(NavRoutes.ONBOARDING_WELCOME) { inclusive = true }
                    }
                }
            )
        }

        // 2. Sign In
        composable(route = NavRoutes.ONBOARDING_SIGNIN) {
            SignInScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSignInSuccess = {
                    navController.navigate(NavRoutes.MAIN_CONTAINER) {
                        popUpTo(NavRoutes.ONBOARDING_WELCOME) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(NavRoutes.ONBOARDING_SIGNUP)
                },
                onForgotPasswordClick = {
                    navController.navigate(NavRoutes.ONBOARDING_FORGOT)
                }
            )
        }

        // 3. Sign Up
        composable(route = NavRoutes.ONBOARDING_SIGNUP) {
            SignUpScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSignUpSuccess = {
                    navController.navigate(NavRoutes.MAIN_CONTAINER) {
                        popUpTo(NavRoutes.ONBOARDING_WELCOME) { inclusive = true }
                    }
                },
                onSignInClick = {
                    navController.navigate(NavRoutes.ONBOARDING_SIGNIN)
                }
            )
        }

        // 4. Forgot Password
        composable(route = NavRoutes.ONBOARDING_FORGOT) {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- CORE MAIN WORKSPACE HUB ---

        // 5. Main Bottom Navigation Tab Container
        composable(route = NavRoutes.MAIN_CONTAINER) {
            MainTabContainer(
                viewModel = viewModel,
                onClientClick = { clientId ->
                    navController.navigate("${NavRoutes.CLIENT_DETAIL}/$clientId")
                },
                onAddClientClick = {
                    navController.navigate(NavRoutes.ADD_CLIENT)
                },
                onLogoutClick = {
                    navController.navigate(NavRoutes.ONBOARDING_WELCOME) {
                        popUpTo(NavRoutes.MAIN_CONTAINER) { inclusive = true }
                    }
                }
            )
        }

        // 6. Add Client Screen
        composable(route = NavRoutes.ADD_CLIENT) {
            AddClientScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onClientAdded = {
                    navController.navigate(NavRoutes.MAIN_CONTAINER) {
                        popUpTo(NavRoutes.MAIN_CONTAINER) { inclusive = false }
                    }
                }
            )
        }

        // 7. Client Detailed Management Hub
        composable(
            route = "${NavRoutes.CLIENT_DETAIL}/{clientId}",
            arguments = listOf(
                navArgument("clientId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
            ClientDetailScreen(
                viewModel = viewModel,
                clientId = clientId,
                onBackClick = { navController.popBackStack() },
                onAskAiClick = {
                    navController.navigate("${NavRoutes.AI_CHAT}/$clientId")
                },
                onClientDeleted = {
                    navController.navigate(NavRoutes.MAIN_CONTAINER) {
                        popUpTo(NavRoutes.MAIN_CONTAINER) { inclusive = false }
                    }
                }
            )
        }

        // 8. Dedicated Context Chat Shortcut
        composable(
            route = "${NavRoutes.AI_CHAT}/{clientId}",
            arguments = listOf(
                navArgument("clientId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
            viewModel.selectClient(clientId)
            
            AiAssistantScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
