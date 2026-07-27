# Cipher — Privacy Notes

Notes. Encrypted. Local. Nothing more.

<a href="https://apt.izzysoft.de/packages/dev.cipher.notes">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" 
         height="80" 
         alt="Get it at IzzyOnDroid">
</a>

## Why Cipher?

- **Your phone is your vault.** Everything stays on device.
- **No accounts.** No login. No tracking who you are.
- **Small footprint.** No bloat.
- **Auditable.** Open source. Every line of code visible.
- **Privacy by design.** No internet permission. Zero telemetry hooks.

## Open Source Libraries & Tech Stack

All dependencies are FOSS and auditable:

- **[Jetpack Compose](https://developer.android.com/jetpack/compose)** – Modern UI toolkit for Android (Material 3, Icons Extended).
- **[Navigation Compose](https://developer.android.com/jetpack/compose/navigation)** – Declarative, type-safe in-app navigation.
- **[Dagger Hilt](https://dagger.dev/hilt/)** – Dependency injection framework with Compose integration.
- **[Room Database](https://developer.android.com/training/data-storage/room)** – Local SQLite object mapping library.
- **[AndroidX Security-Crypto](https://developer.android.com/topic/security/data)** – EncryptedSharedPreferences & AES-256 GCM encryption.
- **[Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)** – Key-value data storage solution for application settings.
- **[AndroidX Core SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen)** – Standardized backward-compatible splash screen API.
- **[Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-overview.html)** – Asynchronous programming and reactive data streams.

##  Features

*  **On-Device Encryption:** Notes are encrypted using AES-256 GCM via Android Keystore.
*  **Offline-First:** No accounts, no cloud sync, no tracking. Your data stays strictly on your device.
*  **Material You:** Fully customizable dynamic theme with Dark/Light mode support.
*  **System Integration:** Quick action support for text sharing directly from browsers and other apps.

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
- Not yet. Still v1.5.1
- Use for personal notes.
- Audit the crypto code if deploying at scale.

 > 💡 **Enjoying Cipher Notes?** If you believe in local-first, encrypted, and open-source software, drop a ⭐ **Star** on this repository.

## Website
https://cipherapps.github.io/

## Alternative host
This project is also available on [Codeberg](https://codeberg.org/CipherApps/cipher-notes).

##  License
Distributed under the MIT License. See `LICENSE` for more information.

**Cipher v1.5.1** — because your notes are yours.
Built with Kotlin + Jetpack Compose. No telemetry. No cloud. No BS.
