# Hiện thực "Phụ âm đầu mở rộng" (z, f, w, j) trên iOS

Tài liệu bàn giao cho người (hoặc AI agent) hiện thực tính năng trên bàn phím iOS. Mọi
đường dẫn tính từ `app/` (git root). Đọc hết mục 1–3 trước khi sửa code.

## 1. Bối cảnh

Funput có tuỳ chọn tương đương "Cho phép phụ âm đầu Z, F, W, J" của UniKey: cho teencode,
từ mượn và tên riêng nhận dấu — `zô`, `zui`, `jờ`, `fải`, `wá`, `Cư Jút`. **Mặc định tắt.**
Người dùng **chọn từng chữ** (chỉ `z`, hoặc `f`+`j`, …).

Logic đã xong ở các tầng dùng chung, iOS chỉ cần nối dây + cài đặt + giao diện:

| Tầng | Đã có | PR |
|---|---|---|
| `funput-core` | `ExtraOnsets` (F/J/W/Z), `SyllableRules`, `ComposeOptions` | Funput/Funput#505 |
| `funput-engine` | `EngineConfig.syllable_rules` | Funput/Funput#505 |
| `funput-ffi` | `funput_set_extra_onsets(engine, uint8_t letters)` + `ONSET_F=1`, `ONSET_J=2`, `ONSET_W=4`, `ONSET_Z=8` (`0` = tắt) | Funput/Funput#506 |
| macOS | Settings → Cách gõ (tham khảo cách làm) | Funput/Funput#506 |

Tài liệu tính năng (hành vi, đánh đổi, số liệu): `docs/features/extra-onsets.md`.

### Điều kiện bắt đầu

- #505 **và** #506 đã merge vào `main` (iOS cần setter FFI của #506). Nếu chưa merge,
  tách nhánh từ `feat/macos-extra-onsets` và mở PR với base đó.
- Nhánh: `feat/ios-extra-onsets` tách từ `origin/main`.
- Sau khi kéo code mới: **phải dựng lại xcframework** để header có `funput_set_extra_onsets`:
  `cd platforms/ios && ./Scripts/bootstrap-ios.sh` (gọi `Scripts/build-ffi.sh`, ra
  `Frameworks/FunputCore.xcframework`).

## 2. Hành vi cần đạt

Chữ được bật cư xử **y như một phụ âm đầu tiếng Việt**: vần vẫn phải là vần tiếng Việt.

| Bật | Gõ (Telex) | Kết quả |
|---|---|---|
| `z` | `zoo ` · `fair ` | `zô ` · `fair ` (f chưa bật) |
| `z`, `f` | `fair ` · `food ` | `fải ` · `food ` (vần `od` không có → giữ tiếng Anh) |
| cả 4 | `jowf was ` | `jờ wá ` |
| cả 4, VNI | `zo6 jo72 ` | `zô jờ ` |
| cả 4, Telex nâng cao | `wa wwas ` | `ưa wá ` (`w` đầu từ vẫn là `ư`; gõ `ww` để có phụ âm w) |
| tắt | mọi thứ trên | như hiện nay |

Đánh đổi (ghi trong giao diện): từ tiếng Anh có vần tiếng Việt cũng được bỏ dấu
(`fast` → `fát`, `for` → `fỏ`); giữ tiếng Anh bằng cách gõ đúp phím dấu (`fasst` → `fast`).

## 3. Kiến trúc và luồng dữ liệu trên iOS

```
App (Settings UI) ──ghi──▶ FunputConfiguration (App Group, JSON)
                                   │  đọc khi bàn phím mở / config đổi
                                   ▼
KeyboardInputCoordinator.apply(_ configuration:)       [KeyboardInput]
        │  map FunputShared → FunputEngine
        ▼
FunputCompositionOptions ──▶ FunputComposer.configure(_:)   [FunputEngine]
        │                         ├─ funput_configure(...)
        │                         └─ funput_set_extra_onsets(...)   ← mới
        └─▶ coordinator.compositionOptions ──▶ LocalTextComposer (ô tìm emoji)
```

Ràng buộc module (xem `Packages/FunputKit/Package.swift`):
- `FunputShared` **không** phụ thuộc `FunputEngine` → định nghĩa kiểu cấu hình riêng ở
  `FunputShared`, kiểu phía engine riêng ở `FunputEngine`, ánh xạ ở `KeyboardInput` (giống
  `ToneStyleOption` → `FunputToneStyle` qua `engineToneStyle`).
- File chạm C (`import FunputCore`) được bọc `#if os(iOS) && canImport(FunputCore)`;
  `FunputCompositionTypes.swift` **không** import C để test chạy được trên host. Vì vậy giá
  trị bit phía engine viết literal, và có test so với macro `ONSET_*` (mục 5).
