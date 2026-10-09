import Foundation

/// Holds automatic clipboard capture back until the user stops typing.
///
/// Reading clipboard contents is the one pasteboard call that can stall — a
/// Universal Clipboard item is fetched from the Mac on demand — and it is the one
/// that raises the system paste prompt. Neither may land in the middle of a word,
/// so a requested capture waits for a quiet period with no key activity.
///
/// `noteActivity()` sits on the typing hot path: it only stores a timestamp. The
/// waiting task notices newer activity when it wakes and simply sleeps again.
@MainActor
public final class ClipboardIdleGate {
    public typealias Sleep = @MainActor (Duration) async throws -> Void

    public static let defaultQuietPeriod: Duration = .seconds(2)

    private let quietPeriod: Duration
    private let now: @MainActor () -> ContinuousClock.Instant
    private let sleep: Sleep
    private var lastActivity: ContinuousClock.Instant
    private var action: (() -> Void)?
    private var task: Task<Void, Never>?

    public init(
        quietPeriod: Duration = ClipboardIdleGate.defaultQuietPeriod,
        now: @escaping @MainActor () -> ContinuousClock.Instant = { .now },
        sleep: @escaping Sleep = { try await Task.sleep(for: $0) }
    ) {
        self.quietPeriod = quietPeriod
        self.now = now
        self.sleep = sleep
        lastActivity = now()
    }

    /// True while a capture is waiting for the user to go quiet.
    public var isWaiting: Bool { action != nil }

    public func noteActivity() {
        lastActivity = now()
    }

    /// Runs `action` once the keyboard has been quiet for the quiet period. A newer
    /// request replaces an older one still waiting; only the latest matters, since
    /// the capture re-reads whatever the pasteboard holds when it finally runs.
    public func requestCapture(_ action: @escaping () -> Void) {
        self.action = action
        guard task == nil else { return }
        task = Task { @MainActor [weak self] in
            await self?.waitForQuiet()
        }
    }

    /// Waits for the pending capture, if any, to run or be cancelled. For tests.
    public func settle() async {
        await task?.value
    }

    public func cancel() {
        task?.cancel()
        task = nil
        action = nil
    }

    private func waitForQuiet() async {
        while !Task.isCancelled {
            let remaining = quietPeriod - lastActivity.duration(to: now())
            guard remaining > .zero else { break }
            do { try await sleep(remaining) } catch { return }
        }
        guard !Task.isCancelled, let action else { return }
        task = nil
        self.action = nil
        action()
    }
}
