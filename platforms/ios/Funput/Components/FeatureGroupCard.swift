import SwiftUI

struct FeatureGroupCard: View {
    let title: String
    let systemImage: String
    let summary: String
    let features: [String]

    var body: some View {
        AdaptiveGlassCard {
            VStack(alignment: .leading, spacing: 14) {
                Label(title, systemImage: systemImage)
                    .font(.headline)
                Text(summary)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Divider()
                ForEach(features, id: \.self) { feature in
                    Label(feature, systemImage: "circle.fill")
                        .font(.subheadline)
                        .symbolRenderingMode(.hierarchical)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}
