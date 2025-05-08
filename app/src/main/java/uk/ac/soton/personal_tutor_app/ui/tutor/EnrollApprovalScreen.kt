package uk.ac.soton.personal_tutor_app.ui.tutor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Enrollment
import uk.ac.soton.personal_tutor_app.data.model.UserProfile
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository
import uk.ac.soton.personal_tutor_app.data.repository.UserRepository

@Composable
fun EnrollApprovalScreen(
    courseId: String
) {
    val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

    var pending by remember { mutableStateOf<List<Enrollment>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("课程报名审批", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(pending) { enr ->
                // 拉取学生档案
                val profile by produceState<UserProfile?>(initialValue = null, enr.studentId) {
                    value = UserRepository.getUserProfile(enr.studentId)
                }
                // 优先显示 displayName，若为空则显示 email，确保永远有内容
                val label = profile?.displayName
                    .takeUnless { it.isNullOrBlank() }
                    ?: profile?.email
                    ?: "未知用户"

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
                                try {
                                    EnrollmentRepository.updateStatus(enr.id, "accepted")
                                } catch (e: Exception) {
                                    /* 可以用 Snackbar 或 Toast 提示失败 */
                                }
                            }
                        }) { Text("同意") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch {
                                try {
                                    EnrollmentRepository.updateStatus(enr.id, "rejected")
                                } catch (e: Exception) {
                                    /* 提示失败 */
                                }
                            }
                        }) { Text("拒绝") }
                    }
                }
            }
        }
    }

    // 1) 实时监听该课程下所有 pending 报名
    LaunchedEffect(courseId) {
        EnrollmentRepository.getEnrollmentsForTutor(tutorId)
            .collect { list ->
                pending = list.filter { it.courseId == courseId && it.status == "pending" }
            }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("课程报名审批", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(pending) { enr ->
                // 2) 显示学生邮箱或昵称，而不是 ID
                val studentProfile by produceState<UserProfile?>(null, enr.studentId) {
                    value = UserRepository.getUserProfile(enr.studentId)
                }
                val label = studentProfile?.displayName
                    ?: studentProfile?.email
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
                                try {
                                    EnrollmentRepository.updateStatus(enr.id, "accepted")
                                } catch (e: Exception) {
                                    error = e.message
                                }
                            }
                        }) {
                            Text("同意")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch {
                                try {
                                    EnrollmentRepository.updateStatus(enr.id, "rejected")
                                } catch (e: Exception) {
                                    error = e.message
                                }
                            }
                        }) {
                            Text("拒绝")
                        }
                    }
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text("操作失败：$it", color = MaterialTheme.colorScheme.error)
        }
    }
}