- `LocalTextComposer` (ô tìm emoji gõ tiếng Việt) cấu hình từ `compositionOptions`, nên khi
  trường mới nằm trong `FunputCompositionOptions` và `configure` đẩy nó xuống, ô tìm kiếm
  tự nhận theo. Không cần sửa `LocalText/`.

Không có xuất/nhập file cấu hình (`app.funput.config`) trên iOS — bỏ qua phần đó.

## 4. Các bước hiện thực

### Bước 1 — Engine wrapper (`FunputKit/Sources/FunputEngine/`)

`FunputCompositionTypes.swift` (72 dòng):

```swift
/// Letters admitted as initial consonants beyond Vietnamese spelling — the C ABI's
/// `ONSET_*` bits, written out because this file stays free of the C import.
public struct FunputExtraOnsets: OptionSet, Hashable, Sendable {
    public let rawValue: UInt8
    public init(rawValue: UInt8) { self.rawValue = rawValue }

    public static let f = FunputExtraOnsets(rawValue: 1) // ONSET_F
    public static let j = FunputExtraOnsets(rawValue: 2) // ONSET_J
    public static let w = FunputExtraOnsets(rawValue: 4) // ONSET_W
    public static let z = FunputExtraOnsets(rawValue: 8) // ONSET_Z
}
```

Thêm vào `FunputCompositionOptions`: `public var extraOnsets: FunputExtraOnsets` và tham số
init **có mặc định** `extraOnsets: FunputExtraOnsets = []` (đặt cuối) để mọi chỗ gọi cũ vẫn
biên dịch. Cập nhật doc comment của struct (nói rằng trường này đi qua setter riêng).

`FunputComposer.swift` — trong `configure(_:)`, ngay sau `funput_configure(...)`:

```swift
// Its own FFI call: `FunputConfig` crosses the ABI by value and must not grow.
funput_set_extra_onsets(handle, options.extraOnsets.rawValue)
```

File đang 98 dòng, còn chỗ. Nếu sát 150, chuyển sang
`FunputComposer+ExtraOnsets.swift` — nhưng lưu ý `handle` phải truy cập được (xem
`FunputComposer+Shortcuts.swift` đang dùng `handle` từ extension).

### Bước 2 — Cấu hình dùng chung (`FunputKit/Sources/FunputShared/Configuration/`)

Kiểu mới `ExtraOnsetLetters.swift` (thư mục đang có 3 file):

```swift
/// Letters the user admits as initial consonants (`zô`, `fải`, `wá`, `jờ`). Stored in
/// the App Group as the letters spelled out (`"zj"`), the same form as the desktop
/// config files (`platforms/CONFIG_FORMAT.md`, `preferences.extraOnsets`).
public struct ExtraOnsetLetters: OptionSet, Codable, Hashable, Sendable {
    public let rawValue: UInt8
    public init(rawValue: UInt8) { self.rawValue = rawValue }

    public static let z = ExtraOnsetLetters(rawValue: 1 << 0)
    public static let f = ExtraOnsetLetters(rawValue: 1 << 1)
    public static let w = ExtraOnsetLetters(rawValue: 1 << 2)
    public static let j = ExtraOnsetLetters(rawValue: 1 << 3)
    public static let all: ExtraOnsetLetters = [.z, .f, .w, .j]

    public struct Letter: Identifiable, Hashable, Sendable {
        public let member: ExtraOnsetLetters
        public let symbol: String     // "z"
        public let examples: String   // "zô, zui"
        public var id: UInt8 { member.rawValue }
    }

    /// Display order.
    public static let letters: [Letter] = [
        Letter(member: .z, symbol: "z", examples: "zô, zui"),
        Letter(member: .f, symbol: "f", examples: "fải, fan"),
        Letter(member: .w, symbol: "w", examples: "wá, wê"),
        Letter(member: .j, symbol: "j", examples: "jờ, Cư Jút"),
    ]

    public var configValue: String { … }        // "zfwj" theo thứ tự `letters`
    public init(configValue: String) { … }      // không phân biệt hoa thường, bỏ chữ lạ

    // Codable dưới dạng một chuỗi (singleValueContainer) dùng configValue.
}
```

Bit ở đây là của `FunputShared`, **không** cần trùng `ONSET_*` — ánh xạ ở Bước 3. (Có thể
dùng cùng giá trị cho dễ đọc, nhưng đừng ép kiểu thẳng `rawValue` qua lại.)

`FunputConfiguration.swift` (119 dòng): thêm
- thuộc tính `public var extraOnsets: ExtraOnsetLetters` + doc comment ngắn;
- `extraOnsets` vào `CodingKeys`;
- tham số init `extraOnsets: ExtraOnsetLetters = []` và gán.

