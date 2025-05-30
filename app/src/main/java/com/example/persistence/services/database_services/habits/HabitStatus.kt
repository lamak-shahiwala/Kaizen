import com.example.persistence.data_classes.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

fun HabitStatus(habit: Habit, isCompleted: Boolean, onComplete: (Boolean, String?) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onComplete(false, "User not authenticated")
        return
    }

    val db = FirebaseFirestore.getInstance()
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val todayDate = Date()
    val dateId = dateFormat.format(todayDate)

    val logRef = db.collection("users")
        .document(user.uid)
        .collection("habits")
        .document(habit.habitId)
        .collection("logs")
        .document(dateId)

    val habitRef = db.collection("users")
        .document(user.uid)
        .collection("habits")
        .document(habit.habitId)

    val aggregateRef = db.collection("users")
        .document(user.uid)
        .collection("dailyAggregates")
        .document(dateId)

    db.runTransaction { transaction ->
        val existingLog = transaction.get(logRef)
        val logExisted = existingLog.exists()
        val prevCompleted = existingLog.getBoolean("isCompleted")

        val existingAggregate = transaction.get(aggregateRef)
        var currentCompleted = existingAggregate.getLong("completedHabits") ?: 0
        val currentTotal = existingAggregate.getLong("totalHabits") ?: 0

        val habitSnapshot = transaction.get(habitRef)
        val currentStreak = habitSnapshot.getLong("currentStreak") ?: 0
        val longestStreak = habitSnapshot.getLong("longestStreak") ?: 0
        val lastCompletedTimestamp = habitSnapshot.getTimestamp("lastCompletedDate")

        val calendar = Calendar.getInstance().apply { time = todayDate }
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayDate = calendar.time

        var newCurrentStreak = currentStreak
        var newLongestStreak = longestStreak
        var newLastCompletedDate = lastCompletedTimestamp

        if (prevCompleted != isCompleted || !logExisted) {
            val logData = mapOf(
                "isCompleted" to isCompleted,
                "timeStamp" to Timestamp.now()
            )
            transaction.set(logRef, logData)

            if (isCompleted) {
                val completedYesterday = lastCompletedTimestamp?.toDate()?.let {
                    val lastCal = Calendar.getInstance().apply { time = it }
                    val yestCal = Calendar.getInstance().apply { time = yesterdayDate }
                    lastCal.get(Calendar.YEAR) == yestCal.get(Calendar.YEAR) &&
                            lastCal.get(Calendar.DAY_OF_YEAR) == yestCal.get(Calendar.DAY_OF_YEAR)
                } ?: false

                val lastDate = lastCompletedTimestamp?.toDate()
                val wasToday = lastDate?.let {
                    val cal = Calendar.getInstance().apply { time = it }
                    val todayCal = Calendar.getInstance().apply { time = todayDate }
                    cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                            cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                } ?: false

                // If habit was unchecked earlier today and now checked again, restore streak
                newCurrentStreak = if (wasToday && currentStreak > 0) {
                    currentStreak // restore streak without resetting
                } else if (completedYesterday) {
                    currentStreak + 1
                } else {
                    1
                }
                newLongestStreak = maxOf(newLongestStreak, newCurrentStreak)
                newLastCompletedDate = Timestamp.now()

            } else if (prevCompleted == true && logExisted) {
                val lastDate = lastCompletedTimestamp?.toDate()
                val wasToday = lastDate?.let {
                    val cal = Calendar.getInstance().apply { time = it }
                    val todayCal = Calendar.getInstance().apply { time = todayDate }
                    cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                            cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                } ?: false

                if (wasToday) {
                    newCurrentStreak = (currentStreak - 1).coerceAtLeast(0)
                    newLastCompletedDate = null
                }
                // if uncheck was not today, don't change streak or lastCompletedDate
            }

            transaction.update(habitRef, mapOf(
                "currentStreak" to newCurrentStreak,
                "longestStreak" to newLongestStreak,
                "lastCompletedDate" to newLastCompletedDate
            ))

            if (!logExisted && isCompleted) {
                currentCompleted += 1
            } else if (prevCompleted != isCompleted) {
                if (isCompleted) currentCompleted += 1
                else currentCompleted -= 1
            }

            currentCompleted = currentCompleted.coerceIn(0, currentTotal)

            transaction.set(aggregateRef, mapOf(
                "completedHabits" to currentCompleted,
                "totalHabits" to currentTotal,
                "timeStamp" to Timestamp.now()
            ), SetOptions.merge())
        }
    }.addOnSuccessListener {
        onComplete(true, null)
    }.addOnFailureListener { e ->
        onComplete(false, e.message)
    }
}
