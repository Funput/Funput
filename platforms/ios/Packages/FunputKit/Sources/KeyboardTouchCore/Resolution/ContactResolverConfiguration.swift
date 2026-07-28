import CoreGraphics
import Foundation

public struct ContactResolverConfiguration: Equatable, Sendable {
    public static let `default` = ContactResolverConfiguration()

    public let tapSlop: CGFloat
    public let maximumTapDuration: TimeInterval

    public init(
        tapSlop: CGFloat = 16,
        maximumTapDuration: TimeInterval = 0.300
    ) {
        precondition(tapSlop.isFinite && tapSlop > 0)
        precondition(maximumTapDuration.isFinite && maximumTapDuration > 0)
        self.tapSlop = tapSlop
        self.maximumTapDuration = maximumTapDuration
    }
}
