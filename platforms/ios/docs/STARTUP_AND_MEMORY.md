# Khởi động & bộ nhớ của Keyboard extension iOS

> **Mục tiêu:** bàn phím hiện ra tức thì, phím đầu tiên không bao giờ phải chờ một
> việc khác, không có hộp thoại hệ thống nào chen vào lúc gõ, bộ nhớ có trần và
> không leak. Đạt được **bằng cấu trúc**: một hợp đồng quy định việc gì được chạy
> trên main ở chặng nào, và mọi vùng nhớ đều có trần ghi trong code.
>
> **Trạng thái:** Hiện trạng đã audit (11/2026). A1–A6 đã hiện thực; A7 đã duyệt
> hướng, chưa hiện thực.
>
> **Cập nhật:** 04/11/2026
>
> **Phạm vi:** Cold start, vòng đời bộ nhớ, clipboard và xử lý ngữ cảnh ô nhập liệu
> của `Keyboard` extension. Không bàn Rust engine nội bộ hay theme store.

Tài liệu này là nơi các quyết định về khởi động và bộ nhớ sống, theo cùng quy ước với
[english-lexicon-suggestion.md](../../../docs/features/english-lexicon-suggestion.md):
**mỗi thay đổi hành vi sau này cập nhật lại tài liệu trong cùng PR**; chỗ nào hiện
thực lệch khỏi bản viết thì sửa lại và đánh dấu **Đã đổi khi hiện thực**.

