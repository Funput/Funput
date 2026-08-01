public enum ContactResolution<Payload: Sendable>: Sendable {
    case began(ContactID)
    case resolved(ContactID, Payload, ContactResolutionMetadata)
    case cancelled(ContactID, ContactCancellationReason)
    case noOp(ContactResolutionNoOp)
}

public struct ContactResolutionMetadata: Equatable, Sendable {
    public let exceededTapSlop: Bool

    public init(exceededTapSlop: Bool) {
        self.exceededTapSlop = exceededTapSlop
    }
}

public enum ContactCancellationReason: Int, Equatable, Sendable {
    case system
    case exceededTapSlop
    case exceededDuration
    case endedOutside
}

public enum ContactResolutionNoOp: Equatable, Sendable {
    case updated
    case duplicateBegin
    case beganOutside
    case unknownContact
}

extension ContactResolution: Equatable where Payload: Equatable {}
