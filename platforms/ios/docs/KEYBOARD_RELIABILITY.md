# Độ tin cậy, tốc độ và bộ nhớ của Keyboard extension iOS

> **Mục tiêu:** chạm vào ô nhập là Funput hiện, hiện ngay, và không bao giờ tự chết.
> Funput không được là nguyên nhân của bất kỳ lần nào người dùng thấy bàn phím hệ
> thống thay cho mình. Đạt được **bằng cấu trúc**: mỗi thứ nặng chỉ tồn tại một bản
> trong tiến trình, hiện lại là gắn lại chứ không dựng lại, đường khởi động chỉ làm
> việc của frame đầu, và không có đường code nào tự giết tiến trình.
>
> **Trạng thái:** B1 đến B5 đã hiện thực (11/2026). Chưa dùng thử trên máy thật, bản Release.
>
> **Cập nhật:** 20/11/2026
>
> **Phạm vi:** vòng đời tiến trình và controller của `Keyboard` extension, đường khởi
> động, bộ nhớ, các đường crash. Không bàn Rust engine nội bộ hay theme store.
>
> **Không làm:** không thêm đo đạc, không thêm signpost mới làm điều kiện nghiệm thu,
> không lưu nhật ký hay bộ đếm nào trên máy người dùng. Nghiệm thu bằng test, kịch bản
> tay và dùng thật.

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
| T1 | **Còn một đường tự giết nhận dữ liệu lúc chạy.** `ContactSample` trap khi timestamp không hữu hạn, mà timestamp đến thẳng từ `UITouch`, không ai kiểm trước. Các `precondition` còn lại trong package (khoảng 40) chỉ nhận hằng số trong code hoặc đã có guard ở nơi gọi, và `Keyboard/` không có trap hay force unwrap nào; nhưng không có gì giữ cho điều đó tiếp tục đúng. | `Sources/KeyboardTouchCore/Model/ContactSample.swift`, `Sources/KeyboardTouchUIKit/Capture/UIKitTouchCaptureAdapter.swift` | B1 |
| T2 | **`heightScale` chỉ được kẹp trên đường snapshot.** Đường fallback đọc cấu hình rồi gán thẳng vào sizing; một giá trị 0 hoặc âm cho ra bàn phím cao 0. | `Sources/KeyboardConfiguration/Presentation/KeyboardPresentationFactory.swift` | B1 |
| T3 | **Mọi thứ thuộc về controller, mà iOS dựng controller mới ở mỗi app host và giữ controller cũ lại.** Quan sát trên simulator iOS 27.2: controller của một app host sống chừng nào app đó còn ở nền, cho tới khi chính app đó hiện bàn phím lần nữa. Hai app là hai controller, và trước B4 là hai surface, hai bộ engine gợi ý, sống cùng lúc và suốt thời gian đó, không chỉ thoáng qua lúc chuyển. | `Keyboard/Controller/KeyboardViewController.swift` (mọi stored property) | B4, B5 |
| T4 | **Hai worker cùng một store.** Mỗi controller có `PersonalSuggestionWorker` riêng. Khi đổi app, worker cũ có thể còn đang flush trong lúc worker mới mở cùng file store. | `Keyboard/Suggestions/PersonalSuggestionWorker.swift` | B5 |
| T5 | **Observer flush nghe thông báo của app.** `flushPersonalSuggestions` đăng ký `UIApplication.didEnterBackgroundNotification`, không phải thông báo dành cho extension. | `Keyboard/Suggestions/KeyboardViewController+Suggestions.swift` | B2 |

### 2.2. Bộ nhớ

