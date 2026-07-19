#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputCore

/// Serial-owner Swift boundary around the local Rust suggestion engine.
public final class PersonalSuggestionEngine: @unchecked Sendable {
    private let handle: OpaquePointer
    private let releaseHandle: @Sendable (OpaquePointer) -> Void

    public static func inMemory() -> PersonalSuggestionEngine? {
        guard let handle = funput_suggestion_engine_new_in_memory() else { return nil }
        return PersonalSuggestionEngine(handle: handle)
    }

    public static func open(storeURL: URL) -> PersonalSuggestionEngine? {
        let path = Array(storeURL.path.utf8)
        let handle = path.withUnsafeBufferPointer {
            funput_suggestion_engine_open($0.baseAddress, UInt($0.count))
        }
        guard let handle else { return nil }
        return PersonalSuggestionEngine(handle: handle)
    }

    init(
        handle: OpaquePointer,
        releaseHandle: @escaping @Sendable (OpaquePointer) -> Void = {
            funput_suggestion_engine_free($0)
        }
    ) {
        self.handle = handle
        self.releaseHandle = releaseHandle
    }

    deinit {
        releaseHandle(handle)
    }

    @discardableResult
    public func learn(_ token: String) -> Bool {
        let codepoints = token.unicodeScalars.map(\.value)
        return codepoints.withUnsafeBufferPointer {
            funput_suggestion_learn(handle, $0.baseAddress, UInt($0.count))
        }
    }

    public func query(_ prefix: String) -> [PersonalSuggestionCandidate] {
        let codepoints = prefix.unicodeScalars.map(\.value)
        let result = codepoints.withUnsafeBufferPointer {
            funput_suggestion_query(handle, $0.baseAddress, UInt($0.count))
        }
        return PersonalSuggestionDecoder.candidates(result)
    }

    @discardableResult public func flush() -> Bool { funput_suggestion_flush(handle) }
    @discardableResult public func compact() -> Bool { funput_suggestion_compact(handle) }
    @discardableResult public func reset() -> Bool { funput_suggestion_reset(handle) }

    public func stats() -> PersonalSuggestionStats {
        let value = funput_suggestion_stats(handle)
        return PersonalSuggestionStats(
            words: Int(value.words),
            promotedWords: Int(value.promoted_words),
            exactNodes: Int(value.exact_nodes),
            foldedNodes: Int(value.folded_nodes),
            pendingMutations: Int(value.pending_mutations),
            journalBytes: value.journal_bytes,
            estimatedHeapBytes: value.estimated_heap_bytes,
            lastSnapshotBytes: value.last_snapshot_bytes
        )
    }
}
#endif
