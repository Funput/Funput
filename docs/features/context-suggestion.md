# Gợi ý từ theo ngữ cảnh (bigram cá nhân)

## Trạng thái

Chưa hiện thực. Tài liệu này chốt mô hình *trước* khi có code và được review riêng;
nó là nơi mọi quyết định thiết kế sống, nên mỗi thay đổi hành vi sau này cập nhật lại
nó trong cùng PR.

Toàn bộ phần lõi nằm trong **`crates/funput-suggestions`**, tức iOS và Android dùng
chung một bản duy nhất qua `funput-ffi` và `funput-jni`. Các shell desktop
(macOS/Windows/Linux) không có thanh gợi ý nên không bị ảnh hưởng.

## Mục tiêu

Gõ `xin` rồi dấu cách thì hệ thống gợi ý `chào`. Dữ liệu **học hoàn toàn từ chính
người dùng**, giống hệt cách gợi ý theo prefix hiện tại: cùng một kho từ, cùng một
quy tắc xếp hạng, cùng một tệp trên đĩa, cùng một nút xoá.

Ba ràng buộc đặt ra từ đầu và không thương lượng:

1. **Không được ảnh hưởng luồng gõ tiếng Việt.** Không thêm một nhánh nào vào đường
   xử lý phím của `funput-core`, không chờ, không khoá.
2. **Không rò bộ nhớ, không phình không giới hạn.** Mọi thứ thêm vào phải có trần
   tĩnh, tính được bằng số.
3. **Nice to have.** Nếu nó phải đánh đổi với cảm giác gõ, nó thua.

## Không thuộc phạm vi

**Từ điển bigram tĩnh kèm app.** Đã cân nhắc và loại: nó giải quyết cold start nhưng
kéo theo corpus, giấy phép, kích thước gói và một đường dữ liệu thứ hai. Nếu sau này
đổi ý, bảng tĩnh là một nguồn ứng viên thứ hai ghép vào cùng chỗ chấm điểm ở mục
"Truy vấn", không đụng phần còn lại.

Cũng ngoài phạm vi: mô hình ngôn ngữ · trigram · học từ nội dung người dùng không tự
gõ (paste, văn bản có sẵn) · gợi ý ở các shell desktop · đồng bộ giữa thiết bị.

## Hai chế độ, một kho dữ liệu

Cùng một bảng bigram phục vụ hai chế độ có mức rủi ro rất khác nhau, nên chúng có
ngưỡng khác nhau:

| Chế độ | Khi nào | Rủi ro khi sai | Ngưỡng |
|---|---|---|---|
| **Rerank** | Đang gõ, prefix ≥ 1 ký tự | Thấp — prefix đã lọc, sai chỉ là đổi thứ tự khe | thấy cặp **≥ 1 lần** |
| **Predict** | Prefix rỗng, vừa xong một từ | Cao — chiếm khe, gợi bừa thì gây bực | **≥ 2 lần** *và* vượt ngưỡng áp đảo |

Rerank là phần đáng giá nhất trên mỗi byte: nó chạy ở **mọi** lần gõ có ngữ cảnh, và
nó **không làm tăng số lần cập nhật thanh gợi ý** so với hôm nay — thanh đó vốn đã
cập nhật trong khi gõ, ta chỉ đổi thứ tự bên trong.

Predict thì ngược lại: hôm nay khi prefix rỗng thanh gợi ý **không cập nhật**, bật nó
lên là thêm một lần cập nhật UI sau mỗi dấu cách. Đây là chi phí thật lớn nhất của
tính năng, và nó nằm ở tầng UI chứ không phải ở Rust.

## Ngưỡng áp đảo: im lặng khi không chắc

Tiếng Việt đơn âm, nên `P(next | prev)` chia làm hai lớp rất khác nhau:

- **Nhọn** — `cảm` → `ơn`, `bởi` → `vì`, `tuy` → `nhiên`, `xin` → `chào`. Gần như
  tất định. Đây là nơi tính năng thật sự thắng.
- **Bẹt** — `của` → ?, `và` → ?, `là` → ?. Hàng trăm ứng viên, không ngưỡng nào cứu
  được, và đây lại là các âm tiết xuất hiện nhiều nhất.

Nên Predict chỉ hiện khi ứng viên dẫn đầu **áp đảo**: `top.uses * 100 >= context_seen
* context_dominance_percent` (mặc định 40). `cảm` vượt ngay; `của` không bao giờ vượt
và tự im. Người dùng cảm nhận "nó chỉ nói khi nó chắc" — đó chính là cảm giác thông
minh, và nó cũng cắt phần lớn số lần cập nhật UI.

## Follower nằm trong `WordRecord`, không có bảng cạnh riêng

