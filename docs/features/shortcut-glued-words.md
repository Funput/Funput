# Gõ tắt: chữ dính phía trước và chữ tắt có ký hiệu

> **Trạng thái:** mục 1 (từ dính, `@` và `/`) đã làm trong engine cho mọi nền tảng; Windows truyền thêm ký tự trước từ khi mở lại. Mục 2 (chữ tắt có ký hiệu) đã làm trong engine nhưng chỉ Windows bật (xem mục Nền tảng). Mục 3: hàm kiểm tra và phần nhập file đã làm; giao diện mới có Windows.

Nguồn: issue #782 mục 3 (chữ tắt có ký hiệu), góp ý ở #737 về `tuoitre.vn`, và lỗi bung trong tên miền phát hiện khi bàn issue #582.

## Người dùng xin gì

1. **Không bung giữa tên miền, email, đường dẫn.** Bật bung bằng Enter rồi gõ `tuoitre.vn` Enter trên thanh địa chỉ thì ra `tuoitre.Việt Nam` và trình duyệt nhảy sang trang tìm kiếm. Người dùng có kinh nghiệm né bằng cách đặt chữ tắt vô nghĩa (`vnn`), nhưng người mới sẽ đặt `vn`, `k`, `com`.
2. **Chữ tắt có ký hiệu** như `;ct`, theo quy ước của các phần mềm text expander: thêm một ký hiệu để chữ tắt không bao giờ trùng chữ thật.

Hai việc dính nhau: cả hai hỏi **ký tự nằm ngay trước từ đang gõ là gì**. Việc 1 coi dấu câu dính phía trước là lý do để không bung, việc 2 coi nó là một phần của chữ tắt. Thiết kế chung một lần.

## Hiện trạng

Mọi dấu câu ASCII là ranh giới từ (`is_english_boundary`, [boundary/mod.rs](../../crates/funput-engine/src/compose/boundary/mod.rs)). Sau một dấu câu, phần chữ gõ tiếp là một từ mới và được tra gõ tắt như một từ đứng riêng. Dấu câu cũng là phím kích hoạt, nên `com` bung ngay lúc gõ dấu `.` theo sau.

Ngoại lệ duy nhất là từ dính sau chữ số: `NumberGlue` ([number_glue.rs](../../crates/funput-engine/src/model/number_glue.rs)) chặn `500k` thành `500không`.

Bảng có `vn` → `Việt Nam`, `k` → `không`, `com` → `công`, `t` → `tôi`:

| Gõ | Ra |
|---|---|
| `abc.vn␣` | `abc.Việt Nam␣` |
| `google.com.vn␣` | `google.công.Việt Nam␣` |
| `a@b.vn␣` | `a@b.Việt Nam␣` |
| `x/vn␣`, `a_vn␣`, `#vn␣`, `-vn␣`, `abc,vn␣` | đều bung |
| `ok.k␣`, `5.k␣` | `ok.không␣`, `5.không␣` |
| `don't␣` | `don'tôi␣` |
| `abc(vn␣` | `abc(Việt Nam␣` |
| `(vn)␣`, `'vn'␣`, `vn.` | bung, đúng ý |
| `500k␣` | `500k␣` (đã chặn) |

Chữ tắt có ký hiệu thì lưu được nhưng **không bao giờ bung**, vì engine chỉ thấy phần chữ sau dấu câu cuối cùng:

| Bảng | Gõ | Ra |
|---|---|---|
| `;ct` | `;ct␣` | `;ct␣` |
| `x.y` | `x.y␣` | `x.y␣` |
| `c t` | `c t␣` | `c t␣` |
| `2k` | `2k␣` | `2k␣` (mở đầu bằng chữ số nên bị `NumberGlue` chặn) |

Chữ tắt tiếng Việt (`đc`) và chữ tắt có chữ số ở giữa hoặc cuối (`k2`) đã bung đúng, không thuộc thiết kế này.

## Quyết định

### 1. Từ dính phía trước không bung

Một từ chỉ được tra như chữ tắt đứng riêng khi ký tự ngay trước nó là:

