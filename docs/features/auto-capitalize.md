# Tự viết hoa

## Trạng thái

**Cả năm nền tảng đã chạy trên core.** Luật nhận diện câu sống một chỗ duy nhất
trong `funput_core::sentence`: Android gọi qua `funput-jni`, iOS qua `funput-ffi`,
còn macOS, Windows và Linux nhận nó qua `funput-engine`, vốn đã link core sẵn.

Tài liệu này là nơi luật sống. Mọi thay đổi cập nhật lại nó trong cùng PR.

## Luật nhận diện câu

Một câu bắt đầu ở đầu văn bản, sau ký tự xuống dòng, và sau một **dấu kết câu có
khoảng trắng theo sau**.

| Thành phần | Giá trị |
| --- | --- |
| Dấu kết câu | `.` `?` `!` `…` |
| Dấu đóng trong suốt | `"` `'` `)` `]` `}` `»` `”` `’` `›` |

Bốn hệ quả rơi ra từ hình dạng đó chứ không phải luật viết riêng:

- **`1.5` giữ nguyên** vì sau dấu chấm là chữ số chứ không phải khoảng trắng.
- **`...` và `?!` kết thúc một câu**, không phải hai hay ba.
- **`Xin chào.` với con trỏ ngay sau dấu chấm chưa hết câu** — phải có khoảng trắng.
- **`nói "Xin chào." rồi` hết câu ở `rồi`**, vì dấu đóng là trong suốt.

Khi tìm chữ cái đầu câu, **dấu mở đầu được bỏ qua** nên `"xin chào"` và `(xin chào)`
đều trỏ vào `x`. **Chữ số thì không**: một câu mở đầu bằng số là đã bắt đầu rồi, bỏ
qua nó sẽ biến `3 con mèo` thành `3 Con mèo`.

### Hai cách đọc dấu chấm

Dấu chấm của từ viết tắt không phân biệt được với dấu chấm hết câu. Hai bên gọi luật
này đọc nó khác nhau, có chủ đích:

| | Chống viết tắt | Vì sao |
| --- | --- | --- |
| `Rules::TYPING` — bàn phím | Có | Chữ hoa đã nằm trong văn bản trước khi người dùng kịp nhìn |
| `Rules::TRANSFORM` — Chuyển mã, `funput case` | Không | Người dùng thấy preview trước khi áp dụng |

Luật chống viết tắt: khi gặp `.`, nếu ngay trước nó là một dãy chữ cái mà ký tự đứng
trước dãy đó lại là `.` thì đây là viết tắt. Bắt được `v.v. `, `U.S. `, `a.m. `; **không**
bắt `TS. ` hay `TP. ` vì chúng không có dấu chấm nào phía trước để lộ ra.

