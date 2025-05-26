package com.example.persistence.services.database_services.habits

import com.example.persistence.data_classes.Habit
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun addHabitToUser(habit: Habit, onComplete: (Boolean, String?) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val todayId = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

    if (currentUser != null) {
        val uid = currentUser.uid
        val habitRef = db.collection("users").document(uid).collection("habits").document(habit.habitId)
        val aggRef = db.collection("users").document(uid).collection("dailyAggregates").document(todayId)

        db.runTransaction { transaction ->
            // 🔍 FIRST: Read
            val aggSnap = transaction.get(aggRef)
            val currentTotal = if (aggSnap.exists()) aggSnap.getLong("totalHabits") ?: 0 else 0
            val currentCompleted = if (aggSnap.exists()) aggSnap.getLong("completedHabits") ?: 0 else 0

            transaction.set(habitRef, habit)

            val newAgg: Map<String, Any> = mapOf(
                "totalHabits" to currentTotal + 1,
                "completedHabits" to currentCompleted,
                "timeStamp" to Timestamp.now()
            )
            transaction.set(aggRef, newAgg, SetOptions.merge())
        }.addOnSuccessListener {
            onComplete(true, null)
        }.addOnFailureListener { e ->
            onComplete(false, e.message)
        }
    } else {
        onComplete(false, "User not authenticated.")
    }
}