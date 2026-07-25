#if canImport(UIKit)
import UIKit

extension KeyboardSurfaceInteractionController {
    func endTouch(token: TouchToken) {
        guard let state = touches.removeValue(forKey: token) else { return }
        if let key = state.currentKey { setHighlighted(key, false) }
        let wasRepeating = repeatTouch == token && repeatController.finish()
        if repeatTouch == token { repeatTouch = nil }
        commitQueue.complete(
            token: token,
            as: state.currentKey == nil ? .cancelled : (wasRepeating ? .suppressed : .released)
        )
        endSignpost(state, token: token, phase: state.currentKey == nil ? 2 : 1)
        refreshPreview()
        flushCompletedKeys()
    }

    func cancelTouch(token: TouchToken) {
        guard let state = touches.removeValue(forKey: token) else { return }
        if let key = state.currentKey { setHighlighted(key, false) }
        if repeatTouch == token { clearKeyRepeat() }
        commitQueue.complete(token: token, as: .cancelled)
        endSignpost(state, token: token, phase: 2)
        refreshPreview()
        flushCompletedKeys()
    }

    func reconcileActiveTouches(_ activeTokens: Set<TouchToken>) {
        // Toolbar and accessibility presses never reach the overlay, so they are
        // absent from `activeTokens` by construction and must not be read as
        // abandoned — an unrelated keycap touch would otherwise cancel them.
        let orphaned = Set(touches.keys)
            .subtracting(activeTokens)
            .filter { $0 < Self.firstLegacyToken }
        for token in orphaned { cancelTouch(token: token) }
    }

    func cancelAll() {
        for (token, state) in touches {
            if let key = state.currentKey { setHighlighted(key, false) }
            endSignpost(state, token: token, phase: 2)
        }
        touches.removeAll(keepingCapacity: true)
        commitQueue.cancelAll()
        clearKeyRepeat()
        refreshPreview()
        flushCompletedKeys()
        legacyTokensByKeyID.removeAll(keepingCapacity: true)
    }

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
            if let token = takeLegacyToken(for: event.key.id) { cancelTouch(token: token) }
        case let .swiped(action):
            if let token = legacyTokensByKeyID[event.key.id]?.first,
               let state = touches[token] {
                finishSwipe(token: token, state: state, action: action)
                _ = takeLegacyToken(for: event.key.id)
            } else if commitQueue.isEmpty {
                if presentation.isHapticFeedbackEnabled { haptics.perform(.space) }
                onEvent(event)
            }
        case .repeated:
            break
        }
    }
}
#endif
