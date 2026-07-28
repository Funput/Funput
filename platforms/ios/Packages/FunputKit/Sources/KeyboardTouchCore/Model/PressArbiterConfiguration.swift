import Foundation

public struct PressArbiterConfiguration: Equatable, Sendable {
    public static let `default` = PressArbiterConfiguration()

    public let rolloverWindow: TimeInterval

    public init(rolloverWindow: TimeInterval = 0.040) {
        precondition(rolloverWindow.isFinite && rolloverWindow > 0)
        self.rolloverWindow = rolloverWindow
    }
}
