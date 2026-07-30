#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit

extension KeyboardTouchCoordinator {
    static let touchRoles: Set<KeyRole> = [
        .character, .vniModifier, .punctuation, .shift, .backspace,
        .symbols, .moreSymbols, .letters, .inputMethod, .systemInputMode,
        .space, .enter, .emoji,
    ]

    func consume(_ sample: ContactSample) {
        switch pipeline.consume(sample) {
        case let .began(id, hit):
            recordTimestampTie(id, at: sample.timestamp)
            beganAt[id] = sample.timestamp
            hits[id] = hit
            registry.begin(id, key: hit.key)
            registry.metrics.capturedContacts += 1
        case let .resolved(_, metadata):
            if metadata.exceededTapSlop {
                registry.metrics.recoveredTapSlop += 1
            }
        case let .fallback(id, reason):
            if reason == .endedOutside { registry.metrics.endedOutside += 1 }
            cancel(id, emit: true, system: false)
        case let .cancelled(id):
            cancel(id, emit: true, system: true)
        case let .ignored(reason):
            switch reason {
            case .unknownContact:
                registry.metrics.resolverUnknownCallback += 1
            case .beganOutside:
                registry.metrics.beganOutside += 1
            case .duplicateBegin:
                registry.metrics.ownershipViolation += 1
            case .updated:
                break
            }
        case .tracking:
            break
        }
        observePipelineState()
        notify()
    }

    func claim(
        token: UInt64,
        kind: KeyboardSurfaceInteractionController.GestureClaim
    ) {
        let id = ContactID(rawValue: token)
        guard claims[id] == nil else { return }
        let accepted = kind == .repeatKey
            ? pipeline.detach(id, at: clock())
            : pipeline.claimForGesture(id)
        guard accepted else {
            registry.metrics.gestureConflict += 1
            notify()
            return
        }
        claims[id] = kind
        if kind == .repeatKey {
            registry.metrics.repeatClaimedContacts += 1
        }
        notify()
    }

    func handleInteraction(
        token: UInt64,
        event: KeyboardKeyEvent
    ) -> KeyboardKeyEvent? {
        let id = ContactID(rawValue: token)
        switch event.phase {
        case .pressed:
            return event
        case .repeated:
            return registry.emitRepeat(id) ? event : nil
        case let .alternateSelected(value):
            resolve(id, action: hits[id].map { .alternate($0, value) })
        case let .swiped(action):
            resolve(id, action: hits[id].map { .swiped($0, action) })
        case .cancelled:
            cancelClaim(id)
        case .released:
            registry.metrics.ownershipViolation += 1
        }
        notify()
        return nil
    }

    func finishUIKitContact(_ token: UInt64) {
        let id = ContactID(rawValue: token)
        finishedUIKitContacts.insert(id)
        if !registry.isPending(id) { clear(id) }
        notify()
    }

    private func resolve(_ id: ContactID, action: KeyboardTouchAction?) {
        guard let action else {
            registry.metrics.gestureConflict += 1
            return
        }
        let now = clock()
        terminalAt[id] = now
        if !pipeline.resolveGesture(id, action: action, at: now) {
            registry.metrics.gestureConflict += 1
        }
    }

    private func cancelClaim(_ id: ContactID) {
        guard let claim = claims[id] else { return }
        _ = pipeline.detach(id, at: clock())
        if claim == .repeatKey {
            registry.finish(id)
        } else {
            cancel(id, emit: true)
        }
    }
}

extension KeyboardTouchAction {
    var keyEvent: KeyboardKeyEvent {
        switch self {
        case let .released(hit):
            KeyboardKeyEvent(key: hit.key, phase: .released)
        case let .repeated(hit):
            KeyboardKeyEvent(key: hit.key, phase: .repeated)
        case let .alternate(hit, alternate):
            KeyboardKeyEvent(key: hit.key, phase: .alternateSelected(alternate))
        case let .swiped(hit, action):
            KeyboardKeyEvent(key: hit.key, phase: .swiped(action))
        case let .cancelled(hit):
            KeyboardKeyEvent(key: hit.key, phase: .cancelled)
        }
    }
}

extension KeyRole {
    var isControl: Bool {
        switch self {
        case .character, .vniModifier, .punctuation, .space, .backspace:
            false
        default:
            true
        }
    }
}
#endif
