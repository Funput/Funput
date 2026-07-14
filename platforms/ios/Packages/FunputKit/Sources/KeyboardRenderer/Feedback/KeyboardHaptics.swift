#if canImport(UIKit)
import UIKit

@MainActor
final class KeyboardHaptics {
    private let light: UIImpactFeedbackGenerator
    private let soft: UIImpactFeedbackGenerator
    private let rigid: UIImpactFeedbackGenerator
    private let medium: UIImpactFeedbackGenerator
    private let repeatSelection: UISelectionFeedbackGenerator

    init(view: UIView) {
        light = UIImpactFeedbackGenerator(style: .light, view: view)
        soft = UIImpactFeedbackGenerator(style: .soft, view: view)
        rigid = UIImpactFeedbackGenerator(style: .rigid, view: view)
        medium = UIImpactFeedbackGenerator(style: .medium, view: view)
        repeatSelection = UISelectionFeedbackGenerator(view: view)
    }

    func prepare() {
        light.prepare()
        soft.prepare()
        rigid.prepare()
        medium.prepare()
        repeatSelection.prepare()
    }

    func perform(_ type: KeyboardHapticType) {
        switch type {
        case .keyPress:
            impact(light, intensity: 0.55)
        case .space:
            impact(soft, intensity: 0.6)
        case .control:
            impact(rigid, intensity: 0.55)
        case .delete:
            impact(medium, intensity: 0.65)
        case .deleteRepeat:
            repeatSelection.selectionChanged()
            repeatSelection.prepare()
        case .submit:
            impact(medium, intensity: 0.8)
        }
    }

    private func impact(_ generator: UIImpactFeedbackGenerator, intensity: CGFloat) {
        generator.impactOccurred(intensity: intensity)
        generator.prepare()
    }
}
#endif
