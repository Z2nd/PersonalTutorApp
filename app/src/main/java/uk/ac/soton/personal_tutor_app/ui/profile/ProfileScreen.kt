@file:OptIn(ExperimentalMaterial3Api::class)

package uk.ac.soton.personal_tutor_app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.ac.soton.personal_tutor_app.data.model.UserProfile
import uk.ac.soton.personal_tutor_app.data.repository.UserRepository
import uk.ac.soton.personal_tutor_app.viewmodel.AuthViewModel



@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // 1️⃣ 首次拉取用户档案
    LaunchedEffect(uiState.email) {
        val auth = Firebase.auth
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        isLoading = true
        try {
            val p = UserRepository.getUserProfile(uid)
            profile = p
            displayName = p.displayName
            bio = p.bio
            photoUrl = p.photoUrl
        } catch (e: Exception) {
            error = e.message
        }
        isLoading = false
    }

    // 2️⃣ 头像选择并上传
    val launcher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    val ref = Firebase.storage
                        .getReference("avatars/${FirebaseAuth.getInstance().currentUser?.uid}.jpg")
                    ref.putFile(it).await()
                    photoUrl = ref.downloadUrl.await().toString()
                } catch (e: Exception) {
                    error = "upload failed：${e.message}"
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    Modifier
                        .systemBarsPadding()
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 头像预览
                    Image(
                        painter = rememberAsyncImagePainter(photoUrl),
                        contentDescription = "Bio",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                    TextButton(onClick = { launcher.launch("image/*") }) {
                        Text("Upload Bio")
                    }
                    Spacer(Modifier.height(16.dp))

                    // 显示名称
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("User Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // 简介
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Introduction") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

                    // 保存按钮
                    Button(
                        onClick = {
                            profile?.let { p ->
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val updated = p.copy(
                                            displayName = displayName,
                                            bio = bio,
                                            photoUrl = photoUrl
                                        )
                                        UserRepository.updateUserProfile(updated)
                                        error = null
                                    } catch (e: Exception) {
                                        error = "Fail to update profile：${e.message}"
                                    }
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save")
                    }

                    // 错误提示
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
