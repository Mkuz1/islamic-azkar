import SwiftUI

// MARK: - Tasbih

/// An electronic tasbih: press the remote (or Play/Pause) to count.
struct TVTasbihView: View {
    @Environment(TVModel.self) private var model
    @FocusState private var focused: Bool

    var body: some View {
        let lang = model.lang, s = model.s, t = model.t
        let target = model.tasbihTarget, count = model.tasbihCount
        let inRound = target > 0 ? count % target : count
        let rounds = target > 0 ? count / target : 0
        let phrase = model.tasbihPhrase % TasbihPhrases.ar.count
        HStack(spacing: 70) {
            Button(action: tap) {
                VStack(spacing: 26) {
                    Text(TasbihPhrases.ar[phrase]).font(.amiri(64, bold: true)).foregroundStyle(Brand.mushafGold)
                        .lineLimit(1).minimumScaleFactor(0.5)
                    if lang != .ar { Text(TasbihPhrases.meaning(phrase, lang)).font(.callout).foregroundStyle(.secondary) }
                    ZStack {
                        Circle().stroke(.white.opacity(0.12), lineWidth: 18)
                        Circle().trim(from: 0, to: target > 0 ? Double(inRound) / Double(target) : 1)
                            .stroke(Brand.mushafGold, style: StrokeStyle(lineWidth: 18, lineCap: .round))
                            .rotationEffect(.degrees(-90))
                            .animation(.easeOut(duration: 0.2), value: inRound)
                        VStack(spacing: 6) {
                            Text(lang.num(inRound)).font(.system(size: 120, weight: .light, design: .rounded).monospacedDigit())
                            Text(target > 0 ? "/ \(lang.num(target))" : "∞").font(.title3).foregroundStyle(.secondary)
                        }
                    }
                    .frame(width: 400, height: 400)
                    Text(t.pressToCount).font(.caption).foregroundStyle(.secondary)
                }
                .padding(50)
            }
            .buttonStyle(.card)
            .focused($focused)
            .onPlayPauseCommand(perform: tap)

            VStack(alignment: .leading, spacing: 26) {
                Text(s.tasbih).font(.system(size: 54, weight: .bold))
                stat(t.round, lang.num(rounds))
                stat(s.total, lang.num(model.tasbihTotal))
                Text(t.phrase).font(.headline).foregroundStyle(Brand.mushafGold)
                Button {
                    model.tasbihPhrase = (phrase + 1) % TasbihPhrases.ar.count
                    model.tasbihCount = 0
                } label: {
                    Label { Text(TasbihPhrases.ar[(phrase + 1) % TasbihPhrases.ar.count]).font(.amiri(30)) }
                        icon: { Image(systemName: "arrow.triangle.2.circlepath") }
                }
                Text(s.target).font(.headline).foregroundStyle(Brand.mushafGold)
                HStack(spacing: 20) {
                    ForEach([33, 99, 100, 0], id: \.self) { n in
                        Button { model.tasbihTarget = n; model.tasbihCount = 0 } label: {
                            Text(n == 0 ? "∞" : lang.num(n)).fontWeight(n == target ? .bold : .regular)
                        }
                        .tint(n == target ? Brand.mushafGold : nil)
                    }
                }
                Button(s.reset, role: .destructive) { model.tasbihCount = 0 }
            }
            .frame(width: 620)
            .focusSection()   // the remote moves between the counter and these controls as two groups
        }
        .padding(.horizontal, 90)
        .onAppear { focused = true }
    }

    private func stat(_ label: String, _ value: String) -> some View {
        HStack { Text(label).foregroundStyle(.secondary); Spacer(); Text(value).font(.title3.monospacedDigit().weight(.semibold)) }
    }

    private func tap() {
        model.tasbihCount += 1
        model.tasbihTotal += 1
        TasbihHistory.add()
    }
}

// MARK: - Record

/// The month calendar of completed azkar, the streak and today's readings.
struct TVRecordView: View {
    @Environment(TVModel.self) private var model
    @State private var month = Calendar.current.date(from: Calendar.current.dateComponents([.year, .month], from: .now)) ?? .now

