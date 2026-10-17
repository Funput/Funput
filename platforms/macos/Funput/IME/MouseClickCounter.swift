import AppKit

/// Counts mouse clicks in other apps. A word written straight into the document has no
/// marked text, so IMKit never learns that a click moved the caret; a change in this count
/// is that signal (see `CompositionSession.endIfClicked`).
///
/// The monitor is installed on the first read, so it only exists once a word has been
/// written directly. Watching mouse events needs no Accessibility permission; only key
/// events do.
final class MouseClickCounter {
    static let shared = MouseClickCounter()

    private var count = 0
    private var monitor: Any?

    var clicks: Int {
        if monitor == nil {
            monitor = NSEvent.addGlobalMonitorForEvents(
                matching: [.leftMouseDown, .rightMouseDown, .otherMouseDown]
            ) { [weak self] _ in
                self?.count &+= 1
            }
        }
        return count
    }
}
