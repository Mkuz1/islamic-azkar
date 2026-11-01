# Islamic Azkar · أذكار إسلامية

Hisn al-Muslim (Fortress of the Muslim) in Arabic, English and French, with a Liquid Glass design,
prayer times and home-screen widgets in Arabic calligraphy.

صدقة جارية عن محمد الكوسا وديمة غنوم وعائلاتهم

| Folder | What it holds |
| --- | --- |
| `IslamicAzkar/` | The Android app (Kotlin, Jetpack Compose). `ios/` holds an earlier SwiftUI version. See its README. |
| `website/` | The website: plain HTML/CSS/JS reading the same azkar data, deployed on Cloudflare Pages. |
| `store/` | Google Play listing: icon, feature graphics and captioned screenshots per language, and the store text (`listing.md`). |

## Android

```bash
cd IslamicAzkar
./gradlew assembleDebug                     # debug APK
./gradlew recordRoborazziDebug              # renders every screen, widget and store graphic
```

Release builds are signed with an upload key kept outside this repository
(`../upload-key/keystore.properties`); without it they fall back to the debug key.

## Website

Live at https://islamicazkar.net (also https://islamic-azkar.pages.dev)

Static files, no build step. Preview locally with `python3 -m http.server --directory website`,
deploy with `wrangler pages deploy website --project-name islamic-azkar`.

## Links

- Privacy policy: https://mkuz1.github.io/islamic-azkar-privacy/
- Mushaf app: https://www.mushafapp.com
