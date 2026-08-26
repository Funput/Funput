import PhotosUI
import SwiftUI
import ThemeSchema

struct ThemeImageControls: View {
    @Binding var draft: ThemeEditorDraft
    @State private var selection: PhotosPickerItem?
    @State private var cropRequest: ThemeCropRequest?
    @State private var confirmsDelete = false
    @State private var isProcessing = false
    @State private var processingTask: Task<Void, Never>?

    var body: some View {
        ContentCard {
            Text("Ảnh nền").font(.headline)
            if let data = draft.renderedImageData, let image = UIImage(data: data) {
                Image(uiImage: image).resizable().scaledToFill()
                    .frame(height: 110).clipShape(.rect(cornerRadius: 12))
                HStack {
                    picker("Đổi ảnh")
                    Button("Điều chỉnh khung", systemImage: "crop") {
                        cropRequest = ThemeCropRequest()
                    }
                        .accessibilityIdentifier("themeEditor.imageCrop")
                }
                Button("Xóa ảnh", systemImage: "trash", role: .destructive) {
                    confirmsDelete = true
                }
                .accessibilityIdentifier("themeEditor.imageDelete")
                blurSlider
            } else {
                ContentUnavailableView("Chưa có ảnh", systemImage: "photo")
                picker("Chọn ảnh")
                Text("Hãy chọn ảnh để có thể lưu theme ở chế độ Ảnh.")
                    .font(.caption).foregroundStyle(.secondary)
                    .accessibilityIdentifier("themeEditor.imageError")
            }
            if isProcessing { ProgressView("Đang xử lý ảnh…") }
        }
        .sheet(item: $cropRequest) { _ in ThemeImageCropEditor(draft: $draft) }
        .confirmationDialog("Xóa ảnh nền?", isPresented: $confirmsDelete) {
            Button("Xóa ảnh", role: .destructive) { draft.removeImage() }
        }
        .onChange(of: selection) { _, item in load(item) }
        .onDisappear { processingTask?.cancel() }
    }

    private func picker(_ title: String) -> some View {
        PhotosPicker(selection: $selection, matching: .images) {
            Label(title, systemImage: "photo.on.rectangle")
        }
        .buttonStyle(.borderedProminent)
        .accessibilityIdentifier("themeEditor.imagePicker")
    }

    private var blurSlider: some View {
        ThemeMetricSlider(
            title: "Blur ảnh",
            value: Binding(
                get: { draft.customTheme.theme.backgroundEffects.image?.blurRadius ?? 0 },
                set: { updateBlur($0) }
            ),
            range: 0...24,
            step: 1,
            format: .points
        )
        .accessibilityIdentifier("themeEditor.imageBlur")
    }

    private func load(_ item: PhotosPickerItem?) {
        guard let item else { return }
        processingTask?.cancel()
        processingTask = Task {
            isProcessing = true
            defer { isProcessing = false }
            guard let data = try? await item.loadTransferable(type: Data.self),
                  !Task.isCancelled,
                  let processed = await ThemeImageProcessor.process(data, blurRadius: 0),
                  !Task.isCancelled
            else { return }
            draft.installImage(processed)
            cropRequest = ThemeCropRequest()
        }
    }

    private func updateBlur(_ value: Double) {
        draft.customTheme.theme.backgroundEffects.image?.blurRadius = value
        guard let source = draft.sourceImageData else { return }
        processingTask?.cancel()
        processingTask = Task {
            isProcessing = true
            defer { isProcessing = false }
            try? await Task.sleep(for: .milliseconds(80))
            guard !Task.isCancelled,
                  let processed = await ThemeImageProcessor.process(source, blurRadius: value),
                  !Task.isCancelled
            else { return }
            draft.installRenderedImage(processed, blur: value)
        }
    }
}

private struct ThemeCropRequest: Identifiable {
    let id = UUID()
}
