package com.royalshield.app.features.riskprediction.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.features.riskprediction.data.remote.RiskFactor
import com.royalshield.app.features.riskprediction.data.remote.ZoneDetailsResponse
import com.royalshield.app.ui.theme.RoyalGold

@Composable
fun RiskExplanationSheet(
    zoneDetails: ZoneDetailsResponse,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFF0F172A))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(RoyalGold.copy(alpha=0.5f), Color.Transparent)),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "RISK INTELLIGENCE",
                            color = RoyalGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Zone Analysis",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // RISK SCORE CARD
            item {
                RiskScoreCard(
                    score = zoneDetails.riskScore,
                    level = zoneDetails.riskLevel,
                    trend = zoneDetails.trends.direction
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // AI EXPLANATION
            item {
                Text(
                    "AI DIAGNOSIS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Natural Language Explanation
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            Icons.Default.AutoAwesome, 
                            null, 
                            tint = RoyalGold, 
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Simulated NL explanation based on data if not provided (mock for now if field missing)
                        Text(
                            "High risk detected due to elevated violent crime reports in the last 7 days compared to 30-day average. Reduced lighting infrastructure reported in this sector.",
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // CONTRIBUTING FACTORS
            item {
                Text(
                    "CONTRIBUTING FACTORS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Mock factors derived from stats if not direct list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RiskFactorItem("Recent Crime Spike (7d)", 0.85f, true)
                    RiskFactorItem("Historical Density", 0.60f, true)
                    RiskFactorItem("Police Proximity", 0.30f, false) // False means mitigates risk
                    RiskFactorItem("Environmental Hazards", 0.10f, true)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // STATS GRID
            item {
                Text(
                    "LIVE STATISTICS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatBadge("CRIMES", zoneDetails.statistics.crimeEvents.toString(), Icons.Default.Warning)
                    StatBadge("FIRES", zoneDetails.statistics.fireEvents.toString(), Icons.Default.LocalFireDepartment)
                    StatBadge("REPORTS", (zoneDetails.statistics.osintEvents ?: 0).toString(), Icons.Default.Campaign)
                }
            }
        }
    }
}

@Composable
fun RiskScoreCard(score: Double, level: String, trend: String) {
    val color = when(level) {
        "CRITICAL" -> Color(0xFFFF3B30)
        "HIGH" -> Color(0xFFFF9800)
        "MEDIUM" -> Color(0xFFFFEB3B)
        else -> Color(0xFF00E676)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "RISK LEVEL",
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    level,
                    color = color,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if(trend == "INCREASING") Icons.Default.TrendingUp else Icons.Default.TrendingFlat,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        trend.lowercase().replaceFirstChar { it.uppercase() },
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Circular Score
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (score / 100).toFloat() },
                    modifier = Modifier.size(80.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f),
                    strokeWidth = 8.dp,
                )
                Text(
                    score.toInt().toString(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RiskFactorItem(name: String, impact: Float, isNegative: Boolean) {
    val color = if (isNegative) Color(0xFFFF3B30) else Color(0xFF00E676)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Icon(
            if (isNegative) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
            null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Color.White, fontSize = 14.sp)
        }
        Text(
            if (isNegative) "+${(impact*10).toInt()}%" else "-${(impact*10).toInt()}%",
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatBadge(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(icon, null, tint = RoyalGold, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
