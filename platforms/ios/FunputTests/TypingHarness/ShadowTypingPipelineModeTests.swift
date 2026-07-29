#if DEBUG
import Foundation
import FunputShared
import Testing
@testable import Funput

@MainActor
struct ShadowTypingPipelineModeTests {
    @Test("Harness defaults to primary and writes mode before focus")
    func primarySession() throws {
        let name = "test.touch-pipeline-mode.\(UUID())"
        let defaults = UserDefaults(suiteName: name)!
        defer { defaults.removePersistentDomain(forName: name) }
        let diagnostics = KeyboardTouchDiagnosticStore(defaults: defaults)
        let model = ShadowTypingHarnessModel(
            diagnosticStore: diagnostics,
            overrideStore: .init(defaults: defaults),
            accessCheck: { true }
        )

        #expect(model.selectedPipeline == .primaryFastTap)
        model.startGuided()
        let session = try #require(diagnostics.activeSession())
        #expect(session.pipelineMode == .primaryFastTap)
    }
}
#endif
