# Royal Baghchal · Android

Native **Kotlin + Jetpack Compose** app for the Nepal-first Baghchal game. This is an Android application, not a WebView wrapper. It coexists with the existing web game at the repository root.

## Royal design and features

- Cinematic midnight navy / antique gold / ruby, decorative Shreepech-inspired crown, ॐ and Chandra–Surya identity.
- Native home screen with three modes: Solo (real offline AI), Royal Duel (two players on one device) and World Arena (server-authoritative WebSocket rooms).
- Actual interactive 5×5 board with original vector goat/tiger tokens, legal-move indicators, turn status, 20-placement and 5-capture counters, undo and hints.
- Screens: Home, Play, Arena, Rankings, Profile, Settings, Baghchal Rules and Bluetooth physical board pairing.
- Background-free AI, local match persistence, seven-language selection (complete Nepali/English text, core labels for Spanish, French, German, Chinese and Japanese; remaining labels fall back to English).
- World Arena integration speaks the same protocol as the existing Node server (room codes, spectators, quick matchmaking, ratings, rematches, reconnection). **It is not live until a dedicated WSS server is deployed.**
- Native BLE GATT connection for an external compatible Baghchal board (Android runtime Bluetooth permissions, scanning, board sync and legal move commands). **Not generic Bluetooth phone-to-phone play.** BLE firmware is required.
- Accessible 48dp game-cell targets, native buttons, screenreader descriptions, dark system chrome, and device-safe insets.

## Build

Open the \`android/\` directory as a project in Android Studio and let Gradle sync.

Build/test on a computer with Android SDK API 37, Java 17 and Gradle 9.5:

\`\`\`bash
cd android
gradle :app:testDebugUnitTest :app:assembleDebug
\`\`\`

For every change, GitHub Actions creates a signed-with-debug-key **debug APK**, downloadable from the Android workflow artifacts. It is for installation/testing only, not a Google Play release. Publishing an AAB to Google Play requires the owner's Play Console account, release signing key, policies, app listing, icons/screenshots, and review.

Android Gradle Plugin 9.3, Kotlin 2.4.21, Compile SDK 37, Target SDK 36, minSdk 26, Compose BOM 2026.09.00. The SDK 36 target meets the Google Play requirement that took effect on August 31, 2026.

## Protocol and security

- Server connection must be \`wss://host/ws\`. No public multiplayer server is hard-coded or falsely claimed. Session identity is local, scoped by the configured WSS URL; Android backups are disabled.
- Device BLE service \`b0a6c7a1-7f0b-4e3b-b416-14df3b0a0001\`; native writes \`M:from:to\`, \`S:turn:placed:captured:ternaryBoard\` using the exact web game's compact protocol. Validated incoming moves pass through the Kotlin rules engine.
- BLE scanning requires user action and permissions (Android 12+ BLUETOOTH_SCAN/CONNECT, Android 11 and below ACCESS_FINE_LOCATION). No location is uploaded to the app backend.
- Bluetooth is disabled during online matchmaking; online moves must come through the authoritative server.

## App architecture

- \`game/GameRules.kt\`: immutable rules engine compatible with web semantics.
- \`game/GameAi.kt\`: minimax AI and difficulty settings.
- \`online/OnlineClient.kt\`: TLS WebSocket client, guest token and room JSON parsing.
- \`bluetooth/RoyalBleController.kt\`: BLE protocol and runtime-safe peripheral connection.
- \`RoyalGameViewModel.kt\`: game state, navigation, profile, persistence and online actions.
- \`ui/RoyalTheme.kt\`, \`ui/RoyalComponents.kt\`, \`ui/RoyalScreens.kt\`: palette, original vector pieces, board and full UI.
- \`app/src/test/.../RulesTest.kt\`: native game rules tests.

## Known release prerequisites

Run real-device QA, connectivity tests against the dedicated WSS server, physical BLE firmware integration, accessibility review, and professional Google Play signing/privacy disclosures before app-store publication. Do not market guest ratings as verified rankings. The Android app is independently installable without an online server for Solo and Local modes.
