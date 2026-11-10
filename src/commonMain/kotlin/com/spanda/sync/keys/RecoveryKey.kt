package com.spanda.sync.keys

import com.spanda.sync.crypto.CryptoPrimitives

/**
 * The recovery key (SPEC.md §4.2): 16 random bytes, shown to the user once as 28 Crockford base32
 * characters in 7 groups of 4, with a 12-bit checksum so typos are caught before any network call.
 *
 * It is not a password: 128 random bits need no slow key derivation. Spanda never stores it.
 */
class RecoveryKey private constructor(internal val bytes: ByteArray) {

    /** The 28-character display form, e.g. `7K3Q-M9PA-…-XW2E`. */
    fun display(crypto: CryptoPrimitives): String =
        TODO("RK || first 12 bits of SHA-256(RK), Crockford base32, grouped by 4")

    /** Key-encryption key: HKDF(RK, salt = accountId, info = "spanda-sync/v1/recovery-kek"). */
    fun kek(crypto: CryptoPrimitives, account: AccountId): ByteArray = TODO("SPEC.md §4.1")

    override fun toString() = "RecoveryKey(<redacted>)"

    companion object {
        fun generate(crypto: CryptoPrimitives): RecoveryKey = TODO("16 bytes from crypto.randomBytes")

        /**
         * Parses what the user typed. Ignores case, spaces and hyphens; maps O→0, I/L→1.
         * Returns [ParseResult.Typo] if the checksum fails, so the app can say "check for typos"
         * instead of trying to decrypt with a wrong key.
         */
        fun parse(input: String, crypto: CryptoPrimitives): ParseResult = TODO("SPEC.md §4.2")
    }

    sealed interface ParseResult {
        data class Ok(val key: RecoveryKey) : ParseResult
        data object Typo : ParseResult
        data object WrongLength : ParseResult
    }
}
