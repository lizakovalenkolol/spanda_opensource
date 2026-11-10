package com.spanda.sync.document

/**
 * The synced data (SPEC.md §6): a map from key to [Entry], each with a [Stamp]. A deleted entry is
 * kept as a tombstone (`value == null`), so a device that hadn't seen the deletion can't bring it back.
 *
 * Keys and values are opaque strings to this library; the app decides what they mean.
 * Spanda's mapping (`period/YYYY-MM-DD`, `note/<uuid>`, `setting/…`) is listed in SPEC.md §6.4.
 */
data class SyncDocument(val entries: Map<String, Entry> = emptyMap()) {

    data class Entry(val stamp: Stamp, val value: String?) {
        val isTombstone: Boolean get() = value == null
    }

    /** Live values only, without tombstones: what the app shows. */
    val values: Map<String, String> get() = TODO("entries minus tombstones")

    fun put(key: String, value: String, clock: HybridClock): SyncDocument = TODO("new stamp from clock.tick()")

    fun delete(key: String, clock: HybridClock): SyncDocument = TODO("tombstone with a new stamp")

    /**
     * Merge (SPEC.md §6.3): for every key in either document, keep the entry with the greatest stamp.
     * Commutative, associative and idempotent, so devices converge whatever order they sync in.
     */
    fun merge(other: SyncDocument): SyncDocument = TODO("SPEC.md §6.3")

    /** Canonical UTF-8 JSON: keys sorted, no insignificant whitespace (SPEC.md §6.1, §7.1). */
    fun encode(): ByteArray = TODO("SPEC.md §6.1")

    companion object {
        fun decode(bytes: ByteArray): SyncDocument = TODO("reject unknown v")
    }
}