Quyết định trung tâm. Mỗi từ nhớ luôn vài từ hay đi sau nó:

```rust
pub(crate) struct Follower {
    word: u32,        // chỉ số vào `words`
    generation: u16,  // xem mục dưới
    uses: u16,
}                      // đúng 8 byte

pub(crate) struct WordRecord {
    text: String,
    uses: u32,
    last_used: u64,
    generation: u16,              // của chính slot này
    context_seen: u32,            // số lần từ này đứng ở vị trí `prev`
    followers: [Follower; FOLLOWER_SLOTS],   // FOLLOWER_SLOTS = 4
}
```

Vì sao không tách bảng cạnh riêng:

- **Không có cấp phát mới nào.** Mảng cố định nằm trong `WordRecord` vốn đã có. Không
  `Vec` con, không `HashMap`, không arena thứ hai — tức không có gì để rò.
- **Vòng đời tự đúng.** Từ bị evict thì follower của nó biến mất cùng lúc; không có
  bảng thứ hai để lệch pha hay để quên dọn.
- **Trần bộ nhớ tính được**: `4 × 8 = 32` byte thêm mỗi từ, cộng `context_seen` và
  `generation`. Với `max_words = 5000` là **≈ 200 KB**, cố định, không phụ thuộc
  người dùng gõ bao nhiêu.
- Tra cứu là đọc thẳng 4 phần tử liền nhau trong cùng cache line của bản ghi.

Sức chứa 5000 từ vốn đã dư cho tiếng Việt: engine học **âm tiết** (`learn` loại token
có khoảng trắng), mà tiếng Việt chỉ có ~7000 âm tiết hợp lệ và ~2500–3000 dùng hằng
ngày. Chỗ chật là không gian cặp, và đó là việc của chính sách nạp slot bên dưới.

## Cạm bẫy: chỉ số từ bị tái sử dụng

Đây là chỗ sinh bug âm thầm nếu không xử lý ngay từ thiết kế. Trong code hiện tại:

- `engine/learning.rs::upsert_word` khi đầy chỗ làm `self.words[index] = record` —
  **chỉ số cũ giờ trỏ sang một từ khác**.
- `engine/mod.rs::enforce_capacity` dùng `swap_remove(index)` — **từ cuối đổi chỉ số**.

Một follower lưu bằng `u32` chỉ số sẽ lặng lẽ trỏ sai sau evict: `xin → chào` biến
thành `xin → hoá đơn`. Không crash, không test nào bắt được, chỉ là gợi ý bậy.

**Quyết định: generation tag.** Mỗi slot trong `words` có `generation: u16`, tăng lên
mỗi lần slot bị ghi đè hoặc bị `swap_remove` chuyển chủ. Follower lưu kèm generation
lúc ghi; lúc đọc, lệch generation thì coi như cạnh chết và slot đó được tái dùng
ngay.

Phương án thay thế — quét dọn cạnh khi evict — bị loại vì nó là `5000 × 4 = 20.000`
thao tác **ngay giữa luồng gõ**, đúng thứ tài liệu này cam kết không làm.

## Chính sách nạp slot và lão hoá

Bốn slot cố định có bệnh "đến trước chiếm chỗ": nếu 4 lần gõ nhầm lấp đầy slot của
`xin` thì `chào` không bao giờ chen vào. Dùng luật kiểu Misra–Gries, O(4), không cấp
phát:

```text
learn_after(prev, next):
    prev.context_seen += 1
    nếu next đã có slot            -> slot.uses += 1
    ngược lại nếu có slot trống/chết -> chiếm, uses = 1
    ngược lại                       -> slot yếu nhất giảm 1;
                                       về 0 thì bị next chiếm với uses = 1
```

Nhiễu và lỗi gõ không lặp lại sẽ bị chính áp lực cạnh tranh đẩy ra theo thời gian.

**Lão hoá.** `uses: u16` sẽ bão hoà và khoá cứng thói quen cũ. Khi `context_seen` của
một từ chạm trần (hoặc bất kỳ follower nào sắp tràn `u16`), **chia đôi tại chỗ** cả
4 follower lẫn `context_seen`. Đây là thao tác O(4) trên đúng một bản ghi — không có
lượt quét toàn cục nào ở bất kỳ đâu.

Tăng `FOLLOWER_SLOTS` không làm nó thông minh hơn một cách hiển nhiên: slot dư cần
thêm bằng chứng để lấp, nên với dữ liệu cá nhân thưa nó chỉ ổn định chậm hơn và chứa
nhiều nhiễu hơn. 4 là mặc định; 6 là trần hợp lý nếu đo được là thiếu.

## Hợp đồng của crate: ngữ cảnh truyền vào, không tự giữ

