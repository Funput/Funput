import Foundation
#if canImport(UIKit)
import UIKit
#endif

/// Samples metadata while a keyboard is visible, including changes made by its host
/// process. No clipboard contents or history files are read by the monitor.
@MainActor
public final class ClipboardChangeMonitor: NSObject {
    private var task: Task<Void, Never>?
    private var previous: ClipboardSnapshot?
    private var readSnapshot: (() -> ClipboardSnapshot?)?
    private var onChange: (() -> Void)?

    public override init() { super.init() }

    deinit { task?.cancel(); NotificationCenter.default.removeObserver(self) }

    public func start(
        readSnapshot: @escaping () -> ClipboardSnapshot?,
        onChange: @escaping () -> Void
    ) {
        stop()
        self.readSnapshot = readSnapshot
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
                self?.sample()
            }
        }
    }

    public func stop() {
        NotificationCenter.default.removeObserver(self)
        task?.cancel()
        task = nil
        previous = nil
        readSnapshot = nil
        onChange = nil
    }

    public func refresh() { sample() }

    @objc nonisolated private func clipboardChanged() {
        Task { @MainActor [weak self] in self?.sample() }
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
