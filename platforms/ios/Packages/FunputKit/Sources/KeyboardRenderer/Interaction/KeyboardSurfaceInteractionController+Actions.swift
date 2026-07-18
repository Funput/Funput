#if canImport(UIKit)
import KeyboardLayout
import os

extension KeyboardSurfaceInteractionController {
    func finishSwipe(token: TouchToken, state: TouchState, action: KeySwipeAction) {
        touches.removeValue(forKey: token)
        if let key = state.currentKey { setHighlighted(key, false) }
        if repeatTouch == token { clearKeyRepeat() }
        commitQueue.complete(token: token, as: .swiped(action))
        endSignpost(state, token: token, phase: 3)
        if hapticsEnabled { haptics.perform(.control) }
        refreshPreview()
        flushCompletedKeys()
    }

    func repeatActiveKey() {
        guard let token = repeatTouch,
              let state = touches[token],
              state.initialKey.role == .backspace || state.initialKey.role == .space,
              commitQueue.bufferRepeat(for: token) else { return }
        if hapticsEnabled { haptics.perform(.deleteRepeat) }
        flushCompletedKeys()
    }

    func finishRepeatedTouch(token: TouchToken, state: TouchState) {
        touches.removeValue(forKey: token)
        if let key = state.currentKey { setHighlighted(key, false) }
        commitQueue.complete(token: token, as: .suppressed)
        endSignpost(state, token: token, phase: 1)
        refreshPreview()
        flushCompletedKeys()
    }

    func flushCompletedKeys() {
        while let action = commitQueue.popReadyAction() {
            switch action {
            case let .event(event): onEvent(event)
            case .suppressed: continue
            }
        }
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
        guard let token = touches.keys.max(),
              let state = touches[token],
              let key = state.currentKey else {
            onPreview(nil, nil)
            return
        }
        onPreview(key, state.currentFrame)
    }

    func clearKeyRepeat() {
        repeatController.cancel()
        repeatTouch = nil
    }

    func takeLegacyToken(for keyID: String) -> TouchToken? {
        guard var tokens = legacyTokensByKeyID[keyID], !tokens.isEmpty else { return nil }
        let token = tokens.removeFirst()
        if tokens.isEmpty {
            legacyTokensByKeyID.removeValue(forKey: keyID)
        } else {
            legacyTokensByKeyID[keyID] = tokens
        }
        return token
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
            commitQueue.depth
        )
    }
}
#endif
