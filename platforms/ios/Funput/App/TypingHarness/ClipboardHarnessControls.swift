#if DEBUG
import FunputShared
import SwiftUI
import UIKit

/// A host-process copy while the real extension stays visible. Only enabled by
/// the clipboard UI test launch argument; the status reads the shared history.
struct ClipboardHarnessControls: View {
    private let text = ProcessInfo.processInfo.environment["CLIPBOARD_TEST_TEXT"] ?? "Clipboard test"

    var body: some View {
        VStack(alignment: .leading) {
            Button("Copy test text") { UIPasteboard.general.string = text }
                .accessibilityIdentifier("clipboardHarness.copy")
            TimelineView(.periodic(from: .now, by: 0.2)) { _ in
                Text(ClipboardStore().load().contains { $0.text == text } ? "Saved" : "Waiting")
                    .accessibilityIdentifier("clipboardHarness.status")
            }
        }
        .padding()
    }
}
#endif
