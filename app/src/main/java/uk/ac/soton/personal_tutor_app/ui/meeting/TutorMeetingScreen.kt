package uk.ac.soton.personal_tutor_app.ui.meeting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.TutorAvailability
import uk.ac.soton.personal_tutor_app.data.model.TimeSlot
import uk.ac.soton.personal_tutor_app.data.repository.CalendarRepository
import uk.ac.soton.personal_tutor_app.ui.meeting.calendar.AddTimeSlotDialog
import uk.ac.soton.personal_tutor_app.ui.meeting.calendar.ManageTimeSlotDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorCalendarScreen(navController: NavHostController) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

    var tutorAvailability by remember { mutableStateOf<TutorAvailability?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var selectedTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var addTimeSlot by remember { mutableStateOf(false) }


    LaunchedEffect(currentUid) {
        isLoading = true
        errorMessage = null
        try {
            tutorAvailability = CalendarRepository.getTutorAvailability(currentUid)
                ?: CalendarRepository.createDefaultAvailability(currentUid)
        } catch (e: Exception) {
            errorMessage = e.message
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Slot") })
        }
    ) { padding ->
        Box(Modifier
            .fillMaxSize()
            .padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    "Fail to load：$errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                tutorAvailability != null -> {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Button(
                            onClick = {
                                showDialog = true
                                addTimeSlot = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Slot")
                        }

                        Spacer(Modifier.height(16.dp))

                        LazyColumn {
                            items(tutorAvailability!!.timeSlots) { slot ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Start time: ${CalendarRepository.formatTimestamp(slot.start)}\nEnd time: ${CalendarRepository.formatTimestamp(slot.end)}"
                                    )
                                    Button(onClick = {
                                        selectedTimeSlot = slot
                                        showDialog = true
                                    }) {
                                        Text("Manage")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            if (selectedTimeSlot != null) {
                ManageTimeSlotDialog(
                    timeSlot = selectedTimeSlot!!,
                    onDismiss = {
                        showDialog = false
                        selectedTimeSlot = null},
                    onDelete = {
                        scope.launch {
                            try {
                                val updatedSlots = tutorAvailability!!.timeSlots - selectedTimeSlot!!
                                CalendarRepository.updateTimeSlots(currentUid, updatedSlots)
                                tutorAvailability = tutorAvailability!!.copy(timeSlots = updatedSlots)
                                showDialog = false
                            } catch (e: Exception) {
                                errorMessage = e.message
                            }
                        }
                    }
                )
            }else if(addTimeSlot) {
                AddTimeSlotDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { start, end ->
                        scope.launch {
                            try {
                                val newSlot = TimeSlot(start = start, end = end)
                                val updatedSlots = tutorAvailability!!.timeSlots + newSlot
                                CalendarRepository.updateTimeSlots(currentUid, updatedSlots)
                                tutorAvailability =
                                    tutorAvailability!!.copy(timeSlots = updatedSlots)
                                showDialog = false
                            } catch (e: Exception) {
                                errorMessage = e.message
                            }
                        }
                    }
                )
            }
        }
    }
}