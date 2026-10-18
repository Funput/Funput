# Gõ thẳng không gạch chân trên macOS

| Thuộc tính | Nội dung |
| :--- | :--- |
| Trạng thái | **Đã hiện thực** trên nhánh `feat/macos-direct-composition`, chờ thử trên máy thật, chưa merge |
| Cập nhật | 18/10/2026 |
| Phạm vi | Shell macOS (`platforms/macos`), không đụng engine Rust |
| Quyết định đã chốt | Hai chế độ song song; công tắc trong Cài đặt **mặc định bật**; không có danh sách app; xoá cờ khi tính năng ổn định |

## 1. Vấn đề

Funput trên macOS ghép chữ bằng **marked text** (preedit): chữ đang gõ hiện qua
`setMarkedText`, chỉ `insertText` khi gặp ranh giới từ. Mọi app đều gạch chân marked
text, kể cả Spotlight. Người dùng quen Windows (Funput chèn thẳng, không gạch chân)
thấy gõ trên Mac nặng nề hơn.

Mục tiêu: chữ đang gõ trông như chữ đã gõ ở càng nhiều app càng tốt, không hy sinh độ
an toàn (không bao giờ ghi đè nhầm chữ của người dùng).

## 2. Những gì đã thử và vì sao không được

### Hướng A: giữ preedit, đổi thuộc tính hiển thị (thất bại)

Ý tưởng: vẫn `setMarkedText`, nhưng gửi thuộc tính khiến app không vẽ gạch. Đã thử
lần lượt:

| Biến thể | Kết quả trên máy thật |
| :--- | :--- |
| Gạch chân màu `.clear` | Đa số app vẫn gạch. Spotlight gạch từ đầu tiên rồi thôi |
| Highlight (`backgroundColor: .clear`) cho app nhận màu nền, gạch alpha 0,01 cho app còn lại | Vẫn gạch |

Đọc mã nguồn các engine cho thấy vì sao không có thuộc tính nào dùng được cho mọi app:

- **Chromium/Electron** tự thêm gạch mặc định khi chuỗi không có thuộc tính gạch chân
  (`InputMethodController::SetComposition`). Gạch màu trong suốt bị hiểu là "dùng màu
  chữ" (`StyleableMarker::UseTextColor`). Ở dark mode Blink luôn dùng màu chữ, bỏ qua
  mọi màu IME gửi.
- **WebKit** (macOS 14+) có chế độ highlight khi chuỗi chỉ mang màu nền/màu chữ.
- **Firefox** chỉ đọc kiểu gạch để phân loại đoạn; cách vẽ do pref
  `ui.IMERawInputUnderlineStyle` quyết định.
- **Terminal/iTerm** tự vẽ.

Quan trọng hơn: một chương trình thử gọi `setMarkedText` **thẳng** vào `NSTextView`
cho thấy NSTextView tôn trọng đúng các thuộc tính trên (không gạch). Trong TextEdit
thật vẫn gạch, nên gạch chân được thêm ở **cầu nối IMKit phía app**, ngoài tầm của IME.
Đây là suy luận từ hai quan sát, chưa lần ra đoạn mã của Apple.

InputMethodKit không có API mới từ macOS 10.7 (header SDK macOS 27).
`NSTextInputClient` từ macOS 14 đến 27 chỉ thêm API về con trỏ và glyph ảnh.

### Bộ gõ Việt của Apple làm thế nào

`/System/Library/Input Methods/VietnameseIM.app` chạy phần gõ trong
`VIM_Extension.appex`, đăng ký extension point private
`com.apple.textinputmethod-services`. Các selector trong binary
(`attachedTextDocument`, `insertCharacterAtCursor:`, `deleteCharactersInRange:`,
`commitChanges`, log `Text Input is %@available`) cho thấy nó **sửa thẳng tài liệu**
khi ô nhập hỗ trợ kiến trúc mới (Spotlight), và quay về marked text ở nơi khác. Đúng
như quan sát: bộ gõ hệ thống không gạch chân trong Spotlight nhưng gạch ở app khác.

Funput không dùng được đường này (private, không tài liệu, nhiều khả năng chỉ nạp
extension do Apple ký), nhưng nó xác nhận mô hình: **sửa trực tiếp khi được, quay về
marked text khi không**.

### Event tap gửi Backspace (kiểu OpenKey/EVKey): loại

Cần quyền Accessibility, Funput thôi là input method chuẩn, dễ race khi gõ nhanh, và
dính lỗi ô tự hoàn thành (Spotlight, thanh URL). Không làm.

## 3. Thiết kế: gõ thẳng qua IMKit công khai

