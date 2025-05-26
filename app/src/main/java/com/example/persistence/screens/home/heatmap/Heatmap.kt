package com.example.persistence.screens.home.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.persistence.utilities.LoadingScreen
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

fun Heatmap(
    onComplete: (Map<String, Float>, String?) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onComplete(emptyMap(), "User not authenticated")
        return
    }

    val db = FirebaseFirestore.getInstance()
    val userLogsRef = db.collection("users").document(user.uid).collection("dailyAggregates")

    val today = Calendar.getInstance()

    val startCalendar = Calendar.getInstance()
    startCalendar.time = today.time
    startCalendar.add(Calendar.DAY_OF_YEAR, -59) // 60 days including today

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val startTimestamp = Timestamp(startCalendar.time)

    userLogsRef
        .whereGreaterThanOrEqualTo("timeStamp", startTimestamp)
        .get()
        .addOnSuccessListener { querySnapshot ->
            val ratioMap = mutableMapOf<String, Float>()

            // Initialize 60 days, default 0f
            val dayIterator = Calendar.getInstance()
            dayIterator.time = today.time
            dayIterator.add(Calendar.DAY_OF_YEAR, -59)

            for (i in 0 until 60) {
                val dateStr = dateFormat.format(dayIterator.time)
                ratioMap[dateStr] = 0f
                dayIterator.add(Calendar.DAY_OF_YEAR, 1)
            }

            for (doc in querySnapshot.documents) {
                val logDate = doc.id
                val total = doc.getLong("totalHabits") ?: 0
                val completed = doc.getLong("completedHabits") ?: 0
                val ratio = if (total > 0) completed.toFloat() / total else 0f
                ratioMap[logDate] = ratio.coerceIn(0f, 1f) // Clamp to 0..1
            }

            onComplete(ratioMap.toSortedMap(), null)
        }
        .addOnFailureListener { e ->
            onComplete(emptyMap(), e.message)
        }
}


@Composable
fun HeatMapView(refreshTrigger: Boolean) { // pass a trigger boolean that changes on habit update
    var completionMap by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger) { // reload heatmap every time refreshTrigger changes
        isLoading = true
        Heatmap { map, error ->
            if (error == null) {
                completionMap = map
            } else {
                errorMessage = error
            }
            isLoading = false
        }
    }

    if (isLoading) {
        LoadingScreen()
    }

    if (errorMessage != null) {
        Text("Error loading data: $errorMessage")
        return
    }

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

    val startCalendar = Calendar.getInstance().apply {
        time = today.time
        add(Calendar.WEEK_OF_YEAR, -6)
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }

    val allDates = mutableListOf<Date>()
    val iterCalendar = startCalendar.clone() as Calendar
    while (!iterCalendar.after(today)) {
        allDates.add(iterCalendar.time)
        iterCalendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    val weeks = allDates.chunked(7)

    // Month Row
    Row(modifier = Modifier.padding(start = 54.dp, bottom = 8.dp)) {
        var lastMonth: String? = null
        for (week in weeks) {
            val firstDate = week.firstOrNull()
            val month = firstDate?.let { monthFormat.format(it) } ?: ""
            val showMonth = month != lastMonth
            lastMonth = month

            Box(
                modifier = Modifier
                    .width(32.dp)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showMonth) {
                    Text(
                        text = month,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Main Grid Row
    Row(modifier = Modifier.padding(start = 16.dp)) {
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
        for (week in weeks) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                for (i in 0..6) {
                    val date = week.getOrNull(i) ?: continue
                    val dateStr = dateFormat.format(date)

                    // Skip future dates
                    val isFutureDate = run {
                        val cal = Calendar.getInstance().apply {
                            time = date
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        cal.after(today)
                    }
                    if (isFutureDate) continue

                    val ratio = completionMap[dateStr]
                    val cellColor = when {
                        ratio == null || ratio == 0f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = ratio.coerceIn(0f, 1f))
                    }

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