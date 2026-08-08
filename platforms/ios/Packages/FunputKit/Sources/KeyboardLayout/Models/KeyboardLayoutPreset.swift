/// Which arrangement of keys the keyboard presents.
///
/// A preset decides *which keys exist and in what order* — nothing else. Colours,
/// corner radii, telex hints, the suggestion toolbar, and the swipe-to-switch-language
/// gesture on space are all preset-independent, so a user can pair either preset with
/// any theme. That separation is why this is not a theme: themes are authorable JSON,
/// and letting them describe a layout would make every custom theme a keyboard the
/// validator has to prove sane.
public enum KeyboardLayoutPreset: String, CaseIterable, Hashable, Sendable, Codable {
    /// Funput's own arrangement, with comma and period on the letters page.
    case funput

    /// Mirrors the key order of Apple's stock Vietnamese keyboard, for people who
    /// switched to Funput and kept reaching for keys where iOS puts them.
    case system
}