Chữ đang gõ được ghi thẳng vào tài liệu bằng `insertText`. Khi engine đổi dấu hay đổi
chữ, Funput thay đúng phần đã đổi bằng `insertText(_:replacementRange:)`. Không có
marked text nên không có gạch chân.

### 3.1 Kiến trúc

Việc "đưa chữ đang gõ ra app" là một tầng riêng ở `Funput/IME/Composition/`. Controller
chỉ quyết định phím nghĩa là gì và gọi engine; nó không tự ghi gì vào client.

```text
FunputInputController ── engine (FunputComposer), quyết định phím
        │  show / commit / reopen / reset / endIfClicked
        ▼
CompositionSession ── chọn cách trình bày cho mỗi từ, nhớ khả năng client mỗi lần activate
        ├── MarkedPresenter   setMarkedText / insertText, đúng như trước khi có tính năng này
        └── DirectPresenter   insertText + replacementRange, đo lùi từ con trỏ, kiểm tra chữ
                │
                ▼
        CompositionSurface (protocol) ◄── IMKCompositionSurface: nơi duy nhất ghi chữ
                                         đang gõ vào IMKTextInput
```

| Thành phần | Vai trò |
| :--- | :--- |
| `CompositionSurface` | Những gì một cách trình bày cần từ client: `insert`, `mark`, `selection`, `text(in:)`, `supportsDocumentAccess`. Test dùng bản giả ghi lại mọi lời gọi |
| `CompositionPresenter` | Protocol một cách trình bày: `show` (trả `.shown` / `.dropped` / `.unsupported`), `commit(word, boundary)`, `reopen`, `writesIntoDocument` |
| `MarkedPresenter` | Đường cũ, không trạng thái. Test khoá chuỗi lời gọi IMK giống hệt code trước khi tách tầng |
| `DirectPresenter` | Chỉ lưu `shown` (chữ đã ghi cho từ này). **Không lưu vị trí trong tài liệu**, xem 3.3 |
| `CompositionSession` | Chọn presenter ở đầu từ, giữ nó tới khi chốt; chuyển từ marked sang ghi thẳng khi client vừa chứng minh; chốt từ khi có click |
| `ClientTrust` | Client có được ghi thẳng không: hỏi khả năng, bắt chứng minh, nhớ lời chứng minh theo app, rút lại khi thất bại (3.8) |
| `DirectEdit` | Phép tính thuần: phần cần thay và range của nó (cùng file với `DirectPresenter`) |

Mở rộng về sau: thêm một cách trình bày mới là thêm một `CompositionPresenter` và một dòng
trong lựa chọn của session; controller không đổi. Xoá cờ khi ổn định là bỏ closure
`directEnabled` khỏi session cùng khoá cài đặt.

### 3.2 Phép ghi nhỏ nhất

`DirectEdit.edit(from: shown, to: new)` so tiền tố chung theo `Character` (không bao
giờ tách dấu khỏi nguyên âm), trả về `Edit(text, replacedTail)`:

- `replacedTail == 0`: chỉ gõ thêm tại con trỏ, y như phím thường. Đa số phím rơi vào
  đây và **không cần đọc tài liệu**.
- `replacedTail > 0`: thay `replacedTail` đơn vị UTF-16 cuối của từ bằng `text`
  (`tieng` → `tiêng` thay `eng` bằng `êng`; Backspace thay bằng chuỗi rỗng; gõ tắt
  `vn` → `Việt Nam` thay cả từ).

### 3.3 Đo lùi từ con trỏ, không lưu vị trí

Bản đầu lưu vị trí tuyệt đối của từ lúc bắt đầu. Cursor (Monaco) làm lộ giả định sai:
nó báo vị trí trong một textarea ẩn và **dựng lại textarea giữa các phím**. Log thật:
từ bắt đầu ở `1`, gõ 5 ký tự, con trỏ báo `5` chứ không phải `6`, nên từ bị bỏ.

Bản hiện tại chỉ dựa vào hai điều mọi app hỗ trợ truy cập tài liệu đều phải đúng:

1. **Từ đang gõ nằm ngay trước con trỏ.** Khi cần thay, đọc `selectedRange()` một lần;
   từ nằm ở `[selection.location - shown.utf16, selection.location)`
   (`DirectEdit.wordRange`).
2. **Chữ ở đó phải đúng là `shown`.** Đọc `attributedSubstring` của range đó và so
   sánh. Không khớp nghĩa là app đã tự sửa chữ (tự hoàn thành, autocorrect, chấp nhận
   gợi ý) hoặc con trỏ đã đi chỗ khác: **bỏ từ, không ghi đè**.

