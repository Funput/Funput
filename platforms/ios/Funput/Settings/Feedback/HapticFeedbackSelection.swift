import Combine
import FunputShared
import KeyboardRenderer
import SwiftUI
import UIKit

/// Keeps one set of generators attached to a mounted view for each settings screen.
@MainActor
final class HapticFeedbackSelection: ObservableObject {
    private var feedback: KeyboardHaptics?

    func attach(to view: UIView) {
        feedback = KeyboardHaptics(view: view)
        feedback?.prepare()
    }

    /// Shares permission, persistence and sample playback between the page and quick sheet.
    func select(_ strength: HapticFeedbackStrength?, model: SettingsModel) -> FullAccessFeature? {
        if let strength, !model.hasFullAccess { return .hapticStrength(strength) }
        if model.selectHapticStrength(strength), let strength {
            feedback?.perform(strength: strength)
        }
        return nil
    }
}

struct HapticFeedbackSelectionHost: UIViewRepresentable {
    let selection: HapticFeedbackSelection

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        view.isUserInteractionEnabled = false
        view.isAccessibilityElement = false
        selection.attach(to: view)
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}
