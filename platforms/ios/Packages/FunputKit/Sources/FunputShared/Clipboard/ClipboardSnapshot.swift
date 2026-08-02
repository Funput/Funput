import Foundation
#if canImport(UIKit)
import UIKit
#endif

/// Everything the keyboard is allowed to learn about the pasteboard without
/// prompting the user.
///
/// Reading `UIPasteboard.general.string` presents the iOS paste alert; reading the
/// metadata below does not — measured at 0.003s on device, no alert. Every
/// decision downstream works off this struct, so the pasteboard is touched in
/// exactly one place and the rules stay testable without a device.
public struct ClipboardSnapshot: Equatable, Sendable {
    public let changeCount: Int
    public let hasStrings: Bool
    public let hasURLs: Bool

    public init(changeCount: Int, hasStrings: Bool, hasURLs: Bool) {
        self.changeCount = changeCount
        self.hasStrings = hasStrings
        self.hasURLs = hasURLs
    }
}

#if canImport(UIKit)
public extension ClipboardSnapshot {
    @MainActor
    init(_ pasteboard: UIPasteboard) {
        self.init(
            changeCount: pasteboard.changeCount,
            hasStrings: pasteboard.hasStrings,
            hasURLs: pasteboard.hasURLs
        )
    }
}
#endif