Đây là cách chung, không có nhánh riêng cho app nào.

### 3.4 Gợi ý bôi đen sau con trỏ

Excel, thanh URL, Spotlight chèn sẵn phần gợi ý và bôi đen nó sau con trỏ. Gõ thêm chữ
tự thay vùng bôi đen. Khi thay chữ, range được kéo qua hết vùng chọn
(`DirectEdit.rewriteRange`), giống hệt người dùng gõ đè lên gợi ý, để app tự tính gợi ý
mới. So với Windows (phải gửi Delete trước Backspace cho Firefox), ở đây xoá gợi ý và
thay chữ nằm trong một lần ghi.

### 3.5 Click chuột

Không có marked text thì IMKit không báo khi người dùng click chỗ khác.
`MouseClickCounter` đếm click chuột toàn hệ thống bằng
`NSEvent.addGlobalMonitorForEvents` (sự kiện chuột không cần quyền Accessibility, chỉ
phím mới cần). Monitor chỉ được cài khi có từ ghi thẳng đầu tiên. Đầu mỗi phím, nếu số
click đã đổi kể từ lúc bắt đầu từ thì chốt từ (`CompositionSession.endIfClicked`), để
phím đó bắt đầu từ mới ở vị trí con trỏ mới. Từ marked text không cần việc này: IMKit tự
chốt khi có click.

### 3.6 Bỏ từ mà không mất phím

Khi `DirectPresenter` trả `.dropped` (chữ trước con trỏ không còn là từ của Funput, hoặc
con trỏ quá gần đầu để chứa từ), tài liệu giữ nguyên, session quên từ và controller xoá
engine:

- Phím chữ được **phát lại** như chữ đầu của từ mới
  (`FunputInputController.showKey`), nên trường hợp xấu nhất chỉ là ra chữ thô cho từ đó.
- Backspace được trả về cho app tự xoá.
- Ở chế độ English, chữ không bao giờ bị viết lại khi đang gõ, nên phím được trả cho app.

### 3.7 Retone sau Backspace

`Retone.plan` giữ nguyên (đọc tài liệu, hỏi engine `adopt`). `CompositionSession.reopen`
chọn presenter như một từ mới: `DirectPresenter.reopen` ghi từ lại bằng một `insertText`
thay cả từ lẫn ký tự đang xoá; `MarkedPresenter.reopen` mở lại bằng marked text như trước.
Việc này cũng sửa lỗi gợi ý của Cursor bị lặp khi xoá từ.

### 3.8 Khi nào bật

Một từ được ghi thẳng khi **cả ba** đúng lúc từ bắt đầu:

1. Công tắc bật (`AppSettings.directComposition`, đọc live nên đổi công tắc có tác dụng
   từ từ kế tiếp).
2. `supportsDocumentAccess` (`kTSMDocumentSupportDocumentAccessPropertyTag`), hỏi một lần
   mỗi lần activate: chỉ client nói giao thức truy cập tài liệu của TSM mới tôn trọng
   `replacementRange` (theo header `IMKInputSession.h`).
3. **Client đã chứng minh** nó báo đúng chữ trước con trỏ (`ClientTrust`).

Điều 3 có vì lời nhận "hỗ trợ truy cập tài liệu" không đủ. Warp (và nhiều view dựng theo
GLFW) cài `NSTextInputClient` tối giản: `selectedRange` luôn `(0, 0)`,
`attributedSubstringForProposedRange` luôn `nil`, `insertText` bỏ qua `replacementRange`
(mã nguồn: `crates/warpui/src/platform/mac/objc/host_view.m` của warpdotdev/warp). Bản đầu
tin lời nhận đó: mọi lần đổi dấu đều bị bỏ từ, gõ tiếng Việt trong Warp ra chữ thô.

Cách chứng minh, không ghi gì trước khi chứng minh xong:

- **Đầu từ**, nếu con trỏ không ở đầu văn bản: đọc ký tự ngay trước con trỏ
  (`DirectPresenter.readsBeforeCaret`). Đọc được là đủ.
- **Ô trống** (con trỏ ở 0, không phân biệt được với Warp): từ bắt đầu bằng marked text.
  Ở phím kế tiếp, nếu client báo đúng chữ marked đó ngay trước con trỏ
  (`DirectPresenter.locate`) thì client đã chứng minh, và từ chuyển sang ghi thẳng ngay:
  `insertText` gõ đè lên marked text. Gạch chân chỉ hiện ở chữ cái đầu tiên.
