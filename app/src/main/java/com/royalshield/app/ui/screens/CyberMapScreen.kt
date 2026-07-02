package com.royalshield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.royalshield.app.models.ThreatType
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.viewmodels.MapViewModel

@Composable
fun CyberMapScreen(onBack: () -> Unit = {}) {
    val mapViewModel: MapViewModel = viewModel()
    val threats by mapViewModel.threats.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = MapStyleOptions(
                    // Highly customized "Cyber" style: Black background, Neon Cyan labels
                    """
                    [
                      { "elementType": "geometry", "stylers": [{"color": "#000000"}] },
                      { "elementType": "labels.text.fill", "stylers": [{"color": "#00E5FF"}] },
                      { "elementType": "labels.text.stroke", "stylers": [{"color": "#000000"}] },
                      { "featureType": "road", "elementType": "geometry", "stylers": [{"color": "#1A1A1A"}] },
                      { "featureType": "water", "elementType": "geometry", "stylers": [{"color": "#002020"}] },
                      { "featureType": "transit", "stylers": [{"visibility": "off"}] },
                      { "featureType": "poi", "stylers": [{"visibility": "off"}] },
                      { "featureType": "administrative", "elementType": "geometry", "stylers": [{"color": "#333333"}] }
                    ]
                    """.trimIndent()
                ),
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                myLocationButtonEnabled = false
            )
        ) {
            threats.forEach { threat ->
                Marker(
                    state = MarkerState(position = LatLng(threat.lat, threat.lon)),
                    title = threat.label,
                    snippet = "Security Alert: ${threat.type}",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when (threat.type) {
                            ThreatType.THEFT -> BitmapDescriptorFactory.HUE_RED
                            ThreatType.MANIFESTATION -> BitmapDescriptorFactory.HUE_ORANGE
                            ThreatType.SUSPICIOUS -> BitmapDescriptorFactory.HUE_YELLOW
                        }
                    )
                )
            }
        }

        // Header Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "GLOBAL THREAT RADAR",
                        color = RoyalGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "OTX REAL-TIME FEED",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isLoading) {
                    CircularProgressIndicator(
                        color = RoyalGold,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    // Manual Refresh Button if needed could go here
                }
            }
        }
        
        // Footer Legend
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LegendItem(Color.Red, "Critical / Theft")
                LegendItem(Color(0xFFFFA500), "Warning / Mob")
                LegendItem(Color.Yellow, "Suspicious Activity")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}