README của crate ghi rõ nó không quan sát document context. Giữ nguyên hợp đồng đó —
lõi vẫn là hàm thuần, dễ test, dễ chạy differential:

```rust
impl SuggestionEngine {
    pub fn learn_after(&mut self, prev: Option<&str>, token: &str) -> LearnOutcome;
    pub fn suggest_with(&self, prev: Option<&str>, prefix: &str) -> SuggestionSet<'_>;
}
```

`learn` và `suggest` cũ giữ nguyên, thành `learn_after(None, …)` /
`suggest_with(None, …)`. Không có breaking change cho consumer hiện tại.

`prev` là **token đã hoàn tất gần nhất**, do platform giữ và truyền theo từng lời
gọi. `None` nghĩa là "không có ngữ cảnh" và engine hành xử đúng như hôm nay.

Cấu hình thêm ba khoá, và `engine/config.rs::sanitize` phải kẹp cả ba:

```rust
context_rerank_uses: u16,        // mặc định 1
context_predict_uses: u16,       // mặc định 2
context_dominance_percent: u8,   // mặc định 40, kẹp 0..=100
```

## Ranh giới ngữ cảnh do platform quyết định

Tin tốt: **đường ống đã có sẵn ở cả hai nền tảng.** iOS có
`KeyboardSuggestionInputUpdate { prefix, completedToken }`; Android có
`AuthoredSuggestionUpdate` với đúng hai trường đó. Hôm nay `completedToken` chỉ được
đem đi `learn` rồi bỏ. Việc cần làm là **giữ nó lại làm `prev` cho các lần sau**.

Đây là một biến `String` duy nhất trong service của mỗi nền tảng. Nó phải bị xoá về
`nil` khi:

- gặp dấu câu hoặc xuống dòng;
- caret nhảy, có selection, hoặc người dùng paste;
- backspace lùi qua ranh giới từ (liên quan `Engine::adopt`);
- đổi focus / đổi editor / đổi panel;
- editor không cho phép học (ô mật khẩu, ô số, `allowsPersonalizedLearning` tắt).

**Tuyệt đối không đọc lại `documentContextBeforeInput` mỗi phím** để lấy `prev`. Đó
là đúng cái đã gây trễ khi gõ nhanh trên iOS. `prev` luôn là giá trị engine đã có
trong tay.

Hai chi tiết dễ sót:

- **Chấp nhận gợi ý cũng sinh cặp.** `acceptSuggestion` chèn `suggestion + " "`, tức
  nó hoàn tất một token; cặp `(prev, suggestion)` phải được học như gõ tay.
- **Prefix rỗng.** Android hiện chặn truy vấn dưới `MinimumPrefix`; chế độ Predict
  phải là đường riêng, không nới ngưỡng đó cho Rerank.
- **Viết hoa.** `PersonalSuggestionCasing` suy ra kiểu chữ từ prefix; với prefix rỗng
  nó không có đầu vào. Quy tắc cho Predict: theo trạng thái Shift hiện tại (trên iOS
  `auto_capitalize` của engine đang vô hiệu, Shift mới là thứ quyết định).

## C ABI và JNI

Thêm hàm mới, không sửa hàm cũ — shell cũ và thư viện mới vẫn khớp:

- `funput_suggestion_learn_after(engine, prev, prev_len, token, token_len) -> bool`
- `funput_suggestion_query_with(engine, prev, prev_len, prefix, prefix_len) -> FunputSuggestionResult`
- `nativeLearnAfter(handle, prev: String?, token: String)`
- `nativeQueryWith(handle, prev: String?, prefix: String)`

`prev` null / `prev_len == 0` nghĩa là `None`. `FunputSuggestionResult` giữ nguyên
hình dạng POD hiện tại, nên phía Swift/Kotlin không phải sửa gì ở tầng giải mã.

## Lưu trữ

**Journal — thêm bigram gần như 0 byte.** Journal hiện là danh sách token *theo đúng
thứ tự gõ*. Ghi cặp thành hai chuỗi sẽ làm nó phình gấp đôi, tức chạm
`JOURNAL_COMPACT_BYTES` (64 KB) gấp đôi tần suất, tức gấp đôi I/O nén file. Thay vào
đó: thêm **bản ghi đánh dấu ngắt ngữ cảnh** dưới dạng token độ dài 0, phát ra mỗi khi
platform reset `prev`. Lúc replay, mọi cặp được dựng lại miễn phí từ các token liền
kề. Chi phí: vài byte mỗi câu.

**Snapshot — chỉ ghi follower của từ đã promoted.** Từ mới gõ một lần thì follower
của nó cũng chưa đáng tin. Cắt được phần lớn phần phình, và tự nhất quán với ngưỡng
`promotion_uses` đang dùng cho trie.

