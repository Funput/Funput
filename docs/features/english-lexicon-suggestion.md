# Gợi ý từ điển tiếng Anh

## Trạng thái

**Bước 1–7 đã hiện thực** — dữ liệu, định dạng `en.lex`, trộn vào `suggest_with`,
C ABI, JNI, bàn phím và màn hình giấy phép trên cả iOS lẫn Android.
**Còn đo hiệu năng và nghiệm thu trải nghiệm trên máy thật (bước 8), do người duy trì thực hiện.**
Xem [bàn giao](#bàn-giao-cho-bước-58) và [kiểm chứng iOS](#kiểm-chứng-ios).

Tài liệu này chốt mô hình trước khi hiện thực và được review riêng. Như
[context-suggestion.md](context-suggestion.md), nó là nơi mọi quyết định thiết kế
sống: mỗi thay đổi hành vi sau này cập nhật lại nó trong cùng PR, chỗ nào hiện thực
lệch khỏi bản viết thì sửa lại và đánh dấu **Đã đổi khi hiện thực**.

Phần lõi nằm trong **`crates/funput-suggestions`**, iOS và Android dùng chung qua
`funput-ffi` và `funput-jni`. Các shell desktop không có thanh gợi ý nên không bị ảnh
hưởng.

## Mục tiêu

Người dùng gõ gì thì thanh gợi ý trả về top 3 từ **cả** kho cá nhân **lẫn** một từ
điển tiếng Anh kèm app — **không phân biệt chế độ VI hay EN**. Gõ `wor` thì có
`work · world · words` ngay từ lần cài đầu tiên, không phải gõ tay mỗi từ hai lần như
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
→ en.lex                        (định dạng bên dưới)    ← sinh lúc build shell, không commit
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
từ nào vào, từ nào ra. CI chỉ mã hoá `en.tsv` bên trong test (cổng kích thước, test
vét cạn), không bao giờ tải vài GB Google Books. Bước xếp hạng chạy tay khi cập nhật
dữ liệu; `en.lex` được sinh từ `en.tsv` lúc build shell (bước 5–6).

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
Google Books Ngram và LDNOOBW yêu cầu ghi nguồn. iOS đã hiển thị đầy đủ `NOTICE.md`
trong Giới thiệu → Giấy phép bên thứ ba. Android cũng hiển thị đầy đủ notice offline tại cùng mục Giới thiệu.

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
| Android | `assets/`, nén trong APK | Lần đầu (hoặc khi `body_crc` đổi) chép ra `noBackupFilesDir/Lexicon/en-<body_crc>.lex`, attach thành công rồi mới xoá các phiên bản cũ do installer tạo |

## C ABI và JNI

Thêm hàm mới, **không sửa hàm cũ**. `funput_suggestion_query_with` giữ nguyên chữ ký
và `FunputSuggestionResult` giữ nguyên hình dạng POD:

```c
bool funput_suggestion_attach_lexicon(FunputSuggestionEngine *engine,
                                      const uint8_t *path, uintptr_t path_len);
```

```kotlin
external fun nativeAttachLexicon(handle: Long, path: String): Boolean
```

Gọi một lần trên worker nối tiếp, ngay sau `open` / `in_memory`. Cả hai chỉ là lớp
mỏng quanh `SuggestionEngine::attach_lexicon`:

- **Đường dẫn**: C nhận UTF-8 bytes + độ dài, đúng quy ước của
  `funput_suggestion_engine_open` — hai hàm dùng chung một `path_from_raw`. JNI nhận
  `String` như `nativeOpen`; chuỗi rỗng là `false`.
- **`false`** khi handle null hoặc không tồn tại, đường dẫn không phải UTF-8 (hoặc
  con trỏ null kèm độ dài khác 0 — không bao giờ đọc), tệp không có hoặc không phải
  `en.lex` hợp lệ. Khi đó **từ điển đang gắn (nếu có) được giữ nguyên** và engine gợi
  ý như trước. Không log gì.
- Panic trong engine bị chặn tại biên bằng `abi::safe` như mọi entry point khác.
- JNI đặt quyết định ở `registry::attach_lexicon` để test được không cần JVM; hàm
  `extern "system"` chỉ mở chuỗi Java. Khai báo `external fun` trong
  `PersonalSuggestionNative.kt` đổi cùng commit với export Rust — thiếu một bên là
  không gọi được hoặc `UnsatisfiedLinkError`.

**Test cần một `en.lex` thật.** `funput-ffi` và `funput-jni` có dev-dependency
`funput-suggestions` với feature `lexicon-build`. Resolver 2 không bật feature của
dev-dependency khi build thư viện bằng `-p` (đúng lệnh các shell dùng), nên bộ mã hoá
vẫn không vào bàn phím. **Đã đổi khi hiện thực**: guard CI "lexicon encoder stays out
of the mobile crates" đọc `cargo tree -e features,no-dev`, vì `cargo tree` mặc định
tính cả cạnh dev và sẽ báo sai; đã thử hai chiều với một dev-dependency tạm.

## Thay đổi ở shell

**iOS và Android đã hiện thực.** Đường đi cụ thể nằm ở [Bàn giao cho bước 5–8](#bàn-giao-cho-bước-58).

| | iOS | Android |
|---|---|---|
| Nạp | `attach_lexicon` với đường dẫn trong bundle | Chép asset, rồi `attach_lexicon` |
| Cổng ngôn ngữ | Bỏ `state.language == .vietnamese` trong cả `publishPersonalSuggestionUpdate` và `KeyboardInputCoordinator.replaceState`; giữ nguyên các điều kiện panel, surface, loại editor | `eligible()` không kiểm tra ngôn ngữ; instrumentation xác minh VI/EN truy vấn và chấp nhận qua JNI thật |
| Viết hoa | `PersonalSuggestionCasing` đã trả nguyên ứng viên khi prefix thường — không đổi | Nhánh cuối trả nguyên ứng viên: `ip → iPhone`, `IP → IPHONE` |
| Công tắc | Dùng chung "Gợi ý từ" | Dùng chung "Gợi ý từ" |

Đổi luật viết hoa trên Android an toàn với từ cá nhân: `normalize::exact` đã hạ mọi từ
học được xuống chữ thường, nên chỉ từ điển mới có chữ hoa để giữ.

Không đổi: ranh giới token, `MinimumPrefix`, đường chấp nhận gợi ý, đường học, điều
kiện mật khẩu / ô số.

**Đã đổi khi hiện thực iOS:** bàn giao ban đầu chỉ nhắc cổng ở controller. Coordinator
cũng khóa `suggestionTrackingActive` theo ngôn ngữ; phải bỏ cả hai để EN có prefix và
chấp nhận được gợi ý. Cổng bật bộ ghép dấu tiếng Việt (`usesVietnameseComposition`)
vẫn giữ nguyên. **Hệ quả về dữ liệu:** từ gõ ở chế độ EN giờ cũng được học vào kho cá
nhân (trước đây EN không học gì) — vẫn chỉ trong ô `.text` / `.search`, không bao giờ ở
ô mật khẩu, PIN, email, URL, điện thoại hay số, và vẫn tắt theo công tắc "Gợi ý từ". Thứ tự thật của prefix `wh` trong dữ liệu đã commit là
`which · when · what` (hạng 32, 40, 43); ví dụ trong mô tả PR ban đầu khác thứ tự này.

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
  `real`, `merge`, `yield`).
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
- **Casing Android** (bước 6): `iPhone` với prefix `ip` giữ nguyên; prefix `IP` thành `IPHONE`.

## Viết hoa theo người gõ

Luật chung cho cả hai nền tảng, đọc theo thứ tự ưu tiên — `SuggestionCaseStyle` (iOS) và
`PersonalSuggestionCasing` (Android) là hai bản chép tay của cùng bảng này, và cùng phải
khớp `classify_case` của gõ tắt trong `funput-engine`:

| Tín hiệu | Kết quả | Ví dụ |
|---|---|---|
| Caps Lock bật | TẤT CẢ HOA | `việt` → `VIỆT` |
| Shift bật | Viết hoa đầu | prefix `vi` → `Việt` |
| Prefix: ≥2 chữ cái đều hoa | TẤT CẢ HOA | `VI` → `VIỆT` |
| Prefix: chữ cái đầu hoa, phần sau thường | Viết hoa đầu | `Vi`, `V1` → `Việt` |
| Prefix: chữ thường, hoặc hoa-thường lẫn lộn | giữ nguyên ứng viên | `ip` → `iPhone`, `VNa` → `việt` |

Ba điểm đáng nhớ. Chỉ xét **chữ cái**, nên `1v` đọc theo `v`. **Một chữ hoa duy nhất là
Title**, không phải ALL CAPS — hai bản trước đây cùng sai chỗ này. Và prefix cố ý viết
lẫn (`iOS`, `VNa`) thì không đụng tới, giống nhánh `None` của `classify_case`.

Shift đứng trên prefix chứ không chỉ lấp chỗ trống: bật Shift khi thanh đang hiện chữ
thì danh sách được tô lại từ **danh sách thô đã lưu**, không hỏi lại engine — tô từ danh
sách đã viết hoa sẽ không bao giờ quay về được `Việt` từ `VIỆT`. Danh sách hiển thị và
danh sách lưu luôn đi cùng nhau vì đường chấp nhận so khớp đúng chuỗi đang hiển thị.

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
   → **Xong**, 4 commit: guard CI bỏ qua cạnh dev · `funput_suggestion_attach_lexicon`
   (header tái sinh) · `nativeAttachLexicon` + khai báo Kotlin · tài liệu này. Cả hai
   symbol có trong `libfunput_ffi.so` / `libfunput_jni.so`; `cargo tree -p … -e
   features,no-dev` không kéo `lexicon-build`. Test biên C: lấp ô trống, gắn lỗi giữ từ
   điển cũ (tệp không có / không phải UTF-8 / tệp hỏng), con trỏ null, `reset` trên
   store bền giữ từ điển. Test registry JNI: handle không tồn tại, tệp không có, tệp
   hợp lệ, handle đã huỷ.
5. **iOS**: đóng gói, nạp, bỏ cổng ngôn ngữ. Đây là bước đầu tiên người dùng thấy
   được.
   → **Đã hiện thực.** Build phase Keyboard sinh `en.lex`; worker gắn sau mỗi lần tạo
   engine, kể cả engine trong bộ nhớ; bỏ cả hai cổng ngôn ngữ nêu trên.
6. **Android — đã hiện thực**: sinh asset, cài file theo CRC, attach trên worker,
   giữ casing và kiểm thử đường VI/EN qua JNI thật.
7. **Màn hình giấy phép bên thứ ba** ở cả hai app. Phải xong trước bản phát hành đầu
   tiên mang từ điển.
   → **iOS và Android đã hiện thực**, trong Giới thiệu → Giấy phép bên thứ ba.
8. **Đo trên máy thật.** Tiêu chí dừng: nếu từ điển làm p99 độ trễ phím xấu đi, hoặc
   nhiễu khi gõ tiếng Việt mà ngưỡng nhường không chặn được, thì sửa luật trộn trước
   khi phát hành. Các hằng dưới đây đều là phỏng đoán và chờ hiệu chỉnh:

   | Hằng | Hiện tại | Quyết định điều gì |
   |---|---:|---|
   | `N` | 30k (đã chốt ở bước 1) | Bao nhiêu từ vào `en.lex` — đánh đổi độ phủ và kích thước |
   | `lexicon_yield_after_words` | 200 | Bao nhiêu từ có dấu thì kho được coi là kho tiếng Việt |
   | Kích thước khối | 16 | Cân giữa kích thước block index và độ dài lượt quét |
   | Trần `en.lex` | 512 KiB | Mức phình tối đa chấp nhận được |

## Bàn giao cho bước 5–8

Bước 1–4 được làm và kiểm trên Ubuntu. Phần iOS sau đó được tích hợp trên macOS;
Swift gọi nạp qua C ABI; Kotlin gọi JNI thật và đã kiểm thử trên emulator.
Rust và hai lớp biên đã có test từ các bước trước.

**Sinh `en.lex`** (không commit tệp này; mỗi shell tự sinh lúc build):

```bash
cargo run --release -p funput-lexicon-tool -- pack \
    < crates/funput-suggestions/data/lexicon/en.tsv > "$OUT/en.lex"
```

`pack` xác minh tệp bằng chính reader của thư viện trước khi ghi, in số từ / số prefix
nặng / kích thước ra stderr, và thoát khác 0 nếu danh sách hỏng. Hiện ra 30.000 từ,
8.740 prefix nặng, 402.522 byte.

**Bước 5 — iOS (đã hiện thực):**

- `platforms/ios/Scripts/build-lexicon.sh` chạy trong build phase Keyboard ở Debug
  và Release, dùng Cargo `--locked`, target host, ghi tạm rồi thay file trong
  `DERIVED_FILE_DIR/Lexicon`; chỉ chép vào **Keyboard extension** khi pack thành công
  và kích thước trong trần 512 KiB. Không commit nhị phân, không tải dữ liệu.
- Phase chạy mỗi lần build để không bỏ sót thay đổi TSV hoặc bộ mã hóa; Cargo cache
  phần biên dịch. Chỉ target Keyboard tắt Xcode user-script sandbox vì Cargo cần
  truy cập toolchain và cache; không thay đổi sandbox/quyền của app hay extension.
- `PersonalSuggestionEngine.attachLexicon(url:) -> Bool` gọi C ABI theo mẫu
  `open(storeURL:)` (UTF-8 bytes + độ dài). Worker gọi một lần khi tạo mỗi engine,
  trước khi query; lỗi nạp vẫn cho phép gợi ý cá nhân. Nguồn URL có thể truyền vào
  initializer nội bộ để test production worker với store riêng.
- Bỏ hai cổng ngôn ngữ ở controller và coordinator; giữ cổng loại editor và công
  tắc gợi ý. Nhãn cài đặt giải thích rõ cả từ cá nhân lẫn từ điển đi kèm.
- Không cần Full Access để nạp từ điển; không đổi casing (bảng "Thay đổi ở shell").

**Bước 6 — Android (đã hiện thực):**

- `BuildLexiconTask` chạy `scripts/build-lexicon.sh` bằng host Cargo `--locked`;
  generated assets của `ime` ở cả Debug/Release chứa `lexicon/en.lex` và nguyên
  `lexicon/NOTICE.md`. Inputs gồm TSV, notice, Cargo manifests/lockfile, toolchain,
  source Rust và script. Không tải dữ liệu hay commit nhị phân. Pack lỗi hoặc quá
  512 KiB làm build thất bại; file tạm chỉ được rename khi pack thành công.
- `suggestions/lexicon/LexiconInstaller` đọc CRC unsigned little-endian tại offset
  20; tái sử dụng `noBackupFilesDir/Lexicon/en-<crc-8-hex>.lex`. Copy stream kiểm
  chiều dài và CRC vào file tạm cùng thư mục, sync rồi rename nguyên tử. Bản cache
  bị Rust từ chối được chép lại và thử attach đúng một lần; chỉ dọn tên phiên bản
  do installer quản lý sau khi attach thành công. Không truncate file đang mmap.
- `PersonalSuggestionEngine.attachLexicon(File)` giữ owner-thread check; worker
  attach một lần sau khi mở engine lưu bền hoặc fallback trong bộ nhớ, trước query.
  I/O không chạy trên main hoặc mỗi phím. Lỗi file/attach vẫn dùng gợi ý cá nhân.
  Reset giữ từ điển; không đổi chữ ký JNI hoặc R8 keep rule.
- Nhánh casing mặc định giữ nguyên `iPhone`; đường EN nhập trực tiếp và VI ghép dấu
  cùng query/chấp nhận qua production service, worker và JNI. Giữ chính sách editor
  Android, nguồn gợi ý, prefix 2 ký tự và `IME_FLAG_NO_PERSONALIZED_LEARNING`.
- Source suggestions tách engine/worker/service/lexicon, giữ package cũ cho phần
  hiện có. Thư mục About và test mới được kiểm tra layout; mọi `.kt`/`.kts` ≤150 dòng,
  các thư mục chuẩn hóa ≤5 file Kotlin trực tiếp, không thêm ngoại lệ.

**Bước 7 — giấy phép (hai nền tảng đã hiện thực):** iOS đọc resource tham chiếu
thẳng `crates/funput-suggestions/data/lexicon/NOTICE.md`; Android đóng gói nguyên file
nguồn vào generated assets. Màn hình Compose đọc trên `Dispatchers.IO`, hỗ trợ cuộn,
chọn văn bản, cỡ chữ hệ thống và TalkBack; destination trong tab Giới thiệu dùng
back stack và saver hiện có. Không duy trì bản notice thủ công trong Swift/Kotlin.

**Bước 8 — đo trên máy thật:** p99 độ trễ phím, peak memory của extension iOS, và độ
nhiễu khi gõ tiếng Việt; hiệu chỉnh `lexicon_yield_after_words` theo bảng hằng ở bước 8
của "Thứ tự hiện thực".

**Còn treo, không thuộc bước nào:**

- `refresh.sh` mới vào danh sách ShellCheck của CI; máy làm bước 1–4 không có
  ShellCheck, nên lần chạy CI đầu tiên là lần kiểm đầu tiên.

Đã xử lý: step CI "charset stays out of the mobile crates" dùng `! lệnh`, mà dưới
`bash -e` chỉ dòng cuối thật sự chặn — sửa ở #387. Guard của từ điển viết theo cùng
khuôn đó: phủ định là `if … then refuse`, và `grep` đọc hết đầu vào để `pipefail`
không biến một lần khớp thành "qua".

## Kiểm chứng iOS

- Build lại XCFramework trước khi test để Swift dùng đúng header và thư viện C ABI.
- Chạy `Scripts/test-funput-kit.sh` trên iOS Simulator, rồi `FunputTests` và
  `FunputUITests` của scheme Funput. UI tests cần bật bàn phím bằng
  `Scripts/uitest-enable-keyboard.sh`; test clipboard còn cần Full Access và quyền
  dán. Các test giao diện dọc phải không kế thừa hướng ngang từ launch tests.
- Test tích hợp đọc `en.lex` từ chính `Keyboard.appex` trong app đã build, kiểm tra
  gợi ý, thứ tự cá nhân, bỏ trùng, ngưỡng nhường, reset và lỗi attach. Test worker
  dùng production source cùng target membership, với URL và store riêng.
- Kiểm tra Debug Simulator và Release device: `en.lex` trong extension ≤ 512 KiB,
  `NOTICE.md` trong app trùng nguồn và `MinimumOSVersion` của cả hai vẫn là 16.0.
- `Scripts/check-swift-loc.sh`, ShellCheck cho `Scripts/build-lexicon.sh`,
  `Scripts/tests/test_build_lexicon.py`, cùng guard `lexicon-build` của CI phải xanh.

**Bàn giao người dùng thử trên máy thật (chưa xác nhận):**

1. Kho trống: gõ `wh` ở VI và EN, chọn gợi ý; kiểm tra đúng từ và một dấu cách.
2. Thử `ip` / `IP`, tắt rồi bật gợi ý, đổi VI/EN và mở lại bàn phím.
3. Thử không Full Access, bật lại Full Access, và “Xóa từ đã học”: từ điển vẫn có.
4. Gõ tiếng Việt thường dùng; kiểm tra từ cá nhân đứng trước và luật nhường sau khi
   đủ 200 từ đã promoted mang dấu. Thử ô mật khẩu, ô số và chuyển ứng dụng.
5. Mở giấy phép khi offline. Đo p99 độ trễ phím, peak memory extension và đánh giá
   độ nhiễu khi gõ; kết quả Simulator không thay thế các phép đo này.

## Kiểm chứng Android

Thực hiện ngày 2026-09-15 trên macOS, emulator **Pixel_10a / emulator-5556, Android 17
(API 37), arm64**. Giữ minSdk 26, JNI `arm64-v8a` và `x86_64`; không sửa source iOS.

| Cổng | Kết quả |
|---|---|
| `clean testDebugUnitTest lintDebug assembleDebug :app:assembleRelease :app:bundleRelease` | Xanh; build sạch, sinh lại JNI từ Rust hiện tại và generated assets |
| Unit test toàn Android | **625 đạt**, không skip: app 35, ime 266, renderer 222, keyboard-ui 39, theme-runtime 34, theme-store 29 |
| Instrumentation `ime` | **31 test chức năng đạt**; 1 benchmark Release-only không chạy ở Debug |
| Instrumentation `keyboard-ui` | **14 đạt** |
| Instrumentation `app` | **21 đạt**, gồm mở notice đầy đủ và quay lại Giới thiệu |
| Instrumentation `keyboard-renderer` | Task đã gọi nhưng module không có source instrumentation; **không tính là có test đạt** |
| LOC/layout Kotlin, ShellCheck, `bash -n`, `git diff --check` | Xanh; không tăng giới hạn hoặc thêm ngoại lệ |
| Packer failure tests | 2 test Python đạt, gồm lỗi pack, output thiếu/quá lớn, giữ artifact cũ và dọn file tạm |
| Gradle incremental | TSV đổi nội dung làm chạy lại pack; khôi phục TSV chạy lại; lượt tiếp theo `UP-TO-DATE` |
| Guard `lexicon-build` | Không xuất hiện trong `cargo tree --locked -p funput-ffi/funput-jni -e features,no-dev` |
| APK Debug, APK Release, AAB Release | Đều có **30.000 từ / 402.522 byte / CRC b31198ec**, notice trùng nguồn và JNI đủ hai ABI |

Clean build chạy toàn bộ unit test; test bổ sung về copy bị gián đoạn và lint IME
được chạy lại sau đó. Instrumentation gọi production engine/worker/service, xác minh
fallback bộ nhớ, attach một lần mỗi engine, mở lại, personal-first, dedup, ngưỡng
nhường, reset và lỗi attach. Đường VI/EN chèn đúng một dấu cách, học đúng một lần;
kết quả cũ bị loại khi đổi session/panel/editor hoặc tắt gợi ý. Kiểm tra riêng
`IME_FLAG_NO_PERSONALIZED_LEARNING`, password và chuyển ngôn ngữ.

**Các giới hạn cần đọc cùng kết quả:**

- Máy local không có khóa ký Release được cấu hình: APK là `app-release-unsigned.apk`,
  AAB cũng không ký. Build sử dụng cấu hình hiện có, không thêm hoặc thay khóa ký.
- `warmQueryMeetsReleaseLatencyAndMemoryBudgets` giữ assumption Release-only. Gradle
  báo task xanh, nhưng XML của runner ghi `AssumptionViolatedException` dưới failure
  thay vì skipped. Đây là **không chạy benchmark**, không phải test đạt; không dùng
  số liệu emulator để kết luận hiệu năng máy thật.
- `keyboard-renderer` chỉ có 222 unit test, chưa có instrumentation. ABI x86_64 được
  build và kiểm artifact, không thực thi trên emulator arm64 này.
- Lint không có lỗi chặn build; các warning/deprecation hiện hữu vẫn được báo.

Có thể kiểm lại artifact bằng
`python3 scripts/tests/verify_lexicon_artifacts.py <apk> <aab>` từ thư mục Android.
Test packer: `python3 scripts/tests/test_build_lexicon.py`.

**Bàn giao kiểm thử máy thật cho người duy trì (bước 8 còn chờ):**

- Cài mới, gõ VI/EN khi chưa học từ; thử `wh`, `ip`, `IP`, chuyển ngôn ngữ và nhận gợi ý.
- Học từ, kiểm ưu tiên và bỏ trùng; reset và xác nhận từ điển vẫn còn; tắt/bật gợi ý.
- Kiểm gõ liên tục, đổi app/editor/panel, mật khẩu và editor yêu cầu không học cá nhân.
- Nâng cấp app có bản từ điển mới, mở lại IME; kiểm hoạt động offline và notice đầy đủ.
- Thử font lớn, TalkBack, chọn văn bản, Back và khôi phục màn hình giấy phép.
- Đo p99 độ trễ phím, bộ nhớ IME/extension và độ nhiễu khi gõ tiếng Việt với kho nhỏ/lớn;
  hiệu chỉnh ngưỡng nhường nếu cần. Chưa nghiệm thu hiệu năng máy thật của hai nền tảng.

## Nợ mang theo

- **Không xoá được một từ điển.** "Xoá dữ liệu gợi ý" chỉ chạm kho cá nhân; người dùng
  không có cách bỏ một từ từ điển mà họ thấy khó chịu. Nếu cần, đó là một danh sách
  chặn cá nhân — một đường dữ liệu mới, việc riêng.
- **Dạng rút gọn** chờ sửa ranh giới token ở cả hai tracker.
- **Văn phong sách.** Google Books xếp thấp từ văn nói; `supplement.tsv` bù bằng tay,
  kho cá nhân bù phần còn lại sau hai lần gõ.
