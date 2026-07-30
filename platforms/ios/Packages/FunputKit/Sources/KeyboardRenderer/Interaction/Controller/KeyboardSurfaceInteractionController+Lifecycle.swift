#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceInteractionController {
    func endTouch(token: TouchToken) {
        guard let state = touches.removeValue(forKey: token) else { return }
        if !usesLegacyTouchOutput {
            finishV2Touch(token: token, state: state)
            return
        }
        alternateHoldController.cancel(for: token)
        if let key = state.currentKey { setHighlighted(key, false) }
        let wasRepeating = repeatTouch == token && repeatController.finish()
        if repeatTouch == token { repeatTouch = nil }
        let completion: KeyboardPressCommitQueue.Completion
        if let index = state.selectedAlternateIndex,
           state.alternateLayout != nil,
           state.initialKey.alternates.indices.contains(index) {
            completion = .alternate(state.initialKey.alternates[index])
        } else {
            completion = state.currentKey == nil
                ? .cancelled : (wasRepeating ? .suppressed : .released)
        }
        commitQueue.complete(token: token, as: completion)
        endSignpost(state, token: token, phase: completion.isCommit ? 1 : 2)
        refreshPreview()
        flushCompletedKeys()
    }

    func cancelTouch(token: TouchToken, reason: Cancellation) {
        guard let state = touches.removeValue(forKey: token) else { return }
        if !usesLegacyTouchOutput {
            cancelV2Touch(token: token, state: state)
            return
        }
        alternateHoldController.cancel(for: token)
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
        guard usesLegacyTouchOutput else { return }
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
        alternateHoldController.cancelAll()
        for (token, state) in touches {
            if let key = state.currentKey { setHighlighted(key, false) }
            endSignpost(state, token: token, phase: 2)
        }
        touches.removeAll(keepingCapacity: true)
        if usesLegacyTouchOutput { commitQueue.cancelAll() }
        clearKeyRepeat()
        refreshPreview()
        if usesLegacyTouchOutput { flushCompletedKeys() }
        legacyTokensByKeyID.removeAll(keepingCapacity: true)
    }

}

private extension KeyboardPressCommitQueue.Completion {
    var isCommit: Bool {
        switch self {
        case .released, .alternate, .swiped: true
        case .cancelled, .suppressed: false
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
