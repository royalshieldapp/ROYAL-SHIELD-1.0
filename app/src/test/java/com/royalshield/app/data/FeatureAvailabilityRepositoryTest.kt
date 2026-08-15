package com.royalshield.app.data

import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAvailabilityRepositoryTest {
    private val now = Instant.parse("2026-08-15T15:00:00Z")

    @Test
    fun mapsReadyDegradedAndUnavailableStates() = runTest {
        val remote = FakeFeatureStatusRemote(
            response = """
                {
                  "vpn": {"status":"READY","dataFreshness":"2026-08-15T14:30:00Z","sources":["e2e:vpn"],"modelVersion":null,"confidence":null},
                  "forecast": {"status":"DEGRADED","reason":"MODEL_DATA_DELAYED","dataFreshness":"2026-08-15T14:00:00Z","sources":["NVD"]},
                  "threatMap": {"status":"UNAVAILABLE","reason":"THREAT_DATA_NOT_VERIFIED"},
                  "automations": {"status":"UNAVAILABLE","reason":"AUTOMATION_ACTIONS_NOT_VERIFIED"},
                  "reports": {"status":"UNAVAILABLE","reason":"REPORT_SOURCES_NOT_VERIFIED"},
                  "policies": {"status":"UNAVAILABLE","reason":"POLICY_ENFORCEMENT_NOT_VERIFIED"}
                }
            """.trimIndent(),
        )
        val result = repository(remote).refresh()

        assertTrue(result.vpn is AvailabilityState.Ready)
        assertTrue(result.forecast is AvailabilityState.Degraded)
        assertEquals(
            "THREAT_DATA_NOT_VERIFIED",
            (result.threatMap as AvailabilityState.Unavailable).reason,
        )
        assertEquals("token", remote.receivedToken)
    }

    @Test
    fun staleReadyEvidenceFailsClosed() = runTest {
        val remote = FakeFeatureStatusRemote(
            response = completeResponse("READY", "2026-08-13T14:30:00Z"),
        )

        val result = repository(remote).refresh()

        assertEquals("STATUS_STALE", (result.vpn as AvailabilityState.Unavailable).reason)
    }

    @Test
    fun missingModuleAndMalformedPayloadFailClosed() = runTest {
        val missing = repository(FakeFeatureStatusRemote(response = "{}" )).refresh()
        val malformed = repository(FakeFeatureStatusRemote(response = "not-json")).refresh()

        assertEquals("STATUS_UNVERIFIED", (missing.vpn as AvailabilityState.Unavailable).reason)
        assertEquals("STATUS_UNVERIFIED", (malformed.vpn as AvailabilityState.Unavailable).reason)
        assertTrue(malformed.forecast is AvailabilityState.Unavailable)
    }

    @Test
    fun networkFailureMapsEveryFeatureUnavailable() = runTest {
        val result = repository(FakeFeatureStatusRemote(failure = IOException("offline"))).refresh()

        assertAllUnavailable(result, "STATUS_UNREACHABLE")
    }

    @Test
    fun missingAuthenticationDoesNotCallBackend() = runTest {
        val remote = FakeFeatureStatusRemote(response = completeResponse("UNAVAILABLE", null))
        val repository = FeatureAvailabilityRepository(
            tokenProvider = FeatureAuthTokenProvider { null },
            remote = remote,
            now = { now },
        )

        val result = repository.refresh()

        assertAllUnavailable(result, "AUTH_REQUIRED")
        assertEquals(0, remote.calls)
    }

    private fun repository(remote: FakeFeatureStatusRemote) = FeatureAvailabilityRepository(
        tokenProvider = FeatureAuthTokenProvider { "token" },
        remote = remote,
        now = { now },
    )

    private fun assertAllUnavailable(result: AdvancedFeatureAvailability, reason: String) {
        result.all().forEach {
            assertEquals(reason, (it as AvailabilityState.Unavailable).reason)
        }
    }

    private fun completeResponse(status: String, freshness: String?): String {
        val freshnessField = freshness?.let { "\"dataFreshness\":\"$it\"," }.orEmpty()
        val module = "{\"status\":\"$status\",${freshnessField}\"reason\":\"NO_EVIDENCE\"}"
        return "{\"vpn\":$module,\"forecast\":$module,\"threatMap\":$module," +
            "\"automations\":$module,\"reports\":$module,\"policies\":$module}"
    }
}

private class FakeFeatureStatusRemote(
    private val response: String = "",
    private val failure: IOException? = null,
) : FeatureStatusRemoteDataSource {
    var calls: Int = 0
    var receivedToken: String? = null

    override suspend fun fetch(authToken: String): String {
        calls += 1
        receivedToken = authToken
        failure?.let { throw it }
        return response
    }
}
