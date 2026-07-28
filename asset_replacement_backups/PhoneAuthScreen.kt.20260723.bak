package com.royalshield.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.ui.theme.*
import com.royalshield.app.ui.viewmodels.PhoneAuthState
import com.royalshield.app.ui.viewmodels.PhoneAuthViewModel
import com.royalshield.app.ui.components.RoyalGoldButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: PhoneAuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState) {
        when (uiState) {
            is PhoneAuthState.Success -> {
                Toast.makeText(context, "Verification Successful!", Toast.LENGTH_SHORT).show()
                onAuthSuccess()
            }
            is PhoneAuthState.Error -> {
                Toast.makeText(context, (uiState as PhoneAuthState.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.bg_google_login),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.9f))
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
                text = if (uiState is PhoneAuthState.CodeSent) "Enter the 6-digit code" else "Secure Login with Phone",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (uiState is PhoneAuthState.CodeSent) 
                    "Sent to $phoneNumber" else "We'll send you a verification code to ensure it's you.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState !is PhoneAuthState.CodeSent) {
                // Phone Number Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (+1...)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RoyalGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = RoyalGold) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                RoyalGoldButton(
                    text = "SEND CODE",
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState is PhoneAuthState.Loading,
                    onClick = {
                        if (phoneNumber.isNotBlank()) {
                            viewModel.sendVerificationCode(phoneNumber, context as Activity)
                        } else {
                            Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                // Verification Code Input
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { if (it.length <= 6) verificationCode = it },
                    label = { Text("Verification Code", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RoyalGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Pin, null, tint = RoyalGold) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                RoyalGoldButton(
                    text = "VERIFY & CONTINUE",
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState is PhoneAuthState.Loading,
                    onClick = {
                        if (verificationCode.length == 6) {
                            viewModel.verifyCode(verificationCode)
                        } else {
                            Toast.makeText(context, "Please enter the 6-digit code", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                TextButton(onClick = { /* Implement Resend */ }) {
                    Text("Didn't receive code? Resend", color = RoyalGold)
                }
            }
        }
    }
}
