# Tự sửa lỗi gõ nhầm phím

## Trạng thái

**Rust và iOS đã hiện thực** trên nhánh `feat/typo-correction`: lõi
(`crates/funput-engine/src/correction/`), hai câu truy vấn ở `funput-suggestions`, cầu nối C ABI
+ JNI, và bàn phím iOS (bật sẵn, tắt được trong Cài đặt). Engine để mặc định tắt; mỗi nền tảng
tự bật. Android chưa làm. Mọi quyết định hành vi sống ở tài liệu này; khi hiện thực lệch khỏi bản
viết, cập nhật lại tài liệu trong cùng PR — các mục dưới đây đã được sửa theo đúng những gì
code làm, chỗ nào lệch đều ghi rõ lý do.

Tài liệu này viết đủ chi tiết để một người (hoặc một agent) khác lập kế hoạch hiện thực mà
không cần đọc lại toàn bộ điều tra: mọi điểm nối đều ghi rõ file, hàm và thứ tự gọi.

## 1. Vì sao

Người gõ tiếng Việt không nhìn bàn phím thấy bàn phím hệ thống iOS "nhận phím chuẩn hơn"
Funput. Điều tra 17–18/09/2026 (XCUITest trên bàn phím Telex hệ thống, iOS 27) cho thấy khác
biệt **không nằm ở vùng chạm**:

- Vùng chạm của bàn phím Telex hệ thống **cố định**, không đổi theo ngữ cảnh (sau `ng`, sau
  `gh` hay đầu từ đều như nhau; chỉ lệch cố định ~2pt). Bàn phím tiếng Anh thì có đổi, 3–8pt.
- Ở ô nhập cho phép autocorrect — mặc định của Tin nhắn, Ghi chú, Zalo, Messenger — bàn phím
  hệ thống **sửa lỗi gõ nhầm phím bên cạnh khi kết thúc từ**. Tắt autocorrect thì không sửa.

Chi tiết số đo: [KEY_ACCURACY_INVESTIGATION.md](../../platforms/ios/docs/KEY_ACCURACY_INVESTIGATION.md).

### Hiện trạng Funput còn xấu hơn "gõ ra chữ sai"

Kiểm chứng bằng `funput dev run` (bản `target/release/funput`, 18/09/2026):

| Chuỗi phím (Telex) | Funput hôm nay | Bàn phím hệ thống |
|---|---|---|
| `dduwowfnh ` | `dduwowfnh ` | đường |
| `tpoi ` | `tpoi ` | tôi |
| `khpong ` | `khpong ` | không |
| `vieeyj ` | `vieeyj ` | việt |
| `dduowxj ` | `đuợ ` | được |
| `minhg ` | `minhg ` | mình |

Trong số này **`dduowxj ` nằm ngoài tầm với của bản đầu**: nó ra `đuợ`, mà
`funput_core::is_complete_syllable` nhận là âm tiết hoàn chỉnh. Sửa nó nghĩa là sửa cả những từ
hợp lệ — đúng thứ §11 xếp cuối vì rủi ro cao nhất. Xem §3.4.

Vì âm tiết không hợp lệ, `should_restore` trong
[boundary/mod.rs](../../crates/funput-engine/src/compose/boundary/mod.rs) trả chuỗi phím thô
(English restore). Người dùng nhận ra một chuỗi ký tự vô nghĩa, tệ hơn một từ sai dấu.

## 2. Mục tiêu và ràng buộc

Khi kết thúc một từ mà từ vừa gõ **không phải âm tiết tiếng Việt hợp lệ**, Funput thay nó bằng
âm tiết hợp lệ gần nhất theo **vị trí chạm thật** của từng phím.

Ràng buộc, theo thứ tự ưu tiên:

1. **Không bao giờ sửa một từ hợp lệ.** Điều kiện cứng, không phải ngưỡng điểm.
2. **Không sửa khi không chắc.** Hai ứng viên sát điểm nhau thì để nguyên, chỉ gợi ý.
3. **Tôn trọng ô nhập và người dùng.** Tắt khi host cấm autocorrect hoặc cài đặt tắt.
4. **Hoàn tác một chạm**, và từ đã hoàn tác không bị sửa lại.
5. **Không làm chậm luồng gõ.** Chỉ chạy ở ranh giới từ, trần phép thử cố định, không I/O.
6. **Một bản lõi cho mọi nền tảng**, trong Rust.

### Không thuộc phạm vi (bản đầu)

- Sửa từ **hợp lệ** nhưng sai ngữ cảnh (`bán` ↔ `bạn`, `đưởng` ↔ `đường`). Cần mô hình ngôn ngữ.
- Sửa tiếng Anh, hoặc sửa khi đang ở chế độ tiếng Anh.
- Thêm/bớt/đảo phím. Bản đầu chỉ **thay phím** (xem §11 để mở rộng).
- Vùng chạm đổi theo ngữ cảnh (bàn phím hệ thống cũng không làm cho tiếng Việt).
- Bảng **tần suất** tiếng Việt đóng gói kèm app. Thay vào đó (chốt 16/10/2026) là một **bảng xếp
  hạng viết tay bốn bậc** (`funput-suggestions/data/syllables/vi.txt`, ~2 100 âm tiết): bậc 1 là
  từ chức năng và từ rất thường gặp, bậc 4 là từ hiếm nhưng có thật. Chọn bậc chứ không chọn số
  đếm vì hai lý do. Một là **giấy phép**: mọi bảng tần suất sẵn có đều lấy từ kho ngữ liệu không
  tương thích MIT (Viet74K không bao giờ được đóng gói), còn một bảng viết tay không trả lời được
  câu hỏi "số này đo ở đâu" thì cũng không nên giả vờ là số đo. Hai là **độ chính xác cần thiết**:
  prior chỉ cộng vào điểm chạm dưới dạng `ln(1 + uses)`, nên thứ tự giữa các bậc đã là toàn bộ
  thông tin engine dùng được; một con số chính xác đến hàng đơn vị không đổi quyết định nào. Mỗi
  bậc ánh xạ sang số "lần dùng giả" (`SYLLABLE_TIER_WEIGHTS = [4, 3, 2, 1]`, đo ở §12 đợt 3) để
  `choose_correction` không phải đổi công thức. Đây là **prior**, không phải cửa chặn:
  `is_known_word` không hỏi bảng này.
- Desktop tự động sửa — xem §10.

## 3. Hành vi

### 3.1. Điều kiện chạy

Cần đủ **tất cả**:

| Điều kiện | Kiểm ở đâu |
|---|---|
| Phím vừa gõ là ranh giới từ (dấu cách, dấu câu, Enter) | `is_word_boundary` trong `boundary/mod.rs` |
| Chế độ tiếng Việt, method Telex / Telex nâng cao / VNI | `session.config` |
| Buffer **không** là âm tiết hoàn chỉnh | `boundary::judge` (một lần cho cả restore lẫn correction) |
| Buffer không phải nguyên âm trần (`ă`, `â`) | `funput_core::is_bare_shaped_vowel` |
| Người dùng chưa tự lật dạng chữ cho từ này | `session.restore_override` |
| Không phải gõ tắt (shortcut khớp) | `boundary/shortcut.rs` chạy trước |
| Từ không chứa chữ số (trừ VNI), không toàn chữ hoa | `correction::eligible` |
| Từ chưa bị người dùng hoàn tác | `correction::undone_before` |
| Cài đặt `typo_correction` bật | `EngineConfig` |
| Host cho phép autocorrect | nền tảng, §8.1 và §9.1 |
| Có dữ liệu chạm cho **mọi** phím của từ, khớp với `session.keys` | `TouchLog::matches` |

**Đã bỏ so với bản thiết kế:** điều kiện "không rơi vào `keystrokes_intend_vietnamese`". Chính
hai ca phải sửa của §3.4 lại bị nó bắt — `dduowxj` có `đ` trong buffer, `d9u7o7nh2` có chữ số
trong keys. Hàm đó trả lời "có nên quay về chữ Latin thô không", không phải "từ này đã xong
chưa". Riêng chữ số: VNI dùng chữ số làm phím dấu nên luật "có chữ số thì bỏ qua" chỉ áp cho
Telex.

### 3.2. Khi sửa

- Thay phần từ đang hiển thị bằng ứng viên thắng, **giữ nguyên ký tự ranh giới**.
- Giữ kiểu chữ theo từ gốc: `Nhad ` → `Nhà `, `NHAD ` không bị sửa (toàn hoa → bỏ qua).
  (Bản thiết kế viết `Nhsf `; chuỗi đó không có nguyên âm nên Telex không ra `Nhà`.)
- Thanh gợi ý hiện chip hoàn tác `↩ dduwowfnh` cho tới khi người dùng gõ phím tiếp theo.

### 3.3. Hoàn tác

- **Xoá ngay sau khi sửa**: trả lại đúng chuỗi người dùng đã gõ (bao gồm ký tự ranh giới), và
  đánh dấu từ đó "không sửa lại". Dấu này **tiêu thụ một lần**: gõ lại từ đó lần nữa trong cùng
  phiên vẫn được đề nghị sửa, giống bàn phím hệ thống.
- Gõ phím khác: mất khả năng hoàn tác một chạm, như iOS.
- Chạm chip trên thanh gợi ý: tương đương bấm Xoá.

### 3.4. Ví dụ (đã kiểm bằng engine)

**Sửa** — từ không hợp lệ, một phím kề:

| Gõ | Hôm nay | Sau tính năng | Phép sửa |
|---|---|---|---|
| `dduwowfnh ` | `dduwowfnh ` | **đường** | h → g |
| `tpoi ` | `tpoi ` | **tôi** | p → o |
| `khpong ` | `khpong ` | **không** | p → o |
| `vieeyj ` | `vieeyj ` | **việt** | y → t |
| `minhg ` | `minhg ` | **mình** | g → f |
| `quaa ` | `quâ ` | **quá** | a → s |
| `d9u7o7nh2 ` (VNI) | `d9u7o7nh2 ` | **đường** | h → g |

`quaa ` là ca *đang hiển thị dạng đã ghép* trước phím ranh giới. Thứ phải xoá là những gì
màn hình hiển thị **sau** phím đó: với English restore bật (mặc định) ranh giới đã trả về 4 phím
`quaa` nên xoá 4; tắt restore thì màn hình còn `quâ` nên xoá 3. Bảng test
`tests/correction/table.rs` giữ đúng các hàng này, `tests/correction/protocol.rs` ghim cả hai
con số.

**Hai ứng viên** — `nhad `: `d→f` ra **nhà**, `d→s` ra **nhá**. Ngón lệch về `f` chọn nhà, lệch
về `s` chọn nhá, chạm giữa `d` thì **không sửa** và đưa cả hai lên thanh gợi ý.

**Không đụng** — `bans ` → bán, `banj ` → bạn, `d9u7o7ng3 ` → đưởng: đều hợp lệ.

**`dduowxj ` không sửa được ở bản đầu.** Nó ra `đuợ`, và `is_complete_syllable("đuợ")` là
`true`, nên luật cứng "không bao giờ sửa một từ hợp lệ" (§2) chặn lại. Muốn sửa phải mở sang
"sửa từ hợp lệ theo ngữ cảnh" — mục rủi ro nhất ở §11 — nên nó ở lại danh sách không đụng cho
tới lúc đó.

## 4. Kiến trúc

