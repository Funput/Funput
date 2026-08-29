@MainActor
public protocol KeyboardDocumentWriting {
    var snapshot: KeyboardDocumentSnapshot { get }
    func apply(_ transaction: InputTransaction)
}
