public struct PressEmission<Payload: Sendable>: Sendable {
    public let contactID: ContactID
    public let intentSequence: UInt64
    public let payload: Payload

    public init(contactID: ContactID, intentSequence: UInt64, payload: Payload) {
        self.contactID = contactID
        self.intentSequence = intentSequence
        self.payload = payload
    }
}

extension PressEmission: Equatable where Payload: Equatable {}
