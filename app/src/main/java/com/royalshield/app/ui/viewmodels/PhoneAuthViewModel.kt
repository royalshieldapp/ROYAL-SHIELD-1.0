package com.royalshield.app.ui.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseTooManyRequestsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.royalshield.app.auth.AuthProfileRepository
import com.royalshield.app.auth.isValidE164Phone
import com.royalshield.app.auth.isValidOtp
import com.royalshield.app.auth.normalizeE164Phone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PhoneAuthState {
    data object Idle : PhoneAuthState()
    data object Loading : PhoneAuthState()
    data object CodeSent : PhoneAuthState()
    data object Success : PhoneAuthState()
    data class Error(val message: String) : PhoneAuthState()
}

class PhoneAuthViewModel(
    private val profileRepository: AuthProfileRepository = AuthProfileRepository()
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Idle)
    val uiState = _uiState.asStateFlow()
    private val _resendSeconds = MutableStateFlow(0)
    val resendSeconds = _resendSeconds.asStateFlow()

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var pendingName = ""
    private var pendingPhone = ""
    private var resendJob: Job? = null

    fun sendVerificationCode(fullName: String, phoneNumber: String, activity: Activity) {
        if (fullName.trim().length !in 2..60) {
            _uiState.value = PhoneAuthState.Error("Enter your full name")
            return
        }
        if (!isValidE164Phone(phoneNumber)) {
            _uiState.value = PhoneAuthState.Error(
                "Use international format, for example +13055551234"
            )
            return
        }

        pendingName = fullName.trim()
        pendingPhone = normalizeE164Phone(phoneNumber)
        startVerification(activity, forceResend = false)
    }

    fun resendVerificationCode(activity: Activity) {
        if (_resendSeconds.value > 0 || pendingPhone.isBlank()) return
        startVerification(activity, forceResend = true)
    }

    private fun startVerification(activity: Activity, forceResend: Boolean) {
        _uiState.value = PhoneAuthState.Loading
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(error: FirebaseException) {
                _uiState.value = PhoneAuthState.Error(phoneAuthErrorMessage(error))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken = token
                _uiState.value = PhoneAuthState.CodeSent
                startResendCooldown()
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(pendingPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (forceResend) {
            resendToken?.let(builder::setForceResendingToken)
        }
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun verifyCode(code: String) {
        if (!isValidOtp(code)) {
            _uiState.value = PhoneAuthState.Error("Enter the complete 6-digit code")
            return
        }
        val verificationId = storedVerificationId
        if (verificationId == null) {
            _uiState.value = PhoneAuthState.Error("Request a new verification code")
            return
        }

        _uiState.value = PhoneAuthState.Loading
        signInWithPhoneAuthCredential(
            PhoneAuthProvider.getCredential(verificationId, code)
        )
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                _uiState.value = PhoneAuthState.Error(phoneAuthErrorMessage(task.exception))
                return@addOnCompleteListener
            }

            val user = task.result.user
            if (user == null) {
                _uiState.value = PhoneAuthState.Error("Firebase did not return a user")
                return@addOnCompleteListener
            }

            viewModelScope.launch {
                runCatching { profileRepository.ensureProfile(user, pendingName) }
                    .onSuccess { _uiState.value = PhoneAuthState.Success }
                    .onFailure {
                        _uiState.value = PhoneAuthState.Error(
                            "Phone verified, but the profile could not be synchronized. Check your connection and try again."
                        )
                    }
            }
        }
    }

    fun returnToCodeEntry() {
        if (storedVerificationId != null) {
            _uiState.value = PhoneAuthState.CodeSent
        }
    }

    private fun startResendCooldown() {
        resendJob?.cancel()
        resendJob = viewModelScope.launch {
            for (remaining in 60 downTo 0) {
                _resendSeconds.value = remaining
                if (remaining > 0) delay(1_000)
            }
        }
    }

    private fun phoneAuthErrorMessage(error: Throwable?): String = when (error) {
        is FirebaseAuthInvalidCredentialsException ->
            "The phone number or verification code is invalid"
        is FirebaseTooManyRequestsException ->
            "Too many attempts. Wait before requesting another code"
        else -> "Phone verification failed. Check your connection and try again"
    }
}
