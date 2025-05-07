package uk.ac.soton.personal_tutor_app.ui.lesson

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import uk.ac.soton.personal_tutor_app.data.model.Lesson
import uk.ac.soton.personal_tutor_app.data.repository.LessonRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun LessonListScreen(
    navController: NavHostController,
    courseId: String
) {
    // 订阅该课程下所有课时
    val lessons by LessonRepository
        .getLessonsForCourse(courseId)
        .collectAsState(initial = emptyList())

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .systemBarsPadding()
    ) {
        Button(onClick = { navController.navigate("lessonDetail/new/$courseId") }) {
            Text("新建课时")
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(lessons) { lesson ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("lessonDetail/${lesson.id}/$courseId")
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Text(text = lesson.title)
                    Spacer(Modifier.height(4.dp))
                    Text(text = lesson.content, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
