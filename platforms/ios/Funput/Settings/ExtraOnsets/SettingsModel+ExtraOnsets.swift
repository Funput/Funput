import FunputShared
import SwiftUI

extension SettingsModel {
    /// The master switch reflects the selected letters rather than separate state.
    var extraOnsetsEnabledBinding: Binding<Bool> {
        Binding(
            get: { !self.configuration.extraOnsets.isEmpty },
            set: { self.update(\.extraOnsets, to: $0 ? .all : []) }
        )
    }

    func extraOnsetBinding(_ member: ExtraOnsetLetters) -> Binding<Bool> {
        Binding(
            get: { self.configuration.extraOnsets.contains(member) },
            set: { enabled in
                var letters = self.configuration.extraOnsets
                if enabled { letters.insert(member) }
                else { letters.remove(member) }
                self.update(\.extraOnsets, to: letters)
            }
        )
    }
}
