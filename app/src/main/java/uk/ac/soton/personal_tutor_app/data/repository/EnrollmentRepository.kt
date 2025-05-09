// app/src/main/java/uk/ac/soton/personal_tutor_app/data/repository/EnrollmentRepository.kt
package uk.ac.soton.personal_tutor_app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.Enrollment

object EnrollmentRepository {
    private val ref = Firebase.firestore.collection("enrollments")

    /** 学生发起报名 */
    suspend fun createEnrollment(courseId: String, studentId: String) {
        val e = Enrollment(courseId = courseId, studentId = studentId,status    = "pending" )
        ref.add(e.copy(id = "")).await()
    }

    fun getEnrollmentsForTutor(tutorId: String): Flow<List<Enrollment>> = callbackFlow {
                try {
                        // 1. 在协程里同步拿到该导师的所有课程 ID
                        val coursesSnap = Firebase.firestore
                            .collection("courses")
                            .whereEqualTo("tutorId", tutorId)
                            .get()
                            .await()                             // <- import kotlinx.coroutines.tasks.await

                        val courseIds = coursesSnap.documents.map { it.id }

                        // 2. 基于课程 ID 列表启动快照监听
                        val sub = ref
                            .whereIn("courseId", courseIds)
                            .addSnapshotListener { snapshot, err ->
                                    if (err != null) { close(err); return@addSnapshotListener }
                                    val list = snapshot?.documents
                                        ?.mapNotNull { it.toObject(Enrollment::class.java) }
                                        ?: emptyList()
                                    trySend(list)
                                }

                        // 3. callbackFlow 退出时取消监听
                        awaitClose { sub.remove() }
                    } catch (e: Exception) {
                        close(e)
                    }
    }

    /** Student 拉取自己的报名 */
    fun getEnrollmentsForStudent(studentId: String): Flow<List<Enrollment>> = callbackFlow {
        val sub = ref.whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(Enrollment::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    /** Tutor 更新报名状态 accepted/rejected */
    suspend fun updateStatus(enrollId: String, newStatus: String) {
        ref.document(enrollId).update("status", newStatus).await()
    }

    /**
     * 标记某节课完成／未完成，需要给学生自己用。
     * completed=true 时会把 lessonId 加到 array 字段 completedLessons 里，
     * 否则移除它。
     */

    suspend fun markLessonComplete(
        enrollmentId: String,
        lessonId: String,
        completed: Boolean
    ) {
        val doc = ref.document(enrollmentId)
        if (completed) {
            doc.update("completedLessons", FieldValue.arrayUnion(lessonId)).await()
        } else {
            doc.update("completedLessons", FieldValue.arrayRemove(lessonId)).await()
        }
    }

}