Phía không bật guard giữ nguyên quyết định đã ghi trong
[text-case.md](text-case.md#không-thuộc-phạm-vi): một danh sách viết tắt tiếng Việt
không bao giờ đủ và sai sót của nó khó đoán hơn một quy tắc đơn giản.

## Ranh giới core và nền tảng

Tính năng gồm hai việc, và chỉ việc đầu là dùng chung được.

**Quyết định** — "vị trí này có phải đầu câu không" — là phân tích văn bản thuần, nên
ở `funput_core::sentence`. Module này **không nằm sau cargo feature nào**, khác với
`textcase`: các bàn phím link core mà không bật `textcase`, và giữ một bản duy nhất
của câu trả lời chính là lý do nó ở đây.

**Áp dụng** thì mỗi nền tảng một kiểu. Desktop sửa thẳng ký tự bên trong engine.
Bàn phím mềm phải nâng trạng thái phím Shift hiển thị, nếu không bàn phím vẽ chữ
thường trong khi gõ ra chữ hoa.

```
funput_core::sentence  ── Scanner ──┬── textcase::sentence  (Chuyển mã, funput case)
                                    ├── starts_sentence()   ├── funput-jni  → Android
                                    │   starts_word()       └── funput-ffi  → iOS
                                    └── funput-engine       → macOS, Windows, Linux
```

### Hai cách vào cùng một máy quét

`Scanner` quét tăng dần, nên phục vụ được cả hai kiểu người gọi.

**Bên đọc được văn bản** — bàn phím mềm và batch transform — đưa cả đoạn vào rồi đọc
kết quả. `starts_sentence` dựng một `Scanner::new`, chạy hết đoạn trước con trỏ, trả
về cờ cuối. Tính lại mỗi lần chứ không tích luỹ, vì con trỏ còn nhảy vì dán, chọn gợi
ý, chạm chỗ khác, hoặc mở một ô đã có sẵn nội dung.

**Bên chỉ thấy phím gõ** — desktop shell — giữ một `Scanner` sống suốt phiên trong
`Session` và đẩy từng phím vào. Nó dùng `Scanner::mid_text` chứ không phải
`Scanner::new`: engine không biết con trỏ đang ở đâu, và coi mặc định là đầu tài liệu
sẽ viết hoa từ đầu tiên người dùng gõ sau khi đổi app, giữa một đoạn văn. Chỉ khi shell
biết chắc — nó vừa nhận sự kiện focus — nó mới nói ra bằng `Engine::arm_capitalization`,
và hàm đó thay scanner bằng `Scanner::new`.

Chiều ngược lại cũng cần nói ra. Khi con trỏ nhảy tới chỗ shell không thấy — click chuột,
đổi app, phím mũi tên — trạng thái câu dựng từ những phím gõ trước đó không còn đúng nữa:
`Xong. `, click vào giữa một câu khác, `tiếp` sẽ thành `Tiếp`. Shell báo điều đó bằng
`Engine::disarm_capitalization`, hàm này đưa scanner về `Scanner::mid_text`.

Đẩy mọi phím vào scanner cũng là cách `…` được tính là hết câu. `is_english_boundary`
chỉ nhận `is_whitespace() || is_ascii_punctuation()` nên U+2026 không phải ranh giới
từ; mở rộng predicate đó là cách duy nhất để một bản vá tại chỗ với tới `…`, nhưng nó
đồng thời gác cửa gõ tắt và English restore.

`on_english_boundary` vẫn cố ý bỏ qua auto-capitalize, nên chế độ tiếng Anh không có.

## Chọn chế độ

Hai nền tảng đọc **cùng bốn nhánh theo cùng thứ tự**, chỉ khác tên tín hiệu:
`EditorInfoPolicy.autoCapitalizationMode(preferenceEnabled)` trên Android,
`KeyboardInputContextResolver.autocapitalization` trên iOS.

1. Ô không bao giờ chứa văn xuôi — mật khẩu, email, URL, số, điện thoại — thì không
   viết hoa gì, bất kể ai yêu cầu. Đây là `allowsAutoCapitalization`, có trên cả
   `EditorInfoPolicy` lẫn `KeyboardEditorMode`.
2. Ô đòi viết hoa toàn bộ (`CAP_MODE_CHARACTERS` / `.allCharacters`) thắng cả khi
   người dùng tắt công tắc: đó là một phát biểu về nội dung ô, không phải một tiện
   ích được mời.
3. Công tắc "Tự viết hoa" tắt thì im hai chế độ tiện ích còn lại.
4. Còn lại là câu, **kể cả khi ô không yêu cầu gì hoặc yêu cầu không viết hoa**.

### Vì sao nhánh 4 lấn quyền ô nhập liệu

Đây là chỗ duy nhất Funput đi xa hơn bàn phím hệ thống, và lý do khác nhau ở hai bên:

- **Android không có mặc định.** App không đặt cờ `CAP_*` nghĩa là chưa nghĩ tới, và
  phần lớn app không đặt — luật cũ "chỉ viết hoa khi app yêu cầu" khiến tính năng tắt
  ở gần như mọi chỗ.
- **iOS mặc định `.sentences`**, nên `.none` là từ chối có chủ đích. Nhưng nó xuất
  hiện đầy trong những ô không hề có ý đó: input web mang `autocapitalize="off"`, ô
  soạn tin chép từ một ô tìm kiếm. Người dùng bật công tắc là đang xin viết hoa đúng
  ở những chỗ đó.

Nhánh 1 là thứ giữ cho quyết định này không lan tới ô mà nó sai.

## Android

### Đồng bộ lại sau khi đổi bảng phím

Đổi bảng ABC ↔ `?123` ↔ emoji sẽ dựng lại layout, và `KeyboardSurfaceView.updateKeyboardLayout()`
gọi `interaction.reset()` xoá Shift. Vì `!` và `?` chỉ có trên bảng ký hiệu, chuyến đi
về ABC luôn xoá mất chữ hoa mà dấu cách vừa dựng lên — đó là lý do hai dấu này trước
đây không bao giờ viết hoa. `ImeKeyboardCallbackBinder` nối `onPanelChanged` vào
`updateCapitalization()` để dựng lại.

## iOS

`KeyboardCapitalizationResolver` chỉ còn phân nhánh theo chế độ: `.none` và
`.allCharacters` là phát biểu về ô nhập liệu nên trả lời ngay, hai chế độ còn lại hỏi
`FunputSentence`, wrapper Swift quanh `funput_starts_sentence` / `funput_starts_word`.

Wrapper nằm trong target `FunputEngine` chứ không trong `KeyboardInput`, theo ranh giới
module mà `platforms/ios/docs/ARCHITECTURE.md` đặt ra: chỉ `FunputEngine` được chạm vào
lớp C. Nó cắt context còn 256 ký tự cuối trước khi vượt biên, bằng cửa sổ Android dùng.

**Xcode build lại Rust ở mọi configuration.** Pre-action của scheme `Funput` từng bọc
`build-ffi.sh` trong `if [ "$CONFIGURATION" = "Release" ]`, nên một lần Run ở Debug link
đúng cái xcframework đang nằm trên đĩa và thay đổi Rust vắng mặt trong im lặng cho tới
khi ai đó archive. Điều kiện đó đã bỏ; cargo incremental nên một crate không đổi chỉ tốn
vài giây.

**Engine không còn nhận cờ.** Trước đây `KeyboardInputCoordinator+Configuration.swift`
truyền `configuration.autoCapitalize` xuống `funput_configure` dù kết quả của bộ đếm câu
trong engine không bao giờ thắng Shift. `FunputCompositionOptions` nay không có trường
đó nữa và `FunputComposer.configure` ghim `auto_capitalize: false`, nên không ai bật lại
được do nhầm. `KeyboardCapitalizationOwnershipTests` vẫn ghim kết quả: chữ đi theo Shift
kể cả khi người dùng hạ Shift ngay chỗ một bộ đếm câu sẽ viết hoa.

## Desktop

macOS, Windows và Linux không viết một dòng nào cho tính năng này: cả ba link
`funput-engine`, và engine giữ một `Scanner` trong `Session` thay cho hai cờ
`cap_armed` / `cap_sentence_ended` cũ. `update_caps_on_boundary` đã bị xoá.

Việc này vá luôn ba điểm engine từng lệch khỏi luật chung: thiếu `…`, không có luật
chống viết tắt, và danh sách dấu đóng chỉ có ASCII nên `»` `”` `’` `›` không trong
suốt.

`prepare_key` đọc `awaiting_sentence()` rồi gọi `consume_sentence_start()` **chỉ khi
thực sự lấy được arm**. Tiêu thụ vô điều kiện sẽ xoá mất một dấu kết câu còn đang chờ
khoảng trắng — đúng trường hợp của `»`, vốn không phải ASCII nên không đi qua đường
ranh giới từ mà rơi vào `prepare_key`.

**Chữ hoa phải được inject, không được pass-through.** `prepare_key` viết hoa phím
trước khi vào `pipeline::process`, nên phép kiểm "chỉ nối thêm đúng phím vừa gõ" phải
so với phím **người dùng gõ** (`typed`), không phải phím đã viết hoa. Trước đây nó so
với `'V'`, thấy khớp, trả `Action::None` — và hook Windows, vốn thả phím vật lý qua khi
nhận `None`, để lọt chữ `v` thường vào app trong khi buffer của engine là `V`. Chữ đầu
câu chỉ hiện hoa khi phím sau sửa lại nó (`dd` → `Đ`, `as` → `Á`), nên tính năng trông
như lúc được lúc không. `autocap_injects_the_capital_instead_of_passing_the_key_through`
ghim lại `Send(0, "V")`.

### Windows: con trỏ nhảy thì không đoán

Hook Windows không đọc được tài liệu, nên mỗi lần con trỏ nhảy mà không qua phím gõ, nó
báo cho engine qua `ShellState::caret_moved(Caret)` trong `funput-desktop`. Hàm này vừa
commit composition vừa đặt lại trạng thái câu, nên không hook nào làm được một việc mà
quên việc kia.

| Sự kiện | `Caret` | Chữ kế tiếp |
| --- | --- | --- |
| Enter, kể cả khi giữ Shift/Alt/Ctrl | `LineStart` | Viết hoa |
| Click chuột, đổi app, mọi phím điều hướng khác (kể cả Esc, Delete, F-key), mọi phím tắt Ctrl/Alt/Win | `Unknown` | Giữ nguyên, cho tới khi gõ hết câu mới |

`classify` quyết định `Caret` cho phím, nên luật "phím nào nói gì về con trỏ" nằm ở phần
dùng chung và có unit test, không nằm trong hook.

**Đánh đổi có chủ đích.** Trước đây đổi app thì luôn viết hoa, còn click chuột thì giữ
trạng thái câu cũ, nên cả hai đều viết hoa sai ở giữa câu. Giờ cả hai đều không viết hoa,
kể cả khi con trỏ thật ra đang ở ô trống hay đầu dòng. Viết hoa thiếu tốn một lần Shift,
viết hoa sai thì phải xoá.

Vì vậy cả những phím không dời con trỏ cũng xoá trạng thái câu: `Xong. `, Ctrl+B,
`quan trọng` không còn viết hoa. Hook không phân biệt được Ctrl+B với Ctrl+V hay Ctrl+Z,
hai phím thay đổi chính chữ đứng trước con trỏ, nên nó không đoán ở đây.

Muốn không phải đánh đổi thì phải đọc được chữ trước con trỏ, bằng UI Automation hoặc
chuyển sang bộ gõ TSF. Nếu làm, câu trả lời mới sẽ đi vào `caret_moved`, các hook không
phải sửa.

**Vẫn tắt mặc định trên desktop** (`auto_capitalize: false` trong
`funput-config/src/settings/model/defaults.rs` và trong UserDefaults của macOS), khác
mobile bật sẵn. Đợt hợp nhất này làm đúng luật cho ai đã bật, không đổi mặc định.
