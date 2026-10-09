import Foundation
#if canImport(UIKit)
import UIKit
#endif

@MainActor
public protocol ClipboardGateway {
    func snapshot() -> ClipboardSnapshot
    /// Reads the clipboard's text. Asynchronous because the contents may live on
    /// another device (Universal Clipboard) and arrive only after a network fetch.
    func readText() async -> String?
}

#if canImport(UIKit)
@MainActor
public struct SystemClipboardGateway: ClipboardGateway {
    public init() {}
    public func snapshot() -> ClipboardSnapshot { ClipboardSnapshot(.general) }

    /// Off the main thread: a Universal Clipboard item copied on a Mac is fetched
    /// on demand, and that wait must never freeze the keys. `UIPasteboard` carries
    /// no main-actor requirement, and only the `Sendable` string crosses back.
    public func readText() async -> String? {
        await Task.detached(priority: .utility) {
            UIPasteboard.general.string
        }.value
    }
}
#endif
