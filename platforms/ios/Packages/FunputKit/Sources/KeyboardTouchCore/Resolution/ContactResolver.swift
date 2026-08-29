import CoreGraphics
import Foundation

/// Turns a stream of contact samples into one decision per press.
///
/// A press commits the key the finger **landed** on. The finger moves between landing and
/// lifting — it rolls as it comes off, and the roll carries the direction of whatever key is
/// typed next — so the lift point is the noisiest moment of the whole gesture and the one the
/// user has least control over. Resolving to it made a press near a key edge commit its
/// neighbour without any signal that something had gone wrong: not far enough to trip the tap
/// slop, and never visible to the person typing, who had pressed the right key.
///
/// The lift point still decides one thing: whether the finger lifted inside the tracked area
/// at all. Where inside it lifted is not the resolver's business.
public struct ContactResolver<Payload: Sendable>: Sendable {
    struct State: Sendable {
        let beganAt: TimeInterval
        let startLocation: CGPoint
        /// What the finger was on when it landed. This is what a press commits.
        let landedPayload: Payload
        /// What it is on now, which decides only whether the lift happened inside the
        /// tracked area — never which key the press meant.
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
            landedPayload: payload,
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
        guard state.currentPayload != nil else {
            return .cancelled(sample.id, .endedOutside)
        }
        return .resolved(
            sample.id,
            state.landedPayload,
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
