package com.royalshield.app.data

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class ThreatBriefingRepositoryTest {

    @Test
    fun fetchLiveBriefings_validJson_parsesCorrectlyWithoutSimulation() = runBlocking {
        val json = """
            {
                "status": "READY",
                "source": "OTX",
                "results": [
                    {
                        "id": "pulse_abc123",
                        "name": "Targeted Phishing via Fake Booking Portal",
                        "description": "Adversary deploying cloned hospitality portals to steal payment credentials.",
                        "author_name": "AlienVault",
                        "created": "2026-11-09T12:00:00.000000",
                        "tags": ["phishing", "credential theft"],
                        "targeted_countries": ["US", "JP"],
                        "indicators": [
                            {"id": 1, "indicator": "fake-booking-portal.com", "type": "domain"},
                            {"id": 2, "indicator": "192.0.2.1", "type": "IPv4"}
                        ]
                    }
                ]
            }
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val repo = ThreatBriefingRepository(client = client, baseUrl = "https://test.api")
        val result = repo.fetchLiveBriefings()

        assertTrue(result.isSuccess)
        val briefings = result.getOrThrow()
        assertEquals(1, briefings.size)
        val first = briefings[0]
        assertEquals("pulse_abc123", first.id)
        assertEquals("Targeted Phishing via Fake Booking Portal", first.title)
        assertEquals("Adversary deploying cloned hospitality portals to steal payment credentials.", first.description)
        assertEquals("AlienVault", first.author)
        assertEquals("2026-11-09", first.date)
        assertEquals(listOf("phishing", "credential theft"), first.tags)
        assertEquals(2, first.indicatorsCount)
        assertEquals(listOf("US", "JP"), first.targetedCountries)
    }

    @Test
    fun fetchLiveBriefings_httpError_returnsFailureWithoutMockFallback() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(503)
                    .message("Service Unavailable")
                    .body("Down for maintenance".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        val repo = ThreatBriefingRepository(client = client, baseUrl = "https://test.api")
        val result = repo.fetchLiveBriefings()

        assertTrue(result.isFailure)
    }

    @Test
    fun fetchLiveBriefings_emptyResults_returnsFailureWithoutSyntheticData() = runBlocking {
        val json = """{"status": "READY", "source": "OTX", "results": []}"""

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val repo = ThreatBriefingRepository(client = client, baseUrl = "https://test.api")
        val result = repo.fetchLiveBriefings()

        assertTrue(result.isFailure)
    }
}

