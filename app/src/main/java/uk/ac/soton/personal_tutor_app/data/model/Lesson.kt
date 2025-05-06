package uk.ac.soton.personal_tutor_app.data.model

import com.google.firebase.firestore.DocumentId

/**
 * 课程下的课时实体
 */
data class Lesson(
    @DocumentId val id: String = "",
    val courseId: String = "",
    val title: String = "",
    val description: String = "",          // 课时简介
    val pages: List<LessonPage> = emptyList()  // 最多 3 条学习资料
)

data class LessonPage(
    val text: String = "",
    val imageUrl: String? = null
)

