<p align="center">
  <img
    src="../../assets/horizontal-lockup/gradient.png"
    width="420"
    alt="Funput"
  >
</p>

<p align="center">
  <strong>Funput cho macOS</strong> — Input Method native, tích hợp thẳng vào hệ thống.<br>
  SwiftUI + InputMethodKit · gõ bằng preedit · engine Rust qua
  <code>funput-ffi</code>
</p>

<p align="center">
  <a href="https://github.com/Funput/Funput/releases/latest">
    <img src="https://img.shields.io/badge/Tải_xuống-Bản_mới_nhất-22C55E?style=for-the-badge&logo=github&logoColor=white" alt="Tải Funput macOS">
  </a>
  <a href="https://docs.funput.app/docs/install/macos">
    <img src="https://img.shields.io/badge/Tài_liệu-Hướng_dẫn_cài_đặt-2563EB?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Hướng dẫn cài đặt">
  </a>
  <a href="https://github.com/Funput/Funput/issues">
    <img src="https://img.shields.io/badge/Hỗ_trợ-Báo_lỗi-E11D48?style=for-the-badge&logo=github&logoColor=white" alt="Báo lỗi">
  </a>
</p>

---

<p align="center">
  <img
    src="../../assets/screenshot/macOS.png"
    width="640"
    alt="Giao diện Cài đặt Funput trên macOS"
  >
  <br>
  <sub>Cửa sổ Cài đặt — Tổng quan, Cách gõ, Phím tắt, Gõ tắt.</sub>
</p>

## Tính năng

| | |
|---|---|
| ⌨️ **Ba kiểu gõ** | Telex · **Telex nâng cao** (thêm `w` đầu từ và phím tắt `[` `]`) · VNI |
| 🔤 **Kiểu đặt dấu** | Truyền thống (`hòa`, `khỏe`) hoặc hiện đại (`hoà`, `khoẻ`) |
| 🧠 **Gõ thông minh** | Tự khôi phục từ tiếng Anh · khôi phục tức thì · kiểm tra chính tả · tự động viết hoa |
| ↩️ **Bỏ dấu sau Backspace** | Xoá lùi rồi gõ dấu khác, Funput đặt lại dấu cho đúng từ |
| 🗂️ **Nhớ theo ứng dụng** | Bật tiếng Việt ở Pages, tắt ở Terminal — Funput tự chuyển khi bạn đổi app |
| ✂️ **Gõ tắt** | Bảng viết tắt tự bung, tuỳ chọn khớp cả hoa lẫn thường |
| 🔄 **Chuyển mã** | Đổi qua lại giữa Unicode dựng sẵn, Unicode tổ hợp, TCVN3 (ABC) và VNI-Windows |
| 💾 **Xuất / nhập cấu hình** | Mang toàn bộ tuỳ chọn và gõ tắt sang máy khác bằng một tệp |
| 🍎 **Hợp chuẩn macOS 26** | Liquid Glass, thanh menu, khởi động cùng máy |

## Yêu cầu

| | |
|---|---|
| **Hệ điều hành tối thiểu** | **macOS 26 (Tahoe)**. macOS 27 được hỗ trợ đầy đủ |
| **Chip** | **Apple Silicon và Intel** — bản phát hành là universal binary (`arm64` + `x86_64`) |
| **Cài đặt** | `.pkg` *(cần quyền admin)* hoặc `.app.zip` *(không cần admin)* |
| **Chữ ký** | Ký **Developer ID** và **notarized** — Gatekeeper không chặn |

> [!IMPORTANT]
> **macOS 26 là sàn cứng, không phải khuyến nghị.** `MACOSX_DEPLOYMENT_TARGET` của dự án là
> `26.0`, và `LSMinimumSystemVersion` trong `Info.plist` lấy thẳng giá trị đó. Trên macOS 15
> (Sequoia) trở xuống, hệ thống **từ chối nạp bundle** — không có chế độ chạy giới hạn nào cả.
>
> Sàn này đến từ việc giao diện dựng trên các API SwiftUI của macOS 26 (Liquid Glass —
> xem [`docs/LIQUID_GLASS.md`](docs/LIQUID_GLASS.md)), không phải từ engine gõ.

## Cài đặt

