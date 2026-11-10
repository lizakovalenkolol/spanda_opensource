package com.spanda.sync.keysync

import com.spanda.sync.keys.AccountId
import com.spanda.sync.keys.DataKey

/**
 * Silent, end-to-end encrypted key sync through the platform (SPEC.md §5.1), implemented by the app:
 *
 * - **iOS:** iCloud Keychain. Generic password, service `com.spanda.sync`, account = accountId,
 *   `kSecAttrSynchronizable = true`, `kSecAttrAccessibleAfterFirstUnlock`.
 * - **Android:** Block Store, key `spanda-sync.dk.<accountId>`. Cloud backup is enabled only when
 *   `isEndToEndEncryptionAvailable()` is true (the device has a screen lock); otherwise the item
 *   stays on the device and new devices use QR handover or the recovery key.
 *
 * Stored value: [KeySyncItem] as JSON `{"v":1,"accountId":…,"keyGeneration":…,"dk":…}`.
 */
interface KeySyncStore {
    /** Whether this device can sync the key to the user's other devices end-to-end encrypted. */
    suspend fun isEndToEndSyncAvailable(): Boolean

    suspend fun save(item: KeySyncItem)

    /** The item for [account], or null if it hasn't arrived (new platform, sync off, never saved). */
    suspend fun load(account: AccountId): KeySyncItem?

    /** Removes the item, e.g. when the account is deleted (SPEC.md §10). */
    suspend fun delete(account: AccountId)
}

class KeySyncItem(val account: AccountId, val dataKey: DataKey) {
    fun toJson(): String = TODO("SPEC.md §5.1")

    companion object {
        fun fromJson(json: String): KeySyncItem = TODO("reject unknown v")
    }
}
