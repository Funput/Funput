/// Accessibility inputs that influence how an authored theme resolves.
///
/// Deliberately free of UIKit so `ThemeRuntime` runs on any host. The renderer
/// still reacts to *live* Reduce Transparency changes between resolves; this
/// context makes the resolved value self-consistent at the moment it is built.
public struct ThemeResolveContext: Hashable, Sendable {
    public var reduceTransparency: Bool

    public init(reduceTransparency: Bool = false) {
        self.reduceTransparency = reduceTransparency
    }

    /// A context with no accessibility overrides applied.
    public static let standard = ThemeResolveContext()
}
