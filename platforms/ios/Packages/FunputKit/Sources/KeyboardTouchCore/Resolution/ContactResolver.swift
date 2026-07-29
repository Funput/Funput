import CoreGraphics
import Foundation

public struct ContactResolver<Payload: Sendable>: Sendable {
    struct State: Sendable {
        let beganAt: TimeInterval
        let startLocation: CGPoint
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
        guard let payload = state.currentPayload else {
            return .cancelled(sample.id, .endedOutside)
        }
        return .resolved(
            sample.id,
            payload,
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
