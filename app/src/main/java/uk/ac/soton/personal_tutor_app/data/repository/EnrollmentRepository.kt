package uk.ac.soton.personal_tutor_app.data.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.Enrollment

object EnrollmentRepository {
    private val db = Firebase.firestore
    private val coursesRef = db.collection("courses")
    private val enrollmentsRef = db.collection("enrollments")

    /** 学生发起报名 */
    suspend fun createEnrollment(courseId: String, studentId: String) {
        val e = Enrollment(courseId = courseId, studentId = studentId)
        enrollmentsRef.add(e.copy(id = "")).await()
    }

    /** 获取某课程所有报名（Tutor 用） */
    fun getEnrollmentsForTutor(tutorId: String): Flow<List<Enrollment>> = callbackFlow {
        // 1. 先同步拉一次 Tutor 拥有的课程 ID 列表
        val snap = coursesRef
            .whereEqualTo("tutorId", tutorId)
            .get()
            .await()

        val courseIds = snap.documents.map { it.id }
        if (courseIds.isEmpty()) {
            trySend(emptyList())
            awaitClose() // 没课程可监听，就直接关闭
            return@callbackFlow
        }

        // 2. 实时监听这些课程对应的 enrollment 文档
        val subscription = enrollmentsRef
            .whereIn("courseId", courseIds)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    val list = snapshot
                        ?.documents
                        ?.mapNotNull { it.toObject(Enrollment::class.java) }
                        ?: emptyList()
                    trySend(list)
                }
            }

        // 3. 等待关闭，移除监听
        awaitClose { subscription.remove() }
    }

    /** 获取学生自己的所有报名（Student 用） */
    fun getEnrollmentsForStudent(studentId: String): Flow<List<Enrollment>> = callbackFlow {
        val subscription = enrollmentsRef
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    val list = snapshot
                        ?.documents
                        ?.mapNotNull { it.toObject(Enrollment::class.java) }
                        ?: emptyList()
                    trySend(list)
                }
            }
        awaitClose { subscription.remove() }
    }

    /** Tutor 更新报名状态 */
    suspend fun updateStatus(enrollId: String, newStatus: String) {
        enrollmentsRef.document(enrollId)
            .update("status", newStatus)
            .await()
    }
}
