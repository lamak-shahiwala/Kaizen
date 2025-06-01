package com.example.persistence.services.database_services.habits

import com.example.persistence.data_classes.Habit
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.FieldValue

fun DeleteHabit(habit: Habit, onComplete: (Boolean, String?) -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete(false, "No user found")
    val db = FirebaseFirestore.getInstance()
    val todayId = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

    val aggRef = db.collection("users").document(uid).collection("dailyAggregates").document(todayId)
    val habitRef = db.collection("users").document(uid).collection("habits").document(habit.habitId)
    val logsRef = habitRef.collection("logs")

    isHabitCompletedToday(habit.habitId) { isCompletedToday, error ->
        if (error != null || isCompletedToday == null) {
            onComplete(false, error)
            return@isHabitCompletedToday
        }

        logsRef.get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()

                // Delete all logs
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }

                // Delete habit doc
                batch.delete(habitRef)

                // Update daily aggregates
                val currentDate = Timestamp.now()
                batch.set(
                    aggRef,
                    mapOf(
                        "totalHabits" to FieldValue.increment(-1),
                        "completedHabits" to if (isCompletedToday) FieldValue.increment(-1) else FieldValue.increment(0),
                        "timeStamp" to currentDate
                    ),
                    SetOptions.merge()
                )

                batch.commit()
                    .addOnSuccessListener {
                        onComplete(true, null)
                    }
                    .addOnFailureListener { e ->
                        onComplete(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
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
