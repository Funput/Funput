public enum ContactResolution<Payload: Sendable>: Sendable {
    case began(ContactID)
    case resolved(ContactID, Payload)
    case cancelled(ContactID)
    case noOp(ContactResolutionNoOp)
}

public enum ContactResolutionNoOp: Equatable, Sendable {
    case updated
    case duplicateBegin
    case beganOutside
    case unknownContact
}

extension ContactResolution: Equatable where Payload: Equatable {}
