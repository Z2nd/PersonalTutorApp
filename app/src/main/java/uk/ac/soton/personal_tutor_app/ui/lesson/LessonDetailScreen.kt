// File: LessonDetailScreen.kt
package uk.ac.soton.personal_tutor_app.ui.lesson

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Lesson
import uk.ac.soton.personal_tutor_app.data.model.LessonPage
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository
import uk.ac.soton.personal_tutor_app.data.repository.LessonRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    navController: NavHostController,
    lessonId: String,
    courseId: String,
    isTutor: Boolean
) {
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    val pages = remember { mutableStateListOf<LessonPage>() }
    var isCompleted by remember { mutableStateOf(false) }
    var enrollmentId by remember { mutableStateOf("") }

    // Load lesson if editing existing
    LaunchedEffect(lessonId) {
        if (lessonId != "new") {
            val les = LessonRepository.getLessonById(lessonId)
            title = les.title
            pages.clear(); pages.addAll(les.pages)
            if (!isTutor) {
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val enroll = EnrollmentRepository
                    .getEnrollmentsForStudent(uid)
                    .first()
                    .first { it.courseId == courseId }
                enrollmentId = enroll.id
                isCompleted = enroll.completedLessons.contains(lessonId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (lessonId == "new") "新建课时" else "课时详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("课时名称") },
                enabled = isTutor,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Divider()
            Spacer(Modifier.height(12.dp))

            // Lesson content pages
            pages.forEachIndexed { idx, page ->
                OutlinedTextField(
                    value = page.text,
                    onValueChange = { newText ->
                        pages[idx] = page.copy(text = newText)
                    },
                    label = { Text("资料 ${idx + 1} 文本") },
                    enabled = isTutor,
                    modifier = Modifier.fillMaxWidth()
                )
                page.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(vertical = 8.dp)
                    )
                }
                if (isTutor) {
                    Button(onClick = { /* TODO: select & upload image, then update pages[idx] */ }) {
                        Text("上传/替换图片")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (isTutor && pages.size < 3) {
                Button(onClick = { pages.add(LessonPage()) }) {
                    Text("新增资料页")
                }
                Spacer(Modifier.height(16.dp))
            }

            Divider()
            Spacer(Modifier.height(16.dp))

            // Student: show completion status
            if (!isTutor) {
                Text(
                    text = if (isCompleted) "已完成" else "未完成",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))
            }

            // Tutor: Save/Update & Delete
            if (isTutor) {
                Button(
                    onClick = {
                        scope.launch {
                            val idToSave = if (lessonId == "new") "" else lessonId
                            val lesson = Lesson(
                                id = idToSave,
                                courseId = courseId,
                                title = title,
                                pages = pages.toList()
                            )
                            LessonRepository.saveOrUpdate(lesson)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (lessonId == "new") "保存" else "更新")
                }
                if (lessonId != "new") {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                LessonRepository.deleteLesson(lessonId)
                                navController.popBackStack()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("删除")
                    }
                }
            }
        }
    }
}
