# Gợi ý từ theo ngữ cảnh hai chữ (trigram cá nhân)

## Trạng thái

Bản thiết kế, chưa có code. Nối tiếp [context-suggestion.md](context-suggestion.md) (bigram cá
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
    followers: [Follower; FOLLOWER_SLOTS],   // FOLLOWER_SLOTS = 4, như bigram
}

pub(crate) struct NgramTable<const K: usize> {
    entries: Box<[Entry<K>]>,   // dung lượng cố định, cấp một lần
}
```

- **Băm địa chỉ mở, dung lượng cố định.** Băm trên `(index, generation)` của các chữ trong khoá.
  Tra là băm rồi dò tuyến tính trong một cửa sổ có giới hạn (`PROBE_WINDOW`, khởi điểm 8). Không cấp
  phát khi tra, không cấp phát khi học.
- **Cấp lười.** Bảng chỉ cấp ở lần ghi trigram đầu tiên, nên người dùng mới hoặc tắt trigram không
  tốn byte nào.
- **Nạp slot và lão hoá dùng lại nguyên `bigram/slots.rs`.** `slots::record` đã nhận
  `&mut [Follower; FOLLOWER_SLOTS]` và `&mut u16`, không biết gì về `WordRecord`, nên một `Entry` gọi
  thẳng vào được. Luật Misra-Gries, `AGING_AFTER = 256` và giới hạn `u16` giữ y nguyên.
- **Bảng đầy.** Trong cửa sổ dò, lấy theo thứ tự: ô trống, rồi ô chết (một chữ trong khoá lệch
  generation), rồi ô có `context_seen` thấp nhất. O(`PROBE_WINDOW`), không quét toàn bảng.
- **Đuổi từ trên đường học không quét bảng.** Giống bigram: `upsert_word` tăng generation, và mọi
  ô có chữ đó trong khoá hay trong follower tự thành chết. Lý do y như bigram: quét dọn giữa luồng gõ
  là thứ tài liệu bigram đã cam kết không làm.

### Ngân sách bộ nhớ

Một `Entry<2>` khoảng 52 byte (khoá 16, đếm 2, follower 32, cộng padding). Cấu hình
`ngram_contexts` mặc định **4096 ô ≈ 208 KiB** mỗi bậc, kẹp trong `sanitize`. So với bigram
(256 KiB đo thật) và trần retained heap 4 MiB của `tests/stress.rs`, vẫn dư.

Con số 4096 là ước lượng: gõ vài chục nghìn chữ thì phần lớn cặp chỉ gặp một lần, và luật nạp ô sẽ
đẩy chúng ra. Ghi số đo thật vào đây khi hiện thực xong, đặt trần trong test như
`the_shipped_tables_stay_small`, rồi đo lại trên extension iOS ở bước cuối.

## Học

```text
learn_after(context = [a, b], token = c):
    như hôm nay: học c, ghi cạnh bigram b → c
    nếu a và b đều còn sống trong kho, và bậc 3 đang bật:
        ô = bảng trigram tìm hoặc chiếm khoá [a, b]
        slots::record(ô.followers, ô.context_seen, c)
```

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
  xong, không phải mỗi từ một lượt.
- `reset()` xoá luôn các bảng.
- Mọi edit vẫn cắt chuỗi ngữ cảnh của journal (`journalled_previous`), như hôm nay.

## Lưu trữ

**Journal không đổi định dạng.** Cờ `chained` trên mỗi token đã đủ dựng lại chuỗi: token `c` nối
với `b`, và `b` nối với `a`, thì replay có trigram `[a, b] → c`. Replay giữ hai token gần nhất thay
vì một, edit vẫn cắt chuỗi. Luật cũ giữ nguyên: replay có thể **bỏ sót** một cạnh, không bao giờ
**bịa** một cạnh sai.

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

## Hợp đồng ngữ cảnh và binding

Crate vẫn không tự giữ ngữ cảnh: nền tảng truyền vào, cũ → mới.

**Rust.** Một kiểu `Context<'a>` (tối đa `MAX_CONTEXT` chữ, dài hơn thì giữ phần cuối), có
`From<Option<&str>>` và `From<&[&str]>`. `learn_after` và `suggest_with` nhận
`impl Into<Context<'a>>`, nên mọi chỗ gọi `learn_after(Some("xin"), …)` hôm nay vẫn biên dịch,
không có breaking change.

