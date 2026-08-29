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

/// In-memory transaction writer standing in for the host text field.
///
/// `reportedContext` lets a test replay the host's notorious callback
/// behavior: `textDidChange` arriving late, with the context the document
/// had one or two mutations ago.
@MainActor
final class ScriptedWriter: KeyboardDocumentWriting {
    private(set) var text = ""
    /// Character index of the caret; the trackpad gesture is the only thing that moves it
    /// away from the end of the text.
    private(set) var caret = 0
    /// `text.count` walks the whole string, and the hundred-thousand-key stress tests ask
    /// for it on every keystroke. Maintained alongside every mutation instead.
    private var textCount = 0
    let documentIdentifier = UUID()
    /// When set, `contextBeforeInput` reports this instead of `text`,
    /// simulating a stale host callback. Cleared by the test after delivery.
    var reportedContext: String?

    var snapshot: KeyboardDocumentSnapshot {
        .init(
            documentIdentifier: documentIdentifier,
            contextBeforeInput: reportedContext
                ?? (caret == textCount ? text : String(text.prefix(caret))),
            hasSelection: false
        )
    }

    func replaceTextExternally(with text: String) {
        self.text = text
        textCount = text.count
        caret = textCount
    }

    func apply(_ transaction: InputTransaction) {
        for mutation in transaction.mutations {
            switch mutation {
            case let .deleteBackward(count):
                for _ in 0..<count where caret > 0 {
                    if caret == textCount {
                        text.removeLast()
                    } else {
                        text.remove(at: text.index(text.startIndex, offsetBy: caret - 1))
                    }
                    caret -= 1
                    textCount -= 1
                }
            case let .insert(inserted):
                if caret == textCount {
                    text += inserted
                } else {
                    text.insert(
                        contentsOf: inserted,
                        at: text.index(text.startIndex, offsetBy: caret)
                    )
                }
                let insertedCount = inserted.count
                caret += insertedCount
                textCount += insertedCount
            case let .moveCursor(offset):
                caret = min(max(0, caret + offset), textCount)
            }
        }
    }
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
