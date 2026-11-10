# Spanda Sync — Protocol Specification

**Status:** draft v1, for review. Nothing here is implemented yet.
**Date:** 10 November 2026

This document specifies how Spanda encrypts a user's cycle data and keeps it in sync between their devices through a server that cannot read it. It is the reference for the code in this repository and for an external security audit.

The key words MUST, MUST NOT, SHOULD and MAY are used as in RFC 2119.

---

## 1. Goals and non-goals

### 1.1 Goals

1. **Only the user's devices can read the data.** The Spanda server, Apple, Google, Paddle and anyone on the network see ciphertext or account metadata, never period days, notes or settings.
2. **No passwords, no prompts.** A user signs in with Apple or Google. On a second device of the same platform the data key arrives silently through the platform's end-to-end encrypted key sync.
3. **A way back when everything else is lost.** A recovery key, shown once, can always restore access.
4. **Concurrent edits never lose data.** Two devices editing offline converge to the same result without asking the user.
5. **Small and auditable.** One blob per account, a handful of primitives, no custom cryptography.

### 1.2 Non-goals

- **Protecting against a compromised device.** Anyone who can unlock the phone can read the app.
- **Hiding that someone uses Spanda.** Apple, Google, Paddle and the Spanda server know an account exists.
- **Hiding all metadata.** The server sees blob size (rounded to a bucket), save times and IP addresses (§9.3).
- **Protecting data the user sends to a calendar.** Calendar output is opt-in and outside this protocol.
- **Sharing data between different people.** One account is one person.

---

## 2. Overview

```
 Device A                         Spanda server                    Device B
 ┌──────────────┐                ┌──────────────────┐            ┌──────────────┐
 │ document     │  encrypt+pad   │ account record   │  download  │ document     │
 │ (LWW map)    │ ─────────────▶ │  wrapped DK      │ ─────────▶ │ (LWW map)    │
 │ data key DK  │  PUT if-match  │ blob + version   │  decrypt   │ data key DK  │
 └──────┬───────┘                └──────────────────┘   merge    └──────┬───────┘
        │      iCloud Keychain / Android Block Store (E2EE key sync)    │
        └──────────────────────────── DK ───────────────────────────────┘
```

- Each account has one random **data key (DK)**. It never leaves the user's devices unencrypted.
- The user's data is a **document**: a map of entries with timestamps, so any two copies can be merged (§6).
- The document is padded, encrypted with a key derived from DK and stored on the server as one **blob** with a **version number**. A save succeeds only if the device has seen the latest version (§7).
- DK reaches new devices in one of three ways:
  - **Silent key sync:** iCloud Keychain or Android Block Store, for the same platform (§5.1).
  - **QR handover:** from an existing device (§5.3).
  - **Recovery key:** typed once (§5.2).
- The server also keeps a copy of DK **wrapped by the recovery key**. Without the recovery key that copy is useless.

---

## 3. Primitives

All primitives are standard and provided by the platform (CryptoKit on iOS, `javax.crypto` on Android/JVM). This library does not implement any cryptography itself. It receives the primitives through the `CryptoPrimitives` interface, and the shared test vectors (§12) prove that every platform behaves the same.

| Purpose | Primitive | Notes |
|---|---|---|
| Authenticated encryption | AES-256-GCM | 96-bit random nonce, 128-bit tag |
| Key derivation | HKDF-SHA-256 (RFC 5869) | |
| Checksums | SHA-256 | recovery-key checksum only |
| Randomness | OS CSPRNG | `SecRandomCopyBytes` / `SecureRandom` |

**Why AES-256-GCM.** It is available natively on both platforms with hardware acceleration, so no third-party native library is needed.

**Nonce limits.** Random 96-bit nonces are safe for up to 2³² encryptions per key. One encryption happens per save, so a user would need millions of saves per day for centuries to get close. Implementations MUST still count encryptions per key generation and MUST rotate the key (§5.5) before 2³⁰.

**Considered and not chosen.** XChaCha20-Poly1305 would remove the nonce-count concern but needs libsodium on both platforms. This is an open question for the audit (§13).

### 3.1 Encodings

- Integers are unsigned, big-endian.
- `||` means byte concatenation.
- `utf8(s)` is the UTF-8 encoding of string `s`.
- `lp(x)` is `u16(len(x)) || x`.
- `b64u` is base64url without padding (RFC 4648 §5).
- Labels are ASCII strings used as HKDF `info` and AEAD associated data. Each label is listed once, in §3.2.

### 3.2 Labels

