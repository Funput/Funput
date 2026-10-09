# Phụ âm đầu mở rộng: `z`, `f`, `w`, `j`

## Trạng thái

Có trong core (`funput-core`) và engine (`funput-engine`), **mặc định tắt**. FFI có
`funput_set_extra_onsets(engine, letters)` với các bit `ONSET_F | ONSET_J | ONSET_W |
ONSET_Z` (`0` = tắt) — setter riêng, không nới `FunputConfig`.

Android dùng setter JNI `nativeSetExtraOnsets(handle, letters)` với contract bit
`f=1`, `j=2`, `w=4`, `z=8`; không đổi chữ ký `nativeConfigure`.

| Nền tảng | Trạng thái |
|---|---|
| macOS | Settings → Cách gõ → "Phụ âm đầu mở rộng": một công tắc + chọn từng chữ; xuất/nhập qua `preferences.extraOnsets` |
| iOS | Cài đặt → "Phụ âm đầu mở rộng": một công tắc + chọn từng chữ; lưu qua App Group, áp dụng cho bàn phím và ô tìm emoji |
| Android | Cài đặt → "Phụ âm đầu mở rộng": một công tắc + chọn từng chữ; lưu qua Preferences DataStore, áp dụng cho bàn phím, phím vật lý và ô tìm emoji |
| Windows | Cài đặt → Cách gõ → "Phụ âm đầu mở rộng": một công tắc + chọn từng chữ; xuất/nhập qua `preferences.extraOnsets`. Link thẳng Rust, không qua FFI — xem [Tích hợp Windows](#tích-hợp-windows) |
| Linux | Cài đặt → Cách gõ → "Phụ âm đầu mở rộng": một công tắc + chọn từng chữ; xuất/nhập qua `preferences.extraOnsets`; áp dụng cho cả Fcitx5 lẫn IBus — xem [Tích hợp Linux](#tích-hợp-linux) |

## Mục tiêu

Tương đương tuỳ chọn "Cho phép phụ âm đầu Z, F, W, J" của UniKey / OpenKey: cho
teencode, từ mượn và một số tên riêng nhận dấu — `zô`, `zui`, `jờ`, `fải`, `wá`,
`Cư Jút`.

Bốn chữ này không có trong chính tả tiếng Việt, và ba trong số đó là phím Telex
(`f` huyền, `j` nặng, `z` xoá dấu, `w` móc/trăng), nên nhận chúng làm phụ âm đầu thì
English restore buông một số từ tiếng Anh (xem [Đánh đổi](#đánh-đổi)). Vì vậy đây là
tuỳ chọn, và khi tắt thì hành vi giống hệt trước đây.

## Bật tuỳ chọn

```rust
use funput_core::{ExtraOnsets, SyllableRules};

engine.update_config(|c| {
    c.syllable_rules = SyllableRules::STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ);
});
```

`ExtraOnsets` là một tập bit: `ZFWJ` là công tắc kiểu UniKey, còn `F`, `J`, `W`, `Z`
bật từng chữ (`ExtraOnsets::F.union(ExtraOnsets::Z)`). Dùng thẳng core thì truyền
`ComposeOptions::with_syllable_rules` cho `apply_with`, và hỏi các phương thức của
`SyllableRules` (`is_complete_syllable`, `is_definitely_invalid`, …) thay cho các hàm
tự do cùng tên (vốn luôn xét theo `SyllableRules::STANDARD`).

## Những gì được chấp nhận

Chữ được nhận cư xử **y như một phụ âm đầu bản ngữ** (`v`, `b`…): chỉ phần âm đầu
được nới, vần vẫn phải là vần tiếng Việt, luật âm cuối tắc + thanh giữ nguyên.

| Telex | VNI | Kết quả |
|---|---|---|
| `zoo`, `zoos` | `zo6`, `zo61` | `zô`, `zố` |
| `jowf` | `jo72` | `jờ` |
| `fair` | `fa3i` | `fải` |
| `was` | `wa1` | `wá` |
| `Juts`, `Just` | `Ju1t`, `Jut1` | `Jút` |
| `fwa` | — | `fă` (`w` trễ, như `twa` → `tă`) |
| `fomo` | — | `fôm` (mũ rời, như `tomo` → `tôm`) |

Vẫn là tiếng Anh vì vần/cụm không có trong tiếng Việt: `food`, `wood`, `wear`,
`zebra`, `from`, `jump`, `with`, `file`. Không có cụm phụ âm nào bắt đầu bằng bốn chữ
này (`fl`, `fr`, `wh`, `wr`, `zh`).

## Telex nâng cao

Telex nâng cao **giữ nguyên** `w` → `ư` ở đầu từ (`wa` → `ưa`, `wf` → `ừ`,
`wowcs` → `ước`) — từ tiếng Việt thật luôn thắng. Gõ `w` hai lần để lấy phụ âm `w`
(cơ chế undo sẵn có): `wwas` → `wá`, `WWas` → `Wá`. `z`, `j`, `f` không phải phím tắt
nên dùng như Telex thường.

Khi cách đọc `ư` đi vào ngõ cụt và engine trả về phím gốc, chữ `w` vừa trả về được
đọc là phụ âm, giống Telex thường: `waor` → `wảo`, `wits` → `wít`.

## Đánh đổi

Giống UniKey: một từ tiếng Anh có vần tiếng Việt sẽ được bỏ dấu, **đúng như** từ cùng
vần với phụ âm đầu bản ngữ vẫn bị hôm nay (`vast` → `vát`, `bin10` → `bin`).

- Telex: `fast` → `fát`, `for` → `fỏ`, `fix` → `fĩ`, `fee` → `fê`, `just` → `jút`
  (trùng phím với `Cư Jút`), `war` → `wả`, `wow` → `wơ`, `zoom` → `zôm`. Từ có phụ âm
  đôi mất một chữ như với âm đầu bản ngữ (`ferry` → `fery`, giống `berry` → `bery`).
- VNI: chỉ chạm tới tiếng Anh qua số dính chữ — `win10` → `win`, giống `bin10` →
  `bin`.

Đo trên `/usr/share/dict/words` (235 976 từ), chỉ có Telex bị ảnh hưởng: 638 từ
(0,27 %) đổi — `f` 342, `w` 158, `j` 79, `z` 59. VNI: 0.

## Kiến trúc

- `funput-core/src/validation/rules/`
  - `onsets.rs`: `ExtraOnsets` — tập bit + bảng `SPELLINGS` (mỗi dòng một âm đầu).
  - `mod.rs`: `SyllableRules` — chính tả mà mọi kiểm tra âm tiết đọc theo; phương
    thức `is_valid`, `is_complete_syllable`, `is_reopenable_syllable`,
    `is_definitely_invalid(_in)`. Các hàm tự do ở gốc crate gọi
    `SyllableRules::STANDARD`, nên `charset`, `funput-suggestions`, `funput-cli
    coverage` không đổi.
- `validation/parse/onset.rs::match_onset` hỏi `ExtraOnsets::admits` **sau cùng**,
  sau âm đầu bản ngữ, cụm tên Tây Nguyên và `qu`/`gi`; tập rỗng trả lời bằng một phép
  so sánh byte. Đây là cổng âm đầu duy nhất: `invalid_onset` là toàn bộ phán quyết.
- `ComposeOptions` (`options/compose.rs`) mang `SyllableRules` xuống mọi bước soạn
  (dấu, mũ/móc, `w` trễ, mũ rời, cổng kiểm tra chính tả). Bộ phân loại phím
  (`input_method/`) không đổi.
- Engine: `EngineConfig.syllable_rules` (mặc định `STANDARD`), dựng `ComposeOptions`
  ở một chỗ (`compose_options()`), và dùng cùng `SyllableRules` cho eager restore
  (`is_dead_end`), restore ở ranh giới từ (`should_restore`) và `adopt`.

`EngineConfig` giữ cả `SyllableRules` chứ không chỉ `ExtraOnsets`: một nới lỏng chính
tả sau này là một trường mới của `SyllableRules`, không phải sửa engine.

## Thêm một âm đầu mới

1. Thêm hằng vào `ExtraOnsets` và một dòng vào `SPELLINGS`, kèm ví dụ thật. Âm nhiều
   chữ (vd `dz`) thắng tiền tố bản ngữ (`d`) vì `match_onset` thử tiền tố dài nhất
   trước.
2. Thêm test vào `validation/rules/tests.rs`, `tests/spellcheck_corpus.rs` (core) và
   `tests/restore/extra_onsets.rs` (engine), cùng một từ tiếng Anh mà âm đó có thể
   làm hỏng.
3. Chạy differential bên dưới.

## Acceptance

- **Tắt = như cũ:** so với `main`, 0 khác biệt trên Viet74K sinh phím (thanh ở mọi
  vị trí từ nguyên âm đầu, cộng biến thể thay âm đầu bằng `f`/`j`/`w`/`z`: Telex 31 909,
  VNI 32 169 chuỗi), toàn bộ `/usr/share/dict/words` (Telex, VNI) và 283 405 chuỗi Telex
  nâng cao.
- **Bật chỉ chạm từ mở bằng z/f/w/j:** mọi dòng đổi đều bắt đầu bằng một trong bốn
  chữ, và **giống hệt** (sau chữ đầu) kết quả trên `main` khi thay chữ đó bằng `v` —
  tức là đúng như một phụ âm đầu bản ngữ. Một proptest giữ bất biến: phím đầu khác
  bốn chữ này thì bật/tắt cho cùng kết quả ở cả ba bộ gõ.
- Không nới allocation budget (1,5 alloc/phím như `main`, bật hay tắt như nhau).
- Criterion so với `main` (18 nhóm: core `apply`, engine `process_char`): không nhóm
  nào chậm hơn — cổng 3 % của `benchmarks/README.md` giữ nguyên.

Thử tay:

```bash
cargo run -p funput-cli -- dev run -m telex --extra-onsets "zoo jowf fair was Juts food "
cargo run -p funput-cli -- dev run -m vni --extra-onsets "zo6 jo72 fa3i wa1 Ju1t win10 "
```

### Kiểm tra tay trên iOS

- Cài đặt → **Phụ âm đầu mở rộng**: mặc định tắt; bật hiện bốn switch và chọn cả bốn
  chữ. Bỏ chữ cuối cùng tự tắt switch chính; bật lại chọn cả bốn.
- Chỉ giữ `z`, dùng Telex trong Notes: `zoo ` → `zô `, `fair ` → `fair `.
  Bật thêm `f`: `fair ` → `fải `, `food ` vẫn là `food `;
  `fast ` → `fát `, `fasst ` → `fast `.
- Bật cả bốn: Telex `jowf was ` → `jờ wá `; VNI `zo6 jo72 ` → `zô jờ `.
  Telex nâng cao `wa` → `ưa`, `wwas` → `wá`; footer giải thích `w`/`ww` chỉ hiện
  khi chọn Telex nâng cao và bật `w`.
- Đổi lựa chọn trong app, quay lại Notes và mở bàn phím: lựa chọn mới có hiệu lực.
  Đóng/mở app vẫn giữ lựa chọn; tắt switch chính trả về cách gõ trước đây.
- Mở ô tìm emoji khi bật `z`: `zoo` → `zô`; tắt và mở lại ô tìm kiếm:
  `zoo` giữ nguyên. Chuyển sang tiếng Anh giữ nguyên `zoo`, `fair` ở cả hai nơi.
- Kiểm tra Light/Dark, Dynamic Type lớn và VoiceOver đọc tên/trạng thái từng switch.
  Bật Reduce Motion để xác nhận các dòng chữ hiện/ẩn không có animation.
- Kiểm tra trên iOS 26 trở lên và một iOS cũ: control/nền theo Settings hiện tại,
  đầy đủ chức năng và không có lỗi availability.

## Tích hợp Android

- `ExtraOnsetLetters` lưu chuỗi canonical `zfwj` trong khóa `extra_onsets` của
  Preferences DataStore. Đọc chữ hoa/thường như nhau, bỏ chữ lạ và gộp chữ trùng;
  khóa thiếu hoặc rỗng là tắt, không cần migration.
- Bật công tắc chính chọn cả bốn; tắt xóa toàn bộ. Mỗi dòng thêm/bỏ đúng một chữ,
  bỏ chữ cuối tự tắt công tắc chính. Ghi bằng `DataStore.edit` trên giá trị mới
  nhất; UI chỉ hiển thị snapshot đã lưu và báo lỗi nếu ghi thất bại.
- `SmartCompositionPreferences` mang lựa chọn theo flow hiện có tới
  `ImeSettingsController`. Snapshot đầy đủ giữ lựa chọn qua các lần đổi kiểu gõ
  và kiểu đặt dấu, tiếp tục để `autoCapitalize = false` phía engine.
- `NativeVietnameseEngine.configure` gọi `nativeConfigure` rồi
  `nativeSetExtraOnsets`. Kotlin và Rust ánh xạ từng chữ qua contract JNI
  `f=1`, `j=2`, `w=4`, `z=8`; không ép kiểu model sang bitset Rust.
- Setter dùng `update_config`, chỉ sửa extra onsets và giữ tùy chọn khác.
  Bit lạ bị bỏ qua; handle đã đóng hoặc không hợp lệ là no-op.
- IME nhận thay đổi qua flow mà không cần restart app. Khi bắt đầu tìm emoji,
  `LocalTextComposer.reset()` sao chép snapshot mới từ engine của document.
  Bàn phím vật lý dùng cùng session của document.
- UI dùng FunputUI hiện có, gồm theme và glass fallback trên API 26 trở lên;
  motion tôn trọng thiết lập tắt animation của Android và mỗi dòng có một switch
  semantics cho TalkBack.

### Kiểm tra tay Android

- Cài đặt → Phụ âm đầu mở rộng: mặc định tắt; bật chọn cả bốn; thử riêng từng chữ,
  bỏ chữ cuối, bật lại và xác nhận mọi lựa chọn vẫn đúng sau khi mở lại app.
- Telex: `zoo` → `zô`, `fair` → `fải`, `jowf` → `jờ`, `was` → `wá` khi bật chữ
  tương ứng; `food` giữ nguyên. Khi bật `f`, `fast` → `fát`, `fasst` → `fast`.
- VNI: `zo6` → `zô`, `jo72` → `jờ`. Telex nâng cao: `wa` → `ưa`, khi bật `w`
  thì `wwas` → `wá`; chỉ lúc đó section hiển thị hướng dẫn `w`/`ww`.
- Tắt mọi chữ và xác nhận các từ mở bằng chữ đó trở về hành vi cũ.
- Đổi lựa chọn rồi mở lại bàn phím, bắt đầu tìm emoji, đổi kiểu gõ; thử bàn phím
  vật lý và chuyển VI/EN. Không cần khởi động lại app chủ.
- Kiểm tra Light/Dark, font lớn, TalkBack, animation tắt, API 26 và thiết bị mới
  có glass; trạng thái và nội dung từng switch phải đọc được.

Test hồi quy Kotlin, Compose, JNI instrumented và Rust được bổ sung cho tích
hợp này. Build/compile source test có thể chạy độc lập; không cần chạy test để
dựng APK. Xem lệnh build trong README Android.

## Tích hợp Windows

Windows link thẳng Rust nên không qua FFI. Ba tầng, mỗi tầng một việc:

- **`funput-config`** — `ExtraOnsetLetters` (cùng tên với macOS) là tập các
  `OnsetLetter` (`Z`, `F`, `W`, `J`), lưu trong `settings.json` ở trường `extraOnsets`
  và xuất/nhập ở `preferences.extraOnsets`, cả hai cùng dạng chữ viết ra (`"zj"`).
  Bit bên trong là private, không bao giờ ghi ra đĩa, nên không dính tới bit của core
  hay bit wire `ONSET_*` của FFI. Thiếu trường = không chữ nào (file cũ giữ hành vi
  cũ); nhập file không có trường thì giữ lựa chọn cục bộ; chữ lạ bị bỏ qua. Thêm một
  chữ mới (vd `dz`) là thêm một biến thể `OnsetLetter` — các `match` đầy đủ sẽ chỉ ra
  mọi chỗ cần sửa, kể cả ví dụ hiển thị trên UI.
- **`funput-desktop`** — `ShellState::sync_engine_config` dựng
  `SyllableRules::STANDARD.with_extra_onsets(..)` từ settings, nên thao tác trong Cài
  đặt, nhập cấu hình và `reload_settings` (process nền đọc lại file do process Cài đặt
  ghi) đều tới engine bằng một đường. `set_extra_onsets` nhận cả tập.
- **`platforms/windows`** — mục `ui/pages/typing/extra_onsets/` (Slint) nói chuyện với
  Rust qua global `ExtraOnsetsState`, không luồn prop qua `SettingsWindow` →
  `SettingsContent` → `TypingPage`. Rust là nguồn sự thật
  (`src/ui/settings_callbacks/extra_onsets.rs`): mỗi cú bấm ghi tập mới rồi đẩy lại
  trạng thái, nên bỏ chữ cuối thì công tắc tự tắt mà UI không phải giữ luật riêng. Ô
  "Bung thành" của gõ tắt (`FieldComposer`) dựng lại từ toàn bộ `Settings`, nên gõ
  `zô` được y như ngoài hệ thống.

### Kiểm tra tay Windows

Chạy với cấu hình tạm để không đụng cấu hình thật:

```powershell
$env:FUNPUT_CONFIG = "$env:TEMP\funput-test\settings.json"
cargo run                 # process nền: hook + tray
cargo run -- --settings   # cửa sổ Cài đặt
```

- Cài đặt → Cách gõ → Phụ âm đầu mở rộng: mặc định tắt; bật → cả bốn ô được chọn;
  chỉ giữ `z`; bỏ ô cuối → công tắc tắt và danh sách thu lại. Mở lại Cài đặt giữ lựa chọn.
- Notepad, Telex: `zoo ` → `zô `, `fair ` → `fair `; chọn thêm `f`: `fair ` → `fải `,
  `food ` giữ nguyên, `fasst ` → `fast `.
- VNI: `zo6 jo72 ` → `zô jờ `. Telex nâng cao + `w`: `wa` → `ưa`, `wwas` → `wá`; dòng
  gợi ý `ww` chỉ hiện khi đang dùng Telex nâng cao và có chọn `w`.
- Gõ tắt → ô "Bung thành": gõ `zoo` ra `zô` khi đã chọn `z`.
- Dữ liệu → Xuất rồi Nhập trên một cấu hình khác: giữ `"extraOnsets"`; nhập file cũ
  không có trường thì lựa chọn hiện tại giữ nguyên.
- Sáng/Tối, đổi màu nhấn của Windows, Narrator đọc đúng tên và trạng thái từng ô.

## Tích hợp Linux

Hai process dùng chung `~/.config/Funput/settings.json`: app Cài đặt GTK ghi file, còn addon
Fcitx5/IBus đọc file (theo dõi bằng inotify, kiểm tra lại mtime khi focus-in) rồi đẩy vào
engine qua C ABI. Có ba tầng, mỗi tầng làm một việc:

- **App Cài đặt (`settings-gtk`, Rust)**: dùng lại `funput_config::ExtraOnsetLetters` /
  `OnsetLetter`, cùng kiểu với Windows, nên trường `extraOnsets` (`"zj"`) được đọc và ghi theo
  một cách duy nhất. `Settings` của app có trường này vì `save()` ghi đè cả file. Xuất/nhập
  dùng `preferences.extraOnsets`: thiếu trường thì giữ lựa chọn cục bộ, chữ lạ thì bỏ qua.
  UI nằm ở `settings_window/typing/extra_onsets.rs`: một `AdwExpanderRow` có công tắc riêng,
  mỗi chữ một dòng gồm checkbox, keycap và ví dụ (`OnsetLetter::examples()`, dùng chung với
  Windows). Quy tắc của mỗi cú bấm nằm trong hàm thuần `change::next` và có test riêng: bật thì
  chọn cả bốn, tắt thì xoá hết, bỏ chữ cuối thì công tắc tự tắt. Mỗi lần bấm, tập mới được lưu
  rồi `show()` đặt lại mọi widget. `show()` ghi nhận tập mới trước khi đụng tới widget, nên các
  notify do chính nó gây ra không lưu gì, và không phụ thuộc việc ghi file có thành công hay
  không. Handler chỉ giữ `Weak`, còn tham chiếu mạnh gắn vào group nên không tạo chu trình.
  Mỗi lần mở trang, section đọc lại file để gợi ý `ww` theo đúng phương thức hiện tại.
- **Addon, tầng settings (`common/settings/`, C++)**: `onsets/letters.h` là bản C++ của
  `ExtraOnsetLetters`, với bit private, `id()` / `fromId()` và cùng các luật đọc. `io.cpp` đọc
  `extraOnsets`; khóa thiếu hoặc sai kiểu được coi là không chữ nào. Addon **không bao giờ ghi**
  khóa này: lúc lưu (khi bật/tắt VI/EN), phần merge giữ nguyên giá trị trong file, nên không thể
  ghi đè một lựa chọn vừa lưu từ Cài đặt.
- **Addon, tầng engine (`common/ffi/`, `compose/`)**: `ffi/onsets.h` ánh xạ từng chữ sang
  `ONSET_*` bằng `switch` liệt kê đủ trường hợp; `common/CMakeLists.txt` bật
  `-Werror=switch`, nên thiếu một chữ là build lỗi. `Handle::setExtraOnsets` gọi
  `funput_set_extra_onsets`, còn `Composer::applySettings()` gọi nó ngay sau `configure`. Đây
  là đường đi duy nhất cho lúc khởi động, khi watcher nạp lại và khi focus-in, nên cả hai shell
  đều không phải sửa.

Thêm một chữ mới: thêm biến thể vào `OnsetLetter` (funput-config) và `OnsetLetter` cùng
`kAllOnsetLetters` (C++). Trình biên dịch sẽ chỉ ra những chỗ còn lại: `match` trong Rust (ví
dụ, ký hiệu) và `switch` trong C++ (ký hiệu, bit wire).

### Kiểm tra tay Linux

Build và cài gói (máy dùng IBus thì `FUNPUT_FRAMEWORK=ibus`), rồi `ibus restart` hoặc
`fcitx5 -r`:

```bash
FUNPUT_FRAMEWORK=ibus platforms/linux/build.sh
```

- Cài đặt → Cách gõ → Phụ âm đầu mở rộng: mặc định tắt; bật → cả bốn ô được chọn; chỉ giữ
  `z`; bỏ ô cuối → công tắc tắt và danh sách thu lại. Đóng/mở lại Cài đặt vẫn giữ lựa chọn.
- Text Editor/gedit, Telex: `zoo ` → `zô `, `fair ` → `fair `; chọn thêm `f`: `fair ` →
  `fải `, `food ` giữ nguyên, `fasst ` → `fast `. Đổi lựa chọn có hiệu lực ngay, không cần
  khởi động lại bộ gõ.
- VNI: `zo6 jo72 ` → `zô jờ `. Telex nâng cao + `w`: `wa` → `ưa`, `wwas` → `wá`; dòng gợi ý
  `ww` chỉ hiện khi đang dùng Telex nâng cao và có chọn `w` (đổi phương thức trên cùng trang
  thì dòng này ẩn/hiện theo).
- Bật/tắt VI/EN bằng phím tắt: `settings.json` vẫn giữ `"extraOnsets"`.
- Xuất rồi Nhập: giữ `"extraOnsets"`; nhập file cũ không có trường thì lựa chọn hiện tại giữ
  nguyên.
- Sáng/Tối; Orca đọc tên và trạng thái từng ô ("Phụ âm đầu z, ví dụ zô, zui").