`FunputConfiguration+Codable.swift` (102 dòng): giải mã khoan dung như mọi dòng khác:
`config.extraOnsets = try container.decodeIfPresent(ExtraOnsetLetters.self, forKey: .extraOnsets) ?? config.extraOnsets`.
Không có hàm `encode` tự viết — mã hoá tổng hợp dùng `CodingKeys` nên tự có. **Không**
tăng `schemaVersion` (khoá thiếu = mặc định là đủ).

### Bước 3 — Ánh xạ sang engine (`KeyboardInput/Configuration/KeyboardInputCoordinator+Configuration.swift`)

Trong `apply(_:)`, khi dựng `FunputCompositionOptions`, thêm
`extraOnsets: configuration.extraOnsets.engineOnsets`. Cuối file, cạnh `engineToneStyle`:

```swift
extension ExtraOnsetLetters {
    var engineOnsets: FunputExtraOnsets {
        var onsets: FunputExtraOnsets = []
        if contains(.z) { onsets.insert(.z) }
        if contains(.f) { onsets.insert(.f) }
        if contains(.w) { onsets.insert(.w) }
        if contains(.j) { onsets.insert(.j) }
        return onsets
    }
}
```

### Bước 4 — Settings UI (app `Funput/Settings/`)

Thư mục `Funput/Settings/` đã có nhiều file — **tạo thư mục con** `Funput/Settings/ExtraOnsets/`
(≤5 file, ≤150 dòng/file):

- `SettingsModel+ExtraOnsets.swift` — extension của `SettingsModel` (file chính đang 146
  dòng, không thêm vào đó):
  - `var extraOnsetsEnabledBinding: Binding<Bool>` — get `!configuration.extraOnsets.isEmpty`;
    set `true` → `.all` (giống UniKey), `false` → `[]`.
  - `func extraOnsetBinding(_ member: ExtraOnsetLetters) -> Binding<Bool>` — chèn/bỏ một chữ.
  - Ghi qua đúng đường lưu sẵn có: xem `boolBinding` dùng `update(keyPath, to:)` trong
    `SettingsModel.swift`; dùng `update(\.extraOnsets, to: newValue)` (nếu `update` đang chỉ
    nhận `WritableKeyPath<FunputConfiguration, Bool>`, thêm overload generic hoặc gọi chung
    hàm lưu mà `update` dùng — đừng tự ghi App Group).
- `ExtraOnsetsSettingsCard.swift` — `SettingsSectionCard(title: "Phụ âm đầu mở rộng",
  systemImage: "character.cursor.ibeam", footer: …)`:
  1. `SettingsToggleRow(title: "Cho phép z, f, w, j đầu từ", summary: "Gõ teencode, từ mượn,
     tên riêng: zô, fải, wá, jờ.", systemImage: "character.cursor.ibeam",
     isOn: model.extraOnsetsEnabledBinding)`.
  2. Khi bật: với mỗi `ExtraOnsetLetters.letters` → `SettingsRowDivider()` +
     `SettingsToggleRow(title: "Chữ \(symbol)", summary: examples,
     systemImage: "\(symbol).square", isOn: model.extraOnsetBinding(member))`.
     (SF Symbols có sẵn `z.square`, `f.square`, `w.square`, `j.square`.) Mỗi chữ một switch
     là control gốc của iOS — đừng làm chip/ô tự vẽ: bản macOS đầu tiên dùng chip và người
     dùng không nhận ra là bấm được.
  3. `footer`: "Từ tiếng Anh có vần tiếng Việt cũng được bỏ dấu (fast → fát) — gõ đúp
     phím dấu (fasst) để giữ tiếng Anh." Nếu `inputMethod == .telexAdvanced` và có `w`:
     thêm "Telex nâng cao: w vẫn là ư — gõ ww để có phụ âm w."
  - Hiện/ẩn các dòng chữ có `.animation(.snappy, value:)`, tôn trọng
    `accessibilityReduceMotion`. Bỏ switch chữ cuối cùng → switch chính tự tắt (suy ra từ
    `isEmpty`, không cần code riêng).
- `Funput/Settings/SettingsScreen.swift`: chèn `ExtraOnsetsSettingsCard(model: model)` ngay
  sau `SmartInputSettingsSection(model: model)` (dòng ~43).

Giữ đúng phong cách thẻ/hàng hiện có (`SettingsSectionCard`, `SettingsToggleRow`,
`SettingsRowDivider` trong `Funput/Settings/`), không thêm design system mới. Chuỗi giao
diện viết thẳng tiếng Việt như các thẻ khác.

## 5. Test (Swift Testing: `@Test`, `#expect`)

Chạy bằng `./Scripts/test-funput-kit.sh` (không cần Xcode project).

- `Tests/FunputSharedTests/` — `ExtraOnsetLettersTests.swift` (mới):
  `configValue` (`[.j, .z]` → `"zj"`, `.all` → `"zfwj"`), `init(configValue:)` bỏ chữ lạ,
  không phân biệt hoa thường; Codable round-trip dạng chuỗi.
  `FunputConfigurationTests.swift`: mặc định `[]`; JSON cũ không có khoá → `[]`; round-trip
  giữ `extraOnsets`.
