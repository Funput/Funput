import CoreGraphics
import Foundation

public struct ContactSample: Sendable {
    public let id: ContactID
    public let phase: ContactPhase
    public let timestamp: TimeInterval
    public let location: CGPoint
    public let previousLocation: CGPoint

    public init(
        id: ContactID,
        phase: ContactPhase,
        timestamp: TimeInterval,
        location: CGPoint,
        previousLocation: CGPoint
    ) {
        precondition(timestamp.isFinite)
        self.id = id
        self.phase = phase
        self.timestamp = timestamp
        self.location = location
        self.previousLocation = previousLocation
    }
}

extension ContactSample: Equatable {}
