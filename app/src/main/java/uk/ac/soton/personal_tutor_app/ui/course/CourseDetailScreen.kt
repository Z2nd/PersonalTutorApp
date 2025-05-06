package uk.ac.soton.personal_tutor_app.ui.course

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Course
import uk.ac.soton.personal_tutor_app.data.repository.CourseRepository
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository
import uk.ac.soton.personal_tutor_app.viewmodel.AuthViewModel

@Composable
fun CourseDetailScreen(
    navController: NavHostController,
    courseId: String
) {
    // —— 1. 拿到 AuthViewModel，获取用户角色 ——
    val authViewModel: AuthViewModel = viewModel()
    val uiState by authViewModel.uiState.collectAsState()
    val userRole = uiState.role

    // —— 2. 课程回显 ——
    val isNew = courseId == "new"
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(courseId) {
        if (!isNew) {
            val course = CourseRepository.getCourseById(courseId)
            title = course.title
            description = course.description
        }
    }

    // —— 3. 协程作用域 & 学生报名状态 ——
    val scope = rememberCoroutineScope()
    var hasPending by remember { mutableStateOf(false) }

    if (userRole == "Student") {
        LaunchedEffect(courseId) {
            val studentId = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
            EnrollmentRepository
                .getEnrollmentsForStudent(studentId)
                .collect { list ->
                    hasPending = list.any { it.courseId == courseId }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // —— 标题 & 简介 输入 ——
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("课程标题") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("课程简介") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // —— 保存 / 更新 按钮 ——
        Button(
            onClick = {
                scope.launch {
                    val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                    val course = Course(
                        id          = if (isNew) "" else courseId,
                        title       = title,
                        description = description,
                        tutorId     = tutorId
                    )
                    if (isNew) CourseRepository.addCourse(course)
                    else      CourseRepository.updateCourse(course)
                    navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isNew) "保存" else "更新")
        }

        // —— 删除 按钮（仅编辑模式可见） ——
        if (!isNew) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        CourseRepository.deleteCourse(courseId)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("删除")
            }
        }

        Spacer(Modifier.height(8.dp))

        // —— 课时管理 按钮（Tutor & Student 共用） ——
        Button(
            onClick = { navController.navigate("lessons/$courseId") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("查看/管理课时")
        }

        Spacer(Modifier.height(8.dp))

        // —— 学生报名 按钮（仅 Student 可见） ——
        if (userRole == "Student") {
            Button(
                onClick = {
                    scope.launch {
                        val studentId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                        EnrollmentRepository.createEnrollment(courseId, studentId)
                    }
                },
                enabled = !hasPending,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasPending) "已报名" else "报名")
            }
        }
    }
}
