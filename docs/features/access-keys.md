# Phím tắt menu sau khi tap Alt (Excel KeyTips)

## Trạng thái

Đã có trong bản Windows. Luôn bật, không có công tắc trong Cài đặt. Issue gốc:
[#566](https://github.com/Funput/Funput/issues/566).

## Vấn đề

Nhiều phím tắt của Windows và Office được gõ **lần lượt từng phím**, không giữ tổ hợp.
Người dùng bấm Alt rồi nhả ra, sau đó gõ từng chữ. Ví dụ trong Excel: **Alt, E, S, V**
mở Paste Special → Values, còn **Alt, H, V, V** dán giá trị bằng ribbon.

Funput chỉ nhận ra một phím là phím tắt khi Ctrl/Alt/Win **đang được giữ**. Với kiểu
gõ trên, Alt đã nhả trước khi bấm E, nên E, S, V bị đưa vào engine như chữ bình thường.
Ở Telex, `e` + `s` biến thành `é`: Funput gửi Backspace và `é`, app không bao giờ
nhận được phím S, và chuỗi phím tắt bị gãy giữa chừng. Mọi chuỗi có phím mà Telex/VNI
biến đổi được (`w`, `dd`, `aa`, các phím dấu, chữ số khi gõ VNI) đều gặp lỗi này.

## Hành vi

Khi người dùng **tap Alt một mình** (bấm rồi nhả, không có phím hay click nào ở giữa),
Funput vào **chế độ access key**:

- Từ đang soạn được chốt lại như hiện có, vì menu đã lấy bàn phím.
- Các phím tiếp theo đi thẳng tới app, **không qua engine**. Gõ tắt cũng không chen vào.

Funput không thấy được lúc app đóng menu, nên chế độ kết thúc theo các tín hiệu dưới đây,
và luôn chọn hướng an toàn:

| Tín hiệu | Phím đó |
| --- | --- |
| Enter, Esc, Tab, phím mũi tên, F-key, phím tắt có Ctrl/Alt/Win | Được xử lý như bình thường (chốt từ) |
| Space hoặc khoảng trắng khác | Đi thẳng tới app, sau đó gõ lại tiếng Việt |
| Tap Alt lần nữa (Windows cũng đóng menu) | — |
| Click chuột, đổi cửa sổ, hotkey VI/EN hoặc flip | — |
| Không gõ phím nào trong 3 giây | Phím kế tiếp được xử lý như bình thường |

Nếu tap Alt ở một app không có menu rồi gõ ngay `xin chaof`, chữ `xin` ra nguyên dạng
và `chào` vẫn đúng. Thiệt hại tối đa là một từ.

Hotkey luôn được ưu tiên: nếu người dùng ghi hotkey VI/EN là một mình Alt, tap Alt vẫn
chuyển ngôn ngữ chứ không vào chế độ access key. Alt+Shift, Alt+phím, Alt+click và AltGr
không phải là tap Alt.

## Thiết kế

| Lớp | File | Vai trò |
| --- | --- | --- |
| Lõi thuần | `crates/funput-desktop/src/access_keys/` | `AccessKeys`: state machine, nhận thời gian từ bên ngoài, có unit test |
| Nhận diện tap | `platforms/windows/src/background/hotkey.rs` | `Watcher` có sẵn trả `Gesture::Modifiers` khi gesture không phải hotkey |
| Glue Win32 | `platforms/windows/src/background/hook/access_keys.rs` | Giữ trạng thái `thread_local`, được gọi từ ba hook |

`AccessKeys::bypasses` nhận `KeyKind` mà `classify` đã tính, nên mỗi phím chỉ được phân
loại một lần. Muốn thêm cách vào chế độ (F10, phím Menu) thì chỉ cần thêm một hàm `on_*`
trong lõi và gọi nó từ hook. Các quy tắc thoát giữ nguyên.

## Giới hạn đã biết

- Chọn menu bằng phím mũi tên sẽ thoát chế độ. Một chữ cái gõ sau đó vẫn đi qua engine,
  nhưng buffer đã được xoá nên chỉ bị ảnh hưởng nếu gõ liền từ hai phím trở lên tạo thành
  dấu, hoặc gõ `w` ở Telex.
- Nếu đọc KeyTips lâu hơn 3 giây giữa hai phím, phím kế tiếp sẽ đi qua engine.
- F10 chưa được coi là cách vào chế độ.

## Checklist kiểm tra thủ công

Bật tiếng Việt và thử cả Telex lẫn VNI.

Excel:

- [ ] Alt, E, S, V, rồi Enter: hộp Paste Special chọn đúng Values.
- [ ] Alt, H, V, V: dán giá trị.
- [ ] Alt, W, F, F: Freeze Panes.
- [ ] Alt, H, O, I: AutoFit Column Width.
- [ ] Sau mỗi chuỗi, bấm Enter, mũi tên hoặc click vào một ô rồi gõ `vieejt` (VNI: `vie6t5`): ra `việt`.

Không cần Office:

- [ ] Notepad: Alt, F, S mở Save As.
- [ ] Paint: Alt, H rồi chữ của một KeyTip trên ribbon.
- [ ] Alt+Shift (nếu đang là hotkey) vẫn chuyển VI/EN.
- [ ] Tap Alt trong một ô nhập liệu không có menu, rồi gõ `xin chaof`: ra `xin chào`.
