package org.schabi.newpipe.player.local

/**
 * One finished_missions row: the file location and its kind ('a' audio, 'v' video, 's' subtitles).
 */
data class DownloadedFile(val path: String, val kind: Char)