- Mỗi lần activate chỉ thử tối đa 4 lần (`ClientTrust.probesPerActivation`), nên client
  không bao giờ trả lời được (Warp) không bị hỏi ở mọi phím; nó dùng marked text như cũ.
- Lời chứng minh được nhớ theo bundle id trong suốt vòng đời process (`ProvenClients`,
  không lưu xuống đĩa), nên lần activate sau ghi thẳng ngay. Lần thay chữ đầu tiên không
  đặt được của một client chỉ được tin nhờ trí nhớ sẽ xoá trí nhớ đó và bắt chứng minh lại.

Nếu lúc thay chữ client trả `NSNotFound` cho vị trí con trỏ (`.unsupported`), mọi từ
còn lại của lần activate đó dùng marked text, tránh mất dấu liên tục. Con trỏ đọc được
nhưng lệch chỉ là `.dropped`: bản thử từng nhầm hai trường hợp này.

Gõ tắt ở chế độ English cũng đi qua session, nên cũng không còn gạch chân.

### 3.9 Ngân sách đọc tài liệu

Mỗi lần đọc là một round trip đồng bộ sang process khác. Trên macOS 26/27, các round
trip này là chỗ cầu nối text input của Chromium hay kẹt (xem
`platforms/macos/docs/KNOWN_ISSUES.md`). Vì vậy:

| Sự kiện | Số lần đọc |
| :--- | :--- |
| Gõ thêm chữ | 0 |
| Đổi dấu, đổi chữ, Backspace giữa từ | 2 (`selectedRange` + `attributedSubstring`) |
| Bắt đầu từ, client đã chứng minh | 0 |
| Client chưa chứng minh | Tối đa 4 lần thử mỗi activate, mỗi lần 1 hoặc 2 lần đọc |
| Retone sau Backspace | Như trước (của `Retone.plan`) |

## 4. Cấu hình

Công tắc **Gõ không gạch chân** ở Cài đặt > Cách gõ > Hiển thị chữ đang gõ, **mặc định
bật**. Lưu ở UserDefaults khoá `directComposition`, đi theo xuất/nhập cấu hình trong khối
`platform.macos.directComposition` (thiết lập riêng macOS).

Cờ thử nghiệm `experimentalDirectComposition` của bản thử không còn được đọc; có thể xoá:

```bash
defaults delete app.funput.inputmethod.Funput experimentalDirectComposition
```

Không có danh sách app. Khi tính năng ổn định, **xoá công tắc**: gõ thẳng là hành vi duy
nhất, marked text chỉ còn là đường dự phòng tự động cho app không hỗ trợ.

## 5. Kết quả thử nghiệm

| App | Kết quả |
| :--- | :--- |
| Spotlight, các app của Apple | Tốt |
| Chrome | Tốt |
| Cursor | Tốt sau khi chuyển sang đo lùi từ con trỏ (3.3) và retone ghi thẳng (3.7) |
| Terminal | Gõ được |
| Warp | Bản đầu ra chữ thô (3.8); đã sửa bằng cơ chế chứng minh, chờ thử lại |
| Excel | Chưa thử |

Log Cursor sau khi sửa: các lần thay (`rewrite {2, 3} selection={5, 0}`, …) đều khớp,
Backspace giữa từ và retone (`reopen {0, 5}`) chạy đúng.

## 6. Việc còn lại

Các mục của bản thử đã xong trong bản kiến trúc: phân loại "không có con trỏ" (3.8),
công tắc trong Cài đặt, gõ tắt English, xoá log tạm, README và `KNOWN_ISSUES.md`.

Còn lại:

1. Thử lại trên máy thật với bản kiến trúc (danh sách ở mục 8), nhất là Terminal và Excel.
2. Theo dõi rủi ro vùng chọn giả (mục 7) sau khi phát hành.
3. Khi ổn định: xoá công tắc (mục 4).

## 7. Rủi ro đã biết

- **Vùng chọn giả.** Cursor từng báo `{0, 17}` khi người dùng chỉ thấy con trỏ. Nếu
  điều đó xảy ra đúng lúc đổi dấu, `rewriteRange` sẽ phủ cả vùng chọn và có thể xoá ký
  tự ngay sau con trỏ (ví dụ `)` tự đóng). Chưa gặp trong thử nghiệm.
- **Undo** có thể tách theo từng lần ghi thay vì cả từ.
- **App nhận sự kiện thay đổi văn bản ở mỗi phím** (tìm kiếm khi đang gõ chạy lại liên
  tục). Với marked text điều này cũng xảy ra ở nhiều app, nên không phải hồi quy lớn.
