import FunputShared
import SwiftUI

/// A feedback switch that only works once the keyboard has Full Access.
enum FullAccessFeature: Equatable {
    case haptics
    case hapticStrength(HapticFeedbackStrength)
    case sound

    var message: String {
        let action = switch self {
        case .haptics, .hapticStrength: "Để rung hoạt động"
        case .sound: "Để phát âm thanh khi gõ"
        }
        return "\(action), hãy mở Funput trong Cài đặt, chọn Bàn phím và bật Cho phép truy cập đầy đủ."
    }
}

extension View {
    /// Asks for Full Access when a switch that needs it is turned on without it.
    ///
    /// Shared by the feedback page and the keyboard tab's quick settings, so both say the
    /// same thing and both save the choice before sending the user to Settings.
    func fullAccessPrompt(_ pending: Binding<FullAccessFeature?>, model: SettingsModel) -> some View {
        modifier(FullAccessPrompt(pending: pending, model: model))
    }
}

private struct FullAccessPrompt: ViewModifier {
    @Environment(\.openURL) private var openURL
    @Binding var pending: FullAccessFeature?
    let model: SettingsModel

    func body(content: Content) -> some View {
        content.alert(
            "Cho phép truy cập đầy đủ",
            isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } }),
            presenting: pending
        ) { feature in
            Button("Hủy", role: .cancel) {}
            Button("Mở Cài đặt") {
                // Saved before leaving: once access is granted the switch already reads on.
                guard model.enableFullAccessFeature(feature) else { return }
                openURL(.funputSystemSettings)
            }
        } message: { feature in
            Text(feature.message)
        }
    }
}
