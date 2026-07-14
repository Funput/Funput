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
    public var geometry: ThemeGeometry

    public init(
        id: String,
        schemaVersion: Int = KeyboardTheme.currentSchemaVersion,
        metadata: ThemeMetadata,
        material: KeyboardMaterial,
        palette: ThemePalette,
        metrics: ThemeMetrics,
        geometry: ThemeGeometry = .default
    ) {
        self.id = id
        self.schemaVersion = schemaVersion
        self.metadata = metadata
        self.material = material
        self.palette = palette
        self.metrics = metrics
        self.geometry = geometry
    }

    private enum CodingKeys: String, CodingKey {
        case id, schemaVersion, metadata, material, palette, metrics, geometry
    }

    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        id = try values.decode(String.self, forKey: .id)
        schemaVersion = try values.decodeIfPresent(Int.self, forKey: .schemaVersion) ?? 1
        metadata = try values.decode(ThemeMetadata.self, forKey: .metadata)
        material = try values.decode(KeyboardMaterial.self, forKey: .material)
        palette = try values.decode(ThemePalette.self, forKey: .palette)
        metrics = try values.decode(ThemeMetrics.self, forKey: .metrics)
        geometry = try values.decodeIfPresent(ThemeGeometry.self, forKey: .geometry) ?? .default
    }

    public func encode(to encoder: Encoder) throws {
        var values = encoder.container(keyedBy: CodingKeys.self)
        try values.encode(id, forKey: .id)
        try values.encode(schemaVersion, forKey: .schemaVersion)
        try values.encode(metadata, forKey: .metadata)
        try values.encode(material, forKey: .material)
        try values.encode(palette, forKey: .palette)
        try values.encode(metrics, forKey: .metrics)
        try values.encode(geometry, forKey: .geometry)
    }

    /// The schema version emitted by this build. Bump when the on-disk shape
    /// changes and add a migration in `ThemeRuntime`.
    public static let currentSchemaVersion = 2
}
