package uk.ac.soton.personal_tutor_app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import uk.ac.soton.personal_tutor_app.ui.auth.LoginScreen
import uk.ac.soton.personal_tutor_app.ui.auth.RegisterScreen
import uk.ac.soton.personal_tutor_app.ui.course.CourseListScreen
import uk.ac.soton.personal_tutor_app.ui.course.CourseDetailScreen
import uk.ac.soton.personal_tutor_app.ui.home.HomeScreen
import uk.ac.soton.personal_tutor_app.ui.lesson.LessonListScreen
import uk.ac.soton.personal_tutor_app.ui.lesson.LessonDetailScreen
import uk.ac.soton.personal_tutor_app.ui.profile.ProfileScreen
import uk.ac.soton.personal_tutor_app.ui.tutor.TutorDashboardScreen
import uk.ac.soton.personal_tutor_app.ui.theme.PersonalTutorAppTheme
import uk.ac.soton.personal_tutor_app.ui.tutor.TutorUserApprovalScreen
import uk.ac.soton.personal_tutor_app.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        /* ---------- 监听当前登录状态 ---------- */
        firebaseAuth.addAuthStateListener { auth ->
            Log.d("FIREBASE_TEST", "currentUser=${auth.currentUser?.uid}")
        }

        /* ---------- 若无账号则注册测试邮箱 ---------- */
        if (firebaseAuth.currentUser == null) {
            firebaseAuth
                .createUserWithEmailAndPassword("test@example.com", "123456")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FIREBASE_TEST", "signUp SUCCESS uid=${task.result.user?.uid}")
                    } else {
                        Log.d("FIREBASE_TEST", "signUp FAILED: ${task.exception?.message}")
                    }
                }
        }

        /* ---------- Compose UI ---------- */
        setContent {
            PersonalTutorAppTheme {
                MaterialTheme {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        // 登录页
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }
                        // 注册页
                        composable("register") {
                            RegisterScreen(
                                viewModel = authViewModel,
                                onRegisterSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        // 主页
                        composable("home") {
                            val uiState by authViewModel.uiState.collectAsState()
                            HomeScreen(
                                userEmail                = uiState.email,
                                userRole                 = uiState.role,                   // + 传入角色
                                onLogout                 = {
                                    authViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onViewCourses            = { navController.navigate("courses") },
                                onNavigateProfile       = { navController.navigate("profile") },
                                onNavigateUserApproval = { navController.navigate("userApproval") }
                            )
                        }
                        // 课程列表
                        composable("courses") {
                            CourseListScreen(navController)
                        }
                        // 课程详情
                        composable("courseDetail/{courseId}") { backStackEntry ->
                            val cid = backStackEntry.arguments?.getString("courseId") ?: "new"
                            CourseDetailScreen(navController, courseId = cid)
                        }
                        // Tutor 审批中心
                        composable("tutorDashboard") {
                            TutorDashboardScreen(navController)
                        }
                        // 课时列表
                        composable("lessons/{courseId}") { backStackEntry ->
                            val cid = backStackEntry.arguments?.getString("courseId") ?: return@composable
                            LessonListScreen(navController = navController, courseId = cid)
                        }
                        // 课时详情
                        composable("lessonDetail/{lessonId}/{courseId}") { backStackEntry ->
                            val lid = backStackEntry.arguments?.getString("lessonId") ?: return@composable
                            val cid = backStackEntry.arguments?.getString("courseId") ?: return@composable
                            LessonDetailScreen(
                                navController = navController,
                                lessonId      = lid,
                                courseId      = cid
                            )
                        }
                        composable("userApproval") {
                            TutorUserApprovalScreen()
                        }
                        composable("profile") {
                            ProfileScreen(onBack = { navController.popBackStack() })
                        }


                    }
                }
            }
        }
    }
}

/* 以下演示 UI，不影响功能 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PersonalTutorAppTheme {
        Greeting("Android")
    }
}
