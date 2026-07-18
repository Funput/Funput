# Telex nâng cao (Full Telex)

## Trạng thái

V5 đã hiện thực core/engine và C FFI/JNI. macOS và Windows expose UI, persistence
và selection; Linux sẽ tích hợp sau. Đây là tính năng desktop: iOS/Android không
expose hoặc persist mode này, còn wire ID `2` trong FFI/JNI vẫn được giữ dormant.
Tên hiển thị là **Telex nâng cao**; tên kỹ thuật là `TelexAdvanced` và giá trị cấu
hình ổn định là `telex_advanced`.

## Mục tiêu

Telex nâng cao kế thừa toàn bộ hành vi của Telex hiện tại và chỉ bổ sung các phím
tắt của Full Telex. Đây là mode riêng do người dùng chủ động chọn; Telex hiện tại
không đổi behavior.

Hai mode dùng chung một composition pipeline. Không sao chép validation, đặt dấu,
Flip, restore hay các resolver đã có.

## Hành vi kế thừa

Telex nâng cao phải giữ nguyên toàn bộ tính năng Telex V1–V3:

- Telex chuẩn: tone, `z`, `aa/ee/oo`, `aw/ow/uw`, `dd`.
- Free-position `aa/ee/oo`, tone convergence và non-adjacent revert.
- Deferred `w`, chuẩn hóa `ươ/ưa/ưu` và Vietnamese-first sau onset.
- Hội tụ bounded multi-intent `w + dd`.
- Traditional/Modern tone placement.
- Spell check, smart/eager restore, raw keys, Flip và sticky Latin.
- Giữ case, auto-capitalize, shortcut và xử lý word boundary hiện tại.

Mọi regression test của Telex hiện tại phải chạy lại cho Telex nâng cao.

## Phần mở rộng Full Telex

| Phím | Kết quả | Ví dụ |
|---|---|---|
| `[` | `ư` | `t[` → `tư` |
| `]` | `ơ` | `m]` → `mơ` |
| `w` ở đầu từ hoặc đứng riêng | `ư` | `w` → `ư`, `wf` → `ừ` |

Các phím mới phải kết hợp với pipeline hiện tại:

```text
tr[]ngf  → trường
ng[]if   → người
wngf     → ừng
```

`[` và `]` tạo trực tiếp vowel đã có shape; chúng không phải pending modifier.
Tone nhập sau đó vẫn dùng quy tắc Traditional/Modern hiện tại.

`W` ở đầu từ tạo `Ư`. Auto-capitalize áp dụng như với mọi chữ cái đầu từ. V5 chưa
gán behavior cho `{` và `}`; hai phím này tiếp tục là ký tự literal cho đến khi có
quyết định sản phẩm riêng.

Nhấn `w` lần hai dùng semantics revert hiện có: `w → ư`, `ww → w`. Raw keystrokes
phải luôn được giữ để Flip khôi phục chính xác input ban đầu.

## So sánh hai mode

| Input | Telex | Telex nâng cao |
|---|---|---|
| `w` | `w` | `ư` |
| `wf` | `wf` | `ừ` |
| `t[` | `t[` | `tư` |
| `m]` | `m]` | `mơ` |
| `tr[]ngf` | literal/restore hiện tại | `trường` |

Telex hiện tại tiếp tục là lựa chọn an toàn hơn khi nhập tiếng Anh hoặc code. Trong
Telex nâng cao, Flip là escape chính thức cho collision; người dùng cũng có thể đổi
mode hoặc tắt bộ gõ để nhập nhiều ký tự literal.

## Kiến trúc

### Rust core

- Bổ sung profile/method `TelexAdvanced`, dùng lại classifier và transform của
  `Telex`.
- Chỉ nhánh khác biệt cho leading `w`, `[` và `]`.
- Không thêm dictionary, replay toàn bộ raw keys hoặc arbitrary intent list.
- Fast path của Telex/VNI hiện tại không được đi qua nhánh Full Telex mới.

### Engine

Engine vẫn là nơi quản lý raw keys, Flip, restore và diff. Cần thay đổi engine vì
`[` và `]` hiện là ASCII punctuation/word boundary. Chỉ trong `TelexAdvanced`, hai
phím này phải được chuyển vào composition pipeline; trong Telex/VNI chúng vẫn là
boundary như trước.

Việc đổi mode phải clear composition đang dở để không trộn semantics của hai mode.

### FFI và JNI

Mode mới dùng mapping ổn định qua biên platform:

- Rust: thêm `InputMethod::TelexAdvanced` theo thay đổi API có phối hợp.
- C FFI: giữ `0 = Telex`, `1 = VNI`, thêm `2 = TelexAdvanced`; macOS dùng ID `2`.
- JNI: giữ cùng mapping `0/1/2`, nhưng ID `2` chưa được Android expose.
- Giá trị không nhận diện vẫn fallback về Telex để tương thích ngược.

Không cần thêm state riêng nếu enum `InputMethod` vẫn vừa layout hiện tại; phải kiểm
chứng lại `size_of::<Engine>()` và ABI tests.

### Platform

Platform không tự hiện thực quy tắc gõ. macOS và Windows hiển thị **Telex**,
**Telex nâng cao**, **VNI** theo thứ tự đó và persist `telex_advanced`. macOS truyền
mode qua C FFI; Windows dùng trực tiếp Rust engine. Linux sẽ tích hợp sau.

iOS/Android không thêm UI, persistence hoặc wiring cho mode này. Khi một consumer
không hỗ trợ đọc `telex_advanced`, nó phải giữ input method hiện tại thay vì ép về
một method khác.

Config interchange mở rộng `preferences.inputMethod` thành:

```text
"telex" | "telex_advanced" | "vni"
```

## Acceptance

- Telex và VNI không thay đổi output, diff, restore hoặc boundary behavior.
- Telex nâng cao pass toàn bộ suite Telex V1–V3 và matrix Full Telex ở trên.
- Flip hai chiều và sticky Latin hoạt động với leading `w`, `[` và `]`.
- Word boundary không mang composition sang từ kế tiếp.
- Core, engine và FFI common-path không chậm quá 3% so với baseline trước V5.
- Viet74K đạt 100% với cả Telex canonical và Full Telex shortcut encoding.
- Không nới allocation budget hiện tại; kiểm tra lại kích thước Engine và ABI.
- Core, engine, C FFI và JNI dùng cùng behavior và wire mapping; macOS và Windows
  đã tích hợp selection, Linux làm sau và mobile giữ mapping dormant.

## Ngoài phạm vi V5

- Deferred tone hoặc arbitrary free-order của V4.
- `{` → `Ư`, `}` → `Ơ`.
- Dictionary, prediction, autocorrect hoặc ambiguity scoring.
- Thay đổi mặc định của người dùng Telex hiện tại.

## Tham chiếu tương thích

- [UniKey Manual — bảng phím Telex đầy đủ](https://www.unikey.org/support/ukmanual.html)
- [UniKey 4.6 RC2 — phân biệt Simple Telex](https://www.unikey.org/version_4_6_2.html)
