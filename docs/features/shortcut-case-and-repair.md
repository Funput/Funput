# Gõ tắt: viết hoa khi bung và sửa chữ tắt sau ranh giới

Thiết kế cho các góp ý về gõ tắt trong issue #582 (Windows). Ba thay đổi, cả ba đã chốt:

1. `Ct` chỉ viết hoa chữ đầu, phần còn lại giữ nguyên như đã lưu.
2. Desktop có Backspace hoàn tác gõ tắt như Android và iOS.
3. Chữ tắt gõ nhầm sửa được bằng Backspace sau khi đã qua ranh giới.

Các góp ý khác trong issue được bàn riêng: file `.txt` ở [shortcut-file-sync.md](shortcut-file-sync.md) (chưa chốt); Ctrl để hủy ở [ctrl-seal-word.md](ctrl-seal-word.md); phím kích hoạt Enter/Tab ở [shortcut-trigger-keys.md](shortcut-trigger-keys.md); lỗi ô nhập trong bảng gõ tắt ở [windows-composing-field-caret.md](windows-composing-field-caret.md); cách đặt dấu mới ở [implied-circumflex-confirm.md](implied-circumflex-confirm.md).

## 1. Viết hoa khi bung

Trạng thái: đã làm (PR #699).

### Hiện trạng

`classify_case` + `apply_shortcut_case` trong [shortcut.rs](../../crates/funput-engine/src/compose/boundary/shortcut.rs), khi bật "Tự nhận diện hoa/thường":

| Gõ | Hiện tại | `ct` → `cơm tấm Sài Gòn` | `tp` → `TP. HCM` |
|---|---|---|---|
| `ct` | giữ nguyên | cơm tấm Sài Gòn | TP. HCM |
| `Ct` | hoa đầu mọi từ, phần còn lại của mỗi từ hạ thường | Cơm Tấm Sài Gòn | Tp. Hcm |
| `CT` | hoa toàn bộ | CƠM TẤM SÀI GÒN | TP. HCM |

Dòng giữa đè lên cách viết người dùng đã lưu: `TP. HCM` thành `Tp. Hcm`, `công ty TNHH` thành `Công Ty Tnhh`. Gõ thường thì tôn trọng bảng, viết hoa một chữ lại không.

### Quyết định

Quy tắc mới: **chỉ nâng lên hoa, không bao giờ hạ cách viết đã lưu.**

| Gõ | Mới | `ct` → `cơm tấm Sài Gòn` | `tp` → `TP. HCM` |
|---|---|---|---|
| `ct` | giữ nguyên | cơm tấm Sài Gòn | TP. HCM |
| `Ct` | hoa chữ cái đầu tiên, phần sau giữ nguyên | Cơm tấm Sài Gòn | TP. HCM |
| `CT` | hoa toàn bộ | CƠM TẤM SÀI GÒN | TP. HCM |

Lý do:

- Viết hoa chữ đầu gần như luôn là để mở câu. Chữ nào cần hoa (tên riêng, viết tắt) người dùng đã lưu sẵn hoa trong bảng (`vn` → `Việt Nam`).
- Giống Unikey, nên người chuyển sang không bị bất ngờ.
- Chữ tắt một ký tự (`K` → `Không`) cho kết quả như cũ.

Không còn cách nhanh để ra "Hoa Đầu Mọi Từ". Trường hợp này hiếm, và lưu sẵn dạng đó trong bảng là đủ. Không thêm công tắc. Đề xuất ban đầu của user (`CTsg` cho từng mức hoa) đã được chính user rút lại, không làm.

### Engine

- `ShortcutCase::Title` đổi nghĩa: viết hoa **chữ cái đầu tiên** của nội dung (bỏ qua ký tự đứng trước không phải chữ cái hay chữ số, ví dụ `(`; gặp chữ số thì giữ nguyên), phần còn lại giữ nguyên từng ký tự. Dùng `to_uppercase` cho chữ đó để `đ` → `Đ`, `ệ` → `Ệ` đúng.
- `classify_case` không đổi: `Ct` và `C` vẫn là `Title`, `CT` vẫn là `Upper`, `cT` vẫn rơi về khớp chính xác.
- Câu mô tả công tắc ("Gõ vn, Vn hay VN đều bung và nội dung tự viết hoa theo") vẫn đúng, không cần sửa trên nền tảng nào.
- Đổi trong engine nên áp dụng cho mọi nền tảng cùng lúc. Ghi rõ trong release note vì người đang quen kiểu cũ sẽ thấy khác.

### Kiểm thử

Test cũ trong `crates/funput-engine/tests/words/shortcut.rs` đổi kỳ vọng theo quy tắc mới:

- `title_case_trigger_capitalizes_the_first_letter` (tên cũ `title_case_trigger_expands_to_title_case`): `Vn` với `việt nam` → `Việt nam`.
- `title_case_leaves_the_rest_of_a_multi_word_expansion_as_stored` (tên cũ `title_case_capitalizes_every_word_in_a_multi_word_expansion`): `Kg` → `Không có gì`.
- `single_uppercase_letter_trigger_capitalizes_rather_than_shouts`: `V` → `Việt nam`.
- `smart_case_keeps_a_stored_acronym_as_stored` thay cho `smart_case_off_keeps_the_expansion_verbatim`: `Tp` → `TP. HCM`.
- `english_mode_expansion_follows_smart_case`: `Vn` → `Việt nam`.

Test mới, cũng trong file đó:

- `title_case_keeps_capitals_stored_mid_expansion`: `ct`/`Ct` với `cơm tấm Sài Gòn`.
- `title_case_skips_leading_punctuation`: `(việt nam)` → `(Việt nam)`.
- `title_case_leaves_an_expansion_opening_with_a_digit_alone`: `3 con mèo` giữ nguyên.
- `title_case_uppercases_a_leading_dd`: `Dc` với `được` → `Được`.
- `title_case_touches_only_the_first_sentence_of_an_expansion`: `ok. cảm ơn\nnhé` → `Ok. cảm ơn\nnhé`.

Ngoài engine, các test đang khẳng định hành vi cũ cũng đổi theo: `crates/funput-ffi/tests/round_trip.rs`, `crates/funput-desktop/src/shell/tests.rs` (`Tp` → `TP. HCM`), `ImeShortcutInstrumentedTest.kt` trên Android, `ShortcutInputTests.swift` và `ShortcutKeyboardUITests.swift` trên iOS (`Vn` → `Việt nam`).

## 2. Backspace hoàn tác gõ tắt trên desktop

### Hiện trạng

Engine đã có hoàn tác ([shortcut-undo.md](shortcut-undo.md), PR #631): Backspace ngay sau khi bung trả lại chữ tắt kèm ký tự ranh giới, và bỏ qua chữ tắt đó một lần. Android và iOS đã nối. macOS, Windows, Linux chưa nối, vì thiết kế cũ cho rằng không có chip `↩` thì người dùng không biết Backspace sẽ lấy lại cả cụm.

Trên Windows, Backspace sau khi bung hiện chỉ xóa dấu cách rồi mở lại từ cuối của phần đã bung (`Việt Nam␣` → ⌫ → `f` → `Việt Nàm`).

### Quyết định

- **Bật trên desktop, không có công tắc.** Hành vi giống hệt Android: Backspace ngay sau khi bung thì hoàn tác. Người dùng xin đúng điều này, và trên Android cũng không ai phải nhìn chip mới dùng được.
- **Giữ ký tự ranh giới.** `ct␣` → `cơm tấm␣` → ⌫ → `ct␣`, không phải `ct`. Ý nghĩa của cú hoàn tác là "tôi muốn đúng `ct`", nên gõ tiếp được ngay. Muốn sửa `ct` thì Backspace thêm một lần (xem mục 3). Mọi nền tảng giống nhau.
- **Đánh đổi chấp nhận:** mất cách sửa dấu chữ cuối của phần đã bung bằng một Backspace. Giờ phải ⌫ hai lần (hoàn tác, xóa dấu cách) hoặc sửa tay.

Cập nhật [shortcut-undo.md](shortcut-undo.md) khi làm: bỏ dòng "macOS, Windows và Linux chưa có chip nên chưa dùng tính năng này", thêm mục desktop.

### Nối vào từng nền tảng

Hệ thống chủ phải biết chắc phần đã bung vẫn nằm ngay trước con trỏ **trước** khi hỏi engine, vì hỏi là tiêu nhịp.

- **Windows** (hook + inject, [funput-desktop](../../crates/funput-desktop)): ở nhánh Backspace, nếu `has_shortcut_undo()` và `CommittedTail` kết thúc bằng `shortcut_undo_written()` thì nuốt phím, gọi `undo_shortcut()`, inject kết quả. Cập nhật `CommittedTail` theo kết quả để mục 3 vẫn đúng. Mọi sự kiện đã gọi `CommittedTail::clear` (click, phím mũi tên, đổi focus, đổi EN/VI) cũng gọi `dismiss_shortcut_undo()`.
- **Linux non-preedit** ([backspace.cpp](../../platforms/linux/common/compose/composer/nonpreedit/backspace.cpp)): cùng cách, kiểm tra bằng surrounding text nếu có, không có thì dựa vào bóng chữ đã ghi như Windows.
- **Linux preedit (fcitx5, ibus) và macOS:** phần đã bung đã được commit. Kiểm tra bằng văn bản trước con trỏ khi client cung cấp, không có thì không hoàn tác (Backspace xóa như thường). Chi tiết chốt khi làm từng nền tảng.

### Kiểm thử

`crates/funput-desktop/tests/` thêm ca kiểu `Shell`: bung rồi ⌫ ra `ct␣`; ⌫ lần hai xóa dấu cách; gõ phím khác rồi ⌫ thì xóa như thường; click (flush) rồi ⌫ thì xóa như thường. Sửa `an_expansion_reopens_its_last_word` cho hợp hành vi mới. Thử tay trên Windows thật, gồm Firefox, Chrome, Word, Zalo.

## 3. Sửa chữ tắt gõ nhầm sau ranh giới

### Hiện trạng

Bảng có `ctsg` → `cơm tấm Sài Gòn`.

- `ctss` ⌫ `g␣`: bung đúng, vì từ chưa kết thúc.
- `ctss␣` ⌫ ⌫ `g␣`: không bung, ra `ctsg␣`.

Nguyên nhân: Backspace lùi vào một từ đã xong thì từ đó được mở lại bằng `Engine::adopt` ([editing.rs](../../crates/funput-engine/src/engine/editing.rs)). `adopt` chỉ nhận âm tiết tiếng Việt (`is_reopenable_syllable`). `ctss` và `cts` không phải âm tiết nên không được mở lại, `g` thành một từ mới chỉ gồm `g`.

Unikey vẫn bung được, kể cả khi người dùng đã gõ dài phía sau rồi xóa lùi về.

### Quyết định

`adopt` nhận thêm chữ là **phần đầu của một chữ tắt có trong bảng** (`cts` là phần đầu của `ctsg`), khi gõ tắt đang bật.

- Khớp theo cùng quy tắc tra chữ tắt hiện có: chữ đã gõ, phím, hoặc chữ đang hiện; có smart case thì `Cts` cũng tính.
- Chữ không phải âm tiết được mở lại thì phím thô chính là chữ đó, nên gõ tiếp `g` cho phím `ctsg` và ranh giới tra thấy chữ tắt như thường.
- Chữ tiếng Anh như `hello` vẫn không được mở lại, trừ khi bảng có chữ tắt bắt đầu bằng nó. Khi đó gõ tiếp thì phím được ghép như một từ mới gõ, đây là hành vi mong muốn.
- Cơ chế mở lại sau mỗi lần Backspace đã có (`a_refused_word_is_reopened_once_backspace_makes_it_a_syllable`), nên `ctss` → ⌫ → `cts` tự rơi vào điều kiện mới.
- Áp dụng cho mọi nền tảng đang gọi `adopt`: Windows, Linux non-preedit, Android, iOS. macOS không mở lại từ đã commit nên không đổi.
- Chi phí: chỉ tra bảng khi chữ **không** phải âm tiết, nên đường gõ tiếng Việt không tốn thêm. Phải giữ trong ngân sách cấp phát (`tests/alloc_budget/`).

### Đoạn xóa dài

`CommittedTail` trên desktop chỉ nhớ 64 byte cuối ([retone.rs:27](../../crates/funput-desktop/src/retone.rs:27)). Xóa lùi qua một đoạn dài hơn thì quên chữ tắt nằm trước đó. Tăng `CAPACITY` lên 256 byte: vẫn rẻ, đủ cho vài câu ngắn. Đây là trường hợp ngách, không cố khớp Unikey ở mọi độ dài.

### Kiểm thử

- Engine: `adopt` nhận `cts` khi bảng có `ctsg`, từ chối khi không có; `Cts` khi bật smart case.
- `crates/funput-desktop/tests/retone.rs`: `ctss␣` ⌫ ⌫ `g␣` → `cơm tấm Sài Gòn␣`; `ctss␣ abc abc` xóa lùi về rồi sửa vẫn bung (trong 256 byte); `hello␣` ⌫ `s` vẫn ra `hellos` khi bảng không có chữ tắt bắt đầu bằng `hello`.
- Chạy differential test của engine (so 70k chuỗi phím với worktree gốc) để chắc không đổi kết quả gõ thường.
