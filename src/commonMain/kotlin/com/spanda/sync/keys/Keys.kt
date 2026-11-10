package com.spanda.sync.keys

import com.spanda.sync.crypto.CryptoPrimitives
import com.spanda.sync.crypto.Labels

/** The account id assigned by the server at account creation (SPEC.md §4), as base64url text. */
data class AccountId(val value: String)

/**
 * The account's data key (SPEC.md §4): 32 random bytes created on the first device.
 * It is never sent anywhere unencrypted and never used directly as an encryption key:
 * each use derives its own key (§4.1).
 */
class DataKey(internal val bytes: ByteArray, val generation: Int) {
    init {
        require(bytes.size == 32) { "data key must be 32 bytes" }
        require(generation >= 1) { "key generation starts at 1" }
    }

    /** Key for encrypting the blob: HKDF(DK, salt = accountId, info = "spanda-sync/v1/blob-key"). */
    fun blobKey(crypto: CryptoPrimitives, account: AccountId): ByteArray =
        TODO("SPEC.md §4.1 using ${Labels.BLOB_KEY}")

    override fun toString() = "DataKey(generation=$generation, bytes=<redacted>)"

    companion object {
        /** A new random data key for a new account or a key rotation (SPEC.md §5.5). */
        fun generate(crypto: CryptoPrimitives, generation: Int = 1): DataKey =
            TODO("32 bytes from crypto.randomBytes")
    }
}
