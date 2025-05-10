package uk.ac.soton.personal_tutor_app.data.repository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.UserProfile
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack


object UserRepository {
    private val db = Firebase.firestore
    private val usersRef = db.collection("users")

    /** 创建用户档案 */
    suspend fun createUserProfile(
        uid: String,
        email: String,
        role: String
    ) {
        val profile = UserProfile(
            id = uid,
            email = email,
            role = role,
            approved = role == "Tutor",   // Tutor 默认通过
            rejected = false
        )
        usersRef.document(uid).set(profile).await()
    }

    /** 拉取单个用户 */
    suspend fun getUserProfile(uid: String): UserProfile {
        val snap = usersRef.document(uid).get().await()
        return snap.toObject(UserProfile::class.java)
            ?: throw IllegalStateException("UserProfile $uid not found")
    }

    /** 获取所有待审核学生 */
    fun getPendingUsers(): Flow<List<UserProfile>> = callbackFlow {
        val sub = usersRef
            .whereEqualTo("role", "Student")
            .whereEqualTo("approved", false)
            .whereEqualTo("rejected", false)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(UserProfile::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    /** Tutor 通过 */
    suspend fun approveUser(uid: String) {
        usersRef.document(uid)
            .update("approved", true)
            .await()
    }

    /** Tutor 拒绝 */
    suspend fun rejectUser(uid: String) {
        usersRef.document(uid)
            .update("rejected", true)
            .await()
    }
    // app/src/main/java/uk/ac/soton/personal_tutor_app/data/repository/UserRepository.kt
    suspend fun updateUserProfile(profile: UserProfile) {
        usersRef.document(profile.id).set(profile).await()
    }

    /** 获取所有已审核的导师 */
    suspend fun getAllTutors(): Flow<List<UserProfile>> = callbackFlow {
        val sub = usersRef
            .whereEqualTo("role", "Tutor")
            .whereEqualTo("approved", true)
            .whereEqualTo("rejected", false)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(UserProfile::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

}
