import Foundation

/// Per-app VI/EN memory: remembers the last manual VI/EN choice for each app and
/// replays it the next time that app is focused — no exclusion list to configure.
/// Backed by `funput-ffi`'s `FunputAppLanguage` handle (see
/// `app/crates/funput-ffi/src/app_language/`), which only decides "should this
/// app be Vietnamese" and never touches disk itself. This class is the
/// persisted source of truth: it seeds the handle from `UserDefaults` at init
/// and re-persists on every remembered change, mirroring how `AppSettings`
/// syncs the gõ tắt table into the composition engine.
final class AppLanguageMemory {
    private let handle: OpaquePointer
    private let defaults: UserDefaults
    /// Authoritative persisted copy. The FFI handle has no "read the whole map
    /// back" call by design (see the `funput-ffi` README), so this dictionary —
    /// not the handle — is what gets saved to and loaded from `UserDefaults`.
    private var remembered: [String: Bool]
    /// A VI/EN choice made from Funput's own UI (menu bar / Settings window)
    /// while our window held focus — bound to the next app the user returns to.
    /// Session-only: re-arming it after a relaunch would be surprising.
    private(set) var pending: Bool?

    private static let memoryKey = "appLanguageMemory"
    /// The old, now-removed "always English" list's `UserDefaults` key. Read
    /// once at init to migrate, then deleted — see `migrateLegacyExcludedApps`.
    private static let legacyExcludedAppsKey = "excludedApps"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        handle = funput_app_language_new()
        remembered = defaults.data(forKey: Self.memoryKey)
            .flatMap { try? JSONDecoder().decode([String: Bool].self, from: $0) } ?? [:]
        Self.migrateLegacyExcludedApps(into: &remembered, defaults: defaults)
        for (id, enabled) in remembered { seed(id: id, enabled: enabled) }
        persist()
    }

    deinit {
        funput_app_language_free(handle)
    }

    /// Decide the VI/EN target when focus lands on `bundleId`, consuming a
    /// pending UI choice first. Returns `nil` when nothing is remembered for
    /// this app — the caller should leave the current state as-is.
    func resolve(for bundleId: String?) -> Bool? {
        if let pending {
            self.pending = nil
            if let bundleId { noteToggle(pending, for: bundleId) }
            return pending
        }
        guard let bundleId else { return nil }
        return noteFocus(bundleId)
    }

    /// Record a choice made by the toggle hotkey, which fires while the target
    /// app is focused: bind immediately, dropping any stale pending UI choice.
    func pin(_ enabled: Bool, to bundleId: String?) {
        pending = nil
        guard let bundleId else { return }
        noteToggle(enabled, for: bundleId)
    }

    /// Record a choice made from Funput's own UI. Our window holds focus at
    /// that moment, so the choice binds to the next app the user returns to.
    func setPending(_ enabled: Bool) {
        pending = enabled
    }

    /// A read-only copy of the remembered map, for exporting into a config
    /// document. No FFI round-trip: this dictionary already is the source of
    /// truth (see the type-level doc comment).
    var snapshot: [String: Bool] { remembered }

    /// Merge externally-provided entries (e.g. an imported config document),
    /// keeping any existing choice for an id that's already remembered.
    func merge(_ incoming: [String: Bool]) {
        var changed = false
        for (id, enabled) in incoming where remembered[id] == nil {
            remembered[id] = enabled
            seed(id: id, enabled: enabled)
            changed = true
        }
        if changed { persist() }
    }

    /// Merge ids from an imported config's now-legacy exclusion list, treating
    /// each as "remembered as English" — the same rule `migrateLegacyExcludedApps`
    /// applies to the local `UserDefaults` key, but for an imported document.
    func mergeLegacyExcludedApps(_ ids: [String]) {
        var asEnglish: [String: Bool] = [:]
        for id in ids { asEnglish[id] = false }
        merge(asEnglish)
    }

    // MARK: - Legacy migration

    /// One-time migration from the removed "always English" exclusion list: an
    /// app that was on it becomes remembered as English, so upgrading users
    /// keep their existing behavior instead of losing it outright. The legacy
    /// key is removed afterwards, which is also what makes this run once.
    private static func migrateLegacyExcludedApps(
        into remembered: inout [String: Bool],
        defaults: UserDefaults
    ) {
        guard let data = defaults.data(forKey: legacyExcludedAppsKey) else { return }
        defer { defaults.removeObject(forKey: legacyExcludedAppsKey) }
        struct LegacyExcludedApp: Decodable { let id: String }
        guard let apps = try? JSONDecoder().decode([LegacyExcludedApp].self, from: data) else { return }
        for app in apps where remembered[app.id] == nil {
            remembered[app.id] = false
        }
    }

    // MARK: - FFI marshalling (app ids are UTF-8, unlike the UTF-32 gõ tắt table)

    private func seed(id: String, enabled: Bool) {
        withUTF8(id) { funput_app_language_seed(handle, $0.baseAddress, UInt($0.count), enabled) }
    }

    private func noteFocus(_ id: String) -> Bool? {
        let code = withUTF8(id) {
            funput_app_language_note_focus(handle, $0.baseAddress, UInt($0.count))
        }
        switch code {
        case APP_LANG_ENGLISH: return false
        case APP_LANG_VIETNAMESE: return true
        default: return nil
        }
    }

    private func noteToggle(_ enabled: Bool, for id: String) {
        let changed = withUTF8(id) {
            funput_app_language_note_toggle(handle, $0.baseAddress, UInt($0.count), enabled)
        }
        guard changed else { return }
        remembered[id] = enabled
        persist()
    }

    private func persist() {
        defaults.set(try? JSONEncoder().encode(remembered), forKey: Self.memoryKey)
    }

    private func withUTF8<R>(_ string: String, _ body: (UnsafeBufferPointer<UInt8>) -> R) -> R {
        var copy = string
        return copy.withUTF8 { body($0) }
    }
}
