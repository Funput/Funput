import FunputShared
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

    static func event(_ name: StaticString) {
        os_signpost(.event, log: log, name: name)
    }
}

/// The system gateway with each content read timed: the one pasteboard call that
/// can wait on another device and raise the paste prompt.
@MainActor
struct TracedClipboardGateway: ClipboardGateway {
    private let system = SystemClipboardGateway()

    func snapshot() -> ClipboardSnapshot { system.snapshot() }

    func readText() async -> String? {
        let log = ClipboardSignpost.log
        let identifier = OSSignpostID(log: log)
        os_signpost(.begin, log: log, name: "ClipboardRead", signpostID: identifier)
        defer { os_signpost(.end, log: log, name: "ClipboardRead", signpostID: identifier) }
        return await system.readText()
    }
}
