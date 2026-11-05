import AppKit
import SwiftUI

// The menu bar item of the Mac app: the app icon (and the next prayer), and a panel with the
// azkar due now and today's prayer times for the place chosen in Settings.

/// Ticks every 30 seconds for the menu bar label. (A TimelineView inside a MenuBarExtra label makes
/// SwiftUI re-layout the status item endlessly, freezing the app at launch.)
@Observable
final class MenuClock {
    static let shared = MenuClock()
    var now = Date()
    private var timer: Timer?

    private init() {
        timer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
            self?.now = Date()
            MacReminders.check()   // backup for the scheduled morning / evening reminders
        }
    }
}

/// Icon in the menu bar, followed by the next prayer and its time when that option is on.
struct MenuBarLabel: View {
    @Environment(AppModel.self) private var model
    @AppStorage("menubar_next_prayer") private var showNext = true
    private let clock = MenuClock.shared

    var body: some View {
        // A bell while the azkar of a reminder that has come due are still unread.
        let bell = MacReminders.pending(at: clock.now) != nil ? "🔔 " : ""
        if showNext, let place = model.place {
            let next = Prayers.next(place, now: clock.now)
            Label {
                Text("\(bell)\(model.m.prayers[next.index]) \(Prayers.clock(next.time, place, model.lang))")
            } icon: {
                Image("MenuBarIcon")
            }
            .labelStyle(.titleAndIcon)
        } else if !bell.isEmpty {
            Label { Text("🔔") } icon: { Image("MenuBarIcon") }.labelStyle(.titleAndIcon)
        } else {
            Image("MenuBarIcon")
        }
    }
}

/// Texts for the Mac menu bar panel.
struct MacTexts {
    let remindNow, doneToday, notYet, readNow, readAgain, nextPrayer, todayTimes, noPlace, setPlace: String
    let openApp, showNext, openAtLogin, quit, method: String
    let inTime: (String) -> String
    let streak: (Int) -> String

    static func of(_ lang: Lang) -> MacTexts {
        switch lang {
        case .ar: return MacTexts(
            remindNow: "تذكير بالأذكار", doneToday: "قرأتها اليوم، تقبّل الله منك", notYet: "لم تقرأها بعد اليوم",
            readNow: "اقرأ الآن", readAgain: "اقرأ مرة أخرى", nextPrayer: "الصلاة القادمة", todayTimes: "مواقيت اليوم",
            noPlace: "اختر مدينتك أو موقعك في الإعدادات لعرض مواقيت الصلاة.", setPlace: "تحديد الموقع",
            openApp: "فتح أذكار إسلامية", showNext: "إظهار الصلاة القادمة في شريط القوائم", openAtLogin: "التشغيل عند بدء الجهاز",
            quit: "إنهاء", method: "دبي · دلوك (الشؤون الإسلامية بدبي)",
            inTime: { "بعد \($0)" }, streak: { "\(Lang.ar.num($0)) أيام متتالية" })
        case .en: return MacTexts(
            remindNow: "Azkar reminder", doneToday: "Read today, may Allah accept it", notYet: "Not read yet today",
            readNow: "Read now", readAgain: "Read again", nextPrayer: "Next prayer", todayTimes: "Today's prayer times",
            noPlace: "Choose your city or location in Settings to see the prayer times.", setPlace: "Set location",
            openApp: "Open Islamic Azkar", showNext: "Show the next prayer in the menu bar", openAtLogin: "Open at login",
            quit: "Quit", method: "Dubai · Dulook DXB (IACAD)",
            inTime: { "in \($0)" }, streak: { "\($0)-day streak" })
        case .fr: return MacTexts(
            remindNow: "Rappel des adhkar", doneToday: "Lus aujourd'hui, qu'Allah les accepte", notYet: "Pas encore lus aujourd'hui",
            readNow: "Lire maintenant", readAgain: "Relire", nextPrayer: "Prochaine prière", todayTimes: "Horaires du jour",
            noPlace: "Choisissez votre ville ou position dans les Réglages pour voir les horaires de prière.", setPlace: "Choisir le lieu",
            openApp: "Ouvrir Islamic Azkar", showNext: "Afficher la prochaine prière dans la barre des menus", openAtLogin: "Ouvrir à la connexion",
            quit: "Quitter", method: "Dubaï · Dulook DXB (IACAD)",
            inTime: { "dans \($0)" }, streak: { "\($0) jours d'affilée" })
        }
    }
}

/// Whether a reading (e.g. "morning") was completed today, and the current streak.
enum DailyAzkar {
    private static var today: Int { Int((Date.now.timeIntervalSince1970 + Double(TimeZone.current.secondsFromGMT())) / 86_400) }

    static func doneToday(_ r: Reading) -> Bool {
        let d = SharedStore.defaults
        return d.object(forKey: "streak_day_\(r.key)") != nil && d.integer(forKey: "streak_day_\(r.key)") == today
    }

    static func streak(_ r: Reading) -> Int { SharedStore.defaults.integer(forKey: "streak_\(r.key)") }
}

/// The panel that opens from the menu bar icon.
struct MenuBarPanel: View {
    @Environment(AppModel.self) private var model
    @Environment(\.openWindow) private var openWindow
    @AppStorage("menubar_next_prayer") private var showNext = true
    @State private var atLogin = LaunchAtLogin.isOn

