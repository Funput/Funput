import SwiftUI

struct AboutScreen: View {
    private let versionLabel = AppMetadata.versionLabel(from: Bundle.main.infoDictionary ?? [:])

    var body: some View {
        AppScreen {
            AdaptiveGlassCard {
                VStack(alignment: .leading, spacing: 18) {
                    FunputIdentityView()
                    Text("Funput mang đến trải nghiệm gõ tiếng Việt nhanh, rõ ràng và riêng tư.")
                        .foregroundStyle(.secondary)
                    Divider()
                    Label(versionLabel, systemImage: "shippingbox")
                        .font(.subheadline)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            AdaptiveGlassCard {
                VStack(alignment: .leading, spacing: 10) {
                    Label("Được xây dựng cho cộng đồng", systemImage: "heart.fill")
                        .font(.headline)
                    Text("Funput là dự án mã nguồn mở, tập trung vào trải nghiệm nhập liệu tiếng Việt.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .navigationTitle("Giới thiệu")
    }
}

#Preview("Giới thiệu · Light") {
    NavigationStack { AboutScreen() }
        .preferredColorScheme(.light)
}

#Preview("Giới thiệu · Dark") {
    NavigationStack { AboutScreen() }
        .preferredColorScheme(.dark)
}
