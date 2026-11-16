package com.surv20.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemoryStoreTest {
    @Test
    fun storesAndRemovesValues() {
        val store = MemoryStore()
        store.putString("a", "1")
        assertEquals("1", store.getString("a"))
        store.putString("a", null)
        assertNull(store.getString("a"))
    }
}
