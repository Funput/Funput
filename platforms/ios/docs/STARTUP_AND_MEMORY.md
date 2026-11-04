# Khởi động & bộ nhớ của Keyboard extension iOS

> **Trạng thái:** Hiện trạng đã audit xong (11/2026) — các đề xuất P1–P3 chờ hiện thực
>
> **Cập nhật:** 04/11/2026
>
> **Phạm vi:** Cold start, vòng đời bộ nhớ và xử lý ngữ cảnh ô nhập liệu của
> `Keyboard` extension. Không bàn containing app, Rust engine nội bộ hay theme store.

Tài liệu này là nơi các quyết định về khởi động và bộ nhớ sống, theo cùng quy ước với
[english-lexicon-suggestion.md](../../../docs/features/english-lexicon-suggestion.md):
**mỗi thay đổi hành vi sau này cập nhật lại tài liệu trong cùng PR**; chỗ nào hiện
thực lệch khỏi bản viết thì sửa lại và đánh dấu **Đã đổi khi hiện thực**.

Bối cảnh: [issue #641](https://github.com/Funput/Funput/issues/641) — Funput bị thay
bằng bàn phím hệ thống trên iOS 26/27. [PR #642](https://github.com/Funput/Funput/pull/642)
đã xử lý nguyên nhân chính (jetsam vì bộ nhớ emoji). Triệu chứng còn lại theo người
báo lỗi: *ngẫu nhiên hiện bàn phím gốc ngay lúc chạm vào ô nhập liệu* — tài liệu này
chốt bức tranh hiện trạng và các việc còn lại.

---

## 1. Nguyên tắc kiến trúc

1. **Frame đầu tiên là bất khả xâm phạm.** Mọi thứ cần cho frame đầu (layout, theme,
   surface) được giải quyết trong `viewDidLoad`, *trước* animation trình bày của hệ
   thống. Mọi thứ không cần cho frame đầu bị đẩy ra sau — emoji, clipboard, từ điển,
   gợi ý, shortcuts đều **không được phép** đứng trên đường này.
2. **Extension chết là cold start đắt nhất.** Keyboard extension bị jetsam không báo
   trước khi vượt trần bộ nhớ (thực nghiệm trên simulator iOS 27: kill process
   `Keyboard` thì iOS thay ngay bằng bàn phím hệ thống). Vì vậy *trần bộ nhớ của mọi
   thành phần phải tính được bằng số* và có đo đạc gác lại.
3. **Mọi chặng phải đo được.** Signpost có sẵn cho từng chặng khởi động và từng mốc
   bộ nhớ; thay đổi nào vào các đường này phải giữ hoặc thêm signpost, không bớt.

---

## 2. Hiện trạng cold start — timeline đã xác minh

Trình tự một lần khởi động nguội (process mới):

```
process spawn + dyld                      ← phần lớn thời gian, ngoài tầm code Swift
  └─ KeyboardViewController init          ← property initializers (xem 2.1)
      └─ loadView                         ← chỉ gán KeyboardAudioInputView
          └─ viewDidLoad                  ← bootstrapKeyboard() (xem 2.2)
              └─ textWillChange           ← traits của field, trước viewWillAppear
                  └─ viewWillAppear       ← activateKeyboardForAppearance() (xem 2.3)
                      └─ first layout     ← decode ảnh nền theo width thật
                          └─ viewDidAppear← mọi thứ trì hoãn bắt đầu (xem 2.4)
```

### 2.1. Controller `init` — property initializers

Chạy trên main thread trước cả `viewDidLoad`. Hiện trạng
(`Keyboard/Controller/KeyboardViewController.swift`):

| Việc | Chi phí | Đánh giá |
|---|---|---|
| `KeyboardInputCoordinator()` → `funput_engine_new()` | Rust engine rule-based, không load từ điển | OK |
| `ClipboardStore()` → `AppGroupDirectory.prepare` | `createDirectory` + `setAttributes` + `setResourceValues` — 3–4 syscall đĩa | **Lệch nguyên tắc 1 — xem P3** |
| Các store còn lại | Chỉ giữ handle (`UserDefaults(suiteName:)`, URL) | OK |
| `keyboardView`, `clipboardCapture` | `lazy` — chưa chạy | OK |

### 2.2. `viewDidLoad` → `bootstrapKeyboard()`

`Keyboard/Controller/Activation/KeyboardViewController+Bootstrap.swift` — có doc
comment giải thích tại sao bootstrap nằm ở đây chứ không phải `viewWillAppear`:
mọi thứ resolve trong `viewWillAppear` là *quá muộn* vì nó chạy bên trong animation,
và trước khi height constraint sống thì Auto Layout kéo view lên 956pt rồi sập xuống
254pt trong ~500ms đầu. **Không chuyển việc từ `viewDidLoad` sang `viewWillAppear`.**

Các bước, theo thứ tự:

1. `loadActivationSource(hasFullAccess:)` — đọc **bootstrap snapshot** (một file nhỏ
   trong App Group, `KeyboardBootstrapSnapshotStore`). Snapshot hit thì bỏ qua được
   cả việc load custom theme catalog lẫn theme resolve; miss thì rơi về đường chậm
   (`configurationStore.load()` + `customThemeStore.load()` + `ThemeResolve`) và xếp
   lịch tự vá snapshot ở `viewDidAppear`.
2. `adoptConfiguration(source)` — guard bằng `KeyboardActivationIdentity`: cấu hình
   và theme không đổi thì giữ nguyên presentation đã build. Khi chạy (cold start luôn
   chạy lần đầu) nó build `cachedThemedPresentation` và **tạo lại `ClipboardStore`**
   theo expiry trong cấu hình (lần `prepare()` thứ hai — xem P3).
3. `inputCoordinator.apply(source.configuration)` — đẩy cấu hình vào engine.
4. `applyTextInputTraits(force: true)` — resolve traits với giá trị mặc định (field
   chưa bind).
5. `applyAdoptedPresentation()` → `applyInputPresentation()` — lần chạm đầu vào
   `keyboardView` lazy → **`makePrimarySurfaceCreation`**: build toàn bộ surface UIKit.
   Đây là việc nặng nhất của bootstrap và *cố ý* nằm ở đây.
6. `activatePreferredHeightForBootstrap()` — height constraint sống trước animation.

### 2.3. `textWillChange` → `viewWillAppear`

- `textWillChange` đến **trước** `viewWillAppear` và chỉnh traits theo field thật
  (`Keyboard/Controller/KeyboardViewController+Traits.swift`). Mọi đường ở đây đều có
  guard chịu được việc chạy trước khi bootstrap xong (xem §4).
- `viewWillAppear` → `activateKeyboardForAppearance()`
  (`Keyboard/Controller/Activation/KeyboardViewController+Activation.swift`):
  - `loadActivationSource` **lần thứ hai** — tồn tại cho warm re-activation
    (settings có thể đổi giữa hai phiên cùng process), nhưng ở cold start là một lần
    đọc đĩa + decode thừa ngay trong animation — **xem P2**;
  - `synchronizeDocument(event: .activated)` — IPC với host, bắt buộc (tính Shift
    từ `documentContextBeforeInput`);
  - `activateShortcuts()` — async qua actor, không chặn;
  - `configurePersonalSuggestions(...)` — toàn bộ trên queue riêng (xem §2.4);
  - `showFunput()` — panel mở ở host trước không được lộ sang host sau.

### 2.4. Những gì đã trì hoãn — giữ nguyên cơ chế

| Thành phần | Bắt đầu khi nào | Cơ chế | File |
|---|---|---|---|
| Emoji catalog (JSON 1898 mục) | Lần đầu bấm phím emoji | `EmojiCatalogCache.load()` lazy, `release()` khi disappear/memory warning | `KeyboardRenderer/Emoji/Catalog/EmojiCatalogCache.swift` |
| Emoji panel + search engine | Mở panel / bắt đầu tìm | Tạo khi vào, giải phóng khi rời (PR #642) | `Keyboard/Controller/Surfaces/` |
| Clipboard monitor | **300ms sau `viewDidAppear`** | `clipboardStartDelay` + `ClipboardIdleGate` (capture chờ user ngừng gõ) | `Keyboard/Clipboard/KeyboardViewController+ClipboardCapture.swift` |
| Từ điển sửa typo (lexicon mmap) | Sau activation | `Task.detached(priority: .utility)`; chưa sẵn sàng thì **từ chối sửa** — hướng an toàn | `PersonalSuggestions/Correction/CorrectionWordSource.swift` |
| Personal suggestions engine + store | Sau activation | Serial queue riêng của `PersonalSuggestionWorker`; store mở off-main | `Keyboard/Suggestions/PersonalSuggestionWorker.swift` |
| Shortcuts (gõ tắt) | Sau activation | Actor async, có `cancel()` ở disappear | `Keyboard/Shortcuts/KeyboardViewController+Shortcuts.swift` |
| Ảnh nền theme | First layout có width > 0 | Decode theo **pixel budget** = width × scale; thả ảnh cũ trước khi decode ảnh mới | `Keyboard/Configuration/KeyboardViewController+Background.swift` |
| Vá bootstrap snapshot | `viewDidAppear` | `Task.detached(priority: .utility)` | `Keyboard/Configuration/KeyboardViewController+Configuration.swift` |

### 2.5. Đo đạc sẵn có

Subsystem `app.funput.keyboard`:

- Category `Launch` (`KeyboardLaunchTrace`): `ColdStart` (begin ở init, end ở
  `viewDidAppear`), `ViewDidLoad`, `ConfigurationLoad`, `BootstrapSnapshot`
  (hit/fallback), `ThemeResolve`, `PrimarySurfaceCreation`, `SurfaceApply`,
  `EngineConfiguration`, `AssetRead`, `AssetDecode`, `HeightReady`,
  `FirstNonZeroLayout`, `PanelCreation`, `PanelFirstOpen`, `PanelReleased`.
- Category `Memory` (`KeyboardMemoryTrace`): event `Footprint` với
  `physicalMB`/`peakMB`/`availableMB` (từ `phys_footprint` và
  `os_proc_available_memory`) tại các mốc: `Appeared`, `Disappeared`,
  `PanelCreated/PanelReleased <surface>`, `MemoryWarning`.

Đọc nhanh không cần Instruments:

```bash
log stream --predicate 'subsystem == "app.funput.keyboard" AND category == "Memory"'
```

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
  descriptor riêng, `CGFont` riêng — không cách nào trả lại. Thứ duy nhất chọn được
  là strike 2x (~16 KB/emoji, gác bằng `EmojiMemoryBudgetTests` ở trần 28 KB/emoji).
  Xem hết catalog vẫn đọng ~30 MB cho đến khi process chết.
- **Trần jetsam của keyboard extension thấp và tùy máy** (thấp hơn hẳn app thường;
  máy cũ thấp hơn máy mới). Nền 36 MB sau khi xem emoji + ảnh nền + spike clipboard
  là đủ gần trần trên máy RAM thấp. `availableMB` trong signpost `Footprint` là số
  thật của máy đang chạy — tin số đó, đừng tin hằng số trên mạng.

### 3.2. Vòng đời các vùng nhớ lớn

| Vùng nhớ | Trần | Thả khi nào |
|---|---|---|
| Primary surface | Cố định theo layout | Sống suốt phiên |
| Emoji/kaomoji/clipboard panel | Theo panel | Rời panel nào thả panel đó; giữ cặp emoji↔kaomoji khi chuyển qua lại; `viewDidDisappear` thả hết |
| Emoji catalog đã decode | Nhỏ (model, không ảnh) | `viewDidDisappear`, memory warning |
| Ảnh nền theme | Pixel budget theo width | Memory warning khi đang ẩn (`releaseBackgroundImageIfHidden`) |
| Lexicon `en.lex` | mmap read-only — trang clean, OS tự thu hồi | Không cần thả |
| Personal suggestion store | `max_words: 5_000` (`crates/funput-suggestions/src/engine/config.rs`) | Flush định kỳ, engine sống trên queue riêng |
| Clipboard history | **50 mục × text KHÔNG giới hạn — xem P1** | Expiry theo cấu hình |

### 3.3. Memory warning

`didReceiveMemoryWarning` (`Keyboard/Suggestions/KeyboardViewController+SuggestionLifecycle.swift`):
flush suggestions → thả panel đang ẩn → thả emoji catalog → thả ảnh nền nếu bàn phím
đang ẩn → ghi `Footprint("MemoryWarning")`. An toàn khi mọi thứ còn nil (khởi động).

---

## 4. Hiện trạng xử lý ngữ cảnh ô nhập liệu (audit 11/2026)

Kết luận audit: **đủ các trường hợp chuẩn của `UIInputViewController`, không tìm
thấy đường crash còn hở.** Giữ các bất biến sau khi sửa bất kỳ gì quanh đây:

- **Mọi trait từ proxy đều nil-safe** — `keyboardType ?? .default`,
  `returnKeyType ?? .default`, `isSecureTextEntry ?? false`,
  `autocapitalizationType ?? .sentences` (`Keyboard/Document/KeyboardDocumentWriter+Traits.swift`).
- **`documentIdentifier` phải đọc qua ObjC runtime** (`KeyboardDocumentWriter.swift`):
  header của Apple khai non-null nhưng host trả nil trước khi document bind; bridge
  thẳng sang `UUID` đã từng `EXC_BREAKPOINT` giữa lúc launch ngoài thực địa (#606) —
  extension chết, host vẽ bàn phím trống. Đây là mẫu cho mọi API proxy "nói dối
  nullability" về sau.
- **Secure field** (`isSecureTextEntry`) → `.password`/`.pin`
  (`KeyboardInput/State/KeyboardInputContext.swift`): tắt gõ tiếng Việt, tắt học từ
  (`suggestionTrackingActive` gate trên `supportsVietnameseComposition`), tắt
  autocorrect, tắt key preview/smart gesture, chặn cả clipboard offer lẫn capture
  (`allowsClipboardCapture`). Không được nới bất kỳ cái nào trong số này.
- **Callback đến sớm chịu được**: `textWillChange`/`textDidChange` trước
  `viewWillAppear` — `applyInputPresentation` guard `cachedThemedPresentation` nil,
  clipboard refresh guard `isClipboardMonitoring`; phím bấm trước khi document bind —
  `commit` luôn `synchronizeBeforeInput` trước.
- Keypad riêng cho `numberPad`/`decimalPad`/`phonePad`/`namePhonePad`/`.pin`;
  `oneTimeCode`/`username`/`newPassword`… chặn autocorrect qua
  `CorrectionFieldPolicy`; selection đang có thì không tính viết hoa, không dedupe
  echo, không re-open từ.

Nguồn crash còn lại trong code: `preconditionFailure` khi `funput_engine_new()` trả
null (chỉ khi malloc fail — chấp nhận) và các `init(coder:)` boilerplate không bao
giờ chạy. Không có `try!`/`as!` trên đường khởi động.

Tồn đọng không-crash, ngoài phạm vi tài liệu này: field `.asciiCapable` hiện rơi vào
`.text` nên vẫn gõ được tiếng Việt dù field tuyên bố chỉ muốn ASCII (và Info.plist
khai `IsASCIICapable = true`). Cần một quyết định sản phẩm riêng.

---

## 5. Vấn đề còn lại và đề xuất

Thứ tự ưu tiên theo ảnh hưởng lên đúng triệu chứng của #641. Mỗi mục ghi rõ thiết
kế để agent khác hiện thực được mà không cần đọc lại cuộc điều tra.

### P1 — Chặn trần bộ nhớ clipboard *(ưu tiên cao nhất)*

**Vấn đề.** `ClipboardItem.text` không giới hạn độ dài ở bất kỳ đâu:

- `ClipboardGateway.readText()` đọc toàn bộ pasteboard — user copy một văn bản
  chục MB là extension ôm trọn chuỗi đó;
- `ClipboardStore.capture()` (`FunputShared/Clipboard/ClipboardStore.swift`) đọc +
  decode **toàn bộ** `clipboard.json`, encode lại rồi ghi — tất cả **trên main
  actor** (closure `save` trong `makeClipboardCapture`), chi phí tạm thời gấp 2–3
  lần kích thước payload khi encode/decode JSON;
- 50 mục × không giới hạn = payload không có trần; panel giữ toàn văn mọi mục.

Việc này nổ ra 300ms sau khi bàn phím hiện hoặc khi user ngừng gõ → jetsam "lúc
được lúc không" tùy clipboard của user — khớp tính ngẫu nhiên trong #641.

**Thiết kế.**

1. Thêm `ClipboardCapturePolicy.maxTextLength = 100_000` (ký tự,
   `String.count`) vào `FunputShared/Clipboard/`. Text vượt trần thì **bỏ qua, không
   lưu vào lịch sử** — tuyệt đối không cắt bớt rồi lưu: paste từ lịch sử ra văn bản
   bị cụt là mất dữ liệu âm thầm, tệ hơn là không có mục đó. User vẫn paste được
   bằng nút paste của hệ thống. 100k ký tự ≈ 25–50 trang văn bản, đủ cho mọi use
   case gõ phím thực tế; trần tổng xấu nhất 50 × 100k ký tự giữ payload ở mức vài MB
   thay vì không trần.
2. Điểm chặn đặt ở `ClipboardCaptureController.finishRead`
   (`FunputShared/Clipboard/ClipboardCaptureController+Read.swift`) ngay sau khi có
   `text`: quá trần thì `processed = before.changeCount` (đánh dấu đã xử lý để không
   đọc lại) và dừng — giống nhánh `text.isEmpty` hiện có.
3. Dọn dữ liệu cũ: `ClipboardStore.prune` lọc bỏ luôn mục có `text.count` vượt trần
   (kể cả mục pinned — chúng là nguồn jetsam dai dẳng nhất vì không bao giờ expire).
   `prune` chạy trong mọi `load`/`capture` nên file cũ tự sạch, không cần migration.
4. Panel: cell chỉ render tối đa ~500 ký tự đầu làm preview (cắt ở tầng cell/entry
   mapping, không đụng store) — một mục 100k ký tự không được kéo cả chuỗi vào
   text layout của `UILabel`.

**Nghiệm thu.**

- Test `FunputShared`: capture text đúng trần được lưu; vượt trần 1 ký tự thì không
  lưu và changeCount vẫn được đánh dấu đã xử lý; `load` trên file chứa mục quá khổ
  (pinned lẫn không) trả về danh sách đã lọc.
- Kịch bản tay trên simulator: copy chuỗi > trần → mở bàn phím → lịch sử không có
  mục mới, không spike trong `log stream` category `Memory`, paste hệ thống vẫn
  hoạt động.
- Cập nhật mục §3.2 của tài liệu này (dòng clipboard) trong cùng PR.

**Rủi ro.** User copy văn bản rất dài rồi tìm nó trong lịch sử Funput sẽ không
thấy — chấp nhận, đổi lấy việc bàn phím không chết. Nếu sau này cần, làm riêng
cơ chế lưu tham chiếu lazy, không quay lại lưu toàn văn.

### P2 — Bỏ lần đọc activation source thừa ở cold start

**Vấn đề.** `loadActivationSource` chạy 2 lần mỗi cold start: trong
`bootstrapKeyboard()` (`viewDidLoad`) và ngay sau đó trong
`activateKeyboardForAppearance()` (`viewWillAppear`) — lần hai là đọc đĩa + decode
thừa nằm **trong animation trình bày**. Nó chỉ có nghĩa cho warm re-activation.

**Thiết kế.** Controller giữ `var bootstrappedSource: KeyboardActivationSource?`;
`bootstrapKeyboard()` gán nó sau khi load; `activateKeyboardForAppearance()` tiêu
thụ (`take`) nếu còn — dùng lại source đó thay vì load — và từ lần activation thứ
hai trở đi (biến đã nil) load như hiện tại. Giữa `viewDidLoad` và `viewWillAppear`
đầu tiên của cùng một lần trình bày không có cách nào user đổi settings, nên dùng
lại là an toàn. Lưu ý `hasFullAccess` đã nằm trong source — không cần đọc lại.

**Nghiệm thu.** Signpost `ConfigurationLoad` xuất hiện đúng **một** lần trong một
cold start (kiểm bằng Instruments hoặc `log stream` category `Launch`); phiên warm
(đổi settings trong containing app rồi quay lại host) vẫn nhận cấu hình mới.

### P3 — Lười hóa việc tạo thư mục App Group của `ClipboardStore`

**Vấn đề.** `ClipboardStore.init` gọi `AppGroupDirectory.prepare` ngay — 3–4
syscall đĩa trên main thread. Nó chạy 2 lần mỗi cold start (property initializer
của controller + `adoptConfiguration`), trước cả khi biết user có bật clipboard
hay không. Vi phạm nguyên tắc 1 ở mức nhỏ nhưng không có lý do tồn tại.

**Thiết kế.** `ClipboardStore` giữ `directoryName` thay vì `fileURL` đã prepare;
resolve URL (gọi `prepare`) lười ở lần `read()`/`write()` đầu tiên và cache kết
quả. `ClipboardStore` là struct bị thay thế mỗi lần đổi cấu hình, nên chỗ cache
hợp lý là một holder nhỏ dùng chung (hoặc static cache theo `directoryName` trong
`AppGroupDirectory` — agent hiện thực chọn, miễn giữ được hai bất biến: *không
syscall nào trước lần dùng clipboard đầu tiên* và *protection class + backup flag
vẫn được set đúng như `AppGroupDirectory` đang làm*). Đường test hiện truyền
`directory:` trực tiếp — giữ initializer đó nguyên vẹn.

**Nghiệm thu.** Đặt breakpoint/log trong `AppGroupDirectory.prepare`: không được
chạy trước `startClipboardMonitoring` (hoặc trước thao tác clipboard đầu tiên);
test `ClipboardStore` hiện có vẫn xanh nguyên vẹn.

### P4 — (Cân nhắc, không bắt buộc) Decode ảnh nền async

Ảnh nền theme decode đồng bộ trên main trong `viewWillLayoutSubviews` lúc trình
bày. Đã bounded theo pixel budget nên không nguy hiểm; chuyển sang decode nền +
fade vào sẽ sạch tuyệt đối nhưng đổi lấy một nhịp không có ảnh nền ở frame đầu.
**Chỉ làm nếu số đo Instruments trên máy thật cho thấy `AssetDecode` chiếm phần
đáng kể của khoảng `ColdStart`** — đừng làm theo cảm tính, đây là trade-off thật.

### P5 — Đo đạc và chẩn đoán từ thực địa

1. **Món nợ của PR #642:** đo bản Release trên máy thật bằng Instruments (template
   os_signpost, lọc subsystem `app.funput.keyboard`), đặc biệt máy RAM thấp; ghi số
   vào §3.1 của tài liệu này.
2. **Hộp đen footprint:** ghi bản ghi `Footprint` cuối (moment, physicalMB,
   availableMB, timestamp) vào App Group tại các mốc `Disappeared` và
   `MemoryWarning`; containing app đọc và hiển thị trong một màn chẩn đoán ẩn.
   Mục đích: khi user báo "bị văng", ta có số liệu của chính máy đó mà không cần
   họ cắm Instruments. Không ghi bất kỳ nội dung gõ phím nào — chỉ số liệu bộ nhớ.
3. **Phân định với lỗi OS:** triệu chứng "ngẫu nhiên hiện bàn phím gốc ngay lúc
   focus" trùng với lỗi iOS 26 đang được báo cáo rộng rãi (bàn phím bên thứ ba bị
   revert khi mở app, không phải giữa lúc gõ). Trước khi tốn thêm công tối ưu, hỏi
   người báo lỗi trong #641:
   - có rơi vào ô password/OTP không (iOS **cố ý** dùng bàn phím hệ thống ở đó);
   - có phải ngay sau khi khởi động lại máy không (trước first unlock bàn phím bên
     thứ ba không load — hành vi chuẩn);
   - thiết bị + bản iOS chính xác;
   - file `JetsamEvent-*.ips` / crash log có process `Keyboard` trong *Cài đặt >
     Quyền riêng tư & Bảo mật > Phân tích & Cải tiến > Dữ liệu phân tích* quanh
     thời điểm xảy ra. **Có jetsam → lỗi của mình (quay lại P1); không có gì →
     nghiêng về lỗi iOS 26, theo dõi release notes của Apple.**

---

## 6. Ràng buộc cho agent hiện thực

- Làm theo đúng thứ tự P1 → P2 → P3; P4/P5.1 cần số đo trước. Mỗi P một PR riêng,
  nhỏ, kèm cập nhật tài liệu này.
- Giữ mọi signpost hiện có; thêm mốc mới nếu đường code mới cần quan sát.
- `Scripts/check-swift-loc.sh`: thư mục mới hoặc có sửa phải ≤ 5 file và
  ≤ 150 dòng mỗi file — tách file theo đúng phong cách extension hiện tại.
- Code và comment bằng tiếng Anh; comment giải thích *ràng buộc* (tại sao phải thế),
  không kể lại code làm gì — theo đúng giọng các doc comment đang có.
- Không đổi hành vi secure field, không nới bất kỳ gate riêng tư nào ở §4.
- Chạy lại kịch bản kiểm chứng của PR #642 (mở emoji, lướt nhóm, tìm kiếm, về bàn
  phím chữ) sau bất kỳ thay đổi nào đụng vòng đời panel hoặc clipboard.