| # | Chỗ chưa tối ưu | Ở đâu | Sửa |
|---|---|---|---|
| M1 | **Một bộ đầy đủ cho mỗi controller.** Mỗi lần đổi app tạo mới: một surface (vài chục phím), một suggestion engine có store, một suggestion engine thứ hai cho sửa lỗi gõ, một cache ảnh nền đã decode, cùng tất cả các store. Bản cũ chỉ được thả khi controller cũ chết. | `KeyboardViewController.swift`, `Sources/PersonalSuggestions/Correction/CorrectionWordSource.swift` | B4, B5 |
| M2 | **Đổi layout là huỷ và dựng lại toàn bộ phím.** Mỗi lần bấm `?123`, quay về chữ, hay ô nhập đổi kiểu, `rebuildKeys()` bỏ hết `KeyboardKeyControl` và tạo lại. Với theme kính, mỗi phím là một `UIVisualEffectView(UIGlassEffect)`, nên mỗi lần đổi là vài chục effect view bị huỷ và vài chục cái mới được cấp phát. | `Sources/KeyboardRenderer/Surface/Core/KeyboardSurfaceView+Presentation.swift`, `Sources/KeyboardRenderer/Keys/KeyboardKeyControl.swift` | B3 |
| M3 | **Âm phím được nạp ngay khi bàn phím hiện và không bao giờ thả.** `updateFeedback` gọi `sounds.prepare` ở mỗi lần đổi presentation; với âm phím đóng gói, việc đó dựng audio session và ba `AVAudioPlayer` cho mỗi loại âm, kể cả khi người dùng không gõ phím nào. Session không bao giờ được ngừng, player sống tới khi surface chết. | `Sources/KeyboardRenderer/Interaction/Feedback/KeyboardSurfaceInteractionController+Feedback.swift`, `Sources/KeyboardRenderer/Feedback/SoundPlayback/KeySoundPlayerPool.swift` | B2 |
| M4 | **`UIPasteControl` luôn tồn tại.** Toolbar tạo chip dán khi khởi tạo, chip dựng control ngay, dù chưa có gì để dán, kể cả khi người dùng tắt clipboard. | `Sources/KeyboardRenderer/Toolbar/KeyboardClipboardChipView.swift` | B2 |
| M5 | **Cache ảnh emoji của CoreText không trả lại được** (STARTUP_AND_MEMORY.md §3.1). Giới hạn đã biết, xem §7. | | Không sửa |

### 2.3. Khởi động

Hai loại khởi động, và cả hai đều đang làm thừa việc.

**Khởi động nguội** (tiến trình mới):

| # | Chỗ chưa tốt | Ở đâu | Sửa |
|---|---|---|---|
| K1 | `UIPasteControl` được dựng **hai lần** trước frame đầu: một lần trong `init` của chip với màu mặc định, một lần nữa ở `apply(theme:)` đầu tiên vì màu theme khác màu mặc định. Đây là một control do hệ thống vẽ hộ, đắt hơn một view thường. | `KeyboardClipboardChipView.swift` | B2 |
| K2 | Đọc snapshot mà phải chuẩn bị thư mục trước: `KeyboardBootstrapSnapshotStore.init` gọi `AppGroupDirectory.prepare`, tức tạo thư mục, đặt thuộc tính bảo vệ, đặt cờ không backup. Ba syscall ghi chỉ để đọc một file. | `Sources/KeyboardConfiguration/Bootstrap/KeyboardBootstrapSnapshotStore.swift` | B2 |
| K3 | `KeyboardSurfaceInteractionController.init` gọi `haptics.prepare()` cho ba generator. Controller này được tạo ở lần gán presentation đầu tiên, tức trong `viewDidLoad`, nên Taptic Engine bị đánh thức trước khi bàn phím hiện. | `Sources/KeyboardRenderer/Interaction/Controller/KeyboardSurfaceInteractionController.swift` | B2 |
| K4 | Nạp âm phím ngay trong `viewDidLoad` (cùng chỗ với M3). Chạy trên queue riêng nhưng vẫn tranh CPU và gọi audio server đúng lúc bàn phím đang dựng. | như M3 | B2 |
| K5 | Phím có thể được dựng hai lần: bootstrap dựng theo traits mặc định, rồi `textWillChange` báo traits thật của ô nhập; nếu layout khác thì `rebuildKeys()` dựng lại toàn bộ (cùng gốc với M2). | `KeyboardSurfaceView+Presentation.swift` | B3 |
| K6 | Mỗi phím một effect view kính ở theme mặc định `funputGlass`. Đây là diện mạo của sản phẩm nên giữ; chi phí dựng được B4 biến thành một lần mỗi tiến trình. | `Sources/KeyboardRenderer/Keys/KeyboardKeySurfaceView.swift` | B4 |

**Hiện ấm** (tiến trình còn sống, đổi sang app khác):