| Label | Used for |
|---|---|
| `spanda-sync/v1/blob-key` | HKDF info: blob encryption key from DK |
| `spanda-sync/v1/recovery-kek` | HKDF info: key-encryption key from the recovery key |
| `spanda-sync/v1/transfer-key` | HKDF info: QR handover key from the transfer secret |
| `spanda-sync/v1/wrapped-dk` | AEAD associated data prefix for the wrapped DK |
| `spanda-sync/v1/transfer` | AEAD associated data prefix for the QR handover package |

---

## 4. Identifiers and keys

| Name | Size | Created by | Where it lives |
|---|---|---|---|
| `accountId` | 16 bytes, shown as b64u | server, at account creation | server; every device |
| `deviceId` | 16 random bytes | each install, once | device; inside the encrypted document |
| `DK` data key | 32 random bytes | first device, at account creation | devices; platform key sync; server only wrapped |
| `keyGeneration` | u32, starts at 1 | incremented on rotation (§5.5) | blob header, wrapped DK, key sync item |
| `RK` recovery key | 16 random bytes | first device; again on regeneration | shown to the user once; never stored by Spanda |
| `T` transfer secret | 32 random bytes | existing device, per QR handover | QR code only; discarded after use |

### 4.1 Derived keys

```
blobKey     = HKDF-SHA-256(ikm = DK, salt = utf8(accountId), info = "spanda-sync/v1/blob-key", L = 32)
kek         = HKDF-SHA-256(ikm = RK, salt = utf8(accountId), info = "spanda-sync/v1/recovery-kek", L = 32)
transferKey = HKDF-SHA-256(ikm = T,  salt = utf8(linkCode),  info = "spanda-sync/v1/transfer-key", L = 32)
```

DK is never used directly as an encryption key. Each use derives its own key, so a weakness in one use cannot spill into another.

### 4.2 Recovery key format

The recovery key is 16 random bytes. The user sees it as 28 characters of Crockford base32 in 7 groups of 4, for example `7K3Q-M9PA-...-XW2E`.

```
check   = first 12 bits of SHA-256(RK)
payload = RK (128 bits) || check (12 bits)        = 140 bits = 28 base32 characters
```

When parsing user input, implementations MUST:
- ignore case, spaces and hyphens;
- map `O`→`0`, `I`/`L`→`1` (Crockford);
- reject input whose checksum doesn't match, with a "check the key for typos" message. They must never try to decrypt with a wrong key.

128 random bits need no slow KDF. The recovery key is not a password, and brute force is out of reach.

---

## 5. Key distribution

### 5.1 Silent key sync (same platform)

After creating or receiving DK, a device stores this **key sync item**:

```json
{ "v": 1, "accountId": "<b64u>", "keyGeneration": 1, "dk": "<b64u 32 bytes>" }
```

**iOS — iCloud Keychain**
- Generic password item, service `com.spanda.sync`, account = `accountId`.
- `kSecAttrSynchronizable = true`.
- `kSecAttrAccessible = kSecAttrAccessibleAfterFirstUnlock`.
- iCloud Keychain is end-to-end encrypted, so Apple can't read the item.

**Android — Block Store**
- Key `spanda-sync.dk.<accountId>`.
- The device MUST call `isEndToEndEncryptionAvailable()` first.
- Only when that returns true does it set `setShouldBackupToCloud(true)`. Otherwise the item stays on the device, and new devices use QR or the recovery key.
- Block Store is end-to-end encrypted only when the device has a screen lock. The check above makes sure that holds.

**On sign-in on a new device**, the device looks for the item with its `accountId`:
- If found, and `keyGeneration` matches the server's wrapped-DK record, the device is ready.
- If not, it offers QR handover (§5.3) or the recovery key (§5.2).

### 5.2 Recovery key and the wrapped DK

**At account creation** the first device:
1. generates DK and RK;
2. computes `kek` (§4.1);
3. uploads the **wrapped DK record**:

```
WrappedDK = magic "SPWK" (4) || formatVersion u8 = 1 || keyGeneration u32 || nonce (12) || AES-256-GCM(kek, nonce, DK, aad)
aad       = "spanda-sync/v1/wrapped-dk" || lp(utf8(accountId)) || u32(keyGeneration)
```

Then the device shows RK **once**. The user MUST confirm they saved it, for example by typing back two groups, before account setup finishes.

**Restoring:** the user types RK, the device fetches WrappedDK, derives `kek` and decrypts. A wrong key fails the GCM tag check; the checksum (§4.2) catches typos before that.

**Regenerating the recovery key** (Settings → Account → New recovery key) needs a device that holds DK and a fresh sign-in:
1. Generate a new RK.
2. Rewrap DK.
3. Replace WrappedDK on the server with a version check.
4. Show the new RK once.

