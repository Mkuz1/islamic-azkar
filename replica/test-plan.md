# Test plan: Islamic Azkar for Mac

Build: v1.0.10 (10), installed from the GitHub release  Date: 2026-11-05
Env: macOS 27.2, 1920×1050 window, Sharjah location, Arabic + English UI
Method: black-box manual pass with background computer use (no source on this Mac,
so no automated specs, see the note at the bottom).

Result: pass / FAIL (bug id) / not run

| case | flow | type | steps | expected | auto | result |
| --- | --- | --- | --- | --- | --- | --- |
| F00-H1 | opening screen | happy | launch, Begin | Home appears | manual | pass |
| F00-E1 | | edge: pick language on opening screen | launch, pick Arabic, Begin, open Morning Azkar | whole UI in Arabic, no crash | manual | pass |
| F01-H1 | Home | happy | open Home | date, next prayer + countdown, 4 recommended cards, 13 categories with counts | manual | pass |
| F01-H2 | | happy: next-prayer card | click the card | opens prayer settings | manual | pass |
| F01-E1 | | edge: Back on a root screen | click Back pill on Home / Settings / Favorites | hidden or disabled | manual | FAIL (BUG-005) |
| F02-H1 | read and count | happy | Morning Azkar, tap counter on 1/23 (target 1) | check mark, scrolls to 2/23, progress bar moves | manual | pass |
| F02-H2 | | happy: multi-count | tap 2/23 three times | completes at 3, scrolls on | manual | pass |
| F02-E1 | | edge: double/extra tap | tap a completed counter again | stays completed, no over-count | manual | pass |
| F02-E2 | | edge: navigation chrome | look at the toolbar of a chapter | one Back control | manual | FAIL (BUG-006) |
| F03-H1 | search | happy | type كرسيه (no harakat) | matches the Ayat al-Kursi azkar | manual | pass |
| F03-H2 | | happy | type سبحان الله | many results, diacritics ignored | manual | pass |
| F03-H3 | | happy: open result | click a result, then Back | chapter opens on that dhikr; Back returns to results with query kept | manual | pass |
| F03-E1 | | edge: emoji, accents, 300 chars | paste 🤲🏽 + accents + 300 × "a" | no crash, tidy empty state | manual | FAIL (BUG-008) |
| F03-E2 | | edge: spaces only | type 3 spaces | treated as empty, Home shows | manual | pass |
| F03-E3 | | edge: clear button | click ✕ | field clears, Home shows | manual | pass |
| F04-H1 | favorites | happy | heart a dhikr, open Favorites | dhikr listed | manual | pass |
| F04-E1 | | edge: progress shared | complete a dhikr in its chapter, view it in Favorites | same completed state | manual | FAIL (BUG-004) |
| F04-H2 | | happy: remove | un-heart in Favorites | empty state with hint | manual | pass |
| F05-H1 | tasbih | happy | tap bead to 33 | ring fills, count 33 | manual | pass |
| F05-E1 | | edge: past target | tap a 34th time | wraps to 1, round counter +1 | manual | pass |
| F06-H1 | categories | happy | sidebar Travel, open first chapter | list of 11, chapter opens, sidebar highlights Travel | manual | pass |
| F07-H1 | Umrah guide | happy | sidebar Guide | 9 steps with azkar and sources | manual | pass |
| F08-N1 | change language | negative | Settings: Arabic to English, Home, open Morning Azkar | language changes, app keeps working | manual | FAIL (BUG-001, S1 crash) |
| F08-N2 | | negative | Home pill: Arabic to English, open Morning Azkar | same | manual | FAIL (BUG-001) |
| F08-E1 | | edge: layout direction | switch Arabic and English in Settings | sidebar moves to the matching side | manual | FAIL (BUG-002) |
| F09-H1 | appearance | happy | Light | readable | manual | pass |
| F09-E1 | | edge: Dark / Auto after sunset | pick Dark, or Auto with macOS on auto appearance after sunset | readable dark UI | manual | FAIL (BUG-003) |
| F10-E1 | prayer | edge: Hanafi Asr | toggle on, compare Asr time | Asr later | manual | not verified (screen was unreadable in dark, see BUG-003) |
| F11-H1 | reading | happy: text size slider | drag to min and max | text resizes, no clipping | manual | not run |
| F12-H1 | reminders | happy | toggles reflect saved prefs | morning, evening, general on | manual | pass |
| F12-H2 | | happy: test reminder | click Send a test reminder | hint shown, notification arrives in seconds | manual | hint pass, arrival needs you |
| F12-E1 | | edge: localisation | read reminder rows in Arabic | Arabic unit and digits | manual | FAIL (BUG-007) |
| F13-H1 | copy | happy | Copy on دعاء الركوب | clipboard has full text + chapter + app name | manual | pass |
| F13-H2 | share | happy | Share | system share sheet | manual | not run (opens a system sheet) |
| F14-H1 | updates | happy | Check for updates | "latest version" (release tag is v1.0.10) | manual | pass |
| F14-H2 | | Mushaf link | click download link | opens mushafapp.com in browser | manual | not run |

Not covered here, for the manual checklist: offline (update check, location),
time zone / DST change (prayer times, "Recommended now"), narrow window, keyboard-only
navigation, VoiceOver labels on the counter buttons, widgets, the menu-bar next prayer,
the reminder actually arriving at 10:00 / 17:00.

Why no Playwright specs: this is a native SwiftUI Mac app, not a web app, and the
source isn't on this Mac. The automated equivalent is XCUITest in the Xcode project.
BUG-001 is the first test to write: launch, switch language, open a Home card, assert
the chapter title exists.
