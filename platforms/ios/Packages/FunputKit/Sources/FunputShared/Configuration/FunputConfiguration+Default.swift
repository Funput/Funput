public extension FunputConfiguration {
    /// Safe defaults used before the user changes anything, and the value
    /// returned when stored configuration is missing or unreadable.
    ///
    /// The engine flags (tone style, spell check, smart/eager restore) deliberately
    /// mirror `funput-engine` `Session::new()`, so an unconfigured install types
    /// identically to before configuration existed. Keep those in sync with the Rust
    /// defaults.
    ///
    /// Two are iOS overrides rather than mirrors: the input method is VNI, as it
    /// always has been, and auto-capitalize is on. A software keyboard is the only
    /// platform where Funput draws the keys, so it is also the only one where the
    /// user can see the Shift state follow the sentence — leaving it off there made
    /// Funput the one keyboard on the phone that does not capitalize. The desktop
    /// shells have no such affordance and keep the Rust default.
    static let `default` = FunputConfiguration()
}