**Migration — đây là chỗ dễ làm mất dữ liệu người dùng.** `persistence` hiện có
`VERSION: u16 = 1` dùng chung, và `snapshot::decode` trả `ErrorKind::Unsupported`
với **bất kỳ** version khác. Nếu chỉ bump lên 2, `Store::open` sẽ lỗi → `open` lỗi →
shell rơi về engine in-memory, và người dùng **mất sạch kho từ đã học** một cách im
lặng.

Quyết định: **đọc chấp nhận cả 1 và 2, ghi luôn ra 2.** Snapshot v1 nạp lên thành
các `WordRecord` với `followers` rỗng và `context_seen = 0`; lần `compact` kế tiếp tự
nâng cấp tệp. Không có bước migration riêng, không có nguy cơ mất dữ liệu.

`reset()` phải xoá cả follower — với thiết kế này thì tự đúng, vì chúng nằm trong
`words`.

## Bất biến về hiệu năng và bộ nhớ

Viết ra để thành test, không phải để làm khẩu hiệu:

1. **`learn` không bao giờ rebuild.** Đường bigram là O(4) và không cấp phát. (Ghi
   chú: `learn_inner` hiện tại *đã* gọi `rebuild_tries()` khi evict một từ promoted —
   bigram không được thêm bất cứ thứ gì cùng loại. Sửa cái có sẵn là việc riêng.)
2. **Truy vấn có ngữ cảnh: 0 cấp phát khi ấm.** Mở rộng `tests/alloc_budget.rs`.
3. **Không gọi vào `funput-core`, không đọc document, không đụng preedit** từ đường
   gợi ý. Chỉ đường chấp nhận gợi ý được ghi, và đường đó đã tồn tại.
4. **Trần bộ nhớ tĩnh.** Không cấu trúc động nào được thêm; tổng phần tăng là
   `max_words × 40` byte, biết trước, không phụ thuộc lịch sử gõ.
5. **UI chỉ đổi text.** Bar cao cố định, 3 ô cố định, không tạo view, không animation;
   danh sách ứng viên giống lần trước thì **bỏ qua hoàn toàn**, không đụng view.

## Kiểm thử và cổng gác

- `scripts/check-loc.sh` giới hạn 150 dòng/tệp → phần mới đặt ở **`src/bigram/`**
  (`slots.rs` cho luật nạp/lão hoá, `query.rs` cho chấm điểm), không phình
  `engine/learning.rs` và `engine/query.rs`.
- Alloc-budget: thêm ca `suggest_with(Some("xin"), "")` và `suggest_with(Some("xin"), "ch")`.
- Bench criterion song song `suggestions_lookup`, kèm trần p99.
- Test riêng cho **chỉ số tái sử dụng**: học đầy `max_words`, ép evict, khẳng định
  cạnh cũ chết chứ không trỏ sang từ mới.
- Test round-trip lưu trữ: nạp snapshot **v1** phải thành công và không mất từ nào.
- Test replay journal có bản ghi ngắt: cặp qua ranh giới câu không được sinh ra.

## Thứ tự hiện thực

1. **`Follower` + generation tag trong `WordRecord`**, chưa có API công khai nào. Chỉ
   cấu trúc dữ liệu và test chỉ-số-tái-sử-dụng. Bước này một mình đã đóng lại cạm bẫy
   nguy hiểm nhất, và nó kiểm chứng được mà không cần đụng tới nền tảng.
2. **`learn_after` + luật nạp slot và lão hoá.** Vẫn chưa ai đọc dữ liệu ra; kiểm
   chứng bằng test và bằng `stats()`.
3. **Lưu trữ**: đọc v1/v2, ghi v2, bản ghi ngắt ngữ cảnh trong journal. Làm **trước**
   khi có consumer, để không có phiên bản nào ngoài đời ghi ra tệp mà bản sau đọc sai.
4. **`suggest_with` chế độ Rerank.** Consumer đầu tiên. Rẻ nhất, an toàn nhất, và
   không thêm một lần cập nhật UI nào.
5. **C ABI + JNI**, rồi giữ `prev` trong service của iOS và Android, cùng danh sách
   điều kiện reset ở mục "Ranh giới ngữ cảnh". Đây là bước đầu tiên người dùng thấy
   được khác biệt.
6. **Chế độ Predict** cùng ngưỡng áp đảo, kèm quy tắc viết hoa cho prefix rỗng và
   quy tắc bỏ qua cập nhật UI khi danh sách không đổi.
7. Đo trên máy thật. **Tiêu chí dừng: nếu bước 6 làm p99 độ trễ phím xấu đi, giữ vĩnh
   viễn ở bước 5.** Tính năng này không đáng đánh đổi cảm giác gõ.
