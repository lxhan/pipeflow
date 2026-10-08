package org.schabi.newpipe.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.ServiceList

class ServiceFilterTest {
    @Test
    fun youtubeIsNotHidden() {
        assertFalse(ServiceFilter.isHidden(ServiceList.YouTube.serviceId))
    }

    @Test
    fun everyOtherServiceIsHidden() {
        ServiceList.all()
            .filter { it.serviceId != ServiceList.YouTube.serviceId }
            .forEach { assertTrue(it.serviceInfo.name, ServiceFilter.isHidden(it.serviceId)) }
    }

    @Test
    fun visibleServicesIsYoutubeOnly() {
        assertEquals(listOf(ServiceList.YouTube), ServiceFilter.visibleServices())
    }
}
