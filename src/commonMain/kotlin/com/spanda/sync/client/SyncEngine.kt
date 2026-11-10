package com.spanda.sync.client

import com.spanda.sync.crypto.CryptoPrimitives
import com.spanda.sync.document.HybridClock
import com.spanda.sync.document.SyncDocument
import com.spanda.sync.keys.AccountId
import com.spanda.sync.keys.DataKey

/**
 * Saving and receiving (SPEC.md §7.3–7.5). One engine per signed-in account on a device.
 *
 * The app owns persistence: it stores [State] (document, last seen version, pending flag) next to
 * its own data and passes it back in. The engine never writes plaintext anywhere.
 */
class SyncEngine(
    private val account: AccountId,
    private val dataKey: DataKey,
    private val server: SyncServer,
    private val crypto: CryptoPrimitives,
    private val clock: HybridClock,
) {
    data class State(
        val document: SyncDocument,
        /** Highest blob version decrypted or saved here; null on a brand-new device (SPEC.md §7.5). */
        val lastSeenVersion: Long?,
        /** Local edits not yet on the server. */
        val pending: Boolean,
    )

    /**
     * Save loop (SPEC.md §7.3): encrypt for `lastSeenVersion + 1`, PUT with If-Match; on conflict
     * download, check for rollback, merge, retry, up to 5 attempts with backoff.
     * Leaves `pending = true` if it gives up, so the next trigger retries.
     */
    suspend fun save(state: State): State = TODO("SPEC.md §7.3")

    /**
     * Receive (SPEC.md §7.4), on push, app start and foreground: download if newer, reject rollback
     * (§7.5), merge into the local document, and save if local edits were pending.
     */
    suspend fun receive(state: State): State = TODO("SPEC.md §7.4")
}

/** The server offered an older version than this device has already seen (SPEC.md §7.5). Sync stops. */
class RollbackDetected(val offered: Long, val lastSeen: Long) :
    Exception("server offered version $offered, device has seen $lastSeen")
