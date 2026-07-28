package com.royalshield.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.R
import com.royalshield.app.auth.maskPhone
import com.royalshield.app.ui.components.RoyalGoldButton
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.viewmodels.PhoneAuthState
import com.royalshield.app.ui.viewmodels.PhoneAuthViewModel

@Composable
fun PhoneAuthScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: PhoneAuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val resendSeconds by viewModel.resendSeconds.collectAsState()

    var fullName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var verificationCode by rememberSaveable { mutableStateOf("") }
    var codeRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            PhoneAuthState.CodeSent -> {
                codeRequested = true
                Toast.makeText(context, "Verification code sent", Toast.LENGTH_SHORT).show()
            }
            PhoneAuthState.Success -> {
                Toast.makeText(context, "Phone verified", Toast.LENGTH_SHORT).show()
                onAuthSuccess()
            }
            is PhoneAuthState.Error ->
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.bg_google_login),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.92f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "Phone Verification",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                Icons.Default.Phone,
                contentDescription = null,
                tint = RoyalGold,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                if (codeRequested) "Enter the 6-digit code" else "Create Account with Phone",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                if (codeRequested) {
                    "Sent to ${maskPhone(phoneNumber)}"
                } else {
                    "Use your full international number. Firebase will send a secure verification code."
                },
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (!codeRequested) {
                AuthTextField(
                    value = fullName,
                    onValueChange = { fullName = it.take(60) },
                    label = "Full name",
                    keyboardType = KeyboardType.Text,
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = RoyalGold)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.take(25) },
                    label = "Phone number (+13055551234)",
                    keyboardType = KeyboardType.Phone,
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = RoyalGold)
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                RoyalGoldButton(
                    text = "SEND CODE",
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState == PhoneAuthState.Loading,
                    onClick = {
                        if (activity == null) {
                            Toast.makeText(
                                context,
                                "Phone verification requires an Android activity",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            viewModel.sendVerificationCode(fullName, phoneNumber, activity)
                        }
                    }
                )
            } else {
                AuthTextField(
                    value = verificationCode,
                    onValueChange = {
                        verificationCode = it.filter(Char::isDigit).take(6)
                    },
                    label = "Verification code",
                    keyboardType = KeyboardType.NumberPassword,
                    leadingIcon = {
                        Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalGold)
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                RoyalGoldButton(
                    text = "VERIFY & CONTINUE",
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState == PhoneAuthState.Loading,
                    onClick = { viewModel.verifyCode(verificationCode) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    enabled = resendSeconds == 0 && uiState != PhoneAuthState.Loading,
                    onClick = {
                        if (activity != null) viewModel.resendVerificationCode(activity)
                    }
                ) {
                    Text(
                        if (resendSeconds > 0) {
                            "Resend code in ${resendSeconds}s"
                        } else {
                            "Resend verification code"
                        },
                        color = if (resendSeconds == 0) RoyalGold else Color.Gray
                    )
                }
            }

            if (uiState is PhoneAuthState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    (uiState as PhoneAuthState.Error).message,
                    color = Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        }

        if (uiState == PhoneAuthState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RoyalGold)
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    leadingIcon: @Composable (() -> Unit)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = RoyalGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        leadingIcon = leadingIcon
    )
}
