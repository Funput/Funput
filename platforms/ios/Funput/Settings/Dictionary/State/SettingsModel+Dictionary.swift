import Foundation
import FunputShared

extension SettingsModel {
    /// Whether "Xoá toàn bộ từ đã học" is still waiting for the keyboard.
    ///
    /// The app cannot wipe the store itself; it leaves a token the keyboard applies
    /// the next time it opens, and records as applied in the App Group. Until then
    /// the page shows the learned words as already gone, which is what they are
    /// about to be.
    var personalSuggestionResetIsPending: Bool {
        guard let token = configuration.personalSuggestionResetToken else { return false }
        let applied = UserDefaults(suiteName: FunputAppGroup.identifier)?
            .string(forKey: FunputAppGroup.personalSuggestionAppliedResetKey)
        return applied != token.uuidString
    }
}
