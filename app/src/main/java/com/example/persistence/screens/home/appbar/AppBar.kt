package com.example.persistence.screens.home.appbar

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.persistence.data_classes.Habit
import com.example.persistence.services.database_services.habits.addHabitToUser
import com.example.persistence.utilities.HabitInputDialog
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(modifier: Modifier = Modifier, onNavigationIconClick: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    TopAppBar(
        title = {
            Row (
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM, yyyy")))
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    showDialog = true
                }) { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit") }
            } },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Navigation Drawer")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,   // Background color
            titleContentColor = MaterialTheme.colorScheme.onBackground, // Title text color
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground // Icon color
        )
    )
    if (showDialog) {
        HabitInputDialog(
            onDismiss = { showDialog = false },
            onConfirm = { habitName ->
                showDialog = false
                if (habitName.isNotEmpty()) {
                    val newHabit = Habit(
                        habitId = UUID.randomUUID().toString(),
                        habitName = habitName,
                        currentStreak = 0,
                        longestStreak = 0,
                        lastCompletedDate = null,
                        createdAt = Timestamp.now()
                    )
                    Toast.makeText(context, "Habit Created: $habitName", Toast.LENGTH_SHORT).show()
                    addHabitToUser(habit = newHabit) { success, error ->
                        if (success) {
                            Toast.makeText(context, "Habit added!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Habit name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            },
            placeholder = "Create a new Habit"
        )
    }
}
