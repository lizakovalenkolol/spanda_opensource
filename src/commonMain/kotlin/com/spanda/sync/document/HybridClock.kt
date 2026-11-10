package com.spanda.sync.document

/**
 * A timestamp for one write (SPEC.md §6.2): wall-clock milliseconds, a counter for writes in the
 * same millisecond, and the writing device. Ordered by (wall, counter, device).
 */
data class Stamp(val wall: Long, val counter: Int, val device: DeviceId) : Comparable<Stamp> {
    override fun compareTo(other: Stamp): Int = TODO("compare wall, then counter, then device bytes")
}

/** A random 16-byte id created once per install (SPEC.md §4), as base64url text. */
data class DeviceId(val value: String)

/**
 * Hybrid logical clock (SPEC.md §6.2). A device whose clock runs behind still produces timestamps
 * newer than anything it has seen, so its edits aren't silently lost to older-looking stamps.
 */
class HybridClock(private val device: DeviceId, private val now: () -> Long) {
    /** Next stamp for a local write: `w = max(now, wLast)`, counter +1 if `w == wLast`, else 0. */
    fun tick(): Stamp = TODO("SPEC.md §6.2")

    /** After merging a remote document: move past the newest stamp it contained. */
    fun observe(remote: Stamp): Unit = TODO("SPEC.md §6.2")
}