The old RK stops working as soon as the record is replaced.

### 5.3 QR handover (device to device)

Used when key sync is unavailable: moving between iPhone and Android, sync turned off, or Block Store without a screen lock.

| Step | Device | Action |
|---|---|---|
| 1 | Existing device A | `POST /v1/link` → server returns `linkCode` (10 bytes, Crockford base32). Single use, expires in 10 minutes. |
| 2 | A | Generate `T`, derive `transferKey`, build the **transfer package** (below) and upload it with `PUT /v1/link/{linkCode}/package`. |
| 3 | A | Show a QR code containing `spanda-link:1:<linkCode>:<b64u(T)>`. |
| 4 | New device B | User signs in with Apple/Google and scans the QR. B calls `POST /v1/link/{linkCode}/claim` with its ID token. |
| 5 | Server | Marks the code claimed and notifies A with a short hint about B's login: provider, plus the masked email from the ID token, e.g. `k•••@gmail.com`. The server MUST NOT store the hint. |
| 6 | A | Asks "Add this sign-in to your Spanda account?". Only after the user confirms: `POST /v1/link/{linkCode}/approve`. |
| 7 | Server | Attaches B's login to the account (if new) and releases the package to B. |
| 8 | B | Decrypts the package with `transferKey`, stores DK (§5.1) and syncs. The server deletes the code and the package. |

Transfer package format:

```
package = AES-256-GCM(transferKey, nonce, json, aad) with nonce prepended
json    = { "v": 1, "accountId": "<b64u>", "keyGeneration": n, "dk": "<b64u>" }
aad     = "spanda-sync/v1/transfer" || lp(utf8(linkCode))
```

**Why the approval step.**
- Someone who photographs the QR has `T` and the `linkCode`. Without step 6 they could claim the code with their own Google account and receive DK.
- With step 6 the account owner sees an unknown login and refuses.
- A refused or expired code is deleted together with its package.

The server never sees `T`, so it can't open the package even though it stores it.

### 5.4 Adding a second login without QR

A device that is already signed in can attach another Apple/Google login: `POST /v1/accounts/logins` with the session and the new ID token. No key material moves, because the device already holds DK.

### 5.5 Key rotation

A new key generation is created when:
- a device is lost and the user chooses "Sign out everywhere";
- the user regenerates the recovery key and asks to rotate;
- the encryption counter nears its limit (§3).

Steps:
1. Generate a new DK and set `keyGeneration` to the previous value + 1.
2. Re-encrypt the current document under the new key.
3. Rewrap the new DK with the current RK (or a new one).
4. Upload the blob and WrappedDK in one request (`POST /v1/rotate`), with a version check.
5. Replace the key sync item.

After that:
- Other devices find a newer `keyGeneration` and pick up the new DK through key sync, or ask for QR or the recovery key.
- A lost device can no longer decrypt **new** saves. It keeps whatever it already decrypted — rotation cannot undo that (§1.2).

---

## 6. The document and merging

### 6.1 Structure

The document is a map from **key** to **entry**. Every entry carries a hybrid logical clock timestamp, and a deleted entry is kept as a **tombstone**.

```json
{
  "v": 1,
  "entries": {
    "<key>": { "w": 1762790400000, "c": 0, "d": "<deviceId b64u>", "val": "<string>" | null }
  }
}
```

| Field | Meaning |
|---|---|
| `w` | wall-clock milliseconds since the Unix epoch, as seen by the writing device |
| `c` | counter, u16, for several writes in the same millisecond or when the clock went backwards |
| `d` | the writing device's `deviceId` |
| `val` | the value; `null` is a tombstone (the entry was deleted) |

The library treats keys and values as opaque strings. The application decides what they mean (§6.4).

### 6.2 Hybrid logical clock

Each device keeps `(wLast, cLast)`.

**Writing an entry:**
```
w = max(now(), wLast)
c = (w == wLast) ? cLast + 1 : 0
```

**After merging a remote document:**
```
wLast = max(wLast, max remote w)
```
with `cLast` set the same way. A device whose clock runs behind still writes timestamps newer than anything it has seen, so its edits are not silently lost.

### 6.3 Merge rule

For every key present in either document, keep the entry with the greatest `(w, c, d)`, compared in that order (`d` as bytes).

This rule is:
- **commutative:** the order of the two copies doesn't matter;
- **associative:** the grouping of several merges doesn't matter;
- **idempotent:** merging the same copy twice changes nothing.

