#if os(iOS) && canImport(FunputCore)
extension CorrectionWordSource {
    /// How many words are counted before the rarest is dropped to make room.
    public static let countLimit = 2_000

    /// How many sightings make a word the user's own. One is what a typo left alone
    /// looks like — an ambiguous pair, a word kept as typed — and recognizing it then
    /// would silence the very preview that exists for it. Two is a word typed on
    /// purpose.
    static let familiarCount: UInt32 = 2

    /// Count a word the user finished typing, so it weighs more against a repair
    /// next time, and counts as theirs once it has been seen again.
    public func learn(_ word: String) {
        let key = word.lowercased()
        guard !key.isEmpty else { return }
        personalCounts[key, default: 0] &+= 1
        if personalCounts.count > Self.countLimit,
           let rarest = personalCounts.min(by: { $0.value < $1.value }) {
            personalCounts.removeValue(forKey: rarest.key)
        }
    }

    /// How likely `word` is: how common a syllable it is in the shipped list, plus
    /// how often this user has typed it this session.
    public func weight(_ word: String) -> UInt32 {
        PersonalSuggestionEngine.syllableWeight(word) &+ (personalCounts[word.lowercased()] ?? 0)
    }

    /// Whether the user has typed `word` often enough for it to be a word of theirs.
    public func isFamiliar(_ word: String) -> Bool {
        (personalCounts[word.lowercased()] ?? 0) >= Self.familiarCount
    }
}
#endif
