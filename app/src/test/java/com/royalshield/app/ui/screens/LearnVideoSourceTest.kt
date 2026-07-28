package com.royalshield.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnVideoSourceTest {
    @Test
    fun parsesStandardYouTubeUrl() {
        val source = parseLearnVideoSource("https://www.youtube.com/watch?v=inWWhr5tnEA")
        assertTrue(source is LearnVideoSource.YouTube)
        assertEquals("inWWhr5tnEA", (source as LearnVideoSource.YouTube).videoId)
    }

    @Test
    fun parsesShortYouTubeUrlWithQueryParameters() {
        val source = parseLearnVideoSource("https://youtu.be/inWWhr5tnEA?t=10")
        assertEquals("inWWhr5tnEA", (source as LearnVideoSource.YouTube).videoId)
    }

    @Test
    fun acceptsOnlyHttpsDirectVideo() {
        assertTrue(parseLearnVideoSource("https://cdn.example.com/lesson.mp4") is LearnVideoSource.Direct)
        assertNull(parseLearnVideoSource("http://cdn.example.com/lesson.mp4"))
    }

    @Test
    fun doesNotTreatLookalikeDomainAsYouTube() {
        val source = parseLearnVideoSource("https://youtube.com.attacker.example/watch?v=inWWhr5tnEA")
        assertTrue(source is LearnVideoSource.External)
    }

    @Test
    fun rejectsBlankAndMalformedUrls() {
        assertNull(parseLearnVideoSource(null))
        assertNull(parseLearnVideoSource(""))
        assertNull(parseLearnVideoSource("not a url"))
    }
}
