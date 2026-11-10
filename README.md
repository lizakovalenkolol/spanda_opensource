# Spanda Sync

End-to-end encrypted sync for Spanda, a cycle tracker for iOS and Android. This library encrypts the user's data on their device and keeps it in sync between their devices through a server that cannot read it.

> **Status: design stage.** This repository holds the protocol specification and a code skeleton. Every function is a placeholder that explains what it will do. Nothing here is ready to use.

## Why this is open source

Spanda's promise is that only your own devices can read your cycle data: not Spanda, not Apple or Google, not the payment provider. The code in this repository is where that promise is kept or broken, so we publish it for anyone to read, test and audit.

## What's here

| Path | What it is |
|---|---|
| [`SPEC.md`](SPEC.md) | The protocol: keys, encryption, the recovery key, QR handover, merging, the blob format and the server API the client talks to. **Start here.** |
| `src/commonMain/kotlin/com/spanda/sync/` | Kotlin Multiplatform skeleton, one file per part of the spec. |
| `src/commonTest/kotlin/com/spanda/sync/` | Planned tests, one per spec section. |
| `testvectors/` | Planned fixed inputs and outputs every implementation must reproduce. |

### Code layout

| Package | Spec | Responsibility |
|---|---|---|
| `crypto` | §3 | Interface to the platform's AES-256-GCM, HKDF, SHA-256 and random bytes. The library implements no cryptography itself. |
| `keys` | §4, §5.2 | Data key, derived keys, recovery key encoding, wrapped data key. |
| `keysync` | §5.1 | Interface for iCloud Keychain / Android Block Store. |
| `transfer` | §5.3 | QR handover between devices. |
| `document` | §6 | The synced document, hybrid logical clock, merge rule. |
| `blob` | §7.1–7.2 | Padding and the encrypted blob format. |
| `client` | §7.3–7.5, §9 | Save/receive loop, rollback checks, server API interface. |

## What's not here

- **The Spanda apps.** They use this library, but aren't open source.
- **The Spanda server.** The client's view of its API is in SPEC.md §9.
- **Calendar output.** Drawing into a calendar is optional, the user turns it on, and it's outside this protocol.

## Platforms

Kotlin Multiplatform: `jvm` (used by Android) and `iosArm64` / `iosSimulatorArm64`.

- **Android:** the cryptographic primitives come from `javax.crypto`.
- **iOS:** they come from CryptoKit, through a small Swift implementation of the `CryptoPrimitives` interface.
- **Both:** the shared test vectors prove the platforms produce identical bytes.

## Building

```sh
./gradlew build
```

Requires JDK 17. The skeleton compiles. Calling any placeholder throws `NotImplementedError`.

## Security

Please report vulnerabilities privately. See [SECURITY.md](SECURITY.md).

## License

Apache License 2.0. See [LICENSE](LICENSE).

"Spanda" and the Spanda logo are not covered by this license.
