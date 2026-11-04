import os

/// Clipboard work in the `Launch` category, so Instruments shows it beside
/// `ColdStart` and can prove it never runs inside it.
enum ClipboardSignpost {
    static let log = OSLog(subsystem: "app.funput.keyboard", category: "Launch")

    static func measure<Value>(_ name: StaticString, _ operation: () -> Value) -> Value {
        let identifier = OSSignpostID(log: log)
        os_signpost(.begin, log: log, name: name, signpostID: identifier)
        defer { os_signpost(.end, log: log, name: name, signpostID: identifier) }
        return operation()
    }
}
