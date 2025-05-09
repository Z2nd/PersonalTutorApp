package uk.ac.soton.personal_tutor_app.data.model

import com.google.firebase.Timestamp

data class TutorAvailability(

    val tutorId: String = "",
    val timeSlots: List<TimeSlot> = emptyList(),
    val bookedSlots: List<BookedSlot> = emptyList()
)

data class BookedSlot(
    val start: Timestamp = Timestamp.now(),
    val end: Timestamp = Timestamp.now(),
    val studentId: String = ""
)

data class TimeSlot(
    val start: Timestamp = Timestamp.now(),
    val end: Timestamp = Timestamp.now()
)