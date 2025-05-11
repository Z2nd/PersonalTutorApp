// File: LessonDetailScreen.kt
package uk.ac.soton.personal_tutor_app.ui.lesson

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Lesson
import uk.ac.soton.personal_tutor_app.data.model.LessonPage
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository
import uk.ac.soton.personal_tutor_app.data.repository.LessonRepository
import uk.ac.soton.personal_tutor_app.utils.FilePicker

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

    val context = LocalContext.current
    val activity = context as Activity
    val filePicker = remember { FilePicker(activity) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        filePicker.handleResult(result.resultCode, result.data)
    }

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
                    Button(onClick = {
                        filePicker.registerFilePicker(launcher) { uri ->
                            val contentResolver = context.contentResolver
                            val fileSize = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: -1
                            println("Selected file size: $fileSize bytes")

                            // 获取 Firebase Storage 实例
                            val storage = FirebaseStorage.getInstance()
                            val storageRef = storage.reference
                            val pdfRef = storageRef.child("pdfs/${System.currentTimeMillis()}.pdf")

                            // 上传文件
                            val uploadTask = pdfRef.putFile(uri)
                            uploadTask.addOnSuccessListener {
                                // 获取下载 URL
                                pdfRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    println("File uploaded successfully: $downloadUrl")
                                    // TODO: 将下载 URL 保存到页面数据中
                                }
                            }.addOnFailureListener { exception ->
                                println("File upload failed: ${exception.message}")
                            }
                        }
                    }) {
                        Text("上传/替换 PDF")
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

