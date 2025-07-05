package com.example.fastped

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.fastped.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repo = com.example.fastped.AuthRepository()

    // null = no intento aún; success(uid) = ok; failure(e) = error
    private val _authState = MutableStateFlow<Result<String>?>(null)
    val authState: StateFlow<Result<String>?> = _authState

    fun login(dni: String, pin: String) {
        viewModelScope.launch {
            _authState.value = null                // resetea
            _authState.value = repo.login(dni, pin)
        }
    }

    fun logout() = repo.logout()
    fun currentUid() = repo.currentUid()
}
