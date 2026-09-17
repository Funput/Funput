import KeyboardLayout
import Testing

struct SystemSearchParityTests {
    @Test("Search renders like the letters page", arguments: KeyboardInputMethod.allCases)
    func matchesTheLettersPage(method: KeyboardInputMethod) {
        // On the stock keyboard the two are the same picture; the magnifying glass on the
        // return key comes from the enter action, so no key here differs.
        let search = SystemKeyboardLayouts.search(method).rows
        let letters = SystemKeyboardLayouts.letters(method).rows
        #expect(search.count == letters.count)
        for (searchRow, lettersRow) in zip(search, letters) {
            #expect(searchRow.keys.map(\.label) == lettersRow.keys.map(\.label))
            #expect(searchRow.keys.map(\.role) == lettersRow.keys.map(\.role))
            #expect(searchRow.keys.map(\.widthWeight) == lettersRow.keys.map(\.widthWeight))
            #expect(searchRow.horizontalInsetUnits == lettersRow.horizontalInsetUnits)
        }
    }

    @Test("Search keeps the number row whatever the preference", arguments: KeyboardInputMethod.allCases)
    func numberRowSurvivesThePreference(method: KeyboardInputMethod) {
        // The Funput preset's search layout always carries digits. Reusing the letters
        // rule would take them away from a Telex user who turned the row off, which is a
        // capability they have today — search is where digits are most likely wanted.
        for showsNumberRow in [true, false] {
            let layout = KeyboardLayoutResolver.resolve(
                inputMethod: method,
                mode: .letters,
                editorMode: .search,
                showsNumberRow: showsNumberRow,
                preset: .system
            )
            #expect(layout.rows.count == 5)
            #expect(layout.rows[0].keys.map(\.label).joined() == "1234567890")
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
