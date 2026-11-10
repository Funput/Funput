import FunputShared
import SwiftUI

/// The feedback choices and the sheets they open.
struct FeedbackSettingsSection: View {
    @ObservedObject var model: SettingsModel
    @State private var showsHapticSheet = false
    @State private var showsSoundSheet = false

    var body: some View {
        FeedbackSettingsCard(
            hapticStrength: model.selectedHapticStrength,
            selectHapticStrength: { showsHapticSheet = true },
            soundStyle: model.selectedKeySoundStyle,
            selectSoundStyle: { showsSoundSheet = true },
            keyPreviews: model.boolBinding(\.showsKeyPreviews)
        )
        .sheet(isPresented: $showsHapticSheet) {
            HapticFeedbackSheet(model: model)
        }
        .sheet(isPresented: $showsSoundSheet) {
            KeySoundSheet(model: model)
        }
    }
}
