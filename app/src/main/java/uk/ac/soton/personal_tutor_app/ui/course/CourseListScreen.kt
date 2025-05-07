package uk.ac.soton.personal_tutor_app.ui.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue                     // ← for `by`
import androidx.compose.runtime.collectAsState            // ← for Flow.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp                         // ← for dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import uk.ac.soton.personal_tutor_app.data.model.Course
import uk.ac.soton.personal_tutor_app.data.repository.CourseRepository

@Composable
fun CourseListScreen(
    navController: NavHostController
) {
    // 1. 获取当前 tutorId
    val tutorId = FirebaseAuth.getInstance().currentUser?.uid
    if (tutorId == null) {
        Text("请先登录", modifier = Modifier.padding(16.dp))
        return
    }

    // 2. 订阅课程列表
    val courses by CourseRepository
        .getCoursesForTutor(tutorId)
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        // “新建课程”按钮
        Button(onClick = { navController.navigate("courseDetail/new") }) {
            Text("新建课程")
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 课程列表
        LazyColumn {
            items(courses) { course ->
                CourseItem(course) {
                    navController.navigate("courseDetail/${course.id}")
                }
            }
        }
    }
}

@Composable
private fun CourseItem(course: Course, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = course.title)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = course.description, modifier = Modifier.padding(start = 8.dp))
    }
}
