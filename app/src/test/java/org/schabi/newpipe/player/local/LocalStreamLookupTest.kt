package org.schabi.newpipe.player.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalStreamLookupTest {
    private val watchUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    @Test
    fun musicUrlAlsoTriesNormalizedUrl() {
        val music = "https://music.youtube.com/watch?v=dQw4w9WgXcQ"
        val urls = LocalStreamLookup.candidateUrls(0, music)
        assertTrue(urls.contains(music))
        assertTrue(urls.contains(watchUrl))
    }

    @Test
    fun shortsUrlAlsoTriesNormalizedUrl() {
        val urls = LocalStreamLookup.candidateUrls(0, "https://youtube.com/shorts/dQw4w9WgXcQ")
        assertTrue(urls.contains(watchUrl))
    }

    @Test
    fun normalizedUrlIsNotDuplicated() {
        assertEquals(listOf(watchUrl), LocalStreamLookup.candidateUrls(0, watchUrl))
    }

    @Test
    fun unknownServiceKeepsInputUrl() {
        assertEquals(listOf(watchUrl), LocalStreamLookup.candidateUrls(-1, watchUrl))
    }
}
