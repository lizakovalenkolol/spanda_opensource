package com.spanda.sync.blob

import com.spanda.sync.crypto.CryptoPrimitives
import com.spanda.sync.document.SyncDocument
import com.spanda.sync.keys.AccountId
import com.spanda.sync.keys.DataKey

/**
 * The encrypted form of the document stored on the server (SPEC.md §7.2):
 *
 * ```
 * header = "SPSY" || u8(1) || u32(keyGeneration) || nonce(12)
 * blob   = header || AES-256-GCM(blobKey, nonce, pad(document), header || lp(utf8(accountId)) || u64(blobVersion))
 * ```
 *
 * Binding [blobVersion] and the account into the associated data stops the server from relabelling
 * an old blob as new or moving a blob to another account.
 */
object Blob {
    const val MAGIC = "SPSY"
    const val FORMAT_VERSION: Byte = 1

    /** [blobVersion] is the version this blob will have once saved: the expected current version + 1. */
    fun seal(
        document: SyncDocument,
        dataKey: DataKey,
        account: AccountId,
        blobVersion: Long,
        crypto: CryptoPrimitives,
    ): ByteArray = TODO("SPEC.md §7.2")

    /**
     * Decrypts a downloaded blob. Throws [com.spanda.sync.crypto.AuthenticationFailed] on any
     * mismatch, and [KeyGenerationMismatch] if the blob was written with a different key generation.
     */
    fun open(
        blob: ByteArray,
        dataKey: DataKey,
        account: AccountId,
        blobVersion: Long,
        crypto: CryptoPrimitives,
    ): SyncDocument = TODO("SPEC.md §7.2")

    /** Reads the key generation from the header without decrypting, to pick the right key. */
    fun keyGeneration(blob: ByteArray): Int = TODO("SPEC.md §7.2 header")
}

class KeyGenerationMismatch(val blobGeneration: Int, val heldGeneration: Int) :
    Exception("blob key generation $blobGeneration, device holds $heldGeneration")
