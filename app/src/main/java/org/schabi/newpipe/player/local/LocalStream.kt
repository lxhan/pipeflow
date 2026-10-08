package org.schabi.newpipe.player.local

import org.schabi.newpipe.database.stream.model.StreamEntity

/**
 * Downloaded files for one stream. Paths stay strings (file://, content:// or a bare legacy
 * path) so selection can be tested without Android's Uri.
 */
class LocalStream(
    val audioPath: String?,
    val videoPath: String?,
    val entity: StreamEntity?,
) {
    fun pick(audioOnly: Boolean): String? =
        if (audioOnly) audioPath ?: videoPath else videoPath ?: audioPath

    companion object {
        private const val AUDIO = 'a'
        private const val VIDEO = 'v'

        /**
         * @param files rows for one url, newest first
         */
        @JvmStatic
        fun fromFiles(
            files: List<DownloadedFile>,
            exists: (String) -> Boolean,
            entity: StreamEntity?,
        ): LocalStream? {
            val audio = files.firstOrNull { it.kind == AUDIO && exists(it.path) }?.path
            val video = files.firstOrNull { it.kind == VIDEO && exists(it.path) }?.path
            return if (audio == null && video == null) null else LocalStream(audio, video, entity)
        }
    }
}
