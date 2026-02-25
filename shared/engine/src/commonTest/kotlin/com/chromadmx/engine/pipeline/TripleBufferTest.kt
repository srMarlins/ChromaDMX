package com.chromadmx.engine.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TripleBufferTest {

    @Test
    fun initialReadReturnsSlotC() {
        val buf = TripleBuffer("A", "B", "C")
        // Read slot starts at index 2 = initialC
        assertEquals("C", buf.readSlot())
    }

    @Test
    fun writeAndReadPropagates() {
        val buf = TripleBuffer(0, 0, 0)
        buf.write(42)
        val result = buf.read()
        assertEquals(42, result)
    }

    @Test
    fun multipleWritesOnlyLatestVisible() {
        val buf = TripleBuffer(0, 0, 0)
        buf.write(1)
        buf.write(2)
        buf.write(3)
        val result = buf.read()
        assertEquals(3, result)
    }

    @Test
    fun swapReadReturnsFalseWhenNothingNew() {
        val buf = TripleBuffer("A", "B", "C")
        // No writes have happened, dirty=false
        assertFalse(buf.swapRead())
    }

    @Test
    fun swapReadReturnsTrueWhenDirty() {
        val buf = TripleBuffer(0, 0, 0)
        buf.write(10)
        assertTrue(buf.swapRead())
        // Second swap should return false (already consumed)
        assertFalse(buf.swapRead())
    }

    @Test
    fun readAfterMultipleWriteReadCycles() {
        val buf = TripleBuffer(0, 0, 0)

        // Cycle 1
        buf.write(100)
        assertEquals(100, buf.read())

        // Cycle 2
        buf.write(200)
        assertEquals(200, buf.read())

        // Cycle 3
        buf.write(300)
        assertEquals(300, buf.read())
    }

    @Test
    fun writeSlotAndSetWriteSlot() {
        val buf = TripleBuffer("A", "B", "C")
        // Write slot starts at index 0 = "A"
        assertEquals("A", buf.writeSlot())
        buf.setWriteSlot("X")
        assertEquals("X", buf.writeSlot())
    }

    @Test
    fun worksWithArrayPayload() {
        val a = intArrayOf(0, 0, 0)
        val b = intArrayOf(0, 0, 0)
        val c = intArrayOf(0, 0, 0)
        val buf = TripleBuffer(a, b, c)

        // Writer fills the write slot in place
        val ws = buf.writeSlot()
        ws[0] = 10
        ws[1] = 20
        ws[2] = 30
        buf.swapWrite()

        // Reader picks it up
        buf.swapRead()
        val rs = buf.readSlot()
        assertEquals(10, rs[0])
        assertEquals(20, rs[1])
        assertEquals(30, rs[2])
    }

    @Test
    fun rapidWriteReadCyclesStayConsistent() {
        val buf = TripleBuffer(0, 0, 0)
        for (i in 1..1000) {
            buf.write(i)
            val v = buf.read()
            assertEquals(i, v)
        }
    }
}
