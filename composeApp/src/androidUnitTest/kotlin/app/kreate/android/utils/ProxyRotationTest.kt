package app.kreate.android.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProxyRotationTest {

    private val a = ProxyEntry(host = "a.example", port = 1)
    private val b = ProxyEntry(host = "b.example", port = 2)
    private val c = ProxyEntry(host = "c.example", port = 3)

    @Test
    fun `next wraps around past the last entry`() {
        val rotation = ProxyRotation(listOf(a, b, c), activeIndex = 2)
        assertEquals(0, rotation.next(from = 2).activeIndex)
    }

    @Test
    fun `next with two proxies alternates back and forth`() {
        var rotation = ProxyRotation(listOf(a, b), activeIndex = 0)
        rotation = rotation.next(from = 0)
        assertEquals(1, rotation.activeIndex)
        rotation = rotation.next(from = 1)
        assertEquals(0, rotation.activeIndex)
        rotation = rotation.next(from = 0)
        assertEquals(1, rotation.activeIndex)
    }

    @Test
    fun `next is a no-op with zero entries`() {
        val rotation = ProxyRotation(emptyList(), activeIndex = 0)
        assertEquals(0, rotation.next(from = 0).activeIndex)
        assertNull(rotation.active)
    }

    @Test
    fun `next is a no-op with a single entry`() {
        val rotation = ProxyRotation(listOf(a), activeIndex = 0)
        assertEquals(0, rotation.next(from = 0).activeIndex)
    }

    @Test
    fun `next collapses a concurrent switch instead of double-advancing`() {
        // Pointer has already moved to 1 (e.g. a concurrent connectFailed); a stale caller
        // still holding "from = 0" must not advance it a second time.
        val rotation = ProxyRotation(listOf(a, b, c), activeIndex = 1)
        assertEquals(1, rotation.next(from = 0).activeIndex)
    }

    @Test
    fun `withEntries clamps an out-of-range active index`() {
        val rotation = ProxyRotation(listOf(a, b, c), activeIndex = 2)
        val shrunk = rotation.withEntries(listOf(a))
        assertEquals(0, shrunk.activeIndex)
        assertEquals(a, shrunk.active)
    }

    @Test
    fun `withEntries clamps to zero for an empty list`() {
        val rotation = ProxyRotation(listOf(a, b, c), activeIndex = 2)
        val emptied = rotation.withEntries(emptyList())
        assertEquals(0, emptied.activeIndex)
        assertNull(emptied.active)
    }

    @Test
    fun `withEntries keeps a still-valid active index`() {
        val rotation = ProxyRotation(listOf(a, b, c), activeIndex = 1)
        val replaced = rotation.withEntries(listOf(a, b, c, a))
        assertEquals(1, replaced.activeIndex)
    }
}
