# Cipher — Privacy Notes

Notes. Encrypted. Local. Nothing more.

<a href="https://apt.izzysoft.de/packages/dev.cipher.notes">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" 
         height="80" 
         alt="Get it at IzzyOnDroid">
</a>

## Quick Start

### Installation

**Option 1: IzzyOnDroid** (recommended for privacy-focused users)
```
F-Droid → Settings → Repositories → Add custom repo
https://apt.izzysoft.de
→ Search "Cipher"
```

**Option 2: GitHub Releases**
```
Download APK → adb install CipherNotes-release.apk
Or: sideload via file manager
```

**Option 3: Build from source**
```bash
git clone https://github.com/CipherApps/cipher-notes
cd cipher-notes
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/CipherNotes-release.apk
```

---

## Why Cipher?

- **Your phone is your vault.** Everything stays on device.
- **No accounts.** No login. No tracking who you are.
- **Small footprint.** No bloat.
- **Auditable.** Open source. Every line of code visible.
- **Privacy by design.** No internet permission. Zero telemetry hooks.

---

## Open Source Libraries

All dependencies are FOSS and auditable:

- **Jetpack Compose** (Apache 2.0) — UI framework
- **Room** (Apache 2.0) — SQLite wrapper
- **Hilt** (Apache 2.0) — Dependency injection
- **kotlinx-serialization** (Apache 2.0) — JSON codec
- **Timber** (Apache 2.0) — Logging
- **Android Security Crypto** (Apache 2.0) — Android-level crypto

## What it does NOT do

- Cloud sync
- Account creation
- Analytics
- Advertisements
- Internet calls (network config enforces it)
- Crash reporting
- Any telemetry

## Privacy by design

- **No INTERNET permission** in AndroidManifest
- **Network security config** blocks all outbound traffic at OS level
- **Passwords never stored** — derived on-demand via PBKDF2
- **Encryption uses:** PBKDF2 (200k iterations) + AES-256-GCM
- **All data stays on device** — Room SQLite database

## Building

```bash
# Clone
git clone https://github.com/CipherApps/cipher-notes
cd cipher-notes

# Build
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/CipherNotes-release.apk
```

## Installation

### From GitHub Releases
Download APK → sideload via `adb install CipherNotes-release.apk`

## FAQ

**Q: How is the app so small?**
- Minimal dependencies (no Gson, Retrofit, Firebase)
- Kotlin/Compose is efficient
- No embedded runtimes (not Flutter, not React Native)

**Q: What if I lose my password?**
- You can't decrypt the note. That's the point.
- Consider using a password manager.

**Q: Can you read my notes?**
- No. The code is open-source. You can audit it.
- No cloud, no accounts, no servers.

**Q: Will you add cloud sync?**
- No. Not without changing the privacy model fundamentally.
- Recommend: sync files via Syncthing / Nextcloud locally.

**Q: Is this production-ready?**
- Not yet. Still v1.4.1
- Use for personal notes.
- Audit the crypto code if deploying at scale.

---
> 💡 **Enjoying Cipher Notes?** If you believe in local-first, encrypted, and open-source software, drop a ⭐ **Star** on this repository.
---

## License

MIT. Use, modify, distribute freely.

---

**Cipher v1.4.1** — because your notes are yours.

Built with Kotlin + Jetpack Compose. No telemetry. No cloud. No BS.
