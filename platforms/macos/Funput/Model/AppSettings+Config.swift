import Foundation

/// What an import changed, for the confirmation shown to the user.
struct ConfigImportSummary {
    var shortcutsAdded = 0
    var shortcutsUpdated = 0
    var appliedMacPlatform = false
    var newerVersion = false
}

/// A user-facing failure while importing a config file.
enum ConfigError: LocalizedError {
    case unreadable   // not JSON / decode failed
    case unrecognized // decoded, but not a Funput config

    var errorDescription: String? {
        switch self {
        case .unreadable:   "Không đọc được tệp cấu hình (định dạng sai hoặc tệp hỏng)."
        case .unrecognized: "Tệp này không phải cấu hình Funput hợp lệ."
        }
    }
}

extension AppSettings {
    /// App version string for the exported document's `source`.
    private static var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0"
    }

    // MARK: - Export

    /// Snapshot the current settings into an interchange document.
    func exportDocument() -> ConfigDocument {
        ConfigDocument(
            exportedAt: Date(),
            source: .init(platform: "macos", appVersion: Self.appVersion),
            preferences: .init(
                inputMethod: inputMethod.configKey,
                toneStyle: toneStyle.configKey,
                smartEnglishRestore: smartEnglishRestore,
                eagerRestore: eagerRestore,
                spellCheck: spellCheckEnabled,
                autoCapitalize: autoCapitalizeEnabled,
                shortcutSmartCase: shortcutSmartCase
            ),
            shortcuts: shortcuts.map { .init(trigger: $0.trigger, expansion: $0.expansion) },
            platform: .init(macos: .init(
                toggleShortcut: toggleShortcut,
                flipShortcut: flipShortcut,
                appLanguageMemory: appLanguageMemory.snapshot
            ))
        )
    }

    /// Encode the current settings to pretty-printed JSON.
    func exportData() throws -> Data {
        try exportDocument().encoded()
    }

    // MARK: - Import

    /// Decode + apply a config file. Throws `ConfigError` on a bad/foreign file.
    @discardableResult
    func importData(_ data: Data) throws -> ConfigImportSummary {
        let document: ConfigDocument
        do {
            document = try ConfigDocument.decoded(from: data)
        } catch {
            throw ConfigError.unreadable
        }
        guard document.schema == ConfigDocument.schemaID else { throw ConfigError.unrecognized }
        return apply(document)
    }

    /// Merge a document into the live settings. Non-destructive: overwrites present
    /// fields, appends/updates shortcuts by trigger, and never deletes existing data.
    @discardableResult
    func apply(_ document: ConfigDocument) -> ConfigImportSummary {
        var summary = ConfigImportSummary()
        summary.newerVersion = document.version > ConfigDocument.currentVersion

        if let prefs = document.preferences {
            if let key = prefs.inputMethod, let value = InputMethod(configKey: key) { inputMethod = value }
            if let key = prefs.toneStyle, let value = ToneStyle(configKey: key) { toneStyle = value }
            if let value = prefs.smartEnglishRestore { smartEnglishRestore = value }
            if let value = prefs.eagerRestore { eagerRestore = value }
            if let value = prefs.spellCheck { spellCheckEnabled = value }
            if let value = prefs.autoCapitalize { autoCapitalizeEnabled = value }
            if let value = prefs.shortcutSmartCase { shortcutSmartCase = value }
        }

        if let incoming = document.shortcuts {
            var merged = shortcuts
            for item in incoming {
                if let index = merged.firstIndex(where: { $0.trigger == item.trigger }) {
                    if merged[index].expansion != item.expansion {
                        merged[index].expansion = item.expansion
                        summary.shortcutsUpdated += 1
                    }
                } else {
                    merged.append(TextShortcut(trigger: item.trigger, expansion: item.expansion))
                    summary.shortcutsAdded += 1
                }
            }
            shortcuts = merged   // didSet persists + bumps shortcutsRevision (re-syncs engine)
        }

        // Platform-specific block: apply only the one matching this OS.
        if let mac = document.platform?.macos {
            if let toggle = mac.toggleShortcut { toggleShortcut = toggle }
            if let flip = mac.flipShortcut { flipShortcut = flip }
            // Newer exports carry the per-app memory directly; older ones only have
            // the now-removed exclusion list, migrated to "remembered as English".
            if let memory = mac.appLanguageMemory { appLanguageMemory.merge(memory) }
            if let legacyApps = mac.excludedApps {
                appLanguageMemory.mergeLegacyExcludedApps(legacyApps.map(\.id))
            }
            summary.appliedMacPlatform = true
        }

        return summary
    }
}