So devices converge whatever order they sync in. Tombstones win over older values, which is what stops a deleted note from coming back from a device that hadn't seen the deletion.

Tombstones are kept for the life of the account in v1. They are a few dozen bytes each, and dropping them safely needs every device to have seen them.

### 6.4 Spanda's mapping (informative)

This is what Spanda's apps put into the document. It is listed here so readers can see exactly what is encrypted.

| Key | Value | Meaning |
|---|---|---|
| `period/YYYY-MM-DD` | `"1"` or tombstone | a marked period day / an unmarked one |
| `note/<uuid>` | JSON `{"date":"YYYY-MM-DD","text":"…"}` or tombstone | a journal note; the id is a random UUID, so two devices never collide |
| `setting/cycleLength` | decimal string | the user's default cycle length |
| `setting/notesInCalendar` | `"true"` / `"false"` | whether notes are shown in the linked calendar |

Things the protocol does not carry:
- **Device-only settings** — language, which calendar a device draws into, What's New state.
- **Purchases** — the server knows the entitlement from store receipts.

---

## 7. Blob format and saving

### 7.1 Padding

The plaintext is padded before encryption, so the server learns only a size bucket, not how much the user has logged.

```
frame  = u32(len(doc)) || doc || zero bytes
size   = smallest 4096 × 2^k (k ≥ 0) with size ≥ 4 + len(doc)
doc    = UTF-8 JSON of §6.1, keys sorted, no insignificant whitespace
```

- A document larger than 4 MiB MUST be rejected. In practice a decade of daily notes stays well below this.
- On decryption the frame length MUST be checked against the bucket, and the padding bytes MUST be zero.

### 7.2 Blob

```
header = magic "SPSY" (4) || formatVersion u8 = 1 || keyGeneration u32 || nonce (12)
blob   = header || AES-256-GCM(blobKey, nonce, frame, aad)
aad    = header || lp(utf8(accountId)) || u64(blobVersion)
```

`blobVersion` is the version number this blob will have on the server: the expected current version + 1.

Putting it in the associated data means:
- the server can't relabel an old blob with a new version number;
- a blob can't be moved to another account.

### 7.3 Saving

The device keeps `lastSeenVersion`: the highest blob version it has decrypted or saved.

```
save(local):
    for attempt in 1..5:
        blob = encrypt(local, blobVersion = lastSeenVersion + 1)
        response = PUT /v1/blob  (If-Match: lastSeenVersion, body: blob)
        if response is 200:   lastSeenVersion += 1; return OK
        if response is 409:   remote = GET /v1/blob
                              check remote.version ≥ lastSeenVersion (§7.5)
                              local = merge(local, decrypt(remote))
                              lastSeenVersion = remote.version
        wait with exponential backoff and jitter
    leave changes pending; retry on next app start, push or connectivity change
```

Saves are debounced: a device SHOULD wait about 2 seconds after the last edit before saving, so marking five period days is one save.

### 7.4 Receiving

On a silent push (§8), on app start and when returning to the foreground:

```
GET /v1/blob?ifNewerThan=lastSeenVersion
if newer:  check version ≥ lastSeenVersion; local = merge(local, decrypt(remote));
           lastSeenVersion = remote.version; if local had pending changes → save(local)
```

### 7.5 Rollback protection

- A device MUST reject a blob whose version is lower than `lastSeenVersion`. Otherwise a malicious server could serve an old copy and make recent edits disappear.
- A device MUST reject a blob whose `keyGeneration` is lower than the one it holds.
- A brand-new device has no `lastSeenVersion`, so it can't detect a rollback on its first download. This is a known limitation (§13).

---

## 8. Push

When a save succeeds, the server sends a silent push to the account's other devices: APNs background notification on iOS, FCM data message on Android.

The payload contains only `{"t":"sync"}`. There is no version, no account id and no content. The push is only a hint: devices also sync on start and on foreground, so a lost push only delays sync.

---

## 9. Server API (client view)

The server implementation is not part of this repository. This section is the contract the client code is written against.

- All requests use HTTPS with TLS 1.3.
- Authentication is a session token from `POST /v1/session`.

### 9.1 Endpoints

