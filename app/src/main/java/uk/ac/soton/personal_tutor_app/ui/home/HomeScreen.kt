package uk.ac.soton.personal_tutor_app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    userEmail: String?,
    userRole: String,
    onLogout: () -> Unit,
    onViewCourses: () -> Unit,
    onNavigateProfile: () -> Unit,         // + 新增
    onNavigateUserApproval: () -> Unit,
    onNavigateDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome, ${userEmail ?: "User"}", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Logout")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onViewCourses, modifier = Modifier.fillMaxWidth()) {
            Text("查看我的课程")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 我的资料
        Button(onClick = onNavigateProfile, modifier = Modifier.fillMaxWidth()) {
            Text("我的资料")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Tutor 专属审批
        if (userRole == "Tutor") {
            Button(
                onClick = onNavigateUserApproval,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("学员审核")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateDashboard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dashboard")
            }
        }
    }
}