- **Chromium báo vị trí từ bộ nhớ đệm bất đồng bộ.** Gõ rất nhanh có thể làm lệch;
  thiết kế chọn bỏ từ chứ không ghi sai chỗ.

## 8. Kiểm thử

Tự động, `FunputTests/Composition/`:

| File | Khoá điều gì |
| :--- | :--- |
| `MarkedPresenterTests` | Chuỗi lời gọi IMK của đường cũ giữ nguyên sau khi tách tầng |
| `DirectPresenterTests` | Gõ thêm không đọc tài liệu; đổi dấu; Backspace; client đánh số lại kiểu Cursor; phủ gợi ý bôi đen; chữ bị sửa sau lưng thì không ghi; con trỏ lệch là `.dropped`, không đọc được con trỏ là `.unsupported`; commit, gõ tắt, reopen |
| `CompositionSessionTests` | Chọn presenter theo công tắc và khả năng client; hỏi client một lần mỗi activate; từ giữ presenter khi đổi công tắc; `.unsupported` dùng marked tới khi đổi ô; click chỉ chốt từ ghi thẳng |
| `CompositionTrustTests` | Chứng minh bằng ký tự trước con trỏ; ô trống chuyển sang ghi thẳng ở phím thứ hai; client mù kiểu Warp giữ marked và chỉ bị hỏi 4 lần; nhớ lời chứng minh theo app; rút lại khi thất bại; mất con trỏ thì marked tới khi đổi ô |
| `DirectEditTests` | Phép ghi nhỏ nhất và range, tính theo UTF-16, không tách dấu tổ hợp |

`FunputTests/FakeCompositionSurface.swift` là ô nhập giả dùng chung: áp dụng ghi và marked
text như app thật, mô phỏng client đánh số lại và client mù, ghi lại mọi lời gọi.

`ConfigDocumentTests` khoá xuất/nhập công tắc. `scripts/check-swift-loc.sh` giới hạn hai
thư mục `Composition` ở 5 file.

Bằng tay, kịch bản: gõ từ có dấu, đổi dấu giữa từ, Backspace giữa từ, retone sau Backspace, gõ
tắt, khôi phục tiếng Anh, click chỗ khác giữa từ rồi gõ tiếp, Undo, ô tự hoàn thành
(Spotlight, thanh URL, Excel), editor có gợi ý (Cursor, VS Code), gõ nhanh trong
Chrome, Terminal.

## 9. Mã nguồn

Đường dẫn tính từ `platforms/macos/`.

| File | Vai trò |
| :--- | :--- |
| `Funput/IME/Composition/CompositionSurface.swift` | Protocol surface và `IMKCompositionSurface` |
| `Funput/IME/Composition/CompositionPresenter.swift` | Protocol presenter, `CompositionOutcome`, `MarkedPresenter` |
| `Funput/IME/Composition/DirectPresenter.swift` | Gõ thẳng: thay chữ có kiểm tra, cách chứng minh client; kèm `DirectEdit` |
| `Funput/IME/Composition/ClientTrust.swift` | `ClientTrust` và `ProvenClients` |
| `Funput/IME/Composition/CompositionSession.swift` | Chọn presenter, nhớ khả năng client, chốt từ khi click |
| `Funput/IME/MouseClickCounter.swift` | Đếm click chuột toàn hệ thống, cài lười |
| `Funput/IME/FunputInputController+Composition.swift` | `showComposition`, `showKey` (phát lại phím), commit qua session |
| `Funput/Settings/Panes/TypingPane.swift` | Công tắc **Gõ không gạch chân** |

## 10. Nguồn tham khảo

- [IMKInputController `mark(forStyle:at:)`](https://developer.apple.com/documentation/inputmethodkit/imkinputcontroller/mark(forstyle:at:).md)
- [Chromium `render_widget_host_view_cocoa.mm`](https://chromium.googlesource.com/chromium/src/+/main/content/app_shim_remote_cocoa/render_widget_host_view_cocoa.mm)
- [Blink `styleable_marker_painter.cc`](https://chromium.googlesource.com/chromium/src/+/main/third_party/blink/renderer/core/paint/styleable_marker_painter.cc)
- [Blink `input_method_controller.cc`](https://chromium.googlesource.com/chromium/src/+/main/third_party/blink/renderer/core/editing/ime/input_method_controller.cc)
- [WebKit `WebViewImpl.mm`](https://github.com/WebKit/WebKit/blob/main/Source/WebKit/UIProcess/mac/WebViewImpl.mm)
- [Firefox `TextInputHandler.mm`](https://github.com/mozilla-firefox/firefox/blob/main/widget/cocoa/TextInputHandler.mm)
