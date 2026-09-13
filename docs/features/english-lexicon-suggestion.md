# Gợi ý từ điển tiếng Anh

## Trạng thái

**Bước 1 (dữ liệu), bước 2 (định dạng `en.lex`) và bước 3 (trộn vào `suggest_with`) đã xong; từ bước 4 (C ABI/JNI) chưa có code.** Tài liệu này chốt mô hình
trước khi hiện thực và được review riêng. Như [context-suggestion.md](context-suggestion.md), nó là nơi mọi quyết
định thiết kế sống: mỗi thay đổi hành vi sau này cập nhật lại nó trong cùng PR, chỗ
nào hiện thực lệch khỏi bản viết thì sửa lại và đánh dấu **Đã đổi khi hiện thực**.

Phần lõi nằm trong **`crates/funput-suggestions`**, iOS và Android dùng chung qua
`funput-ffi` và `funput-jni`. Các shell desktop không có thanh gợi ý nên không bị ảnh
hưởng.

## Mục tiêu

Người dùng gõ gì thì thanh gợi ý trả về top 3 từ **cả** kho cá nhân **lẫn** một từ
điển tiếng Anh kèm app — **không phân biệt chế độ VI hay EN**. Gõ `wor` thì có
`work · world · would` ngay từ lần cài đầu tiên, không phải gõ tay mỗi từ hai lần như
hôm nay.

Ràng buộc kế thừa nguyên từ gợi ý theo ngữ cảnh, không thương lượng:

1. **Không ảnh hưởng luồng gõ tiếng Việt.** Không thêm nhánh nào vào đường xử lý phím
   của `funput-core`, không chờ, không khoá.
2. **Không phình.** Trần kích thước tệp và trần bộ nhớ tính được bằng số, gác bằng
   test.
3. **Nice to have.** Nếu phải đánh đổi với cảm giác gõ, nó thua.

## Không thuộc phạm vi

- **Dạng rút gọn** (`don't`, `I'm`): cả hai `AuthoredTokenTracker` coi `'` là ranh
  giới từ, nên các dạng này không bao giờ là một prefix. Bị loại khỏi từ điển ở bản
  này; sửa tracker là việc riêng.
- **Từ tiếng Anh có ký tự ngoài ASCII** (`café`, `naïve`): loại, để phép so khớp
  trong từ điển chỉ là so byte ASCII không phân biệt hoa thường.
- Đoán từ tiếp theo bằng dữ liệu tĩnh · autocorrect · sửa chính tả · từ điển ngôn ngữ
  khác · tải từ điển qua mạng · từ điển tiếng Việt.

## Hai kho, trộn khi truy vấn

Từ điển là **tệp chỉ đọc** `en.lex`, tách hẳn khỏi kho cá nhân (`words`, trie,
snapshot, journal). Hai kho chỉ gặp nhau trong `suggest_with`.

Vì sao không nạp từ điển vào kho cá nhân:

| Chỗ vỡ | Lý do |
|---|---|
| Sức chứa | `max_words = 5000` có evict — từ điển đẩy từ người dùng ra hoặc bị đẩy ra |
| Đường học | `upsert_word` tìm tuyến tính; 30k từ làm mỗi `learn` chậm ~7 lần |
| Lưu trữ | Snapshot phình, dữ liệu công khai ghi vào vùng dữ liệu riêng tư |
| Nút xoá | "Xoá dữ liệu gợi ý" hoặc xoá mất từ điển, hoặc phải phân biệt từng từ |
| Cập nhật | Đổi từ điển thành migration kho cá nhân |
| Bộ nhớ | Mất mmap; trang nhớ thành dirty |

Tách riêng thì **`WordRecord`, snapshot và journal không đổi một byte nào**, và
`reset()` tự đúng: nó chỉ chạm kho cá nhân.

## Truy vấn

```text
suggest_with(prev, prefix):
    P = prefix_candidates(prefix) → with_context(prev)       // như hôm nay
    nếu kho đã là kho tiếng Việt và P có từ mang dấu tiếng Việt:
        trả P                                                 // nhường, xem mục dưới
    L = lexicon.top3(prefix)
    trả P, rồi lấp ô trống bằng L không trùng P (so eq_ignore_ascii_case)
```

