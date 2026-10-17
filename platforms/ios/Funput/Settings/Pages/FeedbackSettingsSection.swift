import FunputShared
import SwiftUI

/// The feedback choices and the Full Access prompt they can raise.
struct FeedbackSettingsSection: View {
    @ObservedObject var model: SettingsModel
    @State private var pendingAccess: FullAccessFeature?
    @State private var showsHapticSheet = false

    var body: some View {
        FeedbackSettingsCard(
            hapticStrength: model.selectedHapticStrength,
            selectHapticStrength: { showsHapticSheet = true },
            sound: model.fullAccessBinding(\.isKeySoundEnabled) { pendingAccess = .sound },
            keyPreviews: model.boolBinding(\.showsKeyPreviews)
        )
        .sheet(isPresented: $showsHapticSheet) {
            HapticFeedbackSheet(model: model)
        }
        .fullAccessPrompt($pendingAccess, model: model)
    }
}
