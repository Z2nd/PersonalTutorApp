package uk.ac.soton.personal_tutor_app.data.model

import com.google.firebase.firestore.DocumentId

data class UserProfile(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val role: String = "",
    val approved: Boolean = false,
    val rejected: Boolean = false
)
