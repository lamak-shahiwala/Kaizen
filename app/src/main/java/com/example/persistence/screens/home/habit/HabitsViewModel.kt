package com.example.persistence.screens.home.habit

import HabitStatus
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.persistence.data_classes.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class HabitsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    private val _habitStatusMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val optimisticStatusMap = mutableMapOf<String, Boolean>()

    val habits: StateFlow<List<Habit>> = _habits
    val habitStatusMap: StateFlow<Map<String, Boolean>> = _habitStatusMap

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val todayId = dateFormat.format(Date())

    init {
        if (userId != null) {
            listenToHabits()
        }
    }

    private fun listenToHabits() {
        db.collection("users").document(userId!!).collection("habits")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val habitList = snapshot?.documents?.mapNotNull { it.toObject(Habit::class.java) } ?: emptyList()
                _habits.value = habitList
                fetchTodayLogStatus(habitList)
            }
    }

    private fun fetchTodayLogStatus(habitList: List<Habit>) {
        viewModelScope.launch {
            val statusMapFromFirestore = mutableMapOf<String, Boolean>()
            habitList.forEach { habit ->
                val logRef = db.collection("users").document(userId!!)
                    .collection("habits").document(habit.habitId)
                    .collection("logs").document(todayId)

                try {
                    val document = logRef.get().await()
                    statusMapFromFirestore[habit.habitId] = document.getBoolean("isCompleted") ?: false
                } catch (e: Exception) {
                    // Handle error if needed
                }
            }

            // Merge optimistic updates
            val mergedMap = statusMapFromFirestore.toMutableMap()
            for ((habitId, localStatus) in optimisticStatusMap) {
                if (mergedMap[habitId] != localStatus) {
                    mergedMap[habitId] = localStatus
                }
            }
            _habitStatusMap.value = mergedMap.toMap()
        }
    }


    fun updateHabitStatus(habit: Habit, isChecked: Boolean, onComplete: () -> Unit = {}) {
        // Optimistic UI update
        val updatedMap = _habitStatusMap.value.toMutableMap()
        updatedMap[habit.habitId] = isChecked
        _habitStatusMap.value = updatedMap.toMap()

        // Store optimistic state
        optimisticStatusMap[habit.habitId] = isChecked

        // Firestore update
        HabitStatus(habit, isChecked) { success, error ->
            if (!success) {
                // Revert on failure
                val revertedMap = _habitStatusMap.value.toMutableMap()
                revertedMap[habit.habitId] = !isChecked
                _habitStatusMap.value = revertedMap.toMap()
                optimisticStatusMap[habit.habitId] = !isChecked
            }

            onComplete()
        }
    }

}
