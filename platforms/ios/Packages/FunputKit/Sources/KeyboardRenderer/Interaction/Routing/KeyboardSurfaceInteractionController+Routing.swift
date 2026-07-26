#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceInteractionController {
    func handle(
        _ event: KeyboardKeyEvent,
        sourceFrame: CGRect?,
        presentation: KeyboardPresentation
    ) {
        switch event.phase {
        case .pressed:
            let token = nextLegacyToken
            nextLegacyToken &+= 1
            legacyTokensByKeyID[event.key.id, default: []].append(token)
            beginTouch(
                token: token,
                key: event.key,
                point: sourceFrame.map { CGPoint(x: $0.midX, y: $0.midY) } ?? .zero,
                sourceFrame: sourceFrame,
                presentation: presentation
            )
        case .released:
            if let token = takeLegacyToken(for: event.key.id) { endTouch(token: token) }
        case .cancelled:
            if let token = takeLegacyToken(for: event.key.id) {
                cancelTouch(token: token, reason: .userIntent)
            }
        case let .swiped(action):
            handleSwipe(event, action: action, presentation: presentation)
        case .repeated:
            break
        case .alternateSelected:
            if commitQueue.isEmpty {
                if presentation.isHapticFeedbackEnabled { haptics.perform(.control) }
                onEvent(event)
            }
        }
    }

    private func handleSwipe(
        _ event: KeyboardKeyEvent,
        action: KeySwipeAction,
        presentation: KeyboardPresentation
    ) {
        if let token = legacyTokensByKeyID[event.key.id]?.first,
           let state = touches[token] {
            finishSwipe(token: token, state: state, action: action)
            _ = takeLegacyToken(for: event.key.id)
        } else if commitQueue.isEmpty {
            if presentation.isHapticFeedbackEnabled { haptics.perform(.space) }
            onEvent(event)
        }
    }
}
#endif
