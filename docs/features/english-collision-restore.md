# Khôi phục từ tiếng Anh bị va chạm (collision restore)

## Trạng thái

**Đã hiện thực** cho một luật: `w` được giữ lại trong Telex thường. Nguồn:
[issue #661](https://github.com/Funput/Funput/issues/661). Telex gõ `two` ra `tơ`.

| Thành phần | Ở đâu |
|---|---|
| Logic | `crates/funput-engine/src/compose/collision/` (`mod.rs`, `class.rs`), nối vào `compose/boundary/mod.rs` (`listed_restore`) |
| Luật ở core | `funput_core::telex_defers_w` (`crates/funput-core/src/input_method/telex/mod.rs`), đúng luật mà phần ghép chữ dùng |
| Công tắc | `EngineConfig.restore_collisions` (mặc định bật, đi chung `smart_restore`, chỉ Rust) |
| Bảng từ | `crates/funput-engine/data/collision/<luật>/words.txt` và `overrides.txt` |
| Bộ sinh | `funput-lexicon-tool collision` (`crates/funput-lexicon-tool/src/collision/`), dùng chung `src/wordlist/` với `doubled` |
| Test | `compose/collision/tests.rs`, `tests/restore/collision/`, `tests/alloc_budget/collision.rs`, `funput-lexicon-tool/tests/collision_table.rs` |

## 1. Vấn đề

Ở ranh giới từ, English restore tin vào phép kiểm âm tiết. Từ nào ghép thành âm tiết tiếng Việt hoàn
chỉnh thì giữ, không thì trả phím thô. Cách này đúng với hầu hết mọi từ.

*Va chạm* là ngoại lệ do một luật ghép chữ tạo ra. Luật đọc phím của một từ tiếng Anh phổ biến thành
dấu, và kết quả là một âm tiết có thật nhưng không phải điều người gõ muốn. Telex thường giữ `w` gõ sau
phụ âm đầu lại cho nguyên âm phía sau (`cwon` → `cơn`), nên `two` ra `tơ`. `tơ` hợp lệ, nên English
restore không đụng tới.

Đo bằng `funput dev coverage --english` trên 25 979 từ của `en.tsv`: 577 từ bị giữ dạng tiếng Việt khi
bật eager restore. Chỉ 9 từ trong số đó đi qua luật `w` được giữ lại:

| Gõ | Trước | Sau |
|---|---|---|
| `two` | `tơ` | `two` |
| `swam`, `swan`, `swans` | `săm`, `săn`, `sắn` | giữ nguyên phím |
| `swaps`, `swarm`, `swarms` | `sắp`, `sẳm`, `sắm` | giữ nguyên phím |
| `sworn`, `swung` | `sởn`, `sưng` | giữ nguyên phím |

568 từ còn lại (`how` → `hơ`, `too` → `tô`, `is` → `í`) là sự mơ hồ vốn có của Telex: cả hai cách
đọc đều là từ thật, và không có luật nào để phân biệt. Chúng nằm ngoài phạm vi tài liệu này.

## 2. Thiết kế

### 2.1. Một lớp cho mỗi luật

Mỗi luật ghép chữ gây va chạm là một biến thể của `enum Collision` (`class.rs`, công khai là
`funput_engine::Collision`). Đây là định nghĩa duy nhất: bộ sinh đọc cùng cổng và cùng tên từ đây,
nên hai bên không thể lệch nhau. Biến thể sở hữu:

- **tên** (`name`): thư mục dữ liệu `data/collision/<tên>/`;
- **cổng** (`method` + `shapes`): kiểu gõ mà luật chạy, và cách nhận ra từ chuỗi phím thô rằng luật
  đã ghép từ này. Với `DeferredW`: `Telex` và `telex_defers_w(keys)`;
- **bảng từ** (`words`): danh sách tiếng Anh nhúng sẵn, tìm nhị phân tại chỗ bằng `SortedLines`.

Ranh giới từ chỉ trả phím thô khi **cả hai** đều đúng. Vì vậy chuỗi ngoài bảng không bao giờ đổi:
`tow` vẫn ra `tơ`, `cwon` vẫn ra `cơn`. Telex nâng cao và VNI không bao giờ giữ `w` lại, nên cũng
không đổi.

### 2.2. Thứ tự ở ranh giới từ

1. Gõ tắt (`shortcut::expansion`). Trigger `two` vẫn nổ.
2. `listed_restore`: trước là phím lặp (`doubled`), sau là va chạm (`collision`). Từ được trả lại thì
   không để lại gợi ý sửa chính tả.
3. `judge`: English restore theo âm tiết.

Flip luôn thắng bảng. Flip một lần hiện phím thô. Flip lần hai ghim tiếng Việt, và đó là cách người
quen gõ `w` sớm vẫn giữ được `tơ`. Họ cũng có thể gõ dạng chuẩn (`tow`, `sawps`).

### 2.3. Chỉ ở ranh giới từ

Không đổi giữa từ. `tơ`, `să`, `sư` mở đầu rất nhiều âm tiết thật (`swang` → `săng`), nên chỉ khi từ
đã xong mới biết nó là tiếng Anh.

### 2.4. Chi phí

Cổng chạy trước, không cấp phát. Chỉ chuỗi qua cổng mới bị tìm trong bảng, cũng không cấp phát.
`tests/alloc_budget/collision.rs` ghim điều đó: gõ `cwon tow` 64 lần cấp phát y hệt khi bật và khi tắt.

## 3. Bảng từ và bộ sinh

`funput-lexicon-tool collision --data DIR --out DIR < en.tsv` gõ từng từ của `en.tsv` qua engine
thật, với `restore_collisions = false`. Một từ được ghi vào `<out>/<luật>/words.txt` khi:

- cổng của luật nhận chuỗi phím (`Collision::shapes`);
- có ít nhất một cấu hình của kiểu gõ đó (`STANDARD`/`ZFWJ` × eager restore bật/tắt) giữ từ ở dạng
  tiếng Việt khi gõ dấu cách (`scan::left_vietnamese`).

`overrides.txt` cạnh mỗi bảng chứa các dòng `+từ` / `-từ`, ghi lý do trong comment. Dòng không thay
đổi gì thì bộ sinh báo lỗi. Hiện tệp trống: cả 9 từ đều được giữ.

`refresh.sh` (bước 6) sinh lại bảng sau khi `en.tsv` đổi. Sau khi đổi core, chạy tay:

```bash
cargo run -p funput-lexicon-tool -- collision \
    --data crates/funput-engine/data/collision \
    --out crates/funput-engine/data/collision < crates/funput-suggestions/data/lexicon/en.tsv
```

Test `collision_table` sinh lại và so từng byte, nên bảng cũ làm CI đỏ. Test cũng so danh sách
thư mục hai bên, nên thư mục engine có mà bộ sinh không tạo ra cũng làm CI đỏ. `--data` trỏ tới thư
mục không tồn tại là lỗi, để gõ sai đường dẫn không âm thầm bỏ mất `overrides.txt`.

## 4. Thêm một luật mới

1. Thêm biến thể vào `Collision` (`compose/collision/class.rs`): `name`, `method`, `shapes`, một
   `static` bảng từ và nhánh trong `words`. Bộ sinh tự nhận biến thể mới.
2. Tạo `data/collision/<tên>/overrides.txt` (chỉ header), chạy bộ sinh. Test
   `every_class_embeds_the_list_its_name_points_at` giữ đường dẫn `include_str!` khớp với `name`.
3. Thêm ca vào `tests/restore/collision/` và hạ baseline coverage tiếng Anh trong cùng thay đổi.

Cổng phải hẹp và rẻ: nó là thứ bảo đảm tiếng Việt ngoài bảng không bị đụng tới.

## 5. Số đo

| | eager bật | eager tắt |
|---|---|---|
| Từ tiếng Anh giữ dạng tiếng Việt, trước | 577 | 1 230 |
| Sau | 568 | 1 221 |

Baseline: `benchmarks/results/english-coverage.txt`.

## 6. Liên quan: vần mở `uơ` (cũng #661)

Cùng issue có lỗi Telex nâng cao: `twos` → `tuớ`. Nguyên nhân không phải bảng từ mà là phép kiểm âm
tiết. Vần mở `uơ` chỉ có sau ∅, `h`, `kh`, `th` (`thuở`, `huơ`, `khuơ`, `uở` trong Viet74K), nhưng
trước đây được nhận với mọi phụ âm đầu. Bảng `RESTRICTED_RHYMES` trong
`crates/funput-core/src/validation/syllable/spelling.rs` giờ giới hạn điều đó, nên `tuớ`, `nuớ`,
`giuờ` không còn là âm tiết và ranh giới từ trả lại phím thô. Phần ghép chữ không đổi: `trưo` + `f`
→ `truờ` → `trường` vẫn chạy. Phụ phẩm: sửa chính tả giờ sửa được `dduowxj` (`đuợ`) thành `được`.
