import SwiftUI
import ThemeSchema

struct ThemeImageCropEditor: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var draft: ThemeEditorDraft
    @State private var crop: ThemeBackgroundImage
    @State private var startCrop: ThemeBackgroundImage?

    init(draft: Binding<ThemeEditorDraft>) {
        _draft = draft
        _crop = State(initialValue: draft.wrappedValue.customTheme.theme
            .backgroundEffects.image ?? ThemeBackgroundImage(assetID: "pending"))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 18) {
                Text("Kéo ảnh để đặt chủ thể. Chụm hai ngón để phóng to.")
                    .font(.subheadline).foregroundStyle(.secondary)
                GeometryReader { proxy in
                    cropImage(size: proxy.size)
                }
                .aspectRatio(1.29, contentMode: .fit)
                .clipShape(.rect(cornerRadius: 18))
                .overlay { RoundedRectangle(cornerRadius: 18).stroke(.white.opacity(0.7)) }
                ThemeMetricSlider(
                    title: "Phóng to",
                    value: $crop.zoom,
                    range: 1...4,
                    step: 0.05,
                    format: .decimal
                )
                .accessibilityIdentifier("themeEditor.imageZoom")
                Spacer()
            }
            .padding(18)
            .navigationTitle("Điều chỉnh khung")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Hủy") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Xong") {
                        draft.customTheme.theme.backgroundEffects.image = crop
                        dismiss()
                    }
                    .accessibilityIdentifier("themeEditor.imageCropDone")
                }
            }
        }
    }

    @ViewBuilder private func cropImage(size: CGSize) -> some View {
        if let data = draft.sourceImageData, let image = UIImage(data: data) {
            let overflow = overflow(image: image.size, target: size, zoom: crop.zoom)
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .scaleEffect(crop.zoom)
                .offset(
                    x: (0.5 - crop.focalX) * overflow.width * 2,
                    y: (0.5 - crop.focalY) * overflow.height * 2
                )
                .frame(width: size.width, height: size.height)
                .clipped()
                .contentShape(.rect)
                .gesture(dragGesture(overflow: overflow))
                .simultaneousGesture(magnificationGesture)
        }
    }

    private func dragGesture(overflow: CGSize) -> some Gesture {
        DragGesture()
            .onChanged { value in
                if startCrop == nil { startCrop = crop }
                guard let startCrop else { return }
                if overflow.width > 0 {
                    crop.focalX = min(max(startCrop.focalX - value.translation.width / (overflow.width * 2), 0), 1)
                }
                if overflow.height > 0 {
                    crop.focalY = min(max(startCrop.focalY - value.translation.height / (overflow.height * 2), 0), 1)
                }
            }
            .onEnded { _ in startCrop = nil }
    }

    private var magnificationGesture: some Gesture {
        MagnificationGesture()
            .onChanged { value in
                if startCrop == nil { startCrop = crop }
                crop.zoom = min(max((startCrop?.zoom ?? crop.zoom) * value, 1), 4)
            }
            .onEnded { _ in startCrop = nil }
    }

    private func overflow(image: CGSize, target: CGSize, zoom: Double) -> CGSize {
        guard image.width > 0, image.height > 0 else { return .zero }
        let scale = max(target.width / image.width, target.height / image.height) * zoom
        return CGSize(
            width: max(0, (image.width * scale - target.width) / 2),
            height: max(0, (image.height * scale - target.height) / 2)
        )
    }
}
