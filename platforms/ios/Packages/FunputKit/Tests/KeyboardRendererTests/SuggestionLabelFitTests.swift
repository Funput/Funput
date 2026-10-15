#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

@Suite("Suggestion label fit")
struct SuggestionLabelFitTests {
    @Test("A word that fits keeps its size")
    func fits() {
        #expect(SuggestionLabelFit.sizeFactor(textWidth: 80, availableWidth: 100) == 1)
    }

    @Test("A slightly long word only gets as small as it needs")
    func shrinks() {
        let factor = SuggestionLabelFit.sizeFactor(textWidth: 110, availableWidth: 100)
        #expect(abs(factor - 100 / 110) < 0.0001)
    }

    @Test("A long word stops shrinking at the floor and is left to the narrower widths")
    func floors() {
        let factor = SuggestionLabelFit.sizeFactor(textWidth: 400, availableWidth: 100)
        #expect(factor == SuggestionLabelFit.minimumSizeFactor)
    }

    @Test("A slot with no room leaves the text alone rather than dividing by zero")
    func noRoom() {
        #expect(SuggestionLabelFit.sizeFactor(textWidth: 50, availableWidth: 0) == 1)
    }

    @MainActor
    @Test("The bar shows a long candidate whole and inside its slot")
    func longCandidateInTheBar() throws {
        let height = KeyboardSizingProfile.default.toolbarHeight
        let toolbar = KeyboardToolbarView(frame: CGRect(x: 0, y: 0, width: 360, height: height))
        toolbar.apply(spec: .standard, theme: .funputGlass, traits: .init())
        let words = ["Automati", "Automatic", "Automatically"]
        toolbar.updateSuggestions(words.map { KeyboardSuggestionCandidate(text: $0, generation: 1) })
        toolbar.layoutIfNeeded()

        let bar = toolbar.suggestionBar
        let button = try #require(
            bar.subviews.compactMap { $0 as? UIButton }
                .first { $0.accessibilityLabel == "Gợi ý, Automatically" }
        )
        let title = try #require(button.attributedTitle(for: .normal))
        #expect(title.string == "Automatically")
        let available = button.bounds.width - KeyboardSuggestionBarView.titleHorizontalInset * 2
        #expect(title.size().width <= available + 0.5)
    }
}
#endif
