# Bugs: Islamic Azkar for Mac v1.0.10 (10)

Tested 2026-11-05 on macOS 27.2 (26B123), window 1920×1050, Sharjah.
Evidence files are in `replica/bugs/evidence/`.

| id | sev | title | status |
| --- | --- | --- | --- |
| BUG-001 | S1 | App crashes when you open anything after changing language | open |
| BUG-002 | S3 | Sidebar stays on the old side after a language change | open |
| BUG-003 | S2 | Dark appearance is unreadable (Auto goes dark every evening) | open |
| BUG-004 | S3 | Favorites shows a dhikr as not done after you completed it | open |
| BUG-005 | S4 | Back button shows on root screens and does nothing | open |
| BUG-006 | S4 | Two Back buttons with different labels in the chapter view | open |
| BUG-007 | S4 | Reminder rows show "1 h" and Latin digits in the Arabic UI | open |
| BUG-008 | S4 | Search empty state prints the whole long query | open |

No fixes yet: the source isn't on this Mac.

---

### BUG-001: App crashes when you open anything after changing language

- Severity: S1 (app quits on the core flow; any unsaved state is lost)
- Flow / case: F08 / F08-N1, F08-N2
- Screen: S01 Home → S02 Chapter
- Build: 1.0.10 (10)  Device: macOS 27.2, Apple silicon

Steps
1. Launch, click Begin.
2. Change language while the main window is open: either Settings → Language → English, or the "EN" pill on Home.
3. (Optional: click the Back pill on Home.)
4. Click the Morning Azkar card on Home.

Expected: Morning Azkar opens in the new language.
Actual: the app quits at once. Reproduced 3 out of 3 times (Arabic → English twice, English → Arabic once), with and without step 3.
It does not happen when the language is picked on the opening screen before Begin, or after a relaunch.
Evidence: `evidence/Islamic Azkar-2026-11-05-173918.ips`, `-174007.ips`, `-174026.ips`.
EXC_CRASH / SIGABRT from an uncaught NSException thrown in `-[NSWindow(NSDisplayCycle) _postWindowNeedsUpdateConstraints]`, reached from `NSHostingView.cancelAsyncRendering()` → `NSHostingView._willUpdateConstraintsForSubtree()`.
This is AppKit's "window needs more Update Constraints passes than there are views" exception, which means a layout feedback loop.
Suspected cause: the language change flips `layoutDirection` / locale on the content but not on the `NavigationSplitView` itself (see BUG-002). The next navigation push then makes the split view and the hosting view keep invalidating each other's constraints.
Likely fix: put the locale and `layoutDirection` on the window root and force a rebuild of the whole split view with `.id(lang)`, or reset the navigation path when the language changes.
Related: the two older crashes from today (`-152228.ips`, `-152828.ips`, v1.0.7) were a different navigation crash (`swift_unexpectedError` in `NavigationColumnState.boundPathChange`). That one did not come back on 1.0.10.
Status: open

### BUG-002: Sidebar stays on the old side after a language change

- Severity: S3 (looks broken; a relaunch fixes it)
- Flow / case: F08 / F08-E1
- Screen: whole window

Steps
1. Start in Arabic (sidebar on the right).
2. Settings → Language → English.

Expected: the sidebar moves to the left with the LTR content.
Actual: the content flips to LTR but the sidebar stays on the right, and the reverse happens English → Arabic. It is only correct after a relaunch.
Evidence: `evidence/BUG-002-sidebar-right-in-english.jpg`, `evidence/BUG-002-sidebar-left-in-arabic.jpg`
Suspected cause: same root as BUG-001.
Status: open

### BUG-003: Dark appearance is unreadable (Auto goes dark every evening)

