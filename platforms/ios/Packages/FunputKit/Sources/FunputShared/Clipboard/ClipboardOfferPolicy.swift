import Foundation
import KeyboardLayout

/// Decides whether the keyboard may invite the user to paste.
///
/// A pure function on purpose: the rule that matters most — never offer the
/// clipboard in a password field — is one a unit test can hold to account, and it
/// should not depend on having a device to hand.
public enum ClipboardOfferPolicy {
    public struct Context: Equatable, Sendable {
        public let editorMode: KeyboardEditorMode
        public let hasToolbar: Bool
        public let hasFullAccess: Bool
        public let isEnabled: Bool

        public init(
            editorMode: KeyboardEditorMode,
            hasToolbar: Bool,
            hasFullAccess: Bool,
            isEnabled: Bool = true
        ) {
            self.editorMode = editorMode
            self.hasToolbar = hasToolbar
            self.hasFullAccess = hasFullAccess
            self.isEnabled = isEnabled
        }
    }

    /// Whether the *surroundings* permit an offer at all, independent of what is on
    /// the pasteboard.
    ///
    /// Split out so a reading that may have been refused by the system cannot leave a
    /// stale chip sitting in a password field: the caller can enforce these rules
    /// even when it has no snapshot it trusts.
    public static func allowsOffer(context: Context) -> Bool {
        // The user can decline the whole feature.
        guard context.isEnabled else { return false }
        // Full Access is what makes the pasteboard readable at all.
        guard context.hasFullAccess else { return false }
        // Password and PIN fields never see a paste invitation, even though the
        // pasteboard may well hold exactly what the user wants there.
        guard !context.editorMode.isPassword else { return false }
        // Number and password layouts have no toolbar to host the chip.
        return context.hasToolbar
    }

    public static func offer(
        snapshot: ClipboardSnapshot,
        lastCapturedChangeCount: Int?,
        context: Context
    ) -> ClipboardOffer? {
        guard allowsOffer(context: context) else { return nil }
        // v1 is text only: an image-only pasteboard stays silent.
        guard snapshot.hasStrings else { return nil }
        // Already captured — do not invite the user to paste the same thing twice.
        guard snapshot.changeCount != lastCapturedChangeCount else { return nil }

        return ClipboardOffer(
            kind: snapshot.hasURLs ? .link : .text,
            changeCount: snapshot.changeCount
        )
    }
}
