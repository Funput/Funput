struct InputTransactionBuilder {
    private(set) var mutations: [DocumentMutation] = []

    var isEmpty: Bool { mutations.isEmpty }

    mutating func deleteBackward(count: Int = 1) {
        guard count > 0 else { return }
        if case let .deleteBackward(previous)? = mutations.last {
            mutations[mutations.count - 1] = .deleteBackward(
                count: previous + count
            )
        } else {
            mutations.append(.deleteBackward(count: count))
        }
    }

    mutating func insert(_ text: String) {
        guard !text.isEmpty else { return }
        if case let .insert(previous)? = mutations.last {
            mutations[mutations.count - 1] = .insert(previous + text)
        } else {
            mutations.append(.insert(text))
        }
    }
}
