import SwiftUI

/// Tells the user when a newer iPhone build is published on GitHub.
///
/// iOS does not let an app install an IPA itself, so this only announces the new version
/// and opens its release page; the user installs it from there (e.g. with Sideloadly or
/// AltStore). A release is offered only if it carries an `IslamicAzkar.ipa` asset and its
/// tag ("v1.0.4") is newer than this app's version.
@Observable
final class UpdateChecker {
    static let shared = UpdateChecker()

    struct Release: Equatable { let version: String; let notes: String; let page: URL }
    enum State: Equatable { case idle, checking, upToDate, available(Release), failed }

    var state: State = .idle
    private var lastCheck: Date?

    private static let latest = URL(string: "https://api.github.com/repos/Mkuz1/islamic-azkar-releases/releases/latest")!
    private static let asset = "IslamicAzkar.ipa"
    private static let dismissedKey = "update_dismissed"

    static var installedVersion: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0" }

    var dismissed: String? {
        get { UserDefaults.standard.string(forKey: Self.dismissedKey) }
        set { UserDefaults.standard.set(newValue, forKey: Self.dismissedKey) }
    }

    @MainActor
    func check(manual: Bool) async {
        if !manual, let last = lastCheck, Date.now.timeIntervalSince(last) < 12 * 3600 { return }
        lastCheck = .now
        if manual { state = .checking }
        do {
            var req = URLRequest(url: Self.latest, timeoutInterval: 15)
            req.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")
            let (body, response) = try await URLSession.shared.data(for: req)
            guard (response as? HTTPURLResponse)?.statusCode == 200,
                  let o = try JSONSerialization.jsonObject(with: body) as? [String: Any],
                  let tag = o["tag_name"] as? String,
                  let page = (o["html_url"] as? String).flatMap(URL.init(string:)) else { throw URLError(.badServerResponse) }
            let assets = o["assets"] as? [[String: Any]] ?? []
            let hasIpa = assets.contains { ($0["name"] as? String) == Self.asset }
            if hasIpa, Self.isNewer(tag, than: Self.installedVersion), manual || dismissed != tag {
                state = .available(Release(version: tag, notes: o["body"] as? String ?? "", page: page))
            } else {
                state = manual ? .upToDate : .idle
            }
        } catch {
            state = manual ? .failed : .idle
        }
    }

    /// Numeric comparison of dotted versions ("1.0.10" > "1.0.9").
    static func isNewer(_ candidate: String, than installed: String) -> Bool {
        func parts(_ v: String) -> [Int] {
            v.trimmingCharacters(in: CharacterSet(charactersIn: "vV ")).split(separator: ".").map { Int($0.prefix { $0.isNumber }) ?? 0 }
        }
        let a = parts(candidate), b = parts(installed)
        for i in 0..<max(a.count, b.count) {
            let x = i < a.count ? a[i] : 0, y = i < b.count ? b[i] : 0
            if x != y { return x > y }
        }
        return false
    }
}

struct UpdateTexts {
    let available, open, later, check, checking, upToDate, failed: String
    let version: (String) -> String
    let current: (String) -> String
    let howTo: String

    static func of(_ lang: Lang) -> UpdateTexts {
        switch lang {
        case .ar: return UpdateTexts(available: "يتوفّر إصدار جديد", open: "صفحة التنزيل", later: "لاحقًا", check: "البحث عن تحديثات",
                                     checking: "جارٍ البحث…", upToDate: "لديك أحدث إصدار", failed: "تعذّر البحث، حاول لاحقًا.",
                                     version: { "الإصدار \($0)" }, current: { "الإصدار الحالي \($0)" },
                                     howTo: "لا يسمح iOS للتطبيق بتثبيت تحديثه بنفسه: نزّل الملف من صفحة الإصدار وثبّته كما ثبّتّ التطبيق أول مرة.")
        case .en: return UpdateTexts(available: "A new version is available", open: "Download page", later: "Later", check: "Check for updates",
                                     checking: "Checking…", upToDate: "You have the latest version", failed: "Couldn't check. Try again later.",
                                     version: { "Version \($0)" }, current: { "Installed version \($0)" },
                                     howTo: "iOS doesn't let an app install its own update: download the file from the release page and install it the way you first installed the app.")
        case .fr: return UpdateTexts(available: "Une nouvelle version est disponible", open: "Page de téléchargement", later: "Plus tard", check: "Rechercher des mises à jour",
                                     checking: "Recherche…", upToDate: "Vous avez la dernière version", failed: "Recherche impossible. Réessayez plus tard.",
                                     version: { "Version \($0)" }, current: { "Version installée \($0)" },
                                     howTo: "iOS ne permet pas à une application d'installer elle-même sa mise à jour : téléchargez le fichier depuis la page de la version et installez-le comme la première fois.")
        }
    }
}

/// Card at the top of Home when a newer version exists.
struct UpdateBanner: View {
    @Environment(AppModel.self) private var model
    @Environment(\.openURL) private var openURL
    private var checker = UpdateChecker.shared

    var body: some View {
        Group {
            if case .available(let r) = checker.state {
                let t = UpdateTexts.of(model.lang)
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 12) {
                        SymbolSquare(symbol: "arrow.down.app.fill", color: .green, size: 36)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(t.available).font(.headline)
                            Text(t.version(r.version.trimmingCharacters(in: CharacterSet(charactersIn: "v")))).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                    Text(t.howTo).font(.footnote).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                    HStack {
                        Button(t.later) {
                            checker.dismissed = r.version
                            withAnimation { checker.state = .idle }
                        }
                        .buttonStyle(.borderless)
                        Spacer()
                        Button { openURL(r.page) } label: { Label(t.open, systemImage: "safari").padding(.horizontal, 6) }
                            .prominentGlassButton()
                            .tint(Theme.accent)
                    }
                }
                .padding(18)
                .glassCard(cornerRadius: 24)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .task { await checker.check(manual: false) }
    }
}

/// Settings row: installed version and a manual check.
struct UpdateSettingsRow: View {
    @Environment(AppModel.self) private var model
    @Environment(\.openURL) private var openURL
    private var checker = UpdateChecker.shared

    var body: some View {
        let t = UpdateTexts.of(model.lang)
        Button {
            if case .available(let r) = checker.state { openURL(r.page) } else { Task { await checker.check(manual: true) } }
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(t.check).foregroundStyle(.primary)
                    Text(status(t)).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                if checker.state == .checking { ProgressView() }
            }
        }
    }

    private func status(_ t: UpdateTexts) -> String {
        switch checker.state {
        case .idle: return t.current(UpdateChecker.installedVersion)
        case .checking: return t.checking
        case .upToDate: return t.upToDate
        case .available(let r): return t.version(r.version)
        case .failed: return t.failed
        }
    }
}
