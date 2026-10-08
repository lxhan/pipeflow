package org.schabi.newpipe.util

import android.content.Context
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.settings.tabs.Tab

/**
 * Kiosks this fork never shows. "Recommended Lives" is YouTube's only and default kiosk. It is
 * hidden here instead of removed from the extractor, because a service without a default kiosk
 * makes KioskList fall back to an unrelated link handler.
 */
object KioskFilter {
    private const val RECOMMENDED_LIVES = "Recommended Lives"

    @JvmStatic
    fun isHidden(kioskId: String?): Boolean = kioskId == RECOMMENDED_LIVES

    @JvmStatic
    @Throws(ExtractionException::class)
    fun visibleKiosks(service: StreamingService): List<String> =
        service.kioskList.availableKiosks.filterNot(::isHidden)

    @JvmStatic
    fun filterTabs(tabs: List<Tab>, defaultKioskId: String?): List<Tab> =
        tabs.filterNot { tab ->
            when (tab) {
                is Tab.KioskTab -> isHidden(tab.kioskId)
                is Tab.DefaultKioskTab -> isHidden(defaultKioskId)
                else -> false
            }
        }

    @JvmStatic
    fun selectedServiceDefaultKioskId(context: Context): String? = try {
        NewPipe.getService(ServiceHelper.getSelectedServiceId(context)).kioskList.defaultKioskId
    } catch (e: ExtractionException) {
        null
    }
}
