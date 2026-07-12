import FunputShared
import KeyboardLayout
import KeyboardRenderer
import ThemeSchema
import Testing
@testable import Funput

@MainActor
struct FunputTests {
    @Test("App shell exposes stable top-level tabs")
    func appTabs() {
        #expect(AppTab.allCases == [.settings, .appearance, .about])
        #expect(AppTab.defaultTab == .settings)
        #expect(Set(AppTab.allCases.map(\.title)).count == AppTab.allCases.count)
        #expect(AppTab.allCases.allSatisfy { !$0.title.isEmpty && !$0.systemImage.isEmpty })
    }

    @Test("Keyboard preview uses the production presentation factory")
    func keyboardPreviewPresentation() {
        var configuration = FunputConfiguration.default
        configuration.inputMethod = .vni
        configuration.selectedThemeID = FunputConfiguration.defaultThemeID

        let presentation = KeyboardPreviewPresentation.make(configuration: configuration)
        #expect(presentation.layout.inputMethod == .vni)
        #expect(!presentation.layout.rows.isEmpty)
        #expect(presentation.theme == .funputGlass)
    }

    @Test("Version metadata includes version and build")
    func versionMetadata() {
        let label = AppMetadata.versionLabel(from: [
            "CFBundleShortVersionString": "1.2.3",
            "CFBundleVersion": "45",
        ])
        #expect(label == "Phiên bản 1.2.3 (45)")
        #expect(AppMetadata.versionLabel(from: [:]) == "Phiên bản đang phát triển")
    }
}
