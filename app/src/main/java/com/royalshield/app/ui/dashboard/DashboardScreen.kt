






package com.royalshield.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.ui.dashboard.components.*
import com.royalshield.app.ui.dashboard.state.DashboardState

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    DashboardContent(
        state = state,
        selectedNavIndex = selectedNavIndex,
        onNavItemSelected = { selectedNavIndex = it },
        onThreatClick = { viewModel.onThreatSelected(it.id) },
        onTimeFilterClick = { viewModel.onTimeFilterSelected(it) }
    )
}

@Composable
fun DashboardContent(
    state: DashboardState,
    selectedNavIndex: Int,
    onNavItemSelected: (Int) -> Unit,
    onThreatClick: (com.royalshield.app.ui.dashboard.models.ThreatEvent) -> Unit,
    onTimeFilterClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050508)) // Deep black/dark bg
    ) {
        // 1. Navigation Rail (Fixed on left)
        DashboardNavRail(
            selectedItem = selectedNavIndex,
            onItemSelected = onNavItemSelected
        )

        // 2. Main Content Area
        BoxWithConstraints(
            modifier = Modifier.weight(1f)
        ) {
            val isWideScreen = maxWidth > 800.dp
            
            if (isWideScreen) {
                // Tablet / Desktop Layout (3 Columns)
                WideDashboardLayout(
                    state = state,
                    onThreatClick = onThreatClick
                )
            } else {
                // Mobile Layout (Vertical Stack)
                MobileDashboardLayout(
                    state = state,
                    onThreatClick = onThreatClick
                )
            }
        }
    }
}

@Composable
fun WideDashboardLayout(
    state: DashboardState,
    onThreatClick: (com.royalshield.app.ui.dashboard.models.ThreatEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Main Row (KPI + Radar + Actions)
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel: KPIs
            Column(
                modifier = Modifier.width(280.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SystemThreatStatusCard(
                    activeThreats = 14,
                    graphData = listOf(0.2f, 0.5f, 0.4f, 0.8f, 0.6f, 0.9f, 0.7f)
                )
                
                KpiPanel(
                    metrics = state.metrics,
                    connectors = state.connectors,
                    modifier = Modifier.weight(1f)
                )
            }

            // Center: Radar
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Header (e.g. "Live Threat Map")
                Text("Live Threat Map", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                RadarChart(
                    threats = state.threatEvents,
                    selectedThreatId = state.selectedThreatId,
                    onThreatClick = onThreatClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F0F13), MaterialTheme.shapes.medium)
                )
            }

            // Right Panel: Action Items & Global Map
            Column(
                modifier = Modifier.width(320.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlobalThreatMapCard()
                
                ActionItemsPanel(
                    items = state.actionItems,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom: Timeline
        TimelineBarRow(
            data = state.timelineData,
            modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
fun MobileDashboardLayout(
    state: DashboardState,
    onThreatClick: (com.royalshield.app.ui.dashboard.models.ThreatEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Radar comes first on mobile for visual impact? Or KPIs?
        // Let's do KPIs first for summary.
        SystemThreatStatusCard(
            activeThreats = 14,
            graphData = listOf(0.2f, 0.5f, 0.4f, 0.8f, 0.6f, 0.9f, 0.7f),
            modifier = Modifier.fillMaxWidth()
        )

        KpiPanel(
            metrics = state.metrics,
            connectors = state.connectors,
            modifier = Modifier.fillMaxWidth()
        )

        GlobalThreatMapCard(modifier = Modifier.fillMaxWidth())

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp) // Fixed height for radar on mobile
                .background(Color(0xFF0F0F13), MaterialTheme.shapes.medium)
        ) {
            RadarChart(
                threats = state.threatEvents,
                selectedThreatId = state.selectedThreatId,
                onThreatClick = onThreatClick,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        TimelineBarRow(
            data = state.timelineData,
            modifier = Modifier.fillMaxWidth()
        )

        ActionItemsPanel(
            items = state.actionItems,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp) // Fixed height for scrollable list inside
        )
    }
}

@Preview(device = Devices.TABLET, widthDp = 1200, heightDp = 800)
@Composable
fun WideDashboardPreview() {
    MaterialTheme {
        DashboardScreen()
    }
}

@Preview(device = Devices.PHONE)
@Composable
fun MobileDashboardPreview() {
    MaterialTheme {
        DashboardScreen()
    }
}
