#if canImport(UIKit)
import KeyboardLayout
import UIKit

/// Release-build unit-test seam. SPI keeps touch internals out of the normal
/// KeyboardRenderer API while allowing the app's native test target to drive
/// the exact production controller.
@_spi(Testing)
@MainActor
public final class KeyboardTouchTestDriver {
    private final class EventBox {
        var values: [KeyboardKeyEvent] = []
        var alternateCenters: [CGPoint] = []
    }

    @MainActor
    private final class CancellationBox {
        var isCancelled = false
    }

    @MainActor
    private final class ManualRepeatScheduler {
        private var pending: [(CancellationBox, @MainActor @Sendable () -> Void)] = []

        func schedule(
            _ delay: TimeInterval,
            _ action: @escaping @MainActor @Sendable () -> Void
        ) -> KeyboardScheduledAction {
            _ = delay
            let cancellation = CancellationBox()
            pending.append((cancellation, action))
            return KeyboardScheduledAction { cancellation.isCancelled = true }
        }

        func runNext() {
            guard !pending.isEmpty else { return }
            let (cancellation, action) = pending.removeFirst()
            if !cancellation.isCancelled { action() }
        }
    }

    private let box: EventBox
    private let repeatScheduler: ManualRepeatScheduler
    private let controller: KeyboardSurfaceInteractionController
    private let presentation: KeyboardPresentation

    public var events: [KeyboardKeyEvent] { box.values }
    public var queueDepth: Int { controller.queueDepth }

    public init() {
        let box = EventBox()
        let repeatScheduler = ManualRepeatScheduler()
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        presentation.isKeySoundEnabled = false
        presentation.showsKeyPreviews = false
        self.box = box
        self.repeatScheduler = repeatScheduler
        self.presentation = presentation
        controller = KeyboardSurfaceInteractionController(
            onEvent: { box.values.append($0) },
            onPreview: { _, _ in },
            onAlternatePreview: { _, layout, _ in
                guard let layout else {
                    box.alternateCenters = []
                    return
                }
                box.alternateCenters = layout.itemFrames.map {
                    CGPoint(
                        x: $0.midX + layout.frame.minX,
                        y: $0.midY + layout.frame.minY
                    )
                }
            },
            repeatScheduler: { delay, action in
                repeatScheduler.schedule(delay, action)
            }
        )
    }

    public func begin(
        token: UInt64,
        key: KeySpec,
        point: CGPoint = .zero,
        sourceFrame: CGRect? = nil,
        containerBounds: CGRect = .zero
    ) {
        controller.beginTouch(
            token: token,
            key: key,
            point: point,
            sourceFrame: sourceFrame,
            containerBounds: containerBounds,
            presentation: presentation
        )
    }

    public func move(token: UInt64, key: KeySpec?, point: CGPoint) {
        controller.moveTouch(
            token: token,
            key: key,
            point: point,
            sourceFrame: nil,
            presentation: presentation
        )
    }

    public func end(token: UInt64) {
        controller.endTouch(token: token)
    }

    public func runNextRepeat() {
        repeatScheduler.runNext()
    }

    public func alternateCenter(at index: Int) -> CGPoint? {
        box.alternateCenters.indices.contains(index) ? box.alternateCenters[index] : nil
    }

    public func reconcile(activeTokens: Set<UInt64>) {
        controller.reconcileActiveTouches(activeTokens)
    }

    public func performAccessibilitySwipe(key: KeySpec, action: KeySwipeAction) {
        controller.handle(
            .init(key: key, phase: .swiped(action)),
            sourceFrame: nil,
            presentation: presentation
        )
    }

    public static func hitKey(
        in geometry: ResolvedKeyboard,
        at point: CGPoint
    ) -> KeySpec? {
        let overlay = KeyboardTouchOverlayView()
        overlay.updateGeometry(geometry)
        return overlay.resolvedHit(at: point)?.key
    }
}
#endif
