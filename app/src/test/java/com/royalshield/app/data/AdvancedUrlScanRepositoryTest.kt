package com.royalshield.app.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedUrlScanRepositoryTest {
    @Test
    fun `authenticated scan sends only url and token to remote`() = runTest {
        var capturedUrl: String? = null
        var capturedToken: String? = null
        val repository = AdvancedUrlScanRepository(
            tokenProvider = AdvancedScanTokenProvider { "firebase-token" },
            remote = AdvancedUrlScanRemote { url, token ->
                capturedUrl = url
                capturedToken = token
                "analysis-1"
            },
        )

        val result = repository.submit("https://example.com/path")

        assertEquals("https://example.com/path", capturedUrl)
        assertEquals("firebase-token", capturedToken)
        assertEquals(AdvancedScanResult.Submitted("analysis-1"), result)
    }

    @Test
    fun `missing authentication is unavailable without remote call`() = runTest {
        var calls = 0
        val repository = AdvancedUrlScanRepository(
            tokenProvider = AdvancedScanTokenProvider { null },
            remote = AdvancedUrlScanRemote { _, _ -> calls += 1; "unexpected" },
        )

        val result = repository.submit("https://example.com")

        assertEquals(0, calls)
        assertEquals(AdvancedScanResult.Unavailable("AUTH_REQUIRED"), result)
    }

    @Test
    fun `invalid url is rejected locally`() = runTest {
        val repository = AdvancedUrlScanRepository(
            tokenProvider = AdvancedScanTokenProvider { "token" },
            remote = AdvancedUrlScanRemote { _, _ -> "unexpected" },
        )

        assertTrue(repository.submit("javascript:alert(1)") is AdvancedScanResult.Rejected)
    }
}
