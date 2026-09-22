# Tự viết hoa

## Trạng thái

**Android đã chạy trên core.** Luật nhận diện câu sống một chỗ duy nhất trong
`funput_core::sentence` và Android gọi tới nó qua `funput-jni`. iOS và ba nền tảng
desktop vẫn dùng bản riêng của mình; [phần cuối](#chưa-hợp-nhất) liệt kê việc còn lại.

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
funput_core::sentence      starts_sentence() / starts_word()
        │                            │
        ├── textcase::sentence       ├── funput-jni  → Android
        │   (Chuyển mã, funput case) └── (funput-ffi → iOS: chưa nối)
```

### Vì sao không dùng `auto_capitalize` của engine

`funput-engine` có sẵn một đường auto-capitalize **có trạng thái**
(`cap_armed`, `cap_sentence_ended` trong `Session`), và Android để nó tắt. Ba lý do:

- Không có API để truy vấn. Bàn phím cần *hỏi* ở mỗi lần con trỏ đổi chỗ để biết có
  sáng phím Shift hay không.
- Trạng thái đó chỉ theo dõi phím đi qua engine. Con trỏ trên Android nhảy vì dán,
  chọn gợi ý, chạm chỗ khác, hoặc mở một ô đã có sẵn nội dung — engine không thấy gì
  trong số đó, nên trạng thái sẽ lệch. Android tính lại từ văn bản thật mỗi lần.
- `on_english_boundary` cố ý bỏ qua auto-capitalize, nên chế độ tiếng Anh không có.

## Android

### Chọn chế độ

`EditorInfoPolicy.autoCapitalizationMode(preferenceEnabled)` đọc bốn nhánh theo thứ tự:

1. Ô không bao giờ chứa văn xuôi — mật khẩu, email, URI, số, điện thoại — thì không
   viết hoa gì, bất kể ai yêu cầu. Đây là `EditorInfoPolicy.allowsAutoCapitalization`.
2. Ô đòi `CAP_MODE_CHARACTERS` thắng cả khi người dùng tắt công tắc: đó là một phát
   biểu về nội dung ô, không phải một tiện ích được mời.
3. Công tắc "Tự viết hoa" tắt thì im hai chế độ tiện ích còn lại.
4. Còn lại là `SENTENCES`, **kể cả khi ô không đặt cờ `CAP_*` nào**.

Nhánh 4 là thay đổi cốt lõi. Android không có mặc định cho cờ này và phần lớn app
không đặt, nên luật cũ "chỉ viết hoa khi app yêu cầu" khiến tính năng tắt ở gần như
mọi chỗ. iOS nhận `.sentences` mặc định từ UIKit, và đó là hành vi đang được san bằng.

### Đồng bộ lại sau khi đổi bảng phím

Đổi bảng ABC ↔ `?123` ↔ emoji sẽ dựng lại layout, và `KeyboardSurfaceView.updateKeyboardLayout()`
gọi `interaction.reset()` xoá Shift. Vì `!` và `?` chỉ có trên bảng ký hiệu, chuyến đi
về ABC luôn xoá mất chữ hoa mà dấu cách vừa dựng lên — đó là lý do hai dấu này trước
đây không bao giờ viết hoa. `ImeKeyboardCallbackBinder` nối `onPanelChanged` vào
`updateCapitalization()` để dựng lại.

## Chưa hợp nhất

- **iOS** vẫn dùng `KeyboardCapitalizationResolver` viết bằng Swift. Nó thiếu luật dấu
  đóng trong suốt và luật chống viết tắt. Cần chuyển sang gọi core qua `funput-ffi`.
- **iOS chạy hai đường song song.** `KeyboardInputCoordinator+Configuration.swift`
  truyền `configuration.autoCapitalize` (mặc định bật) xuống engine, đồng thời
  resolver Swift cũng nâng Shift. Chưa lộ lỗi vì viết hoa hai lần là idempotent, nhưng
  phải chốt bên nào làm chủ.
- **Desktop** (macOS, Windows, Linux) vẫn dùng `update_caps_on_boundary` trong
  `funput-engine`. Ba điểm nó lệch khỏi luật trên:
  - Thiếu `…`. Nặng hơn: `is_english_boundary` chỉ nhận `is_whitespace() ||
    is_ascii_punctuation()`, mà `…` ở U+2026 không phải ASCII, nên `on_word_boundary`
    không bao giờ chạy cho dấu chấm lửng.
  - Không có luật chống viết tắt, dù nó là đường gõ trực tiếp và đáng lẽ phải có.
  - Danh sách dấu đóng hardcode ASCII, thiếu `»` `”` `’` `›`.