- `Tests/FunputEngineTests/FunputConfigureTests.swift` (chỉ chạy khi có FunputCore, theo
  cách các test hiện có làm):
  - bit `FunputExtraOnsets` khớp macro: `.f.rawValue == UInt8(ONSET_F)` … (bảo vệ giá trị
    literal ở Bước 1);
  - `[.z]`: gõ `zoo` → buffer `zô`; `fair` + tone → giữ `fair`;
  - `[.z, .f]`: `fair` → `fải`; `[]`: `zoo` → `zoo`;
  - gọi `configure` lần hai với `[]` tắt được (setter không bị `funput_configure` ghi đè).
- `Tests/KeyboardInputTests/` — một test ở nhóm `Coordinator/` (hoặc `Composition/`):
  `apply(FunputConfiguration(extraOnsets: [.z]))` rồi gõ `zoo` qua coordinator → `zô`;
  ánh xạ `engineOnsets` đủ 4 chữ.

## 6. Kiểm chứng trước khi mở PR

```bash
cd platforms/ios
./Scripts/bootstrap-ios.sh          # nếu header chưa có funput_set_extra_onsets
./Scripts/test-funput-kit.sh
./Scripts/check-swift-loc.sh        # 150 dòng/file, 5 file/thư mục tính năng
xcodebuild -project Funput.xcodeproj -scheme Funput \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

Test iOS **không** chạy trên CI của PR (chỉ `check-swift-loc.sh`) — phải chạy ở máy.
Build đầy đủ rất lâu (~30 phút lần đầu); dùng lại `-derivedDataPath` cố định giữa các lần.

Test tay (người review): Settings → "Phụ âm đầu mở rộng": bật → 4 dòng chữ hiện ra, chỉ
giữ `z`; ở Notes gõ Telex `zoo ` → `zô `, `fair ` → `fair `; bật `f` → `fair ` → `fải `,
`food ` vẫn `food `; VNI `zo6 jo72 ` → `zô jờ `; Telex nâng cao + `w`: `wa` → `ưa`,
`wwas` → `wá`; ô tìm emoji gõ `zoo` → `zô`; tắt switch chính → như cũ. Kiểm tra Light/Dark,
Dynamic Type lớn, VoiceOver đọc đúng tên từng chữ.

## 7. Bẫy thường gặp

- Quên dựng lại xcframework → lỗi `cannot find 'funput_set_extra_onsets'`.
- Ép `rawValue` thẳng giữa `ExtraOnsetLetters` (FunputShared) và `FunputExtraOnsets`
  (engine): hai tập bit độc lập, luôn đi qua `engineOnsets`.
- Thêm trường vào `FunputConfig` (C struct): **cấm** — ABI truyền theo giá trị; dùng setter.
- `FunputComposer.configure` cố ý ép `auto_capitalize: false` — giữ nguyên.
- Đừng tăng `schemaVersion`; giải mã khoan dung đã lo khoá thiếu.
- Không thêm file vào thư mục đã ≥5 file; tạo thư mục con theo tính năng.

## 8. Commit và PR

- Commit theo thứ tự: (1) `feat(ios): carry extra onsets from settings to the engine`
  (FunputEngine + FunputShared + KeyboardInput + test), (2) `feat(ios): choose the extra
  onsets z, f, w, j one by one in Settings` (UI), (3) `docs: …` — cập nhật bảng trạng thái
  trong `docs/features/extra-onsets.md` (dòng iOS) và README iOS nếu có bảng tính năng.
- Commit message: tiếng Anh, prose, có trailer `Co-Authored-By` theo quy ước repo.
- PR: tiêu đề `feat(ios): let users pick the extra onsets z, f, w, j`, **mô tả tiếng Việt**
  (Tóm tắt · Thay đổi · Kiểm chứng · Test tay), base `main` (hoặc nhánh của #506 nếu chưa merge).

## 9. Hoàn thành khi

- [ ] Header trong xcframework có `funput_set_extra_onsets`; `FunputComposer.configure` gọi nó.
- [ ] `FunputConfiguration.extraOnsets` lưu/đọc qua App Group, khoá thiếu = `[]`.
- [ ] Bàn phím và ô tìm emoji nhận lựa chọn mà không cần khởi động lại app chủ.
- [ ] Settings có thẻ "Phụ âm đầu mở rộng": switch chính + một switch mỗi chữ + footer đánh đổi.
- [ ] `test-funput-kit.sh`, `check-swift-loc.sh`, `xcodebuild … build` xanh.
- [ ] Tài liệu trạng thái cập nhật; PR mở đúng quy ước.