    var body: some View {
        let _ = model.historyVersion
        let lang = model.lang, t = model.t, cal = Calendar.current
        let days = AzkarHistory.all()
        let range = cal.range(of: .day, in: .month, for: month) ?? 1..<31
        let lead = (cal.component(.weekday, from: month) - cal.firstWeekday + 7) % 7
        let keys = range.map { AzkarHistory.dayKey(cal.date(byAdding: .day, value: $0 - 1, to: month) ?? month) }
        let active = keys.filter { (days[$0]?.count ?? 0) > 0 }.count
        let finished = keys.reduce(0) { $0 + (days[$1]?.count ?? 0) }
        let todayKey = AzkarHistory.dayKey(.now)
        HStack(alignment: .top, spacing: 60) {
            VStack(alignment: .leading, spacing: 24) {
                Text(t.record).font(.system(size: 54, weight: .bold))
                Text(t.recordSubtitle).foregroundStyle(.secondary)
                stat("flame.fill", lang.num(AzkarHistory.currentStreak(days)), t.streak)
                stat("calendar", lang.num(active), t.activeDays)
                stat("checkmark.circle.fill", lang.num(finished), t.finished)
                Text(t.today).font(.headline).foregroundStyle(Brand.mushafGold).padding(.top, 10)
                let today = days[todayKey]?.readings ?? []
                if today.isEmpty {
                    Text(t.nothing).foregroundStyle(.secondary)
                } else {
                    ForEach(today, id: \.self) { key in
                        if let r = Reading(key: key) {
                            Label(AzkarData.shared.title(r, model.s, lang), systemImage: "checkmark.circle.fill").foregroundStyle(.white)
                        }
                    }
                }
            }
            .frame(width: 560, alignment: .leading)

            VStack(spacing: 22) {
                HStack {
                    Button { shift(-1) } label: { Image(systemName: lang.isRTL ? "chevron.right" : "chevron.left") }
                    Spacer()
                    Text(month.formatted(.dateTime.month(.wide).year().locale(lang.locale))).font(.title3.weight(.semibold))
                    Spacer()
                    Button { shift(1) } label: { Image(systemName: lang.isRTL ? "chevron.left" : "chevron.right") }
                }
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 14), count: 7), spacing: 14) {
                    // One grid, so every cell needs its own id: "w0…w6" (weekdays), "l0…" (lead-in), day keys.
                    ForEach((0..<7).map { "w\($0)" }, id: \.self) { w in
                        let i = Int(w.dropFirst()) ?? 0
                        let name = Hijri.weekday(lang, (i + cal.firstWeekday - 1) % 7)
                        Text(lang == .ar ? name : String(name.prefix(3)))
                            .font(.caption).foregroundStyle(.secondary).lineLimit(1).minimumScaleFactor(0.7)
                    }
                    ForEach((0..<lead).map { "l\($0)" }, id: \.self) { _ in Color.clear.frame(height: 76) }
                    ForEach(Array(keys.enumerated()), id: \.element) { i, key in
                        let n = days[key]?.count ?? 0
                        VStack(spacing: 4) {
                            Text(lang.num(i + 1)).font(.headline.monospacedDigit())
                            Text(n > 0 ? lang.num(n) : " ").font(.caption2).foregroundStyle(Brand.mushafGold)
                        }
                        .frame(maxWidth: .infinity, minHeight: 76)
                        .background(n == 0 ? Color.white.opacity(0.05) : Brand.emerald.opacity(min(0.35 + Double(n) * 0.15, 0.95)),
                                    in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(key == todayKey ? Brand.mushafGold : .clear, lineWidth: 3))
                    }
                }
            }
            .padding(36)
            .background(.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 30, style: .continuous))
        }
        .padding(.horizontal, 90).padding(.vertical, 40)
        .task { await AzkarSync.syncNow() }
    }

    private func stat(_ symbol: String, _ value: String, _ label: String) -> some View {
        HStack(spacing: 18) {
            Image(systemName: symbol).font(.title2).foregroundStyle(Brand.emeraldBright).frame(width: 50)
            Text(value).font(.system(size: 40, weight: .bold).monospacedDigit())
            Text(label).foregroundStyle(.secondary)
        }
    }

    private func shift(_ months: Int) {
        if let d = Calendar.current.date(byAdding: .month, value: months, to: month) { month = d }
    }
}

