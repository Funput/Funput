# Địa danh Tây Nguyên và tên dân tộc

## Trạng thái

Có trong core (`funput-core`) và engine (`funput-engine`), nên cả 5 nền tảng đều
nhận qua funput-ffi / funput-jni mà không cần sửa shell.

## Mục tiêu

Gõ được địa danh và tên dân tộc có cách viết khác âm tiết tiếng Việt thuần —
`Đắk Lắk`, `Krông Pắc`, `M'Đrắk`, `Chư Păh`, `Ia Kdăm`, `Xtiêng`, `Hrê` — mà
English restore vẫn trả `draw`, `cash`, `cool`, `know` về đúng tiếng Anh.

Địa danh Tây Bắc (`Mường Nhé`, `Sìn Hồ`, `Mù Cang Chải`…) viết theo chính tả tiếng
Việt thường, nên không cần ngoại lệ nào.

## Những gì được chấp nhận

| Cách viết | Ví dụ | Telex | VNI |
|---|---|---|---|
| Âm cuối `k` đọc như `c` | `Đắk`, `Lắk`, `Búk` | ✓ | ✓ |
| Cụm âm đầu tiếng Anh cũng có (`bl br dr đr gl gr kl kr phl pl pr bh`) — vần phải là vần tiếng Việt | `Krông`, `Plông`, `Đrắk`, `Phlắc` | ✓ | ✓ |
| Cụm âm đầu tiếng Anh không có (`kp kd kb kt ktl km khl hr hđr hm hn mr mđh rc rl rs tb xr xt dl`) — vần nào cũng được | `Kpă`, `Kdăm`, `Rcăm`, `Dliê`, `Xtiêng` | ✓ | ✓ |
| Âm cuối `h`, `l`, `r` | `Păh`, `Tẻh`, `Nuôl`, `Blơr` | Flip / phím kép | ✓ |
| Âm cuối tắc không dấu thanh | `Môt`, `Mâp`, `Trôk` | Flip hai lần | ✓ |

Cách gõ khi Telex va chạm với phím:

- `r` là phím hỏi, `a…a` là mũ. Gõ phím hai lần để lấy chữ thường:
  `Karr` → `Kar` (Ea Kar), `garr` → `gar` (Cư M'gar), `Anaa` → `Ana` (Krông Ana),
  `Blowrr` → `Blơr`.
- Âm cuối `h` bị English restore ngay khi gõ (`Pawh`): bấm **Flip** để lấy lại
  `Păh`.
- Âm cuối tắc không dấu (`Môt`) bị restore ở Space: bấm Flip hai lần trước Space để
  ghim dạng tiếng Việt.
- `Tbuăn`: gõ `w` sau âm cuối (`Tbuanw`). Vì `ua` + `w` luôn là `ưa`, như `mưa`.

## Vì sao không rộng hơn

Mỗi ngoại lệ chỉ rộng đến mức tiếng Anh cho phép. Những thứ cố ý để ngoài:

- **Âm cuối `h`/`l`/`r` trong Telex.** `s` + `h` chính là đuôi `sh` (`cash` →
  `cáh`, `bush` → `búh`); mũ trễ biến `aha` thành `âh`; `cool` → `côl`; `r` là phím
  hỏi. Buffer không phân biệt được những từ đó với `Păh`, nên chỉ VNI nhận các âm
  cuối này — ở VNI chỉ chữ số mới tạo dấu, tiếng Anh không bao giờ tới được đó.
- **Cụm `kn`, `sl`, `sr`.** `know` → `knơ`, `knee` → `knê`, `slow` → `slơ`. Vì vậy
  `Ea Knuếc`, `Slìn`, `SRó` chưa gõ được dấu.
- **Âm đầu `j`, `f`, `w`, `z`.** Đây là phím Telex; trong VNI, `win10` sẽ thành
  `win`. Vì vậy `Cư Jút` chưa gõ được dấu.
- **Âm cuối tắc không dấu với âm đầu thường.** `moot` → `môt`, `book` → `bôk`.

## Kiến trúc

- `validation/ethnic/` giữ **mọi** ngoại lệ, mỗi cụm kèm tên thật cần đến nó:
  - `clusters.rs`: `ClusterKind::Shared` / `Distinct`, xét theo **phím** gõ ra cụm
    chứ không theo chữ. Telex đánh `đ` từ một `d` gõ sau, nên `đr` dùng chung `dr`
    với tiếng Anh.
  - `mod.rs`: `admits` (vần sau cụm distinct) và `closes_with_name_final` (âm cuối
    `h`/`l`/`r` sau nguyên âm mà tiếng Việt khép được: `ăh` như `ăn`).
- `is_complete_syllable` (ranh giới từ) và `is_definitely_invalid` (eager restore)
  hỏi `ethnic`. Eager restore của engine gọi `is_definitely_invalid_in(buffer, method)`:
  chỉ VNI nhận thêm âm cuối của tên riêng.
- `apply_stroke` không đánh `đ` trễ vào một `d` mở đầu cụm phụ âm (`droid`,
  `dried`): `đ` tiếng Việt luôn đứng trước nguyên âm, còn `Đr` được gõ ngay tại chỗ.

## Thêm một tên mới

1. Tìm đúng loại: cụm âm đầu hay âm cuối.
2. Cụm mới: thêm vào `SHARED` nếu có từ tiếng Anh bắt đầu bằng các phím đó
   (`grep -ci '^<cụm>' /usr/share/dict/words`), ngược lại vào `DISTINCT`. Luôn ghi
   tên thật bên cạnh.
3. Thêm tên vào `tests/restore/place_names.rs` (engine) hoặc
   `tests/spellcheck_corpus.rs` (core), và một từ tiếng Anh mà ngoại lệ đó có thể
   làm hỏng vào `english_still_restores_in_telex`.
4. Chạy differential: từ điển tiếng Anh gõ Telex qua engine, so với `main`. Không
   được có từ nào trước đây restore mà nay không restore.
