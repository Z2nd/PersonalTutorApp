// File: app/src/main/java/uk/ac/soton/personal_tutor_app/data/model/Enrollment.kt
package uk.ac.soton.personal_tutor_app.data.model

import com.google.firebase.firestore.DocumentId

data class Enrollment(
    @DocumentId val id: String = "",
    val courseId: String = "",
    val studentId: String = "",
    val status: String = "pending",             // ← 默认 pending
    val completedLessons: List<String> = emptyList()
)