- Severity: S2 (the content can't be read in Dark. Light is a workaround, but it hits the default "Auto" setting every night, which is when evening azkar are read.)
- Flow / case: F09 / F09-E1
- Screen: all content screens (Home, Settings, chapters)

Steps
1. Settings → Appearance → Dark. Or leave Auto with macOS set to automatic appearance, after sunset.
2. Look at Home or Settings.

Expected: a dark background with light text.
Actual: the sidebar and toolbar go dark, but the content backdrop stays the light cream/green gradient while text and controls turn white. Almost nothing is readable: titles, prayer times, category names and switches.
Evidence: `evidence/BUG-003-dark-home.jpg`, `evidence/BUG-003-dark-settings.jpg`
Suspected cause: the Backdrop gradient and card fills use fixed light colours, while the text uses `.primary` / semantic colours.
Status: open

### BUG-004: Favorites shows a dhikr as not done after you completed it

- Severity: S3
- Flow / case: F04 / F04-E1

Steps
1. Morning Azkar → tap the counter on 1/23 (Ayat al-Kursi) until it shows the check.
2. Heart it, open Favorites.

Expected: the same completed state (check mark) in Favorites.
Actual: Favorites shows the counter back at 0 ("مرة", empty ring). The two views keep separate progress for the same dhikr.
Evidence: `evidence/BUG-favorites-counter.jpg`
Status: open

### BUG-005: Back button shows on root screens and does nothing

- Severity: S4
- Flow / case: F01 / F01-E1

Steps: open Home (or Settings, or Favorites) from the sidebar. Click the "Back" / "رجوع" pill at the top.
Expected: no Back button on a root screen, or a disabled one.
Actual: it looks enabled, and clicking it does nothing.
Status: open

### BUG-006: Two Back buttons with different labels in the chapter view

- Severity: S4
- Flow / case: F02 / F02-E2

Steps: open any chapter.
Expected: one Back control.
Actual: there is a round chevron in the title bar (labelled "الخلف" in Arabic) and a "Back" / "رجوع" pill below it. Both do the same thing, and their labels don't match.
Status: open

### BUG-007: Reminder rows show "1 h" and Latin digits in the Arabic UI

- Severity: S4
- Flow / case: F12 / F12-E1

Steps: Arabic UI → Settings → Reminders.
Expected: Arabic unit ("ساعة"), and the same digits as the prayer times (٠١٢…).
Actual: "كل 1 h". The reminder times use 8:00 / 6:00 / 10:00 while the prayer-time row on the same page uses Arabic-Indic digits.
Status: open

### BUG-008: Search empty state prints the whole long query

- Severity: S4
- Flow / case: F03 / F03-E1

Steps: paste a 300-character string into search.
Expected: no results, with the query cut off (for example the first 40 characters and "…").
Actual: the whole string wraps into a large block in the middle of the screen. No crash.
Evidence: `evidence/BUG-long-query.jpg`
Status: open

---

## To check (not reproduced, or needs you)

- **Test reminder arrival.** Clicking "Send a test reminder" shows the hint. I can't see Notification Centre from the background, so please confirm a banner arrived around 17:55 today.
- **"الكرسي" finds nothing.** Search is diacritic-insensitive (كرسيه works), but it doesn't match the common name "آية الكرسي", which is in the transliteration line only. Consider indexing the `tr` field and chapter titles too.
- **Menu-bar next prayer and widgets.** The strings are in the binary ("Show the next prayer in the menu bar", Home Screen widgets), but there is no setting for them on Mac. Check whether they are meant to ship on macOS.
- **Older crash.** The v1.0.7 `NavigationColumnState.boundPathChange` crash (twice at 15:22–15:28 today) did not reproduce on 1.0.10.
- **Not run:** text-size extremes, share sheet, Mushaf link, narrow window, keyboard-only, VoiceOver labels on counter buttons, offline update check, time-zone/DST change.

## Side effects of this test pass

Settings are back as they were: language Arabic, appearance Auto, reminders on, Hanafi off, favorites empty.
Today's counters changed: Morning Azkar 1/23 and 2/23 are marked done, and tasbih went up by about 32.
