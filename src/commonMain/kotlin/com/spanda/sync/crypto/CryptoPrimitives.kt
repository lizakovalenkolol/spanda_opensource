package com.spanda.sync.crypto

/**
 * The cryptographic primitives this library uses (SPEC.md §3), supplied by the app.
 *
 * The library implements no cryptography itself. Android passes an implementation backed by
 * `javax.crypto` / `SecureRandom`; iOS passes a small Swift class backed by CryptoKit and
 * `SecRandomCopyBytes`. The shared test vectors (SPEC.md §12) prove both produce identical bytes.
 */
interface CryptoPrimitives {
    /** [count] bytes from the operating system's cryptographically secure random generator. */
    fun randomBytes(count: Int): ByteArray

    /**
     * AES-256-GCM encryption with a 12-byte [nonce] and 16-byte tag.
     * Returns ciphertext with the tag appended.
     */
    fun aesGcmSeal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, associatedData: ByteArray): ByteArray

    /**
     * AES-256-GCM decryption. Throws [AuthenticationFailed] if the tag doesn't match
     * (wrong key, tampered data or wrong associated data). Never returns unauthenticated bytes.
     */
    fun aesGcmOpen(key: ByteArray, nonce: ByteArray, ciphertextAndTag: ByteArray, associatedData: ByteArray): ByteArray

    /** HKDF-SHA-256 (RFC 5869) extract-and-expand, producing [length] bytes. */
    fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray

    /** SHA-256 digest. Used only for the recovery key checksum (SPEC.md §4.2). */
    fun sha256(data: ByteArray): ByteArray
}

/** The AEAD tag didn't verify. Callers must treat the data as untrusted and stop (SPEC.md §11). */
class AuthenticationFailed(message: String = "authentication tag mismatch") : Exception(message)
