import Foundation
#if canImport(UIKit)
import UIKit
#endif

/// Samples metadata while a keyboard is visible, including changes made by its host
/// process. No clipboard contents or history files are read by the monitor.
///
/// Polling is what catches the host's changes, which `UIPasteboard.changedNotification`
/// does not report, and it runs on the main thread the whole time the user types. A
/// full snapshot is four pasteboard queries, about 3ms on a device, so a tick that
/// lands on a keystroke delays it. Each tick therefore asks only for the change
/// count and reads the rest when that moves. Every other path (start, refresh, the
/// notification) still reads the full snapshot.
@MainActor
public final class ClipboardChangeMonitor: NSObject {
    private var task: Task<Void, Never>?
    private var previous: ClipboardSnapshot?
    private var readSnapshot: (() -> ClipboardSnapshot?)?
    private var readChangeCount: (() -> Int?)?
    private var onChange: (() -> Void)?

    public override init() { super.init() }

    deinit { task?.cancel(); NotificationCenter.default.removeObserver(self) }

    /// - Parameter readChangeCount: the cheap probe each tick uses. nil from the
    ///   probe means the same as nil from `readSnapshot`, no access right now;
    ///   without a probe every tick reads the full snapshot.
    public func start(
        readSnapshot: @escaping () -> ClipboardSnapshot?,
        readChangeCount: (() -> Int?)? = nil,
        onChange: @escaping () -> Void
    ) {
        stop()
        self.readSnapshot = readSnapshot
        self.readChangeCount = readChangeCount
        self.onChange = onChange
#if canImport(UIKit)
        NotificationCenter.default.addObserver(
            self, selector: #selector(clipboardChanged),
            name: UIPasteboard.changedNotification, object: nil
        )
#endif
        sample()
        task = Task { @MainActor [weak self] in
            while !Task.isCancelled {
                do { try await Task.sleep(for: .milliseconds(500)) }
                catch { return }
                guard !Task.isCancelled else { return }
                self?.poll()
            }
        }
    }

    public func stop() {
        NotificationCenter.default.removeObserver(self)
        task?.cancel()
        task = nil
        previous = nil
        readSnapshot = nil
        readChangeCount = nil
        onChange = nil
    }

    public func refresh() { sample() }

    @objc nonisolated private func clipboardChanged() {
        Task { @MainActor [weak self] in self?.sample() }
    }

    /// One polling tick: the full snapshot only when the change count moved, or when
    /// there is no settled answer to compare with. An indeterminate snapshot is iOS
    /// declining to answer (see ``ClipboardSnapshot/isIndeterminate``), so it is
    /// asked again in full until it gives a real one.
    func poll() {
        if let previous, !previous.isIndeterminate, let count = readChangeCount?(),
           count == previous.changeCount {
            return
        }
        sample()
    }

    func sample() {
        guard let snapshot = readSnapshot?() else {
            previous = nil
            return
        }
        guard snapshot != previous else { return }
        previous = snapshot
        onChange?()
    }
}
