package com.royalshield.app.managers.crypto

import android.util.Log
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec
import java.security.*

/**
 * Manager for Post-Quantum Signature (PQS) operations.
 * Implements ML-DSA (Dilithium) for quantum-resistant authentication.
 */
object PqcSignatureManager {
    private const val TAG = "PqcSignatureManager"
    private const val PROVIDER = "BCPQC"
    
    // ML-DSA-65 (Dilithium3) is the recommended balance for security and size
    private val DILITHIUM_PARAM = DilithiumParameterSpec.dilithium3

    /**
     * Generates a PQC Signature KeyPair (ML-DSA / Dilithium)
     */
    fun generateDilithiumKeyPair(): KeyPair? {
        return try {
            val kpg = KeyPairGenerator.getInstance("Dilithium", PROVIDER)
            kpg.initialize(DILITHIUM_PARAM, SecureRandom())
            kpg.generateKeyPair()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Dilithium KeyPair", e)
            null
        }
    }

    /**
     * Signs data using a PQC private key.
     */
    fun sign(privateKey: PrivateKey, data: ByteArray): ByteArray? {
        return try {
            val signature = Signature.getInstance("Dilithium", PROVIDER)
            signature.initSign(privateKey)
            signature.update(data)
            signature.sign()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing data with Dilithium", e)
            null
        }
    }

    /**
     * Verifies a signature using a PQC public key.
     */
    fun verify(publicKey: PublicKey, data: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            val signature = Signature.getInstance("Dilithium", PROVIDER)
            signature.initVerify(publicKey)
            signature.update(data)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying Dilithium signature", e)
            false
        }
    }

    /**
     * Hybrid Verification: Checks both a classical signature (ECDSA) and a PQC signature.
     * Use this for critical artifacts (OTA, Config, Rules).
     */
    fun verifyHybrid(
        data: ByteArray,
        classicalPublicKey: PublicKey,
        classicalSignature: ByteArray,
        pqcPublicKey: PublicKey,
        pqcSignature: ByteArray
    ): Boolean {
        // Verify classical signature first
        val classicalOk = try {
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(classicalPublicKey)
            sig.update(data)
            sig.verify(classicalSignature)
        } catch (e: Exception) {
            Log.e(TAG, "Error during classical verification", e)
            false
        }

        if (!classicalOk) return false

        // Verify PQC signature
        return verify(pqcPublicKey, data, pqcSignature)
    }
}
