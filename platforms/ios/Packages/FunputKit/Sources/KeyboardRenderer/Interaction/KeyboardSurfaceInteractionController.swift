#if canImport(UIKit)
import KeyboardLayout
import os
import UIKit

/// Converts tracked touches into semantic keyboard events while preserving
/// touch-down ordering. Raw touches are identified independently from keys so a
/// finger can move between keycaps before it commits.
@MainActor
final class KeyboardSurfaceInteractionController {
    typealias TouchToken = KeyboardPressCommitQueue.TouchToken
    typealias PreviewHandler = (_ key: KeySpec?, _ sourceFrame: CGRect?) -> Void
    typealias HighlightHandler = (_ key: KeySpec, _ highlighted: Bool) -> Void

    /// Why a tracked touch is ending without a release.
    enum Cancellation {
        /// UIKit took the touch away, or stopped reporting it. The user did press.
        case system
        /// The user lifted or dragged off on purpose.
        case userIntent
    }

    struct TouchState {
        let initialKey: KeySpec
        let startPoint: CGPoint
        var currentKey: KeySpec?
        var currentFrame: CGRect?
        var swipeTracker = KeySwipeGestureTracker()
        /// Set once the finger travels past `tapSlop`, which separates a tap from a
        /// gesture that merely started on a keycap.
        var hasWandered = false
        let signpostID: OSSignpostID
    }

    /// Half the swipe threshold: past this a touch is no longer a tap.
    static let tapSlop: CGFloat = 16

    let haptics: KeyboardHaptics
    let onEvent: (KeyboardKeyEvent) -> Void
    let onPreview: PreviewHandler
    let onHighlight: HighlightHandler
    let repeatScheduler: BackspaceRepeatController.Scheduler
    lazy var repeatController = BackspaceRepeatController(
        schedule: repeatScheduler
    ) { [weak self] in
        self?.repeatActiveKey()
    }
    var commitQueue = KeyboardPressCommitQueue()
    var touches: [TouchToken: TouchState] = [:]
    var highlightCounts: [String: Int] = [:]
    var repeatTouch: TouchToken?
    var hapticsEnabled = true
    var legacyTokensByKeyID: [String: [TouchToken]] = [:]
    /// Toolbar and accessibility presses are minted from here up, which keeps them
    /// distinguishable from the overlay's tokens without a second bookkeeping map.
    static let firstLegacyToken: TouchToken = 1 << 63
    var nextLegacyToken: TouchToken = KeyboardSurfaceInteractionController.firstLegacyToken

    var activeKey: KeySpec? { commitQueue.activeKey }
    var queueDepth: Int { commitQueue.depth }

    init(
        feedbackView: UIView = UIView(),
        onEvent: @escaping (KeyboardKeyEvent) -> Void,
        onPreview: @escaping PreviewHandler,
        onHighlight: @escaping HighlightHandler = { _, _ in },
        repeatScheduler: @escaping BackspaceRepeatController.Scheduler =
            BackspaceRepeatController.schedule
    ) {
        haptics = KeyboardHaptics(view: feedbackView)
        self.onEvent = onEvent
        self.onPreview = onPreview
        self.onHighlight = onHighlight
        self.repeatScheduler = repeatScheduler
        touches.reserveCapacity(10)
        haptics.prepare()
    }

}

enum KeyboardTouchSignpost {
    static let log = OSLog(subsystem: "app.funput.keyboard", category: "Touch")
}
#endif
