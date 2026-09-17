# funput-desktop

Logic **thuần, không phụ thuộc OS** cho các shell kiểu **"hook + inject"** — loại chặn phím toàn
cục rồi tự gõ chữ ra, như Windows `WH_KEYBOARD_LL`. Crate này nằm giữa hai phần phụ thuộc nền tảng:

```
[host đọc phím thô] → classify → ShellState + engine → plan_inject → [host inject]
      tuỳ nền tảng     thuần           thuần             thuần        tuỳ nền tảng
```

Mọi quyết định **"phím này nghĩa là gì"**, **"cần xoá/gõ gì"** và **"app này gõ tiếng gì"** sống ở
đây nên unit-test được mà không cần một API hệ điều hành nào. Cùng mô hình với `result_bytes` của
`funput-term`, nhưng sinh ra một *plan trung tính* thay vì byte terminal.

> Chỉ shell **hook + inject** dùng crate này — hiện là **Windows** ([`platforms/windows`](../../platforms/windows)).
> macOS (IMKit marked text) và Linux (Fcitx5/IBus preedit) đi mô hình khác nên **không** link;
> hai shell đó chỉ chạm Rust qua C ABI của [`funput-ffi`](../funput-ffi).

Dù vậy thiết kế ở đây vẫn đi xa hơn cái tên: Linux port nó sang C++ bằng tay và nói rõ như vậy —
[`common/compose/plan.h`](../../platforms/linux/common/compose/plan.h) tự nhận là bản đối chiếu của
`inject.rs`, [`key/event.h`](../../platforms/linux/common/compose/key/event.h) mirror `key.rs`, và
chế độ non-preedit của Linux chính là inject với cùng con số `backspace` từ engine.

## Năm phần

| Module | Việc |
|---|---|
| `key` | `classify(&KeyEvent) -> KeyKind` — một phím nghĩa là gì |
| `inject` | `plan_inject(&ImeResult) -> InjectPlan` — cần xoá bao nhiêu, gõ gì |
| `layout` | `is_foreign_layout(u32)` — bàn phím đang focus có gõ được tiếng Việt không |
| `retone` | `CommittedTail` — bản bóng của chữ đã gõ, thay cho tài liệu không đọc được |
| `shell` | `ShellState` — trạng thái mà bốn thứ trên được quyết định trên đó |

### `key` — phím nghĩa là gì

```rust
pub struct Mods { pub ctrl: bool, pub alt: bool, pub win: bool, pub shift: bool }
// is_shortcut() = ctrl || alt || win  (Shift KHÔNG tính — vẫn là gõ thường)

pub struct KeyEvent {
    pub mods: Mods,
    pub ch: Option<char>,     // ký tự phím tạo ra (Windows: từ ToUnicodeEx), nếu có
    pub is_backspace: bool,
    pub is_navigation: bool,  // mũi tên, Home/End, PageUp/Down, Esc, Delete, F-keys, Enter, Tab
    pub source: KeySource,    // Standard hay Numpad
}

pub enum KeyKind {
    Compose(char, KeySource), // nạp cho engine (kể cả space/dấu câu — engine tự quyết ranh giới từ)
    Backspace,                // gọi ShellState::on_backspace
    Flush,                    // commit/clear composition rồi để phím đi qua
    PassThrough,              // phím vô nghĩa — bỏ qua, giữ nguyên composition
}
```

Thứ tự quyết định: phím tắt (ctrl/alt/win) → `Flush`; Backspace → `Backspace`; navigation →
`Flush`; có `ch` → `Compose`; còn lại → `PassThrough`. Toggle VI/EN do host xử lý **trước**
`classify`, vì tổ hợp toggle cấu hình được và tuỳ host.

`source` đi kèm suốt đường tới engine vì phím số ở numpad phải giữ nguyên là con số, không được
làm phím dấu/phím hình của VNI.

### `inject` — cần xoá gì, gõ gì

