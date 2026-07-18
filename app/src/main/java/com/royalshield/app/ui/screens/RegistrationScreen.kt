package com.royalshield.app.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.royalshield.app.ui.theme.*
import com.royalshield.app.R
import com.royalshield.app.ui.components.RoyalGoldButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class RegistrationParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val drift: Float,
    val phase: Float,
    val color: Color
)

@Composable
private fun rememberRegistrationTilt(): Pair<Float, Float> {
    val context = LocalContext.current
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        var baselinePitch: Float? = null
        var baselineRoll: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)

                val pitch = orientation[1]
                val roll = orientation[2]
                if (baselinePitch == null || baselineRoll == null) {
                    baselinePitch = pitch
                    baselineRoll = roll
                    return
                }

                fun angleDelta(current: Float, baseline: Float): Float {
                    var delta = current - baseline
                    while (delta > PI.toFloat()) delta -= (2f * PI.toFloat())
                    while (delta < -PI.toFloat()) delta += (2f * PI.toFloat())
                    return delta
                }

                val targetX = (angleDelta(roll, baselineRoll!!) / 0.35f).coerceIn(-1f, 1f)
                val targetY = (angleDelta(pitch, baselinePitch!!) / 0.35f).coerceIn(-1f, 1f)
                tiltX += (targetX - tiltX) * 0.14f
                tiltY += (targetY - tiltY) * 0.14f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltX to tiltY
}

@Composable
private fun CyberParticleField(
    tiltX: Float,
    tiltY: Float,
    modifier: Modifier = Modifier
) {
    val particles = remember {
        val random = Random(7421)
        List(52) { index ->
            RegistrationParticle(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = random.nextFloat() * 2.2f + 0.8f,
                speed = random.nextFloat() * 0.045f + 0.018f,
                drift = random.nextFloat() * 0.035f + 0.008f,
                phase = random.nextFloat() * (2f * PI.toFloat()),
                color = if (index % 3 == 0) Color(0xFFFF32D2) else Color(0xFF27DFFF)
            )
        }
    }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startedAt = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                elapsedSeconds = (frameTime - startedAt) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val animatedY = (particle.y - elapsedSeconds * particle.speed).let {
                ((it % 1.15f) + 1.15f) % 1.15f
            } - 0.08f
            val wave = sin(elapsedSeconds * 0.8f + particle.phase) * particle.drift
            val px = (particle.x + wave + tiltX * 0.018f) * size.width
            val py = (animatedY + tiltY * 0.012f) * size.height
            val pulse = 0.55f + 0.45f * sin(elapsedSeconds * 1.6f + particle.phase)

            drawCircle(
                color = particle.color.copy(alpha = 0.10f + pulse * 0.16f),
                radius = particle.radius * 3.2f,
                center = androidx.compose.ui.geometry.Offset(px, py)
            )
            drawCircle(
                color = particle.color.copy(alpha = 0.35f + pulse * 0.45f),
                radius = particle.radius,
                center = androidx.compose.ui.geometry.Offset(px, py)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onRegistrationSuccess: () -> Unit,
    onNavigateToPhoneAuth: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showEmailForm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val (backgroundTiltX, backgroundTiltY) = rememberRegistrationTilt()

    // Firebase Auth instance
    val auth = remember {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            android.util.Log.e("RegistrationScreen", "Firebase not initialized: ${e.message}")
            null
        }
    }

    // Google Sign-In configuration
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Google Sign-In result launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null && auth != null) {
                coroutineScope.launch {
                    try {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(credential).await()
                        val displayName = auth.currentUser?.displayName ?: "User"
                        Toast.makeText(context, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        onRegistrationSuccess()
                    } catch (e: Exception) {
                        isLoading = false
                        Toast.makeText(context, "Auth error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                isLoading = false
                Toast.makeText(context, "Google Sign-In: no ID token received", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            isLoading = false
            android.util.Log.w("RegistrationScreen", "Google sign-in failed, code=${e.statusCode}", e)
            Toast.makeText(context, "Google Sign-In failed (${e.statusCode})", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Cyber face background with a subtle sensor-driven parallax effect.
        Image(
            painter = painterResource(id = R.drawable.bg_registration_cyber_face),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.12f
                    scaleY = 1.12f
                    translationX = backgroundTiltX * 28.dp.toPx()
                    translationY = backgroundTiltY * 22.dp.toPx()
                }
        )

        CyberParticleField(
            tiltX = backgroundTiltX,
            tiltY = backgroundTiltY,
            modifier = Modifier.fillMaxSize()
        )
        
        // Dark overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Skip for Testing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { 
                    Toast.makeText(context, "Skipping registration (Testing Mode)", Toast.LENGTH_SHORT).show()
                    onRegistrationSuccess() 
                }) {
                    Text("SKIP >>", color = RoyalGold, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Logo/Icon
            Image(
                painter = painterResource(id = R.drawable.royal_shield_logo),
                contentDescription = "Royal Shield Logo",
                modifier = Modifier.size(120.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Protection Starts Here",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Security assurances list
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp), 
                horizontalAlignment = Alignment.Start
            ) {
                Text("🔒 Encrypted connection", color = Color.Gray, fontSize = 12.sp)
                Text("🛡️ Your data is protected", color = Color.Gray, fontSize = 12.sp)
                Text("✅ Secure authentication", color = Color.Gray, fontSize = 12.sp)
                Text("🔐 Privacy guaranteed", color = Color.Gray, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(48.dp))

            // --- Social Login Buttons ---

            SocialLoginButton(
                text = "Continue with Phone",
                icon = Icons.Default.Phone,
                color = RoyalGold,
                textColor = Color.Black
            ) {
                onNavigateToPhoneAuth()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real Google Sign-In button
            SocialLoginButton(
                text = "Continue with Google",
                iconRes = R.drawable.ic_google,
                color = Color.White,
                textColor = Color.Black
            ) {
                isLoading = true
                // Sign out first to always show the account picker
                googleSignInClient.signOut().addOnCompleteListener {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            SocialLoginButton(
                text = "Continue with Apple",
                iconRes = R.drawable.ic_apple,
                color = Color.Black,
                textColor = Color.White
            ) {
                isLoading = true
                Toast.makeText(context, "Redirecting to Apple ID...", Toast.LENGTH_SHORT).show()
                onRegistrationSuccess()
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            SocialLoginButton(
                text = "Continue with GitHub",
                icon = Icons.Default.Code,
                color = Color(0xFF24292E),
                textColor = Color.White
            ) {
                isLoading = true
                Toast.makeText(context, "Redirecting to GitHub...", Toast.LENGTH_SHORT).show()
                onRegistrationSuccess()
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SocialLoginButton(
                text = "Continue with Facebook",
                iconRes = R.drawable.ic_facebook,
                color = Color(0xFF1877F2),
                textColor = Color.White
            ) {
                isLoading = true
                Toast.makeText(context, "Redirecting to Facebook...", Toast.LENGTH_SHORT).show()
                onRegistrationSuccess()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                Text(" OR ", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (!showEmailForm) {
                OutlinedButton(
                    onClick = { showEmailForm = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Email, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Email", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Sign In",
                    color = RoyalGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { 
                        Toast.makeText(context, "Switching to Login...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "By signing up, you agree to our Terms of Service and Privacy Policy",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RoyalGold)
            }
        }
    }
}

@Composable
fun SocialLoginButton(
    text: String,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        color = color,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            } else if (icon != null) {
                Icon(icon, null, tint = textColor, modifier = Modifier.size(24.dp))
            }
            Text(
                text = text,
                color = textColor,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
