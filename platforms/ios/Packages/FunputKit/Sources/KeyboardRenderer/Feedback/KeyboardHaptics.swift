#if canImport(UIKit)
import UIKit

@MainActor
final class KeyboardHaptics {
    private let light = UIImpactFeedbackGenerator(style: .light)
    private let soft = UIImpactFeedbackGenerator(style: .soft)
    private let rigid = UIImpactFeedbackGenerator(style: .rigid)
    private let medium = UIImpactFeedbackGenerator(style: .medium)
    private let repeatSelection = UISelectionFeedbackGenerator()

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
            light.impactOccurred(intensity: 0.55)
        case .space:
            soft.impactOccurred(intensity: 0.6)
        case .control:
            rigid.impactOccurred(intensity: 0.55)
        case .delete:
            medium.impactOccurred(intensity: 0.65)
        case .deleteRepeat:
            repeatSelection.selectionChanged()
        case .submit:
            medium.impactOccurred(intensity: 0.8)
        }
    }
}
#endif
