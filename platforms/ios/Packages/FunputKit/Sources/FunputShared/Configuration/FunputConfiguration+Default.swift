public extension FunputConfiguration {
    /// Safe defaults used before the user changes anything, and the value
    /// returned when stored configuration is missing or unreadable.
    ///
    /// The engine flags (tone style, spell check, smart/eager restore, auto
    /// capitalize) deliberately mirror `funput-engine` `Session::new()`, so an
    /// unconfigured install types identically to before configuration existed.
    /// iOS overrides only the input method to VNI, as it always has. Keep these
    /// in sync with the Rust defaults.
    static let `default` = FunputConfiguration()
}
