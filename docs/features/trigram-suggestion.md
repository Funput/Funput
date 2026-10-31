# Gợi ý từ theo ngữ cảnh hai chữ (trigram cá nhân)

## Trạng thái

Engine Rust đã hiện thực (bước 2); C ABI, JNI và hai nền tảng còn lại. Nối tiếp [context-suggestion.md](context-suggestion.md) (bigram cá
nhân) và [vietnamese-seed-suggestion.md](vietnamese-seed-suggestion.md) (dữ liệu kèm app). Như hai
tài liệu đó, đây là nơi mọi quyết định thiết kế sống: chỗ nào hiện thực lệch khỏi bản viết này thì
sửa lại ngay trong PR đó và đánh dấu **Đã đổi khi hiện thực**.

Bước dọn đường đã có PR riêng: snapshot ghi cả cạnh đã chết, nên sau khi khởi động lại một cạnh tới
từ bị đuổi trỏ sang từ đang thuê slot đó (#664). Bảng trigram dùng lại đúng luật generation này, nên
lỗi phải sửa trước.

## Mục tiêu

Predict hôm nay nhìn **một âm tiết** đứng trước. Với tiếng Việt như vậy là chưa tới một từ:
`cảm ơn` là hai đơn vị, nên sau `ơn` engine chỉ biết "chữ sau `ơn`", không biết "chữ sau `cảm ơn`".

Mục tiêu là nhìn **hai âm tiết**, xấp xỉ một từ tiếng Việt:

| Gõ xong | Bigram nhìn | Trigram nhìn | Đoán |
|---|---|---|---|
| `cảm ơn` | `ơn` | `cảm ơn` | `bạn` |
| `không có` | `có` | `không có` | `gì` |
| `bởi vì` | `vì` | `bởi vì` | `vậy` |

Giữ nguyên ba ràng buộc của bigram: không chạm đường gõ của `funput-core`, trần bộ nhớ tĩnh tính
được bằng số, và nếu phải đánh đổi với cảm giác gõ thì tính năng này thua.

**Mở cho 4-gram.** Bậc là một tham số. Thêm bậc 4 là thêm một bảng cùng kiểu và một mục trong vòng
backoff, rồi đo bộ nhớ và tỉ lệ đoán đúng. Không phải viết lại phần nào.

## Không thuộc phạm vi

- **Trigram kèm app.** Đợt này chỉ học từ người dùng. Người mới cài không có trigram nào và rơi về
  bigram cá nhân, rồi về `data/pairs/vi.txt`, tức không tệ hơn hôm nay. Trigram có sẵn phải sinh từ
  corpus (viết tay không đủ sức), nên chỉ làm sau khi chốt được nguồn và giấy phép. Xem
  "Mở rộng sau này".
- Mô hình nơ-ron, ngữ cảnh dài hơn `MAX_CONTEXT`, học từ văn bản người dùng không tự gõ, đồng bộ
  giữa thiết bị.

## Bậc là tham số

```rust
/// Số chữ ngữ cảnh lớn nhất mà engine dùng. 2 là trigram; 3 sẽ là 4-gram.
pub const MAX_CONTEXT: usize = 2;
```

- **Bậc 2 (bigram) giữ nguyên trong `WordRecord`.** Không migration, không đổi đường đang chạy.
- **Mỗi bậc từ 3 trở lên là một `NgramTable`.** Cùng một kiểu, khác số chữ trong khoá. Trigram là
  bảng có khoá hai chữ.
- Truy vấn đi từ bậc cao nhất có đủ ngữ cảnh xuống bậc 2, rồi tới dữ liệu kèm app (mục "Truy vấn và
  backoff").

## Nơi lưu: `NgramTable`

Không nhét được vào `WordRecord` như bigram: khoá là một *cặp* chữ, mà số cặp thì lớn hơn số chữ
nhiều lần. Nên bậc cao có bảng riêng, nhưng giữ đúng các tính chất đã làm bigram an toàn.

```rust
/// Một chữ trong khoá: slot của nó và generation lúc ghi.
/// Cùng luật với `Follower::word`: lệch generation là chết.
pub(crate) struct WordRef { index: u32, generation: u16 }

pub(crate) struct Entry<const K: usize> {
    key: [WordRef; K],                       // cũ → mới
    context_seen: u16,
    challenges: u16,                         // xem "Bảng đầy"; nằm trong phần padding
    followers: [Follower; FOLLOWER_SLOTS],   // FOLLOWER_SLOTS = 4, như bigram
}

pub(crate) struct NgramTable<const K: usize> {
    entries: Box<[Entry<K>]>,   // dung lượng cố định, cấp một lần
}
```

- **Băm địa chỉ mở, dung lượng cố định.** Băm nhân trên `(index, generation)` của các chữ trong
  khoá, lấy **các bit cao** làm ô bắt đầu (bit thấp của một tích chỉ thấy bit thấp của đầu vào,
  nên generation ở bit 32 trở lên sẽ không bao giờ tới được). Tra là băm rồi dò tuyến tính trong một cửa sổ có giới hạn (`PROBE_WINDOW`, khởi điểm 8). Không cấp
  phát khi tra, không cấp phát khi học.
- **Cấp lười.** Bảng chỉ cấp ở lần ghi trigram đầu tiên, nên người dùng mới hoặc tắt trigram không
  tốn byte nào.
- **Nạp slot và lão hoá dùng lại nguyên `bigram/slots.rs`.** `slots::record` đã nhận
  `&mut [Follower; FOLLOWER_SLOTS]` và `&mut u16`, không biết gì về `WordRecord`, nên một `Entry` gọi
  thẳng vào được. Luật Misra-Gries, `AGING_AFTER = 256` và giới hạn `u16` giữ y nguyên.
- **Bảng đầy.** Trong cửa sổ dò, lấy theo thứ tự: ô trống, rồi ô chết (một chữ trong khoá lệch
  generation), rồi ô có `context_seen` thấp nhất. O(`PROBE_WINDOW`), không quét toàn bảng.
  **Đã đổi khi hiện thực: ô yếu nhất không nhường ngay.** Bản đầu cho cụm mới chiếm ngay ô ít gặp
  nhất, nên khi cửa sổ toàn thói quen thì một câu gõ một lần cũng đẩy được thói quen ra. Giờ dùng
  đúng luật thách đấu của follower: mỗi cụm mới cộng một vào `challenges` của ô yếu nhất, và chỉ
  chiếm chỗ khi `challenges` bằng `context_seen` của ô đó; ô đó được gặp lại thì `challenges` về 0.
  `challenges` không ghi xuống đĩa. Nạp từ snapshot hay chèn lại sau khi xoá từ thì chỉ đẩy được ô
  ít gặp hơn chính nó.
- **Đuổi từ trên đường học không quét bảng.** Giống bigram: `upsert_word` tăng generation, và mọi
  ô có chữ đó trong khoá hay trong follower tự thành chết. Lý do y như bigram: quét dọn giữa luồng gõ
  là thứ tài liệu bigram đã cam kết không làm.

### Ngân sách bộ nhớ

Một `Entry<2>` khoảng 52 byte (khoá 16, đếm 2, follower 32, cộng padding). Cấu hình
`ngram_contexts` mặc định **4096 ô ≈ 208 KiB** mỗi bậc, kẹp trong `sanitize`. So với bigram
(256 KiB đo thật) và trần retained heap 4 MiB của `tests/stress.rs`, vẫn dư.

Con số 4096 là ước lượng: gõ vài chục nghìn chữ thì phần lớn cặp chỉ gặp một lần, và luật nạp ô sẽ
đẩy chúng ra. Đo lại trên extension iOS ở bước cuối.

**Đã đổi khi hiện thực**: test gác thay cho lời hứa, trong `src/tests/ngram/memory.rs`:
`size_of::<Entry<2>>() <= 52` và bảng mặc định ≤ 256 KiB; chưa học trigram nào thì bảng chưa cấp
byte nào; 50.000 lần học trên bảng 64 ô không làm bảng lớn thêm một byte; `reset()` trả lại bộ nhớ.
`ngram_contexts` kẹp trong `8..=65536` và làm tròn lên luỹ thừa 2.

## Học

```text
learn_after(context = [a, b], token = c):
    như hôm nay: học c, ghi cạnh bigram b → c
    nếu a và b đều còn sống trong kho, và bậc 3 đang bật:
        ô = bảng trigram tìm hoặc chiếm khoá [a, b]
        slots::record(ô.followers, ô.context_seen, c)
```

**Đã đổi khi hiện thực: chữ `a` lấy từ journal, không do nền tảng truyền.** `learn_after(prev,
token)` giữ nguyên chữ ký. `JournalChain` (`src/ngram/chain.rs`, thay `journalled_previous`) nhớ hai
chữ vừa ghi journal liền mạch. Trigram `[a, b] → c` được học **đúng khi** `c` nối với `b` và `b` đã
nối với `a` trong journal, tức đúng khi replay dựng lại được nó. Nhờ vậy:

- replay **không bao giờ bịa** một trigram, và khi bảng còn dư chỗ thì bảng đọc lại sau một lần
  crash **bằng đúng** bảng trong bộ nhớ (proptest `the_table_on_disk_is_the_table_in_memory`).
  Khi bảng đầy, bảng nạp lại xếp ô theo chỗ mới, nên cụm mới sau đó có thể thách đấu một ô khác
  với trước khi crash: khác nhau ở chỗ trigram nào còn được giữ, không bao giờ ở chỗ có bịa ra
  trigram hay không (proptest `a_full_table_only_ever_holds_typed_trigrams`);
- nền tảng không phải đổi đường học, chỉ đường truy vấn cần hai chữ;
- cái giá: sau mỗi `compact` chuỗi bắt đầu lại, nên mất một hai trigram ở chỗ đó.

- Chữ bị chặn không bao giờ được học (đã gác sẵn ở `learn_inner`), nên cũng không vào khoá hay
  follower của trigram nào.
- Token bị `Ignored` thì dừng như hôm nay, không ghi gì ở bậc nào.
- Ngữ cảnh dài hơn `MAX_CONTEXT` thì engine chỉ lấy `MAX_CONTEXT` chữ gần nhất.

## Truy vấn và backoff

```text
predict(context, prefix rỗng):
    với từng bậc từ cao xuống 2, nếu context đủ dài cho bậc đó:
        Speaks(w)   -> trả w, dừng
        Unsure      -> trả rỗng, dừng
        NoEvidence  -> xuống bậc dưới
    mọi bậc cá nhân đều NoEvidence -> dữ liệu kèm app (seed_after, chữ cuối), như hôm nay
```

**`Unsure` ở bậc cao cũng dừng.** Bậc cao nói "ngữ cảnh hai chữ này đã gặp đủ nhiều và nó bẹt". Đó
là bằng chứng cụ thể hơn bậc dưới, nên lùi xuống bậc 2 để kiếm một dự đoán là ngược với ngưỡng áp
đảo: thà im còn hơn đoán bừa. Chỉ `NoEvidence` mới lùi, giữ đúng luật ở
`src/bigram/query/mod.rs` hôm nay.

**Đường prefix (Rerank).** Follower bậc cao lên trước, rồi follower bậc 2, rồi kết quả prefix; bỏ
trùng, lọc bằng `marks::completes` như hôm nay, cắt ở `TOP_K`. `yields_to` và phần dữ liệu kèm app
không đổi.

**Ngưỡng.** Dùng chung `context_rerank_uses`, `context_predict_uses`,
`context_dominance_percent`, `context_evidence_seen` cho mọi bậc. Thêm hai khoá vào
`SuggestionConfig`, kẹp trong `sanitize`:

```rust
context_order: u8,       // 2..=MAX_CONTEXT + 1, mặc định 3; 2 là chỉ bigram như hôm nay
ngram_contexts: usize,   // số ô mỗi bảng bậc cao, mặc định 4096
```

Ngưỡng riêng cho từng bậc chỉ thêm khi đo trên máy thật cho thấy cần.

**Tìm slot của chữ ngữ cảnh.** `context_slot` hôm nay là một lượt quét tuyến tính `words` mỗi phím.
Trigram cần hai chữ, tức hai lượt. Nếu bench nhóm `suggestions/predict` cho thấy chậm đi rõ rệt thì
tra qua trie chính xác (O(độ dài chữ)) thay vì quét.

## Xoá, chặn, giảm sức chứa

Đây là các thao tác hiếm (người dùng sửa từ điển, hay lúc nạp), không nằm trên đường gõ, nên được
phép quét bảng.

- `remove_slot` (quên một từ, chặn một từ, `enforce_capacity`) gọi thêm một lượt quét mỗi
  `NgramTable`: bỏ ô có chữ bị xoá trong khoá, bỏ follower trỏ tới nó và trừ `uses` của nó khỏi
  `context_seen` (như `slots::relocate`), đổi đích chữ bị dời chỗ do `swap_remove`.
- `enforce_capacity` lúc nạp có thể xoá cả nghìn từ. Gom thành **một** lượt quét bảng sau khi xoá
  xong, không phải mỗi từ một lượt. **Đã đổi khi hiện thực**: bảng bị **xoá sạch** khi phải bỏ bớt
  chữ. Kho chỉ co lại khi một bản build hạ `max_words`; dời chỉ số cho cả bảng sau một loạt
  `swap_remove` cần bảng ánh xạ riêng, trong khi trigram học lại được. Chữ và cặp bigram giữ nguyên.
- Khoá có chữ bị dời chỗ thì băm ra chỗ khác, nên ô đó được nhấc ra rồi chèn lại sau lượt quét
  (`src/ngram/table/relocate.rs`).
- `reset()` xoá luôn các bảng và trả lại bộ nhớ.
- Mọi edit vẫn cắt chuỗi ngữ cảnh của journal (`JournalChain::clear`), như hôm nay.

## Lưu trữ

**Journal không đổi định dạng.** Cờ `chained` trên mỗi token đã đủ dựng lại chuỗi: token `c` nối
với `b`, và `b` nối với `a`, thì replay có trigram `[a, b] → c`. Luật cũ giữ nguyên: replay có thể
**bỏ sót** một cạnh, không bao giờ **bịa** một cạnh sai. **Đã đổi khi hiện thực**: replay không đổi
một dòng nào, vì học trigram đã dựa trên chính chuỗi journal (mục "Học").

**Snapshot v4.** Thêm các khối bảng sau danh sách từ:

```text
u8 order · u32 byte_len · u32 count · count × (K × u32 key · u16 context_seen · u8 n · n × (u32 word, u16 uses))
```

- Chỉ ghi ô mà mọi chữ trong khoá còn sống, và chỉ ghi follower còn sống. Đây đúng là bài học ở
  #664: thứ gì đã chết mà vẫn ghi thì nạp lại ở generation 0 sẽ sống lại sai chỗ.
- `byte_len` cho phép bản đọc bỏ qua khối của bậc nó không biết, nên thêm 4-gram sau này không phải
  tăng version lần nữa.
- Đọc v1 đến v3 thì bảng rỗng; lần `compact` sau tự nâng tệp. Hạ cấp app gặp v4 thì `TooNew`, giữ
  nguyên tệp như mọi lần nâng version trước.
- 4096 ô ≈ 140 KiB trên đĩa, trong trần 2 MiB của snapshot.
- Bảng phải nạp **trước** `enforce_capacity`, vì bước đó dời chỉ số từ.
- Khối trigram ghi ngữ cảnh và follower bằng đúng bộ mã của bản ghi chữ (`encode_edges`,
  `decode_edges` trong `snapshot/words.rs`).

## Hợp đồng ngữ cảnh và binding

Crate vẫn không tự giữ ngữ cảnh: nền tảng truyền vào, cũ → mới.

**Rust.** **Đã đổi khi hiện thực**: không có kiểu `Context<'a>`; suy kiểu cho `None` qua
`impl Into<…>` dễ vỡ ở chỗ gọi. Thay bằng `suggest_in(context: &[&str], prefix)`, cũ → mới, chỉ đọc
`MAX_CONTEXT` chữ cuối; `suggest_with(prev, prefix)` là `suggest_in(prev.as_slice(), prefix)`.
Không có breaking change. Học không cần hàm mới (mục "Học").

Chữ ngữ cảnh được tìm trong `context_slots` (`src/ngram/context.rs`): chữ cuối trước, chữ trước nó
chỉ khi chữ cuối có trong kho. Một lượt quét gộp cả hai chữ đã được đo và không nhanh hơn.

Đo bằng `benches/suggestions.rs` trên kho 5000 chữ:

| Bench | Trước trigram | Sau trigram |
|---|---|---|
| `predict/speaks` (một chữ ngữ cảnh) | 3,92 µs | 3,92 µs |
| `lookup/with-context/exact-medium` | 2,92 µs | 2,77 µs |
| `predict/trigram` (hai chữ ngữ cảnh) | không có | 7,86 µs |

Đường học: `learn/chained` (bậc 2) 8,17 µs, `learn/trigram` (bậc 3) 8,19 µs, tức bảng trigram gần
như không tốn gì thêm khi học. Ngữ cảnh hai chữ tốn thêm đúng một lượt quét `words`, và chỉ khi bậc 3 đang bật: với
`context_order = 2` chữ trước chữ cuối không được tìm. Toàn bộ chạy trên worker nền của nền tảng,
không trên luồng gõ, và không cấp phát.

**C ABI.** Thêm `funput_suggestion_query_in`. Hàm hiện có giữ nguyên ABI; `funput.h` tạo lại bằng
`crates/funput-ffi/scripts/gen-header.sh`.

**JNI.** `nativeQueryIn(handle, context: String, prefix)`.

**Đã đổi khi hiện thực: ngữ cảnh là một chuỗi, các chữ cách nhau bằng dấu cách**, cũ → mới
(`"cảm ơn"`), thay vì một mảng `{ptr, len}` hay `Array<String>`. Token đã học không bao giờ chứa dấu
cách, nên tách lại là chính xác. Swift và Kotlin chỉ cần nối chuỗi, không phải giữ nhiều con trỏ
cùng lúc hay tạo mảng mỗi phím, và 4-gram sau này không phải đổi chữ ký. Việc tách nằm trong crate
(`suggest_after_words`, không cấp phát), nên C ABI và JNI chỉ chuyển chuỗi qua.

**Nền tảng.** `AuthoredTokenTracker` ở iOS và Android giữ một vòng `MAX_CONTEXT` chữ thay vì một chữ
**cho truy vấn**, cắt ở đúng các ranh giới đang có: dấu câu không phải dấu cách, xoá lùi qua ranh
giới, đổi ô nhập, reset. Đường học giữ nguyên. Ranh giới câu vẫn do nền tảng quyết định như tài
liệu bigram đã chốt.

## Bố cục mã

`scripts/check-loc.sh`: tối đa 150 dòng mỗi tệp, tối đa 5 tệp mỗi thư mục.

**Đã đổi khi hiện thực**:

- `src/types.rs`: `WordRef`, dùng chung cho cạnh bigram, khoá trigram và việc xoá từ.
- `src/ngram/`: `key.rs` (băm), `context.rs` (tìm chữ ngữ cảnh), `chain.rs` (`JournalChain`),
  `table/` (`mod.rs` ghi và luật thách đấu, `probe.rs` dò cửa sổ, `entry.rs`, `relocate.rs`).
- Học và truy vấn ở lại `src/bigram/`: `learning.rs` ghi cả hai bậc; `query/predict.rs` có `judge`
  dùng chung cho mọi bậc và vòng lùi bậc; `query/rerank.rs` gộp follower theo bậc.
- `src/persistence/codec/snapshot/ngram.rs`: khối bảng.
- `src/dictionary/edit/removal.rs`: thêm lượt quét bảng.
- `src/engine/policy/`: `admission.rs` (có sẵn) và `capacity.rs` (chính sách nhường chỗ, tách khỏi
  `engine/mod.rs` để giữ dưới 150 dòng), để `engine/` giữ đúng 5 tệp.
- Test: `src/tests/ngram/` (5 tệp); test của bảng nằm ngay trong `ngram/table/mod.rs`. Để mọi thư
  mục test giữ tối đa 5 tệp, test của kho cá nhân chuyển vào `src/tests/store/` và test lexicon gắn
  vào engine chuyển vào `src/tests/lexicon/attached/`.

## Kiểm thử và cổng gác

- **Bảng:** dò và chiếm ô, chọn ô khi đầy (trống, chết, yếu nhất), ô chết khi chữ trong khoá bị đuổi.
- **Học:** `[cảm, ơn] → bạn` ghi đúng ô; chữ bị chặn không vào bảng; ngữ cảnh một chữ không ghi bậc 3.
- **Backoff:** ma trận `Speaks`/`Unsure`/`NoEvidence` ở bậc 3 với bậc 2 và dữ liệu kèm app; riêng ca
  `Unsure` ở bậc 3 không được lùi.
- **Rerank:** follower bậc 3 đứng trước follower bậc 2.
- **Xoá:** mở rộng proptest `removal.rs` (mọi ô và follower còn sống chỉ trỏ tới chữ còn sống).
- **Lưu trữ:** mở rộng proptest `persistence/eviction.rs` (#664) sang trigram: compact rồi mở lại
  giữ đúng tập trigram còn sống; replay journal dựng lại trigram từ chuỗi `chained`, và không dựng
  qua chỗ edit cắt chuỗi; nạp snapshot v3 thành công với bảng rỗng.
- **Cấu hình:** `context_order = 2` cho đúng hành vi hôm nay (chạy lại toàn bộ test bigram).
- **`tests/alloc_budget.rs`:** truy vấn có ngữ cảnh hai chữ, cả Predict lẫn Rerank, 0 cấp phát khi
  ấm.
- **Bench:** `suggestions/predict/trigram` (ngữ cảnh hai chữ, trigram nói) và
  `suggestions/learn/trigram` (mỗi lần học đều ghi một trigram). So với nhánh nền như các PR trước.

## Thứ tự hiện thực

Mỗi bước một PR, xếp chồng:

1. #664: snapshot bỏ cạnh chết (đã mở).
2. Engine Rust: `NgramTable`, học, xoá, snapshot v4, backoff, cấu hình, test và bench.
3. C ABI và JNI (chỉ đường truy vấn), `funput_suggestion_query_in` và `nativeQueryIn`.
4. Android: tracker giữ vòng hai chữ (`older` và `queryContext` trên `AuthoredSuggestionUpdate`), service
   giữ bản cập nhật cuối của tracker, engine truy vấn qua `nativeQueryIn`.
5. iOS: như Android. `KeyboardSuggestionInputUpdate` có `older` và `queryContext`, service giữ bản
   cập nhật cuối, `PersonalSuggestionEngine.query(_:after:)` nhận các chữ cách nhau bằng dấu cách và
   gọi `funput_suggestion_query_in`.
6. Đo trên máy thật: bộ nhớ extension iOS, tỉ lệ đoán, `ngram_contexts`, `PROBE_WINDOW`.

## Mở rộng sau này

1. **4-gram.** Đổi `MAX_CONTEXT` thành 3, thêm `NgramTable<3>` vào vòng backoff và một khối snapshot
   với `order = 4`. Chỉ bật khi đo được tỉ lệ đoán đúng tăng đủ để bù bộ nhớ: dữ liệu cá nhân càng
   dài chuỗi càng thưa.
2. **Trigram kèm app từ corpus.** Một bước mới trong `funput-lexicon-tool` đếm bộ ba âm tiết, giữ bộ
   ba "nhọn", ghi ra file gọn. Nối vào đúng chỗ dữ liệu kèm app trong vòng backoff. Chốt nguồn và
   giấy phép trước: Wikipedia là CC BY-SA, nên file sinh ra có thể phải theo cùng giấy phép.
3. **Ngưỡng riêng từng bậc**, nếu đo cho thấy bậc 3 cần ít bằng chứng hơn để nói.
