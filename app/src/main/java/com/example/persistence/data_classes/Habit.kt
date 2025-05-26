package com.example.persistence.data_classes

import com.google.firebase.Timestamp

data class Habit(
    val habitId: String = "",
    val habitName: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now()
)