Tải bản mới nhất từ [GitHub Releases](https://github.com/Funput/Funput/releases/latest). Mỗi bản
macOS có hai lựa chọn:

| File | Cài vào | Khi nào chọn |
|---|---|---|
| `Funput-*.pkg` | `/Library/Input Methods/` | Bạn có quyền admin — một bước, dùng chung cho mọi tài khoản |
| `Funput-*.app.zip` | `~/Library/Input Methods/` | Máy công ty, tài khoản Standard — **không cần admin** |

Kiểm tra toàn vẹn trước khi cài, so với asset `.sha256` cùng bản phát hành:

```bash
shasum -a 256 Funput-*.pkg
```

### Bật bộ gõ

Cài xong **chưa gõ được ngay** — macOS cần bạn thêm Funput vào nguồn nhập:

1. Mở **System Settings** → **Keyboard** → **Input Sources**
2. Nhấn **+** → chọn **Vietnamese** → **Funput** → **Add**
3. Chuyển sang Funput bằng phím tắt đổi bộ gõ của macOS (thường là `⌃Space` hoặc phím 🌐)

> [!TIP]
> Không thấy Funput trong danh sách? **Đăng xuất rồi đăng nhập lại** một lần — macOS chỉ quét lại
> cơ sở dữ liệu Input Method khi phiên đăng nhập bắt đầu.

### Tìm Funput ở đâu sau khi cài

Bundle Input Method nằm trong `Library/Input Methods` nên **Spotlight không thấy nó**. Vì vậy
Funput tự đặt thêm một app stub tí hon:

| Cách cài | Stub nằm ở |
|---|---|
| `.pkg` | `/Applications/Funput.app` |
| `.app.zip` hoặc `install.sh` | `~/Applications/Funput.app` *(app tự tạo ở lần chạy đầu)* |

Stub này không phải bộ gõ — nó chỉ mở `funput://settings`, để bạn gõ “Funput” trong Spotlight là
ra cửa sổ Cài đặt.

## Dùng hằng ngày

### Thanh menu

Icon **VI** trên thanh menu là bảng điều khiển nhanh: bật / tắt tiếng Việt, đổi kiểu gõ, mở Cài
đặt. Tắt được icon này trong **Cài đặt → Tổng quan → Hiện biểu tượng thanh menu**.

### Phím tắt

| Phím | Việc |
|---|---|
| **`⌃` `\`** | Bật / tắt tiếng Việt *(mặc định — ghi đè được trong **Cài đặt → Phím tắt**)* |
| *(chưa đặt)* | Lật lại từ vừa gõ, `card` ⇄ `cải` — tự chọn tổ hợp trong **Cài đặt → Phím tắt** |

Tổ hợp tự đặt bắt buộc phải kèm `⌃`, `⌥` hoặc `⌘`; phím trần sẽ bắn nhầm khi đang gõ nên bị từ chối.

### Thử nhanh

| Kiểu gõ | Gõ | Ra |
|---|---|---|
| Telex | `tieesng vieejt` | tiếng việt |
| VNI | `xin chao2` | xin chào |

### Cấu hình lưu ở đâu

macOS không dùng file cấu hình riêng — Funput ghi vào `UserDefaults`:

| Domain | Của |
|---|---|
| `app.funput.inputmethod.Funput` | Bộ gõ và mọi tuỳ chọn |
| `app.funput.Funput` | App stub trong Applications |

Xem hoặc chỉnh tay bằng `defaults`, ví dụ tắt tính năng bỏ dấu sau Backspace:

```bash
defaults write app.funput.inputmethod.Funput retoneAfterBackspace -bool false
```

Muốn mang cấu hình sang máy khác thì dùng **Cài đặt → Dữ liệu → Xuất cấu hình**.

## Giới hạn đã biết

> [!CAUTION]
> **Gõ vào app Chromium/Electron trên macOS 26/27 beta có thể đứng im.** Sau khi chuyển nguồn nhập
> trong lúc một ô nhập của Chrome, Cursor, VS Code, Slack, Discord, Notion… đang có focus, phím chữ
> ngừng ra chữ. **Không phải lỗi của Funput** — Apple đổi nội bộ `TextInputUIMacHelper` và cầu
> `NSTextInputContext` của Chromium chưa theo kịp; mọi bộ gõ bên thứ ba đều dính.
> Chi tiết và cách né: [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md).

- **Bỏ dấu sau Backspace không chạy trong Cursor.** Cursor báo vị trí con trỏ mâu thuẫn qua
  `IMKTextInput`, nên Funput từ chối thay vì đoán bừa. VS Code thì bình thường.
- **Không tự kiểm tra cập nhật nền.** `SUEnableAutomaticChecks` được đặt `false` — chỉ chạy khi
  bạn bấm **Kiểm tra cập nhật**.
- **Không có bản Mac App Store.** Input Method không thể chạy sandbox, nên chỉ phát hành trực tiếp.

## Cập nhật

**Cài đặt → Kiểm tra cập nhật** (Sparkle). Funput đọc `appcast.xml` đã ký từ GitHub Releases, xác
minh chữ ký **Ed25519** rồi cài bản mới. Cùng khoá ký với bản Windows.

Cập nhật thủ công thì tải bản mới rồi cài đè đúng cách bạn đã dùng lần trước (`.pkg` hoặc
`.app.zip`).

## Gỡ cài đặt

Làm đúng thứ tự này, vì macOS nhớ nguồn nhập độc lập với file trên đĩa:

1. **System Settings → Keyboard → Input Sources**, chọn **Funput**, nhấn **−**.
2. Chạy script gỡ — nó dọn cả bản system-wide lẫn bản per-user, app stub, receipt của `.pkg`,
   preferences và cache:

   ```bash
   ./scripts/uninstall.sh              # gỡ sạch
   ./scripts/uninstall.sh --keep-prefs # gỡ app, giữ lại cài đặt
   ```

3. **Đăng xuất rồi đăng nhập lại** (hoặc khởi động lại máy) để macOS quên hẳn nguồn nhập.

> [!WARNING]
> Chạy script bằng tài khoản thường, **đừng `sudo ./uninstall.sh`**. Script tự gọi `sudo` cho những
> chỗ cần quyền; chạy cả script dưới root sẽ xoá preferences của root thay vì của bạn.

Hướng dẫn đầy đủ hơn: [docs.funput.app — macOS](https://docs.funput.app/docs/install/macos).

---

## Dành cho developer

### Kiến trúc

Funput trên macOS là **bundle Input Method**, không phải app thường: macOS nạp nó, nó dựng
`IMKServer`, và từ đó chạy như một background agent (`LSUIElement`). Cùng process đó cũng host các
scene SwiftUI — thanh menu, Cài đặt, Onboarding, Chuyển mã — nên không có process con nào cả (khác
hẳn shell Windows).

Composition đi theo đường **preedit**: chữ đang gõ hiện ra qua `setMarkedText` và chỉ `insertText`
khi từ đã chốt. Engine là Rust, vào qua [`funput-ffi`](../../crates/funput-ffi) dưới dạng static
library `Vendor/libfunput_ffi.a`.

```text
Funput.app   trong /Library/Input Methods — MỘT process, hai vai trò
│
├── IMKServer ................ cầu nối với app đang gõ (XPC/Mach)
│        │
│        └──▶ FunputInputController ──▶ FunputComposer
│                     │                       │
│                     │                       └──▶ funput-ffi ──▶ funput-engine
│                     │
│                     ├── setMarkedText   preedit — chữ đang gõ, gạch chân
│                     └── insertText      commit — chốt từ đã xong
│
├── Thanh menu ............... SwiftUI · bật/tắt VI · đổi kiểu gõ
├── Cài đặt · Onboarding ..... SwiftUI Scene, cùng process
└── Sparkle .................. cập nhật — chỉ chạy khi bạn bấm

Funput.app   trong /Applications — bundle stub riêng, id app.funput.Funput
│
└── mở funput://settings ..... để Spotlight tìm được “Funput”
```

Hai bundle mang **bundle id khác nhau** (`app.funput.inputmethod.Funput` và `app.funput.Funput`)
để LaunchServices không bao giờ nhầm hai bản với nhau.

### Cây thư mục

```text
platforms/macos/
  Funput.xcodeproj · Info.plist · Bridging-Funput.h · ExportOptions.plist
  Funput/
    FunputApp.swift        IMKServer · xử lý URL funput:// · cài stub launcher
    IME/                   Controller · Composer · retone · chính sách phím
    Model/                 AppSettings (UserDefaults) · KeyCombo · nhớ theo app
    Settings/ MenuBar/ Onboarding/ Convert/     SwiftUI
    DesignSystem/          Theme + lớp Liquid Glass
  Launcher/                Stub Spotlight (một file main.swift)
  FunputTests/             XCTest
  Vendor/libfunput_ffi.a   Sinh ra bởi scripts/build-ffi.sh, không commit
  scripts/
  docs/                    LIQUID_GLASS.md · KNOWN_ISSUES.md
```

### Stack

| | |
|---|---|
| Swift | `5.0` language mode · SwiftUI · InputMethodKit |
| Deployment target | `26.0` — cả app, launcher lẫn test target |
| Kiến trúc | Debug: chỉ arch đang chạy. Release: universal, ghép bằng `lipo` |
| Engine | [`funput-ffi`](../../crates/funput-ffi) `--features convert`, link tĩnh |
| Cập nhật | Sparkle · appcast ký Ed25519 |
| Phát hành | Developer ID + notarize + `.pkg` *(Team `RSARFZ5CD3`)* |

Crate dùng chung: [`funput-engine`](../../crates/funput-engine) ·
[`funput-convert`](../../crates/funput-convert/README.md) ·
[`funput-core`](../../crates/funput-core).
Định dạng cấu hình: [`../CONFIG_FORMAT.md`](../CONFIG_FORMAT.md).

### Build

Cần **macOS** + Xcode, cộng Rust toolchain cho `funput-ffi`. Build phase “Run Script” của Xcode tự
gọi [`scripts/build-ffi.sh`](scripts/build-ffi.sh) trước khi compile Swift, nên không phải build
thư viện bằng tay.

```bash
# Build rồi cài thẳng làm input method của người dùng hiện tại
./scripts/install.sh
CONFIGURATION=Debug ./scripts/install.sh
```

```bash
# Test
xcodebuild -project Funput.xcodeproj -scheme Funput -destination 'platform=macOS' test
```

```bash
# Bản phát hành đầy đủ: universal + Developer ID + notarize + .pkg
./scripts/release.sh
DRY_RUN=1 ./scripts/release.sh   # ký ad-hoc, bỏ qua notarize — thử pipeline
```

> [!TIP]
> `install.sh` mặc định build **Release**, cố ý: bản Debug tách ra một `.debug.dylib` mà trình quét
> input method của macOS có thể từ chối. Script cũng tự `lsregister` + `TISRegisterInputSource` để
> nguồn nhập hiện ra ngay, không cần đăng xuất.

Giới hạn kích thước file Swift: **150 dòng/file**, tối đa **5 file/thư mục tính năng** —
[`scripts/check-swift-loc.sh`](scripts/check-swift-loc.sh), và CI có gác cổng này.

### CI

| Workflow | Việc |
|---|---|
| [`ci.yml`](../../.github/workflows/ci.yml) | Job `shell-line-budget` chạy `check-swift-loc.sh`. **Không** compile hay test Swift |
| [`build-macos.yml`](../../.github/workflows/build-macos.yml) | `workflow_call`: test macOS · build universal · Developer ID · notarize · `.pkg` + `.app.zip` · sinh và ký `appcast.xml` |
| [`release.yml`](../../.github/workflows/release.yml) | Gọi workflow trên và đẩy artifact lên GitHub Releases |

> [!WARNING]
> **Test macOS không chạy trên pull request.** Chúng chỉ nằm trong `build-macos.yml`, mà workflow
> này là `workflow_call` — chỉ `release.yml` gọi tới. Nghĩa là một thay đổi Swift làm hỏng test có
> thể vào `main` mà không ai biết, cho đến lúc phát hành. Hãy chạy `xcodebuild … test` tại máy
> trước khi mở PR.

## Giấy phép

MIT — [`LICENSE`](../../LICENSE).

<p align="center">
  <sub>
    <a href="https://funput.app">funput.app</a>
    ·
    <a href="../../README.md">README gốc</a>
    ·
    Made with ♥ by Funput
  </sub>
</p>
