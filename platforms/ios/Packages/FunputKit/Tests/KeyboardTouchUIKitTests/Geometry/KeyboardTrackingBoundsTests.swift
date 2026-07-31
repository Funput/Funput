import CoreGraphics
import KeyboardLayout
import KeyboardTouchUIKit
import Testing

/// Keys sit at y 56...248 with a toolbar at y 6...50 — the real portrait proportions.
@MainActor
struct KeyboardTrackingBoundsTests {
    @Test("Keycap slack never reaches over the toolbar")
    func toolbarKeepsItsTouches() {
        let bounds = KeyboardTrackingBounds.resolve(for: keyboard(hasToolbar: true))

        // Without the clamp this would be 44 and swallow the toolbar's bottom 6pt.
        #expect(bounds.minY == 50)
        #expect(bounds.maxY == 260)
        #expect(bounds.minX == -6)
    }

    @Test("Without a toolbar the slack stays symmetric")
    func slackIsSymmetricWithoutToolbar() {
        let bounds = KeyboardTrackingBounds.resolve(for: keyboard(hasToolbar: false))

        #expect(bounds.minY == 44)
        #expect(bounds.maxY == 260)
    }

    @Test("A tap on the toolbar's lower edge is not a keycap hit")
    func toolbarEdgeIsNotAKey() {
        let snapshot = KeyboardGeometrySnapshot(
            revision: 1,
            geometry: keyboard(hasToolbar: true)
        )

        #expect(snapshot.touchHit(at: CGPoint(x: 100, y: 46)) == nil)
        // Just below the toolbar still resolves to the nearest key.
        #expect(snapshot.touchHit(at: CGPoint(x: 100, y: 52))?.key.id == "a")
    }

    /// A degenerate layout must never produce an empty region, because that would leave the
    /// whole keyboard untouchable.
    @Test("A toolbar covering the keys falls back to the plain slack")
    func degenerateToolbarFallsBack() {
        let bounds = KeyboardTrackingBounds.resolve(
            for: ResolvedKeyboard(
                size: CGSize(width: 393, height: 254),
                toolbarFrame: CGRect(x: 0, y: 0, width: 393, height: 254),
                rows: [[ResolvedKey(
                    spec: KeySpec(id: "a", label: "a", role: .character),
                    frame: CGRect(x: 6, y: 56, width: 190, height: 192)
                )]]
            )
        )

        #expect(bounds.height > 0)
        #expect(bounds.minY == 44)
    }

    private func keyboard(hasToolbar: Bool) -> ResolvedKeyboard {
        ResolvedKeyboard(
            size: CGSize(width: 393, height: 254),
            toolbarFrame: hasToolbar
                ? CGRect(x: 6, y: 6, width: 381, height: 44) : nil,
            rows: [[
                ResolvedKey(
                    spec: KeySpec(id: "a", label: "a", role: .character),
                    frame: CGRect(x: 6, y: 56, width: 190, height: 192)
                ),
                ResolvedKey(
                    spec: KeySpec(id: "b", label: "b", role: .character),
                    frame: CGRect(x: 201, y: 56, width: 186, height: 192)
                ),
            ]]
        )
    }
}