- đầu văn bản, hoặc engine không biết phía trước có gì (vừa click, đổi ô);
- khoảng trắng (dấu cách, Tab, xuống dòng);
- một **dấu mở** `(` `[` `{` `"` `'` mà chính nó đứng sau khoảng trắng hoặc ở đầu văn bản.

Mọi trường hợp khác là **từ dính** và không bung: sau chữ số (như hiện nay), sau dấu câu ASCII, sau dấu mở nằm dính vào chữ khác (`abc(vn`, `don't`), và sau một từ vừa chốt bằng Ctrl ([ctrl-seal-word.md](ctrl-seal-word.md): phím gõ sau Ctrl bắt đầu từ mới dính liền phía sau).

| Gõ | Ra |
|---|---|
| `abc.vn␣`, `google.com.vn␣`, `a@b.vn␣` | giữ nguyên |
| `x/vn␣`, `a_vn␣`, `#vn␣`, `abc,vn␣` | giữ nguyên |
| `don't␣` | `don't␣` |
| `(vn)␣`, `'vn'␣`, `"vn"␣` | `(Việt Nam)␣`, `'Việt Nam'␣`, `"Việt Nam"␣` |
| `vn.`, `vn,` | `Việt Nam.`, `Việt Nam,` (ký tự **sau** từ không đổi gì) |

Phía **sau** từ, `@` và `/` không còn là phím kích hoạt: hai dấu này gần như không bao giờ đứng ngay sau một từ trong câu văn, nhưng luôn nằm trong email và đường dẫn. Các dấu câu khác (`.`, `,`, `;`, `:`, `!`, `?`, `)`…) vẫn kích hoạt như hiện nay.

| Gõ | Ra |
|---|---|
| `ct@gmail.com` | giữ nguyên (trước đây `cơm tấm@gmail.com`) |
| `ct/abc` | giữ nguyên |
| `ct.`, `ct,`, `ct)` | `cơm tấm.`, `cơm tấm,`, `cơm tấm)` |

Nguyên tắc giống `NumberGlue`: **bỏ lỡ một lần bung thì được, bung sai thì không.** Bỏ lỡ thì người dùng gõ tiếp là xong, còn bung sai trong một địa chỉ hay một mã thì phải xóa tay.

Đánh đổi chấp nhận:

- `có/k` không còn ra `có/không`. Dấu `/` vừa là cách viết "hoặc" vừa là dấu phân cách đường dẫn (`abc.com/vn`). Chọn chặn theo nguyên tắc trên.
- Quên dấu cách sau dấu câu (`Xin chào.vn`, `a,k`) thì không bung.
- `-vn` (gạch đầu dòng không có dấu cách) không bung.

Giới hạn: engine chỉ biết những gì gõ liền mạch. Click ra sau `abc.` rồi gõ `vn␣` thì engine coi như đầu văn bản và vẫn bung. Chuỗi dấu câu đang chờ được quên khi nền tảng báo con trỏ đã dời (`arm_capitalization`, `disarm_capitalization`), nên dấu câu gõ ở chỗ cũ không làm dính từ gõ ở chỗ mới.

### 2. Chữ tắt có ký hiệu ở đầu hoặc ở cuối

Một chữ tắt gồm ba phần:

- **Phần đầu** (không bắt buộc): một chuỗi dấu câu ASCII và chữ số, tối đa 8 byte, như `;`, `;;`, `2`, `@`.
- **Lõi** (bắt buộc): chữ cái, có thể có chữ số ở giữa hoặc cuối, như hiện nay (`ct`, `đc`, `k2`).
- **Phần cuối** (không bắt buộc): **một** dấu câu ASCII là phím kích hoạt, như `ct;`. `@` và `/` không làm phần cuối được vì không còn kích hoạt.

Cách tra:

