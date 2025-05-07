package uk.ac.soton.personal_tutor_app.ui.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue                     // ← for `by`
import androidx.compose.runtime.collectAsState            // ← for Flow.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp                         // ← for dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import uk.ac.soton.personal_tutor_app.data.model.Course
import uk.ac.soton.personal_tutor_app.data.model.UserProfile
import uk.ac.soton.personal_tutor_app.data.repository.CourseRepository
import uk.ac.soton.personal_tutor_app.data.repository.UserRepository

@Composable
fun CourseListScreen(
    navController: NavHostController
) {
    // 1. 获取当前用户 ID
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (currentUserId == null) {
        Text("请先登录", modifier = Modifier.padding(16.dp))
        return
    }

    // 2. 订阅课程列表
    // 注意：这里的逻辑可能需要根据您的应用需求调整。
    // 如果学生只能看到他们选的课程，您需要修改 CourseRepository.getCoursesForTutor
    // 或者创建一个新的方法来获取学生相关的课程。
    // 为了演示目的，我们暂时沿用获取导师课程的方法。
    val courses by CourseRepository
        .getCoursesForTutor(currentUserId) // 这里可能需要根据用户角色调整
        .collectAsState(initial = emptyList())

    // 3. 获取当前用户的 UserProfile
    val currentUserProfile by produceState<UserProfile?>(initialValue = null, currentUserId) {
        try {
            value = UserRepository.getUserProfile(currentUserId)
        } catch (e: Exception) {
            // 处理获取用户资料时的错误，例如日志记录或显示错误消息
            e.printStackTrace()
            value = null // 获取失败时设置为 null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        // 4. 根据用户角色条件渲染“新建课程”按钮
        // 只有当用户资料加载成功且角色是 "Tutor" 时才显示按钮
        if (currentUserProfile?.role == "Tutor") {
            Button(onClick = { navController.navigate("courseDetail/new") }) {
                Text("新建课程")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 课程列表
        LazyColumn {
            items(courses) { course ->
                CourseItem(course) {
                    navController.navigate("courseDetail/${course.id}")
                }
            }
        }
    }
}

@Composable
private fun CourseItem(course: Course, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = course.title)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = course.description, modifier = Modifier.padding(start = 8.dp))
    }
}