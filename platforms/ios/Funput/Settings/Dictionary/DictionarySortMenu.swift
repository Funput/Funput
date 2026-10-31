import PersonalDictionary
import SwiftUI

/// The sort, at the right of the line that counts the words it orders. It names the
/// order in use, so the list never has to be read to learn how it is sorted.
struct DictionarySortMenu: View {
    @Binding var sort: DictionarySort

    var body: some View {
        Menu {
            Picker("Sắp xếp", selection: $sort) {
                ForEach(DictionarySort.allCases, id: \.self) { sort in
                    Label(sort.title, systemImage: sort.systemImage).tag(sort)
                }
            }
        } label: {
            HStack(spacing: 4) {
                Text(sort.title)
                Image(systemName: "arrow.up.arrow.down").imageScale(.small)
            }
            .font(.footnote.weight(.semibold))
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .textCase(nil)
        .accessibilityLabel("Sắp xếp")
        .accessibilityValue(sort.title)
        .accessibilityIdentifier("dictionary.sort")
    }
}
