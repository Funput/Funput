import KeyboardInput
import UIKit

/// Every accessibility-element UIControl under `view` — on a keyboard
/// surface, exactly the keycap controls (KeyboardKeyControl).
@MainActor
func accessibleControls(in view: UIView) -> [UIControl] {
    view.subviews.flatMap { child -> [UIControl] in
        var own: [UIControl] = []
        if let control = child as? UIControl, control.isAccessibilityElement {
            own.append(control)
        }
        return own + accessibleControls(in: child)
    }
}

/// In-memory stand-in for the host app's text field: what a
/// UITextDocumentProxy edits, minus the cross-process transport.
///
/// `reportedContext` lets a test replay the host's notorious callback
/// behavior: `textDidChange` arriving late, with the context the document
/// had one or two mutations ago.
@MainActor
final class ScriptedDocument: KeyboardDocument {
    private(set) var text = ""
    let documentIdentifier = UUID()
    /// When set, `contextBeforeInput` reports this instead of `text`,
    /// simulating a stale host callback. Cleared by the test after delivery.
    var reportedContext: String?

    var contextBeforeInput: String? { reportedContext ?? text }
    var hasSelection: Bool { false }

    func insertText(_ string: String) { text += string }
    func deleteBackward() { if !text.isEmpty { text.removeLast() } }
}

/// Deterministic RNG (SplitMix64) so failures replay exactly.
struct TestRand {
    private var state: UInt64
    init(seed: UInt64) { state = seed }

    mutating func next(upTo bound: UInt32) -> UInt32 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return UInt32((z ^ (z >> 31)) % UInt64(bound))
    }
}
