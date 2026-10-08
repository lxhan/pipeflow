package org.schabi.newpipe.util

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService

/**
 * This fork only uses YouTube. Other services are hidden instead of removed, because they live in
 * the untouched extractor and service ids are list indexes.
 */
object ServiceFilter {
    @JvmStatic
    fun isHidden(serviceId: Int): Boolean = serviceId != ServiceList.YouTube.serviceId

    @JvmStatic
    fun visibleServices(): List<StreamingService> =
        NewPipe.getServices().filterNot { isHidden(it.serviceId) }
}
