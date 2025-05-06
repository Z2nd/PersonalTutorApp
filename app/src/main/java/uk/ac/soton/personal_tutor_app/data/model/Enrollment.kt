package uk.ac.soton.personal_tutor_app.data.model

import com.google.firebase.firestore.DocumentId

/**
 * 学生报名实体
 * @param id Firestore 自动生成 ID
 * @param courseId 报名的课程 ID
 * @param studentId 学生 UID
 * @param status    报名状态："pending" | "accepted" | "rejected"
 */
data class Enrollment(
    @DocumentId val id: String = "",
    val courseId: String = "",
    val studentId: String = "",
    val status: String = "pending"
)
