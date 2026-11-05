# Gõ `e` sau mũ tự thêm của `iê`

> **Trạng thái:** đã chốt. Sửa lỗi, không thêm công tắc. Chưa có code.

Nguồn: issue #582 (Windows), mục "Về cách đặt dấu câu mới".

## Vấn đề

Funput tự thêm mũ cho `iê` khi đặt dấu thanh: `ie` + phụ âm cuối (hoặc `u`) bắt buộc là `iê`, nên `viecj` ra `việc` ([tone_position.rs](../../crates/funput-core/src/orthography/tone_position.rs)). Người quen Unikey gõ **dấu thanh trước, mũ sau** (`viecje`), và phím `e` cuối bị hiểu là **gỡ mũ**:

| Gõ (Telex) | Hiện tại | Mong muốn |
|---|---|---|
| `viecj` | việc | việc |
| `vieecj`, `viecej`, `vieejc` | việc | việc |
| `viecje` | **viẹce** | việc |
| `tiengse` | **tiénge** | tiếng |
| `dienje` | **diẹne** | diện |
| `tienges`, `dongso`, `muonjo`, `xuongso` | tiếng, dống, muộn, xuống | không đổi |

Đo bằng `funput dev run -m telex` (bản release ngày 2026-11-02). Chạy lại trên code mới nhất trước khi sửa.

Nguồn gốc lỗi theo `--steps` của `viecje`: phím `j` ra `việc` (mũ tự thêm), phím `e` gặp `ê` sẵn có nên đi vào nhánh `revert` của [circumflex.rs](../../crates/funput-core/src/composition/intent/kinds/circumflex.rs): gỡ mũ rồi chèn `e` thô, ra `viẹce`. Funput gỡ một cái mũ mà người dùng chưa hề gõ.

Các vần khác không bị vì không có mũ tự thêm: `dongs` ra `dóng`, phím `o` sau đó đội mũ bình thường.

## Quyết định

**Mũ do Funput tự thêm thì phím `e` gõ sau là xác nhận, không phải gỡ.** Phím đó bị nuốt, chữ giữ nguyên.

- Chỉ gỡ mũ khi chính người dùng đã gõ mũ (`ee`) rồi gõ `e` thêm lần nữa, như hiện nay.
- Sau khi xác nhận, phím `e` tiếp theo gỡ mũ như thường (`viecjee` ra `viẹce`), giống người dùng đã tự gõ mũ.
- **Không thêm công tắc** bật/tắt việc tự thêm mũ. Sửa xong thì mọi thói quen (`viecj`, `vieecj`, `viecej`, `viecje`) đều ra `việc`, không ai phải đổi cách gõ. Tắt việc tự thêm mũ còn làm hỏng quy tắc `giefm` → `gièm` / `giemf` → `giềm` dựa vào chỗ gõ dấu, đã ghi ở `reposition_existing_tone`.
- **Không chuyển sang sửa lỗi gõ** như issue gợi ý. Quy tắc `iê` là chính tả chắc chắn; sửa lỗi gõ dựa trên xác suất, chạy ở cuối từ, và có thể sai.
- `ngwuoif` → `người`: đã chạy với mọi thứ tự của `w` (`ngwuoif`, `nguoiwf`, `nguowif`, `nguwoif`, `nguoifw`). Không đổi gì.

## Cách nhận biết mũ tự thêm

Lớp `funput-core` chỉ thấy buffer, không thấy phím thô, nên chỉ nhìn buffer thì không phân biệt được `việc` từ `viecj` với `việc` từ `vieecj`. Hai cách:

1. **Engine quyết định** (đề xuất): engine có phím thô (`Session::keys`). Trước khi đưa phím `e` vào `funput-core`, nếu nguyên âm `ê` mục tiêu là `iê` và phím thô chỉ có **một** `e` (không tính phím `e` đang gõ) cho nguyên âm đó, thì nuốt phím: giữ buffer, chỉ thêm phím vào `keys` để lật từ và English restore vẫn thấy đúng phím đã gõ.
2. **Ghi dấu lúc tự thêm:** `funput-core` báo lại khi đặt dấu thanh đã tự thêm mũ, engine giữ một cờ cho từ đang gõ. Chính xác hơn khi trong từ có nhiều `e`, nhưng phải luồn thêm thông tin qua `ComposeResult`.

Chốt cách nào khi code; yêu cầu là hành vi ở bảng trên. Với VNI, phím mũ là `6`: kiểm tra `viec56`/`tieng16` có cùng lỗi không và áp dụng cùng quy tắc nếu có.

## Ảnh hưởng

- **Lật từ:** dạng phím thô vẫn là mọi phím đã gõ (`viecje`), không bỏ phím `e` bị nuốt.
- **English restore:** từ dùng phím thô để quyết định. `viecje` là một âm tiết tiếng Việt đúng nên không restore.
- **Mở lại sau Backspace (`adopt`):** phím thô được dựng lại từ chữ, không có thông tin mũ tự thêm. Coi mọi `ê` của từ được mở lại là người dùng đã gõ, giữ hành vi gỡ mũ như hiện nay.
- **Mọi nền tảng:** sửa trong engine nên Windows, macOS, Linux, iOS, Android cùng được.

## Hướng dẫn

Thêm vào trang Telex trên website: Funput tự thêm mũ cho `iê` khi có phụ âm cuối (`viecj` ra `việc`, `tiengs` ra `tiếng`), và gõ thêm `e` như thói quen cũ cũng ra cùng chữ. Ghi tương tự cho VNI nếu áp dụng.

## Ngoài phạm vi

Funput chỉ tự thêm mũ cho `iê`; `uô` thì không (`muonj` ra `muonj`, phải gõ `muoonj` hoặc `muonjo`). Hai vần đang không nhất quán. Mở rộng việc tự thêm mũ là thay đổi cách gõ, bàn riêng.

## Kiểm thử

- `crates/funput-engine/tests/words/`: các dòng của bảng Vấn đề, cả chữ hoa (`VIECJE`, `Viecje`); `viecjee` → `viẹce`; `vieecje` → `viẹce` (mũ do người dùng gõ, `e` thứ ba gỡ mũ như cũ); `giefm`/`giemf` không đổi.
- Lật từ trên `viecje` ra phím thô đủ `viecje`.
- Differential test của engine (so 70k chuỗi phím với worktree gốc): chỉ được đổi các chuỗi có phím `e` sau mũ tự thêm của `iê`. Liệt kê các chuỗi bị đổi để duyệt, không tự sửa cho hết khác biệt.
- `tests/alloc_budget/`: không tăng cấp phát trên đường gõ thường.
