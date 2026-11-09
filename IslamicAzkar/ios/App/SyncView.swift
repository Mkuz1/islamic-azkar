import CoreImage.CIFilterBuiltins
import SwiftUI

/// Texts for syncing between devices.
struct SyncTexts {
    let title, intro, turnOn, haveCode, enterCode, link, linking, yourCode, scanHint, syncNow, lastSync, never,
        stop, stopConfirm, badCode, offline, linked: String

    static func of(_ lang: Lang) -> SyncTexts {
        switch lang {
        case .ar: return SyncTexts(
            title: "المزامنة بين الأجهزة",
            intro: "اعرض سجلّ أذكارك على كل أجهزتك: آيفون وآيباد وماك وأندرويد والموقع. بلا حساب ولا بريد: رمز خاص يربط أجهزتك، ثم تتم المزامنة تلقائيًا.",
            turnOn: "تفعيل المزامنة", haveCode: "لديّ رمز من جهاز آخر", enterCode: "أدخل رمز المزامنة", link: "ربط", linking: "جارٍ الربط…",
            yourCode: "رمز المزامنة", scanHint: "على جهازك الآخر: امسح الرمز بالكاميرا، أو أدخله في الإعدادات ← المزامنة (وفي الموقع: سجلّي ← المزامنة). احتفظ به لنفسك.",
            syncNow: "زامن الآن", lastSync: "آخر مزامنة", never: "لم تتم بعد",
            stop: "إيقاف المزامنة على هذا الجهاز", stopConfirm: "يبقى سجلّك على هذا الجهاز وعلى أجهزتك الأخرى.",
            badCode: "الرمز غير صحيح. تأكد منه وحاول مرة أخرى.", offline: "تعذّر الاتصال. تحقق من الإنترنت وحاول مرة أخرى.", linked: "تم ربط هذا الجهاز")
        case .en: return SyncTexts(
            title: "Sync between devices",
            intro: "See your azkar record on all your devices: iPhone, iPad, Mac, Android and the website. No account or email: a private code links your devices, then they sync automatically.",
            turnOn: "Turn on sync", haveCode: "I have a code from another device", enterCode: "Enter the sync code", link: "Link", linking: "Linking…",
            yourCode: "Sync code", scanHint: "On your other device, scan this with the camera, or enter the code in Settings → Sync (on the website: Record → Sync). Keep it to yourself.",
            syncNow: "Sync now", lastSync: "Last synced", never: "not yet",
            stop: "Stop syncing on this device", stopConfirm: "Your record stays on this device and on your other devices.",
            badCode: "That code isn't right. Check it and try again.", offline: "Couldn't connect. Check your internet and try again.", linked: "This device is linked")
        case .fr: return SyncTexts(
            title: "Synchronisation entre appareils",
            intro: "Retrouvez votre historique d'adhkar sur tous vos appareils : iPhone, iPad, Mac, Android et le site. Sans compte ni e-mail : un code privé relie vos appareils, puis tout se synchronise automatiquement.",
            turnOn: "Activer la synchronisation", haveCode: "J'ai un code d'un autre appareil", enterCode: "Saisissez le code de synchronisation", link: "Relier", linking: "Liaison…",
            yourCode: "Code de synchronisation", scanHint: "Sur votre autre appareil, scannez-le avec l'appareil photo, ou saisissez le code dans Réglages → Synchronisation (sur le site : Historique → Synchronisation). Gardez-le pour vous.",
            syncNow: "Synchroniser", lastSync: "Dernière synchronisation", never: "pas encore",
            stop: "Arrêter la synchronisation sur cet appareil", stopConfirm: "Votre historique reste sur cet appareil et sur vos autres appareils.",
            badCode: "Ce code n'est pas valide. Vérifiez-le et réessayez.", offline: "Connexion impossible. Vérifiez Internet et réessayez.", linked: "Cet appareil est relié")
        }
    }
}

