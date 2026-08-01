#if DEBUG
import Foundation
import FunputShared

enum KeyboardTouchAcceptanceClassification: String, Equatable {
    case pass = "Pass"
    case typingMismatch = "Typing mismatch"
    case incompleteCoverage = "Incomplete coverage"
    case pipelineRegression = "Pipeline regression"
}

struct KeyboardTouchAcceptanceResult: Equatable {
    let classification: KeyboardTouchAcceptanceClassification
    let exactMatch: Bool?
    let characterCount: Int
    let firstMismatchIndex: Int?

    static func make(
        text: String,
        report: KeyboardTouchDiagnosticReport?,
        exactMatch: Bool?,
        requiresGestureCoverage: Bool = false
    ) -> Self {
        let unresolved = report.map {
            !$0.isSettled || $0.activeContactCount > 0
                || $0.pendingContactCount > 0
        } ?? true
        let metrics = report?.metrics
        let incomplete = requiresGestureCoverage && !(
            (metrics?.alternateCommitted ?? 0) >= 1
                && (metrics?.repeatEmitted ?? 0) >= 2
                && (metrics?.swipeCommitted ?? 0) >= 2
                && (metrics?.controlCommitted ?? 0) >= 4
        )
        let classification = classify(
            unresolved: unresolved,
            regression: metrics?.hasRegression ?? true,
            exactMatch: exactMatch,
            incomplete: incomplete
        )
        return Self(
            classification: classification,
            exactMatch: exactMatch,
            characterCount: text.count,
            firstMismatchIndex: exactMatch == false
                ? mismatchIndex(text, KeyboardTouchAcceptanceFixture.expected) : nil
        )
    }

    static func mismatchIndex(_ actual: String, _ expected: String) -> Int? {
        let actual = Array(actual)
        let expected = Array(expected)
        let common = zip(actual, expected).prefix { $0 == $1 }.count
        return common == max(actual.count, expected.count) ? nil : common
    }

    func reclassified(_ value: KeyboardTouchAcceptanceClassification) -> Self {
        Self(
            classification: value,
            exactMatch: exactMatch,
            characterCount: characterCount,
            firstMismatchIndex: firstMismatchIndex
        )
    }

    private static func classify(
        unresolved: Bool,
        regression: Bool,
        exactMatch: Bool?,
        incomplete: Bool
    ) -> KeyboardTouchAcceptanceClassification {
        if unresolved || regression { return .pipelineRegression }
        if exactMatch == false { return .typingMismatch }
        return incomplete ? .incompleteCoverage : .pass
    }
}

extension KeyboardTouchDiagnosticReport {
    var numericJSON: String? {
        guard let data = try? JSONEncoder().encode(self) else { return nil }
        return String(data: data, encoding: .utf8)
    }
}
#endif
