package com.spanda.sync.crypto

/**
 * Every domain-separation label in the protocol, in one place (SPEC.md §3.2).
 * Each label is used for exactly one purpose, so keys and ciphertexts can't be reused across purposes.
 */
object Labels {
    const val BLOB_KEY = "spanda-sync/v1/blob-key"
    const val RECOVERY_KEK = "spanda-sync/v1/recovery-kek"
    const val TRANSFER_KEY = "spanda-sync/v1/transfer-key"
    const val WRAPPED_DK_AAD = "spanda-sync/v1/wrapped-dk"
    const val TRANSFER_AAD = "spanda-sync/v1/transfer"
}