**Đã đổi khi hiện thực.** Hiện thực ở `src/lexicon/merge.rs::complete_from_lexicon`,
gọi ở cuối `suggest_with` sau cả ba nhánh (không ngữ cảnh / rerank / predict). Bỏ
trùng bằng `eq_ignore_ascii_case` thay vì `normalize::exact`: từ cá nhân đã lưu chữ
thường NFC, từ điển là ASCII, nên hai phía chỉ có thể trùng khi đều là ASCII — so
không phân biệt hoa thường là đủ và không cấp phát (`iphone` học được và `iPhone` của
từ điển là một gợi ý). Rust có API công khai
`SuggestionEngine::attach_lexicon(path) -> io::Result<()>`: thành công thì thay từ
điển đang gắn; thất bại thì giữ nguyên cái cũ (hoặc không có). Trạng thái từ điển gom
trong một `LexiconSlot` (tệp đã mở + bộ đếm) để struct engine chỉ thêm một field.

**Cá nhân luôn đứng trước.** Từ người dùng tự gõ là tín hiệu mạnh nhất, và hai thang
đo — `uses` của kho cá nhân, thứ hạng tần suất của từ điển — không so với nhau được
nếu không bịa ra một bộ hằng số quy đổi. Luật lấp chỗ trống không cần hằng nào.

**Top-3 của từ điển là đủ.** Số ô trống là `3 − |P|`, còn số từ của L không trùng P ít
nhất là `3 − |P ∩ L| ≥ 3 − |P|`. Không bao giờ cần ứng viên thứ tư của từ điển.

**Vòng lặp tự cá nhân hoá.** Chấp nhận một gợi ý từ điển đi qua `completedToken` như
gõ tay: từ đó được học, sinh cặp bigram, và sau `promotion_uses` lần thì thành từ cá
nhân, đứng đầu. Không cần cơ chế riêng.

Không đổi:

- **Predict** (prefix rỗng) chỉ dùng bigram cá nhân. Từ điển không đoán từ tiếp theo —
  không cần nhánh riêng: `top3("")` vốn trả rỗng.
- **`MinimumPrefix = 2`** ở cả hai shell.
- Prefix có ký tự ngoài ASCII (`thà`, `đi`) thì `lexicon.top3` trả rỗng ngay ở byte
  đầu tiên không phải ASCII — không cần luật riêng cho tiếng Việt.

## Ngưỡng nhường: khi người dùng đã có kho tiếng Việt

Người mới cài chưa có kho cá nhân: từ điển lấp tự do, đó chính là giá trị của tính
năng. Người đã gõ tiếng Việt lâu thì khác — gõ `an` giữa câu tiếng Việt mà hai ô trống
hiện `and · any` là nhiễu.

**Kho tiếng Việt** là kho có ít nhất `lexicon_yield_after_words` từ **đã promoted và
mang dấu tiếng Việt** — tức `normalize::is_marked(w)` (`folded_chars(w) != w`), đúng
điều kiện `index_word` đang dùng để quyết định có chèn vào trie `folded` hay không;
`index_word` giờ trả luôn kết quả đó để không phải tính hai lần. Từ không dấu (`anh`,
`con`) không được đếm, nhưng người gõ tiếng Việt thật sẽ vượt ngưỡng rất nhanh nhờ
các từ có dấu.

**Nhường:** khi kho đã là kho tiếng Việt và **P có ít nhất một từ mang dấu**, từ điển
không lấp. Ngược lại vẫn lấp như bình thường.

| Prefix | P | Kết quả khi kho đã là kho tiếng Việt |
|---|---|---|
| `an` | `anh · ăn` | `anh · ăn` — có `ăn` mang dấu, nhường |
| `wo` | `work` | `work · would · world` — P không có từ mang dấu |
| `ha` | `hai` | `hai · have · had` — `hai` không dấu, không nhường |
| `xyz` | — | từ điển (nếu có) |

Phương án đã cân nhắc và loại: **nhường khi P khác rỗng.** Đơn giản hơn, nhưng một từ
tiếng Anh người dùng đã học (`work`) sẽ chặn luôn `world · would` — trừng phạt đúng
người dùng song ngữ mà tính năng này nhắm tới.

**Bộ đếm.** `LexiconSlot::vietnamese_words: u32`, không có lượt quét nào trên đường
gõ:

- `+1` trong `learn_inner` khi một từ mang dấu **vừa vượt** `promotion_uses`
  (`previous_uses < promotion_uses ≤ uses`), **trước** lần rebuild thỉnh thoảng của
  chính đường học — nếu không, rebuild đếm lại đã thấy từ đó và phép cộng thành đếm
  hai lần. **Đã đổi khi hiện thực**: bản viết ban đầu nói "khi trả `Promoted`", nhưng
  với `promotion_uses = 1` lần học đầu trả `Recorded` mà từ đã được promoted.
