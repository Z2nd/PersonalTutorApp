package uk.ac.soton.personal_tutor_app.data.model

import com.google.firebase.firestore.DocumentId

/**
 * 课程下的课时实体
 */
data class Lesson(
    @DocumentId val id: String = "",
    val courseId: String = "",
    val title: String = "",
    val content: String = ""
)
