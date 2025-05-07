package uk.ac.soton.personal_tutor_app.ui.lesson

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Lesson
import uk.ac.soton.personal_tutor_app.data.repository.LessonRepository

@Composable
fun LessonDetailScreen(
    navController: NavHostController,
    lessonId: String,
    courseId: String
) {
    val isNew = lessonId == "new"
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 编辑模式回显
    LaunchedEffect(lessonId) {
        if (!isNew) {
            val les = LessonRepository.getLessonById(lessonId)
            title = les.title
            content = les.content
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .systemBarsPadding()
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("课时标题") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("课时内容") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // 保存/更新
        Button(onClick = {
            scope.launch {
                val lesson = Lesson(
                    id = if (isNew) "" else lessonId,
                    courseId = courseId,
                    title = title,
                    content = content
                )
                if (isNew) LessonRepository.addLesson(lesson)
                else      LessonRepository.updateLesson(lesson)
                navController.popBackStack()
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isNew) "保存" else "更新")
        }

        // 删除
        if (!isNew) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    LessonRepository.deleteLesson(lessonId)
                    navController.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("删除")
            }
        }
    }
}
