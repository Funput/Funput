import Foundation
import Observation

/// User preferences for Funput, persisted in `UserDefaults`. The Settings UI and
/// (later) the `IMKInputController` live in the same process, so they share this
/// store directly. `@Observable` drives live SwiftUI updates.
@Observable
final class AppSettings {
    static let shared = AppSettings()

    var inputMethod: InputMethod {
        didSet { defaults.set(inputMethod.rawValue, forKey: Keys.inputMethod) }
    }
    /// Tone-mark placement style (traditional `hòa` vs modern `hoà`). Read live by
    /// `FunputInputController` and pushed to the engine.
    var toneStyle: ToneStyle {
        didSet { defaults.set(toneStyle.rawValue, forKey: Keys.toneStyle) }
    }
    /// Whether Vietnamese composition is active (vs. English pass-through). Flipped by
    /// the toggle shortcut and the menu bar; read live by `FunputInputController`.
    var vietnameseEnabled: Bool {
        didSet { defaults.set(vietnameseEnabled, forKey: Keys.vietnameseEnabled) }
    }
    /// Auto-restore words that aren't valid Vietnamese (English typing).
    var smartEnglishRestore: Bool {
        didSet { defaults.set(smartEnglishRestore, forKey: Keys.smartEnglishRestore) }
    }
    /// Restore the instant a word becomes non-Vietnamese, without waiting for Space.
    var eagerRestore: Bool {
        didSet { defaults.set(eagerRestore, forKey: Keys.eagerRestore) }
    }
    /// Spell-check ("Kiểm tra chính tả"): only place a diacritic when it forms a valid
    /// Vietnamese syllable, otherwise keep the modifier key literal. Off by default.
    var spellCheckEnabled: Bool {
        didSet { defaults.set(spellCheckEnabled, forKey: Keys.spellCheckEnabled) }
    }
    /// Auto-capitalize ("Tự động viết hoa"): uppercase the first letter at the start of
    /// a sentence. Off by default.
    var autoCapitalizeEnabled: Bool {
        didSet { defaults.set(autoCapitalizeEnabled, forKey: Keys.autoCapitalizeEnabled) }
    }
    /// Re-open the previous word on Backspace so the next keystroke retones it
    /// (`chào` + Space + ⌫ + `s` → `cháo`). On by default and deliberately without UI —
    /// Windows and Android just do it too. It exists as an escape hatch for an app whose
    /// text-input bridge misreports the document:
    /// `defaults write app.funput.inputmethod.Funput retoneAfterBackspace -bool false`.
    var retoneAfterBackspace: Bool {
        didSet { defaults.set(retoneAfterBackspace, forKey: Keys.retoneAfterBackspace) }
    }
    /// User-recorded VI/EN toggle hotkey. Defaults to `⌃\`. Read live by
    /// `FunputInputController`.
    var toggleShortcut: KeyCombo {
        didSet { defaults.set(try? JSONEncoder().encode(toggleShortcut), forKey: Keys.toggleShortcut) }
    }
    /// User-recorded hotkey to flip the word being composed VN↔raw. `nil` = off.
    /// Read live by `FunputInputController`.
    var flipShortcut: KeyCombo? {
        didSet {
            if let data = flipShortcut.flatMap({ try? JSONEncoder().encode($0) }) {
                defaults.set(data, forKey: Keys.flipShortcut)
            } else {
                defaults.removeObject(forKey: Keys.flipShortcut)
            }
        }
    }
    var launchAtLogin: Bool {
        didSet { defaults.set(launchAtLogin, forKey: Keys.launchAtLogin) }
    }
    var showMenuBarIcon: Bool {
        didSet { defaults.set(showMenuBarIcon, forKey: Keys.showMenuBarIcon) }
    }
    var hasCompletedOnboarding: Bool {
        didSet { defaults.set(hasCompletedOnboarding, forKey: Keys.hasCompletedOnboarding) }
    }
    /// Apps where Vietnamese input is suppressed (English pass-through). Read live by
    /// `FunputInputController` against the focused client's bundle identifier.
    var excludedApps: [ExcludedApp] {
        didSet { defaults.set(try? JSONEncoder().encode(excludedApps), forKey: Keys.excludedApps) }
    }
    /// Text-expansion shortcuts (gõ tắt). Persisted as JSON and pushed to the engine by
    /// `FunputInputController`, which re-syncs whenever `shortcutsRevision` changes.
    var shortcuts: [TextShortcut] {
        didSet {
            defaults.set(try? JSONEncoder().encode(shortcuts), forKey: Keys.shortcuts)
            shortcutsRevision &+= 1
        }
    }
    /// Bumped on every `shortcuts` mutation so the controller knows when to re-marshal
    /// the table to the engine (instead of doing it on every keystroke). Not persisted.
    @ObservationIgnored private(set) var shortcutsRevision = 0

