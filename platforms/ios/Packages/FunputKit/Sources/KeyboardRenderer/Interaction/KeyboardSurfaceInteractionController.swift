#if canImport(UIKit)
import KeyboardLayout
import UIKit

@MainActor
final class KeyboardSurfaceInteractionController {
    typealias PreviewHandler = (_ key: KeySpec?, _ sourceFrame: CGRect?) -> Void

    private let haptics = KeyboardHaptics()
    private let onEvent: (KeyboardKeyEvent) -> Void
    private let onPreview: PreviewHandler
    private let repeatScheduler: BackspaceRepeatController.Scheduler
    private lazy var repeatController = BackspaceRepeatController(
        schedule: repeatScheduler
    ) { [weak self] in
        self?.repeatBackspace()
    }
    private(set) var activeKey: KeySpec?
    private var hapticsEnabled = true

    init(
        onEvent: @escaping (KeyboardKeyEvent) -> Void,
        onPreview: @escaping PreviewHandler,
        repeatScheduler: @escaping BackspaceRepeatController.Scheduler =
            BackspaceRepeatController.schedule
    ) {
        self.onEvent = onEvent
        self.onPreview = onPreview
        self.repeatScheduler = repeatScheduler
        haptics.prepare()
    }

    func handle(
        _ event: KeyboardKeyEvent,
        sourceFrame: CGRect?,
        presentation: KeyboardPresentation
    ) {
        switch event.phase {
        case .pressed:
            begin(event.key, sourceFrame: sourceFrame, presentation: presentation)
        case .released:
            finish(event.key)
        case .cancelled:
            cancel(event.key)
        case .repeated:
            break
        }
    }

    func cancelAll() {
        if let activeKey {
            onEvent(KeyboardKeyEvent(key: activeKey, phase: .cancelled))
        }
        repeatController.cancel()
        activeKey = nil
        onPreview(nil, nil)
    }

    private func begin(
        _ key: KeySpec,
        sourceFrame: CGRect?,
        presentation: KeyboardPresentation
    ) {
        if activeKey != nil {
            cancelAll()
        }
        activeKey = key
        hapticsEnabled = presentation.isHapticFeedbackEnabled
        performHaptic(for: key.role)
        if presentation.showsKeyPreviews {
            onPreview(key, sourceFrame)
        }
        if key.role == .backspace {
            repeatController.start()
        }
        onEvent(KeyboardKeyEvent(key: key, phase: .pressed))
    }

    private func finish(_ key: KeySpec) {
        guard activeKey?.id == key.id else { return }
        let suppressRelease = key.role == .backspace && repeatController.finish()
        activeKey = nil
        onPreview(nil, nil)
        if !suppressRelease {
            onEvent(KeyboardKeyEvent(key: key, phase: .released))
        }
    }

    private func cancel(_ key: KeySpec) {
        guard activeKey?.id == key.id else { return }
        repeatController.cancel()
        activeKey = nil
        onPreview(nil, nil)
        onEvent(KeyboardKeyEvent(key: key, phase: .cancelled))
    }

    private func repeatBackspace() {
        guard let key = activeKey, key.role == .backspace else { return }
        if hapticsEnabled {
            haptics.perform(.deleteRepeat)
        }
        onEvent(KeyboardKeyEvent(key: key, phase: .repeated))
    }

    private func performHaptic(for role: KeyRole) {
        guard hapticsEnabled, let type = KeyHapticTypeMapper.map(role) else { return }
        haptics.perform(type)
    }
}
#endif
