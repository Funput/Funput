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
        // VoiceOver consumes drags for its own navigation, so the three smart gestures are
        // unreachable by touch there. Custom actions are the only way to keep the
        // functionality available rather than merely the polish.
        if presentation.areSmartGesturesEnabled {
            actions.append(contentsOf: smartGestureActions(role: spec.role, emit: emit))
        }
        return actions.isEmpty ? nil : actions
    }

    private static func smartGestureActions(
        role: KeyRole,
        emit: @escaping (KeyboardKeyEvent.Phase) -> Void
    ) -> [UIAccessibilityCustomAction] {
        switch role {
        case .backspace:
            [UIAccessibilityCustomAction(name: "Xóa cả từ") { _ in
                emit(.deletedWord)
                return true
            }]
        case .space:
            [
                UIAccessibilityCustomAction(name: "Con trỏ sang trái") { _ in
                    emit(.cursorMoved(offset: -1))
                    return true
                },
                UIAccessibilityCustomAction(name: "Con trỏ sang phải") { _ in
                    emit(.cursorMoved(offset: 1))
                    return true
                },
            ]
        default:
            []
        }
    }
}
#endif