```mermaid
flowchart LR
    A[Bàn phím: phím + điểm chạm] -->|"1. process_key_with_touch"| B[funput-engine<br/>Session: keys + touch ring]
    B -->|"2. ở ranh giới từ: candidates"| C[correction/<br/>sinh · phát lại · lọc · chấm theo chạm]
    C -->|"3. tối đa 8 ứng viên"| D[Nền tảng]
    D -->|"4. hỏi tần suất"| E[funput-suggestions<br/>kho từ đã học]
    D -->|"5. apply_correction index"| B
    B -->|"6. ImeResult send"| F[Ghi tài liệu]
```

Hai vòng qua nền tảng giữ `funput-engine` **không phụ thuộc** `funput-suggestions`, đúng như
quan hệ hiện nay giữa hai crate.

| Tầng | Thêm gì |
|---|---|
| `crates/funput-engine/src/correction/` (mới) | Sinh ứng viên, phát lại, lọc, chấm theo chạm |
| `crates/funput-engine/src/model/session.rs` | Đúng một trường `correction: Option<Box<CorrectionState>>` (`model/` đã đủ 5 file; state nằm trong `correction/state.rs`). Box vì state ~900 B — `size_of::<Engine>()` chỉ tăng một con trỏ 8 byte (cùng `NumberGlue` của gõ tắt sau số: 136 → 152) |
| `crates/funput-engine/src/model/config.rs` | `typo_correction: bool` (mặc định `false` cho tới khi bật theo nền tảng) |
| `crates/funput-engine/src/compose/boundary/mod.rs` | Gọi correction **trước** English restore |
| `crates/funput-ffi/src/engine/` | 3 hàm C mới (§7) |
| `crates/funput-jni/src/engine/` | 3 hàm JNI tương ứng |
| `crates/funput-suggestions/src/engine/query.rs` | `pub fn frequency(&self, word: &str) -> u32` |
| iOS `KeyboardTouchUIKit`, `KeyboardRenderer`, `KeyboardInput`, `FunputEngine` | §8 |
| Android `keyboard-renderer`, `ime` | §9 |

## 5. Thuật toán

### 5.1. Đầu vào

Mỗi phím trong từ đang gõ mang theo tối đa 3 phím thay thế kèm khoảng cách, tính theo **bước
phím** (pitch = bề rộng phím + khe):

```rust
/// One typed key and the keys the finger was near, nearest first.
let touch = KeyTouch::new('h', 0.35)
    .with_alternate('g', 0.15)
    .with_alternate('j', 0.60);
```

Trường để riêng tư và dựng bằng builder, vì bên trong engine chúng được lượng tử hoá: `Session`
derive `Eq`, mà `f32` thì không. Khoảng cách lưu Q8.8 (`u16`), điểm lưu milli-nat (`i32`);
`CorrectionCandidate::touch_score()` đổi ngược khi nền tảng đọc.

Nền tảng tự tính danh sách này từ hình học đang hiển thị; lõi không biết bố cục bàn phím. Phím
nào **không** có dữ liệu chạm (gõ tắt, dán, bàn phím vật lý) làm **cả từ** mất khả năng sửa —
bằng chứng khuyết một nửa còn tệ hơn không có. Cũng vậy khi chuỗi phím bị viết lại sau lưng
(phím dấu bị revert, `adopt`, Xoá): log tự đánh dấu không dùng được, và hệ quả luôn là *không
sửa*, không bao giờ là sửa sai.

### 5.2. Sinh ứng viên

```
cho mỗi tập con S các vị trí trong từ, |S| ≤ 2:
    cho mỗi tổ hợp phím thay tại S:
        phát lại toàn bộ chuỗi phím qua một Session nháp (cùng config)
        nếu kết quả là âm tiết hoàn chỉnh hợp lệ → giữ làm ứng viên
```

- **Trần:** từ dài 2–10 phím; tối đa 3 thay thế mỗi phím; tối đa 2 phím thay.
  Số chuỗi phải phát lại tối đa `10×3 + C(10,2)×9 = 435` (không tính chuỗi gốc — nó đã được
  biết là không hợp lệ). Đo thật: một từ 9 phím, mỗi phím 3 phím kề, tốn 4509 lần cấp phát —
  đúng cỡ một lần cho mỗi phím được phát lại, và chỉ trả một lần cho mỗi từ được sửa.
- **Phát lại** chạy thẳng `compose::pipeline::process` trên một `Session` nháp dùng lại (không
  qua `Engine`, để không chạm `prepare_key`, ranh giới từ hay trạng thái viết hoa), nên mọi luật
  Telex, VNI, Telex nâng cao, đặt dấu kiểu cũ/mới đều đúng theo cấu hình hiện tại. Không có bản
  sao luật thứ hai.
- Config nháp **ép tắt** `smart_restore`, `eager_restore`, `spell_check`, `auto_capitalize`:
  nếu không, chính cơ chế English restore sẽ biến một ứng viên hợp lệ thành chữ thô giữa chừng
  và bước kiểm tra sẽ chấm nhầm chuỗi phím thay vì âm tiết. Vòng phát lại cũng phải tự làm hai
  việc `process_key` làm quanh pipeline: chặn chữ số mở đầu từ, và đẩy phím vào `keys`.
- Phím dấu (`s f r x j w z`, nhân đôi nguyên âm, chữ số VNI) được đối xử như mọi phím khác;
  nhờ vậy `dduowxj → dduowcj` và `nhsf → nhaf` nằm trong cùng một cơ chế.

### 5.3. Chấm điểm

```
score = Σ_i  log P(chạm_i | phím_i)  +  log prior(từ)  −  λ × số phím thay
P(chạm | phím) = exp(−d² / (2σ²)),  d tính theo bước phím
```

| Hằng số | Giá trị khởi điểm | Ý nghĩa |
|---|---|---|
| `σ` | 0,45 pitch | Độ tản của điểm chạm quanh tâm phím |
| `λ` | 1,2 | Phạt mỗi phím thay, để 1 phím luôn hơn 2 phím |
| `Δ` | **1,5** | Biên tối thiểu giữa ứng viên nhất và nhì để dám sửa (đo ở §12.1) |
| `prior` nền | 0,5 | Điểm cho âm tiết hợp lệ chưa từng gõ |

- `prior(từ)` = `ln(1 + uses)` + nền, với `uses` = **`syllable_weight(từ)` + số lần người dùng đã gõ
  từ đó**, cả hai lấy từ `funput-suggestions` (§7.3). `syllable_weight` đọc bảng xếp hạng bốn bậc
  đóng gói sẵn (§2) và trả số "lần dùng giả" của bậc:

  | Bậc | Nội dung | `uses` giả | `prior` (nat) | hơn từ ngoài danh sách |
  |---|---|---|---|---|
  | 1 | từ chức năng, từ rất thường gặp (188) | 4 | 2,11 | +1,61 |
  | 2 | từ vựng đời thường (492) | 3 | 1,89 | +1,39 |
  | 3 | thông dụng nhưng ít gặp hơn (898) | 2 | 1,60 | +1,10 |
  | 4 | hiếm nhưng có thật (522) | 1 | 1,19 | +0,69 |
  | — | không có trong danh sách | 0 | 0,50 | 0 |

  Nền tảng cộng hai số rồi truyền vào `choose_correction`; engine không biết số nào từ đâu ra.
  Trọng số cố ý **nhẹ**: bậc 1 hơn một từ ngoài danh sách đúng 1,61 nat, vừa qua Δ = 1,5 — tức
  nó chỉ đủ **phá hoà** khi điểm chạm không phân biệt được hai ứng viên (`nhad` chạm giữa `f`/`s`
  → `nhà`), chứ không lật được một ngón tay đã nghiêng rõ. Bản đầu định `[40, 12, 4, 1]` (bậc 1
  hơn 3,7 nat ≈ 2Δ) và bộ đo bác ngay: xem §12 đợt 3.
- **Chữ nguyên như đã gõ cũng là một ứng viên**, điểm = điểm chạm gốc − `INVALID_COST` (**4,0**,
  §12.1 đợt 2): chỉ một lần chạm cách tâm phím dưới khoảng 0,15 bước phím mới được tin là cố ý. `runner_up` bắt đầu từ điểm đó, không phải âm vô cực. Trước đợt 2, một ứng viên
  duy nhất luôn thắng dù ngón đặt ngay giữa phím — nên `ko`, `atlas`, `robot` gõ đúng vẫn bị đổi.
- **Không sửa** nếu `best − runner_up < Δ`; khi đó gửi cả hai lên thanh gợi ý. Nếu thứ cản là
  chữ nguyên thì engine đếm vào `kept_as_typed`, nếu là ứng viên thứ hai thì `skipped_ambiguous`.
- `σ`, `λ` là hằng số khởi điểm. **`Δ` đã chốt ở 1,5** sau khi đo (§12.1): nó là knob **yếu**
  — đẩy tiếp lên 2,5 chỉ hạ sửa sai từ 3,9% xuống 2,3% mà mất thêm một phần tư số lần sửa đúng
  — nhưng 1,0 → 1,5 là đoạn duy nhất đáng đổi. Thứ thật sự quyết định vẫn là mức nhiễu của ngón.
- **Mặt nạ `allowed`**: nền tảng nói ứng viên nào từ điển của nó công nhận. Việc lọc nằm trong
  cùng hàm quyết định chứ không ở phía gọi, vì chỉ số trả về trỏ vào danh sách **chưa lọc**.

  **Ứng viên bị từ chối không được thắng, nhưng vẫn tranh biên Δ.** Đây là tính chất an toàn của
  một từ điển *chưa đầy đủ*, và nó được đo chứ không phải suy đoán: với danh sách 569 âm tiết,
  để ứng viên bị từ chối rơi hẳn khỏi phép so sánh đưa tỉ lệ sửa sai lên **59,6%** — một từ
  thông dụng nhưng sai thắng *không cần tranh* vì đáp án đúng vừa bị gạt. Giữ nó lại trong phép
  so sánh thì còn **5,3%**: từ điển biết quá ít sẽ **sửa ít đi**, chứ không sửa bậy.

**Cửa chặn tiếng Anh là việc của nền tảng.** Một từ tiếng Anh gõ có chủ ý kết thúc ở đúng trạng
thái mà một từ tiếng Việt gõ nhầm kết thúc: chữ thô, không phải âm tiết. `text ` chẳng hạn có
thể với tới `tẻ` (phím `r` ngay cạnh `t`). Lõi không có từ điển tiếng Anh nên nó **giao lại**
danh sách ứng viên; thứ giữ `text` nguyên vẹn là `SuggestionEngine::is_known_word` ở phía nền
tảng, cộng biên Δ và tần suất từ. Test `an_english_word_the_user_typed_on_purpose_still_reaches_the_platform`
ghim đúng sự phân vai này.

### 5.4. Thứ tự ở ranh giới từ

Trong `boundary/mod.rs`, khi gặp phím ranh giới:

1. **Gõ tắt** (`shortcut.rs`) — nếu khớp, dừng.
2. **`judge`** — một lần `is_complete_syllable` (theo `config.syllable_rules`, nên tôn trọng cả phụ âm đầu z/f/w/j khi bật) trả lời cho cả hai việc còn lại. Nó chỉ chạy khi
   có người cần: English restore còn cửa, hoặc correction có dữ liệu chạm dùng được. Nhờ vậy
   host không gửi điểm chạm thì chi phí bằng đúng hôm nay (đo bằng `alloc_budget_correction`).
