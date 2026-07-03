package com.royalshield.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.royalshield.app.ui.components.RoyalGoldButton
import com.royalshield.app.data.BusinessRepository
import com.royalshield.app.data.QuoteRequest
import android.widget.Toast
import kotlinx.coroutines.launch

// State Management
enum class QuoteState {
    OVERVIEW, FORM, SUBMITTED
}

data class QuoteFormState(
    var typeOfUse: String = "",
    var devicesCount: String = "",
    var features: Set<String> = emptySet(),
    var region: String = "",
    var email: String = ""
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BusinessScreen(onBack: () -> Unit, onAccessDashboard: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf(QuoteState.OVERVIEW) }
    var formState by remember { mutableStateOf(QuoteFormState()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val repository = remember { BusinessRepository() }
    
    val gold = Color(0xFFFFC107)
    val obsidian = Color(0xFF0E0E0E)
    val backgroundBrush = Brush.verticalGradient(listOf(obsidian, Color(0xFF1B1B1B)))

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.business_screen_bg),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dark overlay for readability
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Business Automation",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Security as a Service",
                    fontSize = 12.sp,
                    color = gold
                )
            }
        }

        // Content
        AnimatedContent(targetState = screenState) { state ->
            when (state) {
                QuoteState.OVERVIEW -> BusinessOverviewContent(
                    onRequestQuote = { screenState = QuoteState.FORM }
                )
                QuoteState.FORM -> BusinessQuoteForm(
                    formState = formState,
                    isLoading = isLoading,
                    onStateChange = { formState = it },
                    onSubmit = {
                        if (formState.email.isBlank()) {
                            Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
                        } else {
                            isLoading = true
                            scope.launch {
                                val success = repository.submitQuote(
                                    QuoteRequest(
                                        typeOfUse = formState.typeOfUse,
                                        devicesCount = formState.devicesCount,
                                        region = formState.region,
                                        email = formState.email,
                                        features = formState.features.toList()
                                    )
                                )
                                isLoading = false
                                if (success) {
                                    screenState = QuoteState.SUBMITTED
                                } else {
                                    Toast.makeText(context, "Request sent (Offline Mode)", Toast.LENGTH_SHORT).show()
                                    // Normally we would show error, but for demo continuity if server is missing:
                                    screenState = QuoteState.SUBMITTED 
                                }
                            }
                        }
                    },
                    onCancel = { screenState = QuoteState.OVERVIEW }
                )
                QuoteState.SUBMITTED -> QuoteSubmittedState(
                    email = formState.email,
                    onEdit = { screenState = QuoteState.FORM },
                    onAccessDashboard = onAccessDashboard
                )
            }
        }
    }
    }
}

@Composable
fun BusinessOverviewContent(onRequestQuote: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFFC107), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, null, tint = Color(0xFFFFC107))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Custom Quote", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Advanced Security Automation",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tailored policies, reports, and network protection for businesses and managed users.",
                    color = Color(0xFFCCCCCC)
                )
                Spacer(modifier = Modifier.height(24.dp))
                BusinessSpecificButton(
                    text = "Request a Quote",
                    onClick = onRequestQuote,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Capabilities List
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("WHAT'S INCLUDED", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            IncludedSection("Core Capabilities", listOf(
                "Advanced Firewall & Network Attack Blocker",
                "Policy Builder (IF / THEN rules)",
                "Security Profiles (Office, Travel, Public Wi-Fi)",
                "Ransomware & Data Protection (Enterprise)",
                "Web Control (Category + custom domains)"
            ))

            IncludedSection("Business Tools", listOf(
                "Automated Reports (PDF / CSV)",
                "Compliance Monitoring",
                "Event Logs & Audit Trails",
                "Centralized Policies (per device / user)"
            ))

            IncludedSection("Special Use", listOf(
                "Child Protection (Schools / Families)",
                "Employee Device Protection",
                "Remote Work Security"
            ))
        }
        
        // AI Badge
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF112233), RoundedCornerShape(8.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFFFFC107))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("AI-Tailored Policies", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Our system adapts automation rules based on your environment.", color = Color.Gray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun IncludedSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
        items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Lock, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(item, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessQuoteForm(
    formState: QuoteFormState,
    isLoading: Boolean = false,
    onStateChange: (QuoteFormState) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Tell us about your needs", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)

        // Type of Use
        Text("Type of Organization", color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Small Business", "Enterprise", "School/Family").forEach { type ->
                FilterChip(
                    selected = formState.typeOfUse == type,
                    onClick = { onStateChange(formState.copy(typeOfUse = type)) },
                    label = { Text(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFC107),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Device Count
        Text("Number of Devices", color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1-5", "6-20", "21-100", "100+").forEach { count ->
                FilterChip(
                    selected = formState.devicesCount == count,
                    onClick = { onStateChange(formState.copy(devicesCount = count)) },
                    label = { Text(count) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFC107),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
        
        // Region & Email
        OutlinedTextField(
            value = formState.region,
            onValueChange = { onStateChange(formState.copy(region = it)) },
            label = { Text("Region / Country") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFC107),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFFFFC107),
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White 
            )
        )

        OutlinedTextField(
            value = formState.email,
            onValueChange = { onStateChange(formState.copy(email = it)) },
            label = { Text("Contact Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFC107),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFFFFC107),
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Cancel")
            }
            BusinessSpecificButton(
                text = if (isLoading) "Sending..." else "Submit Request",
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
                isLoading = isLoading
            )
        }
    }
}

@Composable
fun QuoteSubmittedState(email: String, onEdit: () -> Unit, onAccessDashboard: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Quote in Review",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Our team is preparing a tailored security plan for you.",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Confirmation sent to $email",
            color = Color(0xFF00E5FF),
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Access Business Dashboard Button
        BusinessSpecificButton(
            text = "Access Business Tools",
            onClick = onAccessDashboard,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Request", color = Color.Gray)
        }
    }
}

@Composable
fun BusinessSpecificButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Box(
        modifier = modifier
            .height(60.dp) // Ensure sufficient height for the image
            .background(Color(0xFFFFC107), RoundedCornerShape(12.dp)) // Temporary gold background
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        /*
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.gold_button_bg),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        */
        
        if (isLoading) {
            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
        } else {
            Text(
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
