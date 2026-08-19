#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceInteractionController {
    /// Takes ownership of a leftward rub on Backspace while the finger is still on the
    /// keycap, so it can never drift onto a neighbour and commit that key instead.
    func activateWordRatchet(token: TouchToken, translation: CGPoint) -> Bool {
        guard var state = touches[token],
              state.smartGestures,
              state.initialKey.role == .backspace,
              state.ratchet == nil,
              // Once Backspace has begun repeating, the user is watching characters
              // disappear one by one; switching to whole words mid-press is a surprise.
              !(repeatTouch == token && repeatController.hasRepeated)
        else { return false }
        let ratchet = BackspaceWordRatchet()
        guard ratchet.shouldClaim(translation), onClaimGesture(token, .wordDelete) else {
            return false
        }
        clearKeyRepeat()
        state.claimedGesture = .wordDelete
        state.ratchet = ratchet
        touches[token] = state
        return true
    }

    func updateWordRatchet(token: TouchToken, translation: CGPoint) {
        guard var state = touches[token], var ratchet = state.ratchet else { return }
        let words = ratchet.update(translation)
        state.ratchet = ratchet
        touches[token] = state
        guard words > 0 else { return }
        for _ in 0..<words {
            onContactEvent(
                token,
                KeyboardKeyEvent(key: state.initialKey, phase: .deletedWord)
            )
        }
        if hapticsEnabled { haptics.perform(.delete) }
    }

    /// A rub that claimed but never crossed a full step still deletes one character, so a
    /// short flick reads as a backspace rather than as a dropped touch.
    func finishWordRatchet(token: TouchToken, state: TouchState) {
        if state.ratchet?.hasDeleted == false {
            onContactEvent(
                token,
                KeyboardKeyEvent(key: state.initialKey, phase: .repeated)
            )
        }
        completeGestureTouch(token: token, state: state)
    }
}
#endif