**C ABI.** Thêm `funput_suggestion_learn_in` và `funput_suggestion_query_in`, nhận một mảng
`{ptr, len}` UTF-32 cũ → mới. Hai hàm hiện có thành lớp mỏng gọi sang với một chữ, ABI cũ giữ
nguyên. Tạo lại `funput.h` và bản header trong `FunputCore.xcframework`.

**JNI.** `nativeLearnIn(handle, context: Array<String>, token)` và `nativeQueryIn(handle, context,
prefix)`. Phía Kotlin giữ sẵn mảng, không tạo mảng mới mỗi phím.

**Nền tảng.** `AuthoredTokenTracker` ở iOS và Android giữ một vòng `MAX_CONTEXT` chữ thay vì một chữ,
cắt ở đúng các ranh giới đang có: dấu câu không phải dấu cách, xoá lùi qua ranh giới, đổi ô nhập,
reset. Ranh giới câu vẫn do nền tảng quyết định như tài liệu bigram đã chốt.

## Bố cục mã

`scripts/check-loc.sh`: tối đa 150 dòng mỗi tệp, tối đa 5 tệp mỗi thư mục.

- `src/ngram/`: `key.rs` (`WordRef`, băm), `table.rs` (`NgramTable`, dò và chọn ô), `learning.rs`,
  `query.rs` (predict và rerank theo bậc), `mod.rs`.
- `src/bigram/query/mod.rs` thành chỗ chạy vòng backoff, gọi sang `ngram::query`.
- `src/persistence/codec/snapshot/ngram.rs`: khối bảng.
- `src/dictionary/edit/removal.rs`: thêm lượt quét bảng.

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
- **Bench:** `suggestions/predict` thêm ngữ cảnh hai chữ, `suggestions/learn` thêm học có trigram,
  `suggestions/persistence/compact_5000` với bảng đầy. So xen kẽ với `main` như các PR trước.

## Thứ tự hiện thực

Mỗi bước một PR, xếp chồng:

1. #664: snapshot bỏ cạnh chết (đã mở).
2. Engine Rust: `Context`, `NgramTable`, học, xoá, snapshot v4, replay, backoff, cấu hình, test và
   bench. `context_order` mặc định 3 chỉ bật ở PR này khi bench đã sạch.
3. C ABI và JNI.
4. Android: tracker giữ vòng hai chữ, worker truyền ngữ cảnh.
5. iOS: như Android.
6. Đo trên máy thật: bộ nhớ extension iOS, tỉ lệ đoán, `ngram_contexts`, `PROBE_WINDOW`.

## Mở rộng sau này

1. **4-gram.** Đổi `MAX_CONTEXT` thành 3, thêm `NgramTable<3>` vào vòng backoff và một khối snapshot
   với `order = 4`. Chỉ bật khi đo được tỉ lệ đoán đúng tăng đủ để bù bộ nhớ: dữ liệu cá nhân càng
   dài chuỗi càng thưa.
2. **Trigram kèm app từ corpus.** Một bước mới trong `funput-lexicon-tool` đếm bộ ba âm tiết, giữ bộ
   ba "nhọn", ghi ra file gọn. Nối vào đúng chỗ dữ liệu kèm app trong vòng backoff. Chốt nguồn và
   giấy phép trước: Wikipedia là CC BY-SA, nên file sinh ra có thể phải theo cùng giấy phép.
3. **Ngưỡng riêng từng bậc**, nếu đo cho thấy bậc 3 cần ít bằng chứng hơn để nói.
