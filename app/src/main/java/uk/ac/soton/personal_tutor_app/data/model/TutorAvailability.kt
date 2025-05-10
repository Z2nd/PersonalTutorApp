package uk.ac.soton.personal_tutor_app.data.model

import com.google.android.gms.common.internal.IAccountAccessor.Stub
import com.google.firebase.Timestamp

data class TutorAvailability(

    val tutorId: String = "",
    val timeSlots: List<TimeSlot> = emptyList(),
)


data class TimeSlot(
    val start: Timestamp = Timestamp.now(),
    val end: Timestamp = Timestamp.now(),
    val isAvailable: Boolean = true,
    val studentId: String = ""
)