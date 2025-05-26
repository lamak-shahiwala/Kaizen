package com.example.persistence.data_classes

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class User(
    val email: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
