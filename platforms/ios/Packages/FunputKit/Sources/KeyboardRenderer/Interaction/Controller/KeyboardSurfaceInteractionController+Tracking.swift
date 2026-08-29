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
            smartGestures: presentation.areSmartGesturesEnabled,
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
        if presentation.areSmartGesturesEnabled, key.role == .space {
            // Holding space is how the caret pan starts, so the spacebar does not repeat while
            // smart gestures are on. It used to, on a longer delay, which made the gesture a
            // race the user had to win: hold a beat too long before dragging and the repeat
            // had already typed spaces and locked the pan out. Lifting without dragging still
            // types the one space, because arming is not claiming.
            spaceHoldController.start(for: token)
        } else if repeatTouch == nil, key.role == .backspace || key.role == .space {
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
        if state.trackpad != nil {
            updateSpaceTrackpad(token: token, translation: translation(for: state, at: point))
            return
        }
        if state.ratchet != nil {
            updateWordRatchet(token: token, translation: translation(for: state, at: point))
            return
        }
        if let layout = state.alternateLayout {
            let next = layout.selection(at: point, from: state.startPoint)
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
            if state.hasWandered {
                alternateHoldController.cancel(for: token)
                // A finger that travels this far before the hold fires is swiping, not
                // holding: cancelling here is what keeps a quick swipe on the spacebar a
                // language toggle rather than a caret pan.
                if !state.holdArmed { spaceHoldController.cancel(for: token) }
            }
        }
        let travel = translation(for: state, at: point)
        if activateSpaceTrackpad(token: token, translation: travel) {
            updateSpaceTrackpad(token: token, translation: travel)
            return
        }
        if activateWordRatchet(token: token, translation: travel) {
            updateWordRatchet(token: token, translation: travel)
            return
        }
        if let action = state.swipeTracker.update(
            translation: travel,
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

    func translation(for state: TouchState, at point: CGPoint) -> CGPoint {
        CGPoint(x: point.x - state.startPoint.x, y: point.y - state.startPoint.y)
    }
}
#endif
