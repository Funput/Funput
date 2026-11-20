# Độ tin cậy, tốc độ và bộ nhớ của Keyboard extension iOS

> **Mục tiêu:** chạm vào ô nhập là Funput hiện, hiện ngay, và không bao giờ tự chết.
> Funput không được là nguyên nhân của bất kỳ lần nào người dùng thấy bàn phím hệ
> thống thay cho mình. Đạt được **bằng cấu trúc**: mỗi thứ nặng chỉ tồn tại một bản
> trong tiến trình, hiện lại là gắn lại chứ không dựng lại, không có đường code nào tự
> giết tiến trình, và hỏng thì lùi về bàn phím tối thiểu.
>
> **Trạng thái:** Thiết kế đã chốt (11/2026). B1 đến B7 chưa hiện thực.
>
> **Cập nhật:** 20/11/2026
>
> **Phạm vi:** vòng đời tiến trình và controller của `Keyboard` extension, đường khởi
> động, bộ nhớ, các đường crash. Không bàn Rust engine nội bộ hay theme store.
>
> **Không làm:** không thêm đo đạc, không thêm signpost mới làm điều kiện nghiệm thu,
> không lưu nhật ký nào trên máy người dùng. Nghiệm thu bằng test, kịch bản tay và
> dùng thật.

Tài liệu này tiếp nối [STARTUP_AND_MEMORY.md](STARTUP_AND_MEMORY.md) và theo cùng quy
ước: **mỗi thay đổi hành vi cập nhật lại tài liệu trong cùng PR**; chỗ nào hiện thực
lệch khỏi bản viết thì sửa lại và đánh dấu **Đã đổi khi hiện thực**. Hợp đồng khởi
động (§2) và các bất biến (§7) của tài liệu kia vẫn có hiệu lực; §6 dưới đây liệt kê
những dòng của hợp đồng mà tài liệu này sửa.

