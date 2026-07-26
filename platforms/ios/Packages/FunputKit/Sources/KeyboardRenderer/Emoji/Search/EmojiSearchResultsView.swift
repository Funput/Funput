#if canImport(UIKit)
import UIKit

@MainActor
final class EmojiSearchResultsView: UIView, UICollectionViewDataSource, UICollectionViewDelegate {
    var onSelect: ((EmojiItem) -> Void)?
    private let layout = UICollectionViewFlowLayout()
    private let collectionView: UICollectionView
    private let emptyLabel = UILabel()
    private var items: [EmojiItem] = []
    private var expanded = false

    override init(frame: CGRect) {
        collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        super.init(frame: frame)
        collectionView.backgroundColor = .clear
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.register(EmojiCell.self, forCellWithReuseIdentifier: EmojiCell.reuseIdentifier)
        emptyLabel.font = .systemFont(ofSize: 14)
        emptyLabel.textAlignment = .center
        addSubview(collectionView)
        addSubview(emptyLabel)
        configureLayout()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        collectionView.frame = bounds
        emptyLabel.frame = bounds.insetBy(dx: 12, dy: 4)
    }

    func apply(items: [EmojiItem], emptyMessage: String, expanded: Bool, color: UIColor) {
        self.items = items
        emptyLabel.text = emptyMessage
        emptyLabel.textColor = color
        emptyLabel.isHidden = !items.isEmpty
        if self.expanded != expanded {
            self.expanded = expanded
            configureLayout()
        }
        collectionView.reloadData()
    }

    func numberOfSections(in collectionView: UICollectionView) -> Int {
        1
    }

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        items.count
    }

    func collectionView(
        _ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath
    ) -> UICollectionViewCell {
        guard let cell = collectionView.dequeueReusableCell(
            withReuseIdentifier: EmojiCell.reuseIdentifier, for: indexPath
        ) as? EmojiCell else { return UICollectionViewCell() }
        cell.apply(items[indexPath.item])
        return cell
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        onSelect?(items[indexPath.item])
    }

    private func configureLayout() {
        layout.scrollDirection = expanded ? .vertical : .horizontal
        layout.itemSize = CGSize(width: 44, height: 44)
        layout.minimumInteritemSpacing = 2
        layout.minimumLineSpacing = 2
        layout.sectionInset = UIEdgeInsets(top: 2, left: 8, bottom: 2, right: 8)
        layout.invalidateLayout()
    }
}
#endif
