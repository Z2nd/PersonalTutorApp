// File: DashboardScreen.kt
package uk.ac.soton.personal_tutor_app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collect
import uk.ac.soton.personal_tutor_app.data.model.Course
import uk.ac.soton.personal_tutor_app.data.model.Enrollment
import uk.ac.soton.personal_tutor_app.data.repository.CourseRepository
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository

@Composable
fun DashboardScreen(navController: NavHostController) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var enrollments by remember { mutableStateOf<List<Enrollment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 拉取 tutor 的所有课程
    LaunchedEffect(currentUid) {
        courses = CourseRepository.getCoursesByTutor(currentUid)
        isLoading = false
    }
    // 拉取 tutor 所有课程的所有报名
    LaunchedEffect(currentUid) {
        EnrollmentRepository.getEnrollmentsForTutor(currentUid)
            .collect { list ->
                enrollments = list
            }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 按课程聚合报名数
    val countByCourse = remember(enrollments) {
        enrollments.groupingBy { it.courseId }.eachCount()
    }

    Column(Modifier.fillMaxSize().padding(16.dp).systemBarsPadding()) {
        Text("Dashboard: 课程与已报名学生数", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(courses) { course ->
                val count = countByCourse[course.id] ?: 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(course.title, style = MaterialTheme.typography.bodyLarge)
                    Text("已报名：$count", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
