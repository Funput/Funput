import Foundation
import UniformTypeIdentifiers
#if canImport(UIKit)
import UIKit
#endif

/// The representations Funput can faithfully insert into a text document.
///
/// `NSString` also advertises `public.url`, which makes file URLs look pasteable.
/// Keeping this list explicit prevents non-text providers from reaching the UI.
public enum ClipboardPlainText {
    public static let typeIdentifiers = [
        UTType.utf8PlainText.identifier,
        UTType.utf16PlainText.identifier,
        UTType.utf16ExternalPlainText.identifier,
        UTType.plainText.identifier,
    ]

    public static func canLoad(from provider: NSItemProvider) -> Bool {
        typeIdentifiers.contains(where: provider.hasItemConformingToTypeIdentifier)
    }
}

#if canImport(UIKit)
public extension ClipboardPlainText {
    @MainActor
    static func isAvailable(in pasteboard: UIPasteboard) -> Bool {
        pasteboard.contains(pasteboardTypes: typeIdentifiers)
    }
}
#endif
