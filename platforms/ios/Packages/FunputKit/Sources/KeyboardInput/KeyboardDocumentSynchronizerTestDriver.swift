#if os(iOS) && canImport(FunputCore)
import Foundation

/// Release-build unit-test seam for the document echo state machine.
@_spi(Testing)
@MainActor
public struct KeyboardDocumentSynchronizerTestDriver {
    public let documentIdentifier: UUID
    private var synchronizer = KeyboardDocumentSynchronizer()

    public init(context: String? = "") {
        documentIdentifier = UUID()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: documentIdentifier,
            contextBeforeInput: context,
            hasSelection: false
        ))
    }

    public var snapshotContext: String? { synchronizer.snapshot?.contextBeforeInput }
    public var pendingContextCount: Int { synchronizer.pendingAuthoredContextCount }

    public mutating func insert(_ text: String, closesEpoch: Bool = false) {
        synchronizer.beginMutation(closesEpoch: closesEpoch)
        synchronizer.recordInsertion(text)
        _ = synchronizer.finishMutation()
    }

    public mutating func acceptExternal(_ context: String?, hasSelection: Bool = false) {
        synchronizer.accept(
            KeyboardDocumentSnapshot(
                documentIdentifier: documentIdentifier,
                contextBeforeInput: context,
                hasSelection: hasSelection
            ),
            preservingAuthoredEchoes: true
        )
    }

    public mutating func consumeEcho(_ context: String?) -> Bool {
        synchronizer.consumeAuthoredTextChange(
            documentIdentifier: documentIdentifier,
            contextBeforeInput: context
        )
    }

    public func requiresReset(context: String?, buffer: String, hasSelection: Bool = false) -> Bool {
        synchronizer.requiresCompositionReset(
            for: KeyboardDocumentSnapshot(
                documentIdentifier: documentIdentifier,
                contextBeforeInput: context,
                hasSelection: hasSelection
            ),
            composerBuffer: buffer
        )
    }
}
#endif
