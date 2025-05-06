package uk.ac.soton.personal_tutor_app.ui.lesson

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.catch
import uk.ac.soton.personal_tutor_app.data.model.Lesson
import uk.ac.soton.personal_tutor_app.data.repository.LessonRepository
import uk.ac.soton.personal_tutor_app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListScreen(
    navController: NavHostController,
    courseId: String,
    isTutor: Boolean
) {
    // 获取用户角色
    val authViewModel: AuthViewModel = viewModel()
    val uiState by authViewModel.uiState.collectAsState()
    val userRole = uiState.role

    // 页面状态
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 拉取课时列表
    LaunchedEffect(courseId) {
        isLoading = true
        error = null
        LessonRepository.getLessonsForCourse(courseId)
            .catch { e ->
                error = e.message
                isLoading = false
            }
            .collect { list ->
                lessons = list
                if (isLoading) isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tutor 专属：创建新课时按钮
        if (isTutor) {
            Button(
                onClick = { navController.navigate("lessonDetail/new/$courseId") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("创建课时")
            }
        }

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "加载课时失败：$error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            lessons.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无课时，请稍后添加")
                }
            }
            else -> {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                ) {
                    items(lessons) { lesson ->
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // 跳转到详情（编辑 or 查看）
                                    if (userRole == "Tutor") {
                                        navController.navigate("lessonDetail/${lesson.id}/$courseId")
                                    } else {
                                        navController.navigate("lessonDetail/${lesson.id}/$courseId?isTutor=false")
                                    }
                                }
                                .padding(vertical = 8.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
