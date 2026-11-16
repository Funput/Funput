import FunputShared
import SwiftUI

/// Bindings that route every toggle through ``SettingsModel/update(_:to:)``, so a
/// change from any screen is saved and reaches the keyboard the same way.
extension SettingsModel {
    func boolBinding(_ keyPath: WritableKeyPath<FunputConfiguration, Bool>) -> Binding<Bool> {
        Binding(
            get: { self.configuration[keyPath: keyPath] },
            set: { self.update(keyPath, to: $0) }
        )
    }

    var numberRowBinding: Binding<Bool> {
        Binding(
            get: { self.isNumberRowLocked || self.configuration.showsNumberRow },
            set: { enabled in
                guard !self.isNumberRowLocked else { return }
                self.update(\.showsNumberRow, to: enabled)
            }
        )
    }

    func fullAccessBinding(
        _ keyPath: WritableKeyPath<FunputConfiguration, Bool>,
        requestAccess: @escaping () -> Void
    ) -> Binding<Bool> {
        Binding(
            get: { self.configuration[keyPath: keyPath] },
            set: { enabled in
                if enabled && !self.hasFullAccess { requestAccess() }
                else { self.update(keyPath, to: enabled) }
            }
        )
    }

    var heightBinding: Binding<Double> {
        Binding(
            get: { self.configuration.heightScale },
            set: { self.update(\.heightScale, to: min(max($0, 0.85), 1.2)) }
        )
    }

    var saveErrorBinding: Binding<Bool> {
        Binding(get: { self.showsSaveError }, set: { self.showsSaveError = $0 })
    }
}
