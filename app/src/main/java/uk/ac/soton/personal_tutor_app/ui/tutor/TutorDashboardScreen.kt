package uk.ac.soton.personal_tutor_app.ui.tutor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.Enrollment
import uk.ac.soton.personal_tutor_app.data.repository.EnrollmentRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun TutorDashboardScreen(
    navController: NavHostController
) {
    val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val enrollments by EnrollmentRepository
        .getEnrollmentsForTutor(tutorId)
        .collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("待审批报名", Modifier.padding(bottom = 8.dp))
        LazyColumn {
            items(enrollments.filter { it.status == "pending" }) { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("学生 ${e.studentId.takeLast(6)} 报名课程 ${e.courseId.takeLast(6)}")
                    Row {
                        Button(onClick = {
                            scope.launch { EnrollmentRepository.updateStatus(e.id, "accepted") }
                        }) { Text("接受") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch { EnrollmentRepository.updateStatus(e.id, "rejected") }
                        }) { Text("拒绝") }
                    }
                }
            }
        }
    }
}
