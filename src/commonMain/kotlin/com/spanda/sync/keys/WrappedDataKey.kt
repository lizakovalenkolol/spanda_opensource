package com.spanda.sync.keys

import com.spanda.sync.crypto.CryptoPrimitives

/**
 * The data key encrypted with the recovery key (SPEC.md §5.2). This is the only form of the data key
 * the server ever holds; without the recovery key it is useless.
 *
 * ```
 * WrappedDK = "SPWK" || u8(1) || u32(keyGeneration) || nonce(12) || AES-256-GCM(kek, nonce, DK, aad)
 * aad       = "spanda-sync/v1/wrapped-dk" || lp(utf8(accountId)) || u32(keyGeneration)
 * ```
 */
object WrappedDataKey {
    const val MAGIC = "SPWK"
    const val FORMAT_VERSION: Byte = 1

    /** Encrypts [dataKey] under [recoveryKey] for upload at account creation or key regeneration. */
    fun wrap(dataKey: DataKey, recoveryKey: RecoveryKey, account: AccountId, crypto: CryptoPrimitives): ByteArray =
        TODO("SPEC.md §5.2")

    /**
     * Decrypts a downloaded record. Throws [com.spanda.sync.crypto.AuthenticationFailed] if the key
     * is wrong (a valid recovery key from a different account) or the record was tampered with.
     */
    fun unwrap(record: ByteArray, recoveryKey: RecoveryKey, account: AccountId, crypto: CryptoPrimitives): DataKey =
        TODO("check magic, version; derive kek; open; generation from header")
}
