# Gợi ý từ theo ngữ cảnh (bigram cá nhân)

## Trạng thái

Sáu bước code đã xong; chỉ còn bước 7 (đo trên máy thật) trong "Thứ tự hiện thực" ở
cuối tài liệu. Mười hai PR, mỗi PR một bước hoặc một nền tảng:

| Bước | PR |
|---|---|
| Dọn đường: tách `persistence/`, vá ba đường mất dữ liệu, bỏ rebuild khỏi `learn` | #278, #279, #280 |
| 1–2 · `Follower` + generation tag + `learn_after` | #280, #281 |
| 3 · Lưu trữ: snapshot v2, journal v2 | #282 |
| 4 · `suggest_with` chế độ Rerank | #283 |
| 5 · C ABI, JNI, giữ `prev` ở hai nền tảng | #284, #285, #286 |
| 6 · Predict và ngưỡng áp đảo | #287, #288, #289 |

Tài liệu này chốt mô hình *trước* khi có code và được review riêng; nó là nơi mọi
quyết định thiết kế sống, nên mỗi thay đổi hành vi sau này cập nhật lại nó trong cùng
PR. Những chỗ hiện thực lệch khỏi bản viết ban đầu đã được sửa lại bên dưới và đánh
dấu bằng **Đã đổi khi hiện thực**.

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
    context_seen: u16,            // số lần từ này đứng ở vị trí `prev`
    followers: [Follower; FOLLOWER_SLOTS],   // FOLLOWER_SLOTS = 4
}
```

Vì sao không tách bảng cạnh riêng:

- **Không có cấp phát mới nào.** Mảng cố định nằm trong `WordRecord` vốn đã có. Không
  `Vec` con, không `HashMap`, không arena thứ hai — tức không có gì để rò.
- **Vòng đời tự đúng.** Từ bị evict thì follower của nó biến mất cùng lúc; không có
  bảng thứ hai để lệch pha hay để quên dọn.
- **Trần bộ nhớ tính được**: `4 × 8 = 32` byte thêm mỗi từ; `context_seen` lọt vào
  phần padding bản ghi vốn đã có. **Đã đổi khi hiện thực**: dự tính ≈ 200 KB, đo thật
  là **256 KiB** — `words` tăng theo cấp đôi nên 32 byte được trả trên `capacity`
  8192 chứ không phải 5000 từ. Vẫn cố định, không phụ thuộc người dùng gõ bao nhiêu.
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

**Lão hoá.** Không có nó, luật giảm điểm ở trên chỉ trừ một điểm mỗi lần, nên một
slot đã bỏ xa sẽ không bao giờ bị thách thức được. Cứ `AGING_AFTER = 256` lần thấy
một từ ở vị trí ngữ cảnh thì **chia đôi tại chỗ** cả 4 follower lẫn `context_seen`;
slot chia về 0 được giải phóng hẳn. Đây là thao tác O(4) trên đúng một bản ghi —
không có lượt quét toàn cục nào ở bất kỳ đâu.

Cùng một hằng số cũng chặn `uses`: không đếm nào vượt được `context_seen`, và
`context_seen` không vượt 256, nên `u16` không thể tràn dù người dùng gõ gì.

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

Đường ống đã có sẵn ở cả hai nền tảng: iOS có
`KeyboardSuggestionInputUpdate { prefix, completedToken }`, Android có
`AuthoredSuggestionUpdate` với đúng hai trường đó, và `completedToken` chỉ được đem
đi `learn` rồi bỏ. Việc cần làm là **giữ nó lại làm `prev` cho các lần sau**.

**Đã đổi khi hiện thực.** Bản viết ban đầu để `prev` ở service, như một biến `String`.
Nó nằm ở **tracker** — `AuthoredTokenTracker` trên cả hai nền tảng — vì tracker là
thứ duy nhất nhìn thấy mọi thao tác: ký tự nào kết thúc từ, khi nào caret rời đi, khi
nào prefix bị bỏ. Mọi tầng bên dưới chỉ thấy kết quả chứ không thấy lý do.

Và việc thật hoá ra không phải là giữ `prev`, mà là **cả hai tracker đều không phân
biệt được dấu cách với dấu câu**: cả hai chỉ là "không phải chữ cái", cả hai cùng kết
thúc từ, không ai ghi lại là cái nào. Android còn kín hơn — đường composed không hề
nhìn thấy ký tự phân tách, vì `commitBoundary` biết code point nhưng
`takeCompletedToken()` chỉ trả về từ.

Luật, giống nhau từng dòng ở hai nền tảng:

| Sự kiện | Ngữ cảnh |
|---|---|
| Từ kết thúc bằng **dấu cách** | `= completedToken` |
| Từ kết thúc bằng bất kỳ thứ gì khác (dấu câu, xuống dòng, dấu phẩy) | `= nil` |
| `reset()` — caret nhảy, có selection, paste, đổi focus/editor/panel | `= nil` |
| Backspace khi prefix đã rỗng | `= nil` |
| Editor không cho phép học (mật khẩu, ô số) | không học, không giữ |

Hai từ ở hai bên một ranh giới câu chưa bao giờ đứng cạnh nhau; đoán ngược lại là dạy
engine những cặp không ai gõ.

**Tuyệt đối không đọc lại `documentContextBeforeInput` mỗi phím** để lấy `prev`. Đó
là đúng cái đã gây trễ khi gõ nhanh trên iOS. `prev` luôn là giá trị engine đã có
trong tay.

Hai chi tiết dễ sót:

- **Chấp nhận gợi ý cũng sinh cặp.** `acceptSuggestion` chèn `suggestion + " "`, tức
  nó hoàn tất một token; cặp `(prev, suggestion)` phải được học như gõ tay.
- **Prefix rỗng.** **Đã đổi khi hiện thực**: Predict không phải hàm riêng mà chính là
  `suggest_with(prev, "")` — ngưỡng áp đảo bên dưới mới là cổng, nên không cần thêm
  một hàm nữa qua bốn tầng. Hai service nới `MinimumPrefix` **chỉ cho prefix rỗng**;
  prefix một ký tự vẫn bị chặn như trước.
- **Chấp nhận một dự đoán.** Cả hai service đều chặn `prefix.isEmpty()` ở đường chấp
  nhận, nên chạm vào ô dự đoán sẽ im lặng không làm gì. Tầng bên dưới vốn đã đúng —
  xoá 0 ký tự rồi chèn `từ + " "` — chỉ hai cái guard là thừa.
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

**Journal — thêm bigram gần như 0 byte.** Journal là danh sách token *theo đúng thứ
tự gõ*, nên một cặp là hai token liền kề và bản thân các cặp không cần ghi. Ghi cặp
thành hai chuỗi sẽ làm journal phình gấp đôi, tức chạm `JOURNAL_COMPACT_BYTES`
(64 KB) gấp đôi tần suất.

**Đã đổi khi hiện thực.** Bản viết ban đầu chọn **bản ghi ngắt** là token độ dài 0.
Làm xong rồi đo thì hỏng: dấu ngắt là một entry riêng, nên mỗi token học không có ngữ
cảnh sinh **hai** entry — mà trước bước 5 thì *mọi* token đều không có ngữ cảnh.
`pending` chạm trần 256 sau 128 lần learn thay vì 256, và `pending_overflow` biến mọi
`flush` thành một `compact` đầy đủ: `flush_256` từ 5,42 lên **9,97 ms**.

Thay bằng **một byte cờ trên mỗi token**, nói token đó có thật sự theo sau token
trước hay không. `pending` trở lại 1:1 với token, journal chỉ tăng 1 byte/token, và
`flush_256` về 5,37 ms.

Adjacency chỉ đáng tin khi ngữ cảnh caller đưa vào **đúng là** token vừa vào journal —
token ở giữa có thể đã bị `Ignored`, hoặc từ đó vừa bị evict. Engine theo dõi chỗ
chuỗi đang kết thúc và chỉ đánh dấu nối khi hai thứ khớp nhau. Replay khi đó **bỏ
sót** một cạnh chứ không bao giờ **bịa** ra cạnh sai.

**Snapshot — ghi follower của mọi từ có ít nhất một ô không trống.** **Đã đổi khi hiện
thực**: bản viết ban đầu nói "chỉ từ đã promoted". Luật mới cắt dung lượng y hệt (từ
gõ một lần hầu như không có follower), nhưng không phải truyền `promotion_uses` vào
encoder và không vứt dữ liệu thật. Một từ chưa ai theo sau tốn ba byte.

**Migration — đây là chỗ dễ làm mất dữ liệu người dùng.** `persistence` hiện có
`VERSION: u16 = 1` dùng chung, và `snapshot::decode` trả `ErrorKind::Unsupported`
với **bất kỳ** version khác. Nếu chỉ bump lên 2, `Store::open` sẽ lỗi → `open` lỗi →
shell rơi về engine in-memory, và người dùng **mất sạch kho từ đã học** một cách im
lặng.

Quyết định: **đọc chấp nhận cả 1 và 2, ghi luôn ra 2.** Snapshot v1 nạp lên thành
các `WordRecord` với `followers` rỗng và `context_seen = 0`; lần `compact` kế tiếp tự
nâng cấp tệp. Không có bước migration riêng, không có nguy cơ mất dữ liệu.

**Đã đổi khi hiện thực**: hai bản ghi được đánh version **riêng** —
`SNAPSHOT_WRITE_VERSION` và `JOURNAL_WRITE_VERSION`, mỗi bên một cửa sổ đọc. Chúng là
hai định dạng khác nhau tình cờ chung một con số, và đóng dấu v2 lên journal chỉ vì
snapshot lớn lên sẽ hứa với người đọc những dấu ngắt journal chưa có.

Người dùng **hạ cấp** app gặp tệp v2 sẽ bị `TooNew` → shell rơi về in-memory, nhưng
tệp được giữ nguyên, nên nâng cấp lại là phục hồi đủ.

`reset()` phải xoá cả follower — với thiết kế này thì tự đúng, vì chúng nằm trong
`words`.

## Bất biến về hiệu năng và bộ nhớ

Viết ra để thành test, không phải để làm khẩu hiệu:

1. **`learn` không bao giờ rebuild.** Đường bigram là O(4) và không cấp phát. Cái
   rebuild có sẵn — `learn_inner` gọi `rebuild_tries()` khi evict một từ đã promoted,
   đo được **1,82 ms** so với 2,39 µs của một learn thường — đã được gỡ ở PR #280 và
   dồn về `flush`. Chính generation tag của bước 1 là thứ làm cho việc hoãn đó an toàn,
   nên nó được giao sớm ở đó thay vì làm hai lần.
2. **Truy vấn có ngữ cảnh: 0 cấp phát khi ấm.** Mở rộng `tests/alloc_budget.rs`.
3. **Không gọi vào `funput-core`, không đọc document, không đụng preedit** từ đường
   gợi ý. Chỉ đường chấp nhận gợi ý được ghi, và đường đó đã tồn tại.
4. **Trần bộ nhớ tĩnh.** Không cấu trúc động nào được thêm; đo được **256 KiB** cho
   5000 từ, biết trước, không phụ thuộc lịch sử gõ. Retained heap đi từ 572 KB lên
   933 KB, so với trần 4 MiB mà test đang gác.
5. **UI chỉ đổi text.** Bar cao cố định, 3 ô cố định, không tạo view, không animation;
   danh sách ứng viên giống lần trước thì **bỏ qua hoàn toàn**, không đụng view.

## Kiểm thử và cổng gác

- `scripts/check-loc.sh` giới hạn 150 dòng/tệp và mỗi thư mục tối đa 5 tệp → phần mới
  nằm ở **`src/bigram/`**: `follower.rs` (kiểu cạnh), `slots.rs` (nạp slot + lão hoá),
  `learning.rs` (`learn_after`) và `query/` tách theo chế độ (`rerank.rs`,
  `predict.rs`). `engine/learning.rs` và `engine/query.rs` không phình.
- Alloc-budget: cả ba đường — `suggest`, `suggest_with(Some, "ch")` và
  `suggest_with(Some, "")` — đều 0 cấp phát khi ấm.
- Bench criterion song song `suggestions_lookup`, kèm trần p99.
- Test riêng cho **chỉ số tái sử dụng**: học đầy `max_words`, ép evict, khẳng định
  cạnh cũ chết chứ không trỏ sang từ mới.
- Test round-trip lưu trữ: nạp snapshot **v1** phải thành công và không mất từ nào.
- Test replay journal có bản ghi ngắt: cặp qua ranh giới câu không được sinh ra.

## Thứ tự hiện thực

1. **`Follower` + generation tag trong `WordRecord`**, chưa có API công khai nào. Chỉ
   cấu trúc dữ liệu và test chỉ-số-tái-sử-dụng. Bước này một mình đã đóng lại cạm bẫy
   nguy hiểm nhất, và nó kiểm chứng được mà không cần đụng tới nền tảng.
   → **#280** (generation tag) và **#281** (mảng follower). Nửa generation tag được
   giao sớm cùng việc gỡ rebuild, vì đó là cùng một cơ chế.
2. **`learn_after` + luật nạp slot và lão hoá.** Vẫn chưa ai đọc dữ liệu ra.
   → **#281**. **Đã đổi khi hiện thực**: kiểm chứng bằng truy cập trong crate chứ không
   qua `stats()` — struct đó đi qua C ABI, JNI, Swift và Kotlin, và một trường chỉ để
   test không đáng bốn tầng.
3. **Lưu trữ**: đọc v1/v2, ghi v2, đánh dấu nối trong journal. Làm **trước** khi có
   consumer, để không có phiên bản nào ngoài đời ghi ra tệp mà bản sau đọc sai.
   → **#282**. Kèm một lỗi mà chính bước này kích hoạt: `enforce_capacity` dùng
   `swap_remove` mà không bump generation, nên sau khi thu nhỏ `max_words`, một cạnh
   đọc từ đĩa sẽ khớp với từ vừa chiếm slot và đọc ra là sống.
4. **`suggest_with` chế độ Rerank.** Consumer đầu tiên. Rẻ nhất, an toàn nhất, và
   không thêm một lần cập nhật UI nào.
   → **#283**. Ngữ cảnh được phép **bổ sung** ứng viên chứ không chỉ xếp lại: node trie
   chỉ nhớ ba từ theo tần suất, nên một cặp nhọn mà từ đích hiếm gặp toàn cục sẽ không
   bao giờ tới được bộ xếp lại.
5. **C ABI + JNI**, rồi giữ `prev` trong service của iOS và Android, cùng danh sách
   điều kiện reset ở mục "Ranh giới ngữ cảnh". Đây là bước đầu tiên người dùng thấy
   được khác biệt.
   → **#284** (biên), **#285** (iOS), **#286** (Android).
6. **Chế độ Predict** cùng ngưỡng áp đảo, kèm quy tắc viết hoa cho prefix rỗng và
   quy tắc bỏ qua cập nhật UI khi danh sách không đổi.
   → **#287** (crate), **#288** (iOS), **#289** (Android). **Đã đổi khi hiện thực**:
   chỉ hiện **ứng viên dẫn đầu**, hai ô còn lại để trống — chính ngưỡng áp đảo đã nói
   phần còn lại của phân phối là vụn, nên hiện chúng ra là tự mâu thuẫn. Dùng chung
   công tắc "Gợi ý từ" hiện có, không thêm preference nào.
7. Đo trên máy thật. **Tiêu chí dừng: nếu bước 6 làm p99 độ trễ phím xấu đi, giữ vĩnh
   viễn ở bước 5.** Tính năng này không đáng đánh đổi cảm giác gõ.
   → **Chưa làm.** Ba con số dưới đây đều là phỏng đoán và chờ hiệu chỉnh:

   | Hằng | Hiện tại | Quyết định điều gì |
   |---|---:|---|
   | `context_dominance_percent` | 40 | Follower dẫn đầu phải chiếm bao nhiêu phần thì bar mới nói |
   | `context_predict_uses` | 2 | Thấy cặp mấy lần mới được đoán |
   | `AGING_AFTER` | 256 | Bao lâu thì chia đôi điểm để thói quen mới chen lên được |

   Chỗ đáng ngờ nhất không phải Rust — truy vấn có ngữ cảnh 444 ns, 0 cấp phát — mà là
   **thanh gợi ý cập nhật thêm một lần sau mỗi dấu cách**. Phép so "danh sách không đổi
   thì không vẽ lại" ở bước 6 là để chặn đúng chỗ đó, nhưng chỉ máy thật mới nói được
   nó có đủ hay không.

## Nợ mang theo

- **Đếm lặp `uses`** khi crash giữa `rename` và cắt journal (#279 nêu ra, chưa sửa):
  replay cộng lại `uses` cho các token đã nằm trong snapshot. Không hỏng dữ liệu, chỉ
  sai xếp hạng. Cách chữa: ghi `sequence` vào frame journal để replay bỏ qua phần đã
  gấp vào snapshot.
- **Android coi chữ số là ký tự trong từ** (`isLetterOrDigit`) còn iOS thì không
  (`isLetter`), nên ranh giới token của hai nền tảng lệch nhau. Có sẵn từ trước, không
  do tính năng này sinh ra.
