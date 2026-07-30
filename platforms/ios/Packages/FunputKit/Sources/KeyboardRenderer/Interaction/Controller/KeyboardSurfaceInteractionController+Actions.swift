#if canImport(UIKit)
import KeyboardLayout
import os
import UIKit

extension KeyboardSurfaceInteractionController {
    func activateAlternates(for token: TouchToken) {
        guard var state = touches[token],
              !state.hasWandered,
              state.currentKey?.id == state.initialKey.id,
              let sourceFrame = state.currentFrame,
              !state.initialKey.alternates.isEmpty else { return }
        onClaimGesture(token, .alternate)
        state.alternateLayout = .resolve(
            count: state.initialKey.alternates.count,
            sourceFrame: sourceFrame,
            bounds: state.containerBounds
        )
        state.selectedAlternateIndex = 0
        if let key = state.currentKey { setHighlighted(key, false) }
        state.currentKey = nil
        touches[token] = state
        if hapticsEnabled { haptics.perform(.control) }
        refreshPreview()
    }
    func performSuggestionFeedback(presentation: KeyboardPresentation) {
        if presentation.isHapticFeedbackEnabled { haptics.perform(.control) }
        if presentation.isKeySoundEnabled { UIDevice.current.playInputClick() }
    }

    func finishSwipe(token: TouchToken, state: TouchState, action: KeySwipeAction) {
        onClaimGesture(token, .swipe)
        finishV2Swipe(token: token, state: state, action: action)
    }

    func repeatActiveKey() {
        guard let token = repeatTouch, let state = touches[token],
              state.initialKey.role == .backspace || state.initialKey.role == .space
        else { return }
        onClaimGesture(token, .repeatKey)
        let event = KeyboardKeyEvent(key: state.initialKey, phase: .repeated)
        onContactEvent(token, event)
        if hapticsEnabled { haptics.perform(.deleteRepeat) }
    }

    func finishRepeatedTouch(token: TouchToken, state: TouchState) {
        finishV2RepeatedTouch(token: token, state: state)
    }

    func setHighlighted(_ key: KeySpec, _ highlighted: Bool) {
        let oldCount = highlightCounts[key.id, default: 0]
        let newCount = max(0, oldCount + (highlighted ? 1 : -1))
        if newCount == 0 {
            highlightCounts.removeValue(forKey: key.id)
        } else {
            highlightCounts[key.id] = newCount
        }
        if oldCount == 0, newCount == 1 { onHighlight(key, true) }
        if oldCount == 1, newCount == 0 { onHighlight(key, false) }
    }

    func refreshPreview() {
        let alternateToken = touches.keys
            .filter { touches[$0]?.alternateLayout != nil }
            .max()
        guard let token = alternateToken ?? touches.keys.max(),
              let state = touches[token] else {
            onPreview(nil, nil)
            onAlternatePreview(nil, nil, nil)
            return
        }
        if let layout = state.alternateLayout {
            onPreview(nil, nil)
            onAlternatePreview(state.initialKey, layout, state.selectedAlternateIndex)
            return
        }
        onAlternatePreview(nil, nil, nil)
        guard let key = state.currentKey else {
            onPreview(nil, nil)
            return
        }
        onPreview(key, state.currentFrame)
    }

    func clearKeyRepeat() {
        repeatController.cancel()
        repeatTouch = nil
    }

    func endSignpost(_ state: TouchState, token: TouchToken, phase: Int) {
        os_signpost(
            .end,
            log: KeyboardTouchSignpost.log,
            name: "TouchSemantic",
            signpostID: state.signpostID,
            "token=%{public}llu phase=%{public}d depth=%{public}d",
            token,
            phase,
            touches.count
        )
    }
}
#endif
