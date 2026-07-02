package com.royalshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.managers.crypto.PqcManager
import com.royalshield.app.managers.crypto.PqcSignatureManager
import com.royalshield.app.ui.theme.Royal_shieldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKeyPairGenerator
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberParameters
import java.security.Security

class PqcTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PqcTestScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PqcTestScreen() {
    val scope = rememberCoroutineScope()
    var logText by remember { mutableStateOf("Ready to test PQC...\n") }
    val scrollState = rememberScrollState()

    fun log(msg: String) {
        logText += "> $msg\n"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "PQC Verification Console",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF00FFCC) // Neon teal
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // PQC Status Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val providers = Security.getProviders().map { it.name }
                Text("BCPQC Provider: ${if ("BCPQC" in providers) "DETECTED" else "MISSING"}", color = if ("BCPQC" in providers) Color.Green else Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        log("Testing ML-KEM (Kyber768)...")
                        val startTime = System.currentTimeMillis()
                        val keyPair = withContext(Dispatchers.Default) { PqcManager.generateKyberKeyPair() }
                        val endTime = System.currentTimeMillis()
                        
                        if (keyPair != null) {
                            log("Kyber KeyPair generated in ${endTime - startTime}ms")
                            log("Public Key: ${keyPair.public.algorithm} - ${keyPair.public.encoded.size} bytes")
                        } else {
                            log("Kyber KeyPair generation FAILED")
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Test KEM")
            }

            Button(
                onClick = {
                    scope.launch {
                        log("Testing ML-DSA (Dilithium3)...")
                        val data = "Royal Shield PQC verification payload".toByteArray()
                        
                        val keyPair = withContext(Dispatchers.Default) { PqcSignatureManager.generateDilithiumKeyPair() }
                        if (keyPair == null) {
                            log("Dilithium KeyPair generation FAILED")
                            return@launch
                        }
                        
                        val startTime = System.currentTimeMillis()
                        val signature = withContext(Dispatchers.Default) { PqcSignatureManager.sign(keyPair.private, data) }
                        val endTime = System.currentTimeMillis()
                        
                        if (signature != null) {
                            log("Dilithium Signature created in ${endTime - startTime}ms")
                            log("Signature size: ${signature.size} bytes")
                            
                            val ok = withContext(Dispatchers.Default) { PqcSignatureManager.verify(keyPair.public, data, signature) }
                            log("Signature verification: ${if (ok) "SUCCESS ✅" else "FAILED ❌"}")
                        } else {
                            log("Signature creation FAILED")
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Test DSA")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Console Output
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Text(
                text = logText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color.LightGray
            )
        }
        
        Button(
            onClick = { logText = "> Cleared\n" },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Console")
        }
    }
}
