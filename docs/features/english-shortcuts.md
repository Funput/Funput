# Gõ tắt trong chế độ tiếng Anh

## Trạng thái

Đã có trong bản Windows. Setting `shortcutsInEnglish`, **mặc định bật**, công tắc
nằm ở Cài đặt → Gõ tắt. File cấu hình đã mang sẵn khoá này, nên các nền tảng khác
chỉ còn phần UI và phần shell:

- **macOS, Linux, iOS, Android**: shell tự chặn chế độ EN trước khi gọi engine, nên
  chưa có gì đổi. Muốn bật, ngoài UI còn cần một setter `shortcuts_in_english` trên
  C ABI — hiện chưa có.
- **funput-term**: driver cũng tự chặn EN (`state.composing()`) và không bao giờ gọi
  `Engine::set_enabled`, nên khoá này đọc được mà chưa có tác dụng.

## Mục tiêu

Gõ tắt là *text expansion*, không phải tính năng tiếng Việt: `sdt` → số điện thoại,
`mail` → địa chỉ email vẫn cần bung khi người dùng đang viết tiếng Anh. Trước đây
không bung được, vì cả hook lẫn engine đều buông tay hoàn toàn ở chế độ EN.

## Chế độ EN làm gì và không làm gì

Ở chế độ EN, engine **chỉ** ghi nhớ phím thô của từ đang gõ và bung chữ tắt khi gặp
ranh giới từ (khoảng trắng hoặc dấu câu). Không bỏ dấu, không English restore,
không tự viết hoa, không flip — app tự hiện từng phím như khi Funput đứng ngoài.

Vì app tự hiện phím, `Session::buffer` ở chế độ EN chứa đúng phím thô: đó là "văn
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
