# Islamic Azkar · أذكار إسلامية

Hisn al-Muslim (Fortress of the Muslim) in Arabic, English and French, with a Liquid Glass design,
prayer times and home-screen widgets in Arabic calligraphy.

صدقة جارية عن محمد الكوسا وديمة غنوم وعائلاتهم

| Folder | What it holds |
| --- | --- |
| `IslamicAzkar/` | The Android app (Kotlin, Jetpack Compose), and the iPhone/iPad app in `IslamicAzkar/ios/` (SwiftUI + WidgetKit). |
| `website/` | The website: plain HTML/CSS/JS reading the same azkar data, deployed on Cloudflare Pages. |
| `api/` | Cloudflare Worker on `islamicazkar.net/api/*`: tester sign-ups from the website, saved in KV and emailed to mohamad@koussa.org. Deploy with `cd api && wrangler deploy`. |
| `store/` | Google Play listing: icon, feature graphics and captioned screenshots per language, and the store text (`listing.md`). |

## Android

Two build flavors:

| Flavor | For | Updates |
| --- | --- | --- |
| `play` | Google Play (`bundlePlayRelease`) | Through the Play Store only. Play forbids self-updating, so this build has no update code and no `REQUEST_INSTALL_PACKAGES`. |
| `github` | The APK on [islamic-azkar-releases](https://github.com/Mkuz1/islamic-azkar-releases) and the website's download button (`assembleGithubRelease`) | In the app: it checks the latest release, downloads `IslamicAzkar.apk` and installs it with `PackageInstaller` after the user confirms. |

```bash
cd IslamicAzkar
./gradlew bundlePlayRelease                 # Play upload: app/build/outputs/bundle/playRelease/
./gradlew assembleGithubRelease             # GitHub APK:  app/build/outputs/apk/github/release/
./gradlew assemblePlayDebug                 # debug APK
./gradlew recordRoborazziPlayDebug          # renders every screen, widget and store graphic
```

Release builds are signed with an upload key kept outside this repository
(`../upload-key/keystore.properties`); without it they fall back to the debug key, and a debug-signed
APK cannot update an installed release build, so build releases on the Mac.

**Publishing an update for the in-app updater:** bump `versionCode` and `versionName` in `app/build.gradle.kts`,
build `assembleGithubRelease`, then create a release in `Mkuz1/islamic-azkar-releases` tagged `v` + versionName
(for example `v1.0.4`) with the APK attached as `IslamicAzkar.apk`. The app compares the tag with its own
versionName and installs only an APK whose versionCode is higher. To announce an iPhone build too, attach
`IslamicAzkar.ipa` to the same release: iOS can't install it from inside the app, so the iPhone app only
shows that a new version exists and opens the release page.

`.github/workflows/android.yml` builds both flavors (debug-signed) on every push touching the Android app,
and fails if the Play build ever asks for `REQUEST_INSTALL_PACKAGES`.

## Content

`tools/build_hadith_duas.py` adds the duas from the hadith collections (Bukhari, Muslim, Abu Dawud,
al-Tirmidhi, al-Nasa'i, Ibn Majah) and the ʿUmrah guide to all three copies of `azkar.json`. The Arabic
is copied word for word from the cited hadith in the [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api)
editions, never typed by hand; the build stops if a text, a cross-reference or an al-Albani grade does not check out.
Meanings and steps are in `tools/hadith_duas.py` and `tools/umrah_guide.py`. Re-run with `python3 tools/build_hadith_duas.py`.

## iPhone

The iOS app mirrors the Android one: the same screens, the Widgets tab and 14 widgets (Home Screen and Lock Screen).
This Mac has no Xcode, so it builds on GitHub Actions (`.github/workflows/ios.yml`) on every push that touches
`IslamicAzkar/ios/`, or by hand from the Actions tab (iOS → Run workflow). Each run uploads:

- `IslamicAzkar-ipa`: an unsigned `IslamicAzkar.ipa`. Install it with Sideloadly or AltStore, which sign it with your Apple ID.
- `screenshots`: Simulator screenshots of each screen in Arabic, English and French.

`IslamicAzkar/ios/typecheck.sh` gives a quick local check against the macOS SDK before pushing.

## Mac

`IslamicAzkar/mac/` builds the iPhone app's SwiftUI code (`ios/App`, `ios/Shared`) as a native Mac app, plus
`mac/Sources/`: a menu bar item (app icon and next prayer) whose panel shows the azkar due now, whether they were
read today, the countdown and today's prayer times for the chosen place, "open at login", and the app keeps running
in the menu bar when its window is closed. The Xcode project is generated from `mac/project.yml` with XcodeGen.
`.github/workflows/mac.yml` builds it on every push; releases attach `IslamicAzkar.dmg` (universal, ad-hoc signed:
the first time, right-click the app and choose Open, or allow it in System Settings → Privacy & Security).

## Prayer times

Default method: Dubai, as used by Dubai's Islamic Affairs department (IACAD) in the Dulook DXB app — Fajr and Isha
at 18.2°, Dhuhr and Maghrib +3 minutes (aladhan.com method 16). `PrayerCalcTest.dubaiDulook` checks it against six
reference days through the year.

## Website

Live at https://islamicazkar.net (also https://islamic-azkar.pages.dev)

Static files, no build step. Preview locally with `python3 -m http.server --directory website`,
deploy with `wrangler pages deploy website --project-name islamic-azkar`.

## Links

- Privacy policy: https://mkuz1.github.io/islamic-azkar-privacy/
- Mushaf app: https://www.mushafapp.com

## Publishing a new APK

The website's download button always points to the latest release of the public repo
`Mkuz1/islamic-azkar-releases`. To ship a new build, create a release there with the APK named `IslamicAzkar.apk`:

    gh release create v1.0.N IslamicAzkar/IslamicAzkar.apk --repo Mkuz1/islamic-azkar-releases --latest
