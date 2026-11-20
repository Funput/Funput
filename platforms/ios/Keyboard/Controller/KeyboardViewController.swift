//
//  KeyboardViewController.swift
//  Keyboard
//
//  Created by P-Code Dynamics on 11/7/26.
//

import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import PersonalSuggestions
import ThemeRuntime
import ThemeSchema
import UIKit

/// One host app's keyboard. iOS builds a new one for every host inside the same
/// process, so this holds only what belongs to one presentation and one document;
/// the surface and everything else worth keeping live in `KeyboardRuntime`.
final class KeyboardViewController: UIInputViewController {
    let launchTrace = KeyboardLaunchTrace()
    let censusEnrollment = KeyboardLifetimeCensus.Enrollment(.controller)
    let inputCoordinator = KeyboardInputCoordinator()
    let shortcutsLoader = KeyboardShortcutsLoader(store: KeyboardShortcutSource())
    var emojiView: EmojiKeyboardView?
    var kaomojiView: KaomojiKeyboardView?
    var clipboardPanelView: ClipboardKeyboardView?
    let emojiCatalogCache = EmojiCatalogCache()
    /// The word list typo correction weighs candidates against. Separate from the
    /// store below, and read-only, so the correction handshake can ask it on the
    /// keystroke path without waiting on that store's queue.
    let correctionWords = CorrectionWordSource()
    let personalSuggestionService = PersonalSuggestionService {
        PersonalSuggestionWorker(onResult: $0)
    }
    let activationState = KeyboardActivationState()
    var clipboardRetryTask: Task<Void, Never>?
    let clipboardChangeMonitor = ClipboardChangeMonitor()
    var clipboardStartTask: Task<Void, Never>?
    var isClipboardMonitoring = false
    lazy var clipboardRecorder = makeClipboardRecorder()
    var pendingBootstrapRepair: KeyboardBootstrapSnapshot?
    var bootstrappedSource: KeyboardBootstrappedSource?
    var displayedSurface = KeyboardSurface.funput
    /// This controller's own view of the keyboard: `textWillChange` works it out for
    /// the field about to be typed in before the controller holds the surface.
    var currentPresentation = KeyboardPresentation()
#if DEBUG
    /// Made when the surface is bound to this controller, and gone when it leaves.
    var touchDiagnosticsReporter: KeyboardTouchDiagnosticsReporter?
#endif
    var resolvedTextInputTraits = KeyboardInputContext(
        editorMode: .text,
        enterAction: .newLine,
        initialLayoutMode: .letters
    )
    let heightController = KeyboardHeightController()
    private(set) var isKeyboardVisible = false

    override func loadView() {
        inputView = KeyboardAudioInputView(frame: .zero, inputViewStyle: .keyboard)
    }

    override func viewDidLoad() {
        launchTrace.beginViewDidLoad()
        super.viewDidLoad()
        view.isOpaque = false
        view.backgroundColor = .clear
        inputView?.allowsSelfSizing = true
        heightController.install(on: view)
        // The height only tracks the size class, and `viewWillTransition` alone misses
        // every change that is not a rotation of an already-visible keyboard.
        if #available(iOS 17, *) {
            registerForTraitChanges([UITraitVerticalSizeClass.self]) {
                (controller: KeyboardViewController, _) in
                controller.updatePreferredHeight()
            }
        }
        observeHostBackgrounding()
        bootstrapKeyboard()
        launchTrace.endViewDidLoad()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        launchTrace.finish()
        // Normally a no-op. It is here for a controller that iOS shows again without
        // `viewWillAppear`, after another one took the surface.
        if !isSurfaceOwner { applyAdoptedPresentation(claimingAt: .appearing) }
        releaseSystemGestureTouchDelay()
        accessStateStore.record(hasFullAccess: hasFullAccess)
#if DEBUG
        touchDiagnosticsReporter?.startIfAvailable(hasFullAccess: hasFullAccess)
#endif
        repairBootstrapSnapshotIfNeeded()
        scheduleClipboardMonitoringStart()
        ownedSurface?.prepareFeedback()
        KeyboardMemoryTrace.recordAppearance()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        shortcutsLoader.cancel()
        // The next activation starts on the letters anyway; an open panel would only
        // hold memory while the keyboard is off screen.
        switchSurface(to: .funput)
        emojiCatalogCache.release()
        ownedSurface?.releaseFeedback()
        detachPrimarySurface()
        KeyboardMemoryTrace.record("Disappeared")
#if DEBUG
        touchDiagnosticsReporter?.finish()
#endif
    }

    func markPreferredHeightHidden() {
        isKeyboardVisible = false
    }

    func markPreferredHeightVisible() {
        isKeyboardVisible = true
    }
}
