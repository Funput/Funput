/// The Codable, persistable authoring form of a theme.
///
/// `KeyboardTheme` is what ships in bundled catalogs, user packages and the App
/// Group. It is declarative data only — it carries no code and cannot change
/// input semantics. `ThemeRuntime.resolve(_:context:)` turns it into a
/// ``ResolvedTheme`` before the renderer ever sees it.
public struct KeyboardTheme: Codable, Hashable, Sendable, Identifiable {
    public var id: String
    public var schemaVersion: Int
    public var metadata: ThemeMetadata
    public var material: KeyboardMaterial
    public var palette: ThemePalette
    public var metrics: ThemeMetrics

    public init(
        id: String,
        schemaVersion: Int = KeyboardTheme.currentSchemaVersion,
        metadata: ThemeMetadata,
        material: KeyboardMaterial,
        palette: ThemePalette,
        metrics: ThemeMetrics
    ) {
        self.id = id
        self.schemaVersion = schemaVersion
        self.metadata = metadata
        self.material = material
        self.palette = palette
        self.metrics = metrics
    }

    /// The schema version emitted by this build. Bump when the on-disk shape
    /// changes and add a migration in `ThemeRuntime`.
    public static let currentSchemaVersion = 1
}
