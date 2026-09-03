import FunputShared
import SwiftUI

extension AppearanceModel {
    var keyboardAppearanceBinding: Binding<KeyboardAppearanceOption> {
        Binding(
            get: { self.configuration.keyboardAppearance },
            set: { self.commit(keyboardAppearance: $0) }
        )
    }

    /// A failed save leaves `configuration` untouched, so the picker reads back the old
    /// value on the next pass and the shared save-error alert explains why.
    func commit(keyboardAppearance: KeyboardAppearanceOption) {
        var candidate = configuration
        candidate.keyboardAppearance = keyboardAppearance
        guard persistConfiguration(candidate) else { return }
        acceptPersistedConfiguration(candidate, updatesPreview: false)
    }
}
