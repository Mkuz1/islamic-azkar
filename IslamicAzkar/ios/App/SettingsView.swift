import SwiftUI
import WidgetKit

struct SettingsView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.openURL) private var openURL
    @State private var locating = false
    @State private var locator: Locator?

    var body: some View {
        @Bindable var am = model
        let s = model.s
        let m = model.m
        let lang = model.lang

        Form {
            Section(s.language) {
                Picker(s.language, selection: $am.lang) {
                    Text("العربية").tag(Lang.ar)
                    Text("English").tag(Lang.en)
                    Text("Français").tag(Lang.fr)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }

            Section(s.appearance) {
                Picker(s.appearance, selection: $am.theme) {
                    Text(s.themeSystem).tag(ThemeMode.system)
                    Text(s.themeLight).tag(ThemeMode.light)
                    Text(s.themeDark).tag(ThemeMode.dark)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }

            Section(m.prayerTimes) {
                if let place = model.place {
                    let times = Prayers.times(place)
                    let next = Prayers.next(place)
                    HStack(spacing: 0) {
                        ForEach(0..<6, id: \.self) { i in
                            VStack(spacing: 3) {
                                Text(m.prayers[i]).font(.caption2.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.6)
                                    .foregroundStyle(i == next.index ? Theme.accent : .secondary)
                                Text(Prayers.clock(times[i], place, lang)).font(.footnote.monospacedDigit().weight(i == next.index ? .bold : .regular))
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.vertical, 4)
                }
                LabeledContent {
                    Text(model.place?.label(lang) ?? m.notSet).foregroundStyle(.secondary)
                } label: { Label(m.location, systemImage: "location.fill") }
                Button {
                    locate()
                } label: {
                    HStack {
                        Label(locating ? m.locating : m.useMyLocation, systemImage: "location.circle")
                        if locating { Spacer(); ProgressView() }
                    }
                }
                .disabled(locating)
                Picker(selection: Binding(get: { model.place.flatMap { p in Cities.all.firstIndex(of: p) } ?? -1 },
                                          set: { if $0 >= 0 { model.place = Cities.all[$0] } })) {
                    Text(m.notSet).tag(-1)
                    ForEach(Cities.all.indices, id: \.self) { i in Text(Cities.all[i].label(lang)).tag(i) }
                } label: { Label(m.chooseCity, systemImage: "building.2") }
                Picker(selection: $am.method) {
                    ForEach(CalcMethod.allCases) { c in Text(m.methods[c.rawValue]).tag(c.rawValue) }
                } label: { Label(m.method, systemImage: "function") }
                Toggle(isOn: $am.asrHanafi) {
                    Label { VStack(alignment: .leading) { Text(m.asrHanafi); Text(m.asrHint).font(.caption).foregroundStyle(.secondary) } }
                    icon: { Image(systemName: "sun.min") }
                }
            }

            Section(s.reading) {
                VStack(alignment: .leading) {
                    HStack {
                        Label(s.textSize, systemImage: "textformat.size")
                        Spacer()
                        Text("\(lang.num(Int(model.textScale * 100)))%").foregroundStyle(.secondary)
                    }
                    Slider(value: $am.textScale, in: 0.8...1.6, step: 0.1)
                    Text("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ").font(.amiri(22 * model.textScale)).frame(maxWidth: .infinity)
                }
                Toggle(isOn: $am.showTranslit) { Label(s.showTranslit, systemImage: "character.textbox") }
                Toggle(isOn: $am.showMeaning) { Label(s.showMeaning, systemImage: "translate") }
                Toggle(isOn: $am.haptics) { Label(s.haptics, systemImage: "iphone.radiowaves.left.and.right") }
            }

            Section(s.notifications) {
                Toggle(isOn: withPermission($am.morningOn)) { Label(s.morningReminder, systemImage: "sun.max.fill") }
                if model.morningOn { timeRow(s.time, $am.morningAt) }
                Toggle(isOn: withPermission($am.eveningOn)) { Label(s.eveningReminder, systemImage: "moon.stars.fill") }
                if model.eveningOn { timeRow(s.time, $am.eveningAt) }
                #if os(macOS)
                MacTestReminderRow()
                #endif
                Toggle(isOn: withPermission($am.generalOn)) {
                    Label { VStack(alignment: .leading) { Text(s.generalReminders); Text(s.generalRemindersHint).font(.caption).foregroundStyle(.secondary) } }
                    icon: { Image(systemName: "bell.badge.fill") }
                }
                if model.generalOn {
                    Picker(s.every, selection: $am.generalEvery) {
                        ForEach([1, 2, 3, 4, 6], id: \.self) { h in Text(hours(h)).tag(h) }
                    }
                    timeRow(s.between, $am.generalFrom)
                    timeRow(s.and, $am.generalTo)
                }
            }

            #if os(iOS)
            Section {
                Button { model.showWidgets = true } label: {
                    HStack(spacing: 12) {
                        SymbolSquare(symbol: "square.grid.2x2.fill", color: .blue, size: 30)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(m.widgetsTitle).foregroundStyle(.primary)
                            Text(m.widgetsHint).font(.caption).foregroundStyle(.secondary).lineLimit(2)
                        }
                        Spacer()
                        Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            #endif

            SyncSection()

            Section(m.ourApps) {
                Button { openURL(mushafURL) } label: {
                    HStack(spacing: 14) {
                        Text("مصحف").font(.ruqaa(20)).foregroundStyle(Brand.mushafGoldLight)
                            .frame(width: 58, height: 58)
                            .background(LinearGradient(colors: [Brand.mushafCardTop, Brand.mushafNight], startPoint: .top, endPoint: .bottom),
                                        in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(Brand.mushafGold.opacity(0.5), lineWidth: 0.8))
                        VStack(alignment: .leading, spacing: 3) {
                            Text(m.mushafTitle).font(.headline).foregroundStyle(.primary)
                            Text(m.mushafText).font(.caption).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                            Text(m.mushafCta).font(.caption.weight(.semibold)).foregroundStyle(Theme.accent)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }

            Section(s.about) {
                UpdateSettingsRow()
                Text(s.aboutText).font(.callout)
                if lang != .en { Text(s.translationNote).font(.footnote).foregroundStyle(.secondary) }
                VStack(spacing: 2) {
                    Text(dedicationAr).font(.amiri(17)).foregroundStyle(.secondary)
                    if let t = dedicationTranslation(lang) { Text(t).font(.caption).foregroundStyle(.tertiary) }
                }
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
            }
        }
        .pageBackdrop()
        .navigationTitle(s.settings)
        #if os(iOS)
        .navigationDestination(isPresented: $am.showWidgets) { WidgetsView() }
        #endif
    }

    private func locate() {
        locating = true
        let l = Locator()
        locator = l
        Task {
            let place = await l.find()
            locating = false
            locator = nil
            if let place { model.place = place } else { model.toast = model.m.locationFailed }
        }
    }

    /// Switching a reminder on asks for notification permission first.
    private func withPermission(_ binding: Binding<Bool>) -> Binding<Bool> {
        Binding(get: { binding.wrappedValue }, set: { on in
            guard on else { binding.wrappedValue = false; return }
            Task {
                let granted = await Reminders.requestPermission()
                await MainActor.run {
                    if granted { binding.wrappedValue = true } else { model.toast = model.s.permissionDenied }
                }
            }
        })
    }

    /// "every … hours" in the app's language.
    private func hours(_ n: Int) -> String {
        switch model.lang {
        case .ar: return n == 1 ? "ساعة" : n == 2 ? "ساعتين" : "\(Lang.ar.num(n)) ساعات"
        case .en: return n == 1 ? "1 hour" : "\(n) hours"
        case .fr: return n == 1 ? "1 heure" : "\(n) heures"
        }
    }

    private func timeRow(_ title: String, _ minutes: Binding<Int>) -> some View {
        DatePicker(title, selection: Binding<Date>(
            get: { Calendar.current.date(bySettingHour: minutes.wrappedValue / 60, minute: minutes.wrappedValue % 60, second: 0, of: .now) ?? .now },
            set: {
                let c = Calendar.current.dateComponents([.hour, .minute], from: $0)
                minutes.wrappedValue = (c.hour ?? 0) * 60 + (c.minute ?? 0)
            }
        ), displayedComponents: .hourAndMinute)
        // Same digits as the prayer times (Arabic-Indic in Arabic).
        .environment(\.locale, model.lang == .ar ? Locale(identifier: "ar@numbers=arab") : model.lang.locale)
    }
}
