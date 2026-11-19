import XCTest

/// Drives the Siri Remote in the Simulator and logs where the focus lands after every press
/// ("FOCUS ..." lines in the CI log), so a screen where the remote gets stuck shows up.
final class RemoteNavigationTests: XCTestCase {
    override func setUp() { continueAfterFailure = true }

    private func launch(_ args: [String]) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-ciLang", "en"] + args
        app.launch()
        sleep(3)
        return app
    }

    private func focused(_ app: XCUIApplication) -> String {
        let f = app.descendants(matching: .any).matching(NSPredicate(format: "hasFocus == true")).firstMatch
        guard f.exists else { return "nothing" }
        let label = f.label.isEmpty ? f.identifier : f.label
        return "\(label.prefix(60)) [\(f.elementType.rawValue)]"
    }

    private func name(_ b: XCUIRemote.Button) -> String {
        switch b {
        case .up: "up"; case .down: "down"; case .left: "left"; case .right: "right"
        case .select: "select"; case .menu: "menu"; case .playPause: "play"
        default: "\(b.rawValue)"
        }
    }

    /// Presses each button and returns the focused element after each press.
    @discardableResult
    private func walk(_ app: XCUIApplication, _ screen: String, _ presses: [XCUIRemote.Button]) -> [String] {
        var seen: [String] = []
        print("FOCUS \(screen) start: \(focused(app))")
        for b in presses {
            XCUIRemote.shared.press(b)
            usleep(900_000)
            let f = focused(app)
            seen.append(f)
            print("FOCUS \(screen) \(name(b)) -> \(f)")
        }
        let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        shot.name = screen
        shot.lifetime = .keepAlways
        add(shot)
        return seen
    }

    func testHome() {
        let app = launch([])
        let downs = walk(app, "home", [.down, .down, .down, .down, .down, .down])
        // Going down must reach the categories under the four tiles.
        XCTAssertGreaterThan(Set(downs).count, 3, "the remote got stuck going down the Home page")
        walk(app, "home-up", [.up, .up, .up, .up, .up, .up, .up])
        walk(app, "home-tiles", [.down, .down, .right, .right, .right, .left])
    }

    func testTasbih() {
        let app = launch(["-ciScreen", "tasbih"])
        walk(app, "tasbih", [.select, .select, .right, .down, .down, .right, .down, .left, .left, .up, .up, .up])
    }

    func testReader() {
        let app = launch(["-ciScreen", "reader"])
        walk(app, "reader", [.select, .right, .left, .up, .menu])
    }

    func testRecordAndSettings() {
        let app = launch(["-ciScreen", "record"])
        walk(app, "record", [.down, .right, .left, .up])
        let app2 = launch(["-ciScreen", "settings"])
        walk(app2, "settings", [.down, .down, .down, .down, .down, .up, .up, .up])
    }
}
