package com.echo.innertube

import com.echo.innertube.models.YouTubeClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    @Test
    fun testArtistPage() {
        runBlocking {
            val result = YouTube.artist("UCoIOOL7QKuBhQHVKL8y7BEQ")
            assertTrue("Artist page should succeed", result.isSuccess)
            val page = result.getOrNull()
            assertNotNull("Artist page should not be null", page)
            assertTrue("Artist should have sections", page!!.sections.isNotEmpty())
        }
    }

    @Test
    fun testPlaybackStreamResolution() {
        runBlocking {
            val videoId = "dQw4w9WgXcQ"
            val result = YouTube.player(videoId, null, YouTubeClient.IPADOS)
            assertTrue("IPADOS player request should succeed", result.isSuccess)
            val playerResponse = result.getOrNull()
            assertNotNull(playerResponse)
            assertTrue("Status should be OK", playerResponse!!.playabilityStatus.status == "OK")
            val audioFormat = playerResponse.streamingData?.adaptiveFormats?.firstOrNull { it.mimeType.startsWith("audio/") }
            assertNotNull("Should have audio format", audioFormat)
            assertNotNull("Should have direct audio URL", audioFormat?.url)
        }
    }
}