| # | Chỗ chưa tốt | Sửa |
|---|---|---|
| K7 | Hiện ấm làm gần như toàn bộ việc của khởi động nguội, chỉ trừ bước tạo tiến trình: resolve presentation, dựng surface, decode ảnh nền, mở store gợi ý, mở từ điển sửa lỗi. Trong khi thứ duy nhất thật sự đổi là document của host. | B4, B5 |
| K8 | Cùng lúc đó main thread còn phải huỷ bộ cũ: surface cũ với vài chục effect view bị giải phóng ngay trong lúc bàn phím mới đang hiện. | B4 |

---

## 3. Nguyên tắc

Năm nguyên tắc của STARTUP_AND_MEMORY.md §1 giữ nguyên. Thêm ba nguyên tắc:

6. **Thứ nặng mà không phụ thuộc app host thì thuộc về tiến trình và chỉ có một bản.**
   Controller chỉ giữ những gì gắn với lần trình bày của nó, và những gì mang trạng
   thái của document đang gõ.
7. **Hiện lại là gắn lại, không dựng lại.** Từ lần hiện thứ hai trong cùng một tiến
   trình: không tạo view, không decode ảnh, không mở từ điển. Cùng luật đó cho đổi
   layout: tái dùng phím, không huỷ rồi tạo.
8. **Không có đường code nào tự giết tiến trình vì dữ liệu lúc chạy.** Giá trị đến từ
   cấu hình, từ host, từ đĩa, từ touch hay từ kích thước layout mà sai thì được kẹp về
   giá trị hợp lệ hoặc bỏ qua, không trap.

---

## 4. Kiến trúc đích

```
iOS dựng lại ở mỗi app host                  Sống cùng tiến trình, một bản duy nhất
┌──────────────────────────────┐             ┌────────────────────────────────────────┐
│ KeyboardViewController       │   claim     │ KeyboardRuntime.shared                 │
│  inputView, height constraint│ ──────────▶ │  surface (KeyboardSurfaceView)         │
│  inputCoordinator (engine)   │             │  ảnh nền đã decode                     │
│  presentation của lần hiện   │ ◀────────── │  cấu hình + theme đã resolve           │
│  panel, clipboard monitor    │  surrender  │  suggestion service, correction words  │
└──────────────────────────────┘             │  các store                             │
                                             └────────────────────────────────────────┘
```

### 4.1. Ai giữ cái gì

| Thuộc về tiến trình (`KeyboardRuntime`) | Thuộc về controller |
|---|---|
| Surface chính (`KeyboardSurfaceView`) | `inputView`, `heightController` |
| `backgroundImageCache`, `resolvedBackgroundRequest` | `currentPresentation` |
| `configuration`, `selectedTheme`, `cachedThemedPresentation`, `adoptedIdentity` | `inputCoordinator`, `shortcutsLoader` |
| `personalSuggestionService`, `correctionWords` | `activationState`, `isKeyboardVisible` |
| mọi store (`configurationStore`, `customThemeStore`, `themeAssetStore`, `bootstrapSnapshotStore`, `clipboardStore`, recents) | ba panel, `displayedSurface`, `emojiCatalogCache` |
| | `clipboardRecorder`, `clipboardChangeMonitor`, các task |

Vì sao cột phải giữ những thứ trông có vẻ chia sẻ được:

- **`inputCoordinator`** mang trạng thái của document đang gõ. Một `textDidChange` đến
  muộn từ controller cũ sẽ ghi snapshot của app cũ vào coordinator dùng chung, và lần
  sửa chữ kế tiếp ở app mới sẽ tính số ký tự cần xoá trên sai văn bản. Engine Rust còn
  có trạng thái sống qua `funput_clear` (số chờ ghép của `500k`, bộ đếm sửa lỗi) mà
  Swift không reset được. Tạo engine vốn đã được phép ở `init`.
- **`currentPresentation`**: `textWillChange` của controller mới chạy trước
  `viewWillAppear` và tính layout theo ô nhập mới. Nếu dùng chung, controller cũ sẽ đọc
  layout của controller mới để tính chiều cao của mình.
- **`activationState`**: `allowsClipboardOffer` ghép nó với coordinator của chính
  controller. Dùng chung thì monitor của controller cũ, đang ở ô chữ, có thể mời dán
  lên surface mà controller mới đang hiện trong ô mật khẩu.
- **Panel**: gắn vào `view` của controller và bị thả ở mỗi lần đổi app, nên chia sẻ
  không tiết kiệm gì.

### 4.2. Quyền giữ surface

