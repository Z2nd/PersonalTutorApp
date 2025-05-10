package uk.ac.soton.personal_tutor_app.ui.meeting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.collectLatest
import uk.ac.soton.personal_tutor_app.data.repository.UserRepository
import uk.ac.soton.personal_tutor_app.data.model.UserProfile

@Composable
fun StudentMeetingScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var tutorList by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            UserRepository.getAllTutors().collectLatest { tutors ->
                tutorList = tutors
                isLoading = false // 数据加载完成后更新状态
            }
        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false // 即使出错也需要停止加载状态
        }
    }

    Box(Modifier
        .systemBarsPadding()
        .fillMaxSize()
        .padding(16.dp)) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            errorMessage != null -> Text(
                "加载失败：$errorMessage",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> {
                LazyColumn {
                    items(tutorList) { tutor ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = tutor.displayName ?: "未知导师", modifier = Modifier.weight(1f))
                            Button(onClick = {
                                navController.navigate("tutorAvailableSlots/${tutor.id}")
                            }) {
                                Text("查看日历")
                            }
                        }
                    }
                }
            }
        }
    }
}