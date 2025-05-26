package com.example.persistence.screens.home.habit

import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.persistence.R
import com.example.persistence.data_classes.Habit
import com.example.persistence.services.database_services.habits.DeleteHabit
import com.example.persistence.services.database_services.habits.updateHabitName
import com.example.persistence.utilities.HabitInputDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HabitCard(
    habit: Habit,
    isChecked: Boolean,
    onCheckChanged: (Habit, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardContainerColor =
        if (isChecked) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.tertiary
    val cardContentColor =
        if (isChecked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onTertiary

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
            contentColor = cardContentColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onCheckChanged(habit, it) },
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = MaterialTheme.colorScheme.background,
                        checkedColor = MaterialTheme.colorScheme.tertiary,
                        checkmarkColor = MaterialTheme.colorScheme.onTertiary,
                    )
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = habit.habitName,
                    color = if (isChecked) Color.Gray else MaterialTheme.colorScheme.onTertiary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
            }
            if (habit.currentStreak >1) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GlideImage(
                        model = R.drawable.streak_aura1,
                        contentDescription = "streak count gif",
                        modifier = modifier
                            .size(32.dp),
                    )
                    Text("${habit.currentStreak}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HabitListView(
    viewModel: HabitsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onRefreshHeatmap: () -> Unit
) {
    val habits by viewModel.habits.collectAsState()
    val habitStatus by viewModel.habitStatusMap.collectAsState()

    var habitBeingEdited by remember { mutableStateOf<Habit?>(null) }

    LazyColumn {
        items(habits) { habit ->
            SwipeableHabitCard(
                habit = habit,
                isChecked = habitStatus[habit.habitId] ?: false,
                onCheckChanged = { h, isChecked ->

                    viewModel.updateHabitStatus(h, isChecked) {
                        onRefreshHeatmap()
                    }
                },
                onDelete = { DeleteHabit(it) { _, _ -> } },
                onEdit = { habitBeingEdited = it }
            )

            if (habitBeingEdited?.habitId == habit.habitId) {
                HabitInputDialog(
                    initialText = habit.habitName,
                    onDismiss = { habitBeingEdited = null },
                    onConfirm = { newName ->
                        updateHabitName(habit.habitId, newName) { _, _ ->
                            habitBeingEdited = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SwipeableHabitCard(
    habit: Habit,
    isChecked: Boolean,
    onCheckChanged: (Habit, Boolean) -> Unit,
    onDelete: (Habit) -> Unit,
    onEdit: (Habit) -> Unit
) {
    val swipeOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val maxSwipe = 450f
    val scope = rememberCoroutineScope()

    var deleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        val newOffset = (swipeOffset.value + dragAmount).coerceIn(-maxSwipe, 0f)
                        scope.launch {
                            swipeOffset.snapTo(newOffset)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (swipeOffset.value < -80f) {
                                swipeOffset.animateTo(-maxSwipe)
                            } else {
                                swipeOffset.animateTo(0f)
                            }
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                ),
                modifier = Modifier.padding(horizontal = 10.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                IconButton(
                    modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                    onClick = { onEdit(habit) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Edit")
                }
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                IconButton(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                    onClick = {
                        deleteConfirm = true
                    }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }

        Box(modifier = Modifier.offset { IntOffset(swipeOffset.value.toInt(), 0) }) {
            HabitCard(
                habit = habit,
                isChecked = isChecked,
                onCheckChanged = onCheckChanged,
            )
        }

        if (deleteConfirm) {
            AlertDialog(
                onDismissRequest = { deleteConfirm = false },
                title = { Text("Delete Habit?") },
                text = { Text("A Saiyan wouldn't quit this easily. \nAre you sure?") },
                confirmButton = {
                    val roast = listOf(
                        "Yamcha could’ve kept that habit longer.",
                        "Even Krillin wouldn’t quit like that!",
                        "Vegeta scoffs at your weakness.",
                        "Frieza says: Weak. Just Pathetic.",
                        "Goku forgot stuff, but not habits!",
                        "That habit lasted shorter than Raditz.",
                        "Beerus is too bored to care.",
                        "Cell regenerated faster than that habit lasted.",
                        "Majin Buu had more self-discipline.",
                        "Not even Shenron can bring that habit back.",
                        "Goku says: That’s not very Super Saiyan of you.",
                        "This habit died faster than Planet Vegeta.",
                        "Whis says: Tsk. Not divine material.",
                    ).random()
                    TextButton(
                        onClick = {
                            onDelete(habit)
                            deleteConfirm = false
                            Toast.makeText(context, roast, Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirm = false }) {
                        Text("No")
                    }
                }
            )
        }
    }
}
