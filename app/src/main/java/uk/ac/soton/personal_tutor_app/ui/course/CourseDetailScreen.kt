package uk.ac.soton.personal_tutor_app.ui.course

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Course
import uk.ac.soton.personal_tutor_app.data.repository.CourseRepository

@Composable
fun CourseDetailScreen(
    navController: NavHostController,
    courseId: String,
    userRole: String
) {
    val isNew = courseId == "new"
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 回显
    LaunchedEffect(courseId) {
        if (!isNew) {
            val c = CourseRepository.getCourseById(courseId)
            title = c.title
            description = c.description
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (userRole == "Tutor") {
            // Tutor 可编辑
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("课程标题") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("课程简介") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                scope.launch {
                    val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                    val c = Course(
                        id = if (isNew) "" else courseId,
                        title = title,
                        description = description,
                        tutorId = tutorId
                    )
                    if (isNew) CourseRepository.addCourse(c)
                    else       CourseRepository.updateCourse(c)
                    navController.popBackStack()
                }
            }, Modifier.fillMaxWidth()) {
                Text(if (isNew) "保存" else "更新")
            }

            if (!isNew) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        CourseRepository.deleteCourse(courseId)
                        navController.popBackStack()
                    }
                }, Modifier.fillMaxWidth()) {
                    Text("删除")
                }
            }
        } else {
            // Student 只读显示
            Text(text = title, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(text = description, Modifier.fillMaxWidth())
        }

        val isTutor = (userRole == "Tutor")

        Spacer(Modifier.height(16.dp))
        // 查看 / 管理课时
        Button(
            onClick = {
                // 这里直接把 isTutor 插成 "true" 或 "false"
                navController.navigate("lessons/$courseId/$isTutor")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isTutor) "查看/管理课时" else "查看课时")
        }


    }
}
