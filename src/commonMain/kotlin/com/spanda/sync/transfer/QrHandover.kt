package com.spanda.sync.transfer

import com.spanda.sync.crypto.CryptoPrimitives
import com.spanda.sync.keys.AccountId
import com.spanda.sync.keys.DataKey

/**
 * Device-to-device handover of the data key through a QR code (SPEC.md §5.3), for when platform
 * key sync can't help: moving between iPhone and Android, sync off, or no screen lock on Android.
 *
 * 1. Existing device: gets a single-use `linkCode` (10 min) from the server, generates a transfer
 *    secret T, uploads the [TransferPackage] (DK encrypted with a key derived from T) and shows
 *    `spanda-link:1:<linkCode>:<b64u(T)>` as a QR code.
 * 2. New device: signs in, scans, claims the code with its ID token.
 * 3. Existing device: shows which login claimed it and asks the user to approve. Without this step,
 *    a photo of the QR code would be enough to take the key.
 * 4. New device: downloads the package, decrypts it with T, stores the key.
 *
 * The server stores the package but never sees T, so it can't open it.
 */
object QrHandover {
    const val SCHEME = "spanda-link"
    const val VERSION = 1

    /** What the QR code holds. [secret] is T: 32 random bytes, discarded after use. */
    class QrPayload(val linkCode: String, internal val secret: ByteArray) {
        fun encode(): String = TODO("spanda-link:1:<linkCode>:<b64u(T)>")

        companion object {
            /** Parses a scanned code; returns null for anything that isn't a version-1 Spanda link. */
            fun decode(text: String): QrPayload? = TODO("SPEC.md §5.3")
        }
    }

    /** Step 1 on the existing device: a fresh secret for [linkCode]. */
    fun newPayload(linkCode: String, crypto: CryptoPrimitives): QrPayload = TODO("T = 32 random bytes")
}

/**
 * The data key encrypted for one handover (SPEC.md §5.3):
 * `nonce(12) || AES-256-GCM(transferKey, nonce, json, "spanda-sync/v1/transfer" || lp(linkCode))`,
 * where `transferKey = HKDF(T, salt = linkCode, info = "spanda-sync/v1/transfer-key")`.
 */
object TransferPackage {
    fun seal(payload: QrHandover.QrPayload, account: AccountId, dataKey: DataKey, crypto: CryptoPrimitives): ByteArray =
        TODO("SPEC.md §5.3")

    fun open(payload: QrHandover.QrPayload, sealed: ByteArray, crypto: CryptoPrimitives): Pair<AccountId, DataKey> =
        TODO("throws AuthenticationFailed for a wrong or tampered package")
}
