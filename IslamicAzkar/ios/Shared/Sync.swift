import Foundation

/// Syncs the azkar record (AzkarHistory) between a person's devices — iPhone, iPad, Mac, Android and
/// the website — through islamicazkar.net. No account: the devices share a private 12-character sync
/// code; the server keeps only the record (days → completed readings) under a hash of it.
/// Once a device has a code, syncing is automatic: when a reading is completed, when the app comes
/// to the front, when the Record page opens, and every few minutes while the Mac app runs.
enum AzkarSync {
    static let changed = Notification.Name("AzkarSyncChanged")

    private static let base = URL(string: "https://islamicazkar.net/")!
    private static let codeKey = "sync_code"
    private static let lastKey = "sync_last"
    private static var d: UserDefaults { SharedStore.defaults }

    /// The sync code (12 characters, no separators), or nil when sync is off on this device.
    static var code: String? { d.string(forKey: codeKey) }
    static var lastSync: Date? { d.object(forKey: lastKey) as? Date }

    /// "ABCD-EFGH-JKMN" for display.
    static func display(_ code: String) -> String {
        stride(from: 0, to: code.count, by: 4).map { i -> String in
            let a = code.index(code.startIndex, offsetBy: i)
            return String(code[a..<code.index(a, offsetBy: min(4, code.count - i))])
        }.joined(separator: "-")
    }

    /// The link a QR code carries: scanning it with the camera opens the app and links it.
    static func link(for code: String) -> URL { URL(string: "islamicazkar://sync/\(code)")! }

    static func normalize(_ s: String) -> String {
        String(s.uppercased().unicodeScalars.filter { CharacterSet.alphanumerics.contains($0) && $0.isASCII }.map(Character.init))
    }

    enum Failure: Error { case unknownCode, network }

    /// Turns sync on with a new code, uploading this device's record.
    @discardableResult
    static func start() async throws -> String {
        let result = try await post("sync/new", ["days": AzkarHistory.readingsByDay()])
        guard let code = result.code else { throw Failure.network }
        d.set(code, forKey: codeKey)
        apply(result.days)
        return code
    }

    /// Links this device to the code shown on another device, merging both records.
    static func link(_ typed: String) async throws {
        let c = normalize(typed)
        guard c.count == 12 else { throw Failure.unknownCode }
        let result = try await post("sync", ["code": c, "days": AzkarHistory.readingsByDay()])
        d.set(c, forKey: codeKey)
        apply(result.days)
    }

    /// Sends this device's record and takes in the other devices'. Quietly does nothing offline.
    static func syncNow() async {
        guard let c = code else { return }
        do {
            let result = try await post("sync", ["code": c, "days": AzkarHistory.readingsByDay()])
            apply(result.days)
        } catch Failure.unknownCode {
            stop()   // the code no longer exists on the server
        } catch {}
    }

    /// Turns sync off on this device (the record stays here and on the other devices).
    static func stop() {
        d.removeObject(forKey: codeKey)
        d.removeObject(forKey: lastKey)
        NotificationCenter.default.post(name: changed, object: nil)
    }

    private static func apply(_ days: [String: [String]]) {
        AzkarHistory.merge(days)
        d.set(Date(), forKey: lastKey)
        DispatchQueue.main.async { NotificationCenter.default.post(name: changed, object: nil) }
    }

    private struct Result: Decodable { let code: String?; let days: [String: [String]] }

    private static func post(_ path: String, _ body: [String: Any]) async throws -> Result {
        var req = URLRequest(url: base.appendingPathComponent(path), timeoutInterval: 15)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "content-type")
        req.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, response): (Data, URLResponse)
        do { (data, response) = try await URLSession.shared.data(for: req) } catch { throw Failure.network }
        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        if status == 404 { throw Failure.unknownCode }
        guard status == 200, let r = try? JSONDecoder().decode(Result.self, from: data) else { throw Failure.network }
        return r
    }
}