/// Settings section: turn sync on (shows the code and its QR), or link with another device's code.
struct SyncSection: View {
    @Environment(AppModel.self) private var model
    @State private var code = AzkarSync.code
    @State private var last = AzkarSync.lastSync
    @State private var typing = false
    @State private var typed = ""
    @State private var busy = false
    @State private var error: String?
    @State private var confirmStop = false

    var body: some View {
        let t = SyncTexts.of(model.lang)
        Section {
            if let code {
                VStack(spacing: 10) {
                    Text(t.yourCode).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                    Text(AzkarSync.display(code))
                        .font(.system(.title2, design: .monospaced).weight(.semibold))
                        .textSelection(.enabled)
                        .environment(\.layoutDirection, .leftToRight)
                    if let qr = qrImage(AzkarSync.link(for: code).absoluteString) {
                        qr.resizable().interpolation(.none).scaledToFit()
                            .frame(width: 150, height: 150)
                            .padding(10)
                            .background(.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    Text(t.scanHint).font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
                HStack {
                    Button { Task { busy = true; await AzkarSync.syncNow(); busy = false; refresh() } } label: {
                        Label(t.syncNow, systemImage: "arrow.triangle.2.circlepath")
                    }
                    .disabled(busy)
                    Spacer()
                    Text(last.map { "\(t.lastSync): \($0.formatted(date: .omitted, time: .shortened))" } ?? "\(t.lastSync): \(t.never)")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Button(role: .destructive) { confirmStop = true } label: { Label(t.stop, systemImage: "xmark.circle") }
                    .confirmationDialog(t.stop, isPresented: $confirmStop) {
                        Button(t.stop, role: .destructive) { AzkarSync.stop(); refresh() }
                    } message: { Text(t.stopConfirm) }
            } else {
                Text(t.intro).font(.callout).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                Button {
                    Task {
                        busy = true; error = nil
                        do { try await AzkarSync.start() } catch { self.error = t.offline }
                        busy = false; refresh()
                    }
                } label: { Label(t.turnOn, systemImage: "arrow.triangle.2.circlepath.icloud") }
                .disabled(busy)
                if typing {
                    HStack {
                        TextField(t.enterCode, text: $typed)
                            .font(.system(.body, design: .monospaced))
                            .autocorrectionDisabled()
                            #if os(iOS)
                            .textInputAutocapitalization(.characters)
                            #endif
                            .environment(\.layoutDirection, .leftToRight)
                            .onSubmit(link)
                        Button(busy ? t.linking : t.link, action: link)
                            .disabled(busy || AzkarSync.normalize(typed).count != 12)
                    }
                } else {
                    Button { typing = true } label: { Label(t.haveCode, systemImage: "link") }
                }
            }
            if let error { Text(error).font(.caption).foregroundStyle(.red) }
        } header: {
            Text(t.title)
        }
        .onReceive(NotificationCenter.default.publisher(for: AzkarSync.changed)) { _ in refresh() }
    }

    private func link() {
        let t = SyncTexts.of(model.lang)
        Task {
            busy = true; error = nil
            do { try await AzkarSync.link(typed); typed = ""; typing = false; model.toast = t.linked }
            catch AzkarSync.Failure.unknownCode { error = t.badCode }
            catch { self.error = t.offline }
            busy = false; refresh()
        }
    }

    private func refresh() { code = AzkarSync.code; last = AzkarSync.lastSync }
}

/// A QR code for `text`, drawn with Core Image.
func qrImage(_ text: String) -> Image? {
    let filter = CIFilter.qrCodeGenerator()
    filter.message = Data(text.utf8)
    filter.correctionLevel = "M"
    guard let output = filter.outputImage?.transformed(by: CGAffineTransform(scaleX: 8, y: 8)),
          let cg = CIContext().createCGImage(output, from: output.extent) else { return nil }
    return Image(decorative: cg, scale: 1)
}
