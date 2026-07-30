#if canImport(UIKit)
import KeyboardLayout
import os
import UIKit

extension KeyboardSurfaceInteractionController {
    func beginTouch(
        token: TouchToken,
        key: KeySpec,
        point: CGPoint,
        sourceFrame: CGRect?,
        containerBounds: CGRect = .zero,
        presentation: KeyboardPresentation
    ) {
        guard touches[token] == nil else { return }
        hapticsEnabled = presentation.isHapticFeedbackEnabled
        let signpostID = OSSignpostID(log: KeyboardTouchSignpost.log)
        touches[token] = TouchState(
            initialKey: key,
            startPoint: point,
            currentKey: key,
            currentFrame: sourceFrame,
            containerBounds: containerBounds,
            alternateLayout: nil,
            selectedAlternateIndex: nil,
            signpostID: signpostID
        )
        os_signpost(
            .begin,
            log: KeyboardTouchSignpost.log,
            name: "TouchSemantic",
            signpostID: signpostID,
            "token=%{public}llu depth=%{public}d",
            token,
            touches.count
        )
        setHighlighted(key, true)
        if hapticsEnabled, let type = KeyHapticTypeMapper.map(key.role) {
            haptics.perform(type)
        }
        if presentation.isKeySoundEnabled { UIDevice.current.playInputClick() }
        if repeatTouch == nil, key.role == .backspace || key.role == .space {
            repeatTouch = token
            repeatController.start()
        }
        if !key.alternates.isEmpty, sourceFrame != nil, !containerBounds.isEmpty {
            alternateHoldController.start(for: token)
        }
        if presentation.showsKeyPreviews { refreshPreview() }
        onContactEvent(token, KeyboardKeyEvent(key: key, phase: .pressed))
    }

    func moveTouch(
        token: TouchToken,
        key hitKey: KeySpec?,
        point: CGPoint,
        sourceFrame: CGRect?,
        presentation: KeyboardPresentation
    ) {
        guard var state = touches[token] else { return }
        hapticsEnabled = presentation.isHapticFeedbackEnabled
        if let layout = state.alternateLayout {
            let next = layout.index(at: point)
            if next != state.selectedAlternateIndex, hapticsEnabled {
                haptics.perform(.control)
            }
            state.selectedAlternateIndex = next
            touches[token] = state
            refreshPreview()
            return
        }
        if !state.hasWandered {
            let dx = point.x - state.startPoint.x
            let dy = point.y - state.startPoint.y
            let slop = KeyboardSurfaceInteractionController.tapSlop
            state.hasWandered = dx * dx + dy * dy > slop * slop
            if state.hasWandered { alternateHoldController.cancel(for: token) }
        }
        if let action = state.swipeTracker.update(
            translation: CGPoint(x: point.x - state.startPoint.x, y: point.y - state.startPoint.y),
            action: state.initialKey.horizontalSwipeAction
        ) {
            touches[token] = state
            let hadRepeated = repeatTouch == token && repeatController.finish()
            if repeatTouch == token { repeatTouch = nil }
            if hadRepeated {
                finishRepeatedTouch(token: token, state: state)
            } else {
                finishSwipe(token: token, state: state, action: action)
            }
            return
        }

        let target = state.initialKey.horizontalSwipeAction == nil
            ? hitKey
            : (hitKey == nil ? nil : state.initialKey)
        guard target?.id != state.currentKey?.id else {
            state.currentFrame = sourceFrame
            touches[token] = state
            return
        }
        if let old = state.currentKey { setHighlighted(old, false) }
        state.currentKey = target
        state.currentFrame = sourceFrame
        if let target {
            setHighlighted(target, true)
        }
        if repeatTouch == token, target?.role != state.initialKey.role { clearKeyRepeat() }
        if target?.id != state.initialKey.id { alternateHoldController.cancel(for: token) }
        touches[token] = state
        if presentation.showsKeyPreviews { refreshPreview() }
    }
}
#endif
