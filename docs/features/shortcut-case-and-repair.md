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

Trạng thái: đã làm (PR #702).

### Hiện trạng

Bảng có `ctsg` → `cơm tấm Sài Gòn`.

- `ctss` ⌫ `g␣`: bung đúng, vì từ chưa kết thúc.
- `ctss␣` ⌫ ⌫ `g␣`: không bung, ra `ctsg␣`.

Nguyên nhân: Backspace lùi vào một từ đã xong thì từ đó được mở lại bằng `Engine::adopt` ([editing.rs](../../crates/funput-engine/src/engine/editing.rs)). `adopt` chỉ nhận âm tiết tiếng Việt (`is_reopenable_syllable`). `ctss` và `cts` không phải âm tiết nên không được mở lại, `g` thành một từ mới chỉ gồm `g`.

Unikey vẫn bung được, kể cả khi người dùng đã gõ dài phía sau rồi xóa lùi về.

### Quyết định

`adopt` nhận thêm chữ là **phần đầu của một chữ tắt có trong bảng** (`cts` là phần đầu của `ctsg`), kể cả trọn chữ tắt (`ctsgx␣` ⌫⌫ về `ctsg` vẫn bung), khi gõ tắt đang bật. Ở chế độ tiếng Anh, chỉ nhận khi bật "Gõ tắt cả khi ở chế độ tiếng Anh", và không bao giờ nhận âm tiết.

- Khớp chữ đang hiện theo cùng quy tắc hoa/thường của tra chữ tắt (`starts_a_trigger` trong [shortcut.rs](../../crates/funput-engine/src/compose/boundary/shortcut.rs), phần hoa/thường tách ra `shortcut/case.rs`): bật smart case thì `Cts`, `CTS` cũng tính, tắt thì chỉ đúng chữ đã lưu.
- Chữ không phải âm tiết được mở lại thì phím thô chính là chữ đó, nên gõ tiếp `g` cho phím `ctsg` và ranh giới tra thấy chữ tắt như thường.
- Chữ tiếng Anh như `hello` vẫn không được mở lại, trừ khi bảng có chữ tắt bắt đầu bằng nó. Khi đó phím gõ tiếp được ghép vào chữ như với một âm tiết mở lại: bảng có `dc` thì `d␣` ⌫ `d` ra `đ` (trước đây `dd`), giống `a␣` ⌫ `a` ra `â`. Bảng càng nhiều chữ tắt ngắn thì càng nhiều cụm phụ âm ngắn mở lại được; trên Android và macOS chữ đó hiện gạch chân như vùng đang gõ.
- Từ chối chữ mở đầu bằng chữ số (`500` với chữ tắt `500k`): gõ thẳng thì `500k` là số kèm đơn vị và không bung, mở lại không được làm khác. Từ chối cả chữ chứa ký tự ranh giới (dấu cách, dấu câu).
- Cơ chế mở lại sau mỗi lần Backspace đã có (`a_refused_word_is_reopened_once_backspace_makes_it_a_syllable`), nên `ctss` → ⌫ → `cts` tự rơi vào điều kiện mới.
- Áp dụng cho mọi nền tảng đang gọi `adopt` khi Backspace: Windows, Linux non-preedit, Android, iOS, và macOS khi bật sửa dấu sau Backspace (`RetoneScan`). Chưa có test riêng cho macOS. Công cụ chuyển mã gọi một hàm `adopt` khác, không bị ảnh hưởng.
- Chế độ tiếng Anh: engine hỗ trợ, nhưng hiện chỉ shell desktop (Windows) gọi `adopt` khi tắt tiếng Việt. Android, iOS, Linux và macOS chỉ mở lại từ khi bật tiếng Việt.
- Chi phí: chỉ duyệt bảng khi chữ **không** phải âm tiết, và `adopt` chỉ chạy khi Backspace lùi vào từ đã xong, nên đường gõ thường không tốn thêm.
- Vừa hoàn tác một lần bung rồi xóa dấu cách: chữ tắt được mở lại nhưng lệnh bỏ qua một lần vẫn còn, nên Space tiếp theo giữ nguyên chữ tắt.

### Giới hạn đã biết

- Chỉ khớp chữ đang hiện. Chữ tắt lưu dạng phím thô (`ddc`, màn hình hiện `đc`) không sửa được theo cách này.
- Chữ tắt dài hơn cửa sổ nhìn lại của nền tảng không sửa được: Android và macOS chỉ đọc 12 ký tự trước con trỏ.
- Phần nhớ 256 byte của desktop rộng hơn 64 byte cũ, nên nếu app tự sửa chữ mà hook không thấy (Word AutoCorrect đổi `dont` thành `don't`), phần nhớ lệch xa hơn. Rủi ro này có từ trước và vẫn được chặn bằng mọi sự kiện gọi `CommittedTail::clear` (click, phím mũi tên, đổi focus).

### Đoạn xóa dài

`CommittedTail` trên desktop chỉ nhớ 64 byte cuối ([retone.rs:27](../../crates/funput-desktop/src/retone.rs:27)). Xóa lùi qua một đoạn dài hơn thì quên chữ tắt nằm trước đó. Tăng `CAPACITY` lên 256 byte: vẫn rẻ, đủ cho vài câu ngắn. Đây là trường hợp ngách, không cố khớp Unikey ở mọi độ dài.

### Kiểm thử

- `crates/funput-engine/tests/adopt.rs`: nhận phần đầu và trọn chữ tắt (rồi Space bung); từ chối chữ không mở đầu chữ tắt nào; chữ tắt một chữ cái chỉ mở lại chính nó; chữ tắt viết bằng chữ Việt (`đc`) và chữ hoa xen kẽ (`iOS`); chữ tắt lưu dạng phím (`ddc`) không khớp; VNI; từ chối chữ mở đầu bằng số và chữ có ký tự ranh giới; theo smart case (`Cts`, `CTS` nhận, `cTs` không; tắt smart case thì chỉ `cts`); từ chối khi tắt gõ tắt; chế độ tiếng Anh chỉ nhận khi bật gõ tắt tiếng Anh; chữ tắt vừa bị hoàn tác vẫn giữ nguyên khi mở lại.
- `crates/funput-desktop/src/shell/tests.rs` (qua `ShellState` thật): sửa chữ tắt ở chế độ tiếng Anh; `d␣` ⌫ `d` ra `đ` khi bảng có `dc`.
- `tests/alloc_budget.rs`: `adopt("Cts")` tốn đúng 2 lần cấp phát (kiểm âm tiết và hạ chữ thường), duyệt bảng không cấp phát.
- `crates/funput-desktop/tests/retone.rs`: `ctss␣` ⌫⌫ `g␣` → `cơm tấm Sài Gòn␣`; `Ctss␣` → `Cơm tấm Sài Gòn␣`; `ctsgx␣` ⌫⌫ Space bung; xóa lùi qua 160 byte `abc abc …` về chữ tắt rồi sửa vẫn bung (test này hỏng với 64 byte cũ); `hello␣` ⌫ `s` vẫn ra `hellos`.
- Không chạy differential: `adopt` chỉ chạy khi Backspace lùi vào từ đã xong, đường gõ thường không đi qua.
