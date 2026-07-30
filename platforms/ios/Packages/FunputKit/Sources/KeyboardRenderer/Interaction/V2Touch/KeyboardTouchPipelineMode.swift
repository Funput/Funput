public enum KeyboardTouchPipelineMode: String, Equatable, Sendable {
    case legacy
    case v2

    @available(*, deprecated, renamed: "v2")
    public static var primaryFastTap: Self { .v2 }
}
