package dev.proofly.getmnemo

import dev.proofly.getmnemo.services.MnemoService
import dev.proofly.getmnemo.services.Memory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MnemoServiceTest {
    @Test
    fun `service exposes config keys as constants`() {
        assertEquals("getmnemo.apiKey", MnemoService.KEY_API_KEY)
        assertEquals("getmnemo.workspaceId", MnemoService.KEY_WORKSPACE)
        assertEquals("getmnemo.endpoint", MnemoService.KEY_ENDPOINT)
        assertEquals("getmnemo.defaultLimit", MnemoService.KEY_LIMIT)
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
        assertEquals("dev.proofly.getmnemo", MnemoPlugin.PLUGIN_ID)
        assertEquals("Mnemo", MnemoPlugin.DISPLAY_NAME)
        assertNotNull(MnemoPlugin.log)
    }
}
