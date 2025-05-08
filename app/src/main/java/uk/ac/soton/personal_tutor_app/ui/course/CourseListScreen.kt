
package uk.ac.soton.personal_tutor_app.ui.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Course
import uk.ac.soton.personal_tutor_app.data.model.Enrollment
import uk.ac.soton.personal_tutor_app.data.repository.CourseRepository
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    navController: NavHostController,
    userRole: String
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // All courses or tutor's courses
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    // Map<courseId, status>
    var statuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(userRole) {
        isLoading = true
        error = null
        try {
            if (userRole == "Tutor") {
                courses = CourseRepository.getCoursesByTutor(currentUid)
            } else {
                courses = CourseRepository.getAllCourses()
                // initial enrollment statuses for student
                val list = EnrollmentRepository
                    .getEnrollmentsForStudent(currentUid)
                    .first()
                statuses = list.associate { it.courseId to it.status }
            }
        } catch (e: Exception) {
            error = e.message
        }
        isLoading = false
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text("加载失败：$error", Modifier.align(Alignment.Center))
            } else {
                Column(Modifier.fillMaxSize()) {
                    // 搜索框
                    var searchQuery by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜索课程名称") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    // 创建课程按钮（Tutor）
                    if (userRole == "Tutor") {
                        Button(
                            onClick = { navController.navigate("courseDetail/new/$userRole") },
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) { Text("创建课程") }
                    }

                    // 过滤课程列表
                    val filteredCourses = remember(courses, searchQuery) {
                        if (searchQuery.isBlank()) courses
                        else courses.filter { it.title.contains(searchQuery, ignoreCase = true) }
                    }

                    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                        items(filteredCourses) { course ->
                            val stat = statuses[course.id]
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("courseDetail/${course.id}/$userRole")
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(course.title)
                                if (userRole == "Tutor") {
                                    Button(onClick = {
                                        navController.navigate("enrollApproval/${course.id}")
                                    }) { Text("报名审批") }
                                } else {
                                    val (label, enabled) = when (stat) {
                                        null -> "报名" to true
                                        "pending" -> "已申请" to false
                                        "accepted" -> "已报名" to false
                                        "rejected" -> "已拒绝" to false
                                        else -> stat to false
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    EnrollmentRepository.createEnrollment(
                                                        course.id,
                                                        currentUid
                                                    )
                                                    statuses = statuses + (course.id to "pending")
                                                    snackbarHost.showSnackbar("申请已发送")
                                                } catch (e: Exception) {
                                                    snackbarHost.showSnackbar("申请失败：${e.message}")
                                                }
                                            }
                                        },
                                        enabled = enabled
                                    ) { Text(label) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
