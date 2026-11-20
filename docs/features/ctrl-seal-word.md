# Bấm Ctrl để chốt từ đang gõ

> **Trạng thái:** đã làm cho Windows (issue #736): engine, FFI, shell desktop và công tắc trong Cài đặt. **Bật mặc định, có công tắc tắt.** Linux làm sau, macOS chưa làm.

Nguồn: issue #582 (Windows), mục "Chức năng của Ctrl".

## Vấn đề

Unikey cho bấm Ctrl rồi thả (không kèm phím nào) để dừng biến đổi từ đang gõ:

- `dung` Ctrl `x` ra `dungx`, không thành `dũng`.
- `CT` Ctrl Space ra `CT␣`, không bung gõ tắt `ct` → `cơm tấm`.

Trước đây Funput bỏ qua cú bấm này: [keydown.rs](../../platforms/windows/src/background/hook/keyboard/keydown.rs) trả các phím bổ trợ về cho app trước khi tới engine, nên `dung` Ctrl `x` vẫn ra `dũng` và `CT` Ctrl Space vẫn bung.

## Hành vi

Bấm và thả **Ctrl đứng một mình** khi đang gõ dở một từ thì **chốt từ đó đúng như đang hiện**:

- Từ không còn nhận phím nào nữa. Phím gõ tiếp bắt đầu một từ mới, dính liền phía sau.
- Từ đã chốt **không** bung gõ tắt, **không** bị sửa lỗi gõ, **không** bị English restore. Người dùng đã nói rõ là muốn đúng thứ đang thấy.
- Con trỏ không di chuyển, nên Backspace vẫn mở lại được từ vừa chốt như mọi từ đã xong (`retone`).
- Trạng thái câu (tự viết hoa đầu câu) giữ nguyên: chốt không kết thúc câu.
- Không có từ nào đang gõ thì cú bấm không làm gì.

Ví dụ:

| Gõ | Kết quả |
|---|---|
| `dung` Ctrl `x` | `dungx` |
| `CT` Ctrl Space | `CT␣` |
| `haf` Ctrl `s` | `hàs` (giữ dấu phần đầu, phần sau gõ thẳng) |
| `dung` Ctrl ⌫ `s` | `dún` (⌫ xóa `g` và mở lại `dun`, `s` thêm dấu) |
| Ctrl khi không gõ dở | không đổi gì |

### Khác phím lật từ

Phím lật từ (Ctrl+Shift+Z/X, [flip.rs](../../crates/funput-engine/src/compose/flip.rs)) vẫn giữ nguyên, hai tính năng bổ sung cho nhau:

| | Lật từ | Ctrl để chốt |
|---|---|---|
| Dùng lúc nào | **Sau** khi chữ đã bị biến đổi, để *đổi lại* | **Trước** phím sẽ gây biến đổi, để *chốt* |
| `dung` + `x` | `dũng` rồi lật thành `dungx` | Ctrl sau `dung`, gõ `x` ra luôn `dungx` |
| Gõ tắt `CT` | Không làm gì (không có dấu để lật), Space vẫn bung | Space không bung |
| Giữ dấu phần đầu | Không, lật cả từ | Được |
| Chiều ngược (`card` → `cải`) | Được | Không |

Không gộp làm một: gán Ctrl đứng một mình làm phím lật thì `dung` Ctrl không có gì để lật; cho phím lật chặn thêm gõ tắt thì một phím làm hai việc tùy ngữ cảnh, khó đoán.

### So với Backspace hoàn tác gõ tắt

Backspace hoàn tác ([shortcut-undo.md](shortcut-undo.md), [shortcut-case-and-repair.md](shortcut-case-and-repair.md)) sửa **sau** khi đã bung. Ctrl chặn **trước**, nên chữ bung không hiện ra rồi bị xóa, đỡ chớp chữ trong app chat. Giữ cả hai.

## Engine

`Engine::seal()` ([editing.rs](../../crates/funput-engine/src/engine/editing.rs)):

- Kết thúc từ đang gõ **mà không chạy ranh giới**: không tra gõ tắt, không sửa lỗi gõ, không English restore, không ghi chữ nào ra. Từ đã nằm sẵn trên màn hình (Windows inject) hoặc nằm trong vùng đang gõ (preedit), việc đưa nó vào văn bản là của nền tảng.
- Trả về chữ đang hiện để nền tảng preedit commit, và để shell desktop đưa vào `CommittedTail`.
- Xóa trạng thái từ đang gõ như ranh giới làm (`Session::clear`), giữ trạng thái câu.
- Hủy nhịp hoàn tác gõ tắt đang chờ và lệnh bỏ qua một lần (`shortcut_undo::dismiss`).
- Không có từ đang gõ thì không làm gì và báo lại là không có gì để chốt.

Có ở FFI là `funput_engine_seal`, để Linux dùng khi làm tới. Chưa đưa qua JNI: Android không có Ctrl, thêm vào lúc này chỉ là code không ai gọi.

Chế độ tiếng Anh cũng chốt được khi engine đang giữ từ, tức là khi gõ tắt chạy ở tiếng Anh: `ct` Ctrl Space không bung ở cả hai chế độ.

## Nền tảng

### Windows (đã làm)

Phần nhận diện có sẵn: `hotkey::on_key_event` báo `Gesture::Modifiers` khi bấm rồi thả một tập phím bổ trợ không kèm phím thường ([keyboard/mod.rs](../../platforms/windows/src/background/hook/keyboard/mod.rs)). Alt đứng một mình dùng cho phím tắt menu Office ([access-keys.md](access-keys.md)), Ctrl đứng một mình dùng cho việc chốt từ.

- Khi `Gesture::Modifiers` là **đúng Ctrl** (trái hoặc phải) và công tắc đang bật: `ShellState::seal_word` ([compose.rs](../../crates/funput-desktop/src/shell/compose.rs)) gọi `seal()`, rồi đưa chữ đã chốt vào `CommittedTail` như chữ vừa được ghi ra. **Không** gọi `caret_moved`: hàm đó xóa `CommittedTail` và làm mất khả năng mở lại từ bằng Backspace.
- Không nuốt phím Ctrl: thả Ctrl phải tới app, nếu không app tưởng Ctrl vẫn đang giữ.
- Ctrl+Shift, Ctrl+Alt và các tổ hợp khác không chốt.

### Linux (fcitx5, ibus)

Nhận được sự kiện thả phím bổ trợ. Preedit: commit vùng đang gõ bằng chữ `seal()` trả về. Non-preedit: như Windows. Làm sau Windows.

### macOS

Phím bổ trợ đi qua `flagsChanged`, không qua đường phím thường. Ctrl trên Mac ít dùng hơn, cần xem có xung đột với phím tắt hệ thống (Ctrl+Space đổi nguồn nhập) không. Chưa chốt có làm không.

### Điện thoại

Không có Ctrl. Đã có chip và Backspace để hoàn tác gõ tắt.

## Cài đặt

Một công tắc ở trang Phím tắt, ngay dưới phím lật từ: "Bấm Ctrl để giữ nguyên từ đang gõ". Câu mô tả: "Bấm và thả Ctrl khi đang gõ dở thì từ giữ nguyên như đang hiện, không thêm dấu và không bung gõ tắt."

Lưu trong `settings.json` là `ctrlSealsWord`, và đi theo file xuất cấu hình ở `preferences.ctrlSealsWord`. File viết trước khi có công tắc cũng đọc là **bật**: trước đó cú bấm không làm gì, nên bật lên không lấy mất hành vi nào.

Hướng dẫn trên website đặt Ctrl và phím lật từ cạnh nhau để người dùng phân biệt.

## Rủi ro cần kiểm tra trên máy Windows

- **Bấm Ctrl nhầm**, ví dụ định Ctrl+C rồi đổi ý. Cái giá chỉ là từ bị chốt sớm, Backspace mở lại được.
- **Ctrl + cuộn chuột** (phóng to trong trình duyệt, Office): hook chuột báo mọi thao tác trừ di chuột cho bộ nhận diện, nên cuộn hay click giữa lúc giữ Ctrl hủy cú bấm. Vẫn cần thử trên máy thật.
- **Ctrl + click**: cú click đã làm Funput coi như con trỏ di chuyển, nên chốt hay không cũng vậy. Vẫn kiểm tra thứ tự sự kiện.
- **Phím tắt tự ghi**: bộ ghi phím không nhận một phím bổ trợ đứng một mình. Nếu file cấu hình sửa tay gán Ctrl đứng một mình làm phím tắt thì phím tắt thắng và không chốt.
- **Phím tắt menu (Alt)**: chốt không đụng tới `AccessKeys`.

## Đã chốt

- Mặc định bật, kể cả với người đang dùng bản cũ.
- macOS chưa làm.

## Kiểm thử

- Engine: `seal()` trên từ đang gõ trả đúng chữ đang hiện; không bung gõ tắt, không sửa lỗi gõ, không English restore; giữ trạng thái câu; không có từ thì không làm gì; hủy nhịp hoàn tác gõ tắt.
- `crates/funput-desktop/tests/`: các ví dụ trong bảng Hành vi, gồm Backspace mở lại từ đã chốt.
- Windows thật: Notepad, Word, Chrome, Firefox, Zalo. Ctrl + cuộn chuột, Ctrl + click, Ctrl+C bỏ dở.
