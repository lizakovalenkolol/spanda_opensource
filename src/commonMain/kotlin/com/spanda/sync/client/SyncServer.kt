package com.spanda.sync.client

/**
 * The client's view of the Spanda server (SPEC.md §9), implemented by the app over HTTPS.
 * The server itself is not part of this repository. Everything passed here is either ciphertext
 * or account metadata (SPEC.md §9.2).
 */
interface SyncServer {
    /** `GET /v1/blob?ifNewerThan=`: the current blob, or null if nothing is newer than [newerThan]. */
    suspend fun fetchBlob(newerThan: Long?): StoredBlob?

    /** `PUT /v1/blob` with `If-Match: expectedVersion`. */
    suspend fun saveBlob(expectedVersion: Long, blob: ByteArray): SaveResult

    suspend fun fetchWrappedKey(): ByteArray
    suspend fun replaceWrappedKey(expectedGeneration: Int, record: ByteArray)

    /** `POST /v1/rotate`: blob and wrapped key replaced together (SPEC.md §5.5). */
    suspend fun rotate(expectedVersion: Long, blob: ByteArray, wrappedKey: ByteArray): SaveResult

    // QR handover (SPEC.md §5.3)
    suspend fun createLink(): String
    suspend fun uploadLinkPackage(linkCode: String, sealed: ByteArray)
    suspend fun claimLink(linkCode: String, idToken: String)
    suspend fun approveLink(linkCode: String)
    suspend fun fetchLinkPackage(linkCode: String): ByteArray
}

class StoredBlob(val version: Long, val bytes: ByteArray)

sealed interface SaveResult {
    data class Saved(val version: Long) : SaveResult
    /** Another device saved first; [currentVersion] is what the server holds now. */
    data class Conflict(val currentVersion: Long) : SaveResult
}
