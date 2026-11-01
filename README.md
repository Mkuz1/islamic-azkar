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

```bash
cd IslamicAzkar
./gradlew assembleDebug                     # debug APK
./gradlew recordRoborazziDebug              # renders every screen, widget and store graphic
```

Release builds are signed with an upload key kept outside this repository
(`../upload-key/keystore.properties`); without it they fall back to the debug key.

## iPhone

The iOS app mirrors the Android one: the same screens, the Widgets tab and 14 widgets (Home Screen and Lock Screen).
This Mac has no Xcode, so it builds on GitHub Actions (`.github/workflows/ios.yml`) on every push that touches
`IslamicAzkar/ios/`, or by hand from the Actions tab (iOS → Run workflow). Each run uploads:

- `IslamicAzkar-ipa`: an unsigned `IslamicAzkar.ipa`. Install it with Sideloadly or AltStore, which sign it with your Apple ID.
- `screenshots`: Simulator screenshots of each screen in Arabic, English and French.

`IslamicAzkar/ios/typecheck.sh` gives a quick local check against the macOS SDK before pushing.

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
