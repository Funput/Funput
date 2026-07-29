import Foundation

public struct PressArbiter<Payload: Sendable>: Sendable {
    enum EntryState: Sendable {
        case active
        case resolved(Payload)
    }

    struct Entry: Sendable {
        let contactID: ContactID
        let intentSequence: UInt64
        let beganAt: TimeInterval
        var state: EntryState
    }

    let configuration: PressArbiterConfiguration
    var ordered: [Entry] = []
    var detached: [ContactID: Entry] = [:]
    var blockedDeadline: TimeInterval?
    var nextIntentSequence: UInt64 = 1
    public internal(set) var bypassedContactCount: UInt64 = 0

    public init(configuration: PressArbiterConfiguration = .default) {
        self.configuration = configuration
        ordered.reserveCapacity(10)
        detached.reserveCapacity(4)
    }

    public var nextDeadline: TimeInterval? { blockedDeadline }

    @discardableResult
    public mutating func begin(_ contactID: ContactID, at timestamp: TimeInterval) -> Bool {
        guard entryIndex(for: contactID) == nil, detached[contactID] == nil else {
            return false
        }
        precondition(nextIntentSequence < UInt64.max)
        let entry = Entry(
            contactID: contactID,
            intentSequence: nextIntentSequence,
            beganAt: timestamp,
            state: .active
        )
        let insertionIndex = ordered.firstIndex { entry.precedes($0) } ?? ordered.endIndex
        ordered.insert(entry, at: insertionIndex)
        nextIntentSequence += 1
        return true
    }

    public mutating func resolve(
        _ contactID: ContactID,
        payload: Payload,
        at timestamp: TimeInterval
    ) -> [PressEmission<Payload>] {
        if let entry = detached.removeValue(forKey: contactID) {
            return [entry.emission(payload)]
        }
        guard let index = entryIndex(for: contactID),
              case .active = ordered[index].state else { return [] }
        ordered[index].state = .resolved(payload)
        return drain(at: timestamp)
    }

    public mutating func cancel(
        _ contactID: ContactID,
        at timestamp: TimeInterval
    ) -> [PressEmission<Payload>] {
        if detached.removeValue(forKey: contactID) != nil { return [] }
        guard let index = entryIndex(for: contactID),
              case .active = ordered[index].state else { return [] }
        ordered.remove(at: index)
        return drain(at: timestamp)
    }

    public mutating func advance(to timestamp: TimeInterval) -> [PressEmission<Payload>] {
        drain(at: timestamp)
    }

    public mutating func reset() {
        ordered.removeAll(keepingCapacity: true)
        detached.removeAll(keepingCapacity: true)
        blockedDeadline = nil
        bypassedContactCount = 0
    }

    func entryIndex(for contactID: ContactID) -> Int? {
        ordered.firstIndex { $0.contactID == contactID }
    }
}

extension PressArbiter.Entry {
    func precedes(_ other: Self) -> Bool {
        beganAt < other.beganAt
            || beganAt == other.beganAt && intentSequence < other.intentSequence
    }

    func emission(_ payload: Payload) -> PressEmission<Payload> {
        PressEmission(
            contactID: contactID,
            intentSequence: intentSequence,
            payload: payload
        )
    }
}
