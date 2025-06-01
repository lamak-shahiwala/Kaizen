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

            val lastDate = lastCompletedTimestamp?.toDate()
            val lastCal = Calendar.getInstance().apply { time = lastDate ?: Date(0) }

            val todayCal = Calendar.getInstance().apply { time = todayDate }
            val yesterdayCal = Calendar.getInstance().apply { time = todayDate }
            yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)

            val isSameAsToday = lastCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    lastCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

            val isSameAsYesterday = lastCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                    lastCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

            if (isCompleted) {
                newCurrentStreak = when {
                    isSameAsToday -> currentStreak + 1 // restore streak after unchecking
                    isSameAsYesterday -> currentStreak + 1 // continued streak
                    else -> 1 // new streak
                }

                newLongestStreak = maxOf(newLongestStreak, newCurrentStreak)
                newLastCompletedDate = Timestamp.now()

            } else if (prevCompleted == true && logExisted) {
                if (isSameAsToday) {
                    newCurrentStreak = (currentStreak - 1).coerceAtLeast(0)
                }
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
