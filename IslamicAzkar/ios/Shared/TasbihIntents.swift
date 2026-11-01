import AppIntents
import WidgetKit

/// Counts one tasbih from the Home Screen widget without opening the app.
struct IncrementTasbihIntent: AppIntent {
    static var title: LocalizedStringResource = "Count tasbih"
    static var isDiscoverable: Bool = false

    func perform() async throws -> some IntentResult {
        SharedStore.incrementTasbih()
        TasbihHistory.add()
        WidgetCenter.shared.reloadTimelines(ofKind: "TasbihWidget")
        WidgetCenter.shared.reloadTimelines(ofKind: "CounterWidget")
        return .result()
    }
}

struct ResetTasbihIntent: AppIntent {
    static var title: LocalizedStringResource = "Reset tasbih"
    static var isDiscoverable: Bool = false

    func perform() async throws -> some IntentResult {
        SharedStore.resetTasbih()
        WidgetCenter.shared.reloadTimelines(ofKind: "TasbihWidget")
        WidgetCenter.shared.reloadTimelines(ofKind: "CounterWidget")
        return .result()
    }
}
