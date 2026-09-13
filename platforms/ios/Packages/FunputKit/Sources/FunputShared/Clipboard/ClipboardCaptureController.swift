import Foundation

/// Coordinates one active keyboard session. A failed content read pauses automatic
/// capture until an explicit retry; metadata monitoring and manual Paste still work.
@MainActor
public final class ClipboardCaptureController {
    public private(set) var needsRetry = false
    public private(set) var lastPastedChangeCount: Int?
    private var processed: Int?
    private var suppressed: Int?
    private var isReading = false
    private var pending = false
    private var resampleTask: Task<Void, Never>?
    private var generation = 0
    private var active = false
    private let gateway: any ClipboardGateway
    private let allowsCapture: () -> Bool
    private let save: (ClipboardItem) -> Bool
    private let marks: any ClipboardSessionMarkStoring
    private let onUpdate: () -> Void

    public init(
        gateway: any ClipboardGateway,
        allowsCapture: @escaping () -> Bool,
        save: @escaping (ClipboardItem) -> Bool,
        marks: any ClipboardSessionMarkStoring,
        onUpdate: @escaping () -> Void
    ) {
        self.gateway = gateway
        self.allowsCapture = allowsCapture
        self.save = save
        self.marks = marks
        self.onUpdate = onUpdate
    }

    /// A new session inherits what the last one already learned about this
    /// pasteboard. The keyboard is torn down and rebuilt in every app it appears in,
    /// so starting blank meant re-reading the same clipboard — and every read shows
    /// the user a system paste banner — and re-offering text they had already pasted.
    public func begin() {
        generation += 1
        active = true
        processed = marks.lastCapturedChangeCount()
        suppressed = nil
        lastPastedChangeCount = marks.lastPastedChangeCount()
    }

    public func end() {
        generation += 1
        active = false
        pending = false
        resampleTask?.cancel()
        resampleTask = nil
    }

    public func synchronize(retry: Bool = false) {
        guard active, allowsCapture() else { return }
        guard !isReading else { pending = true; return }
        if retry { needsRetry = false; processed = nil }
        guard !needsRetry else { return }
        let before = gateway.snapshot()
        guard !before.isIndeterminate, before.hasStrings,
              before.changeCount != processed, before.changeCount != suppressed else { return }
        isReading = true
        defer {
            isReading = false
            schedulePendingRead()
        }
        let session = generation
        let text = gateway.readText()
        guard active, session == generation, allowsCapture() else { return }
        // Do not label an old provider result with a newer clipboard generation.
        guard gateway.snapshot().changeCount == before.changeCount else { pending = true; return }
        guard let text else { fail(); return }
        guard !text.isEmpty else { processed = before.changeCount; return }
        if save(ClipboardItem(text: text, sourceChangeCount: before.changeCount)) {
            processed = before.changeCount
            onUpdate()
        } else { fail() }
    }

    /// nil means the provider could not establish a stable clipboard generation.
    /// -1 is reserved for such manual pastes and never suppresses a live offer.
    public func didPaste(_ text: String, changeCount: Int?) {
        guard active, !text.isEmpty else { return }
        let current = gateway.snapshot()
        let stable = changeCount == current.changeCount ? changeCount : nil
        lastPastedChangeCount = stable
        if let stable { marks.markPasted(stable) }
        guard allowsCapture() else { return }
        if stable != nil, stable == processed { return }
        if save(ClipboardItem(text: text, sourceChangeCount: stable ?? -1)) {
            if let stable { processed = stable }
            onUpdate()
        } else { fail() }
    }

    public func suppressCurrentAfterClear() {
        guard active, allowsCapture() else { return }
        suppressed = gateway.snapshot().changeCount
        processed = suppressed
    }

    private func fail() {
        needsRetry = true
        onUpdate()
    }

    private func schedulePendingRead() {
        guard pending, active else { return }
        pending = false
        let session = generation
        resampleTask?.cancel()
        resampleTask = Task { @MainActor [weak self] in
            await Task.yield()
            guard !Task.isCancelled, let self, generation == session else { return }
            resampleTask = nil
            synchronize()
        }
    }
}
