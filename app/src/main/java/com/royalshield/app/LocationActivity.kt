package com.royalshield.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.royalshield.app.ui.theme.Royal_shieldTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

// --- 1. THEME & TOKENS ---
object RoyalTheme {
    val BackgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF5C6AC4), Color(0xFF7B42F6))
    )
    val Surface = Color(0xFFFFFFFF).copy(alpha = 0.10f)
    val SurfaceHighlight = Color(0xFFFFFFFF).copy(alpha = 0.15f)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE2E8F0)
    val AccentRed = Color(0xFFE53E3E)
    val AccentRedHover = Color(0xFFC53030)
    val SuccessGreen = Color(0xFF48BB78)
    
    val CardShape = RoundedCornerShape(24.dp)
    val ButtonShape = RoundedCornerShape(12.dp)
    val InputShape = RoundedCornerShape(16.dp)
}

// --- 2. DATA MODELS ---
// Contact class is defined in Contact.kt

// --- 3. MAIN ACTIVITY ---
class LocationActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            Royal_shieldTheme {
                LocationScreenContainer(fusedLocationClient)
            }
        }
    }
}

//--- 4. COMPONENTS ---

@Composable
fun LocationScreenContainer(fusedLocationClient: FusedLocationProviderClient) {
    val context = LocalContext.current
    
    var contacts by remember { mutableStateOf(loadContacts(context)) }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var locationStatus by remember { mutableStateOf("Synchronizing satellites...") }
    val cameraPositionState = rememberCameraPositionState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            locationStatus = "GPS Active"
        } else {
            locationStatus = "GPS Permission Denied"
        }
    }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        
        while(true) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            currentLocation = loc
                            locationStatus = "Updated (Power Saving)"
                            
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                LatLng(loc.latitude, loc.longitude),
                                14f
                            )
                        }
                    }
            }
            delay(15000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.location_bg),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RoyalHeader(title = "Live Location", subtitle = "Share secure location")

            // Google Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, RoyalTheme.BackgroundGradient, RoundedCornerShape(16.dp))
            ) {
                if (currentLocation != null) {
                    val latLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState
                    ) {
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "You are here",
                            snippet = "Secure Location"
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7B42F6), strokeWidth = 3.dp)
                            Text("Synchronizing GPS...", color = RoyalTheme.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = Color(0xFF1A1A2E).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "🗺️ Google Maps",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF7B42F6),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LocationStatusCard(location = currentLocation, status = locationStatus)

            AddContactForm(
                onAddContact = { name, phone ->
                    val newContact = Contact(name = name, phone = phone)
                    val newList = contacts + newContact
                    contacts = newList
                    saveContacts(context, newList)
                }
            )

            ContactList(
                contacts = contacts,
                onSend = { contact ->
                    sendLocationToWhatsApp(context, contact, currentLocation)
                },
                onDelete = { contact ->
                    val newList = contacts.filter { it.id != contact.id }
                    contacts = newList
                    saveContacts(context, newList)
                }
            )
        }
    }
}

@Composable
fun RoyalHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = RoyalTheme.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            color = RoyalTheme.TextSecondary.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
    }
}

@Composable
fun LocationStatusCard(location: android.location.Location?, status: String) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(RoyalTheme.SurfaceHighlight, CircleShape)
                    .border(1.dp, RoyalTheme.TextSecondary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = RoyalTheme.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                if (location != null) {
                    Text(
                        text = "${location.latitude.toString().take(7)}, ${location.longitude.toString().take(7)}",
                        color = RoyalTheme.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        text = "Searching for signal...",
                        color = RoyalTheme.TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = status,
                    color = RoyalTheme.SuccessGreen,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun AddContactForm(onAddContact: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("NEW CONTACT", color = RoyalTheme.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            RoyalInput(
                value = name,
                onValueChange = { name = it },
                placeholder = "Contact Name",
                icon = Icons.Default.Person
            )
            
            RoyalInput(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "Phone (e.g. +1...)",
                icon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone
            )

            RoyalButton(
                text = "ADD TO SECURE LIST",
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onAddContact(name.trim(), phone.trim())
                        name = ""
                        phone = ""
                    }
                },
                icon = Icons.Default.Add
            )
        }
    }
}

