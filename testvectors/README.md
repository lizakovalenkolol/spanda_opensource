# Test vectors

Fixed inputs and the exact bytes every implementation must produce (SPEC.md §12). Not written yet.

Planned files:

| File | Covers |
|---|---|
| `hkdf.json` | blob key, recovery KEK and transfer key derivations (§4.1) |
| `recovery-key.json` | encoding, checksum, accepted spellings, rejected typos (§4.2) |
| `wrapped-dk.json` | wrapped data key with fixed DK, RK and nonce (§5.2) |
| `padding.json` | bucket sizes at the boundaries (§7.1) |
| `blob.json` | a full blob with fixed key, nonce, document, account and version (§7.2) |
| `transfer.json` | QR payload and transfer package (§5.3) |
| `merge.json` | concurrent edits, delete vs edit, same-millisecond writes, clock skew (§6) |

Each file is a JSON array of cases:

```json
[
  { "name": "short description", "input": { }, "expected": { } }
]
```

All binary values are hex strings. Vectors are generated once with an independent tool (e.g. Python `cryptography`), never by this library, so the library is checked against something other than itself.
