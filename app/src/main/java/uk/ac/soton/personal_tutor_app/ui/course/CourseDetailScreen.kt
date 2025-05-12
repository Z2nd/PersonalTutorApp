// File: CourseDetailScreen.kt
package uk.ac.soton.personal_tutor_app.ui.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Course
import uk.ac.soton.personal_tutor_app.data.model.Lesson
import uk.ac.soton.personal_tutor_app.data.repository.CourseRepository
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository
import uk.ac.soton.personal_tutor_app.data.repository.LessonRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    navController: NavHostController,
    courseId: String,
    userRole: String
) {
    val isNew = courseId == "new"
    val isTutor = userRole == "Tutor"
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val scope = rememberCoroutineScope()

    // Course fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    // Lessons and enrollment
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var isEnrolled by remember { mutableStateOf(false) }

    // Load course data
    LaunchedEffect(courseId) {
        if (!isNew) {
            val c = CourseRepository.getCourseById(courseId)
            title = c.title
            description = c.description
            selectedCategory = c.category ?: ""
        }
    }

    // Listen to lessons for progress
    LaunchedEffect(courseId) {
        LessonRepository.getLessonsForCourse(courseId).collect { list ->
            lessons = list
        }
    }

    // Check enrollment for student access
    LaunchedEffect(currentUid) {
        val enroll = EnrollmentRepository
            .getEnrollmentsForStudent(currentUid)
            .first()
            .firstOrNull { it.courseId == courseId }
        isEnrolled = enroll != null
    }

    // Compute progress from lesson.completed
    val totalCount     = lessons.size
    val completedCount = lessons.count { it.completed }
    val progress       = if (totalCount > 0) completedCount / totalCount.toFloat() else 0f

    Scaffold { padding ->
        Column(
            Modifier
                .systemBarsPadding()
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Progress bar
            Text("进度：$completedCount/$totalCount")
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            if (isTutor) {
                // Tutor edit fields
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Brief") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // … 下拉框保留原逻辑 …
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val tutorId = FirebaseAuth.getInstance().currentUser!!.uid
                            val c = Course(
                                id = if (isNew) "" else courseId,
                                title = title,
                                description = description,
                                tutorId = tutorId,
                                category = selectedCategory.ifBlank { null }
                            )
                            if (isNew) CourseRepository.addCourse(c)
                            else       CourseRepository.updateCourse(c)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isNew) "Save" else "Update")
                }
                if (!isNew) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                CourseRepository.deleteCourse(courseId)
                                navController.popBackStack()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete")
                    }
                }
            } else {
                // Student read-only
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(description)
                Spacer(Modifier.height(8.dp))
                Text("Category：${selectedCategory.ifBlank { "—" }}")
            }

            Spacer(Modifier.height(24.dp))

            // Entry to lesson list: Tutor always, student only if enrolled
            if (isTutor || isEnrolled) {
                Button(
                    onClick = { navController.navigate("lessons/$courseId") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isTutor) "Manage Lessons" else "Check Lessons")
                }
            }
        }
    }
}
