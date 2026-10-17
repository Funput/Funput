#if os(iOS) && canImport(FunputCore)
import FunputEngine
import FunputShared
import KeyboardLayout

@testable import KeyboardInput

/// Stands in for the engine's preview: the word on screen maps to the repairs it
/// can reach, the way a dead-ended word does and a live one does not. With eager
/// restore on, a dead-ended word is already shown as its raw keys.
@MainActor
final class FakePreviewer: CorrectionPreviewing {
    private let composer: FunputComposer
    var repairs: [String: [String]]
    var verdict: FunputCorrectionRanking.Verdict = .apply
    private(set) var rankedUses: [[UInt32]] = []
    private(set) var refusals = 0

    init(composer: FunputComposer, repairs: [String: [String]]) {
        self.composer = composer
        self.repairs = repairs
    }

    func previewCorrection() -> Int {
        repairs[composer.buffer()]?.count ?? 0
    }

    func correctionCandidates() -> [FunputCorrectionCandidateText] {
        (repairs[composer.buffer()] ?? []).map { .init(text: $0, touchScore: -1, edits: 1) }
    }

    func rankCorrection(uses: [UInt32], allowed: [Bool]) -> FunputCorrectionRanking {
        rankedUses.append(uses)
        return .init(best: 0, second: uses.count > 1 ? 1 : nil, verdict: verdict)
    }

    func refuseCorrection() { refusals += 1 }
}

/// A dictionary that knows the words it is told, remembers what it is told to keep,
/// and records every word it was asked to weigh.
@MainActor
final class WeighingDictionary: CorrectionDictionary {
    var words: Set<String> = []
    var weights: [String: UInt32] = [:]
    private(set) var kept: [String] = []
    private(set) var weighed: [String] = []

    func recognizes(_ word: String) -> Bool { words.contains(word) || kept.contains(word) }
    func keep(_ word: String) { kept.append(word) }
    func weight(_ word: String) -> UInt32 {
        weighed.append(word)
        return weights[word] ?? 0
    }
}

@MainActor
struct PreviewFixture {
    let coordinator = KeyboardInputCoordinator()
    let document = TestKeyboardWriter()
    let dictionary = WeighingDictionary()
    let previewer: FakePreviewer

    init() {
        coordinator.apply(FunputConfiguration(inputMethod: .telex, typoCorrection: true))
        coordinator.correctionDictionary = dictionary
        previewer = FakePreviewer(composer: coordinator.composer, repairs: ["dduwowfnh": ["đường"]])
        coordinator.correctionPreviewer = previewer
        dictionary.weights["đường"] = 7
    }

    /// Type `keys`, reporting the key at `slip` as having drifted towards `g`.
    func type(_ keys: String, slip: Int? = 8) {
        for (index, character) in keys.enumerated() {
            let key = character == " "
                ? testKey(.space)
                : testKey(.character, label: String(character))
            var touch: KeyboardTouchEvidence?
            if let scalar = String(character).unicodeScalars.first, character != " " {
                touch = index == slip
                    ? KeyboardTouchEvidence(
                        typed: scalar, typedDistance: 0.35, first: .init(scalar: "g", distance: 0.15)
                    )
                    : KeyboardTouchEvidence(typed: scalar, typedDistance: 0.35)
            }
            coordinator.handle(key, touch: touch, writer: document)
        }
    }
}
#endif
