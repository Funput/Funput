import FunputShared
import SwiftUI

/// The "Phản hồi khi chạm" card with the Full Access prompt its switches can raise.
struct FeedbackSettingsSection: View {
    @Environment(\.openURL) private var openURL
    @ObservedObject var model: SettingsModel
    @State private var pendingAccess: FullAccessFeature?

    var body: some View {
        FeedbackSettingsCard(
            haptics: model.fullAccessBinding(\.isHapticFeedbackEnabled) { pendingAccess = .haptics },
            sound: model.fullAccessBinding(\.isKeySoundEnabled) { pendingAccess = .sound },
            keyPreviews: model.boolBinding(\.showsKeyPreviews)
        )
        .alert(
            "Cho phép truy cập đầy đủ",
            isPresented: Binding(get: { pendingAccess != nil }, set: { if !$0 { pendingAccess = nil } }),
            presenting: pendingAccess
        ) { feature in
            Button("Hủy", role: .cancel) {}
            Button("Mở Cài đặt") {
                // Saved before leaving: once access is granted the switch already reads on.
                model.update(feature.keyPath, to: true)
                openURL(.funputSystemSettings)
            }
        } message: { feature in
            Text(feature.message)
        }
    }
}

/// A feedback switch that only works once the keyboard has Full Access.
enum FullAccessFeature {
    case haptics
    case sound

    var keyPath: WritableKeyPath<FunputConfiguration, Bool> {
        switch self {
        case .haptics: \.isHapticFeedbackEnabled
        case .sound: \.isKeySoundEnabled
        }
    }

    var message: String {
        let action = switch self {
        case .haptics: "Để rung hoạt động"
        case .sound: "Để phát âm thanh khi gõ"
        }
        return "\(action), hãy mở Funput trong Cài đặt, chọn Bàn phím và bật Cho phép truy cập đầy đủ."
    }
}
