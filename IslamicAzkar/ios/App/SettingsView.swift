import SwiftUI
import WidgetKit

struct SettingsView: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var m = model
        let s = model.s
        let lang = model.lang

        Form {
            Section(s.language) {
                Picker(s.language, selection: $m.lang) {
                    Text("العربية").tag(Lang.ar)
                    Text("English").tag(Lang.en)
                    Text("Français").tag(Lang.fr)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }

            Section(s.appearance) {
                Picker(s.appearance, selection: $m.theme) {
                    Text(s.themeSystem).tag(ThemeMode.system)
                    Text(s.themeLight).tag(ThemeMode.light)
                    Text(s.themeDark).tag(ThemeMode.dark)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }

            Section(s.reading) {
                VStack(alignment: .leading) {
                    HStack {
                        Label(s.textSize, systemImage: "textformat.size")
                        Spacer()
                        Text("\(lang.num(Int(model.textScale * 100)))%").foregroundStyle(.secondary)
                    }
                    Slider(value: $m.textScale, in: 0.8...1.6, step: 0.1)
                    Text("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ").font(.amiri(22 * model.textScale)).frame(maxWidth: .infinity)
                }
                Toggle(isOn: $m.showTranslit) { Label(s.showTranslit, systemImage: "character.textbox") }
                Toggle(isOn: $m.showMeaning) { Label(s.showMeaning, systemImage: "translate") }
                Toggle(isOn: $m.haptics) { Label(s.haptics, systemImage: "iphone.radiowaves.left.and.right") }
            }

            Section(s.notifications) {
                Toggle(isOn: withPermission($m.morningOn)) { Label(s.morningReminder, systemImage: "sun.max.fill") }
                if model.morningOn { timeRow(s.time, $m.morningAt) }
                Toggle(isOn: withPermission($m.eveningOn)) { Label(s.eveningReminder, systemImage: "moon.stars.fill") }
                if model.eveningOn { timeRow(s.time, $m.eveningAt) }
                Toggle(isOn: withPermission($m.generalOn)) {
                    Label { VStack(alignment: .leading) { Text(s.generalReminders); Text(s.generalRemindersHint).font(.caption).foregroundStyle(.secondary) } }
                    icon: { Image(systemName: "bell.badge.fill") }
                }
                if model.generalOn {
                    Picker(s.every, selection: $m.generalEvery) {
                        ForEach([1, 2, 3, 4, 6], id: \.self) { h in Text("\(lang.num(h)) h").tag(h) }
                    }
                    timeRow(s.between, $m.generalFrom)
                    timeRow(s.and, $m.generalTo)
                }
            }

            Section {
                Text(s.widgetsHint).font(.callout).foregroundStyle(.secondary)
            } header: { Text(s.widgets) }

            Section(s.about) {
                Text(s.aboutText).font(.callout)
                if lang != .en { Text(s.translationNote).font(.footnote).foregroundStyle(.secondary) }
                Text("صدقة جارية عن محمد كوسا وديما غنوم")
                    .font(.amiri(17)).foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
            }
        }
        .navigationTitle(s.settings)
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

    private func timeRow(_ title: String, _ minutes: Binding<Int>) -> some View {
        DatePicker(title, selection: Binding<Date>(
            get: { Calendar.current.date(bySettingHour: minutes.wrappedValue / 60, minute: minutes.wrappedValue % 60, second: 0, of: .now) ?? .now },
            set: {
                let c = Calendar.current.dateComponents([.hour, .minute], from: $0)
                minutes.wrappedValue = (c.hour ?? 0) * 60 + (c.minute ?? 0)
            }
        ), displayedComponents: .hourAndMinute)
    }
}
