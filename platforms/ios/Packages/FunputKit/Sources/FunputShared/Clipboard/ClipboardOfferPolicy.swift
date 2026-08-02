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

        public init(editorMode: KeyboardEditorMode, hasToolbar: Bool, hasFullAccess: Bool) {
            self.editorMode = editorMode
            self.hasToolbar = hasToolbar
            self.hasFullAccess = hasFullAccess
        }
    }

    public static func offer(
        snapshot: ClipboardSnapshot,
        lastCapturedChangeCount: Int?,
        context: Context
    ) -> ClipboardOffer? {
        // Full Access is what makes the pasteboard readable at all.
        guard context.hasFullAccess else { return nil }
        // Password and PIN fields never see a paste invitation, even though the
        // pasteboard may well hold exactly what the user wants there.
        guard !context.editorMode.isPassword else { return nil }
        // Number and password layouts have no toolbar to host the chip.
        guard context.hasToolbar else { return nil }
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
