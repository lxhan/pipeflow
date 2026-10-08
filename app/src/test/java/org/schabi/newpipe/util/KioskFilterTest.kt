package org.schabi.newpipe.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.settings.tabs.Tab

class KioskFilterTest {
    private val lives = "Recommended Lives"

    @Test
    fun isHiddenMatchesOnlyRecommendedLives() {
        assertTrue(KioskFilter.isHidden(lives))
        assertFalse(KioskFilter.isHidden("Trending"))
        assertFalse(KioskFilter.isHidden(null))
    }

    @Test
    fun filterTabsDropsHiddenKioskTab() {
        val trending = Tab.KioskTab(5, "Trending")
        val result = KioskFilter.filterTabs(listOf(Tab.KioskTab(0, lives), trending), null)
        assertEquals(listOf<Tab>(trending), result)
    }

    @Test
    fun filterTabsDropsDefaultKioskTabWhenDefaultIsHidden() {
        val subscriptions = Tab.SubscriptionsTab()
        val result = KioskFilter.filterTabs(listOf(Tab.DefaultKioskTab(), subscriptions), lives)
        assertEquals(listOf<Tab>(subscriptions), result)
    }

    @Test
    fun filterTabsKeepsDefaultKioskTabWhenDefaultIsVisible() {
        val defaultKiosk = Tab.DefaultKioskTab()
        val result = KioskFilter.filterTabs(listOf(defaultKiosk), "Trending")
        assertEquals(listOf<Tab>(defaultKiosk), result)
    }

    @Test
    fun filterTabsReturnsEmptyWhenOnlyHiddenTabs() {
        val result = KioskFilter.filterTabs(
            listOf(Tab.KioskTab(0, lives), Tab.DefaultKioskTab()), lives
        )
        assertTrue(result.isEmpty())
    }
}
