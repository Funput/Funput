#if canImport(UIKit)
import KeyboardLayout

extension KeyboardSurfaceInteractionController {
    func finishTrackedTouch(token: TouchToken, state: TouchState) {
        alternateHoldController.cancel(for: token)
        if let key = state.currentKey { setHighlighted(key, false) }
        let wasRepeating = repeatTouch == token && repeatController.finish()
        if repeatTouch == token { repeatTouch = nil }
        if let index = state.selectedAlternateIndex,
           state.alternateLayout != nil,
           state.initialKey.alternates.indices.contains(index) {
            onContactEvent(
                token,
                KeyboardKeyEvent(
                    key: state.initialKey,
                    phase: .alternateSelected(state.initialKey.alternates[index])
                )
            )
        } else if state.alternateLayout != nil || wasRepeating {
            onContactEvent(
                token,
                KeyboardKeyEvent(key: state.initialKey, phase: .cancelled)
            )
        }
        endSignpost(state, token: token, phase: 1)
        refreshPreview()
    }

    func cancelTrackedTouch(token: TouchToken, state: TouchState) {
        alternateHoldController.cancel(for: token)
        if let key = state.currentKey { setHighlighted(key, false) }
        _ = repeatTouch == token && repeatController.finish()
        if repeatTouch == token { repeatTouch = nil }
        onContactEvent(
            token,
            KeyboardKeyEvent(key: state.initialKey, phase: .cancelled)
        )
        endSignpost(state, token: token, phase: 2)
        refreshPreview()
    }

    func completeSwipe(
        token: TouchToken,
        state: TouchState,
        action: KeySwipeAction
    ) {
        touches.removeValue(forKey: token)
        if let key = state.currentKey { setHighlighted(key, false) }
        if repeatTouch == token { clearKeyRepeat() }
        onContactEvent(
            token,
            KeyboardKeyEvent(key: state.initialKey, phase: .swiped(action))
        )
        endSignpost(state, token: token, phase: 3)
        if hapticsEnabled { haptics.perform(.control) }
        refreshPreview()
    }

    func completeRepeatedTouch(token: TouchToken, state: TouchState) {
        touches.removeValue(forKey: token)
        if let key = state.currentKey { setHighlighted(key, false) }
        endSignpost(state, token: token, phase: 1)
        refreshPreview()
    }
}
#endif
