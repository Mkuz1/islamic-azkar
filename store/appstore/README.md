# Islamic Azkar on the App Store: what to do

Everything for the listing is in this folder:

| File | What it is |
|---|---|
| `listing.json` | Name, subtitle, promotional text, keywords, description and "What's New", in English (en-US), Arabic (ar-SA) and French (fr-FR), all within Apple's limits |
| `screenshots/iPhone-6.9/<lang>/` | iPhone 6.9" screenshots (1320 × 2868) |
| `screenshots/iPhone-6.3/<lang>/` | iPhone 6.3" screenshots (1206 × 2622), the size App Store Connect requires |
| `screenshots/iPad-13/<lang>/` | iPad 13" screenshots (2064 × 2752), required because the app runs on iPad |
| `screenshots/iPhone-Duo-*/<lang>/` | iPhone Duo (inner 2007 × 2853 unfolded, outer 1398 × 2034 folded), if App Store Connect shows slots for it |
| `icon-1024.png` | The App Store icon (also built into the app) |

The app itself is ready for review: no ads, no tracking, a privacy manifest, no GitHub update check
in App Store builds, and it adapts to every iPhone and iPad, including the iPhone Duo folded and unfolded.

## 1. One-time setup (about 20 minutes)

1. **Team ID** — developer.apple.com → Account → Membership details → *Team ID* (10 characters).
2. **App IDs** — developer.apple.com → Certificates, Identifiers & Profiles → Identifiers → **+**:
   - App ID `com.koussa.IslamicAzkar`, description "Islamic Azkar", capability **App Groups**.
   - App ID `com.koussa.IslamicAzkar.Widgets`, description "Islamic Azkar Widgets", capability **App Groups**.
   - Identifiers → App Groups → **+** → `group.com.koussa.IslamicAzkar`; then edit both App IDs →
     App Groups → Configure → tick it.
   - If Apple says the bundle ID is already in use (registered on another account), tell me: I'll
     move the app to a new ID.
3. **App record** — appstoreconnect.apple.com → Apps → **+** → New App:
   iOS · Name **Islamic Azkar: Hisn al-Muslim** · Primary language **English (U.S.)** (or Arabic) ·
   Bundle ID `com.koussa.IslamicAzkar` · SKU `islamic-azkar` · Full access.
4. **API key for the upload** — App Store Connect → Users and Access → Integrations →
   App Store Connect API → Team Keys → **+** → name "GitHub", access **Admin** → Generate →
   download the `.p8` file (only once!) and note the **Key ID** and the **Issuer ID** above the list.
5. **GitHub secrets** — github.com/Mkuz1/islamic-azkar → Settings → Secrets and variables → Actions →
   New repository secret, four times:
   `APPSTORE_TEAM_ID`, `APPSTORE_KEY_ID`, `APPSTORE_ISSUER_ID`, and `APPSTORE_KEY_P8`
   (open the `.p8` file in a text editor and paste all of it, including the BEGIN/END lines).

## 2. Upload the app (done: build 1.0.19 (19.5) with the new icon is in App Store Connect)

Tell me "upload to the App Store" (or run **Actions → App Store → Run workflow**). GitHub builds
the App Store version, signs it with your account and uploads it. After 10–30 minutes it appears
in App Store Connect → **TestFlight**, where you can install it on your iPhone before release.

## 3. Fill in the listing and submit (done for 1.0.19, submitted for review on 15 Nov 2026)

`tools/appstore_submit.py` does all of this through Apple's API: change `store/appstore/SUBMIT`
(`submit build=<build>`, or `no-submit` to only update the listing) and push. Only App Privacy is set
by hand. The review contact number is kept encrypted in `review-phone.enc`.

By hand, if ever needed (App Store Connect → your app → iOS App 1.0):

- **Screenshots**: drag the files from `screenshots/iPhone-6.3/en/` into the iPhone section
  (6.9" accepts the `iPhone-6.9` set too) and `screenshots/iPad-13/en/` into iPad. Then switch the
  language (top right) to Arabic and French and add the `ar` and `fr` sets with their texts.
- **Promotional text, description, keywords, support URL, marketing URL**: from `listing.json`.
  - Support URL: `https://islamicazkar.net`
  - Marketing URL: `https://islamicazkar.net`
- **Build**: pick the uploaded build.
- **App information**: Category **Lifestyle** (secondary: **Reference**) · Content rights: no
  third-party content requiring rights · Age rating: answer **None** to everything → **4+**.
- **Privacy policy URL**: `https://islamicazkar.net/privacy/`
- **App Privacy** (Get Started):
  - Do you collect data? **Yes** → **Other User Content** (the optional sync of the azkar record).
  - Used for **App Functionality** only · **Not linked** to the user · **Not used for tracking**.
  - Nothing else (location is used only on the device and never sent).
- **Pricing and availability**: Free, all countries.
- **App Review information**: no sign-in needed. Notes:
  > Islamic Azkar is a free collection of Islamic remembrances (Hisn al-Muslim) with prayer times,
  > a tasbih counter and widgets. No account is needed. The optional "Sync between devices"
  > (Settings) creates an anonymous 12-character code; only the dates of completed azkar are stored,
  > and users can delete them at https://islamicazkar.net/delete/.
- **Copyright**: `2026 Mkoussa` (the developer name shown on your Apple account)

Then **Add for Review** → **Submit**. Reviews usually take 1–2 days.
