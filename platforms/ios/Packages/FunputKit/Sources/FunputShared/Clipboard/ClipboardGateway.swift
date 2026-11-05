import Foundation
#if canImport(UIKit)
import UIKit
#endif

/// The keyboard's only view of the pasteboard: metadata, never contents.
///
/// There is deliberately no way to read the text through here. Any content read
/// outside a `UIPasteControl` raises the iOS paste prompt, so contents reach Funput
/// only through the paste control the user taps.
/// `Scripts/Quality/check-pasteboard-reads.sh` keeps it that way.
@MainActor
public protocol ClipboardGateway {
    func snapshot() -> ClipboardSnapshot
}

#if canImport(UIKit)
@MainActor
public struct SystemClipboardGateway: ClipboardGateway {
    public init() {}
    public func snapshot() -> ClipboardSnapshot { ClipboardSnapshot(.general) }
}
#endif