    var body: some View {
        let lang = model.lang
        let t = MacTexts.of(lang)
        TimelineView(.periodic(from: .now, by: 1)) { ctx in
            VStack(alignment: .leading, spacing: 12) {
                header
                reminder(t, now: ctx.date)
                prayers(t, now: ctx.date)
                Divider()
                footer(t)
            }
            .padding(16)
            .frame(width: 340)
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            Image(nsImage: NSApp.applicationIconImage).resizable().frame(width: 34, height: 34)
            VStack(alignment: .leading, spacing: 1) {
                Text(model.s.appName).font(.headline)
                Text(Hijri.format(model.lang, Hijri.of())).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
        }
    }

    /// The azkar due now (morning, after prayer, evening or before sleep) and whether they were read today.
    private func reminder(_ t: MacTexts, now: Date) -> some View {
        let reading = Reading.recommended(at: now)
        let done = DailyAzkar.doneToday(reading)
        let streak = DailyAzkar.streak(reading)
        return VStack(alignment: .leading, spacing: 8) {
            Text(t.remindNow).font(.caption.weight(.semibold)).textCase(.uppercase).foregroundStyle(.secondary)
            HStack(spacing: 10) {
                Image(systemName: symbol(reading))
                    .font(.system(size: 16, weight: .semibold)).foregroundStyle(.white)
                    .frame(width: 32, height: 32).background(color(reading), in: RoundedRectangle(cornerRadius: 8))
                VStack(alignment: .leading, spacing: 2) {
                    Text(AzkarData.shared.title(reading, model.s, model.lang)).font(.body.weight(.semibold))
                    Label(done ? t.doneToday : t.notYet, systemImage: done ? "checkmark.circle.fill" : "bell.badge")
                        .font(.caption).foregroundStyle(done ? Theme.accent : .orange)
                    if done, streak > 1 { Text(t.streak(streak)).font(.caption2).foregroundStyle(.secondary) }
                }
                Spacer()
                Button(done ? t.readAgain : t.readNow) { open(reading) }
                    .buttonStyle(.borderedProminent).controlSize(.small)
            }
        }
        .padding(12)
        .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder
    private func prayers(_ t: MacTexts, now: Date) -> some View {
        if let place = model.place {
            let lang = model.lang
            let times = Prayers.times(place, day: now)
            let next = Prayers.next(place, now: now)
            let left = max(0, Int(next.time.timeIntervalSince(now)))
            let countdown = lang.num(left / 3600) + ":" + pad(lang.num(left / 60 % 60), lang) + ":" + pad(lang.num(left % 60), lang)
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .firstTextBaseline) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(t.nextPrayer).font(.caption).foregroundStyle(.secondary)
                        Text(model.m.prayers[next.index]).font(.title2.weight(.semibold))
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(Prayers.clock(next.time, place, lang)).font(.title2.monospacedDigit())
                        Text(t.inTime(countdown)).font(.caption.monospacedDigit()).foregroundStyle(.secondary)
                    }
                }
                VStack(spacing: 0) {
                    ForEach(0..<6, id: \.self) { i in
                        HStack {
                            Text(model.m.prayers[i])
                            Spacer()
                            Text(Prayers.clock(times[i], place, lang)).monospacedDigit()
                        }
                        .font(.callout.weight(i == next.index ? .semibold : .regular))
                        .foregroundStyle(i == next.index ? Theme.accent : (i == Prayers.sunrise ? .secondary : .primary))
                        .padding(.vertical, 5).padding(.horizontal, 10)
                        .background(i == next.index ? Theme.accent.opacity(0.12) : .clear, in: RoundedRectangle(cornerRadius: 7))
                    }
                }
                Text("\(place.label(lang)) · \(model.m.methods[safe: model.method] ?? t.method)")
                    .font(.caption2).foregroundStyle(.tertiary).lineLimit(1)
            }
        } else {
            VStack(alignment: .leading, spacing: 8) {
                Text(t.noPlace).font(.callout).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                Button(t.setPlace) { model.showDedication = false; model.tab = .settings; showMainWindow(openWindow) }
            }
        }
    }

    private func footer(_ t: MacTexts) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Toggle(t.showNext, isOn: $showNext).toggleStyle(.checkbox)
            Toggle(t.openAtLogin, isOn: Binding(get: { atLogin }, set: { LaunchAtLogin.set($0); atLogin = LaunchAtLogin.isOn }))
                .toggleStyle(.checkbox)
            HStack {
                Button(t.openApp) { showMainWindow(openWindow) }
                Spacer()
                Button(t.quit) { NSApp.terminate(nil) }
            }
            .padding(.top, 4)
        }
        .font(.callout)
    }

    private func open(_ reading: Reading) {
        model.open(reading)
        showMainWindow(openWindow)
    }

    private func pad(_ s: String, _ lang: Lang) -> String { s.count < 2 ? (lang == .ar ? "٠" : "0") + s : s }

    private func symbol(_ r: Reading) -> String {
        switch r {
        case .morning: return "sun.max.fill"
        case .evening: return "sunset.fill"
        case .chapter(28): return "moon.zzz.fill"
        default: return "building.columns.fill"
        }
    }

    private func color(_ r: Reading) -> Color {
        switch r {
        case .morning: return .orange
        case .evening: return .indigo
        case .chapter(28): return .blue
        default: return .green
        }
    }
}
