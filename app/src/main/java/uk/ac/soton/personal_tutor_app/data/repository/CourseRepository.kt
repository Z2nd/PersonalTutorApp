// app/src/main/java/uk/ac/soton/personal_tutor_app/data/repository/CourseRepository.kt
package uk.ac.soton.personal_tutor_app.data.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.Course

object CourseRepository {
    private val coursesRef = Firebase.firestore.collection("courses")

    /** Tutor：获取自己发布的所有课程 */
    suspend fun getCoursesByTutor(tutorId: String): List<Course> {
        val snap = coursesRef
            .whereEqualTo("tutorId", tutorId)
            .get()
            .await()
        return snap.documents.mapNotNull { d ->
            d.toObject(Course::class.java)?.copy(id = d.id)
        }
    }

    /** Student：一次性拉取所有课程 */
    suspend fun getAllCourses(): List<Course> {
        val snap = coursesRef.get().await()
        return snap.documents.mapNotNull { d ->
            d.toObject(Course::class.java)?.copy(id = d.id)
        }
    }

    /** 根据 ID 拉单个课程 (编辑时回显) */
    suspend fun getCourseById(id: String): Course {
        val snap = coursesRef.document(id).get().await()
        return snap.toObject(Course::class.java)
            ?.copy(id = id)
            ?: throw IllegalArgumentException("Course $id not found")
    }

    /** 新增课程（Tutor） */
    suspend fun addCourse(course: Course) {
        // Firestore 会自动生成 ID，再写回对象里也可以
        coursesRef.add(course.copy(id = "")).await()
    }

    /** 更新课程（Tutor） */
    suspend fun updateCourse(course: Course) {
        coursesRef.document(course.id)
            .set(course)
            .await()
    }

    /** 删除课程（Tutor） */
    suspend fun deleteCourse(courseId: String) {
        coursesRef.document(courseId)
            .delete()
            .await()
    }
}
