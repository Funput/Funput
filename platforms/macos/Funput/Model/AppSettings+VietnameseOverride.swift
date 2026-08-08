import Foundation
import SwiftUI

/// Thin delegation to `appLanguageMemory` (`AppLanguageMemory`) — kept as its own
/// extension so `FunputInputController`, the menu bar, and Settings keep calling
/// these same names regardless of how the per-app memory is implemented
/// underneath (previously a session-only override map + exclusion list, now the
/// persisted `funput-ffi` handle).
extension AppSettings {
    /// Decide the VI/EN target when focus lands on the app `front`. Returns `nil`
    /// when nothing is remembered for it — leave the current state as-is.
    func resolveVietnamese(for front: String?) -> Bool? {
        appLanguageMemory.resolve(for: front)
    }

    /// Record a VI/EN choice made by the toggle hotkey. The hotkey fires while the
    /// target app is focused, so the choice pins to that app immediately; any stale
    /// pending UI choice is dropped.
    func pinVietnamese(_ on: Bool, to bundleId: String?) {
        vietnameseEnabled = on
        appLanguageMemory.pin(on, to: bundleId)
    }

    /// Record a VI/EN choice made from Funput's own UI (menu bar / Settings). Our
    /// window holds focus at that moment, so the choice binds to the next app the
    /// user returns to — otherwise the per-app memory would leave it unresolved.
    func setVietnameseFromUI(_ on: Bool) {
        vietnameseEnabled = on
        appLanguageMemory.setPending(on)
    }

    /// Binding for the VI/EN switches in Funput's own UI. Writes route through
    /// [`setVietnameseFromUI`] rather than assigning `vietnameseEnabled` directly, so
    /// every switch shares that binding semantic instead of restating it per call site.
    var vietnameseUIBinding: Binding<Bool> {
        Binding(get: { self.vietnameseEnabled }, set: { self.setVietnameseFromUI($0) })
    }
}
