package dev.proofly.ledgermem

import dev.proofly.ledgermem.services.LedgerMemService
import dev.proofly.ledgermem.services.Memory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LedgerMemServiceTest {
    @Test
    fun `service exposes config keys as constants`() {
        assertEquals("ledgermem.apiKey", LedgerMemService.KEY_API_KEY)
        assertEquals("ledgermem.workspaceId", LedgerMemService.KEY_WORKSPACE)
        assertEquals("ledgermem.endpoint", LedgerMemService.KEY_ENDPOINT)
        assertEquals("ledgermem.defaultLimit", LedgerMemService.KEY_LIMIT)
    }

    @Test
    fun `Memory data class round-trips`() {
        val m = Memory(id = "a", content = "hello", createdAt = "2026-01-01", score = 0.91)
        assertEquals("a", m.id)
        assertEquals("hello", m.content)
        assertEquals(0.91, m.score)
        // copy() works (data class)
        val m2 = m.copy(content = "world")
        assertEquals("world", m2.content)
        assertEquals("a", m2.id)
    }

    @Test
    fun `plugin metadata is well-formed`() {
        assertEquals("dev.proofly.ledgermem", LedgerMemPlugin.PLUGIN_ID)
        assertEquals("LedgerMem", LedgerMemPlugin.DISPLAY_NAME)
        assertNotNull(LedgerMemPlugin.log)
    }
}
