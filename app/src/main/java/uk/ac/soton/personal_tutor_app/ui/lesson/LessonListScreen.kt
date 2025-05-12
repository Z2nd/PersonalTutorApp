// File: LessonListScreen.kt
package uk.ac.soton.personal_tutor_app.ui.lesson

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Lesson
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository
import uk.ac.soton.personal_tutor_app.data.repository.LessonRepository
import uk.ac.soton.personal_tutor_app.viewmodel.AuthViewModel

@Composable
fun LessonListScreen(
    navController: NavHostController,
    courseId: String,
    isTutor: Boolean
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val scope = rememberCoroutineScope()
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var completedLessons by remember { mutableStateOf<List<String>>(emptyList()) }
    var enrollmentId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId) {
        LessonRepository.getLessonsForCourse(courseId)
            .catch { e -> error = e.message }
            .collect { lessons = it }
    }

    LaunchedEffect(currentUid) {
        val enroll = EnrollmentRepository.getEnrollmentsForStudent(currentUid)
            .first()
            .firstOrNull { it.courseId == courseId }
        if (enroll != null) {
            enrollmentId = enroll.id
            completedLessons = enroll.completedLessons
        }
        isLoading = false
    }

    Column(
        Modifier
            .systemBarsPadding()
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isTutor) {
            Button(
                onClick = { navController.navigate("lessonDetail/new/$courseId") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Add New Lesson")
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
                    Text("Fail to load lessons：$error", color = MaterialTheme.colorScheme.error)
                }
            }
            lessons.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No lessons available, please add one")
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(lessons) { lesson ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("lessonDetail/${lesson.id}/$courseId")
                                },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = lesson.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isTutor) {
                                    Button(onClick = {
                                        scope.launch {
                                            LessonRepository.saveOrUpdate(
                                                lesson.copy(completed = !lesson.completed)
                                            )
                                        }
                                    }) {
                                        Text(if (lesson.completed) "Finished" else "Not Finished")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}