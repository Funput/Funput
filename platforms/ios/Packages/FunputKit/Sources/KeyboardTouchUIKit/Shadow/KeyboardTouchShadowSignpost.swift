import os

enum KeyboardTouchShadowSignpost {
    private static let log = OSLog(
        subsystem: "app.funput.keyboard",
        category: "TouchShadow"
    )

    static func emit(_ event: KeyboardTouchShadowEvent, total: Int) {
        os_signpost(
            .event,
            log: log,
            name: "ShadowComparison",
            "code=%{public}d total=%{public}d",
            event.rawValue,
            total
        )
    }
}
