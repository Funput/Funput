#if canImport(UIKit)
import FunputShared
import UIKit

/// Lets a test observe feedback without a Taptic Engine.
@MainActor
protocol KeyboardHapticPerforming {
    func prepare()
    func perform(strength: HapticFeedbackStrength)
    /// A light tick for steps that arrive many times a second: caret panning and
    /// Backspace repeat. A full impact per step at a strong setting reads as a buzz.
    func performTick()
}

/// The same typing feedback in the keyboard and the containing app's settings.
@MainActor
public final class KeyboardHaptics: KeyboardHapticPerforming {
    private let light: UIImpactFeedbackGenerator
    private let strong: UIImpactFeedbackGenerator
    private let tick: UISelectionFeedbackGenerator

    public init(view: UIView) {
        if #available(iOS 17.5, *) {
            light = UIImpactFeedbackGenerator(style: .light, view: view)
            strong = UIImpactFeedbackGenerator(style: .heavy, view: view)
            tick = UISelectionFeedbackGenerator(view: view)
        } else {
            light = UIImpactFeedbackGenerator(style: .light)
            strong = UIImpactFeedbackGenerator(style: .heavy)
            tick = UISelectionFeedbackGenerator()
        }
    }

    public func prepare() {
        light.prepare()
        strong.prepare()
        tick.prepare()
    }

    public func performTick() {
        tick.selectionChanged()
        tick.prepare()
    }

    public func perform(strength: HapticFeedbackStrength) {
        let generator: UIImpactFeedbackGenerator
        let intensity: CGFloat
        switch strength {
        case .light:
            generator = light
            intensity = 0.55
        case .medium:
            // The stock keyboard's tap is short and crisp. A `.medium` impact
            // has a heavier body, so the default level uses a full-strength
            // light impact instead, which sits closest to the system keyboard.
            generator = light
            intensity = 1
        case .strong:
            generator = strong
            intensity = 1
        }
        generator.impactOccurred(intensity: intensity)
        generator.prepare()
    }
}
#endif
