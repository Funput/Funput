import Foundation

public struct KeyboardTouchShadowMetrics: Equatable, Sendable {
    public internal(set) var capturedBegan = 0
    public internal(set) var shadowResolved = 0
    public internal(set) var legacyReleased = 0
    public internal(set) var matched = 0
    public internal(set) var orderMismatch = 0
    public internal(set) var legacyMissing = 0
    public internal(set) var shadowMissing = 0
    public internal(set) var legacyLate = 0
    public internal(set) var shadowLate = 0
    public internal(set) var shadowCancelled = 0
    public internal(set) var legacyCancelled = 0
    public internal(set) var timestampTie = 0
    public internal(set) var unknownCallback = 0
    public internal(set) var layoutChangedWhileActive = 0
    public internal(set) var droppedForCapacity = 0

    public var cancellationDisagreement: Int {
        abs(shadowCancelled - legacyCancelled)
    }
}

enum KeyboardTouchShadowEvent: Int32 {
    case capturedBegan = 1
    case shadowResolved
    case legacyReleased
    case matched
    case orderMismatch
    case legacyMissing
    case shadowMissing
    case legacyLate
    case shadowLate
    case shadowCancelled
    case legacyCancelled
    case timestampTie
    case unknownCallback
    case layoutChangedWhileActive
    case droppedForCapacity
}

@MainActor
public final class KeyboardTouchShadowTrace {
    public private(set) var metrics = KeyboardTouchShadowMetrics()

    public init() {}

    func record(_ event: KeyboardTouchShadowEvent) {
        switch event {
        case .capturedBegan: metrics.capturedBegan += 1
        case .shadowResolved: metrics.shadowResolved += 1
        case .legacyReleased: metrics.legacyReleased += 1
        case .matched: metrics.matched += 1
        case .orderMismatch: metrics.orderMismatch += 1
        case .legacyMissing: metrics.legacyMissing += 1
        case .shadowMissing: metrics.shadowMissing += 1
        case .legacyLate: metrics.legacyLate += 1
        case .shadowLate: metrics.shadowLate += 1
        case .shadowCancelled: metrics.shadowCancelled += 1
        case .legacyCancelled: metrics.legacyCancelled += 1
        case .timestampTie: metrics.timestampTie += 1
        case .unknownCallback: metrics.unknownCallback += 1
        case .layoutChangedWhileActive: metrics.layoutChangedWhileActive += 1
        case .droppedForCapacity: metrics.droppedForCapacity += 1
        }
        KeyboardTouchShadowSignpost.emit(event, total: total(for: event))
    }

    private func total(for event: KeyboardTouchShadowEvent) -> Int {
        switch event {
        case .matched: metrics.matched
        case .orderMismatch: metrics.orderMismatch
        case .legacyMissing: metrics.legacyMissing
        case .shadowMissing: metrics.shadowMissing
        default: 1
        }
    }
}
