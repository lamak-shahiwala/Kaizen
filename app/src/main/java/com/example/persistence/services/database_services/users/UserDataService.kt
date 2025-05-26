package com.example.persistence.services.database_services.users

import com.example.persistence.data_classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun createUserInFirestore(onComplete: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        val email = currentUser?.email
        val uid = currentUser?.uid

        if (email != null && uid != null) {
            val user = User(email = email)
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        onComplete(true, null) // User already exists
                    } else {
                        db.collection("users").document(uid).set(user)
                            .addOnSuccessListener { onComplete(true, null) }
                            .addOnFailureListener { e -> onComplete(false, e.message) }
                    }
                }
                .addOnFailureListener { e -> onComplete(false, e.message) }

        } else {
            onComplete(false, "User not signed in or email not available")
        }
    }
}