Tại một thời điểm chỉ một controller giữ surface. Runtime nhớ nó bằng một tham chiếu
**weak**, nên controller chết thì surface tự thành vô chủ.

| Thời điểm | Hành vi |
|---|---|
| `viewDidLoad` | Lấy surface nếu chưa ai giữ hoặc surface đang không gắn vào đâu; ngược lại hoãn |
| `viewWillAppear` | Lấy vô điều kiện |
| `viewDidAppear` | Lấy lại nếu vì lý do nào đó chưa giữ (lưới đỡ) |
| `viewDidDisappear` | Nếu còn giữ: tháo surface khỏi `inputView`, runtime giữ nó |

Trong thực tế đường chính là dòng đầu: controller cũ thường đã `viewDidDisappear` và
tháo surface ra trước khi controller mới được nạp, nên controller mới gắn surface ngay
trong `viewDidLoad`, trước frame đầu.

Một controller được iOS nạp sẵn không cướp được surface của controller đang hiện, vì
surface đang hiện thì đang gắn. Tính đúng không dựa vào thứ tự iOS gọi các callback;
thứ tự chỉ quyết định việc gắn xảy ra sớm hay muộn.

Bốn luật đi kèm:

- **Chỉ người giữ mới ghi được vào surface.** Controller chạm surface qua một
  optional `ownedSurface`, là nil với mọi controller không giữ. Mọi lời gọi của
  controller cũ vì thế tự thành không làm gì, không cần guard ở từng callback.
- **Mất quyền là một lời gọi, không chỉ là một điều kiện.** Lúc trao, runtime gọi
  `surrenderSurface()` trên người giữ cũ: kết thúc activation, dừng clipboard monitor
  và các task, huỷ shortcuts loader, thả panel. `viewWillDisappear` có thể đến muộn
  hoặc không đến.
- **Nhận surface là nhận một surface sạch.** Trước khi nối callback về controller mới,
  surface được đưa về trạng thái đầu: touch pipeline và key repeat đã reset, không
  preview hay bảng alternates, thanh gợi ý trống, không có lời mời dán. Rồi controller
  đẩy presentation và ảnh nền của mình trong một lượt.
- **Trạng thái của phiên nhập không rò sang app khác.** Đổi người giữ là đổi phiên:
  bộ đếm từ của phiên trong `correctionWords` bị xoá; không có Full Access thì các từ
  đã giữ và worker gợi ý in-memory cũng bị bỏ, đúng như một controller mới ngày nay.

A8 vẫn đúng và trở thành hệ quả tự nhiên: surface rời `inputView` cũ ngay lúc được
trao, nên vòng giữ nhau giữa `inputView` và surface không hình thành.
`KeyboardLifetimeCensus` giữ nguyên và phải luôn báo một surface.

---

## 5. Các việc phải làm

Ba PR xếp chồng: B1 và B2; B3; B4 và B5.

### B1: Không còn đường tự giết (T1, T2)

**Thiết kế.**

1. `UIKitTouchCaptureAdapter` bỏ sample có timestamp không hữu hạn trước khi tạo
   `ContactSample`.
2. `KeyboardPresentationFactory` kẹp `heightScale` về cùng khoảng mà
   `KeyboardBootstrapSnapshot` dùng.
3. Xoá `KeyboardEnterAction.validatedCustom`: không ai gọi, và nó chỉ tồn tại để trap.
4. Script `Scripts/Quality/check-keyboard-traps.sh`: đếm `precondition`,
   `preconditionFailure`, `fatalError` (trừ `init(coder:)`), `try!`, `as!` trong
   `Keyboard/` và `Sources/`, so với một allowlist theo file. Thêm trap mới là phải
   sửa allowlist, tức phải có người nhìn. Chạy trong job `shell-line-budget` của
   `.github/workflows/ci.yml`, thêm một step, không đổi tên job.

**Nghiệm thu.** Script xanh trên nhánh, đỏ khi thêm thử một `precondition` mới. Test:
adapter bỏ timestamp NaN và vô cực; factory kẹp `heightScale` 0, âm và quá lớn.

**Đã đổi khi hiện thực.**

- Adapter **không bỏ** sample có timestamp hỏng mà thay timestamp bằng uptime hiện
  tại, đồng hồ mà `UITouch` dùng. Bỏ sample `ended` sẽ để một contact không bao giờ
  kết thúc, tức một phím bị giữ.
