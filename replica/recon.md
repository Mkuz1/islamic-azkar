# Recon map (inferred): Islamic Azkar for Mac

Source code was not on this Mac, so this map comes from the installed app
(`/Applications/Islamic Azkar.app`, v1.0.10 build 10, `com.koussa.IslamicAzkar`),
its bundled data (`azkar.json`, `library.json`, `verses.json`), the strings in the
binary, and clicking through the app on 2026-11-05.

## Screens

| id | screen | how you get there |
| --- | --- | --- |
| S00 | Opening screen (Sadaqa Jariyah dedication, language picker, Begin) | every launch |
| S01 | Home: Hijri date, language pills, search, next-prayer card, "Recommended now" cards, category list | sidebar Home |
| S02 | Chapter reader: dhikr cards with counter, copy, share, favorite, progress bar | Home card, category row, search result |
| S03 | Search results / empty state | Home search field |
| S04 | Favorites (and empty state) | sidebar |
| S05 | Tasbih: bead counter, 6 dhikr chips, target 33/99/100/∞, reset | sidebar |
| S06 | Category chapter list (13 categories, 140 chapters) | sidebar Categories or Home list |
| S07 | Umrah step-by-step guide | sidebar Guide |
| S08 | Settings: language, appearance, prayer times + location + method + Hanafi Asr, reading (text size, transliteration, translation, haptics), reminders (morning, evening, general dhikr every N h between times, test reminder), our apps (Mushaf), about (check for updates, credits) | sidebar or next-prayer card |

Also present in the binary but not reachable on Mac in this pass: Home Screen widgets,
"Show the next prayer in the menu bar", share-as-image.

## Flows

| flow | what the user does |
| --- | --- |
| F00 | Launch, pick language on the opening screen, Begin |
| F01 | Use Home: read next prayer, open a recommended card, open a category |
| F02 | Read a chapter and count each dhikr to its target |
| F03 | Search azkar and open a result |
| F04 | Add a dhikr to favorites, read it there, remove it |
| F05 | Count tasbih, change target, reset |
| F06 | Browse a category from the sidebar |
| F07 | Read the Umrah guide |
| F08 | Change the app language after launch |
| F09 | Change appearance (Auto / Light / Dark) |
| F10 | Set prayer location, method, Hanafi Asr |
| F11 | Reading options (text size, transliteration, translation) |
| F12 | Reminders and test notification |
| F13 | Copy and share a dhikr |
| F14 | Check for updates, Mushaf link |
