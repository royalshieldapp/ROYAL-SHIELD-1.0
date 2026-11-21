package com.royalshield.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.royalshield.app.ui.components.RoyalGradientBackground
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignedOut: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by profileViewModel.uiState.collectAsState()
    val profile = state.profile

    LaunchedEffect(state.errorMessage, state.successMessage) {
        val message = state.errorMessage ?: state.successMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        profileViewModel.consumeMessage()
    }

    RoyalGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Perfil", color = RoyalGold, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = RoyalGold) } },
                    actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Configuración", tint = RoyalGold) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (profile.photoUrl.isNullOrBlank()) {
                    Icon(Icons.Default.AccountCircle, "Foto de perfil", tint = RoyalGold, modifier = Modifier.size(92.dp).border(1.5.dp, RoyalGold, CircleShape).padding(10.dp))
                } else {
                    AsyncImage(model = profile.photoUrl, contentDescription = "Foto de perfil", modifier = Modifier.size(92.dp).clip(CircleShape).border(1.5.dp, RoyalGold, CircleShape))
                }
                Text(profile.email.ifBlank { "Cuenta no autenticada" }, color = Color.LightGray, fontSize = 13.sp)

                if (state.isLoading) {
                    CircularProgressIndicator(color = RoyalGold)
                } else {
                    ProfileField(profile.displayName, { value -> profileViewModel.updateProfile { it.copy(displayName = value.take(60)) } }, "Nombre completo")
                    ProfileField(profile.phone, { value -> profileViewModel.updateProfile { it.copy(phone = value.take(25)) } }, "Teléfono", KeyboardType.Phone)
                    ProfileField(profile.city, { value -> profileViewModel.updateProfile { it.copy(city = value.take(80)) } }, "Ciudad")
                    ProfileField(profile.country, { value -> profileViewModel.updateProfile { it.copy(country = value.take(80)) } }, "País")
                    ProfileField(profile.emergencyPhone, { value -> profileViewModel.updateProfile { it.copy(emergencyPhone = value.take(25)) } }, "Teléfono principal de emergencia", KeyboardType.Phone)

                    Text("El teléfono de emergencia se cifra y permanece en este dispositivo.", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = profileViewModel::saveProfile,
                        enabled = !state.isSaving && state.isAuthenticated,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = Color.Black)
                    ) { Text(if (state.isSaving) "Guardando..." else "Guardar cambios", fontWeight = FontWeight.Bold) }
                }

                TextButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, null, tint = RoyalGold)
                    Spacer(Modifier.size(8.dp))
                    Text("Seguridad, contactos y configuración", color = RoyalGold)
                }
                TextButton(enabled = state.isAuthenticated, onClick = {
                    profileViewModel.signOut()
                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    onSignedOut()
                }) {
                    Icon(Icons.Default.Logout, null, tint = Color(0xFFFF6B6B))
                    Spacer(Modifier.size(8.dp))
                    Text("Cerrar sesión", color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}

@Composable
private fun ProfileField(value: String, onValueChange: (String) -> Unit, label: String, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = RoyalGold,
            unfocusedLabelColor = Color.LightGray,
            focusedBorderColor = RoyalGold,
            unfocusedBorderColor = RoyalGold.copy(alpha = 0.45f),
            cursorColor = RoyalGold
        )
    )
}
