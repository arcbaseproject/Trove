package com.trove.app.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {

    @Test
    fun `newer versions win`() {
        assertTrue(isNewer("0.2.0", "0.1.0"))
        assertTrue(isNewer("v0.10.0", "0.9.9"))
        assertTrue(isNewer("1.0", "0.9.9"))
    }

    @Test
    fun `same or older versions lose`() {
        assertFalse(isNewer("0.1.0", "0.1.0"))
        assertFalse(isNewer("v0.1.0", "0.1.0"))
        assertFalse(isNewer("0.0.9", "0.1.0"))
        assertFalse(isNewer("0.1.0-beta1", "0.1.0"))
        assertFalse(isNewer("", "0.1.0"))
    }
}
