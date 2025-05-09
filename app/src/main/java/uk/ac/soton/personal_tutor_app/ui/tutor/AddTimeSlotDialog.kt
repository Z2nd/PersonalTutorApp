package uk.ac.soton.personal_tutor_app.ui.tutor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import java.util.*

@Composable
fun AddTimeSlotDialog(
    onDismiss: () -> Unit,
    onConfirm: (start: Timestamp, end: Timestamp) -> Unit
) {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf<Timestamp?>(null) }
    var endTime by remember { mutableStateOf<Timestamp?>(null) }

    fun showDateTimePicker(context: Context, onDateTimeSelected: (Timestamp) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onDateTimeSelected(Timestamp(calendar.time))
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加时间段") },
        text = {
            Column {
                Button(
                    onClick = {
                        showDateTimePicker(context) { timestamp ->
                            startTime = timestamp
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(startTime?.let { "开始时间: ${it.toDate()}" } ?: "选择开始时间")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        showDateTimePicker(context) { timestamp ->
                            endTime = timestamp
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(endTime?.let { "结束时间: ${it.toDate()}" } ?: "选择结束时间")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (startTime != null && endTime != null) {
                        onConfirm(startTime!!, endTime!!)
                    }
                },
                enabled = startTime != null && endTime != null
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}