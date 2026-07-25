#if canImport(UIKit)
import KeyboardLayout
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

    func cancelTouch(token: TouchToken, reason: Cancellation) {
        guard let state = touches.removeValue(forKey: token) else { return }
        if let key = state.currentKey { setHighlighted(key, false) }
        let wasRepeating = repeatTouch == token && repeatController.finish()
        if repeatTouch == token { repeatTouch = nil }
        commitQueue.complete(
            token: token,
            as: completion(cancelling: state, reason: reason, wasRepeating: wasRepeating)
        )
        endSignpost(state, token: token, phase: 2)
        refreshPreview()
        flushCompletedKeys()
    }

    /// A press the system took away is still a press the user made, so a key that
    /// was under the finger commits anyway. Three exclusions keep that from
    /// inventing input nobody asked for:
    ///
    /// - keys that are destructive or hard to undo. Losing one Backspace is
    ///   annoying; an extra one damages text, and an extra Return sends a message.
    /// - a finger that wandered, which is a gesture that merely started on a
    ///   keycap: a swipe up from the home indicator begins as a touch on the
    ///   bottom row and ends as a system cancellation.
    /// - a held key whose repeats already fired.
    private func completion(
        cancelling state: TouchState,
        reason: Cancellation,
        wasRepeating: Bool
    ) -> KeyboardPressCommitQueue.Completion {
        guard reason == .system,
              let key = state.currentKey,
              key.role.commitsOnSystemCancellation,
              !state.hasWandered
        else { return .cancelled }
        return wasRepeating ? .suppressed : .released
    }

    func reconcileActiveTouches(_ activeTokens: Set<TouchToken>) {
        // Toolbar and accessibility presses never reach the overlay, so they are
        // absent from `activeTokens` by construction and must not be read as
        // abandoned — an unrelated keycap touch would otherwise cancel them.
        let orphaned = Set(touches.keys)
            .subtracting(activeTokens)
            .filter { $0 < Self.firstLegacyToken }
        for token in orphaned { cancelTouch(token: token, reason: .system) }
    }

    /// Teardown: the surface is going away, so pending presses are discarded
    /// rather than routed through `cancelTouch`, which would honour them.
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
            // The toolbar and accessibility controls report drag-off and system
            // cancellation through one action, so this path cannot tell them apart
            // and keeps discarding the press.
            if let token = takeLegacyToken(for: event.key.id) {
                cancelTouch(token: token, reason: .userIntent)
            }
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

private extension KeyRole {
    /// Text-producing keys that are safe to honour when the system cancels the
    /// touch. Backspace and Return are deliberately absent.
    var commitsOnSystemCancellation: Bool {
        switch self {
        case .character, .vniModifier, .punctuation, .space: true
        default: false
        }
    }
}
#endif