3. **Correction** — nếu qua hết §3.1: ghi ứng viên vào chỗ chờ, kèm chuỗi mà màn hình sẽ hiển
   thị *sau* keystroke này (`keys` nếu bước 4 restore, `buffer` nếu không). Chỉ ghi, không trả
   về gì: ranh giới đi tiếp như thể không có tính năng.
4. **English restore** — chạy **trong cùng keystroke**, như hôm nay, dù correction có nhận từ
   này hay không. Rồi `session.clear()`. Keystroke này cho ra đúng `ImeResult` như khi tắt tính
   năng; chỗ chờ chỉ ghi lại văn bản đã hiển thị để bước áp dụng biết phải xoá bao nhiêu.

Thứ tự này khác bản thiết kế ở một điểm quan trọng: correction **không** treo dưới nhánh
`should_restore`. Với `dduwowfnh` và `d9u7o7nh2`, eager restore trong `pipeline` đã đổi buffer
thành chuỗi phím thô từ một phím trước đó, nên tới ranh giới `buffer == keys` và
`should_restore` là **false** — đúng với những từ mà tính năng này sinh ra để sửa.

## 6. Giao thức hai bước

Vì lõi không được phụ thuộc kho từ, ranh giới từ chạy hai bước **đồng bộ, trong cùng một
keystroke**, không có `Task`, không chờ I/O:

1. Nền tảng gọi `process_key` cho phím ranh giới như hôm nay, và **áp dụng kết quả như hôm
   nay** — kể cả khi đó là `Action::None`, nghĩa là chính nền tảng chèn ký tự ranh giới.
2. Nền tảng hỏi `has_pending_correction()`. Đây là **một truy vấn riêng**, không phải cờ gắn
   vào `ImeResult`: `FunputResult` là `#[repr(C)]` truyền theo giá trị, `size_of` 268 byte đã bị
   test ghim và header đã phát hành — thêm trường vào là vỡ ABI âm thầm.
3. Nền tảng đọc ứng viên (`correction_candidates`), hỏi tần suất từng ứng viên ở
   `funput-suggestions`, rồi `choose_correction(&uses, &allowed)` trả chỉ số thắng hoặc `None`
   khi hai ứng viên đầu sát nhau (biên Δ). Hàm này chỉ xếp hạng — và chỉ đếm chỉ số — khi đang
   có chỗ chờ; xếp hạng một bản xem trước là việc của `rank_correction` (bước 0).
4. Nền tảng gọi `apply_correction(index)`. Với `Some(i)` engine sinh `ImeResult::send`; với
   `None` (hoặc chỉ số không trỏ vào ứng viên nào) là **no-op** — `Action::None` — vì ranh giới
   đã làm xong mọi việc của nó ở bước 1, kể cả English restore. Trả lời kiểu nào thì chỗ chờ và
   danh sách ứng viên cũng được xoá: `correction_candidates()` rỗng, `choose_correction` trả
   `None` và không đếm gì.

**Số ký tự phải xoá đã tính cả ký tự ranh giới**: bước 1 để nền tảng tự chèn ký tự đó (hoặc
chính engine chèn nó trong chuỗi restore), nên nó *đã* nằm trong tài liệu. Vì vậy `backspace =
số ký tự từ đang hiển thị + 1`, trong đó "đang hiển thị" là *sau* bước 1: với từ đã được English
restore, đó là chuỗi phím (`quaa` → 5), với từ còn dạng ghép là buffer (`quâ` → 4).
`pending_correction_backspace()` trả sẵn con số đó cho host cần dựng batch edit trước khi quyết
định.

**Nếu nền tảng không gọi bước 4** (phiên bản cũ, hoặc lỗi), keystroke kế tiếp chỉ xoá chỗ chờ và
danh sách ứng viên. Không có gì bị hoãn nên không có gì phải phát lại: văn bản đã đúng như khi
tắt tính năng ngay từ bước 1. Thắt lưng thứ hai là `typo_correction` mặc định tắt, nên không host
nào đang chạy bị ảnh hưởng.

**Bước 0 — xem trước (tuỳ chọn).** Host muốn hiện phép sửa *trước* khi người dùng bấm Space gọi
`preview_correction()` sau mỗi phím chữ. Engine chạy đúng phép tìm của ranh giới, nhưng chỉ khi
từ đã "chết" — không còn phím nào cứu được nó: eager restore đã trả buffer về phím thô, hoặc luật
reachability (`is_definitely_invalid_in`) nói nó không thể thành âm tiết nữa, rồi một lần
`is_complete_syllable` xác nhận. Từ còn sống (`dduwow`, `chaof`) trả 0 và **không cấp phát gì**;
từ đã hoàn tác cũng trả 0. Kết quả đứng tới phím chữ kế tiếp (hoặc Xoá, flip, `clear`), nên gọi
lại không tìm lại. `rank_correction(&uses, &allowed)` xếp hạng mà **không đếm** chỉ số, trả
`best`, `second` (chip thứ hai khi mơ hồ) và verdict `Apply / Ambiguous / AsTyped / Refused`.

Bước 1 sau đó **tái dùng** kết quả xem trước nếu `keys` không đổi: ranh giới không tìm lần hai
(`alloc_budget_correction` ghim: Space sau preview ≤ Space của từ không có điểm chạm + 2). Có hay
không có preview, văn bản và danh sách ứng viên ở ranh giới giống hệt (`correction_property`).

`refuse_correction()` là người dùng bấm vào chữ gốc trên thanh gợi ý: giữ nguyên từ, nhớ cách gõ
đó **một lần** đúng như hoàn tác bằng phím Xoá (ranh giới kế tiếp bỏ qua và đếm `suppressed`),
xoá preview, chỗ chờ và undo đang chờ. Gọi được cả khi từ còn đang gõ lẫn khi ranh giới vừa ghi
chỗ chờ. Preview là **lời gọi tường minh**, không nằm trong `process_key`, nên ngân sách cấp phát
của đường gõ phím không đổi.

## 7. API

### 7.1. Rust (`funput-engine`)

```rust
impl CorrectionCandidate {
    pub fn text(&self) -> &str;
    /// Touch evidence only, in nats; the platform adds the word prior.
    pub fn touch_score(&self) -> f32;
    pub fn edits(&self) -> usize;
}

impl Engine {
    pub fn set_next_key_touch(&mut self, touch: KeyTouch);
    pub fn has_pending_correction(&self) -> bool;
    pub fn correction_candidates(&self) -> &[CorrectionCandidate];
    /// The word plus the boundary character the platform already echoed.
    pub fn pending_correction_backspace(&self) -> usize;
    /// `uses` is parallel to the candidates; `&[]` gives the touch-only ranking.
    /// `allowed` vetoes candidates; a short or empty mask permits the rest.
    pub fn choose_correction(&mut self, uses: &[u32], allowed: &[bool]) -> Option<usize>;
    pub fn apply_correction(&mut self, index: Option<usize>) -> ImeResult;
    /// While Backspace would still undo the last correction.
    pub fn has_correction_undo(&self) -> bool;
    pub fn correction_undo_text(&self) -> Option<&str>;

    /// Before the word ends: the boundary's search, run early. 0 while the word
    /// could still become a syllable. Explicit — never part of `process_key`.
    pub fn preview_correction(&mut self) -> usize;
    /// Rank without counting anything.
    pub fn rank_correction(&self, uses: &[u32], allowed: &[bool]) -> CorrectionRanking;
    /// Keep the word as typed, and do not offer this spelling again next time.
    pub fn refuse_correction(&mut self);
}

pub struct CorrectionRanking {
    pub best: Option<usize>,
    /// The best allowed candidate after `best` — the second chip when ambiguous.
    pub second: Option<usize>,
    pub verdict: CorrectionVerdict, // Apply | Ambiguous | AsTyped | Refused
}
```

`choose_correction` nằm trong Rust chứ không để mỗi nền tảng tự viết, để công thức prior và
luật Δ chỉ tồn tại một bản. `has_correction_undo` là bắt buộc với host nào đang để phím Xoá tự
đi qua: từ nay `on_backspace` có lúc trả `Action::Send`.

### 7.2. C ABI (`funput-ffi`)

Đã hiện thực (`crates/funput-ffi/src/engine/correction/`), trả `FunputResult` theo giá trị như
mọi hàm engine khác:

```c
void         funput_engine_set_next_key_touch(FunputEngine*, const FunputKeyTouch*);
bool         funput_engine_has_pending_correction(const FunputEngine*);
uint32_t     funput_engine_correction_candidates(const FunputEngine*, FunputCorrectionCandidate* out, uint32_t cap);
uint32_t     funput_engine_pending_correction_backspace(const FunputEngine*);
int32_t      funput_engine_choose_correction(const FunputEngine*, const uint32_t* uses, uint32_t len);
FunputResult funput_engine_apply_correction(FunputEngine*, int32_t index);
uint32_t     funput_engine_correction_undo_text(const FunputEngine*, uint32_t* out, uint32_t cap);
void         funput_set_typo_correction(FunputEngine*, bool);

/* Trước khi từ kết thúc (thanh gợi ý) */
uintptr_t               funput_engine_preview_correction(FunputEngine*);
void                    funput_engine_refuse_correction(FunputEngine*);
FunputCorrectionRanking funput_engine_rank_correction(const FunputEngine*, const uint32_t* uses,
                                                      const bool* allowed, uintptr_t len);

typedef struct { int32_t best; int32_t second; uint32_t verdict; } FunputCorrectionRanking;
#define CORRECTION_VERDICT_APPLY     0
#define CORRECTION_VERDICT_AMBIGUOUS 1
#define CORRECTION_VERDICT_AS_TYPED  2
#define CORRECTION_VERDICT_REFUSED   3
```

`funput_engine_rank_correction` tự bọc guard như `choose`: `Default` của struct là `{0, 0, 0}`,
tức "áp dụng ứng viên 0" — handle null hay panic phải trả `{-1, -1, 3}`.

`funput_set_typo_correction` là setter riêng: `FunputConfig` là `#[repr(C)]` truyền theo giá
trị, thêm trường vào là vỡ ABI — đúng lối `funput_set_shortcuts_enabled` đã đi.

Hai điều học được khi làm:

- `funput_engine_choose_correction` **không** dùng được `with_engine_ref`: fallback của nó là
  `Default`, tức `0` — handle null sẽ nói "áp dụng ứng viên đầu tiên". Sentinel là `-1`, nên hàm
  này tự bọc guard. Test `every_call_is_null_safe` bắt được đúng lỗi này.
- Hằng số trong header không được trỏ sang crate khác: cbindgen đặt `parse_deps = false`, nên
  `TOUCH_ALTERNATE_CAP = MAX_ALTERNATES` sinh ra một `#define` gọi tên macro không tồn tại. Viết
  số thẳng, và `const _: () = assert!(...)` giữ hai bên khớp nhau.

`FunputKeyTouch` và `FunputCorrectionCandidate` là `#[repr(C)]`, chuỗi UTF-32 cố định như
`FunputResult` hiện có (`chars: [u32; 64]`). JNI phản chiếu đúng ba hàm này.

### 7.3. `funput-suggestions`

Cả hai đều có mặt ở C ABI (`funput_suggestion_frequency`, `funput_suggestion_is_known_word`)
và JNI (`nativeFrequency`, `nativeIsKnownWord`) — không có chúng thì nền tảng chỉ truyền được
mảng rỗng vào `choose_correction`, tức đúng trường hợp prior đều mà §12.1 đo được là nguy hiểm.