- `−1` khi `upsert_word` ghi đè một từ đã promoted mang dấu.
- **Đếm lại từ đầu trong `rebuild_tries`**, trong vòng lặp vốn đã duyệt toàn bộ
  `words`. Đây mới là thứ làm `open` đúng (từ nạp từ đĩa không đi qua promotion), và
  nó sửa mọi lệch pha mỗi khi trie được quét: lúc `open`, và lúc `flush` sau khi có
  evict. **Đã đổi khi hiện thực**: `enforce_capacity` không cần `−1` riêng — nó chỉ
  chạy trong `open`, ngay trước rebuild.
- `reset()` về 0.

Mỗi điểm trên đều có test bắt được nếu bị gỡ: bỏ `−1` thì proptest đếm-lại-sau-mỗi-bước
thất bại; bỏ đếm lại thì test mở lại từ đĩa thất bại; bỏ `reset` về 0 thì proptest thất
bại (đã thử bằng mutation).

Kiểm tra "P có từ mang dấu" là so `folded_chars` với `exact_chars` trên tối đa 3 từ,
mỗi từ ≤ 32 ký tự, cùng loại iterator mà `prefix_candidates` đang dùng với 0 cấp phát.

Cấu hình thêm một khoá (không cần kẹp — mọi giá trị `u32` đều có nghĩa):

```rust
lexicon_yield_after_words: u32,   // mặc định 200, 0 = luôn nhường
```

## Dữ liệu

### Nguồn

| Vai trò | Nguồn | Giấy phép |
|---|---|---|
| Danh sách từ được phép gợi ý | **SCOWL / ESDB** mức ≤ 60, en_US | Kiểu MIT, giữ thông báo bản quyền |
| Xếp hạng | **Google Books Ngram v3** (20200217), 1-gram tiếng Anh, năm 2000–2019 | CC BY 3.0 |
| Hiệu chỉnh | `supplement.tsv` tự soạn: thêm từ, và hạng sàn cho từ sách đếm thiếu | Của Funput, MIT |
| Chặn | **LDNOOBW** `en` + `blocklist.txt` tự soạn, có dòng `!word` cho phép lại | CC BY 4.0 / MIT |

