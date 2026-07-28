import Foundation
import KeyboardTouchCore

public struct KeyboardTouchShadowConfiguration: Equatable, Sendable {
    public static let `default` = KeyboardTouchShadowConfiguration()

    public let resolver: ContactResolverConfiguration
    public let arbiter: PressArbiterConfiguration
    public let settlementWindow: TimeInterval
    public let maximumBufferedActions: Int

    public init(
        resolver: ContactResolverConfiguration = .default,
        arbiter: PressArbiterConfiguration = .default,
        settlementWindow: TimeInterval = 0.120,
        maximumBufferedActions: Int = 64
    ) {
        precondition(settlementWindow.isFinite && settlementWindow > 0)
        precondition(maximumBufferedActions > 0)
        self.resolver = resolver
        self.arbiter = arbiter
        self.settlementWindow = settlementWindow
        self.maximumBufferedActions = maximumBufferedActions
    }
}
