#if canImport(UIKit)
import KeyboardLayout
import UIKit

/// Turns raw key touch phases into input events. Supports rollover: several keys
/// can be held at once while typing fast, each committing on its own release.
@MainActor
final class KeyboardSurfaceInteractionController {
    typealias PreviewHandler = (_ key: KeySpec?, _ sourceFrame: CGRect?) -> Void

    private let haptics: KeyboardHaptics
    private let onEvent: (KeyboardKeyEvent) -> Void
    private let onPreview: PreviewHandler
    private let repeatScheduler: BackspaceRepeatController.Scheduler
    private lazy var repeatController = BackspaceRepeatController(
        schedule: repeatScheduler
    ) { [weak self] in
        self?.repeatBackspace()
    }
    private var commitQueue = KeyboardPressCommitQueue()
    private var backspaceKeyID: String?
    private var previewKeyID: String?
    private var hapticsEnabled = true
    var activeKey: KeySpec? { commitQueue.activeKey }

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
        commitQueue.cancelAll()
        clearBackspaceRepeat()
        clearPreview()
        flushCompletedKeys()
    }

    private func begin(
        _ key: KeySpec,
        sourceFrame: CGRect?,
        presentation: KeyboardPresentation
    ) {
        commitQueue.append(key)
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
        guard commitQueue.hasPendingKey(id: key.id) else { return }
        let wasRepeating = key.role == .backspace && repeatController.finish()
        if key.role == .backspace { backspaceKeyID = nil }
        commitQueue.complete(id: key.id, as: wasRepeating ? .suppressed : .released)
        if key.id == previewKeyID { clearPreview() }
        flushCompletedKeys()
    }

    private func cancel(_ key: KeySpec) {
        guard commitQueue.complete(id: key.id, as: .cancelled) else { return }
        if key.role == .backspace { clearBackspaceRepeat() }
        if key.id == previewKeyID { clearPreview() }
        flushCompletedKeys()
    }

    private func handleSwipe(_ event: KeyboardKeyEvent) {
        if commitQueue.hasPendingKey(id: event.key.id) {
            guard case let .swiped(action) = event.phase else { return }
            commitQueue.complete(id: event.key.id, as: .swiped(action))
            if event.key.id == backspaceKeyID { clearBackspaceRepeat() }
            if event.key.id == previewKeyID { clearPreview() }
        } else if !commitQueue.isEmpty {
            return
        } else {
            // Accessibility can invoke the action without a preceding touch.
            if hapticsEnabled { haptics.perform(.control) }
            onEvent(event)
            return
        }
        if hapticsEnabled { haptics.perform(.control) }
        flushCompletedKeys()
    }

    private func repeatBackspace() {
        guard let id = backspaceKeyID,
              commitQueue.bufferRepeat(for: id) else { return }
        if hapticsEnabled { haptics.perform(.deleteRepeat) }
        flushCompletedKeys()
    }

    private func flushCompletedKeys() {
        while let action = commitQueue.popReadyAction() {
            switch action {
            case let .event(event):
                onEvent(event)
            case .suppressed:
                continue
            }
        }
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
