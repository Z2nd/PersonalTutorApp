package uk.ac.soton.personal_tutor_app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import uk.ac.soton.personal_tutor_app.ui.auth.LoginScreen
import uk.ac.soton.personal_tutor_app.ui.auth.RegisterScreen
import uk.ac.soton.personal_tutor_app.ui.home.HomeScreen
import uk.ac.soton.personal_tutor_app.ui.course.CourseListScreen
import uk.ac.soton.personal_tutor_app.ui.course.CourseDetailScreen
import uk.ac.soton.personal_tutor_app.ui.dashboard.DashboardScreen
import uk.ac.soton.personal_tutor_app.ui.profile.ProfileScreen
import uk.ac.soton.personal_tutor_app.ui.tutor.TutorUserApprovalScreen
import uk.ac.soton.personal_tutor_app.ui.tutor.EnrollApprovalScreen
import uk.ac.soton.personal_tutor_app.ui.lesson.LessonListScreen
import uk.ac.soton.personal_tutor_app.ui.lesson.LessonDetailScreen
import uk.ac.soton.personal_tutor_app.ui.theme.PersonalTutorAppTheme
import uk.ac.soton.personal_tutor_app.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        firebaseAuth.addAuthStateListener { auth ->
            Log.d("FIREBASE_TEST", "currentUser=${auth.currentUser?.uid}")
        }

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

        setContent {
            PersonalTutorAppTheme {
                MaterialTheme {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val uiState by authViewModel.uiState.collectAsState()

                    NavHost(navController = navController, startDestination = "login") {
                        // 登录
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
                        // 注册
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
                            HomeScreen(
                                userEmail = uiState.email,
                                userRole = uiState.role,
                                onLogout = {
                                    authViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onViewCourses = { navController.navigate("courses") },
                                onNavigateProfile = { navController.navigate("profile") },
                                onNavigateUserApproval = { navController.navigate("userApproval") },
                                        onNavigateDashboard    = { navController.navigate("dashboard") }
                            )
                        }
                        // 课程列表
                        composable("courses") {
                            CourseListScreen(
                                navController = navController,
                                userRole = uiState.role
                            )
                        }
                        // 课程详情
                        composable(
                            "courseDetail/{courseId}/{userRole}",
                            arguments = listOf(
                                navArgument("courseId") { type = NavType.StringType },
                                navArgument("userRole") { type = NavType.StringType }
                            )
                        ) { back ->
                            val cid = back.arguments!!.getString("courseId")!!
                            val role = back.arguments!!.getString("userRole")!!
                            CourseDetailScreen(
                                navController = navController,
                                courseId = cid,
                                userRole = role
                            )
                        }
                        // 学员注册审批
                        composable("userApproval") {
                            TutorUserApprovalScreen()
                        }
                        // 某门课程的报名审批（Tutor）
                        composable(
                            "enrollApproval/{courseId}",
                            arguments = listOf(
                                navArgument("courseId") { type = NavType.StringType }
                            )
                        ) { back ->
                            val cid = back.arguments!!.getString("courseId")!!
                            EnrollApprovalScreen(courseId = cid)
                        }
                        // 课时列表

                        composable(
                            "lessons/{courseId}",
                            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                        ) { back ->
                            val cid = back.arguments!!.getString("courseId")!!
                            val isTutor = uiState.role == "Tutor"
                            LessonListScreen(navController, cid, isTutor)
                        }


                        // 课时编辑/详情（Tutor）
                        composable(
                            "lessonDetail/{lessonId}/{courseId}",
                            arguments = listOf(
                                navArgument("lessonId") { type = NavType.StringType },
                                navArgument("courseId") { type = NavType.StringType }
                            )
                        ) { back ->
                            val lid = back.arguments!!.getString("lessonId")!!
                            val cid = back.arguments!!.getString("courseId")!!
                            // 根据当前登录状态再判断 isTutor
                            val isTutor = uiState.role == "Tutor"
                            LessonDetailScreen(
                                navController = navController,
                                lessonId      = lid,
                                courseId      = cid,
                                isTutor       = isTutor
                            )
                        }


                        // 课时内容（只读 Student）
                        composable(
                            "lessonContent/{lessonId}/{courseId}",
                            arguments = listOf(
                                navArgument("lessonId") { type = NavType.StringType },
                                navArgument("courseId") { type = NavType.StringType }
                            )
                        ) { back ->
                            val lid = back.arguments!!.getString("lessonId")!!
                            val cid = back.arguments!!.getString("courseId")!!
                            LessonDetailScreen(
                                navController = navController,
                                lessonId = lid,
                                courseId = cid,
                                isTutor = false
                            )
                        }
                        // 我的资料
                        composable("profile") {
                            ProfileScreen(onBack = { navController.popBackStack() })
                        }
                        composable("dashboard") {
                            DashboardScreen(navController)
                        }
                    }
                }
            }
        }
    }
}
