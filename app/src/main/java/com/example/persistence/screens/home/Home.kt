package com.example.persistence.screens.home

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.persistence.screens.home.appbar.AppBar
import com.example.persistence.screens.home.habit.HabitListView
import com.example.persistence.screens.home.heatmap.HeatMapView
import com.example.persistence.screens.home.navigation_drawer.DrawerBody
import com.example.persistence.screens.home.navigation_drawer.DrawerHeader
import com.example.persistence.screens.home.navigation_drawer.MenuItem
import com.example.persistence.screens.settings.ThemeViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(themeViewModel: ThemeViewModel, navController: NavController) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showLogOutAlert by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        gesturesEnabled = drawerState.isOpen,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                DrawerHeader()
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    thickness = 0.2.dp
                )

                // Main menu
                DrawerBody(
                    items = listOf(
                        MenuItem("home", "Home", Icons.Default.Home),
                        MenuItem("analytics", "Analytics", Icons.Default.ShowChart),
                        MenuItem("settings", "Settings", Icons.Default.Settings),
                    ),
                    onItemClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(it.id) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Logout item
                DrawerBody(
                    items = listOf(
                        MenuItem("logout", "Log Out", Icons.Default.Logout),
                    ),
                    onItemClick = {
                        if (it.id == "logout") {
                            showLogOutAlert = true
                        }
                    }
                )

                Spacer(modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    onNavigationIconClick = {
                        scope.launch { drawerState.open() }
                    }
                )
            },
            content = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    if (showLogOutAlert) {
                        AlertDialog(
                            onDismissRequest = { showLogOutAlert = false },
                            title = { Text("Logout?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showLogOutAlert = false
                                        FirebaseAuth.getInstance().signOut()
                                        Toast.makeText(context, "Logged Out", Toast.LENGTH_SHORT).show()
                                        navController.navigate("auth_gate") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                ) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLogOutAlert = false }) {
                                    Text("No")
                                }
                            }
                        )
                    }
                    var refreshHeatmap by remember { mutableStateOf(false) }

                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column (
                            modifier = Modifier
                                .padding(top = 10.dp, end = 16.dp)
                        ) {
                            HeatMapView(refreshTrigger = refreshHeatmap)
                        }
                        Box(modifier = Modifier
                            .padding(top = 20.dp)
                            .weight(1f)) {
                            HabitListView(
                                onRefreshHeatmap = { refreshHeatmap = !refreshHeatmap }
                            )
                        }
                    }

                }
            }
        )
    }
}
