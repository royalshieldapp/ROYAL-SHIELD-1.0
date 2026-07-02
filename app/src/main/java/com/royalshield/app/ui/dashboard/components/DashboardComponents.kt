package com.royalshield.app.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.royalshield.app.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.dashboard.models.Connector
import com.royalshield.app.ui.dashboard.models.KpiMetrics
import com.royalshield.app.ui.dashboard.models.Severity

@Composable
fun DashboardNavRail(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationRail(
        containerColor = Color(0xFF0F0F13), 
        contentColor = Color.White
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Logo",
            tint = Color(0xFF6200EA),
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(32.dp))
        
        val items = listOf(
            Icons.Default.Dashboard,
            Icons.Default.Search, // Fixed icon
            Icons.AutoMirrored.Filled.List,
            Icons.Default.Settings
        )
        
        items.forEachIndexed { index, icon ->
            NavigationRailItem(
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                icon = { Icon(imageVector = icon, contentDescription = null) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color(0xFF6200EA),
                    indicatorColor = Color(0xFF1E1E24),
                    unselectedIconColor = Color.Gray
                )
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun KpiPanel(
    metrics: KpiMetrics,
    connectors: List<Connector>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Investigations Card
        KpiCard(title = "Investigations") {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${metrics.investigationsTotal}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Total", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SevIndicator("Low", metrics.lowSeverity, Severity.LOW.color)
                SevIndicator("Med", metrics.mediumSeverity, Severity.MEDIUM.color)
                SevIndicator("High", metrics.highSeverity, Severity.HIGH.color)
            }
        }

        // Exposed Entities Card
        KpiCard(title = "Exposed Entities") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${metrics.exposedEntities}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(16.dp))
                if (metrics.entitiesDelta > 0) {
                    Text(
                        text = "+${metrics.entitiesDelta} last 24h",
                        color = Severity.HIGH.color,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Connectors Card
        KpiCard(title = "Connectors") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                connectors.forEach { connector ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (connector.isActive) Color.Green else Color.Gray)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = connector.name,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun SevIndicator(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), color = Color.White, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun TimelineBarRow(
    data: List<Float>, 
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF0F0F13))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        data.forEach { value ->
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight(value.coerceAtLeast(0.1f)) // Min height
                    .background(Color(0xFF2A2A35), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun SystemThreatStatusCard(
    activeThreats: Int,
    graphData: List<Float>,
    modifier: Modifier = Modifier
) {
    KpiCard(title = "System Threat Status", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACTIVE THREATS",
                    color = Color(0xFF00E676),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$activeThreats",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Simple Line Graph using Canvas
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(60.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = androidx.compose.ui.graphics.Path()
                    val width = size.width
                    val height = size.height
                    val step = width / (graphData.size - 1)
                    
                    graphData.forEachIndexed { index, value ->
                        val x = index * step
                        val y = height - (value * height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    
                    drawPath(
                        path = path,
                        color = Color(0xFF6200EA),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                    
                    // Add glow effect
                    drawPath(
                        path = path,
                        color = Color(0xFF6200EA).copy(alpha = 0.3f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx())
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "NEURAL NETWORK MONITOR",
            color = Color.Gray.copy(alpha = 0.5f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun GlobalThreatMapCard(
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F13)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Premium Background with World Map
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_cyber_globe),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.6f)
                )
                
                // Technical Grid Effect
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 20.dp.toPx()
                    for (x in 0..(size.width / step).toInt()) {
                        drawLine(Color.White.copy(alpha = 0.05f), Offset(x * step, 0f), Offset(x * step, size.height))
                    }
                    for (y in 0..(size.height / step).toInt()) {
                        drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y * step), Offset(size.width, y * step))
                    }
                }

                // Threat Dots & Connections
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val points = listOf(
                        Offset(width * 0.2f, height * 0.4f),
                        Offset(width * 0.5f, height * 0.3f),
                        Offset(width * 0.8f, height * 0.5f),
                        Offset(width * 0.7f, height * 0.7f),
                        Offset(width * 0.3f, height * 0.8f)
                    )
                    
                    points.forEach { point ->
                        drawCircle(Color.Red.copy(alpha = 0.8f), 3.dp.toPx(), point)
                        drawCircle(Color.Red.copy(alpha = 0.2f), 8.dp.toPx(), point)
                    }
                    
                    for (i in 0 until points.size - 1) {
                        if (i % 2 == 0) {
                            drawLine(Color(0xFF6200EA).copy(alpha = 0.3f), points[i], points[i+1], 1.dp.toPx())
                        }
                    }
                }
                
                // Pulsing Scan Line
                // (Adding a static scan line for now to keep it simple)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF00E676).copy(alpha = 0.5f))
                        .align(Alignment.Center)
                )

                Text(
                    "LIVE THREAT FEED: ACTIVE",
                    color = Color(0xFF00E676),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                )
                
                Column(modifier = Modifier.padding(16.dp).align(Alignment.TopStart)) {
                    Text(
                        "GLOBAL THREAT MAP",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
