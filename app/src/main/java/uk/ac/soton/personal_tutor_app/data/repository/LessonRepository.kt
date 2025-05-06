// File: LessonRepository.kt
package uk.ac.soton.personal_tutor_app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.Lesson

object LessonRepository {
    private val db = FirebaseFirestore.getInstance()
    private val lessonsRef = db.collection("lessons")   // ← 顶层集合

    /** 实时监听某课程下的所有课时（filter by courseId field） */
    fun getLessonsForCourse(courseId: String): Flow<List<Lesson>> = callbackFlow {
        val sub = lessonsRef
            .whereEqualTo("courseId", courseId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Lesson::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    /** 读取某个课时详情（顶层） */
    suspend fun getLessonById(lessonId: String): Lesson {
        val snap = lessonsRef.document(lessonId).get().await()
        return snap.toObject(Lesson::class.java)
            ?.copy(id = lessonId)
            ?: throw IllegalArgumentException("Lesson $lessonId not found")
    }

    /** 新增或更新课时 */
    suspend fun saveOrUpdate(lesson: Lesson) {
        if (lesson.id.isBlank()) {
            lessonsRef.add(lesson.copy(id = "")).await()
        } else {
            lessonsRef.document(lesson.id).set(lesson).await()
        }
    }

    /** 删除课时 */
    suspend fun deleteLesson(lessonId: String) {
        lessonsRef.document(lessonId).delete().await()
    }
}
