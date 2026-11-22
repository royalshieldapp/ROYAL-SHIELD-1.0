package com.royalshield.app.ui.screens

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.widget.Toast
import com.royalshield.app.auth.AuthProfileRepository
import com.royalshield.app.auth.OAuthAuthHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.royalshield.app.auth.googleAuthErrorMessage
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
    var isLoading by remember { mutableStateOf(false) }
    var showLockedOrbDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val (backgroundTiltX, backgroundTiltY) = rememberRegistrationTilt()
    val parallax = registrationParallax(backgroundTiltX, backgroundTiltY)
    val reducedMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) <= 0f
    }

    // Firebase Auth instance
    val auth = remember {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            android.util.Log.e("RegistrationScreen", "Firebase not initialized: ${e.message}")
            null
        }
    }

    val activity = context as? Activity
    val profileRepository = remember { AuthProfileRepository() }
    val oAuthHandler = remember(auth) {
        auth?.let { OAuthAuthHandler(auth = it, profileRepository = profileRepository) }
    }

    // Check for pending OAuth results after activity recreation (e.g. browser return)
    LaunchedEffect(activity, oAuthHandler) {
        if (activity != null && oAuthHandler != null) {
            oAuthHandler.checkPendingAuthResult(
                activity = activity,
                onSuccess = { user ->
                    val displayName = user.displayName ?: "User"
                    Toast.makeText(context, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    onRegistrationSuccess()
                },
                onError = { errorMsg ->
                    isLoading = false
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Auto-advance if already authenticated
    LaunchedEffect(auth?.currentUser) {
        if (auth?.currentUser != null && !isLoading) {
            onRegistrationSuccess()
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
                        val authResult = auth.signInWithCredential(credential).await()
                        val user = authResult.user
                        if (user != null) {
                            runCatching { profileRepository.ensureProfile(user) }
                                .onFailure {
                                    android.util.Log.w("RegistrationScreen", "Profile sync warning: ${it.message}")
                                }
                            val displayName = user.displayName ?: "User"
                            Toast.makeText(context, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                            isLoading = false
                            onRegistrationSuccess()
                        } else {
                            isLoading = false
                            Toast.makeText(context, "Google Sign-In: no user received", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        val errorCode = (e as? FirebaseAuthException)?.errorCode
                        val configurationMissing = e.message?.contains(
                            "CONFIGURATION_NOT_FOUND",
                            ignoreCase = true,
                        ) == true
                        android.util.Log.e(
                            "RegistrationScreen",
                            "Firebase Google auth failed: code=${errorCode ?: "UNKNOWN"}, " +
                                "configurationMissing=$configurationMissing, type=${e.javaClass.simpleName}",
                        )
                        Toast.makeText(
                            context,
                            googleAuthErrorMessage(errorCode, e.message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            } else {
                isLoading = false
                Toast.makeText(context, "Google Sign-In: no ID token received", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            isLoading = false
            android.util.Log.w("RegistrationScreen", "Google sign-in failed, code=${e.statusCode}", e)
            if (e.statusCode != 12501) {
                Toast.makeText(context, "Google Sign-In failed (${e.statusCode})", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Approved gold AI background, exclusive to Registration.
        Image(
            painter = painterResource(id = R.drawable.bg_registration_gold_ai),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.12f
                    scaleY = 1.12f
                    translationX = parallax.backgroundXdp.dp.toPx()
                    translationY = parallax.backgroundYdp.dp.toPx()
                }
        )

        CyberParticleField(
            tiltX = backgroundTiltX,
            tiltY = backgroundTiltY,
            modifier = Modifier.fillMaxSize()
        )
        
        // Preserve the artwork while keeping controls legible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.32f),
                            Color.Black.copy(alpha = 0.48f),
                            Color.Black.copy(alpha = 0.68f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    translationX = parallax.controlsXdp.dp.toPx()
                    translationY = parallax.controlsYdp.dp.toPx()
                }
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(24.dp))
            
            RegistrationOrbHero(
                reducedMotion = reducedMotion,
                onLockedInfo = { showLockedOrbDialog = true },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- Social Login Buttons ---

            LuxuryRegistrationPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Choose how to continue",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SocialLoginButton(
                text = "Continue with Phone",
                iconRes = R.drawable.ic_registration_phone_gold,
                color = RoyalGold,
                textColor = Color.Black,
                enabled = RegistrationProvider.PHONE.isAvailable,
                iconSize = 36.dp,
            ) {
                onNavigateToPhoneAuth()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real Google Sign-In button
            SocialLoginButton(
                text = "Continue with Google",
                iconRes = R.drawable.ic_registration_google_gold,
                color = Color.White,
                textColor = Color.Black,
                enabled = RegistrationProvider.GOOGLE.isAvailable,
                iconSize = 36.dp,
            ) {
                isLoading = true
                // Sign out first to always show the account picker
                googleSignInClient.signOut().addOnCompleteListener {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { onRegistrationSuccess() },
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text(
                    text = "EXPLORE AS GUEST • FULL ACCESS",
                    color = RoyalGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "COMING SOON",
                color = RoyalGold.copy(alpha = 0.80f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComingSoonProviderChip(
                    text = "Apple — COMING SOON",
                    iconRes = R.drawable.ic_registration_apple_gold,
                    enabled = RegistrationProvider.APPLE.isAvailable,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activity?.let { act ->
                            oAuthHandler?.signInWithOAuth(
                                activity = act,
                                provider = RegistrationProvider.APPLE,
                                onStart = { isLoading = true },
                                onSuccess = { user ->
                                    val displayName = user.displayName ?: "User"
                                    Toast.makeText(context, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                    onRegistrationSuccess()
                                },
                                onError = { message ->
                                    isLoading = false
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                )
                ComingSoonProviderChip(
                    text = "GitHub — COMING SOON",
                    iconRes = R.drawable.ic_registration_git_gold,
                    enabled = RegistrationProvider.GITHUB.isAvailable,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activity?.let { act ->
                            oAuthHandler?.signInWithOAuth(
                                activity = act,
                                provider = RegistrationProvider.GITHUB,
                                onStart = { isLoading = true },
                                onSuccess = { user ->
                                    val displayName = user.displayName ?: "User"
                                    Toast.makeText(context, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                    onRegistrationSuccess()
                                },
                                onError = { message ->
                                    isLoading = false
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComingSoonProviderChip(
                    text = "Facebook — COMING SOON",
                    iconRes = R.drawable.ic_registration_facebook_gold,
                    enabled = RegistrationProvider.FACEBOOK.isAvailable,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activity?.let { act ->
                            oAuthHandler?.signInWithOAuth(
                                activity = act,
                                provider = RegistrationProvider.FACEBOOK,
                                onStart = { isLoading = true },
                                onSuccess = { user ->
                                    val displayName = user.displayName ?: "User"
                                    Toast.makeText(context, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                    onRegistrationSuccess()
                                },
                                onError = { message ->
                                    isLoading = false
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                )
                ComingSoonProviderChip(
                    text = "Email — COMING SOON",
                    iconRes = R.drawable.ic_registration_email_gold,
                    enabled = RegistrationProvider.EMAIL.isAvailable,
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "By signing up, you agree to our Terms of Service and Privacy Policy",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            }
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RoyalGold)
            }
        }

        if (showLockedOrbDialog) {
            AlertDialog(
                onDismissRequest = { showLockedOrbDialog = false },
                containerColor = Color(0xFF100D07),
                titleContentColor = RoyalGold,
                textContentColor = Color.White,
                title = { Text("Security Agent Locked") },
                text = {
                    Text(
                        "Continue with Phone or Google to begin setup. Feature availability depends on your plan, device permissions, and configured services."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showLockedOrbDialog = false }) {
                        Text("GOT IT", color = RoyalGold)
                    }
                },
            )
        }
    }
}

@Composable
private fun LuxuryRegistrationPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = 22.dp.toPx()
                shape = RoundedCornerShape(30.dp)
                clip = false
            }
            .background(Color.Black.copy(alpha = 0.24f), RoundedCornerShape(30.dp))
            .neonSweepBorder(
                strokeWidth = 1.8.dp,
                shape = RoundedCornerShape(30.dp),
                glowAlpha = 0.90f,
                durationMillis = 3600,
            )
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Color(0xFFFFC43B).copy(alpha = 0.38f),
                    RoundedCornerShape(25.dp)
                )
                .background(Color(0xFF090704).copy(alpha = 0.18f), RoundedCornerShape(25.dp))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun ComingSoonProviderChip(
    text: String,
    iconRes: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 54.dp)
            .semantics {
                contentDescription = "$text provider, disabled"
            },
        color = Color.Black.copy(alpha = 0.36f),
        contentColor = Color.White.copy(alpha = 0.62f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            RoyalGold.copy(alpha = 0.28f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(36.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text.split("—", "–", "-").first().trim(),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = "COMING SOON",
                    color = RoyalGold.copy(alpha = 0.62f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
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
    enabled: Boolean = true,
    iconSize: Dp? = null,
    onClick: () -> Unit
) {
    val layout = registrationSocialButtonLayout()
    val actualIconSize = iconSize ?: layout.iconSizeDp.dp
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth(layout.widthFraction)
            .height(layout.heightDp.dp)
            .graphicsLayer {
                shadowElevation = 12.dp.toPx()
                shape = RoundedCornerShape(16.dp)
            },
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (enabled) 1f else 0.52f),
        ) {
            Image(
                painter = painterResource(R.drawable.bg_button_gold),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = text,
                color = textColor,
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = layout.labelFontSizeSp.sp,
                maxLines = 1,
                softWrap = false,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = layout.horizontalPaddingDp.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(layout.leadingSlotWidthDp.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = "$text icon",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(actualIconSize),
                        )
                    } else if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = "$text icon",
                            tint = Color.Black,
                            modifier = Modifier.size(actualIconSize),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(layout.trailingSlotWidthDp.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {}
            }
        }
    }
}

@Composable
internal fun Modifier.neonSweepBorder(
    strokeWidth: Dp = 1.8.dp,
    shape: CornerBasedShape = RoundedCornerShape(30.dp),
    glowAlpha: Float = 0.85f,
    durationMillis: Int = 3600,
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "neon_sweep")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "neon_sweep_angle",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "neon_pulse_alpha",
    )

    return this.drawWithCache {
        val rad = Math.toRadians(angle.toDouble())
        val cos = kotlin.math.cos(rad).toFloat()
        val sin = kotlin.math.sin(rad).toFloat()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val rx = w * 0.7f
        val ry = h * 0.7f

        val startX = cx - cos * rx
        val startY = cy - sin * ry
        val endX = cx + cos * rx
        val endY = cy + sin * ry

        val neonBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFD86A).copy(alpha = 0.20f),
                Color(0xFFFFC43B).copy(alpha = 0.50f),
                Color(0xFFFFF7DC).copy(alpha = glowAlpha * pulseAlpha),
                Color(0xFFFFB300).copy(alpha = 0.70f),
                Color(0xFFFFD86A).copy(alpha = 0.20f),
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
        )

        onDrawWithContent {
            drawContent()
            drawOutline(
                outline = shape.createOutline(size, layoutDirection, this),
                brush = neonBrush,
                style = Stroke(width = strokeWidth.toPx()),
            )
        }
    }
}
