package uk.ac.soton.personal_tutor_app.data.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.Lesson
import kotlinx.coroutines.channels.awaitClose


object LessonRepository {
    private val lessonsRef = Firebase.firestore.collection("lessons")

    /** 实时监听某课程下的所有课时 */
    fun getLessonsForCourse(courseId: String): Flow<List<Lesson>> = callbackFlow {
        val sub = lessonsRef
            .whereEqualTo("courseId", courseId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Lesson::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    /** 单条读取 */
    suspend fun getLessonById(id: String): Lesson {
        val snap = lessonsRef.document(id).get().await()
        return snap.toObject(Lesson::class.java)
            ?.copy(id = id)
            ?: throw IllegalArgumentException("Lesson $id not found")
    }

    /** 添加课时 */
    suspend fun addLesson(lesson: Lesson) {
        lessonsRef.add(lesson.copy(id = "")).await()
    }

    /** 更新课时 */
    suspend fun updateLesson(lesson: Lesson) {
        lessonsRef.document(lesson.id).set(lesson).await()
    }

    /** 删除课时 */
    suspend fun deleteLesson(id: String) {
        lessonsRef.document(id).delete().await()
    }
}
