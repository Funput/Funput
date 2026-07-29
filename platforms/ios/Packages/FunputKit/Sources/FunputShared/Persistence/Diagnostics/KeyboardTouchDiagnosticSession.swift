#if DEBUG
import Foundation
import KeyboardLayout

public enum KeyboardTouchDiagnosticPhase: String, Codable, Sendable {
    case guided
    case free
}

public struct KeyboardTouchDiagnosticSession: Codable, Equatable, Sendable {
    public let id: UUID
    public let inputMethod: KeyboardInputMethod
    public let phase: KeyboardTouchDiagnosticPhase
    public let generation: UInt64
    public let startedAt: Date
    public let expiresAt: Date

    public init(
        id: UUID = UUID(),
        inputMethod: KeyboardInputMethod,
        phase: KeyboardTouchDiagnosticPhase,
        generation: UInt64,
        startedAt: Date = Date(),
        expiresAt: Date? = nil
    ) {
        self.id = id
        self.inputMethod = inputMethod
        self.phase = phase
        self.generation = generation
        self.startedAt = startedAt
        self.expiresAt = expiresAt ?? startedAt.addingTimeInterval(15 * 60)
    }

    public func isActive(at date: Date) -> Bool {
        startedAt <= date && date < expiresAt
    }
}
#endif
