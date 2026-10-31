# Gợi ý tiếng Việt có sẵn từ lần cài đầu

## Trạng thái

**Bước 1–2 đã hiện thực** (tài liệu này cùng phần lõi Rust của Phần A, chung một PR):
`SeedLanguage`, bảng cặp với 69 cặp đầu tiên, Predict có dữ liệu kèm app. **Bước 3 đã hiện
thực** (PR xếp chồng): setter qua C ABI và JNI. **Bước 4 đã hiện thực** trên cả Android và iOS
(hai PR riêng, cùng xếp chồng lên bước 3): người mới cài gõ `cảm` rồi dấu cách là có `ơn`, đã thử
trên simulator iOS và bằng instrumented test trên emulator Android. **Bước 6 (Phần B, #561) đã
hiện thực**, chỉ trong `funput-suggestions`: gõ dở `đươ` là có `được · đường`, và kho cá nhân
khớp theo cùng luật dấu. **Cặp tiếng Anh ở chế độ VI** (một danh sách nhỏ viết tay, xem
[Cặp tiếng Anh](#cặp-tiếng-anh-ở-chế-độ-vi)) đã hiện thực. Còn bước 5 và 7, cùng các hướng ở
[Mở rộng sau này](#mở-rộng-sau-này).

Giải quyết [#560](https://github.com/Funput/Funput/issues/560)
(đoán từ tiếp theo sau dấu cách) và [#561](https://github.com/Funput/Funput/issues/561)
(gợi ý tiếng Việt cho người mới cài) bằng một thiết kế chung, hiện thực thành nhiều PR
theo [thứ tự ở cuối](#thứ-tự-hiện-thực).

Như [context-suggestion.md](context-suggestion.md) và
[english-lexicon-suggestion.md](english-lexicon-suggestion.md), tài liệu này là nơi mọi
quyết định thiết kế sống: mỗi thay đổi hành vi sau này cập nhật lại nó trong cùng PR, chỗ
nào hiện thực lệch khỏi bản viết thì sửa lại và đánh dấu **Đã đổi khi hiện thực**.

Phần lõi nằm trong **`crates/funput-suggestions`**, iOS và Android dùng chung qua
`funput-ffi` và `funput-jni`. Các shell desktop không có thanh gợi ý nên không bị ảnh
hưởng.

## Vấn đề

Thanh gợi ý hôm nay có ba nguồn, và với người mới cài thì cả hai nguồn tiếng Việt đều
trống:

| Nguồn | Khi nào nói | Người mới cài |
|---|---|---|
| Kho cá nhân (trie) | Đang gõ dở một từ | Trống |
| Bigram cá nhân (Predict) | Ngay sau dấu cách | Trống: cần `context_slot(prev)` có trong kho và cặp đã gặp ≥ 2 lần |
| `en.lex` | Đang gõ dở, prefix ASCII | Có, nhưng chỉ tiếng Anh |

Người dùng so với Laban thấy ngay: gõ xong `cảm` không có `ơn`, gõ `đươ` không có
`được`. Họ kết luận app thiếu tính năng, trong khi Predict đã ship từ #287 đến #289.

## Mục tiêu

1. **Predict có dữ liệu từ ngày đầu.** Gõ `cảm` rồi dấu cách là có `ơn`, `tuy` có
   `nhiên`, `hạnh` có `phúc`, dù kho cá nhân trống.
2. **Gõ dở âm tiết có gợi ý tiếng Việt.** Gõ `đươ` có `được · đường · đương`.
3. **Dữ liệu của người dùng luôn thắng.** Dữ liệu kèm app chỉ lấp chỗ kho cá nhân chưa nói
   được gì, và tự rút lui khi người dùng đã có đủ dữ liệu của chính họ. Đây cũng là ý "ưu
   tiên theo lịch sử" trong #561, và nó đến miễn phí từ vòng học có sẵn: chấp nhận một
   gợi ý đi qua `completedToken` như gõ tay, nên cặp và từ đó được học.

Ràng buộc kế thừa nguyên từ hai tài liệu trước, không thương lượng:

1. **Không ảnh hưởng luồng gõ tiếng Việt.** Không thêm nhánh nào vào đường xử lý phím
   của `funput-core`, không chờ, không khoá. Đường truy vấn không cấp phát.
2. **Không phình.** Trần kích thước dữ liệu và trần bộ nhớ tính được bằng số, gác bằng
   test.
3. **Nice to have.** Nếu phải đánh đổi với cảm giác gõ, nó thua. Gợi ý sai tệ hơn im
   lặng.

## Không thuộc phạm vi

- ~~**Dự đoán từ tiếp theo bằng dữ liệu tĩnh cho tiếng Anh**~~ (comment trong #560).
  **Đã đổi khi hiện thực**: làm một phần, bằng danh sách nhỏ viết tay chỉ dùng ở chế độ VI (xem
  [Cặp tiếng Anh](#cặp-tiếng-anh-ở-chế-độ-vi)). Chế độ EN và dữ liệu từ corpus nằm ở
  [Mở rộng sau này](#mở-rộng-sau-này). Funput tập trung vào tiếng Việt; tiếng Anh là phần thêm.
- **Từ ghép nhiều âm tiết trên một ô** (`cảm ơn` hiện thành một gợi ý). Engine học và gợi
  ý theo âm tiết (`learn` loại token có khoảng trắng); đổi điều đó là đổi mô hình.
- ~~Đổi cách kho cá nhân khớp prefix.~~ **Đã đổi khi hiện thực**: làm luôn cùng Phần B, vì
  để âm tiết kèm app khớp mềm hơn từ người dùng đã học là ngược với "cá nhân luôn đứng trước".
  Xem [câu hỏi mở](#câu-hỏi-mở) 1.
- Mô hình ngôn ngữ · trigram · tải dữ liệu qua mạng · dữ liệu suy ra từ corpus hoặc danh
  sách từ của bên thứ ba.

## Dữ liệu: viết tay, MIT

Lý do giữ nguyên lập trường của [vi.txt](../../crates/funput-suggestions/data/syllables/NOTICE.md):
các danh sách tiếng Việt có sẵn hoặc có giấy phép không hợp với app MIT, hoặc đặt ra câu
hỏi bảng tần suất lấy từ corpus có phải là tác phẩm phái sinh không. Viết tay thì không
phải trả lời câu hỏi đó.

Viet74K ([duyet/vietnamese-wordlist](https://github.com/duyet/vietnamese-wordlist)) chưa
có giấy phép rõ ràng và có từ sai. Nó **không** là nguồn của dữ liệu ship. Nó chỉ được
dùng trong một lệnh `funput dev` chạy tay để cảnh báo cặp không thành từ ghép có thật, như
`benchmarks/` đang dùng nó để đo.

### Phần A: `data/pairs/vi.txt` (mới)

Một bảng **chữ đứng trước → một chữ đi sau**, mỗi dòng một cặp, cách nhau một dấu cách:

```text
# Pairs whose second syllable a reader would bet on, one per line.
cảm ơn
tuy nhiên
hạnh phúc
đặc biệt
thoải mái
```

- **Mỗi chữ đứng trước chỉ có một chữ đi sau.** Predict chỉ bao giờ đưa ra một ứng viên
  (ứng viên dẫn đầu, xem `bigram/query/predict.rs`), nên một chữ có hai hướng mạnh ngang
  nhau thì không thuộc bảng này. Đó chính là điều kiện để vào bảng.
- **Tiêu chí vào bảng:** người viết tin rằng trong văn nhắn tin thường ngày, chữ đứng
  trước được theo sau bởi chữ đó **trong phần lớn trường hợp**. Chặt hơn ngưỡng áp đảo
  40% của bigram cá nhân, vì ở đây không có bằng chứng nào từ người dùng.
- **Ví dụ bị loại:** `xin` (`chào`, `lỗi`, `phép` ngang nhau), `của`, `và`, `là`, `vì`,
  `nhưng`, `thật` (`sự`, `là`). Đây là các chữ "bẹt" trong context-suggestion.md; bảng
  tĩnh không cứu được chúng, và cố cứu là gợi ý bừa.
- Hai phía đều là âm tiết hoàn chỉnh, chữ thường, NFC, **viết theo kiểu dấu mới**
  (`ToneStyle::Modern`: `hoà`, `khoẻ`). Kiểu cũ do code đổi khi tra, xem
  [tra bảng](#tra-bảng).
- Quy mô ước tính: vài trăm đến khoảng 2.000 cặp, khoảng 25 KB. Bắt đầu với một danh sách
  nhỏ đã review kỹ, mở rộng theo đợt.
- Có `NOTICE.md` bên cạnh, cùng giọng với `data/syllables/NOTICE.md`.

### Phần B: `data/syllables/vi.txt` (có sẵn)

2.100 âm tiết, 4 bậc, viết tay, MIT. Hôm nay chỉ làm prior cho sửa lỗi gõ
(`SYLLABLE_TIER_WEIGHTS`). Phần B dùng lại đúng tệp này, **không thêm dữ liệu**. Thứ tự
gợi ý là bậc trước, thứ tự trong tệp sau.

`vi.txt` liệt kê cả hai kiểu dấu (`hòa` và `hoà`). Phần B lọc theo kiểu dấu đang chọn để
không gợi ý hai lần một âm tiết.

## Chế độ gõ: engine cần biết hai điều

Hôm nay `SuggestionEngine` không biết người dùng đang ở chế độ VI hay EN, cũng không biết
kiểu đặt dấu. English lexicon cố ý không cần biết. Dữ liệu tiếng Việt thì cần:

- Ở chế độ EN, gợi ý `ơn` sau `cảm` hay `thì · thể` cho prefix `th` là nhiễu.
- Gợi ý `hoà` cho người chọn kiểu cũ là sai chính tả theo lựa chọn của họ.

Thêm một setter, không thêm tham số vào `suggest_with` (tránh đổi chữ ký qua bốn tầng):

```rust
pub enum SeedLanguage { Off, Vietnamese(ToneStyle) }

impl SuggestionEngine {
    /// What the shipped Vietnamese data may say. `Off` until a shell says
    /// otherwise, so a shell that never calls this behaves exactly as today.
    pub fn set_seed_language(&mut self, language: SeedLanguage);
}
```

- `ToneStyle` lấy từ `funput-core`, crate này đã phụ thuộc vào nó.
- Shell gọi khi tạo engine và mỗi lần người dùng đổi chế độ VI/EN hoặc đổi kiểu dấu.
  Lệnh gọi rẻ (ghi một field), nên gọi thừa không sao.
- **Bảng được dựng lúc setter chuyển sang `Vietnamese` lần đầu**, không phải ở phím đầu
  tiên. Parse và sắp xếp khoảng 4.000 dòng tốn cỡ mili giây; trả giá đó trên đường gõ là
  vi phạm ràng buộc 1.
- `reset()` (nút "Xoá dữ liệu gợi ý") không đụng tới: đây là dữ liệu kèm app, như `en.lex`.

## Phần A: Predict có dữ liệu kèm app (#560)

### Truy vấn

`suggest_with(prev, "")` hôm nay chỉ gọi `predict` khi `context_slot(prev)` có. Đổi thành:

```text
predict_with_seed(prev):
    nếu seed_language là Off: trả như hôm nay
    nếu context_slot(prev) có:
        r = predict(context)                       // bigram cá nhân, không đổi
        nếu r khác rỗng: trả r
        nếu context_seen(context) >= context_evidence_seen: im lặng
    t = seed_pair(prev)                            // tra bảng, theo kiểu dấu
    nếu không có t, hoặc t bị chặn: im lặng
    trả [t]
```

**Đã đổi khi hiện thực.** Luật rút lui không nằm ở tầng dữ liệu kèm app mà ở chính bigram:
`predict` trả về ba trạng thái (`bigram/query/predict.rs::Prediction`):

| Trạng thái | Khi nào | Thanh gợi ý |
|---|---|---|
| `Speaks(word)` | Ứng viên dẫn đầu qua `context_predict_uses` và ngưỡng áp đảo | Ứng viên đó |
| `Unsure` | Chữ đứng trước đã gặp ≥ `context_evidence_seen` lần mà vẫn bẹt | Im, không nguồn nào được lấp |
| `NoEvidence` | Chưa có trong kho, hoặc gặp quá ít lần | Hỏi dữ liệu kèm app (`seed/query.rs::seed_after`) |

`suggest_with` ghép ba nhánh đó. Bằng cách này, nguồn nào sau này muốn lấp chỗ im lặng
(Phần B, bảng tĩnh tiếng Anh) chỉ cần trả lời nhánh `NoEvidence`, không phải tự đọc
`context_seen`. Tham số đổi tên thành `context_evidence_seen` vì nó là tính chất của bigram
cá nhân, không riêng gì dữ liệu kèm app.

- **Cá nhân luôn đứng trước.** Nếu bigram cá nhân đã đủ tự tin để nói, nó nói, kể cả khi
  khác bảng tĩnh. Người hay gõ `cảm thấy` sẽ thấy `thấy`, không phải `ơn`.
- **Rút lui khi người dùng đã có dữ liệu.** Khi người dùng đã gõ chữ đứng trước đủ
  `context_evidence_seen` lần (mặc định 3) mà bigram cá nhân vẫn im, tức là phân bố của
  chính họ bẹt hoặc khác bảng. Lúc đó bảng tĩnh cũng phải im, không thì nó cãi lại dữ liệu
  của người dùng.
- **Không cần chữ đứng trước có trong kho.** Người mới gõ `cảm` lần đầu thì `cảm` vừa được
  ghi nhận (uses 1, `context_seen` 0) hoặc chưa có trong kho; cả hai trường hợp đều đi tới
  bảng tĩnh.
- **Từ bị chặn** (`blocked.contains`) không bao giờ được gợi ý, như `complete_from_lexicon`
  đang làm. Kiểm tra cả hai cách viết, nên chặn `khỏe` ở kiểu cũ rồi đổi sang kiểu mới thì
  `khoẻ` cũng không hiện.

### Tra bảng

- `prev` được chuẩn hoá lên stack (`normalize::exact_into`: chữ thường, NFC) rồi binary
  search trong bảng đã sắp theo byte. Không cấp phát. **Đã đổi khi hiện thực**: không đổi
  kiểu dấu của `prev` lúc tra; bảng giữ sẵn cả hai cách viết của chữ đứng trước.
- Vị trí dấu do `funput_core::respell_tone` quyết định, là `reposition_existing_tone` mà bộ
  gõ dùng mỗi phím, mở ra public. Không có bản thứ hai của luật đặt dấu trong
  `funput-suggestions`.
- Chữ đi sau trả về theo kiểu dấu đang chọn. Bảng giữ sẵn dạng kiểu cũ cho số ít chữ đi
  sau có `oa`, `oe`, `uy`, dựng một lần lúc bật.
- Trả `&'static str` hoặc chuỗi do bảng sở hữu, nên `SuggestionSet<'_>` không đổi.
  **Đã đổi khi hiện thực**: Predict từ bảng lấp `SuggestionSet` trực tiếp
  (`seed/query.rs::seed_after`), không qua `assemble(ids)`, vì `ids` chỉ trỏ được vào
  kho cá nhân. Với prefix rỗng, `suggest_with` giờ trả thẳng kết quả Predict, bỏ lượt gọi
  `complete_from_lexicon` vốn không bao giờ thêm gì cho prefix rỗng.

### Viết hoa

Không có luật mới: Predict đã viết hoa theo trạng thái Shift hiện tại
(context-suggestion.md, mục "Viết hoa"). `Cảm` + dấu cách cho `ơn` viết thường, trừ khi
Shift đang bật.

### Chấp nhận và học

Không có luật mới. Hai service đã bỏ guard `prefix.isEmpty()` ở đường chấp nhận cho
Predict. Chạm vào `ơn` chèn `ơn `, đi qua `completedToken`, `learn_after("cảm", "ơn")`
ghi cặp vào bigram cá nhân. Sau `context_predict_uses` lần, chính bigram cá nhân nói `ơn`,
và bảng tĩnh không còn được hỏi cho chữ đó nữa.

## Phần B: âm tiết tiếng Việt cho prefix (#561)

### Truy vấn

Thứ tự lấp ba ô khi `seed_language` là `Vietnamese`:

```text
suggest_with(prev, prefix):
    P = như hôm nay (trie cá nhân, rerank theo ngữ cảnh)
    S = [seed_pair(prev)] nếu nó khớp prefix          // ví dụ `tuy` + `nh` → `nhiên`
    V = seed_syllables(prefix), bỏ trùng P và S, bỏ từ bị chặn
    E = en.lex như hôm nay
    lấp: P, rồi S, rồi V, rồi E
```

Khi `seed_language` là `Off`, hàm cho ra đúng kết quả hôm nay.

**Prefix không dấu** (`th`, `an`) là chỗ khó. Gõ Telex hay VNI, phần lớn prefix tiếng Việt
lúc gõ dở chưa có dấu nào, nên bỏ qua chúng là mất gần hết giá trị của phần B. Nhưng người
song ngữ gõ tiếng Anh ở chế độ VI (có tự khôi phục) sẽ mất `the · that` nếu tiếng Việt
chiếm cả ba ô. Luật:

| Prefix | V được lấp tối đa | Ví dụ khi kho trống |
|---|---|---|
| Có dấu (`đươ`, `thà`) | 3 ô (`en.lex` vốn trả rỗng với prefix không phải ASCII) | `được · đường · đương` |
| Không dấu (`th`) | 2 ô, ô cuối để `en.lex` nếu nó có từ | `thì · thể · the` |

Con số 2 là điểm xuất phát, chỉnh lại ở bước đo trên máy thật. Luật nhường hiện có của
`en.lex` (`lexicon_yield_after_words`) giữ nguyên: khi kho đã là kho tiếng Việt và P có từ
mang dấu, `en.lex` không lấp.

### Khớp prefix: theo những dấu đã gõ

Gõ Telex thì thanh thường đến cuối: `đươ` đi trước `được`. So khớp NFC chính xác không bao
giờ thấy `được` từ `đươ`, vì `ơ` và `ợ` là hai ký tự khác nhau. Luật cho phần B, so từng ký
tự:

> Ký tự `p` của prefix khớp ký tự `c` của âm tiết khi `p` bằng `c`, hoặc bằng `c` đã bỏ
> thanh, hoặc bằng `c` đã bỏ hết dấu (kể cả `đ` thành `d`).

| Prefix | Khớp | Không khớp |
|---|---|---|
| `đươ` | `được`, `đường`, `đương` | `đuôi` |
| `duo` | `được`, `đuôi`, `dưới` | |
| `thà` | `thành` | `thầy` (`à` không khớp `ầ`) |

- Âm tiết trùng chính xác với prefix không được gợi ý (gõ `ăn` không cần ô `ăn`).
- Kho cá nhân dùng **cùng luật này** (đã đổi khi hiện thực, xem câu hỏi mở 1).

### Cấu trúc tra cứu

- Bảng âm tiết sắp theo dạng bỏ hết dấu (`folded`). Binary search lấy dải có cùng prefix
  bỏ dấu, rồi lọc từng ký tự theo luật trên, giữ ba ứng viên tốt nhất theo (bậc, thứ tự
  trong tệp). Dạng bỏ dấu tính bằng iterator lúc so, không lưu chuỗi thứ hai.
- Với prefix từ hai ký tự (`MinimumPrefix = 2` ở cả hai shell), dải lớn nhất cỡ vài trăm
  phần tử. Đo bằng bench criterion có sẵn trong `benches/suggestions`; nếu vượt ngân sách
  thì thêm bảng "prefix nặng" như `en.lex`.
- Không cấp phát trên đường truy vấn.

### Đã đổi khi hiện thực (Phần B)

- **Mã nằm ở đâu.** `seed/marks.rs` (luật khớp từng ký tự), `seed/syllables/` (chỉ mục: `mod.rs`
  tra, `build.rs` dựng), `seed/complete.rs` (thứ tự lấp). `crate::syllables::listed()` là chỗ đọc
  `vi.txt` duy nhất, dùng chung cho prior của sửa lỗi gõ và cho chỉ mục này.
- **Tra một lần, lấp hai lượt.** `seed_words` tính cặp tĩnh và tối đa ba âm tiết một lần cho mỗi
  phím; `fill_before_english` lấp (chừa một ô nếu prefix không dấu), `complete_from_lexicon` lấp
  phần tiếng Anh, rồi `fill_rest` lấp nốt. Bản đầu tra hai lần và chuẩn hoá lại prefix cho từng mục
  trong dải, mất 30 µs với `th`.
- **Luật nhường của `en.lex` tính trên từ cá nhân P**, trước khi lấp gì. Tính trên tập đã lấp thì âm
  tiết có dấu kèm app luôn làm tiếng Anh nhường, mất ô dành cho `the`.
- **Chỉ mục:** mỗi mục giữ chuỗi `&'static`, thứ hạng (vị trí trong tệp, vì tệp đã xếp theo bậc rồi
  độ phổ biến), kiểu dấu, và chỉ số cách viết kiểu còn lại để từ bị chặn ở kiểu này cũng bị chặn ở kiểu
  kia. 24 byte mỗi mục, khoảng 50 KiB cho 2.100 âm tiết, gác bằng test ≤ 64 KiB. `skip` (tra từ bị
  chặn) chỉ chạy cho âm tiết đủ tốt để vào top 3.
- **Đo (release, kho 5.000 từ):** `đươ` 1,4 µs, `th` 2,85 µs, `tuy` + `nh` khoảng 4 µs (phần lớn là tìm chữ
  đứng trước trong kho), không khớp 0,5 µs. Nhóm `lookup` và `predict` ở `Off` không đổi so với
  `main` (đo xen kẽ hai nhánh).
- `vi.txt` không có `đương`, nên bảng ví dụ ở trên thực tế ra `được · đường`.
- Luật khớp chuyển về `index/marks.rs`, là luật duy nhất cho cả kho cá nhân lẫn dữ liệu kèm app.

## Cặp tiếng Anh ở chế độ VI

Nhiều người gõ tiếng Anh ngay trong chế độ VI, nhờ bàn phím tự khôi phục chữ gốc. Bigram cá
nhân vốn đã học cặp tiếng Anh của họ; phần thiếu chỉ là dữ liệu cho người mới cài.

- **Dữ liệu:** `data/pairs/en.txt`, khoảng 45 cặp viết tay (MIT) theo đúng tiêu chí "nhọn" của
  `vi.txt` (`thank you`, `according to`, `instead of`, `depends on`...). Không chữ có nhiều hướng
  (`as`, `in`, `make`), không chữ phải viết hoa (`i`, tên riêng).
- **Một bảng, một luật:** `seed/pairs.rs` dựng một `PairTable` từ cả hai danh sách (tiếng Việt
  trước). Chữ tiếng Anh là ASCII nên `respell_tone` không đổi gì. Predict, đường prefix
  (`thank` + `yo` → `you`), luật rút lui, luật chặn từ và "cá nhân trước" giữ nguyên, không thêm
  nhánh nào.
- **Hai ngôn ngữ không chạm nhau**, gác bằng `funput-lexicon-tool/tests/en_pairs.rs`: chữ đứng
  trước không phải âm tiết tiếng Việt hoàn chỉnh (người gõ tiếng Việt không bao giờ nhận dự đoán
  tiếng Anh vì vô tình), không chữ đứng trước nào có ở cả hai danh sách, mọi chữ đều có trong
  `en.tsv` (từ thật, đã qua blocklist), và engine thật đưa đúng chữ đi sau của từng dòng.
- **Chỉ chế độ VI:** ở chế độ EN và ô không ghép, dữ liệu kèm app vẫn `Off`. Không phải sửa nền tảng.

## Mở rộng sau này

Những hướng đã cân nhắc nhưng chưa làm, mỗi hướng ghi cần gì và đụng vào đâu, để không phải
tìm lại:

1. **Dự đoán tiếng Anh ở chế độ EN.** Thêm mã wire (`SeedLanguage::WIRE_ENGLISH`, giải mã ở
   `from_wire`, nên FFI/JNI không đổi logic) và một biến thể `SeedSlot` chỉ dùng bảng tiếng Anh.
   iOS lấy được ngay từ `KeyboardInputCoordinator` (`state.language` và `editorMode`). Android
   khó hơn: `SeedLanguageReportingEngine` chỉ thấy `setEnabled(bool)`, không phân biệt "đang gõ EN"
   với "ô mật khẩu", nên cần đưa thêm tín hiệu ngôn ngữ từ `ImeKeyActionHandler`.
2. **Cặp tiếng Anh từ corpus.** Google Books 2-grams (CC BY 3.0, cùng nguồn với `en.tsv`, đã có
   `NOTICE.md`), thêm một bước trong `funput-lexicon-tool` giống `count`/`rank`. Dung lượng tải lớn
   hơn bộ 1-grams (~13 GB) nhiều lần, nên chỉ đáng làm khi danh sách viết tay tỏ ra quá ngắn.
3. **Mở rộng cặp tiếng Việt (bước 5)** từ 69 lên vài trăm, kèm công cụ đánh giá offline
   (`funput dev`) đo số phím tiết kiệm và tỉ lệ đoán sai trên văn bản tự viết; một đợt làm tỉ lệ
   sai tăng rõ thì không merge.
4. **Đo và chỉnh trên máy thật (bước 7):** `context_evidence_seen` (3), trần 2 ô cho prefix không
   dấu, `MAX_VISITS` (64) của `ArenaTrie::find_matching`, bộ nhớ extension iOS.
5. **Ngữ cảnh dài hơn một chữ** (trigram, cụm cố định như `by the way`, `cảm ơn bạn`): engine chỉ
   nhìn một chữ đứng trước; đổi điều đó là đổi mô hình bigram cá nhân.
6. **Công tắc riêng "Gợi ý có sẵn"**, nếu có phản hồi rằng dữ liệu kèm app gây nhiễu; hôm nay dùng
   chung công tắc "Gợi ý từ".

## Không đổi

- **Sửa lỗi gõ.** `is_known_word` vẫn không hỏi `vi.txt` hay bảng cặp, như
  typo-correction.md đã chốt. Prior `SYLLABLE_TIER_WEIGHTS` giữ nguyên.
- **Từ điển cá nhân** (#552): từ bị chặn chặn cả dữ liệu kèm app. Màn danh sách từ không
  hiện dữ liệu kèm app, vì nó không phải của người dùng.
- **Lưu trữ.** `WordRecord`, snapshot, journal không đổi một byte nào. Dữ liệu kèm app
  biên dịch vào binary bằng `include_str!`, như `vi.txt` hôm nay.
- **Công tắc.** Dùng chung công tắc "Gợi ý từ", như `en.lex`. Không thêm công tắc riêng
  trong bản này.
- **Ô nhập nhạy cảm.** Thanh gợi ý vốn tắt ở ô mật khẩu, PIN, email, URL, số; phần này
  không đổi điều đó.

## Ngân sách

| Mục | Trần | Gác bằng |
|---|---|---|
| Dữ liệu biên dịch thêm | ≤ 40 KB (`pairs/vi.txt`) | test kích thước tệp |
| Bộ nhớ heap của hai bảng | ≤ 128 KiB, đo thật ở bước 2 và 6 rồi chốt | `SuggestionEngine::stats` + test |
| Cấp phát trên đường truy vấn | 0 | test alloc-budget hiện có |
| Độ trễ `suggest_with` | không tăng quá 10% so với baseline | bench criterion |
| Dựng bảng | ngoài đường gõ (lúc gọi setter) | test: phím đầu tiên không dựng bảng |
| Mỗi tệp nguồn | ≤ 150 dòng | `scripts/check-loc.sh` |

## Test

**Dữ liệu** (trong `funput-lexicon-tool/tests/`, cạnh `vi_syllables.rs`):

- Mỗi phía của mỗi cặp là âm tiết hoàn chỉnh theo `funput_core::is_complete_syllable`.
- Chữ thường, NFC, kiểu dấu mới (`respell_tone(x, Modern)` không đổi gì); không có chữ đứng
  trước nào xuất hiện hai lần.
- ~~Chữ đi sau có trong `vi.txt`.~~ **Đã đổi khi hiện thực**: bỏ. `vi.txt` là 2.100 âm
  tiết thường gặp, còn chữ đi sau của từ láy và từ ghép ràng buộc (`khoăn`, `loẹt`, `hức`)
  phần lớn là âm tiết hiếm, vốn là loại cặp nhọn nhất. Thêm chúng vào `vi.txt` thì đổi prior
  của sửa lỗi gõ, là việc khác. `is_complete_syllable` vẫn chặn được lỗi cấu trúc.
- Engine gợi ý đúng điều tệp nói cho từng cặp, trên kho trống.
- Không chữ nào nằm trong danh sách "bẹt" cấm vào bảng (`của`, `và`, `là`, `xin`...), viết
  thành một hằng trong test để reviewer thấy lý do.

**Engine** (`src/tests/seed/`):

- Kho trống, VI: `cảm` + `""` → `ơn`; EN hoặc `Off` → im lặng, đúng như hôm nay.
- Bigram cá nhân đủ tự tin thì thắng bảng (`cảm thấy` ×3 → `thấy`).
- `context_seen ≥ context_evidence_seen` mà cá nhân im → bảng cũng im.
- Từ bị chặn không bao giờ hiện. `reset()` giữ bảng.
- Kiểu cũ: `hoà`/`hòa` ở chữ đứng trước đều tra ra; chữ đi sau viết theo kiểu đang chọn.
- Phần B: bảng luật khớp ở trên; prefix không dấu để lại ô cho `en.lex`; thứ tự P, S, V, E.
- ~~Proptest: với `Off`, `suggest_with` cho ra đúng kết quả của engine hôm nay.~~ **Đã đổi
  khi hiện thực**: không còn "engine hôm nay" nào để so cùng tiến trình. Bằng chứng thay thế:
  toàn bộ test hiện có của `funput-suggestions` chạy ở `Off` và vẫn xanh, và nhánh dữ liệu kèm
  app chỉ mở khi `seed.language` là `Vietnamese`.
- `tests/alloc_budget.rs`: Predict từ bảng tĩnh, cả hai kiểu dấu, 0 cấp phát.

**Đánh giá offline** (`funput dev`, chạy tay, không phải CI): gõ lại một tập văn bản nhắn
tin do người duy trì tự viết (MIT, không lấy từ corpus) với kho trống, đếm:

- số phím tiết kiệm được nếu luôn chấp nhận gợi ý đúng,
- tỉ lệ Predict nói mà sai (gợi ý hiện ra nhưng chữ gõ tiếp khác).

Tỉ lệ sai là thước đo chính khi mở rộng bảng cặp: một đợt thêm cặp làm nó tăng rõ rệt thì
không merge.

## Nền tảng

| Tầng | Việc |
|---|---|
| `funput-ffi` | `funput_suggestion_set_seed_language(engine, language: u8, tone_style: u8) -> bool` (`suggestion/seed.rs`) |
| `funput-jni` | `PersonalSuggestionNative.nativeSetSeedLanguage(handle: Long, language: Int, toneStyle: Int): Boolean`, qua `registry::set_seed_language` |
| iOS | `KeyboardInputCoordinator.syncComposerLanguage()` là nơi duy nhất gọi `composer.setEnabled`; nó báo `onSeedLanguageChange` khi `seedLanguage` đổi. Controller nối callback tới `PersonalSuggestionService.setSeedLanguage` lúc bootstrap, trước lần `apply` đầu tiên |
| Android | `SeedLanguageReportingEngine` bọc engine bộ gõ của tài liệu; mọi `configure` và `setEnabled` đi qua nó, kể cả `ImeSettingsController` (nhận `session.composition.engine`). Báo tới `PersonalSuggestionService.setSeedLanguage` |

**Đã đổi khi hiện thực (bước 4).**

- **Một chỗ trên mỗi nền tảng.** Android không có chỗ nào gom cả kiểu dấu lẫn VI/EN, nên chỗ đó là
  một lớp bọc engine bộ gõ (cả hai đều đi qua engine). iOS đã có coordinator ghi trạng thái bộ gõ,
  nên chỉ cần gom ba lời gọi `setEnabled` về một hàm. Cả hai chỉ báo khi đã biết đủ hai nửa và chỉ
  khi giá trị đổi.
- **Một đường giao duy nhất trên iOS.** Bản viết định truyền thêm giá trị hiện tại lúc
  `configurePersonalSuggestions`; không cần nữa, vì service sống suốt đời controller và callback được
  gắn trước lần `apply` đầu tiên, nên service nghe được mọi thay đổi. Service giữ giá trị trong
  `PersonalSuggestionWorkerConfiguration.seedLanguage` để worker tạo sau vẫn có, và gửi riêng
  `setSeedLanguage` cho worker đang sống để một lần đổi VI/EN không chạy lại đường configure.
- **Race giữa đổi cài đặt và query.** Vòng drain của worker (cả hai nền tảng) lấy query mới ngay
  trong lượt đang chạy, trước một tác vụ "áp cài đặt" đã xếp hàng, nên query đó bị trả lời theo cài
  đặt cũ. Instrumented test Android bắt được (`khoẻ` thay vì `khỏe`). Cách sửa giống nhau: giá trị chờ
  nằm trong một ô có khoá (`AtomicReference` / `OSAllocatedUnfairLock`) và vòng drain áp nó trước mỗi
  query; tác vụ xếp hàng chỉ còn lo trường hợp đổi mà không có query nào theo sau.
- Không có bộ nhớ VI/EN theo app trên Android lẫn iOS (chỉ macOS có), nên không có nguồn đổi thứ ba.

**Mã wire**, giống nhau ở hai tầng:

| Tham số | Giá trị |
|---|---|
| `language` | `0` tắt (`FUNPUT_SEED_LANGUAGE_OFF`), `1` tiếng Việt (`FUNPUT_SEED_LANGUAGE_VIETNAMESE`); giá trị khác coi như tắt |
| `tone_style` | Byte của bộ gõ: `0` kiểu cũ, `1` kiểu mới (như `FunputConfig.tone_style`), giải mã bằng chính `decode_tone_style` của tầng đó |

Trả `false` khi handle null (C) hoặc không còn (JNI), hoặc khi biên ABI phải bỏ một lời gọi bị
panic; engine giữ nguyên cài đặt cũ. Mã và hàm giải mã nằm một chỗ,
`SeedLanguage::{WIRE_OFF, WIRE_VIETNAMESE, from_wire}`; FFI viết lại hai hằng bằng số cho
cbindgen và `const assert` chúng bằng mã của thư viện, JNI thu `int` về byte rồi gọi `from_wire`.

**Một nơi đặt trạng thái gõ.** Handle gợi ý giữ bản sao riêng của ngôn ngữ và kiểu dấu, tách khỏi
cấu hình bộ gõ. Ở bước 4, mỗi nền tảng phải gọi setter từ **cùng một chỗ** đang đẩy cấu hình cho bộ
gõ, cho mọi nguyên nhân đổi: nút VI/EN, đổi kiểu dấu trong cài đặt, và bộ nhớ VI/EN theo app tự
chuyển khi đổi ô nhập. Nếu hai lời gọi tách nhau, bộ gõ có thể ra `hòa` trong khi thanh gợi ý ra
`khoẻ`, hoặc vẫn đoán tiếng Việt khi bàn phím đã sang tiếng Anh, mà không tầng nào phát hiện.

Shell nào chưa gọi setter thì vẫn `Off`, nên các PR Rust merge được trước PR nền tảng mà
không đổi hành vi.

## Thứ tự hiện thực

| Bước | Nội dung | Issue |
|---|---|---|
| 1 | Tài liệu này (cùng PR với bước 2) | |
| 2 | Rust: `SeedLanguage`, đổi kiểu dấu, bảng cặp với danh sách nhỏ (69 cặp), Predict có dữ liệu kèm app, test dữ liệu và engine | #560 |
| 3 | `funput-ffi` + `funput-jni`: setter | #560 |
| 4 | iOS + Android gọi setter (hai PR riêng) | #560 |
| 5 | Mở rộng `pairs/vi.txt` theo đợt, mỗi đợt kèm kết quả đánh giá offline | #560 |
| 6 | Rust: phần B (âm tiết cho prefix, cặp tĩnh khớp prefix, trần 2 ô cho prefix không dấu); xong | #561 |
| 7 | Đo trên máy thật (độ trễ, bộ nhớ extension iOS, cảm giác gõ); chỉnh `context_evidence_seen` và trần 2 ô | cả hai |

Bước 6 không phụ thuộc bước 5, nên có thể chạy song song khi danh sách cặp đang được viết.

## Câu hỏi mở

1. ~~**Kho cá nhân có nên dùng luật khớp mới không?**~~ **Đã giải quyết** (cùng PR Phần B):
   nếu chỉ dữ liệu kèm app khớp theo dấu đã gõ, `đươ` sẽ đưa âm tiết kèm app ra trong khi
   `đường` người dùng đã học không hiện, ngược với "cá nhân luôn đứng trước".
   `prefix_candidates` giờ: khớp chính xác trước; prefix không dấu đi trie `folded` như cũ
   (luật dấu trùng với so khớp bỏ dấu, có proptest giữ); prefix có dấu đi trie chính xác theo
   từng chữ (`ArenaTrie::find_matching` với `marks::matches`), rẽ vào mọi nhánh mà chữ đã gõ
   có thể đứng thay, rồi gộp ba từ tốt nhất của mỗi nút cuối. Mỗi từ khớp đi qua đúng một nút
   cuối, nên đây là ba từ tốt nhất thật trong mọi từ khớp, kể cả từ kết thúc ngay ở prefix
   (`phô` ra `phố` dù `phòng · phóng · phỏng` dùng nhiều hơn). Tối đa 64 nút mỗi lần tra,
   không cấp phát. Rerank theo ngữ cảnh dùng `marks::completes`. Đo: `khô` 152 ns; các ca tra
   cũ nhanh hơn `main` 15–44% vì prefix chỉ chuẩn hoá một lần.
2. **`context_evidence_seen = 3` có đúng không?** Thấp quá thì bảng rút lui trước khi người
   dùng kịp chấp nhận gợi ý; cao quá thì bảng cãi dữ liệu cá nhân lâu hơn. Chốt ở bước 7.
3. **Có cần công tắc riêng "Gợi ý có sẵn" không?** Bản này nói không, dùng chung "Gợi ý từ".
   Xem lại nếu có phản hồi gợi ý tĩnh gây nhiễu.
