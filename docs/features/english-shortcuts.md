# Gõ tắt trong chế độ tiếng Anh

## Trạng thái

Đã có trong bản Windows và macOS. Setting `shortcutsInEnglish`, **mặc định bật**, công tắc
nằm ở Cài đặt → Gõ tắt. File cấu hình đã mang sẵn khoá này, nên các nền tảng khác
chỉ còn phần UI và phần shell:

- **Linux, iOS, Android**: shell vẫn tự chặn chế độ EN trước khi gọi engine.
  C ABI đã có `funput_set_shortcuts_in_english` để các host triển khai sau sử dụng.
- **funput-term**: driver cũng tự chặn EN (`state.composing()`) và không bao giờ gọi
  `Engine::set_enabled`, nên khoá này đọc được mà chưa có tác dụng.

## Mục tiêu

Gõ tắt là *text expansion*, không phải tính năng tiếng Việt: `sdt` → số điện thoại,
`mail` → địa chỉ email vẫn cần bung khi người dùng đang viết tiếng Anh. Trước đây
không bung được, vì cả hook lẫn engine đều buông tay hoàn toàn ở chế độ EN.

## Chế độ EN làm gì và không làm gì

Ở chế độ EN, engine **chỉ** ghi nhớ phím thô của từ đang gõ và bung chữ tắt khi gặp
ranh giới từ (khoảng trắng hoặc dấu câu). Không bỏ dấu, không English restore,
không tự viết hoa, không flip. Windows để app tự hiện phím; macOS giữ cùng chuỗi
phím thô dưới dạng marked text để thay thế được trên các client như Chrome.

Vì không biến đổi phím, `Session::buffer` ở chế độ EN chứa đúng phím thô: đó là "văn
bản đang hiển thị", và cũng là thứ mà expansion phải xoá đi. Nhờ vậy
`compose::boundary::shortcut::expansion` dùng chung, không có nhánh thứ hai.

## Ba điều kiện để bật

`Session::english_shortcuts()` (engine) và `ShellState::english_shortcuts_active()`
(shell) phải cùng đúng:

| Điều kiện | Vì sao |
| --- | --- |
| `shortcutsEnabled` | Tắt gõ tắt là tắt cả hai chế độ — một tính năng, một công tắc |
| `shortcutsInEnglish` | Công tắc riêng của tính năng này |
| Bảng gõ tắt không rỗng | Không có gì để bung thì hook không cần chạm vào phím: EN mode giữ nguyên hành vi cũ |

Phía shell còn một phủ quyết nữa: **layout ngoại**. Khi tiếng Việt bị tạm tắt vì bàn
phím ngoại (xem [foreign-layout.md](foreign-layout.md)), gõ tắt cũng không chạy —
inject vào IME tiếng Nhật làm hỏng composition của nó y như bỏ dấu.

## Đường đi của phím

`hook/keyboard.rs` đổi cổng gác từ `shell::enabled()` sang `shell::hook_active()`
= `effective_enabled() || english_shortcuts_active()`. Qua được cổng đó, phím đi
tiếp đúng đường cũ: `classify` → `shell::process_key` → `Engine::process_key`, và
ở EN mode engine trả `ImeResult::none()` cho mọi phím thường (plan noop ⇒ phím tới
thẳng app), chỉ ranh giới trúng chữ tắt mới inject rồi nuốt phím ranh giới.

Hotkey flip vẫn khoá theo `shell::enabled()`: ở EN mode không có dạng tiếng Việt nào
để lật.

## macOS

Cài đặt → Gõ tắt có công tắc **Gõ tắt khi tắt tiếng Việt**, mặc định bật.
`shortcutsInEnglish` lưu trong UserDefaults và đi cùng preferences khi nhập/xuất
cấu hình Windows/macOS. Tệp cũ thiếu khoá này giữ nguyên lựa chọn hiện tại.

Khi Funput là nguồn nhập đang hoạt động nhưng ở EN, từ đang gõ được giữ dưới dạng
marked text như đường VI, nhưng nội dung là phím thô: không bỏ dấu, tự viết hoa
hay retone. Engine dùng cùng quy tắc gõ tắt với Windows (kể cả `[`/`]` ở Telex nâng
cao và số bàn phím phụ). Khi hệ thống đổi sang nguồn nhập khác, InputMethodKit
ngừng gửi phím tới Funput.

Controller thay marked text bằng expansion khi khớp trigger; nếu không khớp thì
commit nguyên từ và ranh giới. Không truy vấn `selectedRange` hay văn bản đã commit:
Chrome omnibox không cung cấp đủ thông tin cho chiến lược đọc lại đó, mặc dù vẫn
hỗ trợ marked text. Enter/Tab giữ hành động của app sau khi commit. Focus, phím
điều hướng, Command/Control và thay đổi cấu hình kết thúc composition, giữ lại
nguyên văn bản đang gõ và không nhân đôi khi đổi chế độ.

`funput_process_key_text` trả đầy đủ output qua callback UTF-8 đồng bộ, tránh giới
hạn 64 codepoint của `FunputResult` cũ mà không đổi layout ABI đang dùng bởi host khác.

### Kiểm tra thủ công

1. Chọn Funput làm nguồn nhập, thêm `sdt` → `0901234567`, `vn` → `Việt Nam`.
2. Tắt tiếng Việt bằng phím tắt hoặc menu, gõ `sdt ` và `VN.` trong TextEdit:
   nhận `0901234567 ` và `VIỆT NAM.`. Gõ `address` vẫn giữ nguyên.
3. Thử `vnx` + Backspace + Space, Enter/Tab, `vn[` ở Telex nâng cao;
   tắt lần lượt hai công tắc gõ tắt để kiểm tra passthrough.
4. Đổi VI/EN hoặc chuyển ứng dụng giữa chừng: không bung trigger từ phiên cũ,
   không nhân đôi chữ. Thử lại sau khi đóng/mở app và nhập/xuất cấu hình.

Gõ tắt EN dùng marked text nên có thể ảnh hưởng thời điểm app hiển thị autocomplete.
Các vấn đề cầu nối Chromium/Electron đã ghi ở
[KNOWN_ISSUES.md](../../platforms/macos/docs/KNOWN_ISSUES.md) vẫn áp dụng.
