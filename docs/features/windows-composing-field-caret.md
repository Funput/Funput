# Windows: ô Nội dung gõ tắt bỏ qua vị trí con trỏ

> **Trạng thái:** lỗi đã rõ nguyên nhân, chưa sửa. Đã chọn hướng sửa (bộ ghép biết vị trí con trỏ).

Nguồn: issue #582 (Windows), mục "Báo lỗi": trong bảng gõ tắt, không thêm hay xóa được ký tự ở giữa ô Nội dung, bôi đen cũng không được, con trỏ luôn nhảy về cuối.

## Phạm vi

- Chỉ ô **Nội dung** trong bảng gõ tắt của Settings trên Windows ([row.slint](../../platforms/windows/ui/pages/shortcuts/row.slint)). Đây là chỗ duy nhất dùng `ComposingInput`.
- Ô **Chữ tắt** là `LineEdit` thường, không bị.
- macOS và Linux dùng ô nhập của hệ thống, không bị.

## Nguyên nhân

Settings là cửa sổ của chính Funput, hook toàn cục bỏ qua nó (`FOREGROUND_IS_FUNPUT`) và winit cũng bỏ qua phím giả lập. Vì vậy ô Nội dung tự ghép tiếng Việt: `ComposingInput` ([composing_input.slint](../../platforms/windows/ui/overlays/composing_input.slint)) gửi từng phím qua global `Compose` tới `FieldComposer` ([compose.rs](../../platforms/windows/src/ui/compose.rs)).

`FieldComposer` coi nội dung ô là `committed` (phần đã xong) + `engine.buffer()` (từ đang gõ), luôn ở **cuối**. Nó không biết con trỏ ở đâu:

| Thao tác | Code hiện tại | Hậu quả |
|---|---|---|
| Gõ một phím | `self.text = Compose.key(...)` dựng lại cả nội dung, rồi `set-selection-offsets(1000000, 1000000)` | Chữ thêm vào cuối, con trỏ nhảy về cuối |
| Backspace | `committed.pop()` hoặc `engine.on_backspace()`, rồi đẩy con trỏ về cuối | Xóa ký tự cuối, không phải ký tự trước con trỏ |
| Gõ đè vùng bôi đen | Vùng chọn không được xét | Vùng chọn còn nguyên, chữ thêm vào cuối |
| Click chuột vào giữa | Không có sự kiện nào báo cho bộ ghép | Phím kế tiếp vẫn ghép ở cuối |
| Mũi tên, Home, End, Delete | `Compose.reset(text)` rồi để `TextInput` tự xử lý | Con trỏ di chuyển được, nhưng phím gõ tiếp vẫn ghép ở cuối |

Ctrl+X, Ctrl+V, Ctrl+Z, Ctrl+Y gọi API của `TextInput` nên đúng vị trí, rồi `reset` lại, nên chúng không bị lỗi.

## Hướng sửa (đã chọn)

Đổi mô hình của `FieldComposer` thành **phần trước con trỏ + từ đang gõ + phần sau con trỏ**.

1. **Kiểm tra lại trước mỗi phím.** `ComposingInput` gửi kèm nội dung, `cursor-position-byte-offset` và `anchor-position-byte-offset` thật của `TextInput`. Nếu khác chỗ bộ ghép đang nghĩ (click, kéo chọn, mũi tên, Shift+mũi tên, Ctrl+Z…) thì dựng lại trạng thái từ nội dung và con trỏ thật. Một chỗ kiểm tra này bắt được mọi kiểu di chuyển con trỏ, không cần sự kiện riêng cho từng kiểu.
2. **Có vùng chọn thì xóa vùng chọn trước**, rồi ghép phím tại chỗ đó. Phím là ranh giới từ (khoảng trắng, dấu câu) cũng thay vùng chọn.
3. **Backspace:** có vùng chọn thì xóa vùng chọn. Không có thì lùi từ đang gõ nếu đang gõ dở, hoặc xóa một ký tự ngay trước con trỏ.
4. **Trả về nội dung mới và vị trí con trỏ mới** (ngay sau từ đang gõ). `ComposingInput` đặt con trỏ đúng chỗ đó thay cho `1000000`.
5. **Mở lại từ ngay trước con trỏ** bằng `Engine::adopt` khi dựng lại trạng thái, để đặt con trỏ sau `cơm` rồi gõ `f` thì ra `cờm`, giống cách gõ ngoài app. `adopt` từ chối thì gõ như một từ mới.

Global `Compose` đổi chữ ký cho phù hợp, ví dụ `key(text, cursor, anchor, typed) -> { text, cursor }` và tương tự cho `backspace`. `reset` lúc focus giữ nguyên ý nghĩa.

### Chỗ cần cẩn thận

- Slint tính vị trí bằng **byte UTF-8**. Chữ tiếng Việt có dấu chiếm 2 đến 3 byte. Mọi phép cắt phải theo byte và phải rơi đúng ranh giới ký tự (`is_char_boundary`), nếu không sẽ panic. Vị trí lệch ranh giới (không nên xảy ra) thì làm tròn về ranh giới gần nhất phía trước thay vì panic.
- `cursor` có thể nhỏ hơn `anchor` (chọn ngược từ phải sang trái). Luôn lấy `min` và `max`.
- `smart_restore` vẫn tắt trong ô này như hiện nay.

### Không chọn

Chỉ ghép khi con trỏ ở cuối và không có vùng chọn, còn lại để `TextInput` tự chèn ký tự thô. Sửa được chuyện con trỏ nhảy, nhưng gõ ở giữa không có dấu. Với ô chứa nội dung tiếng Việt thì chưa đủ.

## Kiểm thử

- `FieldComposer` (test đơn vị đã có trong [compose.rs](../../platforms/windows/src/ui/compose.rs)), thêm:
  - gõ ở giữa, gõ ở đầu;
  - gõ đè vùng chọn, chọn xuôi và chọn ngược;
  - Backspace ở giữa, Backspace xóa vùng chọn;
  - con trỏ đổi chỗ giữa hai phím (giả lập click) thì phím sau ghép ở chỗ mới;
  - chữ có dấu quanh con trỏ: cắt đúng ranh giới byte;
  - mở lại từ trước con trỏ (`cơm` + `f` → `cờm`), và từ không mở lại được (`hello` + `s` → `hellos`).
- Thử tay trên Windows: click chuột, kéo chọn, Shift+mũi tên, Ctrl+Shift+mũi tên, Home/End, Delete, Ctrl+Z/Y, dán đè vùng chọn.
