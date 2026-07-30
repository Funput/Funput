#if DEBUG
import Foundation
import FunputShared
import Testing

struct KeyboardTouchDiagnosticPrivacyTests {
    @Test("Encoded report has no text or key identity fields")
    func reportSchemaIsNumericAndMetadataOnly() throws {
        let session = KeyboardTouchDiagnosticSession(
            inputMethod: .telexAdvanced,
            phase: .free,
            generation: 9,
            startedAt: Date(timeIntervalSince1970: 20)
        )
        var metrics = KeyboardTouchDiagnosticMetrics()
        metrics.capturedContacts = 42
        let report = KeyboardTouchDiagnosticReport(
            sessionID: session.id,
            generation: session.generation,
            sequence: 2,
            observedAt: session.startedAt,
            metrics: metrics,
            activeContactCount: 1,
            pendingContactCount: 2,
            isSettled: false,
            device: .init(
                model: "iPhone",
                operatingSystem: "iOS",
                maximumFramesPerSecond: 120
            )
        )

        let object = try #require(
            JSONSerialization.jsonObject(with: JSONEncoder().encode(report))
                as? [String: Any]
        )
        let forbidden = ["text", "expected", "actual", "key", "label", "document", "composition"]
        let encodedKeys = recursiveKeys(in: object).map { $0.lowercased() }
        #expect(forbidden.allSatisfy { word in
            encodedKeys.allSatisfy { !$0.contains(word) }
        })
    }

    private func recursiveKeys(in value: Any) -> [String] {
        if let dictionary = value as? [String: Any] {
            return dictionary.keys + dictionary.values.flatMap(recursiveKeys)
        }
        if let array = value as? [Any] {
            return array.flatMap(recursiveKeys)
        }
        return []
    }
}
#endif
