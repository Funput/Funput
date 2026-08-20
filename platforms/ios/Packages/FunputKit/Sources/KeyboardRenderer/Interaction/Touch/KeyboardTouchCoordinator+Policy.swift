#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit

extension KeyboardTouchCoordinator {
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
        case let .fallback(id, _):
            // `endedOutside` is counted by the pipeline, which also sees the recovered ones.
            cancel(id, emit: true, system: false)
        case let .cancelled(id):
            cancel(id, emit: true, system: true)
        case let .ignored(reason):
            switch reason {
            case .unknownContact:
                // A claimed gesture owns the contact, so the resolver no longer tracks it.
                // Those callbacks are expected and must not read as a regression.
                if claims[sample.id] == nil {
                    registry.metrics.resolverUnknownCallback += 1
                }
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

    /// Takes ownership of a contact for a gesture. Returns whether the caller may go on to
    /// emit that gesture: a refusal means the pipeline no longer owns the contact, so emitting
    /// anyway would drop the action and count the failure twice.
    @discardableResult
    func claim(
        token: UInt64,
        kind: KeyboardSurfaceInteractionController.GestureClaim
    ) -> Bool {
        let id = ContactID(rawValue: token)
        if let existing = claims[id] {
            // A repeating press may still be upgraded to a word rub: both are already
            // detached, so the pipeline transition is done and only the label changes.
            guard existing.detachesContact, kind.detachesContact else { return existing == kind }
            claims[id] = kind
            return true
        }
        let accepted = kind.detachesContact
            ? pipeline.detach(id, at: clock())
            : pipeline.claimForGesture(id)
        guard accepted else {
            registry.metrics.gestureConflict += 1
            notify()
            return false
        }
        claims[id] = kind
        if kind == .repeatKey {
            registry.metrics.repeatClaimedContacts += 1
        }
        notify()
        return true
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
        case .cursorMoved, .deletedWord:
            // Written straight to the document by the gesture lane; the arbiter never
            // produces these, so they only pass through once the contact is detached.
            guard claims[id]?.detachesContact == true else {
                registry.metrics.gestureConflict += 1
                notify()
                return nil
            }
            notify()
            return event
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
        if claim.detachesContact {
            registry.finish(id)
        } else {
            cancel(id, emit: true)
        }
    }
}
#endif
