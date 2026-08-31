package com.royalshield.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.securitysetup.SecurityFeature
import com.royalshield.app.securitysetup.SecuritySetupSession
import com.royalshield.app.securitysetup.SecuritySetupStore
import com.royalshield.app.ui.components.RoyalSecurityOrb
import com.royalshield.app.ui.components.RoyalSecurityOrbState
import com.royalshield.app.ui.theme.RoyalGold

@Composable
fun SecuritySetupScreen(
    uid: String,
    store: SecuritySetupStore,
    onComplete: () -> Unit,
) {
    val session = remember(uid) { SecuritySetupSession(store.load(uid)) }
    var currentFeature by remember(uid) { mutableStateOf(session.currentFeature) }
    var showSmsDisclosure by remember { mutableStateOf(false) }

    fun recordChoice(enabled: Boolean) {
        val feature = currentFeature ?: return
        store.saveChoice(uid, feature, enabled)
        val completed = session.answer(enabled)
        currentFeature = session.currentFeature
        if (completed) {
            store.markCompleted(uid)
            onComplete()
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val required = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) add(Manifest.permission.READ_SMS)
        }
        recordChoice(required.all { grants[it] == true })
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> recordChoice(granted) }

    if (showSmsDisclosure) {
        AlertDialog(
            onDismissRequest = { showSmsDisclosure = false },
            title = { Text("Protección automática contra phishing por SMS") },
            text = {
                Text(
                    "Royal Shield accederá a los SMS recibidos para identificar enlaces y patrones sospechosos. " +
                        "El mensaje no se usará para publicidad ni se venderá. Solo una URL extraída podrá enviarse " +
                        "al análisis avanzado de VirusTotal. Puedes revocar el permiso cuando quieras."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSmsDisclosure = false
                    smsPermissionLauncher.launch(
                        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                    )
                }) { Text("Aceptar y continuar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSmsDisclosure = false
                    recordChoice(false)
                }) { Text("Ahora no") }
            },
        )
    }

    val copy = when (currentFeature) {
        SecurityFeature.VPN -> Triple(
            "¿Quieres configurar la VPN?",
            "Ayuda a proteger el tráfico compatible, especialmente en Wi-Fi público.",
            "No garantiza anonimato total ni elimina todos los riesgos.",
        )
        SecurityFeature.SMS_PHISHING -> Triple(
            "¿Quieres activar la protección automática contra phishing por SMS?",
            "Analiza enlaces y patrones sospechosos para advertirte antes de abrirlos.",
            "Requiere acceso a SMS y la aprobación correspondiente de Google Play.",
        )
        SecurityFeature.QR_SCANNER -> Triple(
            "¿Quieres activar el escáner seguro de códigos QR?",
            "Permite revisar el destino de un QR antes de abrirlo.",
            "Ningún análisis puede garantizar que todos los destinos sean seguros.",
        )
        null -> Triple("Configuración completa", "", "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF17120A))))
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RoyalSecurityOrb(
            state = RoyalSecurityOrbState.SETUP,
            modifier = Modifier.size(190.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text("ROYAL SECURITY AGENT", color = RoyalGold, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(20.dp))
        Text(copy.first, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(copy.second, color = Color.White.copy(alpha = 0.88f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(copy.third, color = Color.White.copy(alpha = 0.62f), fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { recordChoice(false) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
            ) { Text("Ahora no") }
            Button(
                onClick = {
                    when (currentFeature) {
                        SecurityFeature.SMS_PHISHING -> showSmsDisclosure = true
                        SecurityFeature.QR_SCANNER -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        SecurityFeature.VPN -> recordChoice(true)
                        null -> Unit
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = Color.Black),
                shape = RoundedCornerShape(24.dp),
            ) { Text("Activar", fontWeight = FontWeight.Bold) }
        }
    }
}
