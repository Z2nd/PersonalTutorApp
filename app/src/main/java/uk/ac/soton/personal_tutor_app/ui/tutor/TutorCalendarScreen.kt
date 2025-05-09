package uk.ac.soton.personal_tutor_app.ui.tutor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.TutorAvailability
import uk.ac.soton.personal_tutor_app.data.model.TimeSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorCalendarScreen(navController: NavHostController) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    // State for tutor availability
    var tutorAvailability by remember { mutableStateOf<TutorAvailability?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load tutor availability
    LaunchedEffect(currentUid) {
        isLoading = true
        errorMessage = null
        try {
            val snapshot = db.collection("tutor_availability")
                .whereEqualTo("tutorId", currentUid)
                .get()
                .await()

            if (snapshot.isEmpty) {
                // 如果没有文档，为当前 tutor 添加一个默认文档
                val defaultAvailability = TutorAvailability(
                    tutorId = currentUid,
                    timeSlots = listOf(
                        TimeSlot(start = Timestamp.now(), end = Timestamp.now())
                    )
                )
                db.collection("tutor_availability")
                    .add(defaultAvailability)
                    .await()
                tutorAvailability = defaultAvailability
            } else {
                tutorAvailability = snapshot.documents.first().toObject(TutorAvailability::class.java)
            }
        } catch (e: Exception) {
            errorMessage = e.message
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("我的日历") })
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    "加载失败：$errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                tutorAvailability != null -> {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        // Add new time slot button
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val newSlot = TimeSlot(
                                            start = Timestamp.now(),
                                            end = Timestamp.now()
                                        )
                                        val updatedSlots = tutorAvailability!!.timeSlots + newSlot
                                        db.collection("tutor_availability")
                                            .whereEqualTo("tutorId", currentUid)
                                            .get()
                                            .await()
                                            .documents
                                            .first()
                                            .reference
                                            .update("timeSlots", updatedSlots)
                                            .await()
                                        tutorAvailability = tutorAvailability!!.copy(timeSlots = updatedSlots)
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("添加可用时间段")
                        }

                        Spacer(Modifier.height(16.dp))

                        // Display time slots
                        LazyColumn {
                            items(tutorAvailability!!.timeSlots) { slot ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${slot.start} - ${slot.end}")
                                    Button(onClick = {
                                        scope.launch {
                                            try {
                                                val updatedSlots = tutorAvailability!!.timeSlots - slot
                                                db.collection("tutor_availability")
                                                    .whereEqualTo("tutorId", currentUid)
                                                    .get()
                                                    .await()
                                                    .documents
                                                    .first()
                                                    .reference
                                                    .update("timeSlots", updatedSlots)
                                                    .await()
                                                tutorAvailability = tutorAvailability!!.copy(timeSlots = updatedSlots)
                                            } catch (e: Exception) {
                                                errorMessage = e.message
                                            }
                                        }
                                    }) {
                                        Text("删除")
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