@Composable
fun ContactList(
    contacts: List<Contact>,
    onSend: (Contact) -> Unit,
    onDelete: (Contact) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CONTACTS (${contacts.size})",
            color = RoyalTheme.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxHeight()
        ) {
            itemsIndexed(contacts) { _, contact ->
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn() + fadeIn(),
                    exit = fadeOut()
                ) {
                    ContactItem(contact, onSend, onDelete)
                }
            }
            if (contacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No contacts yet", color = RoyalTheme.TextSecondary.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact, onSend: (Contact) -> Unit, onDelete: (Contact) -> Unit) {
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockConfirmation by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoyalTheme.Surface, RoyalTheme.ButtonShape)
            .border(1.dp, RoyalTheme.SurfaceHighlight, RoyalTheme.ButtonShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, color = RoyalTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(contact.phone, color = RoyalTheme.TextSecondary, fontSize = 12.sp)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Send Button
            Surface(
                color = RoyalTheme.AccentRed,
                shape = CircleShape,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onSend(contact) },
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            
            // Report Button (NEW)
            Surface(
                color = Color(0xFFFF8C00),
                shape = CircleShape,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { showReportDialog = true },
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Report, contentDescription = "Report", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            
            // Block Button (NEW)
            Surface(
                color = Color(0xFF8B0000),
                shape = CircleShape,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { showBlockConfirmation = true },
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Block, contentDescription = "Block", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            
            // Delete Icon
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = RoyalTheme.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onDelete(contact) }
            )
        }
    }
    
    // Report Dialog
    if (showReportDialog) {
        ReportContactDialog(
            contact = contact,
            onDismiss = { showReportDialog = false },
            onSubmit = { reportReason ->
                // Send to Firestore reports collection
                val firestore = FirebaseFirestore.getInstance()
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                
                val reportData = hashMapOf(
                    "reportedBy" to uid,
                    "contactName" to contact.name,
                    "contactPhone" to contact.phone,
                    "reason" to reportReason,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "pending"
                )
                
                firestore.collection("contact_reports")
                    .add(reportData)
                    .addOnSuccessListener {
                        android.widget.Toast.makeText(
                            context,
                            "Report submitted for ${contact.name}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        android.widget.Toast.makeText(
                            context,
                            "Failed to submit report: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                
                showReportDialog = false
            }
        )
    }
    
    // Block Confirmation Dialog
    if (showBlockConfirmation) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = { Text("Block Contact", color = Color.White) },
            text = { 
                Text(
                    "Are you sure you want to block ${contact.name}?\nThis will prevent sharing location with them.",
                    color = RoyalTheme.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Mark as blocked in Firestore
                        val firestore = FirebaseFirestore.getInstance()
                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                        
                        val blockData = hashMapOf(
                            "blockedBy" to uid,
                            "contactName" to contact.name,
                            "contactPhone" to contact.phone,
                            "blockedAt" to System.currentTimeMillis()
                        )
                        
                        firestore.collection("blocked_contacts")
                            .document("${uid}_${contact.phone}")
                            .set(blockData)
                            .addOnSuccessListener {
                                onDelete(contact) // Remove from local list
                                android.widget.Toast.makeText(
                                    context,
                                    "${contact.name} blocked",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                android.widget.Toast.makeText(
                                    context,
                                    "Failed to block: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        
                        showBlockConfirmation = false
                    }
                ) {
                    Text("BLOCK", color = Color(0xFFFF3131), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmation = false }) {
                    Text("CANCEL", color = RoyalTheme.TextSecondary)
                }
            },
            containerColor = Color(0xFF1A1A2E)
        )
    }
}

@Composable
private fun ReportContactDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reportReason by remember { mutableStateOf("") }
    val reportCategories = listOf(
        "Spam / Unwanted Contact",
        "Suspicious Activity",
        "Harassment",
        "Scam Attempt",
        "Other Security Concern"
    )
    var selectedCategory by remember { mutableStateOf(reportCategories[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text("Report Contact", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${contact.name} (${contact.phone})",
                    color = RoyalTheme.TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select reason:", color = RoyalTheme.TextSecondary, fontSize = 14.sp)
                
                reportCategories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = category }
                            .background(
                                if (selectedCategory == category) 
                                    Color(0xFFFF8C00).copy(alpha = 0.2f) 
                                else 
                                    Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFFF8C00),
                                unselectedColor = RoyalTheme.TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category, color = Color.White, fontSize = 14.sp)
                    }
                }
                
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Additional details (optional)", color = RoyalTheme.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF8C00),
                        unfocusedBorderColor = RoyalTheme.TextSecondary.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit("$selectedCategory: $reportReason") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00))
            ) {
                Text("SUBMIT REPORT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = RoyalTheme.TextSecondary)
            }
        },
        containerColor = Color(0xFF1A1A2E)
    )
}

// --- 5. UI PRIMITIVES ---

@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = RoyalTheme.Surface,
        shape = RoyalTheme.CardShape,
        border = BorderStroke(1.dp, RoyalTheme.SurfaceHighlight),
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    )
}

@Composable
fun RoyalInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = RoyalTheme.TextSecondary.copy(alpha = 0.5f)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = RoyalTheme.TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoyalTheme.InputShape,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = RoyalTheme.SurfaceHighlight,
            unfocusedContainerColor = RoyalTheme.Surface,
            focusedBorderColor = RoyalTheme.TextSecondary,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = RoyalTheme.TextPrimary,
            unfocusedTextColor = RoyalTheme.TextPrimary,
            cursorColor = RoyalTheme.AccentRed
        )
    )
}

@Composable
fun RoyalButton(text: String, onClick: () -> Unit, icon: ImageVector) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoyalTheme.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = RoyalTheme.AccentRed,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

// --- 6. HELPERS & PERSISTENCE ---

private fun sendLocationToWhatsApp(context: Context, contact: Contact, location: android.location.Location?) {
    if (location == null) {
        Toast.makeText(context, "Waiting for GPS signal...", Toast.LENGTH_SHORT).show()
        return
    }
    
    val mapLink = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
    val message = "Hello ${contact.name}, my secure real-time location is: $mapLink"
    val url = "https://api.whatsapp.com/send?phone=${contact.phone}&text=${Uri.encode(message)}"
    
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage("com.whatsapp")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun saveContacts(context: Context, contacts: List<Contact>) {
    val prefs = context.getSharedPreferences("live_location_prefs", Context.MODE_PRIVATE)
    val jsonArray = JSONArray()
    contacts.forEach { 
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("name", it.name)
        obj.put("phone", it.phone)
        jsonArray.put(obj)
    }
    prefs.edit().putString("contacts_list", jsonArray.toString()).apply()
}

private fun loadContacts(context: Context): List<Contact> {
    val prefs = context.getSharedPreferences("live_location_prefs", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("contacts_list", null) ?: return emptyList()
    
    val list = mutableListOf<Contact>()
    try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = if (obj.has("id")) obj.getLong("id") else System.currentTimeMillis() + i
            list.add(Contact(id, obj.getString("name"), obj.getString("phone")))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
