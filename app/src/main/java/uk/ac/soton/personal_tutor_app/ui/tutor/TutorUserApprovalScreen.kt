package uk.ac.soton.personal_tutor_app.ui.tutor

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.model.UserProfile
import uk.ac.soton.personal_tutor_app.data.repository.UserRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext

@Composable
fun TutorUserApprovalScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending by UserRepository
        .getPendingUsers()
        .collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn {
            items(pending) { user ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = user.email)
                    Row {
                        // — 通过 —
                        Button(onClick = {
                            scope.launch {
                                try {
                                    UserRepository.approveUser(user.id)
                                    Toast.makeText(context, "Approved ${user.email}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Approval Failure：${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Text("Accept")
                        }
                        Spacer(Modifier.width(8.dp))
                        // — 拒绝 —
                        Button(onClick = {
                            scope.launch {
                                try {
                                    UserRepository.rejectUser(user.id)
                                    Toast.makeText(context, "Rejected ${user.email}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Rejection Failure：${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}