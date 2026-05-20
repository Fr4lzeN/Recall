package com.recall.app.core.common

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class RecallDispatchersTest {

    @Test
    fun `DefaultRecallDispatchers returns correct dispatchers`() {
        val dispatchers = DefaultRecallDispatchers()
        assertEquals(Dispatchers.IO, dispatchers.io)
        assertEquals(Dispatchers.Default, dispatchers.default)
        assertEquals(Dispatchers.Main, dispatchers.main)
    }
}
