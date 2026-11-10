package com.spanda.sync.blob

/**
 * Padding before encryption (SPEC.md §7.1), so the server learns a size bucket rather than how much
 * the user has logged: `u32(len) || doc || zeros`, padded to the smallest 4096 × 2^k that fits.
 */
object Padding {
    const val MIN_BUCKET = 4096
    const val MAX_DOCUMENT = 4 * 1024 * 1024

    fun pad(document: ByteArray): ByteArray = TODO("SPEC.md §7.1; reject documents over MAX_DOCUMENT")

    /** Checks the bucket size, the length field and that every padding byte is zero. */
    fun unpad(frame: ByteArray): ByteArray = TODO("SPEC.md §7.1")
}