- Khoảng 0,85 đến 1,2 thành một hằng số, `FunputConfiguration.heightScaleRange`, dùng
  chung cho snapshot và factory qua `clampedHeightScale`. Giá trị không phải số (NaN,
  vô cực) về chiều cao mặc định.
- Script so **đúng bằng** với allowlist (`Scripts/Quality/keyboard-traps.allowlist`),
  không chỉ "không vượt": gỡ một trap mà quên sửa allowlist cũng đỏ, nên danh sách
  không bị cũ. Force unwrap không đếm được bằng mẫu dòng nên nằm ngoài script.

### B2: Đường khởi động chỉ còn việc của frame đầu, ẩn là nhẹ (K1 đến K4, M3, M4, T5)

**Thiết kế.**

1. `KeyboardClipboardChipView` không tạo `UIPasteControl` cho tới khi nhận hint đầu
   tiên khác nil. `apply(theme:)` chỉ ghi nhớ màu, và chỉ dựng lại control khi control
   đã tồn tại.
2. `KeyboardBootstrapSnapshotStore` tách đường đọc khỏi `AppGroupDirectory.prepare`:
   khởi tạo chỉ ghép URL, `prepare` chỉ chạy trước lần ghi.
3. `haptics.prepare()` rời khỏi `init` của interaction controller. Đổi presentation
   không còn gọi `sounds.prepare`. Cả hai chạy ở `prepareFeedback()`, mà controller
   gọi trong `viewDidAppear`. Lần chạm vẫn tự chuẩn bị như hiện nay.
4. Khi bàn phím ẩn (`viewDidDisappear`): thả các player âm phím và ngừng audio
   session. Lần hiện sau nạp lại trên queue riêng.
5. `flushPersonalSuggestions` nghe `NSExtensionHostDidEnterBackground`.

**Nghiệm thu.** Test `KeyboardRenderer`: chip mới tạo không có `UIPasteControl`, có
hint thì có; interaction controller mới tạo và đổi presentation không gọi `prepare`
trên haptics và sounds giả, `prepareFeedback()` thì gọi; sau khi thả, lần phát kế tiếp
nạp lại. Test `KeyboardConfiguration`: `load()` trên thư mục chưa tồn tại không tạo
thư mục. Kịch bản tay: đang phát nhạc, gõ có âm phím, ẩn bàn phím, nhạc không bị ngắt
hay giảm âm; hiện lại gõ vẫn có âm.

**Đã đổi khi hiện thực.**

- Interaction controller có ba mức: `updateFeedback` chỉ ghi nhớ cài đặt (đường đổi
  presentation), `armFeedback` thêm việc nạp âm (đường chạm, như trước),
  `prepareFeedback` thêm việc đánh thức haptics (đường `viewDidAppear`).
- Chỉ pool `AVAudioPlayer` được thả. Âm phím theo âm lượng chuông là system sound đã
  đăng ký: chúng nằm ở sound server, không giữ audio session, nên để nguyên.
- Hai test cũ ghi file snapshot hỏng thẳng vào thư mục mà `init` từng tạo hộ; giờ
  chúng tự tạo thư mục.
- Chưa có test cho việc nạp lại sau khi thả ở mức `KeyboardKeySounds`: test bundle
  không có audio session, và pool không có điểm chen. Phần đó dựa vào kịch bản tay.

### B3: Tái dùng phím khi đổi layout (M2, K5)

**Thiết kế.** `rebuildKeys()` không huỷ rồi tạo nữa. Surface giữ một **pool**
`KeyboardKeyControl`:

- `KeyboardKeyControl.adopt(_:)` nhận spec mới và làm lại đúng những gì `init` chỉ làm
  một lần: nhãn và cờ accessibility (kể cả gỡ trạng thái placeholder), bật hay tắt
  nhận vuốt ngang, đưa trạng thái nhấn và vuốt về đầu;
- lớp nền của phím phụ thuộc phím thường hay phím chức năng, nên pool ưu tiên trao
  control cùng nhóm để lớp kính không phải dựng lại;
- layout mới nhiều phím hơn thì tạo thêm phần thiếu; ít hơn thì ẩn phần dư, không huỷ.
  Trần của pool là layout nhiều phím nhất;