Bối cảnh: [issue #641](https://github.com/Funput/Funput/issues/641).
[PR #642](https://github.com/Funput/Funput/pull/642) đã xử lý nguyên nhân jetsam chính
(bộ nhớ emoji). Tài liệu này chốt kiến trúc và các việc còn lại.

**Quy ước đường dẫn:** mọi đường dẫn tính từ `platforms/ios/`. Code trong Swift
package viết đầy đủ `Packages/FunputKit/Sources/<Module>/…`.

---

## 1. Nguyên tắc kiến trúc

Bàn phím iOS là app extension (`UIInputViewController` + `textDocumentProxy`), không
phải InputMethodKit như macOS. Mỗi lần hiện có thể là một process mới, iOS dựng lại
controller ở mỗi app host, và extension bị jetsam không báo trước khi vượt trần bộ
nhớ (thực nghiệm trên simulator iOS 27: kill process `Keyboard` thì iOS thay ngay
bằng bàn phím hệ thống). Năm nguyên tắc suy ra từ mô hình đó:

1. **Frame đầu tiên là bất khả xâm phạm.** Chỉ những việc trong danh sách §2 được
   chạy trên main trước khi bàn phím hiện. Thêm việc vào danh sách đó là một quyết
   định kiến trúc, phải sửa tài liệu này.
2. **Phím đầu tiên cũng bất khả xâm phạm.** Sau khi hiện, main thread thuộc về việc
   gõ. Mọi việc khác chạy off-main, và **chi phí trên main của nó phải có trần
   không phụ thuộc dữ liệu của user** (không đọc một file mà kích thước tùy user đã
   copy bao nhiêu).
3. **Không có gì bật lên khi user không yêu cầu.** Bàn phím không gọi API nào làm iOS
   hiện hộp thoại. Cụ thể: **không bao giờ tự đọc nội dung pasteboard**; nội dung
   chỉ đến qua `UIPasteControl`, tức khi user tự bấm (§4 A1).
4. **Mọi vùng nhớ có trần ghi bằng hằng số trong code**, và footprint đi ngang theo
   thời gian dùng: đổi app, mở/đóng panel bao nhiêu lần cũng quay về mức nền.
   Không load sẵn thứ layout chữ không cần: emoji chỉ load khi user bấm phím emoji,
   vì ảnh emoji CoreText đã giải mã không bao giờ được trả lại (§3.1).
5. **Giữ signpost hiện có** (§3.5) để còn quan sát được khi cần. Thay đổi nào vào
   các đường khởi động thì giữ hoặc thêm signpost, không bớt.

---

## 2. Hợp đồng khởi động

Đây là phần chuẩn tắc của tài liệu. Mỗi chặng ghi rõ việc gì **được phép** chạy trên
main. Việc không có trong bảng thì không được chạy ở chặng đó.

| Chặng | Được phép trên main | Không được phép |
|---|---|---|
| **Pre-main** (spawn, dyld) | Nạp binary đã link tĩnh | Framework động riêng, SwiftUI, static initializer làm I/O |
| **`init` controller** | Tạo engine Rust (`funput_engine_new`, không load từ điển); tạo store **chỉ giữ handle** (URL, `UserDefaults(suiteName:)`) | Mọi syscall đĩa, kể cả tạo thư mục |
| **`viewDidLoad`** | Đọc bootstrap snapshot (một file nhỏ, kích thước cố định theo cấu hình); resolve theme khi snapshot miss; dựng surface UIKit; kích hoạt height constraint | Đọc dữ liệu của user (clipboard, từ điển, lịch sử, shortcuts) |
| **`textWillChange`** | Resolve traits của field | Việc nặng hơn việc đọc traits |
| **`viewWillAppear`** | `synchronizeDocument` (IPC với host, bắt buộc để tính Shift); áp lại presentation khi cấu hình đổi | Đọc lại cấu hình đã đọc ở `viewDidLoad` của cùng lần trình bày |
| **First layout** | Decode ảnh nền theo pixel budget (§2.2) | Decode không có kích thước đích |
| **Sau `viewDidAppear`** | Việc trên main có trần cố định, không phụ thuộc dữ liệu user; đọc metadata pasteboard (không hỏi) | Decode/encode file có kích thước tùy user; truy vấn hệ thống lặp lại nhiều lần trong lúc gõ |
| **Mọi chặng** | Nhận nội dung pasteboard qua `UIPasteControl` khi user bấm | Tự đọc nội dung pasteboard (`string`, `items`, `itemProviders`…) |

### 2.1. Hiện trạng so với hợp đồng

Đã kiểm trong source. Phần lớn đã đúng; các chỗ lệch có đề xuất tương ứng.

**Đúng:**

- Pre-main: mọi product của `FunputKit` là `.library` không khai `type:`
  (`Packages/FunputKit/Package.swift`), nên được link tĩnh vào `Keyboard.appex`. Rust
  engine là `libfunput_ffi.a` trong `FunputCore.xcframework` (`Scripts/build-ffi.sh`),
  build với `lto = "fat"`, `codegen-units = 1`, `strip = "symbols"` (`Cargo.toml` ở
  gốc repo). Extension không import SwiftUI.
- `viewDidLoad` → `bootstrapKeyboard()`
  (`Keyboard/Controller/Activation/KeyboardViewController+Bootstrap.swift`): snapshot
  hit bỏ qua được custom theme catalog lẫn theme resolve; miss thì rơi về đường chậm
  và tự vá snapshot off-main ở `viewDidAppear`. Doc comment ở đó giải thích tại sao
  bootstrap không được nằm trong `viewWillAppear`: chạy trong animation là quá muộn,
  và trước khi height constraint sống thì Auto Layout kéo view lên 956pt rồi sập
  xuống 254pt. **Không chuyển việc từ `viewDidLoad` sang `viewWillAppear`.**
- Surface UIKit được dựng ở lần chạm đầu vào `keyboardView` lazy →
  `makePrimarySurface()`
  (`Keyboard/Controller/Surfaces/KeyboardViewController+PrimarySurfaceLifecycle.swift`).
  Đây là việc nặng nhất của bootstrap và *cố ý* nằm ở đây.
- Emoji, kaomoji và panel clipboard đều lazy (bảng §2.3).
- Bàn phím không đọc nội dung pasteboard ở bất kỳ đâu (A1).
  `ClipboardGateway` (`Packages/FunputKit/Sources/FunputShared/Clipboard/ClipboardGateway.swift`)
  chỉ có `snapshot()`. Nút dán trên thanh công cụ và dòng dán của panel cùng là
  `KeyboardClipboardChipView`
  (`Packages/FunputKit/Sources/KeyboardRenderer/Toolbar/KeyboardClipboardChipView.swift`),
  một `UIPasteControl`, nên dán không bao giờ bị hỏi.
  `Scripts/Quality/check-pasteboard-reads.sh` gác việc này trong CI.
- Mark đã dán nằm trong file riêng `clipboard-marks.json` (A2): `begin()` lúc +300ms
  chỉ đọc vài chục byte, không decode lịch sử
  (`Packages/FunputKit/Sources/FunputShared/Clipboard/ClipboardStoreMarks.swift`).
- Lưu clipboard vẫn decode và encode cả lịch sử trên main actor, nhưng lịch sử có trần
  cố định (A3, `ClipboardHistoryPolicy`): file xấu nhất khoảng 768 KB, không phụ thuộc
  user dán gì.
- Polling pasteboard lúc gõ (A4): mỗi tick 500ms chỉ hỏi `changeCount`, một truy vấn;
  đủ 4 trường metadata chỉ đọc khi số đó đổi
  (`Packages/FunputKit/Sources/FunputShared/Clipboard/ClipboardChangeMonitor.swift`).
- `viewWillAppear` đầu tiên dùng lại cấu hình `viewDidLoad` vừa đọc (A5,
  `bootstrappedSource`), nên mỗi lần trình bày chỉ đọc cấu hình một lần.
- `init` của controller không gọi xuống đĩa lần nào (A6): `ClipboardStore` bắt đầu là
  bản giữ chỗ, `KeyboardBootstrapSnapshotStore` là `lazy` và được tạo lần đầu trong
  `viewDidLoad`. Các store còn lại chỉ giữ handle.

**Lệch:**

| Chặng | Việc lệch | Đề xuất |
|---|---|---|

### 2.2. Ảnh nền: giữ decode đồng bộ (đã quyết định)

Ảnh nền decode đồng bộ trên main ở first layout
(`Keyboard/Configuration/KeyboardViewController+Background.swift`). Giữ nguyên, vì:

- chi phí có trần: decode theo pixel budget = width × scale, không phụ thuộc kích
  thước ảnh gốc; ảnh cũ được thả trước khi decode ảnh mới;
- nó xong trước khi bàn phím hiện, nên không đè lên phím đầu tiên;
- decode async sẽ làm bàn phím hiện ra không có nền rồi mới hiện nền: lỗi nhìn
  thấy được ở mọi lần mở, đổi lấy một cải thiện không nhìn thấy được.

### 2.3. Những gì đã trì hoãn

| Thành phần | Bắt đầu khi nào | Cơ chế | File |
|---|---|---|---|
| Emoji catalog (JSON 1898 mục) | Lần đầu bấm phím emoji | `EmojiCatalogCache.load()` lazy, `release()` khi disappear/memory warning | `Packages/FunputKit/Sources/KeyboardRenderer/Emoji/Catalog/EmojiCatalogCache.swift` |
| Emoji panel + search engine | Mở panel / bắt đầu tìm | Tạo khi vào, giải phóng khi rời (PR #642) | `Keyboard/Controller/Surfaces/` |
| Panel clipboard | Mở panel | Tạo khi vào, giải phóng khi rời | `Keyboard/Clipboard/KeyboardViewController+ClipboardPanel.swift` |
| Clipboard monitor (chỉ metadata) | 300ms sau `viewDidAppear` | `clipboardStartDelay` | `Keyboard/Clipboard/KeyboardViewController+ClipboardMonitoring.swift` |
| Từ điển sửa typo (lexicon mmap) | Sau activation | `Task.detached(priority: .utility)`; chưa sẵn sàng thì **từ chối sửa** (hướng an toàn) | `Packages/FunputKit/Sources/PersonalSuggestions/Correction/CorrectionWordSource.swift` |
| Personal suggestions engine + store | Sau activation | Serial queue riêng của `PersonalSuggestionWorker`; store mở off-main | `Keyboard/Suggestions/PersonalSuggestionWorker.swift` |
| Shortcuts (gõ tắt) | Sau activation | Actor async, có `cancel()` ở disappear | `Keyboard/Shortcuts/KeyboardViewController+Shortcuts.swift` |
| Vá bootstrap snapshot | `viewDidAppear` | `Task.detached(priority: .utility)` | `Keyboard/Configuration/KeyboardViewController+Configuration.swift` |

Đọc nội dung clipboard không nằm trong bảng: từ A1 bàn phím không bao giờ tự đọc,
kể cả khi đã trì hoãn.

---

## 3. Hiện trạng bộ nhớ

### 3.1. Số đo tham chiếu (PR #642, simulator iOS 27, Debug)

| Tình huống | Footprint |
|---|---|
| Bàn phím chữ lúc nghỉ | 21 MB |
| Lướt 6 nhóm emoji | 40 MB (đỉnh 43) |
| Về bàn phím chữ sau emoji | 36 MB |
| Đang tìm emoji | 44 MB (đỉnh 47) |

Hai ràng buộc đã biết, **không sửa được**, chỉ quản lý:

- **Cache ảnh emoji của CoreText không xoá được.** Mỗi emoji từng hiện để lại một
  ảnh đã giải mã trong cache tiến trình (`CopyEmojiImage`); đã thử thả font,
  descriptor riêng, `CGFont` riêng, không cách nào trả lại. Thứ duy nhất chọn được
  là strike 2x (~16 KB/emoji, gác bằng `EmojiMemoryBudgetTests` ở trần 28 KB/emoji).
  Xem hết catalog vẫn đọng ~30 MB cho đến khi process chết. Đây là tăng có trần,
  không phải leak, và là lý do **không được load sẵn emoji**.
- **Trần jetsam của keyboard extension thấp và tùy máy** (thấp hơn hẳn app thường;
  máy cũ thấp hơn máy mới). Nền 36 MB sau khi xem emoji là đủ gần trần trên máy RAM
  thấp, nên mọi vùng nhớ còn lại phải có trần nhỏ.

### 3.2. Vòng đời các vùng nhớ lớn

| Vùng nhớ | Trần | Thả khi nào |
|---|---|---|
| Primary surface | Cố định theo layout | Sống cùng controller |
| Controller cũ khi đổi app host | **Chưa có gì gác, xem §3.4 và A7** | Phải `deinit` khi host đổi |
| Emoji/kaomoji/clipboard panel | Theo panel | Rời panel nào thả panel đó; giữ cặp emoji↔kaomoji khi chuyển qua lại; `viewDidDisappear` thả hết |
| Emoji catalog đã decode | Nhỏ (model, không ảnh) | `viewDidDisappear`, memory warning |
| Ảnh nền theme | Pixel budget theo width | Memory warning khi đang ẩn (`releaseBackgroundImageIfHidden`) |
| Lexicon `en.lex` | mmap read-only, trang clean, OS tự thu hồi | Không cần thả |
| Personal suggestion store | `max_words: 5_000` (`crates/funput-suggestions/src/engine/config.rs`) | Flush định kỳ, engine sống trên queue riêng |
| Clipboard history | ≤ 50 mục; mỗi mục ≤ 16.384 UTF-16; tổng ≤ 262.144 UTF-16 kể cả pinned (`ClipboardHistoryPolicy`, A3) | Expiry theo cấu hình (pinned không bao giờ expire); vượt trần thì bỏ mục unpinned cũ nhất |

### 3.3. Memory warning

`didReceiveMemoryWarning` (`Keyboard/Suggestions/KeyboardViewController+SuggestionLifecycle.swift`):
flush suggestions → thả panel đang ẩn → thả emoji catalog → thả ảnh nền nếu bàn phím
đang ẩn → ghi `Footprint("MemoryWarning")`. An toàn khi mọi thứ còn nil (khởi động).

### 3.4. Leak: đã kiểm gì, còn hở gì

iOS dựng lại `KeyboardViewController` ở mỗi app host trong cùng một process. Nếu
controller cũ không được giải phóng, mỗi lần đổi app đọng lại nguyên một surface
UIKit cùng engine và các panel; footprint tăng dần cho đến khi bị jetsam. Đây là dạng
leak duy nhất có thể làm bàn phím chết "sau một lúc dùng".

Đã đọc trong source, **chưa thấy đường giữ controller nào**:

- Closure lưu lâu (`onCandidates`, `onSuggestionSelected`, closure của
  `ClipboardPasteRecorder`, các `Task` lặp) đều bắt `[weak self]`.
- Observer `NotificationCenter` dùng dạng selector, không giữ target.
- Timer của `BackspaceRepeatController` và `RunLoopDeadlineScheduler` giữ closure
  action; an toàn chừng nào nơi gọi bắt `weak` và `invalidate` khi tắt.
- Handle Rust được giải phóng trong `deinit` (`FunputComposer`,
  `PersonalSuggestionEngine`, `PersonalDictionarySnapshot`).

Nhưng **không có gì gác**: một closure bắt `self` mạnh thêm vào sau này sẽ không bị
phát hiện. Xem A7.

### 3.5. Signpost sẵn có

Subsystem `app.funput.keyboard`:

- Category `Launch` (`Keyboard/Diagnostics/KeyboardLaunchTrace.swift`): `ColdStart`,
  `Bootstrap`, `ConfigurationLoad`, `BootstrapSnapshot` (hit/fallback),
  `ThemeResolve`, `PrimarySurfaceCreation`, `SurfaceApply`, `EngineConfiguration`,
  `AssetRead`, `AssetDecode`, `HeightReady`, `FirstNonZeroLayout`, `PanelCreation`,
  `PanelFirstOpen`, `PanelReleased`; clipboard (`Keyboard/Diagnostics/ClipboardSignpost.swift`):
  `ClipboardStart`.
- Category `Memory` (`Keyboard/Diagnostics/Memory/KeyboardMemoryTrace.swift`): event
  `Footprint` với `physicalMB`/`peakMB`/`availableMB` tại các mốc `Appeared`,
  `Disappeared`, `PanelCreated/PanelReleased <surface>`, `MemoryWarning`.

```bash
log stream --predicate 'subsystem == "app.funput.keyboard" AND category == "Memory"'
```

---

## 4. Đề xuất

Mỗi mục đưa một chỗ lệch ở §2.1 hoặc §3.2 về đúng hợp đồng. Không mục nào cần đo
trước; nghiệm thu bằng test và kịch bản tay. Thứ tự A1 → A7, mỗi mục một PR. A1 đi
đầu vì nó xoá phần lớn luồng clipboard mà A2, A3, A4 sẽ sửa.

### A1: Bỏ tự đọc nội dung clipboard

**Vấn đề.** iOS hiện hộp thoại xin phép dán mỗi khi app đọc nội dung pasteboard mà
không qua `UIPasteControl`. Auto-capture đọc ở `SystemClipboardGateway.readText()`
(`UIPasteboard.general.string`), và luồng đó có ba lỗ, đã đối chiếu với code:

1. **Mỗi mục copy mới là một hộp thoại.** Universal Clipboard làm `changeCount` trên
   iPhone tăng ngay khi user copy trên Mac; monitor thấy thay đổi, chờ yên 2s rồi
   đọc. Vừa làm trên Mac vừa để bàn phím mở trên iPhone là mỗi lần copy bên Mac một
   hộp thoại bên iPhone.
2. **Từ chối hay đọc lỗi không được nhớ, nên hỏi lại cùng nội dung ở mọi app.** Bấm
   "Không cho phép" (hoặc tải từ Mac thất bại) làm `readText()` trả `nil`;
   `finishRead` chỉ gọi `fail()`, đặt `needsRetry` cho **controller hiện tại** và
   không ghi mark nào xuống đĩa
   (`Packages/FunputKit/Sources/FunputShared/Clipboard/ClipboardCaptureController+Read.swift`).
   Đổi app thì iOS dựng controller mới, `begin()` đọc lại mark cũ, lần lấy mẫu đầu
   của monitor luôn báo thay đổi, và bàn phím hỏi lại.
3. **"Yên 2s" xảy ra giữa câu.** `ClipboardIdleGate` coi 2s không bấm phím là rảnh;
   dừng 2s để nghĩ khi soạn tin là chuyện thường, nên hộp thoại bật lên lúc đang gõ.

Vá từng lỗ vẫn không hết lỗ 1: iOS không có API nào đọc nội dung pasteboard mà không
hỏi, ngoại lệ duy nhất là `UIPasteControl` khi user tự bấm. Luồng đọc tự động còn
là nguồn của độ trễ khi tải nội dung từ Mac và của spike bộ nhớ khi user copy văn
bản rất dài.

**Quyết định.** Bàn phím **không bao giờ tự đọc nội dung pasteboard**. Nội dung chỉ
đến qua `UIPasteControl`. Lịch sử clipboard ghi những gì user **dán qua Funput**,
không còn tự ghi mọi thứ user copy.

**Giữ nguyên:**

- Metadata (`ClipboardSnapshot`, đo được không hỏi trên máy thật) quyết định khi
  nào hiện nút dán; copy trên Mac thì nút vẫn hiện, vẫn phân biệt chữ hay link.
- Nút dán `UIPasteControl` trên thanh công cụ → `didPaste` → lưu vào lịch sử.
- `lastPastedChangeCount`: đã dán thì không mời dán lại cùng nội dung, kể cả ở app
  khác.
- Dán lại từ lịch sử bằng `insertText`, không chạm pasteboard.

**Xoá:**

- `ClipboardGateway.readText()` cùng hai bản hiện thực (`SystemClipboardGateway`,
  `TracedClipboardGateway` trong `Keyboard/Diagnostics/ClipboardSignpost.swift`) và
  mốc `ClipboardRead`.
- Cả file `ClipboardCaptureController+Read.swift`. Trong `ClipboardCaptureController`
  bỏ `synchronize`, `needsRetry`, `fail`, `processed`, `suppressed`,
  `suppressCurrentAfterClear`, `isReading`, `pending`, `readTask`, `resampleTask`.
  Phần còn lại (`begin`, `end`, `didPaste`, `lastPastedChangeCount`) chỉ ghi lại
  những gì user dán; đổi tên thành `ClipboardPasteRecorder` cho đúng vai trò.
- `ClipboardIdleGate`, lời gọi `clipboardIdleGate.noteActivity()` trên đường gõ
  (`Keyboard/Controller/KeyboardViewController+Input.swift`),
  `requestIdleClipboardCapture` và mốc `ClipboardCaptureDeferred`.
- Mark `lastCapturedChangeCount` (chỉ phục vụ auto-capture).
- Panel: lời gọi `clipboardCapture.synchronize()` khi mở, `onRetry` và
  `ClipboardRetryBanner`
  (`Packages/FunputKit/Sources/KeyboardRenderer/Clipboard/Components/ClipboardRetryBanner.swift`).
- Containing app: `pastePermissionGuide` trong `Funput/Settings/ClipboardSettingsCard.swift`
  (hướng dẫn đặt "Dán từ ứng dụng khác" thành "Cho phép"), vì không còn gì để hỏi.

**Thêm:**

1. **Dòng dán ở đầu panel clipboard.** Khi `ClipboardOfferPolicy` cho phép mời dán
   (cùng điều kiện với nút trên thanh công cụ: có chữ, chưa dán `changeCount` này,
   không phải ô password), panel hiện ở đầu danh sách một dòng "Nội dung vừa sao
   chép" kèm `UIPasteControl`. Bấm thì dán vào ô nhập và lưu vào lịch sử, y như nút
   trên thanh công cụ. Tách phần tạo và cấu hình `UIPasteControl` của
   `KeyboardClipboardChipView` ra một chỗ dùng chung. Ràng buộc của `UIPasteControl`:
   phải thật sự nhìn thấy được thì iOS mới không hỏi; chỉ chỉnh được màu nền, màu
   chữ, bo góc, và cấu hình chỉ có hiệu lực lúc tạo.
2. **Script gác** `Scripts/Quality/check-pasteboard-reads.sh`: tìm trong `Keyboard/`
   và `Packages/FunputKit/Sources/` mọi lần đọc nội dung `UIPasteboard`
   (`.string`, `.strings`, `.url`, `.urls`, `.image`, `.images`, `.color`, `.colors`,
   `.items`, `.itemProviders`, `.data(forPasteboardType:`, `.value(forPasteboardType:`,
   `.itemSet(`) và báo lỗi nếu có. Metadata (`changeCount`, `hasStrings`, `hasURLs`,
   `hasImages`, `contains(pasteboardTypes:)`, `types`) được phép. Containing app
   (`Funput/`) nằm ngoài phạm vi: harness UI test ghi vào pasteboard. Chạy trong job
   `shell-line-budget` của `.github/workflows/ci.yml`, thêm một step, không đổi tên
   job (tên job là required check).
3. **Chữ trên giao diện**, viết câu tự nhiên, không dùng `·` hay `—`:
   - `ClipboardSettingsCard` summary: "Lưu những gì bạn dán bằng Funput, ngay trên
     thiết bị."
   - `ClipboardEmptyStateView`, trạng thái `.nothingSaved`: "Bấm Dán trên thanh công
     cụ của Funput, nội dung sẽ được lưu vào đây."

**Đã đổi khi hiện thực.**

- Dòng dán của panel **dùng lại nguyên `KeyboardClipboardChipView`** thay vì tách
  riêng phần tạo `UIPasteControl`: cùng một view, cùng theme, không có code control
  thứ hai. Chữ trên dòng vì thế là chữ của chip ("Đã sao chép văn bản" / "Đã sao
  chép một liên kết"), không phải "Nội dung vừa sao chép". Dòng nằm ở chỗ của
  `ClipboardRetryBanner` cũ. Dán từ dòng này thì quay về bàn phím chữ, giống dán từ
  lịch sử. Toolbar và panel nhận cùng một hint từ `refreshClipboardOffer`, nên panel
  không bao giờ mời một lần dán mà toolbar đã rút lại.
- `UIPasteControl` báo `intrinsicContentSize` bằng 0 cho tới khi nó tự layout một
  lượt trên màn hình, mà lượt đó chạy sau lượt của chip. Toolbar được layout lại liên
  tục nên không lộ; panel thì được dựng khi đang ẩn và chỉ layout một lần, nên nút
  Dán co về 0 và biến mất (UI test bắt được, máy ảo iPhone 17 Pro Max iOS 27.2).
  `KeyboardClipboardChipView` vì thế xin thêm tối đa 3 lượt layout khi còn đọc được
  kích thước 0, và `ClipboardKeyboardView` gọi `pasteChip.remeasure()` khi chuyển từ
  ẩn sang hiện. Unit test không kiểm được chuyện này: không có app host thì mọi
  `UIPasteControl` đều báo kích thước 0.
- Chữ trống của panel thành "Nội dung bạn dán bằng nút Dán của Funput sẽ được lưu ở
  đây.", vì panel giờ cũng có nút Dán của riêng nó.
- `ClipboardStore.capture(_:now:)` đổi tên thành `record(_:now:)`. Khái niệm "capture"
  không còn.
- `allowsClipboardCapture` đổi thành `allowsClipboardOffer` ngay trong A1, không chờ
  tới A4. File `KeyboardViewController+ClipboardCapture.swift` đổi tên thành
  `+ClipboardMonitoring.swift`.
- Test mới: `ClipboardPasteRecorderTests`, `ClipboardPasteFixture`,
  `ClipboardRecordStoreTests` (thay `ClipboardCaptureStoreTests`),
  `ClipboardPastePresentationTests`. UI test đổi tên thành
  `FunputUITests/ClipboardPasteUITests.swift`.

**Nghiệm thu.**

- `check-pasteboard-reads.sh` xanh trên nhánh, và đỏ nếu thêm thử một dòng
  `UIPasteboard.general.string` vào `Keyboard/`.
- Test `FunputShared` cho `ClipboardPasteRecorder`: dán thì lưu; dán cùng
  `changeCount` hai lần thì lưu một lần; `lastPastedChangeCount` sống qua phiên mới;
  mời dán bị tắt sau khi đã dán. Xoá `ClipboardIdleGateTests`, viết lại
  `ClipboardCaptureControllerTests`, `ClipboardCaptureLifecycleTests`,
  `ClipboardCaptureFixture`, `ClipboardCapturePresentationTests` theo luồng mới.
- UI test `FunputUITests/ClipboardCaptureUITests.swift` viết lại: harness copy →
  nút dán hiện → không có hộp thoại xin phép dán nào; nếu XCUITest bấm được
  `UIPasteControl` trong bàn phím thì kiểm cả việc mục vào lịch sử
  (`ClipboardHarnessControls` hiện "Saved").
- Kịch bản tay trên máy thật: để bàn phím mở và gõ, copy 10 lần trên Mac → **không
  có hộp thoại nào**; nút dán hiện ra; bấm thì nội dung từ Mac được dán và có trong
  lịch sử; mở panel không có hộp thoại, dòng dán ở đầu hoạt động; đổi qua app khác
  không có hộp thoại.

**Đánh đổi (đã chấp nhận).**

- Thứ user dán bằng cách khác (menu giữ ngón tay của iOS, nút dán của app host,
  Cmd+V trên bàn phím rời) không vào lịch sử Funput.
- Thứ copy từ Mac mà chưa dán qua Funput thì không được giữ lại. Thực tế phần mất
  nhỏ: auto-capture hiện nay cũng chỉ thấy mục đang có lúc bàn phím hiện hoặc copy
  trong lúc bàn phím mở.

### A2: Tách mark đã dán ra khỏi lịch sử clipboard

**Vấn đề.** 300ms sau khi hiện, `begin()` đọc mark qua `ClipboardStore.read()`, tức
decode **toàn bộ** `clipboard.json` trên main chỉ để lấy một số nguyên. Chi phí tỷ
lệ với lịch sử của user, rơi đúng lúc user bắt đầu gõ. Sau A1 chỉ còn một mark là
`lastPastedChangeCount`, kèm `bootTime`.

**Thiết kế.**

1. `lastPastedChangeCount` và `bootTime` chuyển sang file riêng
   `clipboard-marks.json` trong cùng thư mục, cùng protection class
   (`completeFileProtectionUntilFirstUserAuthentication`); thư mục đã set
   `isExcludedFromBackup`. File cỡ vài chục byte.
2. `lastPastedChangeCount()` và `markPasted(_:)` chỉ đụng file marks. Lưu một mục
   mới chỉ ghi `clipboard.json` khi danh sách mục thật sự đổi.
3. `clear()` xoá cả hai file, giữ đúng ngữ nghĩa hiện tại: sau khi xoá, không file
   nào nói đã thấy pasteboard này.
4. Migration: không thấy file marks thì đọc mark từ `Payload` cũ trong
   `clipboard.json` **một lần**, ghi sang file marks. Field mark trong `Payload` giữ
   ở dạng optional để decode được file cũ, không ghi nữa. Mark là dữ liệu phụ: mất
   thì chỉ có nghĩa nút dán được mời lại một lần.

**Nghiệm thu.** Test `FunputShared`: `lastPastedChangeCount()` trả đúng giá trị khi
`clipboard.json` không tồn tại hoặc hỏng; mark đọc được từ file cũ sau migration;
`clear()` xoá cả mark; mark của boot trước vẫn bị bỏ (`marksAreCurrent`); các test
còn lại trong `ClipboardStoreTests` và `ClipboardRecordStoreTests` vẫn xanh.

**Đã đổi khi hiện thực.**

- `clear()` ghi một file marks rỗng thay vì xoá nó: xoá thì lần đọc sau lại đi tìm
  mark cũ trong `clipboard.json` để migrate. Ngữ nghĩa không đổi, sau khi xoá không
  file nào nói đã thấy pasteboard này.
- Mỗi lần ghi lịch sử (`write(_:)`) xoá luôn hai field mark cũ khỏi `clipboard.json`,
  nên file cũ tự sạch ở lần ghi kế tiếp.
- `ClipboardMarks` là struct riêng (`ClipboardMarks.swift`), giữ `stampThisBoot()` và
  `marksAreCurrent` chuyển từ `Payload` sang. Hai file dùng chung
  `ClipboardStore.persist(_:to:)` để cùng protection class.
- Test: `ClipboardMarksFileTests` (đọc mark khi lịch sử hỏng, đánh dấu không ghi lại
  lịch sử, migrate một lần, ghi lịch sử bỏ mark cũ, xoá không migrate lại).

### A3: Trần dung lượng lịch sử clipboard

**Vấn đề.** `ClipboardItem.text` không giới hạn độ dài; pinned không giới hạn số
lượng và không bao giờ expire. Mỗi lần lưu đọc + decode toàn bộ file, encode lại,
ghi, trên main actor, chi phí tạm thời gấp 2–3 lần kích thước payload. Panel giữ
toàn văn mọi mục, và `ClipboardRowText.preview` chạy `split` + `joined` trên **toàn
bộ** text trước khi cắt còn 120 ký tự.

**Thiết kế.**

1. **Trần đo bằng UTF-16 code unit**, hằng số trong `ClipboardHistoryPolicy` mới
   (`Packages/FunputKit/Sources/FunputShared/Clipboard/`):
   - `maxItemUTF16 = 16_384` mỗi mục;
   - `maxTotalUTF16 = 262_144` cho cả lịch sử, **tính cả mục pinned**.

   Dùng `text.utf16.count` chứ không dùng `String.count`: chuỗi `NSString` bridge
   sang có `utf16.count` O(1), còn `count` phải duyệt hết chuỗi để đếm grapheme, trên
   main. Tiếng Việt dựng sẵn mỗi ký tự là 1 unit UTF-16 và tối đa 3 byte UTF-8, nên
   file xấu nhất khoảng 768 KB và chi phí decode có trần cố định.
2. **Vượt trần mỗi mục thì vẫn dán, chỉ không lưu vào lịch sử**, kiểm ở
   `ClipboardPasteRecorder.didPaste` trước khi gọi `save` (mark đã dán vẫn ghi). Không
   cắt bớt rồi lưu: dán lại từ lịch sử ra văn bản bị cụt là mất dữ liệu âm thầm, tệ
   hơn không có mục đó.
3. **Ngân sách tổng trong `prune`**: sau khi lọc expiry và giới hạn 50, nếu tổng
   `utf16.count` vượt `maxTotalUTF16` thì bỏ mục unpinned cũ nhất cho đến khi vừa.
   Nếu chỉ còn mục pinned mà mục mới vẫn không vừa thì không lưu mục mới, giữ nguyên
   mục pinned (pin là ý định rõ ràng của user, không tự xoá). `prune` cũng lọc bỏ
   mục cũ có `utf16.count > maxItemUTF16`, kể cả pinned, vì chúng sinh ra trước khi
   có trần. `prune` chạy trong mọi `load`/`capture` nên file cũ tự sạch.
4. **Preview cắt trước, gộp khoảng trắng sau**: `ClipboardRowText.preview`
   (`Packages/FunputKit/Sources/KeyboardRenderer/Clipboard/ClipboardRowText.swift`)
   lấy `prefix(previewLimit * 4)` trước rồi mới `split`/`joined`; kết quả không đổi
   với mọi text ngắn.

**Nghiệm thu.**

- Test `FunputShared`: mục đúng trần được lưu; vượt trần 1 unit thì không lưu nhưng
  mark đã dán vẫn ghi; tổng vượt ngân sách thì mục unpinned cũ nhất bị bỏ; pinned
  chiếm hết ngân sách thì mục mới không được lưu; `load` trên file chứa mục quá khổ
  (pinned lẫn không) trả về danh sách đã lọc.
- Test `KeyboardRenderer`: preview của text ngắn không đổi; preview của text 1 triệu
  ký tự đúng 120 ký tự + "…".
- Kịch bản tay: copy chuỗi > 16.384 ký tự, dán bằng nút của Funput → văn bản vào ô
  nhập đầy đủ, lịch sử không có mục mới.
- Cập nhật dòng clipboard ở §3.2 trong cùng PR.

**Đã đổi khi hiện thực.**

- Toàn bộ luật cắt tỉa (expiry, 50 mục, trần mỗi mục, ngân sách tổng) dồn vào
  `ClipboardHistoryPolicy.prune`; `ClipboardStore.prune` chỉ còn truyền điều kiện
  expiry. Store là nơi quyết định cuối: `record(_:)` trả `false` khi mục không được
  giữ (quá dài, hoặc không vừa cạnh các mục pinned) và để nguyên lịch sử. Recorder
  kiểm `admits` trước chỉ để khỏi decode lịch sử cho một mục chắc chắn bị từ chối.
- Mục pinned không bao giờ bị tự xoá vì ngân sách, nhưng trần vẫn giữ được: mục chỉ
  vào lịch sử qua `record`, mà `record` từ chối mục không vừa, còn ghim một mục có sẵn
  không làm tổng tăng. Chỉ file cũ (viết trước khi có trần) mới có thể vượt, và các
  mục quá dài trong đó bị lọc ngay ở lần `load` đầu tiên.
- Preview cắt `prefix(previewLimit * 4)` trước khi gộp khoảng trắng, và vẫn thêm "…"
  khi phần đầu đã gộp ngắn hơn giới hạn nhưng clip còn tiếp.
- Test: `ClipboardHistoryPolicyTests`, thêm ca mới trong `ClipboardPasteRecorderTests`
  và `ClipboardRowTextTests`.

### A4: Polling metadata pasteboard chỉ đọc `changeCount`

**Vấn đề.** `ClipboardChangeMonitor`
(`Packages/FunputKit/Sources/FunputShared/Clipboard/ClipboardChangeMonitor.swift`)
cứ 500ms gọi `ClipboardSnapshot(.general)` trên main: 4 truy vấn pasteboard, ~3ms
mỗi lần trên máy thật, suốt lúc user gõ. Một tick rơi đúng lúc bấm phím là phím đó
chờ. Polling vẫn cần cho nút dán, vì `UIPasteboard.changedNotification` không báo
thay đổi do process host làm.

**Thiết kế.**

1. Monitor nhận thêm closure `readChangeCount: () -> Int?`. Mỗi tick chỉ đọc
   `changeCount` (1 truy vấn). Đọc đủ snapshot khi: tick đầu tiên sau `start`;
   `changeCount` khác lần trước; hoặc lần trước `isIndeterminate` (iOS từ chối trả
   lời, xem doc comment của `ClipboardSnapshot`; phải hỏi lại cho tới khi có câu trả
   lời thật).
2. Đường notification (`clipboardChanged`) và `refresh()` vẫn đọc đủ snapshot.
3. `readChangeCount` trả nil cùng điều kiện với `readSnapshot`
   (`allowsClipboardOffer`), để ô password không truy vấn pasteboard.

**Nghiệm thu.** Test `FunputShared` với nguồn metadata giả đếm số lần đọc: 10 tick
không đổi `changeCount` thì đọc snapshot đúng 1 lần; `changeCount` đổi thì
`onChange` gọi đúng 1 lần; snapshot indeterminate thì tick sau đọc lại đủ.

**Đã đổi khi hiện thực.**

- `readChangeCount` là tham số tuỳ chọn của `start`; thiếu nó thì mỗi tick đọc đủ
  snapshot như trước, nên các test monitor cũ giữ nguyên. Tick là hàm riêng `poll()`;
  `sample()` (đọc đủ) vẫn dùng cho lúc start, `refresh()` và notification.
- Probe trả nil (mất quyền, ô password) thì tick rơi về `sample()`, đúng như khi
  `readSnapshot` trả nil: lời mời dán bị gỡ, và lấy lại quyền thì đọc lại đủ.
- Test mới nằm ở file riêng `ClipboardChangeMonitorPollingTests.swift`, vì file test
  monitor cũ đã sát giới hạn 150 dòng.

### A5: Bỏ lần đọc activation source thừa

**Vấn đề.** §2.1: `loadActivationSource` chạy 2 lần mỗi cold start, lần hai nằm
trong animation trình bày.

**Thiết kế.** Controller giữ `var bootstrappedSource: KeyboardActivationSource?`;
`bootstrapKeyboard()` gán nó sau khi load; `activateKeyboardForAppearance()` lấy ra
(gán lại `nil`) và dùng nếu còn, các lần activation sau load như hiện tại. Giữa
`viewDidLoad` và `viewWillAppear` đầu tiên của cùng một lần trình bày user không đổi
được settings, nên dùng lại là an toàn. `hasFullAccess` đã nằm trong source.

**Nghiệm thu.** Signpost `ConfigurationLoad` xuất hiện đúng **một** lần trong một
cold start (`log stream` category `Launch`); đổi settings trong containing app rồi
quay lại host thì bàn phím nhận cấu hình mới.

**Đã đổi khi hiện thực.**

- Chỉ dùng lại khi còn "tươi": `KeyboardBootstrappedSource` giữ thời điểm đọc, và
  `viewWillAppear` chỉ nhận nếu đến trong vòng 1 giây (từ `Bootstrap` tới
  `viewDidAppear` đo được ~140ms, `viewWillAppear` còn sớm hơn). Chậm hơn nghĩa là controller đã nằm sẵn một lúc,
  settings có thể đã đổi, nên đọc lại. Thiết kế ban đầu không có chốt này.
- Đo trên máy ảo iPhone 17 Pro Max iOS 27.2 bằng
  `xcrun simctl spawn <udid> log stream --signpost` (category `Launch`; trong zsh phải
  gọi `/usr/bin/log`, vì `log` trùng tên một builtin): một cold start có đúng một cặp
  begin/end `ConfigurationLoad`, nằm trong `Bootstrap`.
- Về Home rồi mở lại app thì iOS dựng controller mới trong cùng process, nên lần
  trình bày đó đọc cấu hình mới ở `viewDidLoad`, cũng chỉ một lần. Nhánh
  `?? loadActivationSource` giữ cho trường hợp cùng một controller hiện lại mà không
  qua `viewDidLoad`, hoặc source đã hết tươi.
- `KeyboardHeightRefreshUITests.testWarmConfigurationRefreshMatchesKeyboardCycle`
  đỏ trên máy ảo này, nhưng đỏ y hệt ở commit trước A1 (code `main`), nên đây là lỗi
  có sẵn, không do A5. Cần điều tra riêng.

### A6: Bỏ lần tạo `ClipboardStore` thừa

**Vấn đề.** §2.1: property initializer `ClipboardStore()` làm syscall đĩa ở `init`,
rồi `adoptConfiguration` trong bootstrap tạo lại ngay.

**Thiết kế.** Đổi property initializer thành `ClipboardStore(directory: nil)` (không
syscall). `adoptConfiguration` luôn chạy trong `bootstrapKeyboard()` trước mọi thao
tác clipboard và thay store thật vào, nên không đường nào dùng được bản placeholder.
Comment tại property ghi rõ bất biến đó.

**Nghiệm thu.** Breakpoint/log trong `AppGroupDirectory.prepare(named: "Clipboard")`:
không chạy trong `init`, đúng một lần mỗi cold start; test `ClipboardStore` hiện có
vẫn xanh.

**Đã đổi khi hiện thực.**

- Rà lại mọi property của controller thì thấy thêm `KeyboardBootstrapSnapshotStore()`
  cũng gọi `AppGroupDirectory.prepare` trong `init`; §2.1 trước đây bỏ sót. Store đó
  được chuyển thành `lazy var`: `viewDidLoad` đọc snapshot ngay nên chi phí chỉ dời
  sang chặng hợp đồng cho phép, nhưng `init` giờ không còn syscall đĩa nào. Các store
  khác (`FunputConfigurationStore`, `CustomThemeStore`, `KeyboardAccessStateStore`,
  `EmojiRecentsStore`) chỉ tạo `UserDefaults(suiteName:)`; `ThemeAssetStore` chỉ tra
  `containerURL`, không ghi.
- `adoptConfiguration` có comment ghi bất biến: lần gọi đầu luôn chạy tới chỗ tạo
  store thật, vì `adoptedIdentity` bắt đầu là nil.
- Không đặt breakpoint: bất biến đọc thẳng được trong code (một chỗ tạo store thật,
  luôn chạy ở bootstrap), và các UI test clipboard (dùng store thật) vẫn xanh.

### A7: Gác leak controller

**Vấn đề.** §3.4: chưa thấy leak nhưng không có gì phát hiện leak.

**Thiết kế.**

1. Bộ đếm controller đang sống: `init` tăng, `deinit` giảm. `deinit` nonisolated nên
   bộ đếm dùng `OSAllocatedUnfairLock` (hoặc atomic), không dựa vào main actor.
2. Thêm trường `controllers=<n>` vào event `Footprint` của `KeyboardMemoryTrace`.
3. 2 giây sau `viewDidAppear`, nếu số controller đang sống > 1 thì ghi log mức
   `fault` (mọi build, không assert). iOS có thể dựng controller mới trước khi thả
   controller cũ, nên chồng lấn ngắn là bình thường; quá 2 giây mới là leak.

**Nghiệm thu.** Kịch bản tay: đổi qua lại giữa 3 app, mở bàn phím ở mỗi app, tổng
20 lần, xen kẽ mở emoji và clipboard panel. Mọi `Footprint("Appeared")` có
`controllers=1`; không có log `fault`; Memory Graph của Xcode không còn instance
`KeyboardViewController` nào ngoài cái đang hiện.

---

## 5. Kịch bản dùng thử TestFlight

Sau khi A1–A7 vào bản TestFlight, dùng vài ngày theo các thói quen sau, ghi lại mọi
lần bàn phím hiện chậm, khựng ở phím đầu, có hộp thoại bật lên, hoặc bị thay bằng
bàn phím hệ thống:

- mở bàn phím ngay sau khi chuyển app, gõ ngay không chờ;
- vừa làm trên Mac vừa gõ trên iPhone, copy nhiều lần trên Mac rồi dán bằng nút
  của Funput;
- copy văn bản dài (cả trang web, tài liệu) rồi dán;
- dùng emoji nhiều lần trong ngày, xen với gõ chữ;
- đổi qua lại nhiều app trong một phiên dài;
- giữ vài mục pinned trong lịch sử clipboard.

Khi gặp sự cố, ghi giờ, app, thao tác vừa làm; nếu có thể thì lấy file
`JetsamEvent-*.ips` hoặc crash log của process `Keyboard` trong *Cài đặt > Quyền
riêng tư & Bảo mật > Phân tích & Cải tiến > Dữ liệu phân tích*.

---

## 6. Ràng buộc cho agent hiện thực

- Làm theo thứ tự A1 → A7, mỗi mục một PR riêng, nhỏ, kèm cập nhật tài liệu này
  (bảng §2.1 và §3.2 phải khớp code sau mỗi PR).
- Không thêm việc nào vào một chặng của §2 mà bảng không cho phép; nếu bắt buộc
  phải thêm, sửa bảng §2 trong cùng PR và ghi lý do.
- Không thêm lần đọc nội dung pasteboard nào; `check-pasteboard-reads.sh` gác việc
  này sau A1.
- Giữ mọi signpost hiện có; thêm mốc mới nếu đường code mới cần quan sát.
- Hai script giới hạn kích thước:
  - `Scripts/check-swift-loc.sh` (chạy trong CI): mỗi file Swift ≤ 150 dòng.
  - `Scripts/Quality/check-keyboard-touch-layout.sh` (chưa chạy trong CI): thêm giới
    hạn ≤ 5 file Swift mỗi thư mục trong các thư mục nó liệt kê, gồm
    `Keyboard/Controller`, `Keyboard/Diagnostics`, `Keyboard/Document`,
    `Packages/FunputKit/Sources/KeyboardInput`,
    `Packages/FunputKit/Sources/FunputShared/Persistence`. Đầy thì tách thư mục con.
    Lúc viết, script này đã đỏ sẵn ở hai thư mục test
    (`Packages/FunputKit/Tests/KeyboardInputTests/Coordinator`,
    `FunputTests/Suggestions/Service`); PR mới không được thêm vi phạm nào.

  Tách file theo đúng phong cách extension hiện tại. File Swift mới copy bộ `import`
  từ file cùng thư mục (Swift 6 `MemberImportVisibility` bắt lỗi cứng khi thiếu
  import).
- Code và comment bằng tiếng Anh; comment giải thích *ràng buộc* (tại sao phải thế),
  không kể lại code làm gì, theo đúng giọng các doc comment đang có. Chữ trên giao
  diện bằng tiếng Việt, câu tự nhiên, không dùng `·` hay `—`.
- Giữ các bất biến ở §7; không đổi hành vi secure field, không nới gate riêng tư nào.
- Chạy lại kịch bản kiểm chứng của PR #642 (mở emoji, lướt nhóm, tìm kiếm, về bàn
  phím chữ) và kịch bản đổi app của A7 sau bất kỳ thay đổi nào đụng vòng đời
  controller, panel hoặc clipboard.

---

## 7. Bất biến về ngữ cảnh ô nhập liệu (audit 11/2026)

Kết luận audit: **đủ các trường hợp chuẩn của `UIInputViewController`, không tìm
thấy đường crash còn hở.** Giữ các bất biến sau khi sửa bất kỳ gì quanh đây:

- **Mọi trait từ proxy đều nil-safe**: `keyboardType ?? .default`,
  `returnKeyType ?? .default`, `isSecureTextEntry ?? false`,
  `autocapitalizationType ?? .sentences` (`Keyboard/Document/KeyboardDocumentWriter+Traits.swift`).
- **`documentIdentifier` phải đọc qua ObjC runtime** (`Keyboard/Document/KeyboardDocumentWriter.swift`):
  header của Apple khai non-null nhưng host trả nil trước khi document bind; bridge
  thẳng sang `UUID` đã từng `EXC_BREAKPOINT` giữa lúc launch ngoài thực địa (#606),
  extension chết và host vẽ bàn phím trống. Đây là mẫu cho mọi API proxy "nói dối
  nullability" về sau.
- **Secure field** (`isSecureTextEntry`) → `.password`/`.pin`
  (`Packages/FunputKit/Sources/KeyboardInput/State/KeyboardInputContext.swift`): tắt
  gõ tiếng Việt, tắt học từ (`suggestionTrackingActive` gate trên
  `supportsVietnameseComposition`), tắt autocorrect, tắt key preview/smart gesture,
  chặn cả mời dán lẫn lưu clipboard. Không được nới bất kỳ cái nào trong số này.
- **Callback đến sớm chịu được**: `textWillChange`/`textDidChange` trước
  `viewWillAppear` (`applyInputPresentation` guard `cachedThemedPresentation` nil,
  clipboard refresh guard `isClipboardMonitoring`); phím bấm trước khi document bind
  (`commit` luôn `synchronizeBeforeInput` trước).
- Keypad riêng cho `numberPad`/`decimalPad`/`phonePad`/`namePhonePad`/`.pin`;
  `oneTimeCode`/`username`/`newPassword`… chặn autocorrect qua
  `CorrectionFieldPolicy`
  (`Packages/FunputKit/Sources/KeyboardInput/Correction/Policy/CorrectionFieldPolicy.swift`);
  selection đang có thì không tính viết hoa, không dedupe echo, không re-open từ.

Nguồn crash còn lại trong code: `preconditionFailure` khi `funput_engine_new()` trả
null (chỉ khi malloc fail, chấp nhận) và các `init(coder:)` boilerplate không bao
giờ chạy. Không có `try!`/`as!` trên đường khởi động.

Tồn đọng không-crash, ngoài phạm vi tài liệu này: field `.asciiCapable` hiện rơi vào
`.text` nên vẫn gõ được tiếng Việt dù field tuyên bố chỉ muốn ASCII (và Info.plist
khai `IsASCIICapable = true`). Cần một quyết định sản phẩm riêng.