```rust
pub struct InjectPlan {
    pub backspaces: usize,  // số ký tự cần xoá lùi
    pub units: Vec<u16>,    // UTF-16 code unit để gõ sau khi xoá
}
// is_noop() = backspaces == 0 && units.is_empty()
```

`Action::None` cho plan rỗng: để phím tới app nguyên vẹn. `Action::Send` / `Action::Restore` xoá
`backspace` ký tự rồi gõ `output`, và host nuốt phím gốc.

`units` là **UTF-16** vì đó là thứ Windows `SendInput` (`KEYEVENTF_UNICODE`) nhận. Chữ Việt NFC nằm
trong BMP nên mỗi ký tự một unit, nhưng surrogate pair vẫn đúng cho text khác. Đây là chỗ duy nhất
trong crate mang hình dạng của một OS cụ thể — cùng với `is_foreign_layout`, nhận một `HKL` của
Windows dưới dạng `u32` thuần để file đó không phải import gì của OS.

### `retone` — bản bóng của chữ đã gõ

`Engine::adopt` nhận cái từ mà caret đang đứng cuối rồi biến nó thành composition sống lại, nên
`phủ` ␣ ⌫ `s` ra `phú`. Android hỏi editor lấy từ đó; shell hook không có editor nào để hỏi.

Nó không cần. Khi tiếng Việt đang bật, mọi ký tự tới được app đều là ký tự engine này soạn ra hoặc
cho đi qua, nên nhớ vài ký tự cuối là đủ trả lời câu duy nhất `adopt` hỏi.

**Bất biến:** `tail` cộng buffer composition luôn là **hậu tố** của văn bản trước caret. Nhớ ít hơn
sự thật thì vô hại — tính năng đơn giản là không chạy. Nhớ nhiều hơn sẽ đưa cho `adopt` một từ
không có thật và để phím kế tiếp xoá mất ký tự Funput chưa từng gõ. Nên mọi sự kiện shell không mô
hình hoá được — click chuột, phím di chuyển caret, đổi focus, lật VI/EN — đều phải `clear`.

### `shell` — trạng thái

`ShellState` giữ tất cả những gì callback của nền tảng đọc và sửa: engine, bản bóng `CommittedTail`,
settings đã persist ([`funput-config`](../funput-config)), và phần bookkeeping VI/EN theo app. Mỗi
method nhận `&mut self`; biến process-global và mutex của nó ở lại phía nền tảng, vì một hook
callback là hàm `extern "system"` trần không có chỗ mang con trỏ — đó là chuyện của Windows, không
phải của shell.

Để state ngoài global chính là thứ làm nó test được. Các luật dưới đây từng nằm trong một
`OnceLock<Mutex<_>>` và không unit test nào với tới nổi:

| File | Luật |
|---|---|
| `config` | Đẩy settings vào engine, đọc/ghi đĩa, `effective_enabled` |
| `options` | Từng ô setting mà UI Cài đặt bật/tắt |
| `apps` | App đang focus, bộ nhớ VI/EN theo app, và công tắc tắt nó |
| `shortcuts` | Bảng gõ tắt, kể cả những dòng người dùng đang gõ nửa vời |
| `compose` | Những gì keyboard hook gọi trên mỗi phím — đường nóng, không I/O |

Ba luật đáng đọc trước khi sửa:

**Surface nào toggle thì surface đó quyết định phạm vi.** Hotkey được bấm bên trong app nó nhắm tới
nên nó ghim app đó. Tray flyout và cửa sổ Cài đặt là cửa sổ của chính Funput, mở từ tray, không có
app nào trước mặt — nên chúng dịch mặc định toàn cục và không ghim gì. Hai cái sau từng đỗ lựa chọn
rồi gắn vào app nhận focus kế tiếp, và vì flyout giành foreground nên cái nó ghim là taskbar.

**Bộ nhớ theo app tắt được**, và tắt nghĩa là bỏ qua chứ không phải quên: map ở lại trên đĩa nên bật
lại là các app đã ghim quay về.

