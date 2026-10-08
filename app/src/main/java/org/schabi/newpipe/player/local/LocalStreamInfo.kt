package org.schabi.newpipe.player.local

import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.player.mediaitem.PlayerMediaItem

/**
 * A stream-less [StreamInfo] for a downloaded item, so history, resume and the notification work
 * without extraction. Stored [StreamEntity] values win over the queue item's: history writes upsert
 * an entity built from this info, and queue items carry no view count or upload date, which would
 * otherwise be wiped.
 */
object LocalStreamInfo {
    @JvmStatic
    fun from(item: PlayerMediaItem, entity: StreamEntity?): StreamInfo {
        val info = StreamInfo(
            item.serviceId,
            item.url,
            item.url,
            entity?.streamType ?: item.streamType,
            idOf(item),
            entity?.title ?: item.title,
            0,
        )
        info.setUploaderName(entity?.uploader ?: item.uploader)
        info.setUploaderUrl(entity?.uploaderUrl ?: item.uploaderUrl)
        info.setThumbnailUrl(entity?.thumbnailUrl ?: item.thumbnailUrl)
        info.setDuration(entity?.duration?.takeIf { it > 0 } ?: item.duration)
        info.setRoundPlayStream(item.isRoundPlayStream)
        if (entity != null) {
            entity.viewCount?.let { info.setViewCount(it) }
            entity.textualUploadDate?.let { info.setTextualUploadDate(it) }
            entity.uploadDate?.let {
                info.setUploadDate(DateWrapper(it, entity.isUploadDateApproximation == true))
            }
            info.setRequiresMembership(entity.isPaid)
        }
        return info
    }

    private fun idOf(item: PlayerMediaItem): String = try {
        NewPipe.getService(item.serviceId).streamLHFactory.getId(item.url)
    } catch (e: Exception) {
        item.url
    }
}
