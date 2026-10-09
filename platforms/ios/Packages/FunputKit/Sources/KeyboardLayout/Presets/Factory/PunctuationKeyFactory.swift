/// The one definition of a text period key. Decimal keypads build their own separator,
/// because a symbol palette there would only get in the way of typing a number.
func periodKey(_ id: String) -> KeySpec {
    KeySpec(
        id: id,
        label: ".",
        role: .punctuation,
        accessibilityLabel: "Dấu chấm",
        alternates: PunctuationKeyAlternates.period,
        alternateColumns: PunctuationKeyAlternates.periodColumns
    )
}
