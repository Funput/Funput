#if DEBUG
import Foundation
import FunputShared
import KeyboardLayout

struct ShadowTypingStep: Equatable {
    let expected: String
    let rawSequence: String
}

struct ShadowTypingFixture: Identifiable, Equatable {
    let inputMethod: KeyboardInputMethod
    let rawSteps: [String]

    var id: KeyboardInputMethod { inputMethod }
    var rawSequence: String { rawSteps.joined(separator: " ") }
    var steps: [ShadowTypingStep] {
        precondition(rawSteps.count == Self.expectedSteps.count)
        return zip(Self.expectedSteps, rawSteps).map {
            ShadowTypingStep(expected: $0, rawSequence: $1)
        }
    }

    var title: String {
        switch inputMethod {
        case .vni: "VNI"
        case .telex: "Telex"
        case .telexAdvanced: "Advanced Telex"
        }
    }

    static let expectedSteps = [
        "hôm nay trời trong xanh",
        "mình đi dạo quanh",
        "hồ nhỏ rồi ghé",
        "quán cà phê gọi",
        "một ly sữa đá",
        "ngồi ngắm dòng người",
        "qua lại",
    ]
    static let expected = expectedSteps.joined(separator: " ")

    static let all: [Self] = [
        .init(
            inputMethod: .vni,
            rawSteps: [
                "ho6m nay tro72i trong xanh",
                "mi2nh d9i da5o quanh",
                "ho62 nho3 ro62i ghe1",
                "qua1n ca2 phe6 go5i",
                "mo65t ly su74a d9a1",
                "ngo62i nga81m do2ng ngu7o72i",
                "qua la5i",
            ]
        ),
        .init(
            inputMethod: .telex,
            rawSteps: [
                "hoom nay trowfi trong xanh",
                "mifnh ddi dajo quanh",
                "hoof nhor roofi ghes",
                "quasn caf phee goji",
                "moojt ly suwxa ddas",
                "ngoofi ngawsm dofng nguwowfi",
                "qua laji",
            ]
        ),
        .init(
            inputMethod: .telexAdvanced,
            rawSteps: [
                "hoom nay tr]fi trong xanh",
                "mifnh ddi dajo quanh",
                "hoof nhor roofi ghes",
                "quasn caf phee goji",
                "moojt ly s[xa ddas",
                "ngoofi ngawsm dofng ng[]fi",
                "qua laji",
            ]
        ),
    ]

    static func configuration(for method: KeyboardInputMethod) -> FunputConfiguration {
        var value = FunputConfiguration.default
        value.inputMethod = method
        value.language = .vietnamese
        value.toneStyle = .traditional
        value.spellCheck = false
        value.smartRestore = true
        value.eagerRestore = false
        value.autoCapitalize = false
        value.showsNumberRow = false
        value.showsGlobeKey = false
        return value
    }
}

enum ShadowHarnessClassification: String, Equatable {
    case pass = "Pass"
    case legacyDivergence = "Legacy divergence captured"
    case shadowRegression = "Shadow regression"
}

struct ShadowHarnessResult: Equatable {
    let classification: ShadowHarnessClassification
    let exactMatch: Bool?
    let characterCount: Int
    let firstMismatchIndex: Int?

    static func make(
        text: String,
        report: KeyboardTouchDiagnosticReport?,
        exactMatch: Bool?
    ) -> Self {
        let unresolved = report.map {
            !$0.isSettled || $0.activeContactCount > 0
                || $0.pendingComparisonCount > 0
        } ?? true
        let shadowBad = unresolved
            || (report?.metrics.hasShadowRegression ?? true)
        let legacyBad = exactMatch == false
            || (report?.metrics.legacyMissing ?? 0) > 0
            || (report?.metrics.legacyLate ?? 0) > 0
        let classification: ShadowHarnessClassification = shadowBad
            ? .shadowRegression : (legacyBad ? .legacyDivergence : .pass)
        return Self(
            classification: classification,
            exactMatch: exactMatch,
            characterCount: text.count,
            firstMismatchIndex: exactMatch == false
                ? Self.mismatchIndex(text, ShadowTypingFixture.expected) : nil
        )
    }

    static func mismatchIndex(_ actual: String, _ expected: String) -> Int? {
        let actual = Array(actual)
        let expected = Array(expected)
        let common = zip(actual, expected).prefix { $0 == $1 }.count
        return common == max(actual.count, expected.count) ? nil : common
    }
}

extension KeyboardTouchDiagnosticReport {
    var numericJSON: String? {
        guard let data = try? JSONEncoder().encode(self) else { return nil }
        return String(data: data, encoding: .utf8)
    }
}
#endif
