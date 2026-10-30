/// Keeps the decoded emoji catalog between visits to the emoji panel.
///
/// The panel itself is released whenever the user leaves it, and decoding the
/// bundled catalog again (1898 entries of JSON) on every visit would put that work
/// on the main thread at the moment the emoji key is tapped. The decoded catalog is
/// small next to the panel, so it is kept while the keyboard is on screen and
/// dropped with ``release()`` when it goes away or memory runs short.
@MainActor
public final class EmojiCatalogCache {
    private var catalog: EmojiCatalog?
    private let decode: () -> EmojiCatalog

    public init(decode: @escaping () -> EmojiCatalog = { .bundled }) {
        self.decode = decode
    }

    /// The catalog, decoded on first use after a release.
    public func load() -> EmojiCatalog {
        if let catalog { return catalog }
        let decoded = decode()
        catalog = decoded
        return decoded
    }

    public func release() {
        catalog = nil
    }
}
