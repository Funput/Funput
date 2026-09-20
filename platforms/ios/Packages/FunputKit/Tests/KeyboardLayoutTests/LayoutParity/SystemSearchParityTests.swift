import KeyboardLayout
import Testing

struct SystemSearchParityTests {
    @Test("Search renders like the letters page", arguments: KeyboardInputMethod.allCases)
    func matchesTheLettersPage(method: KeyboardInputMethod) {
        // On the stock keyboard the two are the same picture; the magnifying glass on the
        // return key comes from the enter action, so no key here differs.
        // Including the number row, which is why both settings are checked here.
        for showsNumberRow in [true, false] {
            let search = SystemKeyboardLayouts.search(method, showsNumberRow: showsNumberRow).rows
            let letters = SystemKeyboardLayouts.letters(method, showsNumberRow: showsNumberRow).rows
            #expect(search.count == letters.count)
            for (searchRow, lettersRow) in zip(search, letters) {
                #expect(searchRow.keys.map(\.label) == lettersRow.keys.map(\.label))
                #expect(searchRow.keys.map(\.role) == lettersRow.keys.map(\.role))
                #expect(searchRow.keys.map(\.widthWeight) == lettersRow.keys.map(\.widthWeight))
                #expect(searchRow.horizontalInsetUnits == lettersRow.horizontalInsetUnits)
            }
        }
    }

    @Test("Search answers the number row preference", arguments: KeyboardInputMethod.allCases)
    func numberRowFollowsThePreference(method: KeyboardInputMethod) {
        // A search field used to keep the row whatever the user asked. The reason given
        // was that the Funput preset's search page always carries digits — it does not
        // (see `CompactWebPageTests`), so search was the one page that ignored the switch.
        for showsNumberRow in [true, false] {
            let layout = KeyboardLayoutResolver.resolve(
                inputMethod: method,
                mode: .letters,
                editorMode: .search,
                showsNumberRow: showsNumberRow,
                preset: .system
            )
            // VNI spends the row on tone modifiers, so it keeps it either way.
            let keepsRow = showsNumberRow || method == .vni
            #expect(layout.rows.count == (keepsRow ? 5 : 4))
            #expect(layout.rows.contains { $0.keys.map(\.label).joined() == "1234567890" } == keepsRow)
        }
    }

    @Test("A hidden row leaves search and letters the same height", arguments: KeyboardInputMethod.allCases)
    func rowCountMatchesTheLettersPage(method: KeyboardInputMethod) {
        // Layout and measured height both count rows, so a search page that disagreed with
        // the letters page would resize the keyboard when the focused field changed.
        for showsNumberRow in [true, false] {
            #expect(
                SystemKeyboardLayouts.search(method, showsNumberRow: showsNumberRow).rows.count
                    == SystemKeyboardLayouts.letters(method, showsNumberRow: showsNumberRow).rows.count
            )
        }
    }

    @Test("Search drops the slash key for the stock action row", arguments: KeyboardInputMethod.allCases)
    func actionRow(method: KeyboardInputMethod) {
        let keys = SystemKeyboardLayouts.search(method).rows.last?.keys ?? []
        #expect(keys.map(\.label) == ["123", "", "Tiếng Việt", ""])
        #expect(keys.map(\.role) == [.symbols, .emoji, .space, .enter])
        #expect(keys[2].horizontalSwipeAction == .toggleLanguage)

        // The Funput preset keeps its slash and period keys for URL-ish searches.
        let funput = KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: .letters,
            editorMode: .search,
            preset: .funput
        )
        #expect(funput.rows.last?.keys.map(\.label) == ["?123", "/", "Tiếng Việt", ".", ""])
    }

    @Test("Search and letters stay distinct layouts", arguments: KeyboardInputMethod.allCases)
    func distinctIdentities(method: KeyboardInputMethod) {
        // Same picture, different identity: the renderer rebuilds on layout inequality,
        // and the two must not be confusable when the focused field changes.
        let search = SystemKeyboardLayouts.search(method)
        #expect(search.id != SystemKeyboardLayouts.letters(method).id)
        #expect(search.id != KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: .letters,
            editorMode: .search,
            preset: .funput
        ).id)
        #expect(Set(search.rows.flatMap(\.keys).map(\.id)).count
            == search.rows.flatMap(\.keys).count)
    }
}