- Engine nhớ chuỗi dấu câu và chữ số dính ngay trước từ đang gõ (phần đầu). Khi tra, **khớp dài nhất thắng**: thử phần đầu cộng lõi trước, rồi mới tới lõi đứng riêng. Bảng có cả `ct` và `;ct` thì `;ct` ra nội dung của `;ct`, `ct` ra nội dung của `ct`.
- Lõi đứng riêng vẫn phải qua luật ở mục 1. Gõ `;ct` mà bảng chỉ có `ct` thì không bung, vì `ct` dính sau `;`.
- Phần cuối: khi phím ranh giới là một dấu câu `p`, thử lõi cộng `p` trước. Khớp thì bung và **không** ghi thêm `p`, vì `p` là một phần của chữ tắt. Không khớp thì bung lõi và ghi `p` như hiện nay.
- Bung xóa cả phần đầu: `;ct␣` thành `cơm tấm␣`, không còn dấu `;`. Backspace hoàn tác trả lại `;ct␣`.
- Smart case chỉ xét chữ cái, nên `;Ct` và `;CT` viết hoa như `Ct`, `CT`.
- Phần đầu chữ số cũng đi đường này, nên chữ tắt `2k` bung được, còn `500k` vẫn không vì bảng không có chữ tắt `500k` và `k` dính sau chữ số.
- Trong Telex nâng cao, `[` và `]` là chữ `ư`, `ơ`, không phải dấu câu, nên không làm phần đầu hay phần cuối được.

Không hỗ trợ:

- **Dấu câu ở giữa** (`x.y`) và **dấu cách** (`c t`). Muốn làm phải nhìn ngược qua nhiều từ, tốn công cho một nhu cầu hiếm.
- **Phần cuối dài hơn một ký tự** (`ct;;`).
- **Mở lại chữ tắt có phần đầu sau Backspace.** Nền tảng chỉ trao lại phần chữ trước con trỏ (`ct`), không kèm `;`, nên `;ctss␣` ⌫⌫ `g␣` không sửa được thành `;ctsg`. Chữ tắt có phần cuối (`ctsg;`) vẫn sửa được.

### 3. Bảng chỉ nhận chữ tắt bung được

Hiện tại `;ct`, `x.y`, `c t`, `2k` được lưu mà không bao giờ bung, người dùng không biết vì sao. Không nơi nào kiểm tra ký tự của chữ tắt: Windows, Android, iOS, engine và phần nhập file chỉ từ chối chữ tắt rỗng. Thay vào đó:

- Một hàm kiểm tra chung trong Rust quyết định chữ tắt hợp lệ theo hình dạng ở mục 2. Giao diện bảng gõ tắt trên mọi nền tảng và phần nhập file đều gọi hàm này.
- Thêm tay: báo lý do ngay dưới dòng (ví dụ "Chữ tắt không được có dấu cách."), và dòng đó không vào engine. Windows sửa bảng ngay tại chỗ, không có nút Lưu, nên dòng vẫn được lưu như người dùng gõ; nó chỉ không bung cho tới khi sửa.
- Nhập file: bỏ qua dòng không hợp lệ và báo số dòng bị bỏ qua, như cách đang báo dòng lỗi.
- Bảng đã lưu sẵn chữ tắt không hợp lệ thì giữ nguyên, không tự xóa, chỉ đánh dấu trong giao diện.

### 4. Không làm

- **Hoãn bung tại dấu `.` để chữ tắt đứng đầu tên miền (`ct.vn`) không bung.** Ctrl đã giải quyết đúng ca này: `ct` Ctrl `.vn`. Hoãn bung còn làm `vn.` ở cuối tin nhắn không bung nữa và chữ hiện ra chậm một nhịp.
- **Đọc loại ô nhập (ô URL, ô email) để tắt gõ tắt.** Hữu ích nhưng chỉ có trên Android, iOS, Linux; tách ra làm sau nếu mục 1 chưa đủ.

## Engine