**Treo theo layout không phải setting.** `last_layout`, `layout_suspended`, `layout_override` chỉ
sống trong session; `effective_enabled()` là `settings.enabled && !layout_suspended`. Đổi layout xảy
ra quá thường xuyên để tốn một lượt ghi file, và bẻ `settings.enabled` sẽ làm `reload_settings()`
tưởng một session đang bị treo là một lần lật VI/EN từ process khác.

## Host nối dây thế nào (ví dụ Windows)

```
WH_KEYBOARD_LL callback
  ├─ tổ hợp toggle? → state.toggle_enabled_hotkey(), nuốt phím       (trước classify)
  ├─ tổ hợp flip?   → plan_inject(state.flip_composing()), nuốt phím
  ├─ !state.hook_active()? → để phím đi qua, không classify
  └─ dựng KeyEvent (mods, ToUnicodeEx → ch, backspace/navigation, numpad?)
       match classify(&ev):
         Compose(c, src) → plan_inject(state.process_key(c, src)) → SendInput; nuốt phím
         Backspace       → state.on_backspace(); để Backspace vật lý đi qua (app tự xoá)
         Flush           → state.clear(); để phím đi qua
         PassThrough     → để phím đi qua

EVENT_SYSTEM_FOREGROUND → reload_settings → note_foreground → apply_for_app → apply_for_layout
```

`apply_for_layout` đi **sau** `apply_for_app` vì nó là luật chứ không phải hồi tưởng: một app được
ghim tiếng Việt vẫn phải im khi caret đang ở trong một IME tiếng Nhật.

`InjectPlan` chỉ **mô tả** việc cần làm; *cách* gửi — `SendInput` với `INJECT_TAG` để chính hook bỏ
qua, tránh đệ quy — là việc của host. Ghi settings xuống đĩa cũng vậy: `toggle_enabled_hotkey` chỉ
đổi trong bộ nhớ và để host gọi `save_settings` ở chỗ nó chịu được việc block, vì Windows âm thầm
tháo hook nào chạy quá `LowLevelHooksTimeout`.

## Phạm vi & ranh giới

| funput-desktop | Host (`platforms/windows`) |
|---|---|
| `classify` — phím → ý nghĩa | Đọc phím thô (LL hook), map keycode → `KeyEvent` |
| `plan_inject` — `ImeResult` → `InjectPlan` | Gửi Backspace/Unicode (`SendInput`), chống đệ quy |
| `is_foreign_layout` — layout này gõ được không | Hỏi Windows layout nào đang focus (`GetKeyboardLayout`) |
| `ShellState` — settings, bộ nhớ theo app, gõ tắt | Global + mutex, tray, cửa sổ Slint, singleton |
| Pure, OS-neutral, unit-test được | Mọi thứ chạm Win32 |

## Phụ thuộc & ai gọi

- [`funput-engine`](../funput-engine) — `Action`, `ImeResult`, `Engine`.
- [`funput-config`](../funput-config) — `ShellState` cầm settings đã persist và đẩy chúng vào engine.
- [`funput-core`](../funput-core) — `InputMethod` / `ToneStyle`, kiểu mà engine nhận nhưng không re-export.

Không một OS API nào. Consumer: [`platforms/windows`](../../platforms/windows), và mở cho bất kỳ
host hook+inject nào khác.

## Tests

```bash
cargo test   -p funput-desktop
cargo clippy -p funput-desktop --all-targets -- -D warnings
```

`key` và `inject` test ngay trong module. `retone` có `src/retone/tests.rs` cho bất biến của bản
bóng, cộng `tests/retone.rs` chạy nguyên vòng engine. `src/shell/tests.rs` là phần lớn nhất — app
nào được tiếng Việt, surface nào được ghim, khi nào composition bị bỏ.

File nguồn chịu ngân sách **150 dòng** của [`scripts/check-loc.sh`](../../scripts/check-loc.sh);
`tests.rs` và mọi thứ dưới `src/**/tests/` được miễn.
