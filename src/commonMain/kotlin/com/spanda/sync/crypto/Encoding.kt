package com.spanda.sync.crypto

/**
 * Byte encodings shared by every format (SPEC.md §3.1): big-endian integers, length-prefixed
 * fields, base64url without padding and Crockford base32.
 */
internal object Encoding {
    /** `u16(len(x)) || x`. Fails if [bytes] is longer than 65535. */
    fun lengthPrefixed(bytes: ByteArray): ByteArray = TODO("SPEC.md §3.1 lp(x)")

    fun u32(value: Long): ByteArray = TODO("4-byte big-endian")
    fun u64(value: Long): ByteArray = TODO("8-byte big-endian")

    fun base64Url(bytes: ByteArray): String = TODO("RFC 4648 §5, no padding")
    fun fromBase64Url(text: String): ByteArray = TODO("reject padding and non-alphabet characters")

    /** Crockford base32, upper case, no padding. Used for the recovery key and link codes. */
    fun crockford(bits: ByteArray, bitCount: Int): String = TODO("SPEC.md §4.2")

    /** Accepts lower case, spaces and hyphens; maps O→0 and I/L→1. */
    fun fromCrockford(text: String, bitCount: Int): ByteArray = TODO("SPEC.md §4.2")
}