// MARK: - Settings

struct TVSettingsView: View {
    @Environment(TVModel.self) private var model
    @State private var typed = ""
    @State private var busy = false
    @State private var message: String?
    @State private var code = AzkarSync.code

    var body: some View {
        let s = model.s, m = model.m, t = model.t, lang = model.lang
        ScrollView {
            VStack(alignment: .leading, spacing: 30) {
                Text(s.settings).font(.system(size: 54, weight: .bold))

                header(s.language)
                HStack(spacing: 24) {
                    ForEach(Lang.allCases) { l in
                        Button { model.lang = l } label: {
                            Label(name(l), systemImage: l == lang ? "checkmark" : "globe")
                        }
                    }
                }

                header(m.prayerTimes)
                NavigationLink {
                    TVPickerList(title: t.city, items: Cities.all.map { $0.label(lang) },
                                 selected: Cities.all.firstIndex(of: model.place)) { model.place = Cities.all[$0] }
                } label: { row(t.city, model.place.label(lang)) }
                NavigationLink {
                    TVPickerList(title: m.method, items: m.methods, selected: model.method) { model.method = $0 }
                } label: { row(m.method, m.methods[safe: model.method] ?? "") }

                header(t.syncTitle)
                Text(t.syncIntro).font(.callout).foregroundStyle(.secondary)
                if let code {
                    VStack(alignment: .leading, spacing: 10) {
                        Text(t.yourCode).font(.caption).foregroundStyle(.secondary)
                        Text(AzkarSync.display(code)).font(.system(size: 54, weight: .semibold, design: .monospaced))
                        Text(t.codeHint).font(.caption).foregroundStyle(.secondary)
                    }
                    HStack(spacing: 24) {
                        Button(t.syncNow) { Task { busy = true; await AzkarSync.syncNow(); model.historyVersion += 1; busy = false } }
                        Button(t.stop, role: .destructive) { AzkarSync.stop(); self.code = nil }
                    }
                } else {
                    HStack(spacing: 24) {
                        Button(t.turnOn) {
                            Task {
                                busy = true; message = nil
                                do { self.code = try await AzkarSync.start() } catch { message = t.offline }
                                busy = false
                            }
                        }
                        TextField(t.enterCode, text: $typed)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                            .frame(width: 520)
                        Button(t.link) {
                            Task {
                                busy = true; message = nil
                                do {
                                    try await AzkarSync.link(typed)
                                    self.code = AzkarSync.code; typed = ""; message = t.linked; model.historyVersion += 1
                                } catch { message = t.badCode }
                                busy = false
                            }
                        }
                        .disabled(AzkarSync.normalize(typed).count != 12 || busy)
                    }
                }
                if let message { Text(message).font(.callout).foregroundStyle(Brand.mushafGold) }

                header(s.about)
                Text(s.aboutText).font(.callout).foregroundStyle(.secondary)
                    .focusable()   // lets the page scroll down to it
            }
            .padding(.horizontal, 90).padding(.vertical, 50)
        }
    }

    private func name(_ l: Lang) -> String {
        switch l { case .ar: "العربية"; case .en: "English"; case .fr: "Français" }
    }

    private func header(_ text: String) -> some View {
        Text(text).font(.headline).foregroundStyle(Brand.mushafGold).textCase(.uppercase).padding(.top, 10)
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack { Text(label); Spacer(); Text(value).foregroundStyle(.secondary) }
            .frame(width: 1100)
    }
}

/// A list to choose one item from (city, calculation method).
struct TVPickerList: View {
    @Environment(\.dismiss) private var dismiss
    let title: String
    let items: [String]
    let selected: Int?
    let pick: (Int) -> Void

    var body: some View {
        List {
            ForEach(Array(items.enumerated()), id: \.offset) { i, item in
                Button {
                    pick(i); dismiss()
                } label: {
                    HStack { Text(item); Spacer(); if i == selected { Image(systemName: "checkmark") } }
                }
            }
        }
        .navigationTitle(title)
    }
}
