# Tự tắt gõ tiếng Việt theo bàn phím ngoại

## Trạng thái

Đã có trong bản Windows. Setting `autoEnglishOnForeignLayout`, **mặc định bật**,
công tắc nằm ở Cài đặt → Tổng quan → "Chế độ gõ".

## Mục tiêu

Funput là hook toàn cục: nó nuốt phím rồi gõ lại chuỗi thay thế. Cách đó chỉ đúng
trên một layout Latin. Trên một IME tiếng Nhật, Backspace của Funput rơi vào giữa
composition của IME và làm hỏng chữ; trên bàn phím Nga hay Thái thì phím gõ ra
không bao giờ là chữ cái mà Telex/VNI viết bằng.

Người dùng gõ tiếng Nhật thường xuyên trước đó phải tự nhớ tắt Funput mỗi lần đổi
ngôn ngữ bàn phím. Tính năng này bỏ bước đó đi.

## Layout nào bị coi là "ngoại"

`funput_desktop::is_foreign_layout(hkl)` — hàm thuần, quyết định từ chính `HKL`:

| Điều kiện | Ví dụ |
| --- | --- |
| Nibble cao là `0xE` → IME (TSF text service) | `0xE0200411` MS-IME Nhật, `0xE0010804` MS Pinyin |
| Primary language (`LANGID & 0x3FF`) thuộc bảng chữ không-Latin | `0x0419` Nga, `0x041E` Thái, `0x0408` Hy Lạp |

Tiếng Việt (`0x2A`) không bao giờ là ngoại. Layout Latin biến thể vẫn là Latin —
Dvorak (`0xF0010409`) và US-International giữ nguyên phần ngôn ngữ, nên chỉ nửa
thấp quyết định.

CJK nằm ở **cả hai** nhánh: handle chỉ nói "IME" khi IME đang được nạp, còn ngôn
ngữ thì đúng cho cả layout bàn phím trần.

## Trạng thái treo không phải là setting

`ShellState` giữ ba trường **chỉ sống trong session**: `last_layout`,
`layout_suspended`, `layout_override`. `enabled()` trả về
`settings.enabled && !layout_suspended`.

Cố ý không ghi đè `settings.enabled`:

- Đổi layout xảy ra rất thường xuyên — mỗi lần một lượt ghi file là quá đắt.
- `reload_settings()` so `loaded.enabled` với `settings.enabled` để phát hiện một
  lần lật VI/EN từ process khác. Bẻ trường đó sẽ sinh `pending_override` giả.
- Trạng thái treo mô tả bàn phím đang trước mặt người dùng; nó vô nghĩa sau khi
  khởi động lại và được tính lại từ đầu.

## Windows đọc layout ở đâu

Không có thông báo toàn cục nào cho việc đổi ngôn ngữ nhập.
`WM_INPUTLANGCHANGE` chỉ đến cửa sổ của chính app tạo ra thay đổi, còn các sink
TSF (`ITfInputProcessorProfileActivationSink`, `ITfLanguageProfileNotifySink`)
chỉ theo thread và cần TSF thread manager nằm trong process đó. Nên phải hỏi.

`keymap::foreground_layout()` hỏi bằng ba lời gọi user32 rẻ:
`GetForegroundWindow` → `GetWindowThreadProcessId` → `GetKeyboardLayout(tid)`.
Ngôn ngữ nhập là thuộc tính của **thread** sở hữu cửa sổ, nên câu hỏi này đúng dù
người dùng đặt Windows dùng chung một ngôn ngữ hay mỗi cửa sổ một ngôn ngữ.

Hỏi ở hai chỗ:

1. Đầu mỗi keydown trong `WH_KEYBOARD_LL`, **trước** khi có gì quyết định số phận
   phím đó. Windows xử lý xong Win+Space trước phím kế tiếp, nên phím đầu tiên gõ
   bằng layout mới đã được xét đúng — không có phím nào bị inject sai.
2. Trong hook `EVENT_SYSTEM_FOREGROUND`, **sau** `apply_for_app`: layout là tiếng
   nói cuối cùng, và vì ngôn ngữ nhập theo thread nên đổi app có thể đổi layout mà
   không có keystroke nào ở giữa.

`apply_for_layout` trả `None` cho layout nó vừa thấy, tức gần như mọi lần được
hỏi, nên chi phí thường trực chỉ là ba lời gọi đó cộng một lần khoá mutex.

## Người dùng vẫn có tiếng nói cuối

Bấm hotkey VI/EN trong lúc đang bị treo sẽ bật tiếng Việt trở lại và ghi
`layout_override` cho đúng layout đó — Funput nói ý của mình một lần rồi thôi.
Override mất hiệu lực khi người dùng chuyển sang layout khác.

Tắt công tắc trong Cài đặt gỡ ngay trạng thái treo đang có
(`ShellState::redecide_layout`), không đợi tới lần đổi bàn phím kế tiếp.

## Điều đã biết

- Icon khay hệ thống chỉ cập nhật khi có một keystroke hoặc một lần đổi app. Cố ý:
  đánh đổi lấy việc không phải nuôi một `SetTimer` chỉ để bắt kịp cái icon.
- Hotkey mặc định `Alt Shift` của Funput trùng với hotkey đổi ngôn ngữ nhập mặc
  định của Windows. Ai dùng cả hai sẽ thấy hai hành vi chồng lên nhau — không phải
  do tính năng này, nhưng đây là chỗ nó lộ ra rõ nhất.
- Setting không nằm trong file xuất/nhập cấu hình, cùng nhóm với `launchAtLogin`
  và `enabled` — đều là lựa chọn cục bộ của một máy.
