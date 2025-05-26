package com.example.persistence.services.database_services.habits

import com.example.persistence.data_classes.Habit
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun DeleteHabit(habit: Habit, onComplete: (Boolean, String?) -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete(false, "No user found")
    val db = FirebaseFirestore.getInstance()
    val todayId = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

    val aggRef = db.collection("users").document(uid).collection("dailyAggregates").document(todayId)
    val habitRef = db.collection("users").document(uid).collection("habits").document(habit.habitId)

    isHabitCompletedToday(habit.habitId) { isCompletedToday, error ->
        if (error != null || isCompletedToday == null) {
            onComplete(false, error)
            return@isHabitCompletedToday
        }

        db.runTransaction { transaction ->
            // 🔍 Fetch current aggregate data
            val aggSnapshot = transaction.get(aggRef)
            val currentTotal = aggSnapshot.getLong("totalHabits") ?: 0
            val currentCompleted = aggSnapshot.getLong("completedHabits") ?: 0

            val newTotal = (currentTotal - 1).coerceAtLeast(0)
            val newCompleted = if (isCompletedToday) (currentCompleted - 1).coerceAtLeast(0) else currentCompleted

            // 🧹 Delete the habit
            transaction.delete(habitRef)

            // 🧮 Update aggregates
            transaction.set(
                aggRef,
                mapOf(
                    "totalHabits" to newTotal,
                    "completedHabits" to newCompleted,
                    "timeStamp" to Timestamp.now()
                ),
                SetOptions.merge()
            )
        }.addOnSuccessListener {
            onComplete(true, null)
        }.addOnFailureListener { e ->
            onComplete(false, e.message)
        }
    }
}

fun isHabitCompletedToday(habitId: String, onResult: (Boolean?, String?) -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null, "No user")
    val todayId = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    val logRef = FirebaseFirestore.getInstance()
        .collection("users")
        .document(uid)
        .collection("habits")
        .document(habitId)
        .collection("logs")
        .document(todayId)

    logRef.get()
        .addOnSuccessListener { doc ->
            if (doc.exists()) {
                onResult(doc.getBoolean("isCompleted") == true, null)
            } else {
                onResult(false, null)
            }
        }
        .addOnFailureListener { e ->
            onResult(null, e.message)
        }
}
