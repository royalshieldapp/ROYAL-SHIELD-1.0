package com.royalshield.app.features.threatmap

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.royalshield.app.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ThreatMapEvent(
    @Json(name = "sourceCity") val sourceCity: String,
    @Json(name = "sourceLat") val sourceLat: Double,
    @Json(name = "sourceLng") val sourceLng: Double,
    @Json(name = "targetCity") val targetCity: String,
    @Json(name = "targetLat") val targetLat: Double,
    @Json(name = "targetLng") val targetLng: Double,
    @Json(name = "severity") val severity: String,
    @Json(name = "threatType") val threatType: String,
    @Json(name = "sourceModule") val sourceModule: String,
    @Json(name = "observedAt") val observedAt: String,
)

data class ThreatMapUiState(
    val events: List<ThreatMapEvent> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val alertCount: Int
        get() = events.count { it.severity.lowercase() in setOf("high", "critical") }

    val nodeCount: Int
        get() = events.flatMap { listOf(it.sourceCity, it.targetCity) }.distinct().size

    val primaryTarget: String
        get() = events.groupingBy { it.targetCity }.eachCount().maxByOrNull { it.value }?.key ?: "None"
}

private interface ThreatMapApi {
    @GET("api/threat-map/events")
    suspend fun getEvents(@Query("limit") limit: Int = 100): List<ThreatMapEvent>
}

private object ThreatMapRepository {
    private val api: ThreatMapApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ThreatMapApi::class.java)
    }

    suspend fun getEvents(): List<ThreatMapEvent> = api.getEvents()
}

class ThreatMapViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ThreatMapUiState())
    val uiState: StateFlow<ThreatMapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(30_000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { ThreatMapRepository.getEvents() }
                .onSuccess { events ->
                    _uiState.value = ThreatMapUiState(events = events, isLoading = false)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Live threat signals are temporarily unavailable",
                    )
                }
        }
    }
}

@Composable
fun LiveCyberThreatMap(
    state: ThreatMapUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraState = rememberCameraPositionState()
    val transition = rememberInfiniteTransition(label = "threat-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1_300), RepeatMode.Reverse),
        label = "threat-pulse-alpha",
    )

    LaunchedEffect(state.events) {
        val first = state.events.firstOrNull() ?: return@LaunchedEffect
        cameraState.animate(
            CameraUpdateFactory.newLatLngZoom(LatLng(first.targetLat, first.targetLng), 2.2f),
            900,
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF080B0E))
            .border(1.dp, Color(0xFF35D7FF).copy(alpha = 0.38f), RoundedCornerShape(20.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("LIVE SATELLITE CYBER MAP", color = Color(0xFF35D7FF), fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text("Verified Royal Shield signals", color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp)
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp, color = Color(0xFF35D7FF))
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh live signals", tint = Color(0xFF35D7FF))
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(mapType = MapType.SATELLITE),
                uiSettings = MapUiSettings(
                    compassEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                ),
            ) {
                state.events.forEach { event ->
                    val source = LatLng(event.sourceLat, event.sourceLng)
                    val target = LatLng(event.targetLat, event.targetLng)
                    val color = severityColor(event.severity)
                    Polyline(points = listOf(source, target), color = color.copy(alpha = 0.78f), width = 4f)
                    Circle(center = source, radius = 35_000.0, fillColor = color.copy(alpha = pulse * 0.20f), strokeColor = color.copy(alpha = pulse), strokeWidth = 2f)
                    Circle(center = target, radius = 55_000.0, fillColor = color.copy(alpha = pulse * 0.24f), strokeColor = color, strokeWidth = 3f)
                }
            }

            if (!state.isLoading && state.events.isEmpty()) {
                MapMessage("No verified threat events observed", Modifier.align(Alignment.Center))
            } else if (state.errorMessage != null) {
                MapMessage(state.errorMessage, Modifier.align(Alignment.Center))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MapStat("EVENTS", state.events.size.toString())
            MapStat("ALERTS", state.alertCount.toString())
            MapStat("NODES", state.nodeCount.toString())
        }
    }
}

@Composable
private fun MapMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = Color.White,
        fontSize = 12.sp,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun MapStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color(0xFF35D7FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun severityColor(severity: String): Color = when (severity.lowercase()) {
    "critical", "high" -> Color(0xFFFF3157)
    "medium" -> Color(0xFFFF9D2E)
    else -> Color(0xFF35D7FF)
}
