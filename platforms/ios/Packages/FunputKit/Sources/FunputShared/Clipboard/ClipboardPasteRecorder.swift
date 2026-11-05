import Foundation

/// Records what the user pastes through Funput into the clipboard history.
///
/// The keyboard never reads the pasteboard on its own. Every content read outside a
/// `UIPasteControl` raises the iOS paste prompt, and automatic capture raised one for
/// each new copy: every copy made on a Mac reaches the iPhone through Universal
/// Clipboard, a refusal was asked again in the next app, and the idle wait meant the
/// prompt landed mid-sentence. Text now arrives only when the user taps a paste
/// control, so the history holds what was pasted through Funput rather than everything
/// that was copied.
@MainActor
public final class ClipboardPasteRecorder {
    /// The generation already pasted, so the toolbar stops inviting a second paste of
    /// it. Inherited from the previous session: the keyboard is rebuilt in every app.
    public private(set) var lastPastedChangeCount: Int?
    private var active = false
    private let gateway: any ClipboardGateway
    private let allowsRecording: () -> Bool
    private let save: (ClipboardItem) -> Bool
    private let marks: any ClipboardSessionMarkStoring
    private let onUpdate: () -> Void

    public init(
        gateway: any ClipboardGateway,
        allowsRecording: @escaping () -> Bool,
        save: @escaping (ClipboardItem) -> Bool,
        marks: any ClipboardSessionMarkStoring,
        onUpdate: @escaping () -> Void
    ) {
        self.gateway = gateway
        self.allowsRecording = allowsRecording
        self.save = save
        self.marks = marks
        self.onUpdate = onUpdate
    }

    public func begin() {
        active = true
        lastPastedChangeCount = marks.lastPastedChangeCount()
    }

    public func end() {
        active = false
    }

    /// `changeCount` is the generation the paste control delivered, or nil when the
    /// provider could not pin one down. It only counts if the pasteboard still holds
    /// that generation now: a copy that landed in between must not be claimed as
    /// pasted, or its offer would never appear. Such a paste is stored under -1, which
    /// never suppresses an offer.
    ///
    /// Text longer than ``ClipboardHistoryPolicy/maxItemUTF16`` was still pasted, so
    /// the offer is withdrawn as usual, but it is not kept: checked here so the store
    /// never decodes the history only to turn it away.
    public func didPaste(_ text: String, changeCount: Int?) {
        guard active, !text.isEmpty else { return }
        let stable = changeCount == gateway.snapshot().changeCount ? changeCount : nil
        lastPastedChangeCount = stable
        if let stable { marks.markPasted(stable) }
        guard allowsRecording(), ClipboardHistoryPolicy.admits(text),
              save(ClipboardItem(text: text, sourceChangeCount: stable ?? -1)) else { return }
        onUpdate()
    }
}
