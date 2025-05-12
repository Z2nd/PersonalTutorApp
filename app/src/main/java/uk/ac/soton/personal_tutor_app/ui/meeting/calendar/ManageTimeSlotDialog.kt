package uk.ac.soton.personal_tutor_app.ui.meeting.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.ac.soton.personal_tutor_app.data.model.TimeSlot
import uk.ac.soton.personal_tutor_app.data.repository.UserRepository

@Composable
fun ManageTimeSlotDialog(
    timeSlot: TimeSlot,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var studentName by remember { mutableStateOf("Loading...") }

    LaunchedEffect(timeSlot.studentId) {
        if (!timeSlot.studentId.isNullOrEmpty()) {
            try {
                val profile = UserRepository.getUserProfile(timeSlot.studentId)
                studentName = profile.displayName ?: "Unknown Student"
            } catch (e: Exception) {
                studentName = "Failed to load student name"
            }
        } else {
            studentName = "No Student"
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Time Slot") },
        text = {
            Column {
                Text("Start Time: ${timeSlot.start.toDate()}")
                Spacer(Modifier.height(8.dp))
                Text("End Time: ${timeSlot.end.toDate()}")
                Spacer(Modifier.height(8.dp))
                Text("Student: ${studentName ?: "无"}")
            }
        },
        confirmButton = {
            Button(onClick = onDelete) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}