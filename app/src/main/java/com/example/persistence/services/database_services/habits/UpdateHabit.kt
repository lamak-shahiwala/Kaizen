package com.example.persistence.services.database_services.habits

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

fun updateHabitName(habitId: String, newName: String, onComplete: (Boolean, String?) -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete(false, "No user")
    FirebaseFirestore.getInstance()
        .collection("users").document(uid)
        .collection("habits").document(habitId)
        .update("habitName", newName)
        .addOnSuccessListener { onComplete(true, null) }
        .addOnFailureListener { onComplete(false, it.message) }
}