```rust
impl SuggestionEngine {
    /// How many times the user typed `word`, 0 when unknown. Read-only, no I/O.
    pub fn frequency(&self, word: &str) -> u32;

    /// A word the user has typed, one in the shipped English list, or one on the
    /// keep list — the veto.
    pub fn is_known_word(&self, word: &str) -> bool;
}

/// Pseudo-uses per band, band 1 first: about 2.1, 1.9, 1.6 and 1.2 nats through
/// the engine's `ln(1 + uses) + 0.5`, against 0.5 for an unlisted syllable.
pub const SYLLABLE_TIER_WEIGHTS: [u32; 4] = [4, 3, 2, 1];

/// The band `word` sits in on the shipped list (1 = most common), `None` when
/// unlisted. Case and normalization do not matter. No allocation after first use.
pub fn syllable_tier(word: &str) -> Option<u8>;

/// `SYLLABLE_TIER_WEIGHTS` for that band, 0 when unlisted.
pub fn syllable_weight(word: &str) -> u32;
```

**Bảng xếp hạng âm tiết** (`data/syllables/vi.txt`, bốn bậc, tự viết, MIT — §2) được biên dịch
vào `funput-suggestions` và dựng thành bảng tra một lần ở lần gọi đầu (`OnceLock`, sắp theo
byte). Hai hàm `syllable_tier` / `syllable_weight` **không cần handle**: chúng đọc bảng tĩnh,
chuẩn hoá NFC + chữ thường lên bộ đệm trên stack đúng như `context_slot`, rồi tìm nhị phân — không
cấp phát, `tests/alloc_budget.rs` giữ lời hứa đó. Nền tảng cộng `syllable_weight(từ)` vào
`frequency(từ)` để ra `uses` của mỗi ứng viên; `is_known_word` **không đổi** và không hỏi bảng
này, vì cửa chặn dành cho chữ không phải tiếng Việt chứ không phải để phạt âm tiết chưa có trong
danh sách. C ABI: `funput_suggestion_syllable_weight(word, len) -> u32`,
`funput_suggestion_syllable_tier(word, len) -> u32` (0 = không có trong danh sách; giải mã
codepoint vào `[u8; 128]` trên stack, không cấp phát). JNI: `nativeSyllableWeight(String)`,
`nativeSyllableTier(String)`.

**Danh sách giữ nguyên** (`data/correction/keep.txt`, tự viết, MIT) được biên dịch thẳng vào
`funput-suggestions` và `is_known_word` hỏi nó trước tiên — quét tuần tự khoảng 130 chữ, không cấp
phát, không so hoa thường. Nằm ở đây chứ không ở từng nền tảng vì iOS và Android cùng hỏi đúng
hàm này: không phải đóng gói file nào, không có hai bản danh sách để lệch nhau. Nó chặn những chữ
mà `INVALID_COST` không chặn được — `ko`, `mk`, `tl`, `haha` gõ gần mép phím, trông y hệt một lần
trượt (§12.1, đợt 2).

### 7.4. JNI (`funput-jni`)

Một lệnh sửa về Android dưới dạng `IntArray` `[backspace, codepoint…]` — đúng dáng `nativeStats`
đã dùng cho số — nên một lần gọi mang cả hai nửa và không có thứ tự nào để gọi sai:

```kotlin
external fun nativeSetNextKeyTouch(handle: Long, typed: Int, typedDistance: Float,
                                   a1: Int, d1: Float, a2: Int, d2: Float, a3: Int, d3: Float)
external fun nativeHasPendingCorrection(handle: Long): Boolean
external fun nativeCorrectionCandidates(handle: Long): Array<String>
external fun nativePendingCorrectionBackspace(handle: Long): Int
external fun nativeChooseCorrection(handle: Long, uses: IntArray?, allowed: BooleanArray?): Int
external fun nativeApplyCorrection(handle: Long, index: Int): IntArray
external fun nativeHasCorrectionUndo(handle: Long): Boolean
external fun nativeCorrectionUndoText(handle: Long): String

// Trước khi từ kết thúc (thanh gợi ý)
external fun nativePreviewCorrection(handle: Long): Int
external fun nativeRefuseCorrection(handle: Long)
/** `[best, second, verdict]`; -1 cho ô trống, verdict đánh số như header C. */
external fun nativeRankCorrection(handle: Long, uses: IntArray?, allowed: BooleanArray?): IntArray
external fun nativeUndoCorrection(handle: Long): IntArray
external fun nativeSetTypoCorrection(handle: Long, on: Boolean)
```

`nativeUndoCorrection` tách khỏi `nativeBackspace` và là no-op khi `nativeHasCorrectionUndo` sai,
nên một phím Xoá bình thường không bao giờ bị nuốt ở đường này: IME hỏi trước rồi gọi một trong
hai. `String(edit, 1, edit.size - 1)` dựng lại chữ từ phần đuôi.

## 8. iOS

### 8.1. Điểm nối

| Việc | File |
|---|---|
| Phím kề + khoảng cách từ điểm chạm | `KeyboardTouchUIKit/Geometry/KeyboardGeometrySnapshot.swift` — thêm `func alternates(at:limit:) -> [(KeySpec, CGFloat)]` |
| Mang điểm chạm theo sự kiện phím | `KeyboardRenderer/.../KeyboardTouchCoordinator.swift` (đã có `hits`, `beganAt`) → thêm vào `KeyboardKeyEvent` |
| Gọi engine | `FunputEngine/FunputComposer+Text.swift` (đang gọi `funput_process_key_text`) |
| Ranh giới từ và ghi tài liệu | `KeyboardInput/Coordinator/KeyboardInputCoordinator+Actions.swift`, `Transactions/` |
| Tần suất từ | `PersonalSuggestions` (module Swift) |
| Cờ autocorrect của host | `Keyboard/Controller/KeyboardViewController+Traits.swift` — `applyTextInputTraits` đã đọc trait, thêm `autocorrectionType` |
| Chip hoàn tác | `KeyboardRenderer/.../KeyboardToolbarView` (đang có suggestion bar) |
| Công tắc | `Funput/Settings/KeyboardSettingsSections.swift` nhóm "Thông minh" + `FunputConfiguration` (schema v16; v15 là `returnsToLettersAfterPunctuation` của main) |

### 8.2. Ghi tài liệu

Dùng đúng đường đã có cho smart restore: `ImeResult.send(backspace, output)` chạy qua
`InputTransactionBuilder`. Không thêm đường ghi thứ hai, nên `KeyboardDocumentSynchronizer` và
bộ chống echo hiện tại vẫn đúng.

### 8.3. Hoàn tác

✅ Đã hiện thực. Một chỗ phải cẩn thận hơn bản viết: khi hoàn tác nổ, phím Xoá **không được**
đi tiếp vào tài liệu — engine đã nói chính xác xoá bao nhiêu và chèn lại gì, xoá thêm một ký tự
nữa là ăn mất dấu cách người dùng không hề chạm vào. Và `reopenPreviousWord` phải bị chặn cho
đúng lượt đó, nếu không `adopt` sẽ mở lại chính từ vừa bị người dùng từ chối.

Ngoài ra ngữ cảnh tài liệu phải kiểm **trước** khi hỏi engine, vì hỏi là tiêu thụ luôn lệnh hoàn
tác: hỏng sau đó thì engine tin là đã trả chữ về trong khi tài liệu vẫn đang hiện bản sửa.

**Hoàn tác một lần là nhớ mãi.** Engine chỉ tha cho từ vừa hoàn tác **một lần** (lần gõ lại kế
tiếp). Nền tảng làm nốt: `CorrectionDictionary.keep(_:)` ghi chữ gốc vào danh sách của
`CorrectionWordSource`, và từ đó `recognizes` trả `true` cho nó. Danh sách lưu trong App Group
(`TypoKeptWordsStore`), tối đa 200 chữ, bỏ chữ cũ nhất trước, **chỉ khi có Full Access** — cùng
ranh giới với kho từ cá nhân — và bị xoá cùng lúc với kho từ cá nhân (lưu kèm reset token;
khác token thì đọc ra rỗng).

**Đếm lý do và nhật ký Debug.** Dòng log `typo` cuối phiên có thêm: số lần đưa ra lệnh sửa,
vướng Δ, chữ nguyên thắng, bị chặn vì là chữ có thật, bị chặn vì ô nhập, không có ứng viên, và
chữ hợp lệ có chạm gần mép phím. Chỉ số đếm. Riêng bản **Debug** có thêm một dòng cho mỗi từ —
chữ trên màn hình, các ứng viên kèm điểm, và quyết định — để đọc vì sao một lần trượt được hay
không được sửa trên máy đang gõ. Bọc trong `#if DEBUG`, không bao giờ có trong bản TestFlight;
chữ ghi ở mức `.public` vì mục đích của nó chính là để người phát triển đọc được:
`log stream --predicate 'subsystem == "app.funput.keyboard" && category == "typo"' --level debug`.

Phím Xoá ngay sau khi sửa: `KeyboardInputCoordinator` đã có `reopensPreviousWord` cho Backspace.
Việc hoàn tác nằm **trong engine** — `Engine::on_backspace` trả thẳng `ImeResult::send` với chuỗi
gốc và tự bật cờ "không sửa lại". Host chỉ cần hỏi `has_correction_undo()` trước khi để phím Xoá
đi qua, và áp dụng kết quả như mọi `ImeResult` khác.

### 8.4. Thanh gợi ý: thấy phép sửa trước khi bấm Space

