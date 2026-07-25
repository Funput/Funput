import Foundation

/// Safe Swift wrapper around the `funput-ffi` C engine handle.
///
/// One instance per `IMKInputController` (input is single-threaded on the main
/// run loop). Named `FunputComposer` to avoid colliding with the C `FunputEngine`
/// type imported from the bridging header.
final class FunputComposer {
    private let handle: OpaquePointer

    init() {
        handle = funput_engine_new()
    }

    deinit {
        funput_engine_free(handle)
    }

    func setEnabled(_ enabled: Bool) {
        funput_set_enabled(handle, enabled)
    }

    /// Apply the whole engine configuration (method, tone style, and the feature
    /// toggles) in one FFI call — the batch equivalent of the former per-option
    /// setters. `enabled` is a separate runtime toggle (see `setEnabled`).
    func configure(_ config: FunputConfig) {
        funput_configure(handle, config)
    }

    /// Arm capitalization for the next word (call on focus so the first letter typed
    /// in a field is capitalized). A no-op unless auto-capitalize is on.
    func armCapitalization() {
        funput_arm_capitalization(handle)
    }

    func clear() {
        funput_clear(handle)
    }

    /// Remove every text-expansion shortcut (gõ tắt). Pair with `addShortcut` to
    /// replace the whole table when syncing from `AppSettings`.
    func clearShortcuts() {
        funput_clear_shortcuts(handle)
    }

    /// Define a text-expansion shortcut: typing `trigger` then a word boundary injects
    /// `expansion` (`vn` → `Việt Nam`). Both strings cross the C ABI as UTF-32.
    func addShortcut(trigger: String, expansion: String) {
        let t = trigger.unicodeScalars.map(\.value)
        let e = expansion.unicodeScalars.map(\.value)
        funput_add_shortcut(handle, t, UInt(t.count), e, UInt(e.count))
    }

    /// The composed syllable buffer — the text shown as marked (underlined) text.
    func buffer() -> String {
        var out = [UInt32](repeating: 0, count: Int(CHARS_CAP))
        let count = funput_buffer(handle, &out, UInt(out.count))
        return Self.scalars(out, count)
    }

    /// Physical origin of a key, mirroring the engine's `KeySource`. Raw values
    /// match the C `SOURCE_STANDARD` / `SOURCE_NUMPAD` constants in `funput.h`. A
    /// numpad digit is kept a literal number instead of a VNI tone/shape modifier.
    enum KeySource: UInt32 {
        case standard = 0 // SOURCE_STANDARD
        case numpad = 1 // SOURCE_NUMPAD
    }

    /// Feed one Unicode scalar tagged with its physical `source`; returns the
    /// platform instruction. Defaults to the main keyboard, so existing call sites
    /// are unchanged.
    @discardableResult
    func process(_ scalar: Unicode.Scalar, source: KeySource = .standard) -> FunputResult {
        funput_process_key(handle, scalar.value, source.rawValue)
    }

    /// Drop the last composed character (in-composition Backspace).
    @discardableResult
    func backspace() -> FunputResult {
        funput_backspace(handle)
    }

    /// Flip the word being composed between its Vietnamese form and its raw
    /// keystrokes (`card` ⇄ `cải`), and back on a second call. Returns the engine
    /// result; the caller re-renders the marked text from `buffer()` when its
    /// action is not `ACTION_NONE` (macOS shows marked text, so the delete+inject
    /// payload itself is unused).
    func flipComposing() -> FunputResult {
        funput_flip_composing(handle)
    }

    /// Decode a `FunputResult`'s inline `chars` (a C `uint32_t[64]`, imported as a
    /// tuple) into a `String`.
    static func output(of result: FunputResult) -> String {
        var copy = result
        let count = Int(result.count)
        return withUnsafePointer(to: &copy.chars) { tuplePtr in
            tuplePtr.withMemoryRebound(to: UInt32.self, capacity: Int(CHARS_CAP)) { buf in
                var view = String.UnicodeScalarView()
                for i in 0..<count {
                    if let s = Unicode.Scalar(buf[i]) { view.append(s) }
                }
                return String(view)
            }
        }
    }

    private static func scalars(_ codepoints: [UInt32], _ count: UInt) -> String {
        var view = String.UnicodeScalarView()
        for i in 0..<Int(count) {
            if let s = Unicode.Scalar(codepoints[i]) { view.append(s) }
        }
        return String(view)
    }
}
