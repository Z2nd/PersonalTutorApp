package uk.ac.soton.personal_tutor_app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth

    suspend fun register(email: String, password: String): Result<Unit> = try {
        auth.createUserWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch(e: Exception) {
        Result.failure(e)
    }

    suspend fun login(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch(e: Exception) {
        Result.failure(e)
    }

    fun logout() {
        auth.signOut()
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid
}