✅ Đã hiện thực (B1 #575 + B2). Trải nghiệm kiểu iOS: khi từ đang gõ đã "chết" và engine với
tới một phép sửa, thanh gợi ý hiện `[“chữ gốc”] [ứng viên]` (+ ứng viên thứ hai khi hai ứng viên
đầu sát nhau). Space áp dụng như cũ; bấm chữ gốc giữ nguyên; bấm ứng viên commit nó kèm dấu
cách; sau khi sửa, khe đầu thành chip `↩ chữ gốc`.

**Mô hình.** `CorrectionPreview` (`KeyboardInput/Correction/Preview/`) có hai dạng:
`.offered(typed:candidates:)` và `.applied(undoText:)`. `KeyboardInputCoordinator.
refreshCorrectionPreview()` tính lại nó cuối `input(_:builder:)` (sau `applyPendingCorrection`)
và trong `performDeleteBackward`; `clearComposition` / `resetCompositionState` xoá. Thứ tự rẻ
trước, không chạm FFI nếu không cần: `usesEngine && allowsAutocorrect && usesVietnameseComposition
&& correctionDictionary != nil` → `hasCorrectionUndo` (→ `.applied`) → buffer rỗng → kho từ nhận
ra chữ đang gõ (→ không hỏi engine) → `previewCorrection()` → `rankCorrection(uses:allowed:)` với
`uses = candidates.map(dictionary.weight)` → `[best] + (ambiguous ? [second] : [])`. Signpost
`CorrectionPreview` (category `Input`) đo phần hỏi engine.

**Ba lời gọi engine** đi qua protocol `CorrectionPreviewing` (`FunputEngine/Correction/`), mà
`FunputComposer` conform (`FunputComposer+CorrectionPreview.swift`): `previewCorrection()`,
`rankCorrection(uses:allowed:) -> FunputCorrectionRanking` (best / second, `-1` → `nil`, verdict
`apply / ambiguous / asTyped / refused` khớp `CORRECTION_VERDICT_*`), `refuseCorrection()`.
Protocol tồn tại để test của `KeyboardInput` cắm một bản giả quyết định từ nào "chết"
(`FakePreviewer`), còn `FunputEngineTests/Correction/` kiểm ba lời gọi thật.

**Bấm trên thanh.** `KeyboardSuggestionCandidate.Kind` phân biệt `suggestion` /
`correctionTyped` / `correctionCandidate` / `correctionUndo`; `KeyboardSuggestionBarView` vẽ
`“chữ gốc”` và `↩ chữ gốc`, nhãn trợ năng "Giữ nguyên, …", "Sửa thành, …", "Hoàn tác, …".
`SuggestionBarComposer` (thuần, `PersonalSuggestions/Correction/`) ghép ba khe: khe sửa lỗi
trước, gợi ý cá nhân lấp phần còn lại, bỏ gợi ý trùng chữ với khe sửa lỗi; chạy cả khi gợi ý cá
nhân tắt. `PersonalSuggestionService.update(_:canQuery:shift:correction:)` publish lại khi chỉ
correction đổi; `correctionAction(for:)` kiểm generation rồi trả `.refuse / .accept / .undo`.
Host định tuyến ở `Keyboard/Correction/KeyboardViewController+CorrectionBar.swift`:

- **Giữ nguyên** → `refuseCorrection(typed:writer:)`: engine `refuse_correction` (một lần, đếm
  `suppressed` ở ranh giới kế), `dictionary.keep(typed)` (mãi mãi), đếm
  `CorrectionDeclines.refused`.
- **Sửa thành** → `acceptCorrection(_:writer:)`: đặt `forcedCorrection = text` rồi `commit {
  input(" ") }`. Ranh giới tái dùng preview, `chooseCorrection` trả đúng chỉ số bị ép, phép sửa
  đi cùng transaction với dấu cách — nên undo được arm và tracker học từ đã sửa y như Space.
- **Hoàn tác** → `undoCorrectionFromBar(writer:)`: cùng `undoCorrection(builder:)` với phím Xoá.

**Hai dạng của một từ.** Với eager restore bật (mặc định), từ chết đã hiện dạng phím thô ngay
khi gõ nên chữ gốc trên thanh là `“dduwowfnh”`; tắt eager restore (harness UI test) thì là
`“đườnh”` trong khi ranh giới có thể restore về phím thô. Vì thế `chooseCorrection` chặn khi kho
từ nhận ra **một trong hai** dạng — chữ restore trên màn hình (`shown`) hay âm tiết đã ghép
trước phím ranh giới (`composed`) — và hoàn tác `keep` cả hai khi chúng khác nhau. Thanh gợi ý
và Space không bao giờ nói khác nhau.

**Chính sách ô nhập.** `CorrectionFieldPolicy.allowsAutocorrect(autocorrectionType:keyboardType:
textContentType:)` (`KeyboardInput/Correction/Policy/`): `false` cho `.no`, bàn phím
`.asciiCapable` / `.asciiCapableNumberPad`, và `textContentType` username / password /
newPassword / oneTimeCode / URL / emailAddress. `KeyboardViewController+Traits.swift` đọc nó từ
`textDocumentProxy`, tách khỏi `KeyboardInputContext` để một ô chỉ tắt autocorrect không làm
xoá composition.

**Prior của từ.** `CorrectionDictionary.weight(_:)` (mặc định 0) được truyền thật làm `uses`
cho `chooseCorrection` lẫn `rankCorrection`. `CorrectionWordSource.weight` = `syllableWeight`
(bậc trên `vi.txt`, qua `funput_suggestion_syllable_weight`, không cần handle) `&+` số lần người
dùng gõ xong từ đó trong phiên. Số lần đếm ở `personalCounts` trên main actor
(`CorrectionWordSource+Counts.swift`, tối đa 2 000, loại từ hiếm nhất) — **không** ghi vào
engine Rust, vốn vẫn read-only: `is_known_word` nhận bất kỳ từ có `frequency > 0`, nên một lỗi
gõ bỏ qua một lần mà ghi vào engine là bị "nhận ra" cả phiên. `recognizes` chỉ coi từ là của
người dùng từ **lần thứ hai**; lần đầu chỉ tăng trọng số.

**Điểm chạm lúc đặt ngón.** `KeyboardTouchPipeline` giữ `initialLocations[ContactID]` từ
`.began`; `touchEvidence` đo ở điểm đó thay vì điểm nhấc (ngón trượt một chút trên đường rời
kính, điểm nhấc nói về cú trượt hơn là về phím định bấm). Phím vẫn quyết định ở điểm nhấc.

**Chỉ số.** `FunputCorrectionCounts.suppressed` (từ bị bỏ qua vì vừa hoàn tác / từ chối) và
`CorrectionDeclines.refused` (bấm giữ nguyên trên thanh) có trong dòng log `typo` cuối phiên.

## 9. Android

| Việc | File |
|---|---|
| Điểm chạm | `keyboard-renderer/.../interaction/KeyboardTouchHandler.kt` — đã truyền `x, y` ở `onKeyReleased` |
| Phím kề | `keyboard-renderer/.../layout/KeyboardHitTester.kt` — thêm hàm trả phím gần nhất kèm khoảng cách |
| Gọi engine | `ime/.../nativebridge/FunputNative` (đã có `nativeProcess`, `nativeAdopt`) |
| Ghi tài liệu | `ime/.../editing/CommittedBufferWriter.kt` — một batch edit, như retone |
| Tần suất từ | `PersonalSuggestionNative` (đã có `nativeQuery`) |
| Cờ autocorrect | `EditorInfo.inputType` — bỏ qua khi có `TYPE_TEXT_FLAG_NO_SUGGESTIONS` hoặc ô mật khẩu |

**Đã giải quyết:** `nativeBackspace` và `nativeBoundary` chỉ trả `String` và vứt `ImeResult`
đi, nên chúng không tải nổi một lệnh sửa. Thay vì đổi hai hàm đó (và đổi cả chữ ký Java của
chúng), phần correction có đường riêng trả `IntArray` — xem §7.4.

Hai điều cần nhớ khi nối Kotlin (chưa quan sát được vì IME chưa gọi): `nativeApplyCorrection(-1)`
nay là **no-op** — ranh giới đã restore trong cùng keystroke, nên từ chối không còn trả lệnh sửa
nào; và `nativeRankCorrection` trả `[best, second, verdict]` với `-1` cho ô trống, không bao giờ
là `0` (đó là một chỉ số ứng viên thật).

## 10. Desktop

Lõi dùng lại được, nhưng **hai vế chấm điểm biến mất**:

- Không có điểm chạm (bàn phím vật lý) → chỉ còn phím kề với trọng số bằng nhau.
- `funput-desktop` không nối `funput-suggestions` → không có tần suất.

Còn lại chỉ là "âm tiết hợp lệ + ít phím thay nhất", quá yếu để tự thay chữ. Ngược lại, ghi chữ
lại dễ hơn: macOS, fcitx5 và ibus giữ âm tiết trong preedit tới lúc chốt, nên sửa chỉ là chốt một
preedit khác; Windows dùng inject kèm bản sao văn bản đã gõ, khó ngang iOS.

**Quyết định:** desktop không tự sửa ở bản đầu. Khi làm, chỉ **gợi ý** (gạch chân preedit hoặc
đưa vào danh sách chọn), và trước đó phải nối kho từ đã học cho desktop. Lỗi trên bàn phím vật
lý cũng khác (đảo thứ tự, gõ lặp), nên tập phép sửa phải thiết kế riêng.

## 11. Mở rộng về sau

Xếp theo thứ tự giá trị trên mỗi đơn vị rủi ro:

1. **Thêm/bớt một phím** (`ddleer` → `để`): mở rộng tập phép sửa, giữ nguyên phần chấm điểm.
2. **Đảo hai phím kề** (`nhaf` ↔ `nhfa`): rẻ, hay gặp khi gõ nhanh hai ngón.
3. ~~**Bảng tần suất âm tiết đóng gói**~~ — **đã làm** dưới dạng bảng xếp hạng bốn bậc
   (`syllable_weight`, §2 và §7.3), gắn vào đúng chỗ `prior`, không đụng phần còn lại. Việc còn
   lại là nối harness `funput dev typos --prior shipped` vào bảng và đo lại `INVALID_COST` /
   `MARGIN` với prior mới (§12) — PR kế tiếp.
4. **Sửa từ hợp lệ theo ngữ cảnh** (`bán` → `bạn`): cần bigram; rủi ro cao nhất, làm sau cùng.

## 12. Kiểm thử và cổng gác

1. **Kho lỗi tổng hợp**: ✅ `funput dev typos` (`crates/funput-cli/src/dev/typos/`). Lấy âm tiết
   hợp lệ từ đúng bộ ngữ liệu của `funput dev coverage`, gõ lại với ngón trượt theo nhiễu Gauss
   quanh tâm phím trên lưới QWERTY so le, rồi đếm.

   **Đã tách hai chữ σ.** Bản thiết kế dùng một chữ σ cho hai thứ khác nhau: độ tản *thật* của
   ngón tay, và độ tản engine *tin* là có khi chấm điểm. Bộ đo gọi cái đầu là `--noise`, vì đúng
   chỗ hai cái lệch nhau mới đáng đo.

   **Số thật, trên Viet74K (8 955 âm tiết).** `benchmarks/sample.txt` chỉ có 137 âm tiết nên kho
   từ học từ nó gần như luôn phân biệt được đúng/sai — số ở đó quá lạc quan, đừng dùng để quyết
   định:

   | Nhiễu ngón | Kho từ | Sửa đúng | **Sửa sai** |
   |---|---|---|---|
   | 0,20 | không | 54,6% | 0,8% |
   | 0,20 | Viet74K | 57,3% | **0,6%** |
   | 0,25 | không | 48,7% | 8,8% |
   | 0,25 | Viet74K | 51,9% | **6,8%** |
   | 0,30 | Viet74K | 42,9% | **14,8%** |

   Quét Δ và các bộ lọc, ở nhiễu 0,25 với kho từ Viet74K:

   | | Sửa đúng | Sửa sai |
   |---|---|---|
   | Δ = 1,0 | 51,9% | 6,83% |
   | Δ = 1,0 + chỉ từ có thật | 54,4% | 5,63% |
   | **Δ = 1,5 + chỉ từ có thật** ← đã chốt | **50,3%** | **3,87%** |
   | Δ = 2,0 + chỉ từ có thật | 42,6% | 2,86% |
   | Δ = 2,5 + chỉ từ có thật | 35,9% | 2,28% |
   | Δ = 1,0 + chỉ 1 phím thay | 47,8% | 6,42% |

   **Bộ lọc cần độ phủ, không cần bắt đầu sớm.** Đo trên Viet74K ở nhiễu 0,25, so ba cỡ từ điển:

   | Từ điển dùng làm bộ lọc | Sửa đúng | Sửa sai |
   |---|---|---|
   | không lọc | 41,9% | 5,66% |
   | danh sách ship (569 âm tiết) | 4,4% | 5,30% |
   | Viet74K (8 955 âm tiết) | 46,3% | 3,77% |

   Danh sách nhỏ **không phải bước đệm** tới danh sách lớn: ở 569 âm tiết nó cắt chín phần mười
   số lần sửa mà gần như không hạ được tỉ lệ sửa sai. Nên nền tảng **để bộ lọc tắt** cho tới khi
   độ phủ đủ lớn, và ngưỡng đó phải đo bằng `funput dev typos --prior shipped --known-only` chứ
   không đoán. Dùng danh sách nhỏ làm **prior** cũng không đổi gì (5,88% so với 5,66%), vì
   `ln(1 + uses)` quá nhỏ bên cạnh biên Δ.

   Với Δ = 1,5 (hằng số hiện tại) và bộ lọc từ-có-thật, cả đường cong nhiễu là:

   | Nhiễu ngón | Sửa đúng | Sửa sai |
   |---|---|---|
   | 0,20 | 54,6% | **0,42%** |
   | 0,25 | 50,3% | **3,87%** |
   | 0,30 | 43,0% | **8,93%** |

   Ba điều đọc được, và cả ba đều quan trọng hơn con số:

   - **Cổng "σ = 0,45" của bản thiết kế không đạt được.** Ở mức đó gần như từ nào cũng trượt
     nhiều hơn một phím và gần nửa số lần sửa là sửa hỏng.
   - **Δ có đòn bẩy kém.** Đẩy Δ lên 2,5 đánh đổi một nửa số lần sửa đúng mà vẫn còn 2,3% sửa
     sai. Sửa sai không đến từ chỗ hai ứng viên sát nhau, mà từ chỗ mô hình **tự tin mà sai**.
   - **Giới hạn 1 phím thay không giúp** (6,42% so với 6,83%). Một phím trượt duy nhất cũng đủ
     rơi vào một từ thật khác — bản chất bài toán, không phải chuyện chỉnh tham số.

   Nên **vùng an toàn duy nhất là nhiễu ≈ 0,20** (0,6% sửa sai, vá 57%). Ở 0,25 thì cứ 15 lần
   sửa có 1 lần viết lại một từ người dùng cố ý gõ.

   **Đã đo trên máy thật (20/09/2026, iPhone 15 Pro, iOS 27, VNI):** σ ≈ **0,174 ± 0,011** trên
   135 phím, hai phiên gõ tự nhiên. Chạy bộ đo đúng mức đó trên Viet74K: 2,5% số từ có phím
   trượt, vá được 1,1% số từ, và **viết sai 0,01% số từ — 1 trên 8 955**. Kể cả ở cận trên của
   khoảng tin cậy (0,19) vẫn là 0,02%.

   Hai lưu ý để người đọc sau không tin quá mức: mẫu nhỏ (135 phím, một người, hai phiên), và
   con số quy từ khoảng cách tới tâm phím sang σ mỗi trục theo phân phối Rayleigh — hai ước
   lượng độc lập (từ trung bình và từ độ lệch) ra 0,155/0,162 và 0,186/0,182, khớp nhau, nên mô
   hình Gauss hai chiều là hợp lý chứ không phải giả định suông.

   Cùng lần đo: tìm ứng viên nặng nhất **568 µs** trên iPhone 15 Pro — dưới xa ngưỡng 2 ms, và
   thấp hơn ước lượng ~1,5 ms tôi đưa cho máy đời cũ.

   Trước khi có số này thì **chưa ai biết nhiễu thật của người dùng Funput là bao nhiêu** — và
   đó là con số quyết định.
   `KEY_ACCURACY_INVESTIGATION.md` đo *vùng chạm*, không đo độ phân tán của ngón; chỉ biết nửa
   phím ≈ 20pt, tức pitch ≈ 40pt, nên 0,20 là 8pt và 0,25 là 10pt — gõ hai ngón cái trên điện
   thoại nằm đúng quanh khoảng đó. **Việc đầu tiên của đợt iOS phải là đo con số này trên máy
   thật.**

   - Cổng trong CI (trên `sample.txt`, nơi chạy được không cần tải ngữ liệu): sửa sai **< 1%**
     số lần sửa ở nhiễu 0,25, và sửa đúng ≥ 50% số từ trượt ở **nhiễu đo trên máy (0,174)**,
     cộng 10 seed. Cổng sửa đúng từng đặt ở 0,25 — con số đoán trước khi đo; đợt 2 dời nó về
     nhiễu thật (xem dưới). Một test riêng ghi lại vách 0,45 khi không có kho từ, thay vì giả vờ
     nó an toàn.

   **Đợt 2 (26/09/2026): chữ gõ có chủ ý.** Thử trên máy thật lộ ra một lỗ mà bộ đo cũ không
   nhìn thấy, vì nó chỉ gõ âm tiết tiếng Việt: một ứng viên duy nhất **luôn thắng**, nên chữ không
   phải tiếng Việt nhưng gõ đúng ý — viết tắt khi chat, tên hãng, từ mượn — bị đổi. Ba việc:

   - **Chữ nguyên tham gia so sánh** (`INVALID_COST`, §5.3). Đo bằng chế độ mới
     `funput dev typos --keep` trên `crates/funput-suggestions/data/correction/keep.txt` (chữ
     viết tắt, tên hãng, tên không dấu — tự viết, MIT), 40 seed, nhiễu 0,174, không có từ điển
     chặn; cột phải là Viet74K, 8 seed, không có kho từ:

     | `INVALID_COST` | Chữ cố ý bị đổi, VNI / Telex | Sửa đúng, VNI / Telex |
     |---|---|---|
     | không có (trước đợt 2) | 24,5% / 26,4% | 66,9% / 51,7% |
     | **4,0** ← đã chốt | **4,5% / 5,1%** | **66,5% / 51,5%** |
     | 3,0 | 0,3% / 0,5% | 64,3% / 50,2% |
     | 2,5 | 0,1% / 0,1% | 45,8% / 37,1% |

     Một phần tư chữ gõ có chủ ý từng bị đổi. Bảng này gợi ý 3,0 gần như miễn phí — và **nó
     sai trên máy thật**: ở 3,0, `abh` chỉ thành `anh` khi ngón đáp trong 15% sát mép `b` với `n`.
     Bộ đo rải ngón theo phân phối chuẩn quanh phím định gõ, nên mọi lần trượt đều nằm sát mép;
     lỗi thật của người gõ hay rơi sâu hẳn vào phím bên cạnh, thứ bộ đo không mô phỏng. **Bài
     học: một con số bộ đo cho là "miễn phí" phải thử bằng tay trên máy trước khi chốt.**

     4,0 chỉ tin lần chạm cách tâm phím dưới khoảng 0,15 bước phím (test
     `a_slip_deep_into_the_wrong_key_is_still_repaired`). Phần chữ cố ý còn lọt (`tl → to`,
     `hk → hi`, `haha → hây`, và phần lớn ở 4,0) là việc của **danh sách**: bàn phím coi mọi chữ
     trong `keep.txt` là chữ có thật, cùng với từ điển tiếng Anh và chữ người dùng đã hoàn tác.
   - **Phím số ở đầu từ làm mất một chữ cái.** Phép phát lại bỏ qua chữ số mở đầu từ (VNI không
     cho chữ số mở từ), nên thay `t` bằng phím kề `5` cho ra `rước` thay vì `trước`. Giờ ứng viên
     như vậy bị loại (`generate::replay`). Ví dụ đo được trước đó: `quạt → ũa`, `Quắc → ắc`.
   - **Bộ đo so chữ bỏ qua vị trí dấu thanh.** Hơn nửa cột "sửa sai" cũ là `khóa → khoá`,
     `tọa → toạ` — cùng một chữ. Giờ còn đúng 1 lần sửa sai VNI trên 71 640 từ ở nhiễu 0,174.
     "Bỏ sót" cũng được chia theo lý do: không có ứng viên / vướng Δ / chữ nguyên thắng / bị
     chặn.

   Differential trên toàn bộ âm tiết Viet74K, dấu thanh gõ ở mọi vị trí (22 426 chuỗi Telex,
   22 501 VNI), điểm chạm đúng tâm kèm đủ phím kề: **bật tính năng cho ra y hệt lúc tắt, 0 khác
   biệt**. Engine trước đợt 2 đổi 591 chuỗi trong số đó — toàn từ mượn gõ đúng (`ampe → smoe`,
   `atlas → stoá`, `robot → rổn`, `camera → cẩm`, `Latin → Lặn`).
   **Đợt 3 (16/10/2026): prior từ bảng xếp hạng bốn bậc.** `--prior shipped` giờ đọc
   `syllable_weight` thay vì học danh sách vào một kho rỗng (C1/C2), nên lần đầu bộ đo đo được đúng
   thứ một máy mới cài nhìn thấy: kho cá nhân trống + bảng bậc. Viet74K, 8 955 âm tiết, cột "không"
   là prior đều (`--prior uniform`), `INVALID_COST` 4,0, Δ 1,5, 8 seed mỗi ô:

   | Trọng số bậc 1→4 | Telex 0,174: sửa đúng / sửa sai | Telex 0,25 | VNI 0,174 | VNI 0,25 |
   |---|---|---|---|---|
   | không (prior đều) | 50,8% / 7 (0,63%) | 40,3% / 376 (3,94%) | 64,7% / 0 | 51,6% / 501 (4,33%) |
   | `[40, 12, 4, 1]` (bản thảo) | 46,8% / **25** (2,42%) | 39,0% / **554** (5,88%) | 57,9% / 1 | 49,2% / **593** (5,32%) |
   | `[20, 8, 3, 1]` | 47,7% / 16 (1,53%) | 39,7% / 461 (4,85%) | 59,6% / 1 | 50,0% / 515 (4,59%) |
   | `[12, 6, 3, 1]` | 48,6% / 11 (1,04%) | 40,2% / 431 (4,50%) | 60,4% / 0 | 50,5% / 493 (4,36%) |
   | `[6, 3, 2, 1]` | 50,1% / 7 (0,64%) | 40,9% / 386 (3,98%) | 62,1% / 0 | 51,7% / 470 (4,07%) |
   | **`[4, 3, 2, 1]`** ← đã chốt | **50,4% / 7** (0,64%) | **41,1% / 376** (3,87%) | **62,2% / 0** | **51,9% / 475** (4,10%) |
   | `[3, 2, 1, 1]` | 50,9% / 7 (0,63%) | 41,3% / 369 (3,77%) | 63,2% / 0 | 52,1% / 456 (3,92%) |
   | `[2, 1, 1, 1]` | 51,1% / 7 (0,63%) | 41,4% / 364 (3,72%) | 63,6% / 2 | 52,5% / 458 (3,91%) |

   Ba điều đọc được:

   - **Rủi ro §C2 lo trước là thật.** Bậc 1 nặng 40 (3,7 nat ≈ 2Δ) **tăng sửa sai lên một nửa**
     (376 → 554 ở Telex 0,25) và mất 4 điểm sửa đúng ở nhiễu thật: prior lật cả những ngón tay đã
     nghiêng rõ sang một từ thông dụng khác. Đường cong đơn điệu — prior càng nhẹ càng về gần
     mức prior đều — nên trọng số là knob **phải hạ**, không phải Δ.
   - **Viet74K không thưởng cho prior tần suất.** Mỗi âm tiết xuất hiện đúng một lần, từ hiếm
     ngang từ thường, nên bộ đo chỉ đo được *tác hại* của prior (lật từ hiếm sang từ thường) chứ
     không đo được *lợi ích* của nó (trong văn bản thật từ bậc 1 chiếm phần lớn số lần gõ). Con số
     ở đây vì thế là cận trên của hại và không nói gì về lợi — chọn trọng số theo tiêu chí
     **không làm xấu Viet74K ở bất kỳ ô nào**, rồi để lợi ích tự đến ngoài đời.
   - **`[4, 3, 2, 1]` là mức nặng nhất đạt tiêu chí đó**, và cũng là mức nhẹ nhất còn làm được việc:
     bậc 1 hơn từ ngoài danh sách `ln 5` = 1,61 nat, vừa qua Δ = 1,5, tức chỉ đủ phá hoà `nhad`
     chạm giữa `f`/`s` (test `the_shipped_prior_breaks_a_tie_towards_the_commoner_word`); `[3, 2, 1, 1]`
     tốt hơn một chút trên bảng nhưng `ln 4` = 1,39 < Δ, prior không còn quyết định được gì và hai
     bậc cuối trùng nhau.

   Quét lại `INVALID_COST` × Δ với trọng số đã chốt (8 seed; ô 4,0/1,5 là 20 seed), Viet74K, sửa
   đúng / sửa sai, "ship" = `--prior shipped`, "đều" = prior đều, cột cuối là `--keep` (chữ gõ có
   chủ ý bị đổi, không có cửa chặn, Telex / VNI):

   | `INVALID_COST` / Δ | Telex 0,174 ship · đều | Telex 0,25 ship · đều | VNI 0,174 ship · đều | VNI 0,25 ship · đều | `--keep` |
   |---|---|---|---|---|---|
   | 3,0 / 1,0 | 56,6% / 0,65% · 56,8% / 1,13% | 45,3% / 6,12% · 44,0% / 6,51% | 67,1% / 0,22% · 68,3% / 0,64% | 55,3% / 5,90% · 54,0% / 6,63% | 2,3% / 1,4% |
   | 3,0 / 1,5 | 49,9% / 0,55% · 49,7% / 0,56% | 35,9% / 3,74% · 32,9% / 3,76% | 60,8% / 0 · 62,7% / 0 | 44,5% / 3,84% · 41,5% / 4,15% | 0,5% / 0,2% |
   | 3,0 / 2,0 | 34,7% / 0,27% · 30,6% / 0 | 20,8% / 2,65% · 13,9% / 1,87% | 43,5% / 0 · 40,1% / 0 | 26,4% / 2,03% · 18,1% / 1,48% | 0 / 0 |
   | 3,5 / 1,0 | 56,7% / 0,73% · 57,1% / 1,20% | 47,4% / 6,15% · 46,7% / 6,55% | 67,5% / 0,22% · 68,9% / 0,63% | 58,2% / 5,96% · 57,9% / 6,63% | 6,3% / 3,6% |
   | 3,5 / 1,5 | 50,3% / 0,55% · 50,5% / 0,55% | 39,3% / 3,75% · 38,0% / 3,78% | 61,9% / 0 · 64,3% / 0 | 49,3% / 4,06% · 48,1% / 4,33% | 2,1% / 1,2% |
   | 3,5 / 2,0 | 41,3% / 0,22% · 41,1% / 0 | 28,9% / 2,53% · 25,5% / 1,93% | 53,1% / 0 · 55,5% / 0 | 36,9% / 1,91% · 33,1% / 1,64% | 0,5% / 0,2% |
   | 4,0 / 1,0 | 56,8% / 0,73% · 57,2% / 1,20% | 48,7% / 6,20% · 48,6% / 6,58% | 67,6% / 0,21% · 69,0% / 0,63% | 60,2% / 5,96% · 60,8% / 6,55% | 12,1% / 9,7% |
   | **4,0 / 1,5** ← giữ | **51,2% / 0,36%** · 51,5% / 0,36% | **41,2% / 3,80%** · 40,4% / 3,90% | **61,9% / 0,13%** · 63,8% / 0,12% | **51,8% / 3,92%** · 51,5% / 4,20% | 5,8% / 4,4% |
   | 4,0 / 2,0 | 41,5% / 0,22% · 41,6% / 0 | 31,5% / 2,56% · 29,5% / 2,01% | 54,1% / 0 · 56,9% / 0 | 40,8% / 1,95% · 38,6% / 1,62% | 1,7% / 1,1% |

   **Không đổi `INVALID_COST` lẫn Δ.** Δ 2,0 mua 1,5–2 điểm sửa sai ở nhiễu 0,25 bằng 8–11 điểm
   sửa đúng ở cả hai mức nhiễu — đúng đòn bẩy kém đã ghi ở đợt 1; Δ 1,0 thêm 6–9 điểm sửa đúng
   nhưng sửa sai ở 0,25 lên 6% và `--keep` lên 12%. Hạ `INVALID_COST` về 3,0 giữ sửa sai y nguyên
   mà mất 5–7 điểm sửa đúng ở 0,25 (và lý do máy thật ở đợt 2 vẫn còn đó); phần `--keep` nó cứu
   được là việc `is_known_word` đã làm — với bất kỳ kho từ nào, cột đó bằng 0. Đáng chú ý: ở Δ 2,0
   prior lại *tăng nhẹ* sửa sai so với prior đều (1,95% so với 1,62%) vì nó biến ca "vướng Δ" thành
   ca áp dụng; ở Δ 1,5 nó trung tính hoặc tốt hơn ở mọi ô — thêm một lý do để Δ đứng yên.

   **Bộ lọc `--known-only` vẫn tắt.** Với 2 100 âm tiết, `--prior shipped --known-only` vá 16–21%
   số từ trượt (so với 41–62% không lọc) — độ phủ còn xa 8 955 âm tiết của Viet74K, dù sửa sai
   hạ xuống 3,3–3,5% ở 0,25. Ngưỡng bật vẫn phải đo bằng đúng lệnh này khi danh sách lớn hơn.

   **Số liệu 20 seed ở cấu hình chốt** (4,0 / 1,5 / `[4, 3, 2, 1]`): Telex 0,174 vá 51,2%, sai 10 lần
   trên 2 741 lần sửa (0,36%); Telex 0,25 vá 41,2%, sai 3,80%; VNI 0,174 vá 61,9%, sai 4 lần (0,13%);
   VNI 0,25 vá 51,8%, sai 3,92%; mọi ô ngang hoặc tốt hơn prior đều về sửa sai, VNI 0,174 mất
   1,9 điểm sửa đúng vì vài ca từ hiếm giờ rơi vào "vướng Δ" thay vì được áp dụng. Viet74K có
   mục sai chính tả; số sửa sai gồm cả những ca engine đúng mà kho từ sai, không truy về 0.

   **Đợt 4 (16/10/2026, nền): lỗi gõ nhanh, đo trước khi có engine sửa theo từ.** Tới đây bộ đo
   chỉ mô phỏng một loại lỗi — ngón trượt sang *phím kề* — trong khi lỗi thật khi gõ nhanh là
   **đúp phím** (`chowii`), **thiếu phím** (`chwi`), **đảo hai phím** (`chwoi`) và **dư phím dấu**
   (`chowis`). `funput dev typos --faults <tỉ lệ>` (D1) tiêm **tối đa một** lỗi như vậy vào chuỗi
   phím chuẩn của một phần từ, *trước* khi ngón trượt; `--fault <loại>` ép mọi lỗi về một loại,
   `--seeds n` chạy n seed liên tiếp và cộng dồn. Bảng `By fault kind` tách mọi kết quả theo
   chuyện gì đã xảy ra với từ: hàng "trượt chạm" là từ chỉ trượt phím kề, bốn hàng còn lại là bốn
   loại lỗi (từ vừa có lỗi vừa trượt tính vào hàng lỗi). Phím dấu dư lấy từ `key_role` của
   `funput-core` (C0), không viết tay. Lỗi rút từ một luồng ngẫu nhiên riêng, nên `--seed` cũ vẫn
   gõ đúng những điểm chạm của Đợt 1–3 (test `old_seeds_reproduce_the_touch_stream` ghim số seed 1);
   `--keep --faults 0`, 20 seed, ra lại đúng **5,8% / 4,4%** (142 / 2 469 Telex, 109 / 2 467 VNI).

   Số nền **trước mọi thay đổi engine** — E2 phải chạy lại đúng các lệnh này. Viet74K, `--prior
   shipped --faults 0.3 --seeds 8` (30% số từ nhận một lỗi, loại chọn đều; 8 955 × 8 lượt gõ). Mỗi
   ô theo loại: số từ · sửa đúng · sửa sai, tính trên số từ của hàng đó:

   | Kiểu gõ · nhiễu | Từ có lỗi | Sửa đúng | Sửa sai (trên số lần sửa) | Trượt chạm | Đúp phím | Thiếu phím | Đảo phím | Dư phím dấu |
   |---|---|---|---|---|---|---|---|---|
   | Telex 0,174 | 22 994 | 4,0% | 1 299 (**58,8%**) | 1 546 · 50,9% · 0,1% | 5 331 · 0,8% · 6,7% | 5 383 · 0,0% · 6,1% | 5 398 · 0,7% · 7,2% | 5 336 · 0,8% · 4,2% |
   | Telex 0,25 | 37 375 | 18,9% | 2 398 (25,4%) | 15 927 · 41,1% · 1,8% | 5 331 · 1,6% · 8,5% | 5 383 · 0,2% · 13,4% | 5 398 · 4,7% · 8,8% | 5 336 · 2,9% · 8,9% |
   | VNI 0,174 | 22 893 | 4,3% | 997 (**50,4%**) | 1 443 · 59,6% · 0,2% | 5 331 · 0,6% · 4,6% | 5 381 · 0,2% · 5,5% | 5 402 · 0,8% · 4,0% | 5 336 · 0,7% · 4,5% |
   | VNI 0,25 | 36 386 | 22,6% | 1 957 (19,2%) | 14 936 · 51,9% · 2,2% | 5 331 · 0,5% · 5,5% | 5 381 · 0,5% · 11,1% | 5 402 · 5,1% · 5,1% | 5 336 · 2,7% · 8,7% |

   Từng loại tách riêng (`--fault <loại> --faults 0.3 --seeds 8`, ~21 600 từ có lỗi mỗi ô): sửa đúng /
   sửa sai / để nguyên:

   | Loại lỗi | Telex 0,174 | Telex 0,25 | VNI 0,174 | VNI 0,25 |
   |---|---|---|---|---|
   | Đúp phím | 207 (1,0%) / 1 507 (7,0%) / 19 901 | 371 (1,7%) / 1 881 (8,7%) / 19 363 | 130 (0,6%) / 994 (4,6%) / 20 491 | 225 (1,0%) / 1 172 (5,4%) / 20 218 |
   | Thiếu phím | 14 (0,1%) / 1 334 (6,2%) / 20 229 | 62 (0,3%) / 2 719 (12,6%) / 18 796 | 47 (0,2%) / 1 263 (5,9%) / 20 267 | 91 (0,4%) / 2 729 (12,6%) / 18 757 |
   | Đảo phím | 125 (0,6%) / 1 556 (7,2%) / 19 893 | 1 075 (5,0%) / 1 954 (9,1%) / 18 545 | 136 (0,6%) / 792 (3,7%) / 20 649 | 1 016 (4,7%) / 993 (4,6%) / 19 568 |
   | Dư phím dấu | 195 (0,9%) / 1 034 (4,8%) / 20 208 | 668 (3,1%) / 2 038 (9,5%) / 18 731 | 168 (0,8%) / 888 (4,1%) / 20 381 | 545 (2,5%) / 1 829 (8,5%) / 19 063 |

   Ba điều đọc được:

   - **Bốn loại lỗi gần như toàn "để nguyên"** (87–95%), đúng như dự đoán: engine v2 chỉ biết thay
     phím kề, nên `chowii` không có đường nào về `chơi`. Phần sửa đúng 0–5% là những ca hai phép
     thay phím kề tình cờ bù được lỗi — đảo hai phím cạnh nhau trên lưới (`io`, `er`) chính là hai
     phép thay như thế, nên hàng "đảo" nhỉnh hơn ở 0,25 khi mỗi phím có nhiều phím kề hơn.
   - **Nhưng không vô hại.** 4–13% từ có lỗi bị **sửa sai**: v2 thấy từ chết, thay một phím kề và
     rơi vào một từ khác (`cây` → `acay` → từ khác). Ở nhiễu thật 0,174, lỗi gõ nhanh (21 400 từ)
     nhiều hơn hẳn trượt chạm (1 500), nên trong lượt chạy trộn **hơn nửa số lần sửa là sửa sai**
     (58,8% Telex, 50,4% VNI) dù hàng trượt chạm vẫn ở 0,1–0,2%. Thiếu phím tệ nhất (12,6% ở 0,25):
     bỏ một phím thường để lại một chuỗi cách từ khác đúng một phím. Cổng E2 vì thế không chỉ là
     "sửa đúng ≥ 60 / 60 / 40 / 25%" mà còn phải **kéo cột sửa sai của bốn hàng này xuống** — đưa
     ứng viên lên thanh và để Space chỉ tự áp dụng khi hơn ứng viên kế ≥ Δ là chỗ để làm việc đó.
   - **Hàng trượt chạm khớp Đợt 3** (50,9 / 41,1 / 59,6 / 51,9% so với 51,2 / 41,2 / 61,9 / 51,8%
     ở 20 seed): bộ đo mới không đổi gì ở phần đã đo, chỉ thêm phần chưa đo.
2. **Bất biến**: mọi âm tiết hợp lệ gõ đúng phím không bao giờ bị đổi (property test toàn tập).
   ✅ `tests/correction_property.rs`, cùng với: không ứng viên nào trùng chữ đang hiển thị, ứng
   viên luôn xếp giảm dần, sửa rồi Xoá trả lại đúng tài liệu cũ, và **từ chối thì ra đúng kết
   quả của engine khi tắt tính năng**.
3. **Ca đã đo trên iOS** (§3.4) thành test bảng, gồm cả ca `nhad` hai ứng viên: lệch trái ra
   `nhá`, lệch phải ra `nhà`, chạm giữa không sửa.
4. **Hoàn tác**: sửa → Xoá → đúng chuỗi gốc; gõ lại từ đó trong cùng phiên không bị sửa lần hai.
5. **Hiệu năng**: p99 < 2ms cho một từ 10 phím trên thiết bị cũ nhất hỗ trợ.
   ✅ `benches/correction.rs`. Đo trên máy dev (Apple Silicon, release): từ 9 phím **356 µs**,
   từ 10 phím **496 µs** ở trường hợp nặng nhất — mọi phím đều có đủ 3 phím kề. Gõ thường có
   kèm điểm chạm: **0,14 µs/phím**, không đo được khác lúc tắt.

   Con số 496 µs là trên máy dev; một iPhone đời cũ chậm hơn cỡ 3 lần, tức ~1,5 ms — vẫn dưới
   ngưỡng nhưng không dư dả. **Cách rẻ nhất để lấy lại biên**: nền tảng chỉ gửi phím kề khi
   khoảng cách thật sự đáng ngờ (ví dụ < 1,2 pitch). Phím nào gõ giữa tâm thì không có phím kề
   nào, và mỗi vị trí bị loại như vậy cắt cả một tầng của vòng lặp ghép đôi.
6. **UI test iOS**: ✅ `FunputUITests/Correction/TypoCorrectionUITests.swift`, chạy qua đúng
   bàn phím thật với VNI `d9u7o7ng2` và ngón đáp lệch sang `h`. Harness dùng chung ở
   `TypoCorrectionHarness.swift`; `TypoCorrectionBarUITests.swift` thêm bốn ca trên thanh gợi ý:
   thanh hiện `“đườnh”` + `đường` trước Space, bấm ứng viên → `đường ` và chip hoàn tác, bấm
   chữ gốc rồi Space → `đườnh ` và thanh trống, bấm chip → `đườnh ` (B2).

   Hai điều bản viết chưa lường:

   - **Không ô nào trong harness cho phép autocorrect** — cả hai đều đặt `.no` để test cũ khỏi
     bị hệ thống chen vào. Thêm cờ `-uitest-autocorrect` để một lượt chạy xin được autocorrect;
     mọi tính năng thông minh khác vẫn tắt.
   - **Dạng chưa sửa không phải chuỗi phím thô** mà là `đườnh`, vì harness ghim
     `eagerRestore = false` cho xác định, và ranh giới từ cũng không khôi phục (VNI đánh vần
     bằng chữ số nên `keystrokes_intend_vietnamese` đọc ra là cố ý). Hoàn tác vì thế trả về
     `đườnh `, tức đúng thứ đang hiển thị trước khi sửa.
7. **Differential**: ✅ `tests/correction_off.rs` chạy lại toàn bộ fixture với `typo_correction`
   **bật** ở cả ba dáng một nền tảng có thể mang: không gửi điểm chạm (phải ra đúng từng bước
   instruction cũ), gửi điểm chạm rồi từ chối mọi đề nghị, và gửi rồi không trả lời (cả hai phải
   ra đúng văn bản cũ).
8. **Ngân sách cấp phát**: ✅ `tests/alloc_budget_correction.rs` — host không gửi điểm chạm thì
   số lần cấp phát *bằng đúng* lúc tắt; gửi điểm chạm thì tốn thêm nhiều nhất một lần kiểm âm
   tiết cho mỗi từ; một từ được sửa nằm trong trần 5000. `tests/alloc_budget.rs` cũ không đụng tới.

## 13. Chỉ số

`correctionsApplied`, `correctionsReverted`, `correctionsSkippedAmbiguous`, `keptAsTyped`,
`offered`, `noCandidate`, `suppressed`, `validNearEdge` (lõi); `recognized`, `fieldRefused`
(nền tảng); `correctionCandidatesMax`, `correctionMicrosecondsMax`. Chỉ đếm, không lưu nội dung.
Lõi đếm trong `CorrectionMetrics`, iOS in ra log category `typo`. Tỉ lệ hoàn tác cao nghĩa là
`Δ` đặt thấp quá; `keptAsTyped` cao nghĩa là `INVALID_COST` đặt thấp quá.

`suppressed` đếm từ bị bỏ qua vì người dùng đã hoàn tác đúng cách gõ đó ngay trước (§3.3): một
lời từ chối được tôn trọng, không phải từ không có ứng viên — nên nó tách khỏi `noCandidate` để
`noCandidate` vẫn đo đúng độ phủ của phép tìm.

## 14. Thứ tự hiện thực

| PR | Nội dung | Cổng |
|---|---|---|
| 1 ✅ | `funput-engine::correction` + `KeyTouch` + phát lại + lọc + hoàn tác, `typo_correction` mặc định tắt | Differential, property test, ngân sách cấp phát, `check-loc.sh` |
| 2 ✅ | `SuggestionEngine::frequency` + `is_known_word` (cửa chặn tiếng Anh) | Test đơn vị + ngân sách 0 cấp phát |
| 2b ✅ | C ABI + JNI (đường edit riêng trả `IntArray` cho Android) + header cbindgen | Test round-trip FFI, `gen-header.sh --check` |
| 2c ✅ | `funput dev typos` — bộ đo tỉ lệ sửa đúng / sửa sai | Sửa sai < 1%, sửa đúng ≥ 50% ở nhiễu ngón 0,25 |
| 3 ✅ | iOS: phím kề từ hình học, mang điểm chạm qua pipeline, hai bước ở ranh giới từ, ghi tài liệu | Test đơn vị + UI test §12.6 |
| 4 ✅ | iOS: hoàn tác bằng phím Xoá, công tắc Cài đặt, tôn trọng `autocorrectionType` (chip trên thanh gợi ý: chưa) | UI test hoàn tác |
| 4b ✅ | An toàn đợt 2: chữ nguyên tham gia so sánh, loại chữ số mở từ, bộ đo `--keep` | `--keep` < 1%, differential bật = tắt khi gõ đúng |
| 4c ✅ | Danh sách giữ nguyên trong `is_known_word`, iOS: hoàn tác thì nhớ, đếm lý do bỏ sót, nhật ký từng chữ (Debug) | Test đơn vị + thử trên máy |
| A1 ✅ | Engine: khôi phục ngay tại ranh giới, bỏ ứng viên cũ sau khi trả lời, `suppressed` tách khỏi `noCandidate` (#573) | Protocol test, differential, property |
| A2 ✅ | Engine: `preview_correction` / `rank_correction` / `refuse_correction` trước khi từ kết thúc (#577) | Test preview + ngân sách cấp phát |
| C1 ✅ | `vi.txt` bốn bậc, `syllable_tier` / `syllable_weight`, C ABI + JNI không cần handle (#574) | 0 cấp phát, lexicon-tool test, `gen-header.sh --check` |
| C2 ✅ | `funput dev typos --prior shipped` dùng bảng bậc, đo lại `INVALID_COST` / Δ / trọng số bậc | Bảng số §12 đợt 3 |
| B1 ✅ | iOS: thanh gợi ý hiện chữ gốc + ứng viên, bấm để giữ / sửa / hoàn tác, chính sách ô nhập, điểm chạm lúc đặt ngón (#575) | Test đơn vị + `check-swift-loc.sh` |
| B2 ✅ | iOS: `FunputComposer` conform `CorrectionPreviewing` (preview / rank / refuse thật), `syllableWeight` thật, `suppressed`, UI test bốn ca trên thanh gợi ý | `FunputCorrectionPreviewTests` + `TypoCorrectionBarUITests` §12.6 |
| C0 ✅ | `funput_core::key_role`: vai trò phím theo kiểu gõ, không nhìn buffer, chỉ đọc (#583) | Test inline, API frozen giữ nguyên |
| D1 ✅ | `funput dev typos --faults / --fault / --seeds`: tiêm lỗi gõ nhanh, bảng theo loại lỗi, số nền Đợt 4 | `--faults 0` tái lập seed cũ, mỗi loại lỗi đúng một phép, `--seeds` = tổng các seed |
| 5 | Android: cùng lõi, batch edit | Instrumented test |
| 6 | Chỉnh `σ`, `λ`, `Δ` theo kho lỗi và phản hồi TestFlight | Số liệu §13 |

## 15. Câu hỏi còn mở

1. **Mặc định bật hay tắt** ở bản phát hành đầu? Đề xuất: bật cho người cài mới, tắt cho người
   đang dùng, kèm thông báo một lần.
2. ~~**Thanh gợi ý khi không chắc**~~ — đã chốt (B1): **dùng chung khe** của thanh gợi ý. Khe sửa
   lỗi đứng trước, gợi ý cá nhân lấp phần còn lại trong ba khe; không thêm hàng.
3. ~~**Học từ đã sửa**~~ — đã chốt: **có**, và không cần điều kiện riêng. Phép sửa đi cùng
   transaction commit của từ, nên tracker học đúng từ đã sửa như mọi từ người dùng gõ xong; hoàn
   tác trong từ kế tiếp là một commit khác và tracker học lại chữ gốc.
4. **Tên hiển thị** của cài đặt: "Tự sửa lỗi gõ" hay "Sửa lỗi phím bấm"?
