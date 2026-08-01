#if os(iOS) && canImport(FunputCore)
import os

enum KeyboardInputSignposts {
    static let log = OSLog(subsystem: "app.funput.keyboard", category: "Input")

    @inline(__always)
    static func begin(_ name: StaticString) -> OSSignpostID {
        let identifier = OSSignpostID(log: log)
        os_signpost(.begin, log: log, name: name, signpostID: identifier)
        return identifier
    }

    @inline(__always)
    static func end(_ name: StaticString, _ identifier: OSSignpostID) {
        os_signpost(.end, log: log, name: name, signpostID: identifier)
    }
}
#endif
