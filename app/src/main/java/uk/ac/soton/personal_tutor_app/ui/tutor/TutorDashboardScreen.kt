package uk.ac.soton.personal_tutor_app.ui.tutor

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
import uk.ac.soton.personal_tutor_app.data.repository.LessonRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorDashboardScreen(
    navController: NavHostController
) {
    // 1. 当前导师 ID
    val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    // 2. UI 状态
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var enrolls by remember { mutableStateOf<List<Enrollment>>(emptyList()) }
    var lessonCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 3. 一次性加载所有数据
    LaunchedEffect(tutorId) {
        isLoading = true
        errorMessage = null
        try {
            // 3.1 拿到导师自己发布的课程
            courses = CourseRepository.getCoursesByTutor(tutorId)

            // 3.2 拿到所有已“accepted”的报名
            enrolls = EnrollmentRepository
                .getEnrollmentsForTutor(tutorId)
                .first()
                .filter { it.status == "accepted" }

            // 3.3 统计每门课的总课时数
            val counts = mutableMapOf<String, Int>()
            courses.forEach { course ->
                val lessons = LessonRepository
                    .getLessonsForCourse(course.id)
                    .first()
                counts[course.id] = lessons.size
            }
            lessonCounts = counts
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    // 4. 布局
    Scaffold { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Text(
                        text = "加载失败：$errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(courses) { course ->
                            // 计算本门课的已报名学生数
                            val studs = enrolls.filter { it.courseId == course.id }
                            val count = studs.size
                            // 计算平均完成进度
                            val totalLessons = lessonCounts[course.id] ?: 0
                            val averageProgress = if (totalLessons == 0 || count == 0) {
                                0f
                            } else {
                                studs
                                    .map { it.completedLessons.size.toFloat() / totalLessons }
                                    .average()
                                    .toFloat()
                            }

                            Column(Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = course.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("已报名学生：$count")
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = averageProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                )
                            }
                            Divider(Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