    /// Manual VI/EN choices per app id. A toggle (hotkey or Funput's own UI) pins the
    /// choice for that app, winning over the exclusion-list default on later focus
    /// changes — parity with the Windows shell's override map. Session-only.
    @ObservationIgnored var vietnameseOverrides: [String: Bool] = [:]
    /// A VI/EN choice made from Funput's own UI while our window held focus — bound
    /// to the next app the user returns to (see `resolveVietnamese`). Session-only.
    @ObservationIgnored var pendingVietnameseOverride: Bool?

    /// Bumped when an external `funput://settings` request arrives (the /Applications
    /// launcher, opened from Spotlight). Observed by the menu bar label, which opens
    /// the Settings window. Not persisted.
    var openSettingsRequest = 0

    @ObservationIgnored private let defaults: UserDefaults

    /// Injectable defaults keep the preference mapping testable without touching
    /// the user's real Funput configuration. Production uses `.standard` above.
    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        defaults.register(defaults: [
            Keys.smartEnglishRestore: true,
            Keys.eagerRestore: true,
            Keys.showMenuBarIcon: true,
            Keys.vietnameseEnabled: true,
            Keys.retoneAfterBackspace: true,
        ])
        inputMethod = InputMethod.persisted(defaults.object(forKey: Keys.inputMethod))
        toneStyle = ToneStyle(rawValue: defaults.integer(forKey: Keys.toneStyle)) ?? .traditional
        vietnameseEnabled = defaults.bool(forKey: Keys.vietnameseEnabled)
        smartEnglishRestore = defaults.bool(forKey: Keys.smartEnglishRestore)
        eagerRestore = defaults.bool(forKey: Keys.eagerRestore)
        spellCheckEnabled = defaults.bool(forKey: Keys.spellCheckEnabled)
        autoCapitalizeEnabled = defaults.bool(forKey: Keys.autoCapitalizeEnabled)
        retoneAfterBackspace = defaults.bool(forKey: Keys.retoneAfterBackspace)
        toggleShortcut = defaults.data(forKey: Keys.toggleShortcut)
            .flatMap { try? JSONDecoder().decode(KeyCombo.self, from: $0) } ?? .defaultToggle
        flipShortcut = defaults.data(forKey: Keys.flipShortcut)
            .flatMap { try? JSONDecoder().decode(KeyCombo.self, from: $0) }
        launchAtLogin = defaults.bool(forKey: Keys.launchAtLogin)
        showMenuBarIcon = defaults.bool(forKey: Keys.showMenuBarIcon)
        hasCompletedOnboarding = defaults.bool(forKey: Keys.hasCompletedOnboarding)
        excludedApps = defaults.data(forKey: Keys.excludedApps)
            .flatMap { try? JSONDecoder().decode([ExcludedApp].self, from: $0) } ?? []
        shortcuts = defaults.data(forKey: Keys.shortcuts)
            .flatMap { try? JSONDecoder().decode([TextShortcut].self, from: $0) } ?? []
        defaults.set(inputMethod.rawValue, forKey: Keys.inputMethod)
    }
}
