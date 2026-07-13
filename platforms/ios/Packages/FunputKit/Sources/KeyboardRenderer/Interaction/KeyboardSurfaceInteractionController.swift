#if canImport(UIKit)
import KeyboardLayout
import UIKit

/// Turns raw key touch phases into input events. Supports rollover: several keys
/// can be held at once while typing fast, each committing on its own release.
@MainActor
final class KeyboardSurfaceInteractionController {
    typealias PreviewHandler = (_ key: KeySpec?, _ sourceFrame: CGRect?) -> Void

    private struct HeldKey {
        let spec: KeySpec
        var didSwipe = false
    }

    private let haptics: KeyboardHaptics
    private let onEvent: (KeyboardKeyEvent) -> Void
    private let onPreview: PreviewHandler
    private let repeatScheduler: BackspaceRepeatController.Scheduler
    private lazy var repeatController = BackspaceRepeatController(
        schedule: repeatScheduler
    ) { [weak self] in
        self?.repeatBackspace()
    }
    private var heldKeys: [HeldKey] = []  // press order; newest drives the preview
    private var backspaceKeyID: String?
    private var previewKeyID: String?
    private var hapticsEnabled = true
    var activeKey: KeySpec? { heldKeys.last?.spec }

    init(
        feedbackView: UIView = UIView(),
        onEvent: @escaping (KeyboardKeyEvent) -> Void,
        onPreview: @escaping PreviewHandler,
        repeatScheduler: @escaping BackspaceRepeatController.Scheduler =
            BackspaceRepeatController.schedule
    ) {
        haptics = KeyboardHaptics(view: feedbackView)
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
        hapticsEnabled = presentation.isHapticFeedbackEnabled
        switch event.phase {
        case .pressed:
            begin(event.key, sourceFrame: sourceFrame, presentation: presentation)
        case .released:
            finish(event.key)
        case .cancelled:
            cancel(event.key)
        case .repeated:
            break
        case .swiped:
            handleSwipe(event)
        }
    }

    func cancelAll() {
        let released = heldKeys
        heldKeys.removeAll()
        clearBackspaceRepeat()
        clearPreview()
        for held in released {
            onEvent(KeyboardKeyEvent(key: held.spec, phase: .cancelled))
        }
    }

    private func begin(
        _ key: KeySpec,
        sourceFrame: CGRect?,
        presentation: KeyboardPresentation
    ) {
        heldKeys.append(HeldKey(spec: key))
        if hapticsEnabled, let type = KeyHapticTypeMapper.map(key.role) {
            haptics.perform(type)
        }
        if presentation.isKeySoundEnabled { UIDevice.current.playInputClick() }
        if presentation.showsKeyPreviews {
            previewKeyID = key.id
            onPreview(key, sourceFrame)
        }
        if key.role == .backspace {
            backspaceKeyID = key.id
            repeatController.start()
        }
        onEvent(KeyboardKeyEvent(key: key, phase: .pressed))
    }

    private func finish(_ key: KeySpec) {
        guard let held = release(key.id) else { return }
        let wasRepeating = key.role == .backspace && repeatController.finish()
        if key.role == .backspace { backspaceKeyID = nil }
        if !(held.didSwipe || wasRepeating) {
            onEvent(KeyboardKeyEvent(key: key, phase: .released))
        }
    }

    private func cancel(_ key: KeySpec) {
        guard release(key.id) != nil else { return }
        if key.role == .backspace { clearBackspaceRepeat() }
        onEvent(KeyboardKeyEvent(key: key, phase: .cancelled))
    }

    private func handleSwipe(_ event: KeyboardKeyEvent) {
        if let index = heldKeys.firstIndex(where: { $0.spec.id == event.key.id }) {
            heldKeys[index].didSwipe = true
            if event.key.id == backspaceKeyID { clearBackspaceRepeat() }
            if event.key.id == previewKeyID { clearPreview() }
        } else if !heldKeys.isEmpty {
            return
        }
        if hapticsEnabled { haptics.perform(.control) }
        onEvent(event)
    }

    private func repeatBackspace() {
        guard let id = backspaceKeyID,
              let held = heldKeys.first(where: { $0.spec.id == id }) else { return }
        if hapticsEnabled { haptics.perform(.deleteRepeat) }
        onEvent(KeyboardKeyEvent(key: held.spec, phase: .repeated))
    }

    @discardableResult
    private func release(_ id: String) -> HeldKey? {
        guard let index = heldKeys.firstIndex(where: { $0.spec.id == id }) else {
            return nil
        }
        let held = heldKeys.remove(at: index)
        if id == previewKeyID { clearPreview() }
        return held
    }

    private func clearBackspaceRepeat() {
        repeatController.cancel()
        backspaceKeyID = nil
    }

    private func clearPreview() {
        previewKeyID = nil
        onPreview(nil, nil)
    }
}
#endif
