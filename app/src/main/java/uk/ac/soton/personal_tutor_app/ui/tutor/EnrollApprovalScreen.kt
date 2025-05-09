// File: EnrollApprovalScreen.kt
package uk.ac.soton.personal_tutor_app.ui.tutor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Enrollment
import uk.ac.soton.personal_tutor_app.data.model.UserProfile
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository
import uk.ac.soton.personal_tutor_app.data.repository.UserRepository

@Composable
fun EnrollApprovalScreen(
    courseId: String,
    navController: NavHostController
) {
    val tutorId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val scope = rememberCoroutineScope()

    var pending by remember { mutableStateOf<List<Enrollment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 实时监听该课程下 status == "pending" 的报名
    LaunchedEffect(tutorId, courseId) {
        isLoading = true
        error = null
        EnrollmentRepository.getEnrollmentsForTutor(tutorId)
            .catch { e ->
                error = e.message
                isLoading = false
            }
            .collect { list ->
                pending = list.filter { it.courseId == courseId && it.status == "pending" }
                isLoading = false
            }
    }

    Scaffold { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = "加载失败：$error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                pending.isEmpty() -> {
                    Text(
                        text = "暂无待审批报名",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn {
                        items(pending) { enr ->
                            // 异步拉取学生资料
                            val profile by produceState<UserProfile?>(
                                initialValue = null,
                                key1 = enr.studentId
                            ) {
                                value = runCatching {
                                    UserRepository.getUserProfile(enr.studentId)
                                }.getOrNull()
                            }
                            val label = profile?.displayName
                                .takeUnless { it.isNullOrBlank() }
                                ?: profile?.email
                                ?: enr.studentId

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label)
                                Row {
                                    Button(onClick = {
                                        scope.launch {
                                            EnrollmentRepository.updateStatus(enr.id, "accepted")
                                        }
                                    }) {
                                        Text("同意")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = {
                                        scope.launch {
                                            EnrollmentRepository.updateStatus(enr.id, "rejected")
                                        }
                                    }) {
                                        Text("拒绝")
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
