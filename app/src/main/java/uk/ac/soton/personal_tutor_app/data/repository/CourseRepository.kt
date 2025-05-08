package uk.ac.soton.personal_tutor_app.data.repository

//import androidx.paging.map
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.Course



object CourseRepository {
    private val coursesRef = Firebase.firestore.collection("courses")

    /** 实时监听当前 tutorId 的所有课程 */
    fun getCoursesForTutor(tutorId: String): Flow<List<Course>> = callbackFlow {
        val subscription = coursesRef
            .whereEqualTo("tutorId", tutorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)  // 流关闭并上报错误
                    return@addSnapshotListener
                }
                val list = snapshot
                    ?.documents
                    ?.mapNotNull { it.toObject(Course::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    /** 增加新课程 */
    suspend fun addCourse(course: Course) {
        coursesRef.add(course.copy(id = "")).await()
    }

    /** 更新课程信息 */
    suspend fun updateCourse(course: Course) {
        coursesRef.document(course.id).set(course).await()
    }

    /** 删除课程 */
    suspend fun deleteCourse(courseId: String) {
        coursesRef.document(courseId).delete().await()
    }
    // app/src/main/java/uk/ac/soton/personal_tutor_app/data/repository/CourseRepository.kt
    suspend fun getCourseById(id: String): Course {
        val snapshot = coursesRef.document(id).get().await()
        return snapshot.toObject(Course::class.java)
            ?.copy(id = id)
            ?: throw IllegalArgumentException("课程 $id 不存在")
    }

    fun getAllCourses(): Flow<List<Course>> {
        // 实现从数据源获取所有课程的逻辑
        // 例如：
        return FirebaseFirestore.getInstance()
            .collection("courses")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Course::class.java) }
            }
    }
}

