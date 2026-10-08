package org.schabi.newpipe.player.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import org.schabi.newpipe.NewPipeDatabase
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.player.mediaitem.PlayerMediaItem
import us.shandian.giga.get.sqlite.FinishedMissionStore
import java.io.File

/**
 * Finds a downloaded file for a queue item by matching its url against finished downloads.
 * Downloads are stored under the extractor-normalized url, but queue items can carry other forms
 * (music.youtube.com, shorts), so the lookup tries the item's url and its normalized form.
 */
class LocalStreamLookup private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val missionStore = FinishedMissionStore(appContext)
    private val streamTable = NewPipeDatabase.getInstance(appContext).streamDAO()

    fun find(item: PlayerMediaItem): Maybe<LocalStream> =
        Maybe.fromCallable<LocalStream> {
            val files = missionStore.findByUrls(candidateUrls(item.serviceId, item.url))
            if (files.isEmpty()) {
                return@fromCallable null
            }
            val entity = streamTable.getStream(item.serviceId.toLong(), item.url)
                .blockingFirst()
                .firstOrNull()
            LocalStream.fromFiles(files, ::exists, entity)
        }
            .doOnError { Log.w(TAG, "Local lookup failed for " + item.url, it) }
            .onErrorComplete()
            .subscribeOn(Schedulers.io())

    private fun exists(path: String): Boolean {
        val uri = Uri.parse(path)
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT ->
                DocumentFile.fromSingleUri(appContext, uri)?.exists() == true
            ContentResolver.SCHEME_FILE -> uri.path?.let { File(it).exists() } == true
            null -> File(path).exists()
            else -> false
        }
    }

    companion object {
        private const val TAG = "LocalStreamLookup"

        @Volatile
        private var instance: LocalStreamLookup? = null

        @JvmStatic
        fun getInstance(context: Context): LocalStreamLookup =
            instance ?: synchronized(this) {
                instance ?: LocalStreamLookup(context.applicationContext).also { instance = it }
            }

        @JvmStatic
        internal fun candidateUrls(serviceId: Int, url: String): List<String> {
            val normalized = try {
                NewPipe.getService(serviceId).streamLHFactory.fromUrl(url).url
            } catch (e: Exception) {
                return listOf(url)
            }
            return if (normalized == url) listOf(url) else listOf(url, normalized)
        }
    }
}
