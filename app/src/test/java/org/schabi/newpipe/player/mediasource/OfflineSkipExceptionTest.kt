package org.schabi.newpipe.player.mediasource

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class OfflineSkipExceptionTest {
    @Test
    fun emptyIsNotOfflineSkip() {
        assertFalse(OfflineSkipException.onlyOfflineSkips(emptyList()))
    }

    @Test
    fun onlyOfflineSkips() {
        assertTrue(
            OfflineSkipException.onlyOfflineSkips(
                listOf<Exception>(OfflineSkipException(IOException()))
            )
        )
    }

    @Test
    fun mixedErrorsAreNotOfflineSkip() {
        assertFalse(
            OfflineSkipException.onlyOfflineSkips(
                listOf<Exception>(OfflineSkipException(IOException()), IOException())
            )
        )
    }
}