- `keyControls` vẫn là từ điển theo `spec.id`, dựng lại từ pool sau mỗi lần gán, nên
  các nơi tra theo id không đổi;
- luật hiện có giữ nguyên: ngón tay đang đặt trên phím vẫn commit theo layout nó chạm
  vào (`suspendPresentation` và snapshot geometry theo từng contact).

**Nghiệm thu.** Test `KeyboardRenderer`: chuyển chữ → số → ký hiệu → chữ, số control
được tạo không tăng sau vòng đầu; mỗi phím sau khi gán lại có đúng nhãn, vai trò,
alternates, accessibility và trạng thái Shift của spec mới; placeholder của keypad bật
lại được thành phím thường; các test rollover khi đổi layout hiện có vẫn xanh. Ảnh
thumbnail theme (`Scripts/export-theme-thumbnails.sh`) không đổi.

**Đã đổi khi hiện thực.**

- Pool là một kiểu riêng, `KeyboardKeyControlPool`
  (`Sources/KeyboardRenderer/Surface/Keys/`); `rebuildKeys()` chỉ còn gọi nó. Control
  dư được `park()`: ẩn, không nhận touch, không là phần tử accessibility, nhưng vẫn
  nằm trong cây view để keycap không bị dựng lại.
- Pan recognizer cho vuốt ngang được tạo lần đầu một control nhận phím có hành động
  vuốt, rồi bật hay tắt theo phím; trước đây nó chỉ được tạo trong `init`.
- `KeyboardKeyControl.swift` đã ở trần 150 dòng, nên phần tương tác chuyển sang
  `KeyboardKeyControl+Interaction.swift`.
- Test so từng control tái dùng với một control dựng mới cho cùng spec (nhãn, vai
  trò, accessibility, custom action, recognizer), thay cho việc liệt kê từng thuộc
  tính.
- **Không chạy lại script thumbnail.** Nó mã hoá lại ảnh HEIC trên simulator đang có,
  nên ảnh khác đi không nói được gì về thay đổi này; đường dựng lần đầu của surface,
  thứ thumbnail chụp, không đổi.

### B4: Một surface cho cả tiến trình (T3, M1, K6, K7, K8)

**Thiết kế.** Như §4.1 và §4.2, cho surface, ảnh nền, cấu hình và theme đã resolve,
các store.

- `Sources/KeyboardRenderer/Surface/Hosting/`: `KeyboardSurfaceHost` (giữ surface và
  người giữ), `KeyboardSurfaceClaimPolicy` (hàm thuần quyết định giữ, lấy hay hoãn),
  `KeyboardSurfaceClaimant` (thứ một người giữ phải làm được). Nằm trong package để
  test được mà không cần `UIInputViewController`.
- `Keyboard/Runtime/`: `KeyboardRuntime` (`@MainActor`, một instance cho tiến trình)
  và các extension của controller cho quyền giữ surface.
- `KeyboardActivationIdentity` thêm cờ Giảm độ trong suốt: theme đã resolve có chứa
  nó, và identity giờ sống cùng tiến trình.
- Memory warning thả ảnh nền khi không còn ai giữ surface, hoặc người giữ đang ẩn.

**Rủi ro đã biết.** Chuyển một surface có `UIGlassEffect` sang `inputView` của một
controller khác chưa từng được làm trong code này. Nếu kính hiển thị sai sau khi
chuyển, lùi về phương án: giữ surface theo controller như hiện nay, vẫn chia sẻ ảnh
nền, theme đã resolve, store và service. Ghi lại lựa chọn ở mục **Đã đổi khi hiện
thực**.

**Nghiệm thu.** Test `KeyboardRenderer`: bảng chân trị của policy; host giữ một
instance qua nhiều lần claim, đúng bốn constraint sau nhiều lần gắn, người giữ cũ được
báo đúng một lần, lệnh tháo của người giữ cũ bị bỏ qua, người giữ đã chết coi như
trống; surface sau reset không còn gợi ý, hint, preview hay touch đang theo dõi. Kịch
bản tay trên simulator: đổi harness và Settings 10 lần, cùng một tiến trình và cùng
một surface, không có log fault của `KeyboardLifetimeCensus`; đổi app khi đang mở
emoji hoặc `?123` thì app sau bắt đầu ở bàn phím chữ; giữ Backspace trong lúc đổi app
không xoá gì ở app mới; đổi theme trong app chính rồi quay lại thì bàn phím nhận theme
mới; xoay máy; bật Giảm độ trong suốt.

