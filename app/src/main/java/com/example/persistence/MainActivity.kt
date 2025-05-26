package com.example.persistence
import com.example.persistence.services.auth_gate.AuthGate
import com.example.persistence.screens.settings.ThemeViewModel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.persistence.ui.theme.PersistenceTheme

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.example.persistence.screens.analytics.AnalyticsScreen
import com.example.persistence.screens.home.HomeScreen
import com.example.persistence.screens.settings.SettingsScreen

@OptIn(ExperimentalGlideComposeApi::class)
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition{true}
        CoroutineScope(Dispatchers.Main).launch {
            delay(0L)
            splashScreen.setKeepOnScreenCondition {false}
        }*/
        setContent {
            val darktheme by themeViewModel.darkTheme.collectAsState()
            val navController = rememberNavController()
            PersistenceTheme (darkTheme = darktheme){
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "auth_gate") {
                        composable("auth_gate") {
                            AuthGate(themeViewModel = themeViewModel, navController = navController)
                        }
                        composable("home") {
                            HomeScreen(themeViewModel = themeViewModel, navController = navController)
                        }
                        composable("settings") {
                            SettingsScreen(themeViewModel = themeViewModel, navController = navController)
                        }
                        composable("analytics") {
                            AnalyticsScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}

