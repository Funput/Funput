public extension FunputConfiguration {
    /// Safe defaults used before the user changes anything, and the value
    /// returned when stored configuration is missing or unreadable. Matches the
    /// keyboard's historical hardcoded behavior (VNI, Vietnamese, glass theme).
    static let `default` = FunputConfiguration()
}
