import SwiftUI

struct ThirdPartyLicensesScreen: View {
    private let bundle: Bundle
    @State private var blocks: [LicenseNoticeBlock]?
    @State private var loaded = false

    init(bundle: Bundle = .main) {
        self.bundle = bundle
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if let blocks {
                    ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                        noticeBlock(block)
                    }
                } else if loaded {
                    Text("Không thể đọc thông tin giấy phép. Vui lòng mở lại ứng dụng.")
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .textSelection(.enabled)
        }
        .accessibilityIdentifier("licenses.content")
        .navigationTitle("Giấy phép bên thứ ba")
        .navigationBarTitleDisplayMode(.inline)
        // NavigationLink builds its destination eagerly; read the file only once shown.
        .task { load() }
    }

    private func load() {
        guard !loaded else { return }
        blocks = bundle.url(forResource: "NOTICE", withExtension: "md")
            .flatMap { try? String(contentsOf: $0, encoding: .utf8) }
            .map(LicenseNoticeParser.blocks)
        loaded = true
    }

    @ViewBuilder
    private func noticeBlock(_ block: LicenseNoticeBlock) -> some View {
        switch block {
        case .heading(let text):
            Text(text)
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
        case .paragraph(let text):
            Text((try? AttributedString(
                markdown: text,
                options: .init(interpretedSyntax: .inlineOnly)
            )) ?? AttributedString(text))
                .font(.body)
        case .quote(let text):
            Text(verbatim: text)
                .font(.callout.monospaced())
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(.quaternary, in: RoundedRectangle(cornerRadius: 10))
        }
    }
}

#Preview {
    NavigationStack { ThirdPartyLicensesScreen() }
}
