package com.chromadmx.engine.pipeline

import kotlinx.atomicfu.atomic

/**
 * Lock-free triple buffer for single-writer / single-reader communication.
 *
 * Three slots rotate through three roles:
 *   - **write**: the writer fills this slot (never visible to the reader).
 *   - **shared**: the most recently completed write, waiting for the reader.
 *   - **read**: the reader consumes this slot (never visible to the writer).
 *
 * After the writer finishes a frame it calls [swapWrite] to publish the
 * write slot as the new shared slot.  The reader calls [swapRead] to
 * exchange its read slot for the latest shared slot.
 *
 * The atomic integer [shared] stores the index of the shared slot and a
 * "dirty" flag (bit 2) indicating the writer has published something
 * the reader hasn't consumed yet.  This is the *only* synchronisation
 * primitive; there are no locks.
 */
class TripleBuffer<T>(
    initialA: T,
    initialB: T,
    initialC: T
) {
    /** The three data slots. */
    private val slots: Array<Any?> = arrayOf(initialA, initialB, initialC)

    /** Index of the slot the writer is currently filling. */
    private var writeIdx: Int = 0

    /**
     * Packed shared state: bits 0-1 = shared slot index, bit 2 = dirty flag.
     * Only mutated through atomic CAS.
     */
    private val shared = atomic(pack(1, false))  // slot 1 shared, not dirty

    /** Index of the slot the reader is currently consuming. */
    private var readIdx: Int = 2

    /* ------------------------------------------------------------------ */
    /*  Writer API (call from the producer / engine thread only)           */
    /* ------------------------------------------------------------------ */

    /** Get the current write slot value so the writer can mutate it in place. */
    @Suppress("UNCHECKED_CAST")
    fun writeSlot(): T = slots[writeIdx] as T

    /** Replace the current write slot contents. */
    fun setWriteSlot(value: T) {
        slots[writeIdx] = value
    }

    /**
     * Publish the write slot: atomically swap it with the shared slot and
     * mark the buffer dirty.
     */
    fun swapWrite() {
        var old: Int
        var new: Int
        do {
            old = shared.value
            val sharedIdx = unpackIndex(old)
            // New shared becomes our writeIdx (with data), mark dirty
            new = pack(writeIdx, true)
            // We'll take the old shared slot as our next write slot
            writeIdx = sharedIdx
        } while (!shared.compareAndSet(old, new))
    }

    /* ------------------------------------------------------------------ */
    /*  Reader API (call from the consumer thread only)                    */
    /* ------------------------------------------------------------------ */

    /**
     * Get the current read slot value.
     */
    @Suppress("UNCHECKED_CAST")
    fun readSlot(): T = slots[readIdx] as T

    /**
     * If new data is available (dirty flag set), exchange the read slot
     * with the shared slot.  Returns true if the read slot was updated.
     */
    fun swapRead(): Boolean {
        var old: Int
        var new: Int
        do {
            old = shared.value
            if (!unpackDirty(old)) return false   // nothing new
            val sharedIdx = unpackIndex(old)
            new = pack(readIdx, false)
            readIdx = sharedIdx
        } while (!shared.compareAndSet(old, new))
        return true
    }

    /* ------------------------------------------------------------------ */
    /*  Convenience write + publish                                        */
    /* ------------------------------------------------------------------ */

    /** Set the write slot to [value] and immediately publish. */
    fun write(value: T) {
        setWriteSlot(value)
        swapWrite()
    }

    /** Read the latest published value (swaps if dirty, then returns). */
    fun read(): T {
        swapRead()
        return readSlot()
    }

    /* ------------------------------------------------------------------ */
    /*  Bit packing helpers                                                */
    /* ------------------------------------------------------------------ */

    private fun pack(index: Int, dirty: Boolean): Int =
        (index and 0x3) or (if (dirty) 0x4 else 0)

    private fun unpackIndex(packed: Int): Int = packed and 0x3

    private fun unpackDirty(packed: Int): Boolean = (packed and 0x4) != 0
}