Bối cảnh: [issue #751](https://github.com/Funput/Funput/issues/751), tiếp theo
[#641](https://github.com/Funput/Funput/issues/641).

**Quy ước đường dẫn:** mọi đường dẫn tính từ `platforms/ios/`; `Sources/` là viết tắt
của `Packages/FunputKit/Sources/`.

---

## 1. Vì sao cần một vòng nữa

Sau A1 đến A8, triệu chứng đã đổi dạng. #641 là "đang gõ thì bị thay bằng bàn phím hệ
thống". #751 là "chạm vào ô nhập thì lúc ra Funput, lúc ra bàn phím hệ thống", còn
đang gõ thì không sao. Phần bộ nhớ lúc đang dùng đã được chữa; phần còn lại nằm ở
**lúc trình bày**: tiến trình không kịp hiện, hoặc chết đúng lúc đang dựng bàn phím.

Rà lại kiến trúc không thấy một lỗi đơn lẻ nào giải thích được hết, mà thấy một nhóm
điểm yếu cùng dồn vào khoảnh khắc trình bày (§2). Vòng này chữa cả nhóm thay vì đoán
xem cái nào là thủ phạm.

### 1.1. Những lần không phải do Funput

Bàn phím hệ thống khác Funput ở một điểm không san bằng được: nó chạy ngay trong tiến
trình của app host, còn Funput là một tiến trình riêng mà iOS được quyền dọn khi đang
ẩn. Ngoài ra iOS **chủ động** dùng bàn phím hệ thống trong các trường hợp sau, và
không có API nào đổi được:

- ô mật khẩu (`isSecureTextEntry`) và ô kiểu `phonePad`, `namePhonePad`;
- app host cấm bàn phím bên thứ ba;
- sau một lần bị thay, iOS coi bàn phím hệ thống là bàn phím hiện tại cho đến khi
  người dùng bấm quả địa cầu đổi lại; một số app còn nhớ bàn phím theo từng ngữ cảnh,
  nên một lần bị thay có thể hiện lại nhiều lần.

Điều làm được, và là mục tiêu ở đây: tiến trình nhẹ để ít bị dọn khi ẩn, khởi động
nguội ngắn nhất có thể khi đã bị dọn, hiện ấm gần như không tốn gì, và không bao giờ
tự chết.

---

## 2. Hiện trạng: chỗ nào chưa tốt

Đã đọc trong source ngày 20/11/2026. Cột cuối trỏ tới mục sửa ở §5.

### 2.1. Độ tin cậy

| # | Chỗ chưa tốt | Ở đâu | Sửa |
|---|---|---|---|
| T1 | **Còn đường tự giết.** Khoảng 40 `precondition` vẫn chạy ở bản Release; một cái bắn là tiến trình chết và iOS thay bằng bàn phím hệ thống. Vài cái nhận giá trị có nguồn gốc lúc chạy: kích thước layout, hệ số sizing, nhãn phím Enter, timestamp của touch. | `Sources/KeyboardLayout/KeyboardGeometry.swift`, `KeyboardSizingProfile.swift`, `Models/KeyboardEnterAction.swift`, `Sources/KeyboardTouchCore/Model/ContactSample.swift` | B1 |
| T2 | **Mọi thứ thuộc về controller, mà iOS dựng controller mới ở mỗi app host.** Nếu iOS dựng controller mới trước khi thả controller cũ, tiến trình giữ hai bản của surface, ảnh nền và các engine đúng vào khoảnh khắc trình bày. Đó là lúc footprint cao nhất, và là lúc người dùng thấy hậu quả nếu tiến trình bị giết. | `Keyboard/Controller/KeyboardViewController.swift` (mọi stored property) | B3, B4 |
| T3 | **Hai worker cùng một store.** Mỗi controller có `PersonalSuggestionWorker` riêng. Khi đổi app, worker cũ có thể còn đang flush trong lúc worker mới mở cùng file store. | `Keyboard/Suggestions/PersonalSuggestionWorker.swift` | B4 |
| T4 | **Hỏng lúc khởi động thì hỏng mãi.** Nếu một cấu hình, một theme tuỳ biến hay một file ảnh nền làm bootstrap chết, lần khởi động nào cũng chết lại y như vậy; không có đường lùi về bàn phím tối thiểu. | `Keyboard/Controller/Activation/KeyboardViewController+Bootstrap.swift` | B7 |
| T5 | **Observer flush có thể không bao giờ chạy.** `flushPersonalSuggestions` đăng ký `UIApplication.didEnterBackgroundNotification`, thông báo của app, không phải của extension. | `Keyboard/Suggestions/KeyboardViewController+Suggestions.swift` | B6 |

### 2.2. Bộ nhớ

| # | Chỗ chưa tối ưu | Ở đâu | Sửa |
|---|---|---|---|
| M1 | **Một bộ đầy đủ cho mỗi controller.** Mỗi lần đổi app tạo mới: một surface (vài chục phím), một `FunputComposer` (engine Rust), một suggestion engine có store, một suggestion engine thứ hai cho sửa lỗi gõ, một cache ảnh nền đã decode, cùng tất cả các store. Bản cũ chỉ được thả khi controller cũ chết. | `KeyboardViewController.swift`, `Sources/PersonalSuggestions/Correction/CorrectionWordSource.swift` | B3, B4 |
| M2 | **Đổi layout là huỷ và dựng lại toàn bộ phím.** Mỗi lần bấm `?123`, quay về chữ, hay ô nhập đổi kiểu, `rebuildKeys()` bỏ hết `KeyboardKeyControl` và tạo lại. Với theme kính, mỗi phím là một `UIVisualEffectView(UIGlassEffect)`, nên mỗi lần đổi là vài chục effect view bị huỷ và vài chục cái mới được cấp phát. | `Sources/KeyboardRenderer/Surface/Core/KeyboardSurfaceView+Presentation.swift`, `Sources/KeyboardRenderer/Keys/KeyboardKeySurfaceView.swift` | B5 |
| M3 | **Âm phím được nạp ngay khi bàn phím hiện và không bao giờ thả.** `updateFeedback` gọi `sounds.prepare` ở mỗi lần đổi presentation; với âm phím đóng gói, việc đó dựng audio session và ba `AVAudioPlayer` cho mỗi loại âm, kể cả khi người dùng không gõ phím nào. Session không bao giờ được ngừng, player sống tới khi surface chết. | `Sources/KeyboardRenderer/Interaction/Feedback/KeyboardSurfaceInteractionController+Feedback.swift`, `Sources/KeyboardRenderer/Feedback/SoundPlayback/KeySoundPlayerPool.swift` | B2, B6 |
| M4 | **`UIPasteControl` luôn tồn tại.** Toolbar tạo chip dán khi khởi tạo và dựng control ở lần áp theme đầu tiên, dù chưa có gì để dán, kể cả khi người dùng tắt clipboard. | `Sources/KeyboardRenderer/Toolbar/KeyboardToolbarView.swift`, `KeyboardClipboardChipView.swift` | B2 |
| M5 | **Bộ đếm từ của phiên không có trần.** `personalCounts` tăng theo mỗi từ gõ xong. Hiện nó chết cùng controller nên không lộ; khi chuyển sang sống cùng tiến trình (B4) thì phải có trần. | `Sources/PersonalSuggestions/Correction/CorrectionWordSource+Counts.swift` | B4 |
| M6 | **Cache ảnh emoji của CoreText không trả lại được** (STARTUP_AND_MEMORY.md §3.1). Giới hạn đã biết, xem §7. | | Không sửa |

### 2.3. Khởi động

Hai loại khởi động, và cả hai đều đang làm thừa việc.

**Khởi động nguội** (tiến trình mới):

| # | Chỗ chưa tốt | Ở đâu | Sửa |
|---|---|---|---|
| K1 | Dựng `UIPasteControl` trước frame đầu (cùng chỗ với M4). Đây là một control do hệ thống vẽ hộ, đắt hơn một view thường. | `KeyboardClipboardChipView.swift` | B2 |
| K2 | Đọc snapshot mà phải chuẩn bị thư mục trước: `KeyboardBootstrapSnapshotStore.init` gọi `AppGroupDirectory.prepare`, tức tạo thư mục, đặt thuộc tính bảo vệ, đặt cờ không backup. Ba syscall ghi chỉ để đọc một file. | `Sources/KeyboardConfiguration/Bootstrap/KeyboardBootstrapSnapshotStore.swift` | B2 |
| K3 | `KeyboardSurfaceInteractionController.init` gọi `haptics.prepare()` cho ba generator, đánh thức Taptic Engine trước khi có ngón tay nào chạm. | `Sources/KeyboardRenderer/Interaction/Controller/KeyboardSurfaceInteractionController.swift` | B2 |
| K4 | Nạp âm phím ngay lúc hiện (cùng chỗ với M3). Chạy trên queue riêng nhưng vẫn tranh CPU và gọi audio server đúng lúc bàn phím đang dựng. | như M3 | B2 |
| K5 | Phím có thể được dựng hai lần: bootstrap dựng theo traits mặc định, rồi `textWillChange` báo traits thật của ô nhập; nếu layout khác thì `rebuildKeys()` dựng lại toàn bộ (cùng gốc với M2). | `KeyboardSurfaceView+Presentation.swift` | B5 |
| K6 | Mỗi phím một effect view kính ở theme mặc định `funputGlass`. Đây là diện mạo của sản phẩm nên giữ; chi phí dựng được B3 biến thành một lần mỗi tiến trình. | `KeyboardKeySurfaceView.swift` | B3 |

**Hiện ấm** (tiến trình còn sống, đổi sang app khác):

| # | Chỗ chưa tốt | Sửa |
|---|---|---|
| K7 | Hiện ấm làm gần như toàn bộ việc của khởi động nguội, chỉ trừ bước tạo tiến trình: đọc snapshot, resolve presentation, dựng surface, decode ảnh nền, tạo engine, mở store gợi ý, mở từ điển sửa lỗi. Trong khi thứ duy nhất thật sự đổi là document của host. | B3, B4 |
| K8 | Cùng lúc đó main thread còn phải huỷ bộ cũ: surface cũ với vài chục effect view bị giải phóng ngay trong lúc bàn phím mới đang hiện. | B3 |

---

## 3. Nguyên tắc

Năm nguyên tắc của STARTUP_AND_MEMORY.md §1 giữ nguyên. Thêm bốn nguyên tắc:

6. **Tiến trình là đơn vị sống, controller là vỏ.** Thứ gì không phụ thuộc app host
   thì thuộc về tiến trình và chỉ có một bản. Controller chỉ giữ những gì gắn với lần
   trình bày của nó.
7. **Hiện lại là gắn lại, không dựng lại.** Từ lần hiện thứ hai trong cùng một tiến
   trình: không tạo view, không decode ảnh, không mở file. Việc duy nhất bắt buộc là
   đồng bộ document với host mới. Cùng luật đó cho đổi layout: tái dùng phím, không
   huỷ rồi tạo.
8. **Không có đường code nào tự giết tiến trình vì dữ liệu lúc chạy.** Giá trị đến từ
   cấu hình, từ host, từ đĩa hay từ kích thước layout mà sai thì được kẹp về giá trị
   hợp lệ hoặc bỏ qua, không trap.
9. **Hỏng thì lùi, không chết lặp.** Bootstrap chết hai lần liền thì lần sau khởi động
   ở chế độ an toàn: bàn phím tối thiểu luôn hiện được.

---

## 4. Kiến trúc đích

```
iOS dựng lại ở mỗi app host                  Sống cùng tiến trình, một bản duy nhất
┌──────────────────────────────┐             ┌────────────────────────────────────────┐
│ KeyboardViewController (vỏ)  │   claim     │ KeyboardRuntime.shared                 │
│  inputView, height constraint│ ──────────▶ │  surface (KeyboardSurfaceView)         │
│  lease của lần trình bày     │             │  ảnh nền đã decode                     │
│  đồng bộ document với host   │ ◀────────── │  cấu hình + presentation đã resolve    │
│  clipboard monitor (theo lần │   release   │  inputCoordinator (engine Rust)        │
│  hiện)                       │             │  suggestion service, correction words  │
└──────────────────────────────┘             │  shortcuts, các store                  │
                                             └────────────────────────────────────────┘
```

### 4.1. Ai giữ cái gì

| Thuộc về tiến trình (`KeyboardRuntime`) | Thuộc về lần trình bày (controller) |
|---|---|
| `keyboardView` và các panel emoji, kaomoji, clipboard | `inputView`, `heightController` |
| `backgroundImageCache`, `resolvedBackgroundRequest` | `launchTrace` |
| `configuration`, `themeCatalog`, `selectedTheme`, `cachedThemedPresentation`, `adoptedIdentity` | `resolvedTextInputTraits`, `isKeyboardVisible` |
| `inputCoordinator` | `activationState` và generation của nó |
| `personalSuggestionService`, `correctionWords`, `shortcutsLoader` | `clipboardChangeMonitor`, `clipboardStartTask` |
| mọi store (`configurationStore`, `customThemeStore`, `themeAssetStore`, `bootstrapSnapshotStore`, `clipboardStore`, recents) | lease đang giữ |
| `emojiCatalogCache`, `clipboardRecorder` | |

### 4.2. Lease: ai đang là bàn phím

Tại một thời điểm chỉ một controller là bàn phím đang hiện. Runtime phát một **lease**
(một số generation tăng dần) cho controller gọi `claim`, và mọi lease cũ hết hiệu lực
ngay lúc đó.

```
viewDidLoad        runtime.claim(self)
                     lần đầu trong tiến trình: bootstrap như hiện nay (nguội)
                     các lần sau: gắn surface sang inputView mới, nối lại callback,
                                  đưa phiên nhập về trạng thái đầu (ấm)
viewWillAppear     đồng bộ document; áp cấu hình nếu identity đổi
viewWillDisappear  nếu lease còn hiệu lực: flush, dừng clipboard monitor
viewDidDisappear   nếu lease còn hiệu lực: tháo surface khỏi inputView, cho runtime giữ
```

Ba luật đi kèm:

- **Controller cũ không được đụng vào thứ đã trao.** Mọi callback vòng đời của
  controller kiểm `lease.isCurrent` trước khi chạm runtime. Nhờ vậy thứ tự iOS gọi
  `viewDidDisappear` của controller cũ và `viewDidLoad` của controller mới là gì cũng
  đúng; thiết kế không dựa vào thứ tự đó.
- **`claim` nối lại mọi callback trỏ về controller.** `onKeyEvent`,
  `setInputModeSwitch(target:action:)`, callback clipboard và suggestion hiện được gán
  trong `makePrimarySurface()`; chúng tách ra thành `bind(to:)` và chạy ở mỗi lần
  claim. Tạo surface chỉ còn chạy một lần mỗi tiến trình.
- **Trạng thái của phiên nhập không rò sang app khác.** `claim` ấm đưa bàn phím về
  đúng trạng thái mà một controller mới ngày nay có: layout chữ, surface `.funput`,
  không composition dở, thanh gợi ý trống, touch pipeline đã reset. Thứ gì người dùng
  coi là một quyết định (đã dán, đã bỏ qua) vẫn persist qua App Group như cũ.

A8 vẫn đúng và trở thành hệ quả tự nhiên: surface rời `inputView` cũ ngay lúc được
trao, nên vòng giữ nhau giữa `inputView` và surface không hình thành.
`KeyboardLifetimeCensus` giữ nguyên và phải luôn báo một surface.

### 4.3. Chế độ an toàn

Một **số đếm một byte** trong container riêng của extension, không phải nhật ký:
không có thời gian, không có lịch sử, không có gì về người dùng. Lần claim nguội tăng
nó trước khi bootstrap và đưa về 0 khi bootstrap xong, nên giá trị khác 0 lúc khởi
động nghĩa là lần trước chết giữa bootstrap.

| Số đếm lúc khởi động | Bàn phím khởi động với |
|---|---|
| 0 hoặc 1 | Bình thường |
| 2 hoặc 3 | Bậc 1: cấu hình của người dùng (kiểu gõ, layout), nhưng theme gốc không kính, không ảnh nền, không snapshot, không gợi ý, không sửa lỗi gõ, không gõ tắt |
| 4 trở lên | Bậc 2: `FunputConfiguration.default` hoàn toàn |

Ở chế độ an toàn, bootstrap xong **không** đưa số đếm về 0 mà giữ ở ngưỡng của bậc
đang chạy, để lần sau không thử lại cấu hình đã làm chết. Số đếm về 0 khi identity của
cấu hình đổi (người dùng sửa cài đặt hoặc đổi theme) hoặc khi phiên bản app đổi.

---

## 5. Các việc phải làm

Thứ tự B1 → B7, mỗi mục một PR.

### B1: Bỏ các đường tự giết (T1)

**Thiết kế.**

1. Rà toàn bộ `precondition`, `preconditionFailure`, `fatalError`, `try!`, `as!`,
   force unwrap trong `Keyboard/` và `Sources/`. Cái nào nhận giá trị có thể đến từ
   cấu hình, host, đĩa hoặc layout thì đổi thành kẹp về giá trị hợp lệ hoặc trả sớm.
2. Cái nào thật sự là bất biến của lập trình viên (hằng số trong code, `init(coder:)`
   không bao giờ chạy, `funput_engine_new` trả null) thì giữ và ghi vào allowlist kèm
   một dòng lý do.
3. Script `Scripts/Quality/check-keyboard-traps.sh`: báo lỗi khi có trap mới ngoài
   allowlist. Chạy trong job `shell-line-budget` của `.github/workflows/ci.yml`, thêm
   một step, không đổi tên job.
4. Mọi API của proxy trả kiểu non-optional mà chưa có trong §7 của tài liệu kia thì
   đọc theo mẫu `documentIdentifier`.

**Nghiệm thu.** Script xanh trên nhánh, đỏ khi thêm thử một `precondition` mới. Test
cho từng chỗ đã đổi: đầu vào xấu (0, số âm, NaN, chuỗi rỗng) cho ra giá trị kẹp, không
trap.

### B2: Đường khởi động chỉ còn việc của frame đầu (K1 đến K4, M3, M4)

**Thiết kế.**

1. `KeyboardClipboardChipView` không tạo `UIPasteControl` cho tới khi nhận hint đầu
   tiên khác nil. `apply(theme:)` chỉ ghi nhớ màu; control được dựng ở `update(hint:)`.
2. `KeyboardBootstrapSnapshotStore` tách đường đọc khỏi `AppGroupDirectory.prepare`:
   đọc chỉ ghép URL, `prepare` chỉ chạy trước lần ghi.
3. `haptics.prepare()` rời khỏi `init` của interaction controller, chạy ở lần chạm đầu
   tiên (`beginTouch`); generator tự chuẩn bị lại sau mỗi lần rung như hiện nay.
4. `sounds.prepare` rời khỏi `updateFeedback`, chạy ở lần chạm đầu tiên.
   `KeySoundPlayerPool.play` vốn đã tự nạp style trước khi phát, nên lần bấm đầu vẫn
   có âm; gọi `prepare` ngay lúc ngón tay chạm xuống chỉ để việc nạp bắt đầu sớm hơn.

**Nghiệm thu.** Test `KeyboardRenderer`: chip mới tạo không có `UIPasteControl` trong
cây view, có hint thì có; interaction controller mới tạo chưa gọi `prepare` trên
haptics và sounds giả, sau `beginTouch` đầu tiên thì gọi đúng một lần. Test
`KeyboardConfiguration`: `load()` trên thư mục chưa tồn tại không tạo thư mục. Các UI
test clipboard hiện có vẫn xanh.

### B3: `KeyboardRuntime`, phần nhìn thấy (T2, M1, K6, K7, K8)

**Thiết kế.** Như §4.1 và §4.2, cho surface, panel, ảnh nền, cấu hình và presentation,
các store.

- `Keyboard/Runtime/`: `KeyboardRuntime` (`@MainActor`, một instance cho tiến trình),
  `KeyboardLease`.
- Controller giữ **computed property chuyển tiếp** cùng tên (`keyboardView`,
  `configuration`, `selectedTheme`, …) trỏ vào runtime, để các extension hiện có không
  phải sửa.
- `makePrimarySurface()` tách thành tạo (một lần) và `bind(to:)` (mỗi lần claim).
- `detachPrimarySurface()` và `reattachPrimarySurfaceIfNeeded()` chỉ chạy khi lease
  còn hiệu lực.

**Rủi ro đã biết.** Chuyển một surface có `UIGlassEffect` sang `inputView` của một
controller khác chưa từng được làm trong code này. Nếu kính hiển thị sai sau khi
chuyển, lùi về phương án: giữ surface theo controller như hiện nay, vẫn chia sẻ ảnh
nền, presentation và store. Ghi lại lựa chọn ở mục **Đã đổi khi hiện thực**.

**Nghiệm thu.** Test `KeyboardLease`: lease cũ hết hiệu lực sau claim mới; release
bằng lease cũ không làm gì. UI test đổi qua lại harness và Settings 10 lần: chỉ một
surface sống suốt, không có log fault của `KeyboardLifetimeCensus`, và surface là cùng
một instance. Kịch bản tay: đổi app khi đang mở emoji thì app sau bắt đầu ở bàn phím
chữ; đổi theme trong app chính rồi quay lại thì bàn phím nhận theme mới; xoay máy; bật
Giảm độ trong suốt.

### B4: `KeyboardRuntime`, phần xử lý (T3, M1, M5, K7)

**Thiết kế.** Chuyển `inputCoordinator`, `personalSuggestionService`,
`correctionWords`, `shortcutsLoader` sang runtime.

- `claim` ấm gọi một hàm reset phiên trên coordinator, cho kết quả giống một
  coordinator mới tạo rồi `apply(configuration)`.
- Suggestion worker chỉ còn một cho cả tiến trình, nên store gợi ý cá nhân chỉ được mở
  một lần và không còn hai worker trên cùng một file.
- Lexicon và từ điển sửa lỗi chỉ mở một lần mỗi tiến trình.
- `personalCounts` có trần ghi bằng hằng số; vượt thì bỏ các từ đếm thấp nhất.

**Nghiệm thu.** Test `KeyboardInput`: sau reset phiên, trạng thái coordinator bằng
trạng thái của một coordinator mới với cùng cấu hình. Test `PersonalSuggestions`:
`personalCounts` không vượt trần. Kịch bản tay: gõ dở một từ ở app A, đổi sang app B,
gõ tiếp thì không dính composition cũ; từ vừa học ở A được gợi ý ở B; ô mật khẩu vẫn
tắt gõ tiếng Việt, tắt học từ, tắt mời dán.

### B5: Tái dùng phím khi đổi layout (M2, K5)

**Thiết kế.** `rebuildKeys()` không huỷ rồi tạo nữa. Surface giữ một **pool**
`KeyboardKeyControl`:

- đổi layout thì gán spec mới cho các control đang có theo thứ tự; lớp nền của phím
  (kính hoặc màu) được giữ, chỉ nội dung và vai trò đổi;
- layout mới nhiều phím hơn thì tạo thêm phần thiếu; ít hơn thì ẩn phần dư, không huỷ.
  Trần của pool là layout nhiều phím nhất;
- `keyControls` vẫn là từ điển theo `spec.id`, dựng lại từ pool sau mỗi lần gán, nên
  các nơi tra theo id không đổi;
- luật hiện có giữ nguyên: ngón tay đang đặt trên phím vẫn commit theo layout nó chạm
  vào (`suspendPresentation` và snapshot geometry theo từng contact).

**Nghiệm thu.** Test `KeyboardRenderer`: chuyển chữ → số → ký hiệu → chữ, số
`KeyboardKeyControl` được tạo bằng số phím của layout lớn nhất, không tăng thêm ở các
vòng sau; mỗi phím sau khi gán lại có đúng nhãn, vai trò, alternates và trạng thái
Shift của spec mới; các test rollover khi đổi layout hiện có vẫn xanh.

### B6: Ẩn là nhẹ (M3, T5)

**Thiết kế.** Khi release lease ở `viewDidDisappear`:

- thả các player âm phím và ngừng audio session (`KeySoundPlayerPool`); lần chạm đầu
  của lần hiện sau nạp lại trên queue riêng (đường của B2);
- panel và emoji catalog: giữ hành vi hiện tại (đã thả);
- ảnh nền: giữ, vì chỉ còn một bản và hiện lại không được phải decode; vẫn thả khi có
  memory warning lúc đang ẩn;
- `flushPersonalSuggestions` đăng ký thêm `NSExtensionHostDidEnterBackground`, thông
  báo dành cho extension.

**Nghiệm thu.** Test `KeyboardRenderer`: sau lệnh thả, pool không còn player và lần
`play` kế tiếp nạp lại. Kịch bản tay: đang phát nhạc, gõ có âm phím, ẩn bàn phím, nhạc
không bị ngắt hay giảm âm; hiện lại gõ vẫn có âm.

### B7: Chế độ an toàn (T4)

**Thiết kế.** Như §4.3. `Keyboard/Runtime/Safety/`: `KeyboardBootGuard` (đọc, tăng và
đặt lại số đếm; một hàm thuần đổi số đếm thành bậc). `bootstrapKeyboard()` rẽ nhánh
theo bậc trước khi đọc snapshot. Làm sau B3 vì chỉ lần claim nguội mới chạm số đếm.

**Nghiệm thu.** Test hàm bậc với mọi giá trị số đếm. Test bootstrap: ở bậc 1 không đọc
snapshot, không tạo suggestion engine, theme là theme gốc không kính; bootstrap xong ở
chế độ an toàn không đưa số đếm về 0; identity cấu hình đổi thì về 0. File số đếm
hỏng, thiếu hoặc không ghi được thì coi là 0 và bàn phím khởi động bình thường.

---

## 6. Sửa hợp đồng khởi động

Áp vào bảng §2 của STARTUP_AND_MEMORY.md, trong PR tương ứng:

| Chặng | Thay đổi | PR |
|---|---|---|
| `viewDidLoad` | Bỏ khỏi cột được phép: chuẩn bị thư mục App Group. Đọc snapshot không còn ghi gì xuống đĩa | B2 |
| `viewDidLoad` | Lần claim ấm: gắn surface có sẵn, không tạo view, không đọc snapshot nếu identity chưa đổi | B3 |
| `init` controller | Không tạo engine nữa: engine thuộc runtime | B4 |
| `viewDidLoad` | Lần claim nguội: đọc và ghi một byte số đếm khởi động trong container riêng | B7 |
| Sau `viewDidAppear` | Chuẩn bị haptics và âm phím ở lần chạm đầu tiên, không sớm hơn | B2 |

---

## 7. Giới hạn còn lại

- **Cache ảnh emoji của CoreText** vẫn không trả lại được: xem hết catalog để lại
  khoảng 30 MB cho tới khi tiến trình chết. Vòng này không đổi điều đó; nó chỉ bảo đảm
  phần còn lại của tiến trình không cộng thêm vào: không còn bản thứ hai của surface,
  ảnh nền hay engine.
- **iOS vẫn được dọn tiến trình khi đang ẩn**, và các trường hợp ở §1.1 vẫn ra bàn
  phím hệ thống. Không có gì trong extension đổi được hai điều này.

---

## 8. Ràng buộc cho agent hiện thực

- Mỗi mục một PR nhỏ, kèm cập nhật tài liệu này và bảng §2, §3.2 của
  STARTUP_AND_MEMORY.md.
- Giữ mọi ràng buộc ở §6 và các bất biến ở §7 của tài liệu kia: không đổi hành vi ô
  mật khẩu, không thêm lần đọc nội dung pasteboard, giữ mọi signpost hiện có.
- Không thêm nhật ký, bộ đếm thống kê hay bất kỳ dữ liệu chẩn đoán nào ghi xuống đĩa.
  Ngoại lệ duy nhất là số đếm một byte của B7.
- `Scripts/check-swift-loc.sh`: mỗi file Swift tối đa 150 dòng.
  `Scripts/Quality/check-keyboard-touch-layout.sh`: tối đa 5 file Swift mỗi thư mục
  được liệt kê, nên code mới nằm trong thư mục con mới (`Keyboard/Runtime/`).
- Code và comment bằng tiếng Anh. Chữ trên giao diện bằng tiếng Việt, câu tự nhiên,
  không dùng `·` hay `—`.
- Sau B3, B4 và B5 chạy lại kịch bản của PR #642 (emoji) và kịch bản đổi app của A7.
