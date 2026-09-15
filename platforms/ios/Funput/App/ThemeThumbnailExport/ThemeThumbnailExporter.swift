#if DEBUG
import FunputShared
import KeyboardRenderer
import SwiftUI
import ThemeRuntime
import ThemeSchema
import UIKit

/// Renders every bundled theme through the production keyboard surface and writes the
/// gallery artwork that `Scripts/export-theme-thumbnails.sh` copies into the asset catalog.
@MainActor
enum ThemeThumbnailExporter {
    static let launchArgument = "-export-theme-thumbnails"
    /// The gallery card's thumbnail box: ``ThemeCard`` width minus its padding, by the
    /// thumbnail height. Captures use this aspect ratio so the card never crops keys.
    static let thumbnailSize = CGSize(width: 316, height: 204)
    static let pixelScale: CGFloat = 3

    static func run(in window: UIWindow) async throws {
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("ThemeThumbnails", isDirectory: true)
        try? FileManager.default.removeItem(at: directory)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)

        var manifest: [String: String] = [:]
        for theme in BundledThemes.all {
            for style in [UIUserInterfaceStyle.light, .dark] {
                let image = await render(themeID: theme.id, style: style, in: window)
                let name = BundledThemeThumbnail.assetName(themeID: theme.id, style: style)
                try image.pngData()?.write(to: directory.appendingPathComponent("\(name).png"))
            }
            manifest[theme.id] = BundledThemeThumbnail.fingerprint(theme)
        }

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys, .prettyPrinted]
        let manifestURL = directory.appendingPathComponent("\(BundledThemeThumbnail.manifestName).json")
        try encoder.encode(manifest).write(to: manifestURL)
        try Data().write(to: directory.appendingPathComponent("done"))
    }

    private static func render(
        themeID: String,
        style: UIUserInterfaceStyle,
        in window: UIWindow
    ) async -> UIImage {
        var configuration = FunputConfiguration.default
        configuration.selectedThemeID = themeID
        let presentation = KeyboardPreviewPresentation.make(configuration: configuration)
        let height = KeyboardMetrics.phonePortraitHeight(for: presentation.layout)
        let width = (height * thumbnailSize.width / thumbnailSize.height).rounded()

        let surface = KeyboardPreviewSurface(frame: CGRect(x: 0, y: 0, width: width, height: height))
        surface.overrideUserInterfaceStyle = style
        surface.apply(presentation: presentation, backgroundImageData: nil, isInteractive: false)
        window.addSubview(surface)
        surface.layoutIfNeeded()
        // Glass is composited by the render server; it has to be on screen for a few
        // frames before a capture includes it.
        try? await Task.sleep(nanoseconds: 600_000_000)

        let format = UIGraphicsImageRendererFormat()
        format.scale = pixelScale * thumbnailSize.width / width
        format.opaque = true
        let image = UIGraphicsImageRenderer(bounds: surface.bounds, format: format).image { _ in
            surface.drawHierarchy(in: surface.bounds, afterScreenUpdates: true)
        }
        surface.removeFromSuperview()
        return image
    }
}

/// Launch-argument harness: runs the exporter once the window exists, then reports.
struct ThemeThumbnailExportView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> ExportController { ExportController() }
    func updateUIViewController(_ controller: ExportController, context: Context) {}

    final class ExportController: UIViewController {
        private let status = UILabel()
        private var didStart = false

        override func viewDidLoad() {
            super.viewDidLoad()
            view.backgroundColor = .systemBackground
            status.text = "Đang xuất ảnh theme…"
            status.accessibilityIdentifier = "themeThumbnails.status"
            status.frame = CGRect(x: 20, y: 80, width: 360, height: 40)
            view.addSubview(status)
        }

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            guard !didStart, let window = view.window else { return }
            didStart = true
            Task { @MainActor in
                do {
                    try await ThemeThumbnailExporter.run(in: window)
                    status.text = "Xong"
                } catch {
                    status.text = "Lỗi: \(error.localizedDescription)"
                }
            }
        }
    }
}
#endif
