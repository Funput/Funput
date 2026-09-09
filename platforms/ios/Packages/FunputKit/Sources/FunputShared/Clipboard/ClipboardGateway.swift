import Foundation
#if canImport(UIKit)
import UIKit
#endif

@MainActor
public protocol ClipboardGateway {
    func snapshot() -> ClipboardSnapshot
    func readText() -> String?
}

#if canImport(UIKit)
@MainActor
public struct SystemClipboardGateway: ClipboardGateway {
    public init() {}
    public func snapshot() -> ClipboardSnapshot { ClipboardSnapshot(.general) }
    public func readText() -> String? { UIPasteboard.general.string }
}
#endif
