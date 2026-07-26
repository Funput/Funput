#if canImport(UIKit)
import KeyboardLayout
import UIKit

@MainActor
enum KeyboardKeyAccessibilityActions {
    static func make(
        spec: KeySpec,
        presentation: KeyboardPresentation,
        emit: @escaping (KeyboardKeyEvent.Phase) -> Void
    ) -> [UIAccessibilityCustomAction]? {
        var actions = spec.alternates.map { alternate in
            let name = presentation.shiftState.isUppercase
                ? alternate.shiftedText : alternate.accessibilityLabel
            return UIAccessibilityCustomAction(
                name: "Chọn \(name)"
            ) { _ in
                emit(.alternateSelected(alternate))
                return true
            }
        }
        if let swipe = spec.horizontalSwipeAction {
            let target = presentation.language == .vietnamese ? "Tiếng Anh" : "Tiếng Việt"
            actions.append(
                UIAccessibilityCustomAction(name: "Chuyển sang \(target)") { _ in
                    emit(.swiped(swipe))
                    return true
                }
            )
        }
        return actions.isEmpty ? nil : actions
    }
}
#endif
