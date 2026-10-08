package org.schabi.newpipe.player.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.player.mediaitem.PlayerMediaItem
import java.time.OffsetDateTime
import java.time.ZoneOffset

class LocalStreamInfoTest {
    private val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    private val item = PlayerMediaItem.Builder()
        .serviceId(0)
        .url(url)
        .title("Queue title")
        .uploader("Queue uploader")
        .uploaderUrl("https://www.youtube.com/channel/queue")
        .duration(212)
        .thumbnailUrl("https://i.ytimg.com/queue.jpg")
        .streamType(StreamType.VIDEO_STREAM)
        .build()

    @Test
    fun fromItemWhenNoEntity() {
        val info = LocalStreamInfo.from(item, null)
        assertEquals(0, info.serviceId)
        assertEquals(url, info.url)
        assertEquals("dQw4w9WgXcQ", info.id)
        assertEquals("Queue title", info.name)
        assertEquals("Queue uploader", info.uploaderName)
        assertEquals("https://www.youtube.com/channel/queue", info.uploaderUrl)
        assertEquals("https://i.ytimg.com/queue.jpg", info.thumbnailUrl)
        assertEquals(212L, info.duration)
        assertEquals(StreamType.VIDEO_STREAM, info.streamType)
        assertEquals(-1L, info.viewCount)
        assertFalse(info.requiresMembership())
        assertTrue(info.audioStreams.isEmpty())
        assertTrue(info.videoStreams.isEmpty())
        assertTrue(info.relatedItems.isEmpty())
    }

    @Test
    fun fromEntityWhenPresent() {
        val uploadDate = OffsetDateTime.of(2024, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val entity = StreamEntity(
            serviceId = 0,
            url = url,
            title = "Stored title",
            streamType = StreamType.VIDEO_STREAM,
            duration = 213,
            uploader = "Stored uploader",
            uploaderUrl = "https://www.youtube.com/channel/stored",
            thumbnailUrl = "https://i.ytimg.com/stored.jpg",
            viewCount = 1_000_000,
            textualUploadDate = "1 year ago",
            uploadDate = uploadDate,
            isUploadDateApproximation = true,
            isPaid = true,
        )
        val info = LocalStreamInfo.from(item, entity)
        assertEquals("Stored title", info.name)
        assertEquals("Stored uploader", info.uploaderName)
        assertEquals("https://www.youtube.com/channel/stored", info.uploaderUrl)
        assertEquals("https://i.ytimg.com/stored.jpg", info.thumbnailUrl)
        assertEquals(213L, info.duration)
        assertEquals(1_000_000L, info.viewCount)
        assertEquals("1 year ago", info.textualUploadDate)
        assertEquals(uploadDate, info.uploadDate.offsetDateTime())
        assertTrue(info.uploadDate.isApproximation)
        assertTrue(info.requiresMembership())
    }

    @Test
    fun entityWithoutDurationFallsBackToItem() {
        val entity = StreamEntity(
            serviceId = 0, url = url, title = "Stored title",
            streamType = StreamType.VIDEO_STREAM, duration = -1, uploader = "Stored uploader",
        )
        assertEquals(212L, LocalStreamInfo.from(item, entity).duration)
    }

    @Test
    fun idFallsBackToUrlForUnknownService() {
        val unknown = PlayerMediaItem.Builder(item).serviceId(-1).build()
        assertEquals(url, LocalStreamInfo.from(unknown, null).id)
    }
}
