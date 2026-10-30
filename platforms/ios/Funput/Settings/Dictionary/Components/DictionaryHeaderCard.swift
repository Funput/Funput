import PersonalDictionary
import SwiftUI

/// The top of the page: how many words the dictionary holds, what it is, and the
/// switch between its three lists.
struct DictionaryHeaderCard: View {
    @Binding var filter: DictionaryFilter
    let counts: (DictionaryFilter) -> Int

    private var total: Int { counts(.learned) + counts(.added) }

    var body: some View {
        ContentCard {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(total.formatted())
                    .font(.largeTitle.bold())
                    .monospacedDigit()
                    .contentTransition(.numericText())
                Text("từ trong từ điển")
                    .font(.headline)
                    .foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
            .accessibilityIdentifier("dictionary.total")
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Image(systemName: "lock.shield").accessibilityHidden(true)
                Text("Học từ những gì bạn gõ bằng Funput, chỉ lưu trên máy này.")
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
            Picker("Danh sách", selection: $filter.animation(.easeInOut(duration: 0.2))) {
                ForEach(DictionaryFilter.allCases, id: \.self) { filter in
                    Text("\(filter.title) \(counts(filter).formatted())").tag(filter)
                }
            }
            .pickerStyle(.segmented)
            .padding(.top, 4)
            .accessibilityIdentifier("dictionary.filter")
        }
        .listRowInsets(EdgeInsets())
        .listRowBackground(Color.clear)
    }
}
