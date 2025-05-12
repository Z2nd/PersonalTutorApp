// File: CourseListScreen.kt
package uk.ac.soton.personal_tutor_app.ui.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Course
import uk.ac.soton.personal_tutor_app.data.model.Enrollment
import uk.ac.soton.personal_tutor_app.data.repository.CourseRepository
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository

@Composable
fun CourseListScreen(
    navController: NavHostController,
    userRole: String
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Courses state
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var isLoadingCourses by remember { mutableStateOf(true) }
    var coursesError by remember { mutableStateOf<String?>(null) }

    // Enrollment statuses for student
    var statuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingStatus by remember { mutableStateOf(true) }
    var statusError by remember { mutableStateOf<String?>(null) }

    // Load courses once
    LaunchedEffect(userRole) {
        isLoadingCourses = true
        coursesError = null
        try {
            courses = if (userRole == "Tutor") {
                CourseRepository.getCoursesByTutor(currentUid)
            } else {
                CourseRepository.getAllCourses()
            }
        } catch (e: Exception) {
            coursesError = e.message
        }
        isLoadingCourses = false
    }

    // For students, subscribe to enrollment status updates
    if (userRole != "Tutor") {
        LaunchedEffect(currentUid) {
            isLoadingStatus = true
            statusError = null
            try {
                // initial statuses
                val initialList = EnrollmentRepository
                    .getEnrollmentsForStudent(currentUid)
                    .first()
                statuses = initialList.associate { it.courseId to it.status }
                isLoadingStatus = false

                // continue to collect updates
                EnrollmentRepository.getEnrollmentsForStudent(currentUid)
                    .collect { list ->
                        statuses = list.associate { it.courseId to it.status }
                    }
            } catch (e: Exception) {
                statusError = e.message
                isLoadingStatus = false
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoadingCourses || (userRole != "Tutor" && isLoadingStatus) -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                coursesError != null -> {
                    Text(
                        "Failed to load course：$coursesError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                statusError != null -> {
                    Text(
                        "Failed to load status：$statusError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(Modifier
                        .systemBarsPadding()
                        .fillMaxSize()) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search Course Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )

                        // Tutor: create new course
                        if (userRole == "Tutor") {
                            Button(
                                onClick = { navController.navigate("courseDetail/new/$userRole") },
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Create Course")
                            }
                        }

                        // Apply search filter
                        val filtered = remember(courses, searchQuery) {
                            if (searchQuery.isBlank()) courses
                            else courses.filter { it.title.contains(searchQuery, ignoreCase = true) }
                        }

                        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                            items(filtered) { course ->
                                val stat = statuses[course.id]
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = userRole == "Tutor" || stat == "accepted") {
                                            if (userRole == "Tutor" || stat == "accepted") {
                                                navController.navigate("courseDetail/${course.id}/$userRole")
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(course.title)
                                    if (userRole == "Tutor") {
                                        Button(onClick = {
                                            navController.navigate("enrollApproval/${course.id}")
                                        }) {
                                            Text("Application Approval")
                                        }
                                    } else {
                                        val (label, enabled) = when (stat) {
                                            null -> "Apply" to true
                                            "pending" -> "Pending" to false
                                            "accepted" -> "Accepted" to false
                                            "rejected" -> "Rejected" to false
                                            else -> stat to false
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        EnrollmentRepository.createEnrollment(
                                                            course.id,
                                                            currentUid
                                                        )
                                                        snackbarHost.showSnackbar("Application sent")
                                                    } catch (e: Exception) {
                                                        snackbarHost.showSnackbar("Failed application：${e.message}")
                                                    }
                                                }
                                            }, enabled = enabled
                                        ) {
                                            Text(label)
                                        }
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}