**Đã đổi khi hiện thực.**

- Quyền giữ là một tham chiếu weak trong `KeyboardSurfaceHost`, không có kiểu lease
  hay số generation: "ai đang giữ" trả lời được bằng một phép so sánh.
- `KeyboardRuntime` chỉ giữ thứ **không** mang trạng thái của document (bảng §4.1).
  Bản thiết kế đầu định chuyển cả `currentPresentation`, `activationState`, panel và
  coordinator; lý do giữ lại nằm ngay dưới bảng đó.
- `applyAdoptedPresentation(claimingAt:)` tính presentation trước rồi mới xin surface,
  nên surface dựng lần đầu thấy presentation cuối cùng, còn surface nhận lại từ app
  khác được đẩy presentation và ảnh nền trong một lượt (`apply(presentation:
  backgroundImage:)` có sẵn).
- Reporter chẩn đoán touch của bản DEBUG được tạo khi surface được bind và kết thúc khi
  controller mất surface; để theo controller như cũ thì controller cũ sẽ gỡ observer
  của controller mới.
- `KeyboardLifetimeCensus` không còn báo fault khi có hơn một controller: đó là cách
  iOS chạy (T3). Nó báo khi có surface thứ hai.

**Đã kiểm trên simulator** (iPhone 17 Pro Max, iOS 27.2, bản Debug, harness và
Settings, 12 lần hiện): cùng một tiến trình, cùng một địa chỉ `KeyboardSurfaceView`,
một lần `PrimarySurfaceCreation` duy nhất, 43 `KeyboardKeyControl` suốt; kính hiển thị
đúng sau mỗi lần chuyển; rời app ở trang ký hiệu thì app sau bắt đầu ở trang chữ, phím
Enter đúng kiểu của ô mới; chữ gõ vào đúng ô của app đang hiện. Cùng kịch bản trên bản
trước B4 cho hai surface sống cùng lúc.

**Chưa kiểm:** máy thật bản Release; ô mật khẩu trong Safari; nút dán sau khi chuyển
app; memory warning; xoay máy; đổi theme và Giảm độ trong suốt giữa hai app.

Riêng nút dán: simulator dùng để kiểm không có Full Access, nên bàn phím không bao giờ
mời dán. `ClipboardPasteUITests` vì thế đỏ cả hai ca ở đó, và đỏ y hệt trên bản trước
B1 (cùng dòng, cùng lý do), nên chúng không nói được gì về B2 hay B4. Việc dựng
`UIPasteControl` muộn (B2) mới chỉ được kiểm bằng unit test; cần chạy bộ UI test này
trên một simulator hoặc máy có Full Access. `KeyboardHeightRefreshUITests` có một ca
đỏ sẵn trên `main`, trước và sau thay đổi đều cho cùng kết quả.

**Còn lại, có từ trước và không do B4.** Mỗi lần hiện để lại một
`KeyboardAudioInputView` (`inputView` của controller cũ) không được thả: 12 lần hiện
là 12 cái, trên bản trước B4 cũng vậy. Thứ giữ nó là `_UIInputViewContent` của UIKit
qua một associated object, không phải code của Funput. Sau A8 nó không còn kéo theo
surface nên nhẹ, nhưng vẫn là tăng không có trần.

### B5: Một suggestion service, một từ điển sửa lỗi (T4, M1, K7)

**Thiết kế.** `personalSuggestionService` và `correctionWords` chuyển sang runtime.

- Một service là một worker, nên store gợi ý cá nhân chỉ được mở một lần và không còn
  hai worker trên cùng một file. Lexicon và từ điển sửa lỗi chỉ mở một lần mỗi tiến
  trình.
- Controller dùng service qua một optional chỉ có với người giữ surface, cùng luật
  với `ownedSurface`: prefix đang gõ ở app cũ không thể được hoàn thành trên thanh gợi
  ý của app mới.
- Đổi người giữ thì dọn phiên như §4.2.

**Nghiệm thu.** Test: sau khi dọn phiên, bộ đếm từ của phiên trống; có Full Access ở
cả hai phía thì worker được giữ, thiếu ở một phía thì worker được tạo lại. Kịch bản
tay: gõ dở một từ ở app A, đổi sang app B, gõ tiếp thì thanh gợi ý không dính prefix
cũ; từ vừa học ở A được gợi ý ở B; ô mật khẩu vẫn tắt gõ tiếng Việt, tắt học từ, tắt
mời dán.

