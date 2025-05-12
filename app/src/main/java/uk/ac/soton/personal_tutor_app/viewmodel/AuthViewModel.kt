package uk.ac.soton.personal_tutor_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uk.ac.soton.personal_tutor_app.data.AuthRepository
import uk.ac.soton.personal_tutor_app.data.repository.UserRepository

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val role: String = "Student",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(new: String)    = _uiState.update { it.copy(email = new, errorMessage = null) }
    fun onPasswordChanged(new: String) = _uiState.update { it.copy(password = new, errorMessage = null) }
    fun onRoleChanged(new: String)     = _uiState.update { it.copy(role = new, errorMessage = null) }

    /** 注册 */
    fun register() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val email = _uiState.value.email
            val pwd   = _uiState.value.password
            val role  = _uiState.value.role

            val res = AuthRepository.register(email, pwd)
            res.fold(
                onSuccess = {
                    // 1. 创建 Firestore 用户档案，需要传入 email
                    val uid = AuthRepository.currentUserId ?: ""
                    UserRepository.createUserProfile(
                        uid   = uid,
                        email = email,
                        role  = role
                    )
                    // 2. 根据角色区分自动登录或待审核
                    if (role == "Student") {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Waiting for approval",
                                isAuthenticated = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, isAuthenticated = true)
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            )
        }
    }

    /** 登录 */
    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val email = _uiState.value.email
            val pwd   = _uiState.value.password

            val res = AuthRepository.login(email, pwd)
            res.fold(
                onSuccess = {
                    // 拉取 Firestore 档案
                    val uid = AuthRepository.currentUserId ?: ""
                    val profile = UserRepository.getUserProfile(uid)
                    val allowed = (profile.role == "Tutor") || profile.approved
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            role = profile.role,
                            isAuthenticated = allowed,
                            errorMessage = if (!allowed) "Waiting for approval" else null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            )
        }
    }

    /** 登出 */
    fun logout() {
        AuthRepository.logout()
        _uiState.update { AuthUiState() }
    }
}
