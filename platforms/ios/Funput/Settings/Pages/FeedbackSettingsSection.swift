import FunputShared
import SwiftUI

/// The "Phản hồi khi chạm" card with the Full Access prompt its switches can raise.
struct FeedbackSettingsSection: View {
    @ObservedObject var model: SettingsModel
    @State private var pendingAccess: FullAccessFeature?

    var body: some View {
        FeedbackSettingsCard(
            haptics: model.fullAccessBinding(\.isHapticFeedbackEnabled) { pendingAccess = .haptics },
            sound: model.fullAccessBinding(\.isKeySoundEnabled) { pendingAccess = .sound },
            keyPreviews: model.boolBinding(\.showsKeyPreviews)
        )
        .fullAccessPrompt($pendingAccess, model: model)
    }
}
