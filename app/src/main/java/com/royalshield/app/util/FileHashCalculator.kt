package com.royalshield.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

/**
 * Utility object for calculating file hashes securely and efficiently.
 * Currently supports SHA-256 calculation.
 */
object FileHashCalculator {

    /**
     * Calculates the SHA-256 hash of the given [InputStream].
     * Reads the stream in chunks to avoid memory overflow when processing large files.
     * The function suspends and executes on the IO dispatcher.
     *
     * @param inputStream The input stream to read the file data from.
     * @return The SHA-256 hash as a lowercase hexadecimal string, or null if an error occurs.
     */
    suspend fun calculateSha256(inputStream: InputStream): String? = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int

            inputStream.use { stream ->
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            val hashBytes = digest.digest()
            val hexString = StringBuilder(2 * hashBytes.size)
            for (byte in hashBytes) {
                val hex = Integer.toHexString(0xff and byte.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
