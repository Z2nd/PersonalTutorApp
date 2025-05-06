package uk.ac.soton.personal_tutor_app.data.model

import com.google.firebase.firestore.DocumentId

/**
 * 课程实体
 * @param id Firestore 自动生成的文档 ID
 * @param title 课程标题
 * @param description 课程简介
 * @param tutorId 发布该课程的用户 ID
 */
data class Course(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val tutorId: String = ""
)