| Method and path | Purpose |
|---|---|
| `POST /v1/session` | Exchange an Apple/Google ID token for a session. Returns `accountId`, or `404 no-account` for an unknown login. |
| `POST /v1/accounts` | Create an account: ID token + WrappedDK + first blob (version 1). |
| `POST /v1/accounts/logins` | Attach a second login to the signed-in account (§5.4). |
| `DELETE /v1/accounts/logins/{id}` | Detach a login; the last login can't be detached. |
| `GET /v1/blob` | Current blob and version; `?ifNewerThan=n` returns `304` if nothing is newer. |
| `PUT /v1/blob` | Save with `If-Match: <version>`. Returns `200 {version}` or `409 {version}`. |
| `GET /v1/wrapped-key`, `PUT /v1/wrapped-key` | Read / replace WrappedDK (with `If-Match`). |
| `POST /v1/rotate` | Replace blob and WrappedDK together (§5.5). |
| `POST /v1/link`, `PUT /v1/link/{code}/package`, `POST /v1/link/{code}/claim`, `POST /v1/link/{code}/approve`, `GET /v1/link/{code}/package` | QR handover (§5.3). |
| `PUT /v1/push-token`, `DELETE /v1/push-token` | Register / remove this device's push token. |
| `DELETE /v1/accounts` | Delete the account: blob, WrappedDK, logins, push tokens, entitlement. Irreversible. |

### 9.2 What the server stores

| Stored | Not stored |
|---|---|
| `accountId` | cycle data, notes, settings |
| logins (provider + subject id) | DK, RK, T |
| WrappedDK | email, name |
| the current blob and its version | old blob versions (overwritten on save) |
| push tokens | |
| entitlement (store or Paddle) and Paddle customer id | |

### 9.3 Metadata the server can see

- Blob size bucket.
- Times of saves and downloads.
- The number of devices (push tokens).
- IP addresses.
- Which providers the logins come from.

IP retention and the hosting provider are open questions (§13).

---

## 10. Account lifecycle

| Event | What happens |
|---|---|
| **Create** | First device generates DK and RK, uploads WrappedDK and blob v1, stores the key sync item, shows RK once. |
| **New device, same platform** | Sign in → key sync item arrives → download and decrypt. |
| **New device, other platform** | Sign in → QR from an existing device, or the recovery key. |
| **Sign out on a device** | Delete the local document copy and push token. The key sync item stays, so signing back in is silent. |
| **Sign out everywhere / lost device** | Rotate keys (§5.5) and invalidate all sessions. |
| **Delete account** | `DELETE /v1/accounts`, then remove the key sync item and local copy. Other devices find the account gone on their next sync and return to local-only mode, keeping their local data. |
| **Lost recovery key** | While any device still holds DK: regenerate (§5.2). If no device holds DK, the data can't be recovered. This is the cost of end-to-end encryption, and the app says so when it shows the key. |

---

## 11. Errors the user can see

| Situation | Behaviour |
|---|---|
| Recovery key typo | Checksum fails before any network call: "This key has a typo. Check it and try again." |
| Recovery key valid but wrong account | GCM tag fails: "This key belongs to a different account." |
| QR code expired or already used | "This code has expired. Show a new one on your other device." |
| Server unreachable | Edits stay local and are saved later. No error unless sync has failed for more than 7 days. |
| Blob fails to decrypt (tag mismatch) | Stop syncing, keep local data, report "Sync paused" with a support link. Never overwrite the server copy. |
| Rollback detected (§7.5) | Same as above. |

---

## 12. Test vectors

`testvectors/` will hold JSON files with fixed inputs and expected outputs. Every implementation MUST pass them:

| File | Covers |
|---|---|
| `hkdf.json` | the three derivations of §4.1 |
| `recovery-key.json` | encoding, checksum, accepted spellings, rejected typos |
| `wrapped-dk.json` | WrappedDK with fixed DK, RK, nonce |
| `padding.json` | bucket sizes at boundaries (4091, 4092, 4093 bytes…) |
| `blob.json` | full blob with fixed DK, nonce, document, accountId, version |
| `transfer.json` | QR payload and transfer package |
| `merge.json` | merge cases: concurrent edits, delete vs edit, same-millisecond writes, clock skew |

---

## 13. Open questions for review and audit

1. **AES-256-GCM vs XChaCha20-Poly1305** (§3).
2. **First-download rollback** (§7.5). A new device trusts the first blob it sees. A signed version counter kept in the key sync item could close this.
3. **QR direction** (§5.3). The alternative is for the new device to show a QR with an ephemeral public key, and the existing device to scan it and send DK encrypted to that key. That removes the risk of someone photographing the QR, but means the existing device must have a camera at hand.
4. **Block Store behaviour** when a screen lock is added or removed later. To be checked against Google's current documentation.
5. **Tombstone compaction** (§6.3) after all devices have acknowledged a version.
6. **Recovery key confirmation** UX: how much to ask the user to type back.
7. **IP logging and retention**, and the hosting provider (§9.3).
8. **Reproducible builds** of the apps, so users can check that the published code is what runs.
