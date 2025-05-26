package com.example.persistence.screens.analytics.heatmap

// Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Android & Kotlin
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Jetpack Compose
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

// UI Utilities
import com.example.persistence.utilities.LoadingScreen

@Composable
fun AnalyticsHeatMapView() {
    val user = FirebaseAuth.getInstance().currentUser
    var habitMap by remember { mutableStateOf<Map<String, Map<String, Float>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (user == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        val db = FirebaseFirestore.getInstance()
        val userId = user.uid

        try {
            val habitsSnapshot = db.collection("users").document(userId).collection("habits").get().await()
            val allHabits = habitsSnapshot.documents

            val habitDataMap = mutableMapOf<String, Map<String, Float>>()

            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val today = Calendar.getInstance()
            val startOfYear = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTimestamp = Timestamp(startOfYear.time)

            for (habitDoc in allHabits) {
                val habitId = habitDoc.id
                val habitName = habitDoc.getString("habitName") ?: habitId

                val logsRef = db.collection("users").document(userId)
                    .collection("habits").document(habitId)
                    .collection("logs")

                val logsSnapshot = logsRef
                    .whereGreaterThanOrEqualTo("timeStamp", startTimestamp)
                    .get()
                    .await()

                val ratioMap = mutableMapOf<String, Float>()

                // Initialize dates with 0f
                val iterator = startOfYear.clone() as Calendar
                while (!iterator.after(today)) {
                    val dateStr = dateFormat.format(iterator.time)
                    ratioMap[dateStr] = 0f
                    iterator.add(Calendar.DAY_OF_YEAR, 1)
                }

                for (logDoc in logsSnapshot.documents) {
                    val dateId = logDoc.id
                    val isCompleted = logDoc.getBoolean("isCompleted") ?: false
                    val ratio = if (isCompleted) {
                        1f
                    } else {
                        0f
                    }
                    ratioMap[dateId] = ratio.coerceIn(0f, 1f)
                }

                habitDataMap[habitName] = ratioMap.toSortedMap()
            }

            habitMap = habitDataMap
        } catch (e: Exception) {
            errorMessage = e.message
        }

        isLoading = false
    }

    if (isLoading) {
        LoadingScreen()
        return
    }

    if (errorMessage != null) {
        Text("Error: $errorMessage")
        return
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        habitMap.forEach { (habitName, heatMapData) ->
            Text(
                text = habitName,
                style = MaterialTheme.typography.titleLarge,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 20.dp, top = 28.dp)
            )
            HabitHeatMap(completionMap = heatMapData)
            //Divider(Modifier.padding(horizontal = 6.dp, vertical = 28.dp), thickness = 1.dp)
        }
    }
}

@Composable
fun HabitHeatMap(completionMap: Map<String, Float>) {
    if (completionMap.isEmpty()) return

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val displayDateFormat = SimpleDateFormat("d", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Start calendar at the Sunday on or before Jan 1st this year, for proper week alignment
    val startCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, today.get(Calendar.YEAR))
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        if (after(today)) {
            add(Calendar.DAY_OF_YEAR, -7)
        }
    }

    val allDates = mutableListOf<Date>()
    val iterCalendar = startCalendar.clone() as Calendar
    while (!iterCalendar.after(today)) {
        allDates.add(iterCalendar.time)
        iterCalendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    val weeks = allDates.chunked(7)

    // Calculate month spans
    val monthSpans = mutableListOf<Pair<String, Int>>()
    var lastMonth = ""
    var count = 0
    for (week in weeks) {
        val firstDate = week.firstOrNull()
        val month = firstDate?.let { monthFormat.format(it) } ?: ""
        if (month != lastMonth) {
            if (count > 0) monthSpans.add(lastMonth to count)
            lastMonth = month
            count = 1
        } else {
            count++
        }
    }
    if (count > 0) monthSpans.add(lastMonth to count)

    val weekWidth = 32.dp + 4.dp

    // Create one scroll state for both header and dates
    val horizontalScrollState = rememberScrollState()

    Column {
        Row(
            modifier = Modifier
                .padding(start = 54.dp, bottom = 8.dp)
                .horizontalScroll(horizontalScrollState)
        ) {
            monthSpans.forEach { (month, span) ->
                Box(
                    modifier = Modifier
                        .width(weekWidth * span)
                        .padding(end = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = month,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                for (day in dayLabels) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Heatmap dates - inside the same horizontal scroll as the months header
            Row(
                modifier = Modifier.horizontalScroll(horizontalScrollState)
            ) {
                for (week in weeks) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        for (i in 0..6) {
                            val date = week.getOrNull(i) ?: continue
                            val dateStr = dateFormat.format(date)

                            val isFutureDate = Calendar.getInstance().apply {
                                time = date
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.after(today)

                            if (isFutureDate) continue

                            val ratio = completionMap[dateStr]
                            val cellColor = if (ratio == null || ratio == 0f)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.secondary.copy(alpha = 1f)

                            val dayNumber = displayDateFormat.format(date)

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = cellColor,
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ratio != null && ratio > 0.3f)
                                        MaterialTheme.colorScheme.onSecondary
                                    else
                                        MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
