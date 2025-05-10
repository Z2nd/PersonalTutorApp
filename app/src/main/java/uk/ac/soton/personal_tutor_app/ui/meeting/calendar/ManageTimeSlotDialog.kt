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
    var studentName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(timeSlot.studentId) {
        studentName = UserRepository.getUserProfile(timeSlot.studentId).displayName
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理时间段") },
        text = {
            Column {
                Text("开始时间: ${timeSlot.start.toDate()}")
                Spacer(Modifier.height(8.dp))
                Text("结束时间: ${timeSlot.end.toDate()}")
                Spacer(Modifier.height(8.dp))
                Text("预约学生: ${studentName ?: "无"}")
            }
        },
        confirmButton = {
            Button(onClick = onDelete) {
                Text("删除")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}