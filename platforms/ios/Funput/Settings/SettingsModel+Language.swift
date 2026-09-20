import FunputShared
import KeyboardLayout
import SwiftUI

/// The space bar's language switch, and the language picker that only means something
/// while it is there.
extension SettingsModel {
    /// Without the switch there is one language, so the picker has nothing to offer.
    var isLanguageLocked: Bool { !configuration.languageToggleEnabled }

    func setLanguageToggle(_ enabled: Bool) {
        update { configuration in
            configuration.languageToggleEnabled = enabled
            // Leaving English behind a switch the user just removed would strand them
            // there with no way back to Vietnamese from the keyboard.
            if !enabled { configuration.language = .vietnamese }
        }
    }

    var languageToggleBinding: Binding<Bool> {
        Binding(
            get: { self.configuration.languageToggleEnabled },
            set: { self.setLanguageToggle($0) }
        )
    }
}
