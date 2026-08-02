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

    /// Whether this reading may be a refusal rather than an answer.
    ///
    /// iOS sometimes declines a pasteboard read — `PBErrorDomain` code 10,
    /// *"Pasteboard com.apple.UIKit.pboard.general is not available at this time"* —
    /// while the host app is still settling. Chrome trips it often, because its
    /// omnibox reads the pasteboard too. Nothing throws: the query simply answers as
    /// though the pasteboard were empty and untouched since boot.
    ///
    /// That shape is what this flags. It cannot be told apart from a genuinely empty
    /// pasteboard, and it does not need to be: both mean "nothing to offer yet, ask
    /// again shortly".
    public var isIndeterminate: Bool {
        changeCount == 0 && !hasStrings && !hasURLs
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
