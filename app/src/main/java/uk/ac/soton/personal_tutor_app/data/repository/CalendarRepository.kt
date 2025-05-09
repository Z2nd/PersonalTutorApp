package uk.ac.soton.personal_tutor_app.data.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.TutorAvailability
import uk.ac.soton.personal_tutor_app.data.model.TimeSlot
import java.text.SimpleDateFormat
import java.util.Locale

object CalendarRepository {
    private val db = Firebase.firestore
    private val collection = db.collection("tutor_availability")

    /** 获取导师的可用时间 */
    suspend fun getTutorAvailability(tutorId: String): TutorAvailability? {
        val snapshot = collection.whereEqualTo("tutorId", tutorId).get().await()
        return if (snapshot.isEmpty) null else snapshot.documents.first().toObject(TutorAvailability::class.java)
    }

    /** 创建默认的导师可用时间 */
    suspend fun createDefaultAvailability(tutorId: String): TutorAvailability {
        val defaultAvailability = TutorAvailability(
            tutorId = tutorId,
            timeSlots = listOf()
        )
        collection.add(defaultAvailability).await()
        return defaultAvailability
    }

    /** 更新导师的时间段 */
    suspend fun updateTimeSlots(tutorId: String, timeSlots: List<TimeSlot>) {
        val snapshot = collection.whereEqualTo("tutorId", tutorId).get().await()
        if (snapshot.isEmpty) return
        snapshot.documents.first().reference.update("timeSlots", timeSlots).await()
    }

    /** 格式化时间戳为可读格式 */
    fun formatTimestamp(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
}