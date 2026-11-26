# Islamic Azkar · أذكار إسلامية

Hisn Al Muslim in Arabic, English and French, with Apple's Liquid Glass design on Android and iOS.
Opening page: the same sadaqa jariyah dedication as the Mushaf app.

## Android
- **IslamicAzkar.apk** – install this one (release, 2.4 MB).
- **IslamicAzkar-debug.apk** – fallback if the release build misbehaves.
- Source: Android Studio project in this folder (`app/`). Build: `./gradlew bundlePlayRelease` (Google Play) or `./gradlew assembleGithubRelease` (APK for GitHub, updates itself in the app). See the main README.
- Adapts to phones, foldables and tablets: one full-width page on every screen (content centred when unfolded), tabletop posture for the tasbih.
- Design: Apple's iOS style — grouped backgrounds, inset lists, iOS switches; Liquid Glass only on the tab bar, back button and sheets.
- Opening page: the Mushaf app's dark green sadaqa jariyah card, in English, French and Arabic.

## iPhone / iPad
1. Install **Xcode** from the App Store and sign in under Xcode → Settings → Accounts.
2. Connect your iPhone once (so it is registered for development).
3. Run `ios/build_ipa.sh` → produces **IslamicAzkar.ipa** in this folder.
   Or open `ios/IslamicAzkar.xcodeproj` and press Run with your iPhone selected.

Widgets on both platforms: Dhikr of the day, Quick azkar, Tasbih counter (interactive).

Screenshots: `screenshots/android`, `screenshots/ios`.

Note: the French translation is an independent rendering and should be reviewed by a qualified scholar.