**Đã đổi khi hiện thực.**

- Dọn phiên chạy trong `bind`, tức đúng lúc surface đổi người giữ:
  `CorrectionWordSource.forgetSession()` xoá bộ đếm và các từ đã giữ (có Full Access
  thì chúng được nạp lại từ store ngay ở activation),
  `PersonalSuggestionService.beginSession(hasFullAccess:)` xoá thanh gợi ý và bỏ
  worker khi một trong hai phía không có Full Access.
- `followSeedLanguage()` chạy trong `bind` thay vì trong bootstrap: service là của
  chung, nên chỉ nghe ngôn ngữ của controller khi controller đó thành bàn phím.
- `flushPersonalSuggestions` không bị chặn theo quyền giữ: flush chỉ ghi những gì đã
  học, và controller vừa mất bàn phím chính là nơi nên gọi nó.

### B6: đã bỏ

Bản đầu của tài liệu có một chế độ an toàn: lùi về bàn phím tối thiểu sau hai lần
bootstrap chết liền, dựa trên một số đếm ghi xuống đĩa. Khảo sát cho B1 không tìm
thấy đường nào làm bootstrap chết, còn cơ chế thì thêm hai lần ghi đĩa vào mỗi lần
khởi động nguội. Không làm.

---

## 6. Sửa hợp đồng khởi động

Áp vào bảng §2 của STARTUP_AND_MEMORY.md, trong PR tương ứng:

| Chặng | Thay đổi | PR |
|---|---|---|
| `viewDidLoad` | Bỏ khỏi cột được phép: chuẩn bị thư mục App Group. Đọc snapshot không còn ghi gì xuống đĩa | B2 |
| Sau `viewDidAppear` | Thêm: chuẩn bị haptics và âm phím. Không còn chạy trong `viewDidLoad` | B2 |
| `viewDidLoad` | Tiến trình đã có surface và surface đang rảnh: gắn surface có sẵn, không tạo view | B4 |
| `viewWillAppear` | Thêm: nhận surface còn đang gắn ở controller khác. Không tạo view | B4 |

---

## 7. Giới hạn còn lại

- **Cache ảnh emoji của CoreText** vẫn không trả lại được: xem hết catalog để lại
  khoảng 30 MB cho tới khi tiến trình chết. Vòng này không đổi điều đó; nó chỉ bảo đảm
  phần còn lại của tiến trình không cộng thêm vào: không còn bản thứ hai của surface,
  ảnh nền hay engine gợi ý.
- **Mỗi controller vẫn tạo một engine gõ riêng** (§4.1). Engine không nạp từ điển nên
  nhẹ, và đó là cái giá để văn bản không bao giờ bị tính sai giữa hai app.
- **iOS vẫn được dọn tiến trình khi đang ẩn**, và các trường hợp ở §1.1 vẫn ra bàn
  phím hệ thống. Không có gì trong extension đổi được hai điều này.

---

## 8. Ràng buộc cho agent hiện thực

- Mỗi PR kèm cập nhật tài liệu này và bảng §2, §3.2 của STARTUP_AND_MEMORY.md.
- Giữ mọi ràng buộc ở §6 và các bất biến ở §7 của tài liệu kia: không đổi hành vi ô
  mật khẩu, không thêm lần đọc nội dung pasteboard, giữ mọi signpost hiện có.
- Không thêm nhật ký, bộ đếm thống kê hay bất kỳ dữ liệu chẩn đoán nào ghi xuống đĩa.
- `Scripts/check-swift-loc.sh`: mỗi file Swift tối đa 150 dòng.
  `Scripts/Quality/check-keyboard-touch-layout.sh`: tối đa 5 file Swift mỗi thư mục
  được liệt kê, nên code mới nằm trong thư mục con mới (`Keyboard/Runtime/`,
  `Sources/KeyboardRenderer/Surface/Hosting/`).
- Code và comment bằng tiếng Anh. Chữ trên giao diện bằng tiếng Việt, câu tự nhiên,
  không dùng `·` hay `—`.
- Sau B3, B4 và B5 chạy lại kịch bản của PR #642 (emoji) và kịch bản đổi app của A7.
