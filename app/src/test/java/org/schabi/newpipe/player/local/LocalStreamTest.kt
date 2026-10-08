package org.schabi.newpipe.player.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalStreamTest {
    private val existing = setOf(
        "file:///a-new.m4a", "file:///a-old.m4a", "file:///v-old.mp4", "content://dl/v-new.mp4"
    )
    private val exists: (String) -> Boolean = { it in existing }

    @Test
    fun fromFilesReturnsNullWhenNothingPlayable() {
        val files = listOf(
            DownloadedFile("file:///subs.srt", 's'),
            DownloadedFile("file:///a-new.m4a", '?'),
            DownloadedFile("file:///gone.m4a", 'a'),
        )
        assertNull(LocalStream.fromFiles(files, exists, null))
    }

    @Test
    fun fromFilesTakesNewestExistingFilePerKind() {
        // findByUrl returns rows newest first
        val files = listOf(
            DownloadedFile("file:///gone.m4a", 'a'),
            DownloadedFile("file:///a-new.m4a", 'a'),
            DownloadedFile("file:///a-old.m4a", 'a'),
            DownloadedFile("content://dl/v-new.mp4", 'v'),
            DownloadedFile("file:///v-old.mp4", 'v'),
        )
        val local = LocalStream.fromFiles(files, exists, null)!!
        assertEquals("file:///a-new.m4a", local.audioPath)
        assertEquals("content://dl/v-new.mp4", local.videoPath)
    }

    @Test
    fun fromFilesKeepsSingleKind() {
        val local = LocalStream.fromFiles(
            listOf(DownloadedFile("file:///v-old.mp4", 'v')), exists, null
        )!!
        assertNull(local.audioPath)
        assertEquals("file:///v-old.mp4", local.videoPath)
    }

    @Test
    fun pickPrefersKindMatchingMode() {
        val local = LocalStream("a.m4a", "v.mp4", null)
        assertEquals("a.m4a", local.pick(true))
        assertEquals("v.mp4", local.pick(false))
    }

    @Test
    fun pickFallsBackToOtherKind() {
        assertEquals("a.m4a", LocalStream("a.m4a", null, null).pick(false))
        assertEquals("v.mp4", LocalStream(null, "v.mp4", null).pick(true))
    }
}
