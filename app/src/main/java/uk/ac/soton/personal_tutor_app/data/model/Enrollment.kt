package uk.ac.soton.personal_tutor_app.data.model

import com.google.firebase.firestore.DocumentId

data class Enrollment(
    @DocumentId val id: String = "",
    val courseId: String = "",
    val studentId: String = "",
    val status: String = "",
    val completedLessons: List<String> = emptyList()
)

