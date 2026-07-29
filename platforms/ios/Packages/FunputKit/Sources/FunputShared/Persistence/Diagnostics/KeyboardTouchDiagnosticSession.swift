#if DEBUG
import Foundation
import KeyboardLayout

public enum KeyboardTouchDiagnosticPhase: String, Codable, Sendable {
    case guided
    case free
}

public enum KeyboardTouchDiagnosticPipelineMode: String, Codable, Sendable {
    case legacy
    case primaryFastTap
}

public struct KeyboardTouchDiagnosticSession: Codable, Equatable, Sendable {
    public let id: UUID
    public let inputMethod: KeyboardInputMethod
    public let phase: KeyboardTouchDiagnosticPhase
    public let pipelineMode: KeyboardTouchDiagnosticPipelineMode
    public let generation: UInt64
    public let startedAt: Date
    public let expiresAt: Date

    public init(
        id: UUID = UUID(),
        inputMethod: KeyboardInputMethod,
        phase: KeyboardTouchDiagnosticPhase,
        pipelineMode: KeyboardTouchDiagnosticPipelineMode = .legacy,
        generation: UInt64,
        startedAt: Date = Date(),
        expiresAt: Date? = nil
    ) {
        self.id = id
        self.inputMethod = inputMethod
        self.phase = phase
        self.pipelineMode = pipelineMode
        self.generation = generation
        self.startedAt = startedAt
        self.expiresAt = expiresAt ?? startedAt.addingTimeInterval(15 * 60)
    }

    public func isActive(at date: Date) -> Bool {
        startedAt <= date && date < expiresAt
    }

    private enum CodingKeys: String, CodingKey {
        case id, inputMethod, phase, pipelineMode, generation, startedAt, expiresAt
    }

    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        id = try values.decode(UUID.self, forKey: .id)
        inputMethod = try values.decode(KeyboardInputMethod.self, forKey: .inputMethod)
        phase = try values.decode(KeyboardTouchDiagnosticPhase.self, forKey: .phase)
        pipelineMode = try values.decodeIfPresent(
            KeyboardTouchDiagnosticPipelineMode.self,
            forKey: .pipelineMode
        ) ?? .legacy
        generation = try values.decode(UInt64.self, forKey: .generation)
        startedAt = try values.decode(Date.self, forKey: .startedAt)
        expiresAt = try values.decode(Date.self, forKey: .expiresAt)
    }
}
#endif
