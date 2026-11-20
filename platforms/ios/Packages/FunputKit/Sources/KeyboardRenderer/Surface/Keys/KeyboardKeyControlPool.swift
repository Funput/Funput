#if canImport(UIKit)
import KeyboardLayout
import UIKit

/// Every key control a surface has built, handed out again on each layout change.
///
/// Switching to the symbol page and back used to destroy a keyboard's worth of
/// controls and build another, each with its own keycap view. The pool gives the
/// controls it has to the new layout instead, builds only what is missing, and
/// parks what is left over. It never shrinks: its ceiling is the largest layout the
/// surface has shown.
@MainActor
struct KeyboardKeyControlPool {
    private(set) var controls: [KeyboardKeyControl] = []

    /// Gives each spec a control. Returns them by key id, along with the ones that
    /// had to be built and still need a place in the view hierarchy.
    mutating func assign(
        _ specs: [KeySpec],
        make: (KeySpec) -> KeyboardKeyControl
    ) -> (byID: [String: KeyboardKeyControl], created: [KeyboardKeyControl]) {
        // A control's keycap only has to be rebuilt when it moves between a
        // character key and a special one, so like goes to like first.
        var spare = Dictionary(grouping: controls) { $0.role.isSpecial }
        var byID: [String: KeyboardKeyControl] = [:]
        var unmatched: [KeySpec] = []
        for spec in specs {
            guard let control = spare[spec.role.isSpecial]?.popLast() else {
                unmatched.append(spec)
                continue
            }
            control.adopt(spec)
            byID[spec.id] = control
        }

        var leftover = spare.values.flatMap { $0 }
        var created: [KeyboardKeyControl] = []
        for spec in unmatched {
            if let control = leftover.popLast() {
                control.adopt(spec)
                byID[spec.id] = control
            } else {
                let control = make(spec)
                created.append(control)
                byID[spec.id] = control
            }
        }
        leftover.forEach { $0.park() }
        controls.append(contentsOf: created)
        return (byID, created)
    }
}
#endif
