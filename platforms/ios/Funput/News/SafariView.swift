import SafariServices
import SwiftUI

/// A page to open in an in-app Safari sheet.
struct SafariDestination: Identifiable {
    let url: URL
    var id: URL { url }
}

/// Safari inside the app: the reader keeps their place in the tab, and the page gets
/// Safari's own privacy and reader features rather than a web view the app controls.
struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.dismissButtonStyle = .close
        return controller
    }

    func updateUIViewController(_ controller: SFSafariViewController, context: Context) {}
}
