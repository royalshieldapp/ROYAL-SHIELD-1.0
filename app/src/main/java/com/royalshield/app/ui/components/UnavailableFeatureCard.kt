package com.royalshield.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.data.AvailabilityState
import com.royalshield.app.ui.theme.RoyalGold

fun executeWhenReady(state: AvailabilityState, action: () -> Unit): Boolean {
    if (state !is AvailabilityState.Ready) return false
    action()
    return true
}

fun <T> verifiedItems(state: AvailabilityState, items: List<T>): List<T> =
    if (state is AvailabilityState.Ready) items else emptyList()

@Composable
fun FeatureAvailabilityGate(
    moduleName: String,
    state: AvailabilityState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (state is AvailabilityState.Ready) {
        content()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RoyalGold)
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            UnavailableFeatureCard(
                moduleName = moduleName,
                state = state,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
fun UnavailableFeatureCard(
    moduleName: String,
    state: AvailabilityState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDegraded = state is AvailabilityState.Degraded
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, RoyalGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null, tint = RoyalGold)
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isDegraded) "DISPONIBILIDAD LIMITADA" else "NO DISPONIBLE",
                color = RoyalGold,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = unavailableMessage(moduleName),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Royal Shield no ejecutará ni mostrará resultados hasta verificar la fuente y la operación completa.",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 13.sp,
            )
            state.dataFreshness?.let { freshness ->
                Spacer(Modifier.height(8.dp))
                Text("Última evidencia: $freshness", color = Color.Gray, fontSize = 11.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("Reintentar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun unavailableMessage(moduleName: String): String = when (moduleName) {
    "VPN" -> "VPN no disponible temporalmente"
    "Forecast Engine" -> "Modelo en preparación — predicciones no disponibles"
    "Threat Map" -> "Inteligencia de amenazas no verificada"
    "Automations" -> "Automatizaciones: próximamente"
    "Reports" -> "Reportes avanzados no disponibles"
    "Policies" -> "Aplicación de políticas no verificada"
    else -> "$moduleName no está disponible temporalmente"
}
