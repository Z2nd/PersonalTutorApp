package uk.ac.soton.personal_tutor_app.ui.meeting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.TimeSlot
import uk.ac.soton.personal_tutor_app.data.repository.CalendarRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorAvailableSlotsScreen(navController: NavHostController, tutorId: String) {
    val currentId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()
    var timeSlots by remember { mutableStateOf<List<TimeSlot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tutorId) {
        isLoading = true
        errorMessage = null
        try {
            timeSlots = CalendarRepository.getTutorAvailability(tutorId)?.timeSlots ?: emptyList()
        } catch (e: Exception) {
            errorMessage = e.message
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导师可预约时间段") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    "加载失败：$errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                        items(timeSlots) { slot ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "开始时间: ${CalendarRepository.formatTimestamp(slot.start)}\n结束时间: ${CalendarRepository.formatTimestamp(slot.end)}"
                                )

                                Button(
                                    onClick = {
                                        if (slot.available) {
                                            scope.launch {
                                                try {
                                                    val success = CalendarRepository.bookTimeSlot(
                                                        tutorId = tutorId,
                                                        timeSlot = slot,
                                                        studentId = currentId
                                                    )
                                                    if (success) {
                                                        timeSlots = timeSlots.map {
                                                            if (it.start == slot.start && it.end == slot.end) {
                                                                it.copy(available = false)
                                                            } else {
                                                                it
                                                            }
                                                        }
                                                    } else {
                                                        errorMessage = "预约失败，时间段不可用。"
                                                    }
                                                } catch (e: Exception) {
                                                    errorMessage = "预约失败：${e.message}"
                                                }
                                            }
                                        }
                                    },
                                    enabled = slot.available,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (slot.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(if (slot.available) "预约" else "已预约")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}