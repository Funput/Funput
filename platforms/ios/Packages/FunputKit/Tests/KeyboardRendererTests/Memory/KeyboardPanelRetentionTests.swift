import KeyboardRenderer
import Testing

@Suite("Keyboard panel retention")
struct KeyboardPanelRetentionTests {
    @Test("The letters keep no panel")
    func letters() {
        #expect(KeyboardPanelRetention.retained(whileShowing: nil).isEmpty)
    }

    @Test("Emoji and kaomoji keep each other for the way back")
    func browsingPair() {
        for panel in [KeyboardPanel.emoji, .kaomoji] {
            #expect(KeyboardPanelRetention.retained(whileShowing: panel) == [.emoji, .kaomoji])
        }
    }

    @Test("The clipboard keeps only itself")
    func clipboard() {
        #expect(KeyboardPanelRetention.retained(whileShowing: .clipboard) == [.clipboard])
    }

    @Test("Every shown panel is retained")
    func shownIsKept() {
        for panel in KeyboardPanel.allCases {
            #expect(KeyboardPanelRetention.retained(whileShowing: panel).contains(panel))
        }
    }
}
