# Phụ âm đầu mở rộng: `z`, `f`, `w`, `j`

## Trạng thái

Có trong core (`funput-core`) và engine (`funput-engine`), **mặc định tắt**. Chưa
nền tảng nào có công tắc trong phần cài đặt; khi tích hợp, mỗi nền tảng chỉ cần đẩy
một giá trị `SyllableRules` vào `EngineConfig` (FFI/JNI sẽ thêm một setter riêng,
không nới struct `FunputConfig` / chữ ký JNI hiện có).

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
