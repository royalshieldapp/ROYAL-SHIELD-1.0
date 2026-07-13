package com.royalshield.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.ui.components.RoyalGradientBackground
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val user = remember { FirebaseAuth.getInstance().currentUser }
    var displayName by remember { mutableStateOf(user?.displayName.orEmpty()) }
    var phone by remember { mutableStateOf(user?.phoneNumber.orEmpty()) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        if (displayName.isBlank()) {
            displayName = PreferencesManager.getUserName().orEmpty()
        }
    }

    RoyalGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Profile", color = RoyalGold, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = RoyalGold)
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, "Settings", tint = RoyalGold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = RoyalGold,
                    modifier = Modifier
                        .size(92.dp)
                        .border(1.5.dp, RoyalGold, CircleShape)
                        .padding(10.dp)
                )
                Text(user?.email ?: "No authenticated account", color = Color.LightGray, fontSize = 13.sp)

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(60) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = profileFieldColors()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(25) },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = profileFieldColors()
                )

                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            try {
                                user?.let { currentUser ->
                                    currentUser.updateProfile(
                                        UserProfileChangeRequest.Builder()
                                            .setDisplayName(displayName.trim())
                                            .build()
                                    ).await()
                                    FirebaseDatabase.getInstance().reference
                                        .child("users").child(currentUser.uid).child("profile")
                                        .updateChildren(mapOf("name" to displayName.trim(), "phone" to phone.trim(), "email" to (currentUser.email ?: "")))
                                        .await()
                                    PreferencesManager.saveUserName(displayName.trim())
                                    Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not save profile", Toast.LENGTH_SHORT).show()
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving && user != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = Color.Black)
                ) {
                    Text(if (saving) "Saving..." else "Save changes", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, null, tint = RoyalGold)
                    Spacer(Modifier.size(8.dp))
                    Text("Security and app settings", color = RoyalGold)
                }
                TextButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                    onSignedOut()
                }) {
                    Icon(Icons.Default.Logout, null, tint = Color(0xFFFF6B6B))
                    Spacer(Modifier.size(8.dp))
                    Text("Sign out", color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}

@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = RoyalGold,
    unfocusedLabelColor = Color.LightGray,
    focusedBorderColor = RoyalGold,
    unfocusedBorderColor = RoyalGold.copy(alpha = 0.45f),
    cursorColor = RoyalGold
)
