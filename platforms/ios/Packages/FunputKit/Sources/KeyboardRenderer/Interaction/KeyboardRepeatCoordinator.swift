#if canImport(UIKit)
import KeyboardLayout

/// Owns one repeat timer per held repeatable key.
@MainActor
final class KeyboardRepeatCoordinator {
    private let schedule: KeyRepeatController.Scheduler
    private let onRepeat: (KeySpec) -> Void
    private var controllers: [String: KeyRepeatController] = [:]

    init(
        schedule: @escaping KeyRepeatController.Scheduler,
        onRepeat: @escaping (KeySpec) -> Void
    ) {
        self.schedule = schedule
        self.onRepeat = onRepeat
    }

    func start(_ key: KeySpec) {
        guard key.role == .backspace || key.role == .space else { return }
        controllers.removeValue(forKey: key.id)?.cancel()
        let controller = KeyRepeatController(schedule: schedule) { [weak self] in
            self?.onRepeat(key)
        }
        controllers[key.id] = controller
        controller.start()
    }

    func finish(_ key: KeySpec) -> Bool {
        controllers.removeValue(forKey: key.id)?.finish() ?? false
    }

    func cancel(_ key: KeySpec) {
        controllers.removeValue(forKey: key.id)?.cancel()
    }

    func cancelAll() {
        controllers.values.forEach { $0.cancel() }
        controllers.removeAll()
    }
}
#endif
