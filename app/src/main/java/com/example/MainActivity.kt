package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.SeoRepository
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SeoViewModel
import com.example.ui.viewmodel.SeoViewModelFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {

    // Initialize core SQLite Room DB, Auth & Repository cleanly using lazy architecture
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { SeoRepository(database.seoDao()) }
    private val authRepository by lazy { AuthRepository() }

    // Inject state-management ViewModel with custom factories
    private val viewModel: SeoViewModel by viewModels {
        SeoViewModelFactory(application, repository, authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase safely/fallback before super.onCreate and ViewModel loading
        initializeFirebaseSafely()

        super.onCreate(savedInstanceState)
        
        // Support beautiful edge-to-edge system navigation bars safe areas
        enableEdgeToEdge()
        
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            
            MyApplicationTheme(themeMode = themeMode) {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }

    private fun initializeFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:123456789012:android:0123456789abcdef012345")
                    .setApiKey("MockApiKeyString1234567890abcdefghijklm")
                    .setProjectId("mock-firebase-project-id")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("MainActivity", "Firebase initialized successfully via default fallback options.")
            } else {
                Log.d("MainActivity", "Firebase is already initialized by the system provider.")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Fatal error during dynamic Firebase config injection: ${e.localizedMessage}", e)
        }
    }
}