- **Trạng thái dính:** `NumberGlue` thành `Lead` ([model/lead.rs](../../crates/funput-engine/src/model/lead.rs)): từ đang gõ có dính hay không, và chuỗi dấu câu, chữ số dính ngay trước (tối đa 8 byte, lưu tại chỗ, không cấp phát trên đường gõ). Chuỗi đang chờ từ sống qua `Engine::clear()` để Android (xóa engine ngay sau khi commit) vẫn thấy dấu câu vừa gõ; `arm_capitalization` và `disarm_capitalization` xóa nó. `size_of::<Engine>()` tăng 200 → 216.
- **Cập nhật:** `after_key` ghi nhận phím không mở từ, kể cả ở chế độ tiếng Anh: khoảng trắng làm sạch trạng thái, dấu câu và chữ số nối vào chuỗi, phím vừa kết thúc một từ đánh dấu chuỗi là đứng sau chữ. `start_word` chốt trạng thái cho từ sắp gõ. `seal()` đánh dấu từ kế tiếp là dính.
- **Tra:** [shortcut/candidates.rs](../../crates/funput-engine/src/compose/boundary/shortcut/candidates.rs) thử theo thứ tự: phần đầu + lõi + phần cuối, phần đầu + lõi, lõi + phần cuối, rồi lõi đứng riêng nếu từ không dính, mỗi dạng với phím gõ, `keys` rồi `buffer`. Chuỗi ghép nằm trên stack (tối đa 64 byte), nên tra ở mỗi dấu câu không cấp phát; bảng không có chữ tắt ký hiệu chỉ tốn thêm vài lần tra băm. Số ký tự xóa (`backspace`) cộng thêm độ dài phần đầu. Chế độ tiếng Anh có gõ tắt đi cùng đường (ở đó chữ số là một phần của từ, nên `2k` khớp nguyên văn).
- **Phím kích hoạt:** `@` và `/` vẫn là ranh giới từ (kết thúc từ, giữ nguyên tập `is_english_boundary` và bản chép trên Android), chỉ không tra gõ tắt. `expansion` trong [shortcut.rs](../../crates/funput-engine/src/compose/boundary/shortcut.rs) bỏ qua khi `boundary_key` là một trong hai dấu này.
- **Bung bằng Enter, Tab** (`expansion_in_place`): cùng luật dính và phần đầu, không có phần cuối.
- **Hoàn tác** ([shortcut-undo.md](shortcut-undo.md)): `shown` gồm cả phần đầu, để ⌫ trả lại `;ct␣`; với phần cuối, `written` không kèm phím ranh giới và ⌫ trả lại `ct;`. Lệnh bỏ qua một lần giữ phần chữ, không kèm phần đầu, vì nền tảng chỉ mở lại `ct`.
- **Mở lại sau Backspace:** mở lại một từ thì phải biết nó có dính không, nếu không `abc.vn␣` ⌫ Space sẽ bung. `Engine::adopt_after(text, before)` nhận ký tự nằm ngay trước từ. `adopt(text)` giữ nghĩa cũ là không biết phía trước, và từ được coi là đứng riêng: coi là dính thì luồng sửa chữ tắt gõ nhầm (`ctss␣` ⌫⌫ `g␣`, #702) sẽ hỏng trên mọi nền tảng chưa truyền ngữ cảnh, đổi một lỗi hiếm lấy một tính năng đang dùng.
- **Kiểm tra chữ tắt:** `check_trigger` và `compare_triggers` nằm ở `funput_core::shortcut` ([shortcut/trigger.rs](../../crates/funput-core/src/shortcut/trigger.rs)) cùng luật chữ hoa, để `funput-config` (chỉ phụ thuộc `funput-core`) và mọi giao diện gọi chung. Lỗi có câu tiếng Việt sẵn (`Display`). `MAX_PREFIX` là một hằng số dùng chung với `Lead`. Nhập file (`transfer::apply`, cũng là đường của tệp UniKey) bỏ dòng không hợp lệ và đếm vào `ImportSummary::shortcuts_skipped`. Chưa xuất qua FFI và JNI.

## Nền tảng

Phần lớn nằm trong engine. Việc của từng nền tảng:

- **Truyền ký tự trước từ khi mở lại** (`adopt_after`): Windows đã làm, lấy từ `CommittedTail` (`Removed::before`) và từ ô nhập trong Cài đặt. Android, iOS, macOS, Linux đọc được từ văn bản trước con trỏ; chưa nối, vẫn gọi `adopt`.
- **Chữ tắt có ký hiệu cần nền tảng làm đúng lệnh của engine** (`EngineConfig::shortcut_affixes`, mặc định tắt). Phần đầu bắt nền tảng xóa cả ký tự đã commit trước vùng đang gõ, phần cuối bắt nền tảng ghi một nội dung không kết thúc bằng phím ranh giới. Hiện chỉ Windows bật (hook + inject làm đúng `backspace` và `output`, `CommittedTail` theo kịp). Ở nền tảng khác chữ tắt có ký hiệu giữ nguyên như gõ, như trước đây. Muốn bật thì từng nền tảng phải sửa:
  - **Android:** JNI `nativeBoundary` chỉ trả `output`, bỏ số ký tự xóa; `CompositionDocumentEditor` chỉ thay vùng đang gõ.
  - **macOS** (cả gạch chân lẫn gõ trực tiếp) và **Linux có gạch chân:** chỉ thay vùng đang gõ, và tự cắt ký tự cuối của `output` coi là phím ranh giới.
  - **Linux không gạch chân:** đã xóa đúng số ký tự; nhưng khi chưa gõ chữ nào, dấu câu chỉ được đưa vào engine lúc bật tự viết hoa, nên engine không thấy phần đầu.
  - **iOS:** số ký tự xóa vượt vùng đang gõ thì bàn phím bỏ lần bung đó (an toàn, không làm hỏng chữ); cần đo theo văn bản trước con trỏ (`deletionCount(scalars:context:)`).
- **Dấu câu phải đi qua engine.** Engine chỉ biết từ dính nếu thấy dấu câu đứng trước. Đã kiểm: iOS (`KeyboardInputCoordinator+Text.swift`, phím `.punctuation` gọi `composer.process`) và Android (`ImeTypingHandler`, `engine.processBoundary`) đều đưa dấu câu trên trang ký hiệu qua engine, kể cả khi chưa gõ chữ nào. Ngoại lệ là `insertLiteral` trên iOS (emoji, kaomoji, dán), đúng loại chữ chèn sau lưng engine mà mục 1 đã chấp nhận. Khi engine không chạy (tiếng Anh, không bật gõ tắt) thì không có gì để bung nên không cần trạng thái dính. `CompositionBoundary.kt` trên Android chép lại tập ranh giới của engine; thiết kế này không đổi tập đó.
- **Giao diện bảng gõ tắt:** gọi hàm kiểm tra chữ tắt, báo lỗi dưới ô nhập. Câu chữ theo quy ước giao diện (câu tự nhiên, không dùng `·` hay `—`). Windows đã làm: `funput_desktop::shortcut_notes` tính lời nhắn từng dòng, `row.slint` hiện lỗi (`Theme.danger`) hoặc cảnh báo (`Theme.warn`) dưới dòng, và thông báo nhập file nêu số dòng bị bỏ qua. macOS, Linux, Android, iOS chưa làm.

## Kiểm thử

- `crates/funput-engine/tests/words/`: bảng "Hiện trạng" thành test với kết quả mới; `ct@gmail.com`, `ct/abc` không bung; dấu mở đứng đầu và dấu mở dính chữ; từ sau Ctrl; phần đầu và phần cuối (`;ct`, `ct;`, `2k`), khớp dài nhất thắng (`ct` và `;ct` cùng có); `;Ct` viết hoa; `500k` vẫn không bung; Telex nâng cao với `[`; chế độ tiếng Anh; Enter, Tab; hoàn tác trả lại phần đầu; `abc.vn␣` ⌫ Space không bung.
- `tests/alloc_budget/`: tra có phần đầu không cấp phát thêm trên đường gõ.
- Hàm kiểm tra chữ tắt: bảng ca hợp lệ và không hợp lệ; nhập file báo đúng số dòng bỏ qua.
- Differential test của engine (so 70k chuỗi phím với worktree gốc): không chuỗi nào được đổi khi bảng gõ tắt rỗng. Chạy thêm một lượt với bảng gõ tắt mẫu và liệt kê các chuỗi bị đổi để duyệt.
- Thử tay: thanh địa chỉ trình duyệt với bung bằng Enter bật (`tuoitre.vn` Enter), một app chat.

## Đã chốt

1. Dấu `/` đứng trước từ thì chặn, chấp nhận `có/k` không bung.
2. Dấu mở: `( [ { " '`, không thêm `<`.
3. Phần đầu tối đa 8 byte.
4. Chữ tắt không hợp lệ đã có sẵn trong bảng: chỉ đánh dấu, không tự xóa.
5. `@` và `/` không còn là phím kích hoạt.