Đã khảo sát và **loại**: từ điển AOSP LatinIME (NOTICE ghi *"© Lexiteria LLC. Used by
permission"* — giấy phép dành cho Google, không cho bên thứ ba); Norvig `count_1w`
(dữ liệu gốc Google Web 1T qua LDC, không có giấy phép phân phối lại); wordfreq và
FrequencyWords (dữ liệu CC BY-SA 4.0, và wordfreq đã ngừng cập nhật từ 2021); COCA
(thương mại).

SCOWL mức ≤ 60 là mức lớn nhất mà README của ESDB coi là không có lỗi chính tả. Dùng
nó làm danh sách được phép nghĩa là từ điển **không bao giờ gợi ý từ sai chính tả, rác
OCR hay từ lạ** — Google Books chỉ trả lời câu "từ nào hay dùng hơn", không trả lời
câu "từ nào tồn tại".

### Pipeline

```text
SCOWL ≤60 en_US, xuất kèm dấu chấm viết tắt (--dot True)
  → chỉ [A-Za-z], 2..=32 ký tự: tự loại dạng sở hữu, rút gọn, `Blvd.`, `café`
  → mỗi khoá chữ thường giữ một dạng chuẩn: chữ thường, rồi viết hoa đầu,
    rồi dạng còn lại (iPhone, USA); hoà thì theo thứ tự byte
Google Books 1-gram (stream, không lưu file thô)
  → bỏ token có gắn thẻ (_NOUN, _VERB…) và token không phải chữ ASCII,
    cộng match_count các năm 2000–2019, bỏ cách viết < 100 lần, gộp theo khoá chữ thường
+ supplement.tsv − (LDNOOBW ∪ blocklist.txt)
→ sắp theo tần suất giảm dần (hoà thì theo khoá), áp hạng sàn, cắt top N
→ data/lexicon/en.tsv          (word \t rank)          ← commit
→ en.lex                        (định dạng bên dưới)    ← sinh trong CI, không commit
```

**Đã đổi khi hiện thực — `supplement.tsv` có hạng sàn.** Bản viết ban đầu coi nó chỉ
là danh sách thêm từ văn nói (`ok`, `yeah`…). Đo xong thì SCOWL mức 60 đã có sẵn
`ok`, `yeah`, `gonna`, `email`, `app`; chỗ hỏng thật nằm ở **thứ hạng**: tokenizer của
Google tách `cannot` thành `can not` (sách xếp nó hạng ~43.700), và từ phổ biến sau
2019 như `COVID`, `emoji`, `selfie` rơi xuống hạng 50.000–87.000. Nên mỗi dòng là
`word` hoặc `word<TAB>rank`: từ đó được đặt không thấp hơn từ đang giữ hạng `rank`
trong thứ tự chỉ-theo-sách — **mượn số đếm thật** của từ ở hạng đó chứ không bịa ra
một con số.

**Đã đổi khi hiện thực — blocklist cho phép lại từ trung tính.** Nhập nguyên LDNOOBW
chặn 67 từ trong top 30k, trong đó có cả từ mà câu hỏi sức khoẻ, bài báo hay bài
luận ở trường cần tới: `sexual`, `rape`, `penis`, `intercourse`, `domination`,
`escort`, `suck`. Quyết định: **giữ phần nhập nguyên văn** (đối chiếu được với commit
gốc) và thêm dòng `!word` để cho phép lại từng từ mà nghĩa chính là y khoa, pháp lý,
lịch sử hay đời thường. Từ miệt thị, tiếng lóng tục và thuật ngữ khiêu dâm vẫn bị
chặn, kể cả những từ mà nghĩa thường gặp nhất chính là nghĩa tục (`ass`, `cock`,
`dick`). Một dòng `!word` không khớp từ nào bị chặn là lỗi, để lỗi gõ hay việc cập
nhật LDNOOBW không lặng lẽ vô hiệu hoá nó. Kết quả: 39 từ trở lại top 30k.

**Commit TSV, không commit dữ liệu thô.** PR đổi từ điển thành một diff đọc được:
từ nào vào, từ nào ra. CI chỉ chạy bước TSV → `.lex`, không tải vài GB Google Books.
Bước xếp hạng chạy tay khi cập nhật dữ liệu.

Tần suất **không vào app** — sau khi sắp xong nó chỉ còn là số thứ tự `rank`.

Bố trí trong repo:

```text
crates/funput-suggestions/data/lexicon/
  en.tsv            // đầu ra đã xếp hạng, nguồn duy nhất của en.lex
  supplement.tsv
  blocklist.txt
  NOTICE.md         // ghi công SCOWL, Google Books Ngram, LDNOOBW
```

Công cụ xếp hạng (Google Books + SCOWL → `en.tsv`) là crate **`funput-lexicon-tool`**
(`publish = false`), không phải dependency của thư viện. Nó có ba lệnh: `count` đọc
một shard 1-gram đã giải nén từ stdin, `rank` ghép mọi thứ thành `en.tsv`, `pack` biến
`en.tsv` thành `en.lex`; `crates/funput-lexicon-tool/refresh.sh` nối cả quy trình, bỏ
qua phần đã xong nên chạy lại được sau khi bị ngắt. Bộ mã hoá TSV → `.lex` và phép
xác minh nằm **trong** `funput-suggestions` sau feature `lexicon-build`
(`funput_suggestions::lexicon_build::{encode, verify}`); công cụ chỉ gọi chúng, nên
định dạng được định nghĩa đúng một chỗ — chỗ đọc nó — và `pack` xác minh tệp bằng
chính reader của thư viện trước khi ghi. CI gác để feature này không lọt vào
`funput-ffi` / `funput-jni`: build `--workspace` hợp nhất feature, còn các shell build
thư viện bằng `-p`.

### Ghi công

SCOWL yêu cầu thông báo bản quyền xuất hiện trong mọi bản sao và tài liệu kèm theo;
Google Books Ngram và LDNOOBW yêu cầu ghi nguồn. Khảo sát chưa thấy màn hình giấy phép
bên thứ ba nào trong app iOS hay Android — **phải thêm trước khi phát hành**, nội dung
lấy từ `NOTICE.md`.

## Định dạng `en.lex`

Đo trên `/usr/share/dict/american-english` (bản Debian của SCOWL): trie đầy đủ với
top-3 ở mỗi node cần ~68k node cho 30k từ, ≈ 980 KB. Phần lớn node thừa: một prefix
khớp ≤ 3 từ thì chính các từ đó là kết quả. Nên định dạng là **mảng sắp xếp + bảng
top-3 chỉ cho prefix nặng**:

```text
Header      magic "FPLX" · version u16 · flags u16 · word_count u32
            words_len u32 · heavy_count u32 · body_crc u32            // 24 byte
Block index ceil(word_count / 16) × u32   // offset vào Words, mỗi khối 16 từ
Words       word_count × { len u8 · rank u16 · bytes }   // sắp theo khoá chữ thường
Heavy       heavy_count × { lo u16 · plen u8 · top [u16; 3] }   // 9 byte, sắp theo (lo, plen)
```

**Đã đổi khi hiện thực — header 24 byte.** Bản viết ban đầu có `block_count` và
`data_version`. `block_count` suy ra được từ `word_count`, lưu thêm chỉ là thêm một chỗ
để hai con số lệch nhau. `data_version` bị thay bằng **`body_crc`** (CRC-32 của mọi
thứ sau header, dùng chung `crate::binary::checksum` với kho cá nhân): nó vừa là
checksum vừa là danh tính của một bản dữ liệu, và không ai phải nhớ tăng tay. Header
có thêm `words_len` để kích thước từng section suy ra được mà không phải duyệt.

`top3(prefix)`:

1. Prefix có byte ngoài ASCII hoặc dài < 2 → rỗng.
2. Binary search trên block index, quét ≤ 16 từ → `lo`, từ đầu tiên khớp prefix.
3. Từ `lo + 3` còn khớp prefix? **Không** → kết quả là ≤ 3 từ từ `lo`, xếp theo `rank`.
   **Có** → prefix nặng, binary search `(lo, plen)` trong Heavy, đọc 3 id.
4. Id → từ: nhảy tới khối `id / 16`, quét ≤ 15 từ.

O(log n) phép so, không cấp phát. Chuỗi trả về là `&str` mượn thẳng từ vùng nhớ của
tệp, sống cùng engine, nên `SuggestionSet` giữ nguyên `[Option<&str>; 3]`.

**Kiểm tra đầy đủ lúc nạp, không kiểm tra lúc tra.** `Lexicon::open` / `from_bytes`
(bước 3 gọi qua `attach_lexicon`) chạy `format/validate.rs` một lần, O(kích thước tệp),
trên worker; không có cách nào cầm một `Lexicon` chưa qua bước này. Nó kiểm tra: kích
thước ≤ 1 MiB (từ chối trước khi map), magic, `version` (mới hơn → `Unsupported`, như
kho cá nhân; cũ hơn → hỏng), `flags == 0`, tổng độ dài khớp đúng từng byte, `body_crc`;
mọi từ dài 2..=32 chỉ gồm chữ ASCII, `rank < word_count`, khoá chữ thường **tăng
nghiêm ngặt**, mỗi block offset trỏ đúng từ thứ `16k`; mọi mục Heavy tăng theo
`(lo, plen)`, `lo` đúng là chỗ run bắt đầu, run thật sự dài hơn 3, ba id khác nhau,
thuộc run và theo thứ tự rank. Một byte hỏng ngẫu nhiên đã bị CRC bắt; các kiểm tra
cấu trúc dành cho tệp **nhất quán mà vẫn sai**. Qua được bước đó thì đường tra không
thể đọc ra ngoài vùng nhớ; nó vẫn dùng `get` chứ không index trực tiếp, nên tệp hỏng
theo cách chưa nghĩ tới cũng không panic. Nạp thất bại thì engine chạy tiếp **không
có từ điển**, gợi ý cá nhân không bị ảnh hưởng.

`u16` cho id và `lo` đặt trần cứng 65.535 từ, dư cho mọi `N` hợp lý.

### Kích thước

| N | Đo được (trên đĩa) | Nén (IPA/APK) |
|---:|---:|---:|
| 20k | ~201 KB | ~119 KB |
| **30k** | **~331 KB** | **~183 KB** |
| 50k | ~678 KB | ~339 KB |

Mẫu đo thiên về từ ngắn, block index cộng thêm ~7,5 KB ở 30k. Ước tính cho 30k từ phổ
biến thật: **350–450 KB trên đĩa, 200–250 KB khi tải về.** Để so sánh,
`libfunput_ffi.so` bản release trên Linux ≈ 670 KB.

**Trần: `en.lex` ≤ 512 KiB**, gác bằng test `en_lex_stays_under_its_size_ceiling`.

**Đo thật ở bước 2:** `en.tsv` 30.000 từ → `en.lex` **402.522 byte (393 KiB)**, 8.740
prefix nặng; `verify` toàn tệp **2,5 ms** ở bản release. Khớp ước tính.

### Nạp và bộ nhớ

Tệp được **mmap chỉ đọc** (`memmap2`; mmap lỗi thì đọc vào bộ nhớ, dừng ở 1 MiB + 1
byte để tệp phình lên sau khi đo vẫn bị từ chối). Đây là `unsafe` duy nhất của crate,
một khối có `// SAFETY:`: tệp nằm trong bundle chỉ đọc đã ký của iOS hoặc bản chép
riêng của app Android, không gì khác ghi vào nó. Trang nhớ là
clean memory: hệ điều hành chỉ nạp trang thật sự được đọc và thu hồi được bất cứ lúc
nào. `estimated_heap_bytes` và trần 4 MiB mà test đang gác không đổi.

| Nền tảng | Tệp nằm ở | Ghi chú |
|---|---|---|
| iOS | Bundle của Keyboard extension | mmap thẳng; **không cần Full Access** |
| Android | `assets/`, nén trong APK | Lần đầu (hoặc khi `body_crc` đổi) chép ra `noBackupFilesDir/Lexicon/en-<body_crc>.lex`, xoá bản cũ, rồi mmap |

## C ABI và JNI

Thêm hàm mới, **không sửa hàm cũ**. `funput_suggestion_query_with` giữ nguyên chữ ký
và `FunputSuggestionResult` giữ nguyên hình dạng POD:

- `funput_suggestion_attach_lexicon(engine, path, path_len) -> bool`
- `nativeAttachLexicon(handle, path: String): Boolean`

Gọi một lần trên worker nối tiếp, ngay sau `open` / `in_memory`. Trả `false` là chạy
tiếp không có từ điển.

## Thay đổi ở shell

| | iOS | Android |
|---|---|---|
| Nạp | `attach_lexicon` với đường dẫn trong bundle | Chép asset, rồi `attach_lexicon` |
| Cổng ngôn ngữ | **Bỏ `state.language == .vietnamese`** trong `publishPersonalSuggestionUpdate`; giữ nguyên các điều kiện panel, surface, loại editor | `eligible()` hiện không kiểm tra ngôn ngữ — **xác minh** EN mode thật sự truy vấn |
| Viết hoa | `PersonalSuggestionCasing` đã trả nguyên ứng viên khi prefix thường — không đổi | Nhánh cuối đang `candidate.lowercase(...)` — **đổi thành trả nguyên**, nếu không `iPhone` thành `iphone` |
| Công tắc | Dùng chung "Gợi ý từ" | Dùng chung "Gợi ý từ" |

Đổi luật viết hoa trên Android an toàn với từ cá nhân: `normalize::exact` đã hạ mọi từ
học được xuống chữ thường, nên chỉ từ điển mới có chữ hoa để giữ.

Không đổi: ranh giới token, `MinimumPrefix`, đường chấp nhận gợi ý, đường học, điều
kiện mật khẩu / ô số.

## Bất biến về hiệu năng và bộ nhớ

1. **Đường học không đổi.** Từ điển không tham gia `learn`; bộ đếm `vietnamese_words`
   là O(1) ở promote/evict và nằm trong lượt duyệt sẵn có của `rebuild_tries`.
2. **Truy vấn có từ điển: 0 cấp phát khi ấm.** Mở rộng `tests/alloc_budget.rs`.
3. **Không gọi vào `funput-core`, không đọc document.** Tín hiệu duy nhất là prefix
   engine đã có trong tay.
4. **Trần tĩnh.** `en.lex` ≤ 512 KiB; heap không đổi; từ điển không có cấu trúc động
   nào.
5. **UI không cập nhật thêm lần nào.** Thanh gợi ý vốn đã cập nhật khi prefix đổi; từ
   điển chỉ lấp các ô trước đây để trống. Phép so "danh sách không đổi thì không vẽ
   lại" giữ nguyên.

## Kiểm thử và cổng gác

- `scripts/check-loc.sh`: phần mới nằm ở **`src/lexicon/`** — `mod.rs` (`Lexicon`,
  `verify`), `storage.rs` (mmap / đọc), `lookup.rs` (`top3`), `encode.rs` (feature
  `lexicon-build`), `merge.rs` (lấp chỗ trống + nhường, bước 3), và `format/` —
  `mod.rs` (hằng, header, so khoá), `sections.rs` (đọc section theo vị trí),
  `validate.rs`. **Đã đổi khi hiện thực**: bản viết ban đầu gộp header và xác minh vào
  một `format.rs`; tách ra để mỗi tệp dưới 150 dòng. `engine/query.rs` không phình.
  Codec nhị phân dời từ `persistence/codec/binary.rs` lên `src/binary.rs` để hai định
  dạng dùng chung. Test nằm ở `src/tests/lexicon/` (`encode`, `corrupt`, `lookup`,
  `real`).
- **Differential (proptest)**: `top3(prefix)` bằng kết quả brute-force trên danh sách
  từ — lọc theo prefix, sắp theo `rank`, lấy 3. Từ sinh từ ba chữ cái cả hoa lẫn
  thường để run nặng và nhẹ cùng xuất hiện dày đặc.
- **Dữ liệu thật**: mọi prefix của cả 30.000 từ trong `en.tsv`, chữ thường và chữ hoa,
  so với top-3 dựng độc lập; số heavy khớp số run dài hơn 3.
- **Tệp hỏng**: cắt cụt ở mọi độ dài, hỏng từng trường header, một tá chỗ sửa nhất
  quán-mà-sai được **tính lại CRC** để chạm tới kiểm tra cấu trúc, fuzz byte ngẫu nhiên
  rồi tra cứu, và mở qua đường mmap lẫn từ bộ nhớ. `attach_lexicon` với tệp hỏng hoặc
  không tồn tại trả `Err` và giữ nguyên từ điển đang gắn.
- **Trộn** (`src/tests/lexicon/merge.rs`): gắn qua tệp thật (đường mmap); P đứng trước,
  giữ thứ tự kể cả thứ tự rerank theo ngữ cảnh; `iphone` học được không kèm `iPhone`;
  P đủ 3, predict và prefix có dấu không bị đụng; `reset` giữ từ điển. Proptest: với
  mọi lịch sử học và prefix, kết quả trộn **bắt đầu đúng bằng** kết quả chỉ-cá-nhân và
  chỉ thêm từ điển chưa có trong đó, đủ `min(3, |P| + |L \ P|)`.
- **Nhường** (`src/tests/lexicon/yield.rs`): dưới ngưỡng thì lấp; trên ngưỡng + P có từ
  mang dấu thì không; trên ngưỡng + P chỉ có từ không dấu thì vẫn lấp; ngưỡng 0. Proptest
  tới 300 bước learn / flush / reset trên kho 3–7 từ (gần như từ mới nào cũng evict),
  so bộ đếm với đếm lại sau **mỗi** bước; thêm test mở lại từ đĩa, đủ cỡ và bị thu nhỏ.
- **Alloc-budget** (`tests/alloc_budget.rs`, feature `lexicon-build`): 100.000 lượt
  tra trên `en.tsv` thật qua tệp mmap cho heavy `th`, light `quiz`, miss `zzq`, lấp một
  phần `wo`, `kh`, có ngữ cảnh `(xin, wo)`, và một ca **nhường** — đều 0 cấp phát.
  `cargo test --workspace` bật feature qua funput-lexicon-tool nên CI chạy chúng.
- **Bench** (`suggestions/lexicon`, feature `lexicon-build`) trên cùng kho 5.000 từ
  với `suggestions/lookup`. Không gắn trần p99 vào CI — đường hiện có cũng chỉ in số,
  gác cứng trên runner dùng chung sẽ flaky.
- **Kích thước**: `en.lex` sinh từ `en.tsv` ≤ 512 KiB.
- **Casing Android**: `iPhone` với prefix `ip` giữ nguyên; prefix `IP` thành `IPHONE`.

## Thứ tự hiện thực

1. **Dữ liệu.** Công cụ xếp hạng, `en.tsv`, `supplement.tsv`, `blocklist.txt`,
   `NOTICE.md`. Review danh sách bằng mắt và **chốt N** (20k–30k) ở bước này. Chưa có
   dòng code nào trong thư viện.
   → **Xong.** Tải và đếm 12,8 GB 1-gram mất 10 phút (8 luồng), để lại 68 MB số đếm;
   chạy lại khi chỉ đổi supplement / blocklist / N mất 3 giây, và cho ra đúng từng
   byte. 88.387 từ có số đếm. **Chốt N = 30.000**: `en.tsv` 425 KB trong repo,
   `en.lex` ước ≈ 393 KB (độ dài từ trung bình 7,55, 8.745 prefix nặng), dưới trần
   512 KB. Đối chiếu với danh sách tần suất phụ đề (FrequencyWords `en_50k`, chỉ dùng
   để đo, không vào repo):

   | N | Phủ top 1k văn nói | top 5k | top 10k | `en.lex` ước |
   |---:|---:|---:|---:|---:|
   | 20k | 97,7 % | 96,3 % | 89,0 % | 259 KB |
   | **30k** | **98,0 %** | **97,6 %** | **94,5 %** | **393 KB** |
   | 40k | 98,0 % | 97,9 % | 95,5 % | 530 KB |

   Phần thiếu trong top 5k văn nói gần như toàn là thứ nằm ngoài phạm vi: mảnh của
   dạng rút gọn (`didn`, `isn`), chính tả Anh (`colour`, `centre`), chỉ dẫn phụ đề
   (`chuckles`, `beeping`), và từ bị blocklist chặn. Từ văn nói bị sách xếp thấp
   (`wanna` 14.284, `dude` 15.181, `awesome` 11.751) vẫn nằm trong 30k; chỗ còn lại
   là việc của kho cá nhân sau hai lần gõ. Khoảng 13 % là tên riêng (`Keats`,
   `Goebbels`) — di sản văn phong sách, chấp nhận ở bản này.
2. **Định dạng.** `format.rs`, `encode.rs`, `lookup.rs`, xác minh lúc nạp, test
   differential và tệp hỏng, cổng kích thước. Chưa ai dùng tới.
   → **Xong**, 7 commit: dời codec nhị phân ra dùng chung · định dạng và bộ mã hoá ·
   nạp và xác minh · tra cứu · `pack` trong công cụ · guard CI · tài liệu này.
   `en.lex` thật 402.522 byte, 8.740 prefix nặng, `verify` 2,5 ms. Module `lexicon`
   mang `expect(dead_code)` cho tới khi bước 3 dùng nó — là `expect` chứ không `allow`,
   nên khi đã có consumer thì build báo lỗi và buộc gỡ. Bench criterion và
   `tests/alloc_budget.rs` cần API công khai của engine nên nằm ở bước 3, như đã định;
   đường tra hiện không tạo `String`/`Vec`, sắp xếp ≤ 3 phần tử tại chỗ.
3. **Trộn trong `suggest_with`**, cùng ngưỡng nhường và bộ đếm. Alloc-budget, bench.
   → **Xong**, 4 commit: lấp ô trống · ngưỡng nhường · ngân sách cấp phát và độ trễ ·
   tài liệu này. Gỡ `expect(dead_code)`; những gì chỉ test hoặc bộ mã hoá dùng
   (`Lexicon::from_bytes`, `Header::write`…) được compile riêng cho chúng. Đo trên máy
   dev, criterion, kho 5.000 từ + `en.lex` thật:

   | Ca | Không từ điển | Có từ điển |
   |---|---:|---:|
   | Heavy `th` | — | 294 ns |
   | Light `quiz` | — | 343 ns |
   | Miss | 245 ns (`zzzz`) | 263 ns (`zzq`) |
   | Kho cá nhân đủ 3 ô (`word1`) | 334 ns | 329 ns — không hỏi từ điển |
   | Trộn cá nhân + từ điển (`ho`) | — | 287 ns |

   Cấp phát khi ấm: 0 ở mọi ca. Heap ước tính không đổi (935.012 byte) vì từ điển là
   vùng map. Miss đắt thêm khoảng 18 ns; mọi ca vẫn dưới 350 ns, cùng bậc với đường cá
   nhân sẵn có.
4. **C ABI + JNI**: `attach_lexicon`.
5. **iOS**: đóng gói, nạp, bỏ cổng ngôn ngữ. Đây là bước đầu tiên người dùng thấy
   được.
6. **Android**: chép asset, nạp, sửa casing, xác minh cổng ngôn ngữ.
7. **Màn hình giấy phép bên thứ ba** ở cả hai app. Phải xong trước bản phát hành đầu
   tiên mang từ điển.
8. **Đo trên máy thật.** Tiêu chí dừng: nếu từ điển làm p99 độ trễ phím xấu đi, hoặc
   nhiễu khi gõ tiếng Việt mà ngưỡng nhường không chặn được, thì sửa luật trộn trước
   khi phát hành. Các hằng dưới đây đều là phỏng đoán và chờ hiệu chỉnh:

   | Hằng | Hiện tại | Quyết định điều gì |
   |---|---:|---|
   | `N` | 30k (đã chốt ở bước 1) | Bao nhiêu từ vào `en.lex` — đánh đổi độ phủ và kích thước |
   | `lexicon_yield_after_words` | 200 | Bao nhiêu từ có dấu thì kho được coi là kho tiếng Việt |
   | Kích thước khối | 16 | Cân giữa kích thước block index và độ dài lượt quét |
   | Trần `en.lex` | 512 KiB | Mức phình tối đa chấp nhận được |

## Nợ mang theo

- **Không xoá được một từ điển.** "Xoá dữ liệu gợi ý" chỉ chạm kho cá nhân; người dùng
  không có cách bỏ một từ từ điển mà họ thấy khó chịu. Nếu cần, đó là một danh sách
  chặn cá nhân — một đường dữ liệu mới, việc riêng.
- **Dạng rút gọn** chờ sửa ranh giới token ở cả hai tracker.
- **Văn phong sách.** Google Books xếp thấp từ văn nói; `supplement.tsv` bù bằng tay,
  kho cá nhân bù phần còn lại sau hai lần gõ.
