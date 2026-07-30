#if canImport(UIKit)
import KeyboardLayout
import os
import UIKit

/// Converts tracked touches into semantic keyboard events while preserving
/// touch-down ordering. Raw touches are identified independently from keys so a
/// finger can move between keycaps before it commits.
@MainActor
final class KeyboardSurfaceInteractionController {
    typealias TouchToken = UInt64
    typealias PreviewHandler = (_ key: KeySpec?, _ sourceFrame: CGRect?) -> Void
    typealias AlternatePreviewHandler = (
        _ key: KeySpec?,
        _ layout: KeyboardAlternatePaletteLayout?,
        _ selectedIndex: Int?
    ) -> Void
    typealias HighlightHandler = (_ key: KeySpec, _ highlighted: Bool) -> Void
    typealias ContactEventHandler = (
        _ token: TouchToken, _ event: KeyboardKeyEvent
    ) -> Void
    enum GestureClaim: Equatable {
        case alternate
        case repeatKey
        case swipe
    }

    struct TouchState {
        let initialKey: KeySpec
        let startPoint: CGPoint
        var currentKey: KeySpec?
        var currentFrame: CGRect?
        let containerBounds: CGRect
        var alternateLayout: KeyboardAlternatePaletteLayout?
        var selectedAlternateIndex: Int?
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
    let onContactEvent: ContactEventHandler
    let onClaimGesture: (TouchToken, GestureClaim) -> Void
    let onPreview: PreviewHandler
    let onAlternatePreview: AlternatePreviewHandler
    let onHighlight: HighlightHandler
    let repeatScheduler: BackspaceRepeatController.Scheduler
    lazy var repeatController = BackspaceRepeatController(
        schedule: repeatScheduler
    ) { [weak self] in
        self?.repeatActiveKey()
    }
    lazy var alternateHoldController = AlternateHoldController(
        schedule: repeatScheduler
    ) { [weak self] token in
        self?.activateAlternates(for: token)
    }
    var touches: [TouchToken: TouchState] = [:]
    var highlightCounts: [String: Int] = [:]
    var repeatTouch: TouchToken?
    var hapticsEnabled = true
    var activeKey: KeySpec? {
        repeatTouch.flatMap { touches[$0]?.currentKey }
    }
    var queueDepth: Int { touches.count }

    init(
        feedbackView: UIView = UIView(),
        onEvent: @escaping (KeyboardKeyEvent) -> Void,
        onContactEvent: ContactEventHandler? = nil,
        onClaimGesture: @escaping (TouchToken, GestureClaim) -> Void = { _, _ in },
        onPreview: @escaping PreviewHandler,
        onAlternatePreview: @escaping AlternatePreviewHandler = { _, _, _ in },
        onHighlight: @escaping HighlightHandler = { _, _ in },
        repeatScheduler: @escaping BackspaceRepeatController.Scheduler =
            BackspaceRepeatController.schedule
    ) {
        haptics = KeyboardHaptics(view: feedbackView)
        self.onEvent = onEvent
        self.onContactEvent = onContactEvent ?? { _, event in onEvent(event) }
        self.onClaimGesture = onClaimGesture
        self.onPreview = onPreview
        self.onAlternatePreview = onAlternatePreview
        self.onHighlight = onHighlight
        self.repeatScheduler = repeatScheduler
        touches.reserveCapacity(10)
        haptics.prepare()
    }

    var activeTouchCount: Int { touches.count }
}

enum KeyboardTouchSignpost {
    static let log = OSLog(subsystem: "app.funput.keyboard", category: "Touch")
}
#endif
