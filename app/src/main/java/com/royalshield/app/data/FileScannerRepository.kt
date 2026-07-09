package com.royalshield.app.data

import com.royalshield.app.util.FileHashCalculator
import java.io.InputStream

/**
 * Repository responsible for handling file scanning operations.
 * Currently, it delegates to [FileHashCalculator] to calculate the SHA-256 hash of a file.
 * The production API logic for backend interaction will be handled on the backend layer
 * without exposing API keys here.
 */
class FileScannerRepository {

    /**
     * Scans a file by generating its SHA-256 hash.
     * This function suspends to ensure the calculation does not block the main thread.
     *
     * @param inputStream The stream of the file to be scanned.
     * @return The calculated SHA-256 hash as a hex string, or null if an error occurred.
     */
    suspend fun scanFileStream(inputStream: InputStream): String? {
        // Calculate the hash locally
        return FileHashCalculator.calculateSha256(inputStream)

        // Note: Future expansions will send the resulting hash to the Royal Shield backend
        // for threat lookup (e.g. VirusTotal API) without exposing tokens in the client app.
    }
}
