import CoreGraphics
import Foundation

/// Turns a stream of contact samples into one decision per press.
///
/// A press commits the key under the finger when it **lifts**, as Apple's keyboard does. People
/// who come from it see the wrong key highlighted, slide onto the right one and let go; that
/// reflex only works if the lift decides. Measured against the iOS 27 keyboard, the key changes
/// as soon as the finger crosses into its neighbour's area, with no margin held back for a roll
/// on the way up, so none is held back here either.
///
/// A finger that lifts outside the tracked area has no key under it and the press cancels as
/// `endedOutside`. Whether that press still survives on the key it landed on is the host's
/// recovery policy, which hands the landed key back as the lift hit.
public struct ContactResolver<Payload: Sendable>: Sendable {
    struct State: Sendable {
        let beganAt: TimeInterval
        let startLocation: CGPoint
        /// What the finger is on now. At lift this is the key the press commits; `nil` means
        /// the finger is outside the tracked area.
        var currentPayload: Payload?
        var exceededTapSlop = false
    }

    private let configuration: ContactResolverConfiguration
    private var states: [ContactID: State] = [:]

    public init(configuration: ContactResolverConfiguration = .default) {
        self.configuration = configuration
        states.reserveCapacity(10)
    }

    public var activeContactCount: Int { states.count }

    public func isTracking(_ contactID: ContactID) -> Bool {
        states[contactID] != nil
    }

    public mutating func consume(
        _ sample: ContactSample,
        hit payload: Payload?
    ) -> ContactResolution<Payload> {
        switch sample.phase {
        case .began:
            return begin(sample, payload: payload)
        case .moved:
            return move(sample, payload: payload)
        case .ended:
            return end(sample, payload: payload)
        case .cancelled:
            return cancel(sample.id)
        }
    }

    public mutating func reset() {
        states.removeAll(keepingCapacity: true)
    }

    @discardableResult
    public mutating func discard(_ contactID: ContactID) -> Bool {
        states.removeValue(forKey: contactID) != nil
    }

    private mutating func begin(
        _ sample: ContactSample,
        payload: Payload?
    ) -> ContactResolution<Payload> {
        guard states[sample.id] == nil else { return .noOp(.duplicateBegin) }
        guard let payload else { return .noOp(.beganOutside) }
        states[sample.id] = State(
            beganAt: sample.timestamp,
            startLocation: sample.location,
            currentPayload: payload
        )
        return .began(sample.id)
    }

    private mutating func move(
        _ sample: ContactSample,
        payload: Payload?
    ) -> ContactResolution<Payload> {
        guard var state = states[sample.id] else { return .noOp(.unknownContact) }
        update(&state, sample: sample, payload: payload)
        states[sample.id] = state
        return .noOp(.updated)
    }

    private mutating func end(
        _ sample: ContactSample,
        payload: Payload?
    ) -> ContactResolution<Payload> {
        guard var state = states.removeValue(forKey: sample.id) else {
            return .noOp(.unknownContact)
        }
        update(&state, sample: sample, payload: payload)
        let duration = max(0, sample.timestamp - state.beganAt)
        if duration > configuration.maximumTapDuration {
            return .cancelled(sample.id, .exceededDuration)
        }
        guard let committed = state.currentPayload else {
            return .cancelled(sample.id, .endedOutside)
        }
        return .resolved(
            sample.id,
            committed,
            ContactResolutionMetadata(exceededTapSlop: state.exceededTapSlop)
        )
    }

    private mutating func cancel(_ id: ContactID) -> ContactResolution<Payload> {
        guard states.removeValue(forKey: id) != nil else {
            return .noOp(.unknownContact)
        }
        return .cancelled(id, .system)
    }

    private func update(
        _ state: inout State,
        sample: ContactSample,
        payload: Payload?
    ) {
        let dx = sample.location.x - state.startLocation.x
        let dy = sample.location.y - state.startLocation.y
        let slop = configuration.tapSlop
        state.exceededTapSlop = state.exceededTapSlop || dx * dx + dy * dy > slop * slop
        state.currentPayload = payload
    }
}
