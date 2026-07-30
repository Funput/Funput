#if DEBUG
import Foundation

public struct KeyboardTouchDiagnosticDevice: Codable, Equatable, Sendable {
    public let model: String
    public let operatingSystem: String
    public let maximumFramesPerSecond: Int

    public init(model: String, operatingSystem: String, maximumFramesPerSecond: Int) {
        self.model = model
        self.operatingSystem = operatingSystem
        self.maximumFramesPerSecond = maximumFramesPerSecond
    }
}

public struct KeyboardTouchDiagnosticReport: Codable, Equatable, Sendable {
    public let sessionID: UUID
    public let generation: UInt64
    public let sequence: UInt64
    public let observedAt: Date
    public let metrics: KeyboardTouchDiagnosticMetrics
    public let activeContactCount: Int
    public let pendingContactCount: Int
    public let isSettled: Bool
    public let device: KeyboardTouchDiagnosticDevice

    public init(
        sessionID: UUID,
        generation: UInt64,
        sequence: UInt64,
        observedAt: Date,
        metrics: KeyboardTouchDiagnosticMetrics,
        activeContactCount: Int,
        pendingContactCount: Int,
        isSettled: Bool,
        device: KeyboardTouchDiagnosticDevice
    ) {
        self.sessionID = sessionID
        self.generation = generation
        self.sequence = sequence
        self.observedAt = observedAt
        self.metrics = metrics
        self.activeContactCount = activeContactCount
        self.pendingContactCount = pendingContactCount
        self.isSettled = isSettled
        self.device = device
    }
}
#endif
