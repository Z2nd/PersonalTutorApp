// File: LessonDetailScreen.kt
package uk.ac.soton.personal_tutor_app.ui.lesson

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Lesson
import uk.ac.soton.personal_tutor_app.data.model.LessonPage
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository
import uk.ac.soton.personal_tutor_app.data.repository.LessonRepository
import uk.ac.soton.personal_tutor_app.ui.lesson.utils.FilePicker
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color

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
                title = { Text(if (lessonId == "new") "Created Lesson" else "Lesson Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
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
                label = { Text("Title") },
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
                    label = { Text("Resource ${idx + 1}") },
                    enabled = isTutor,
                    modifier = Modifier.fillMaxWidth()
                )
                page.imageUrl?.let { url ->
                    val fileName = url.substringAfterLast("%").substringBeforeLast("?")
                    Text(
                        text = AnnotatedString(
                            text = "当前文件: $fileName",
                            spanStyle = androidx.compose.ui.text.SpanStyle(
                                color = Color.Blue,
                                textDecoration = TextDecoration.Underline
                            )
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                    )
                }
                if (isTutor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(onClick = {
                            filePicker.registerFilePicker(launcher) { uri ->
                                val storage = FirebaseStorage.getInstance()
                                val storageRef = storage.reference
                                val pdfRef = storageRef.child("pdfs/${System.currentTimeMillis()}.pdf")

                                val uploadTask = pdfRef.putFile(uri)
                                uploadTask.addOnSuccessListener {
                                    pdfRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                        println("File uploaded successfully: $downloadUrl")
                                        pages[idx] = pages[idx].copy(imageUrl = downloadUrl.toString())
                                    }
                                }.addOnFailureListener { exception ->
                                    println("File upload failed: ${exception.message}")
                                }
                            }
                        }) {
                            Text(if (page.imageUrl.isNullOrEmpty()) "Upload PDF" else "Replace PDF")
                        }
                        Button(onClick = { pages.removeAt(idx) }) {
                            Text("Delete Resource Page")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (isTutor) {
                Button(onClick = { pages.add(LessonPage()) },modifier = Modifier.fillMaxWidth()) {
                    Text("New Resource Page")
                }

                Spacer(Modifier.height(16.dp))
            }

            Divider()
            Spacer(Modifier.height(16.dp))

            // Student: show completion status
//            if (!isTutor) {
//                Text(
//                    text = if (isCompleted) "已完成" else "未完成",
//                    style = MaterialTheme.typography.titleLarge
//                )
//                Spacer(Modifier.height(16.dp))
//            }

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
                    Text(if (lessonId == "new") "Save" else "Update")
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
                        Text("Delete")
                    }
                }
            }
        }
    }
}

