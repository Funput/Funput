<p align="center">
  <img
    src="../../assets/horizontal-lockup/gradient.png"
    width="420"
    alt="Funput"
  >
</p>

<p align="center">
  <strong>Funput cho Windows</strong> — bộ gõ tiếng Việt portable, chạy nền ở khay hệ thống.<br>
  Một file <code>.exe</code> · không cài đặt · UI native <a href="https://slint.dev">Slint</a> · link trực tiếp
  <code>funput-engine</code>
</p>

<p align="center">
  <a href="https://github.com/Funput/Funput/releases/latest">
    <img src="https://img.shields.io/badge/Tải_xuống-Bản_mới_nhất-22C55E?style=for-the-badge&logo=github&logoColor=white" alt="Tải Funput Windows">
  </a>
  <a href="https://docs.funput.app/docs/install/windows">
    <img src="https://img.shields.io/badge/Tài_liệu-Hướng_dẫn_cài_đặt-2563EB?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Hướng dẫn cài đặt">
  </a>
  <a href="https://github.com/Funput/Funput/issues">
    <img src="https://img.shields.io/badge/Hỗ_trợ-Báo_lỗi-E11D48?style=for-the-badge&logo=github&logoColor=white" alt="Báo lỗi">
  </a>
</p>

---

<p align="center">
  <img
    src="../../assets/screenshot/windows.png"
    width="640"
    alt="Giao diện Cài đặt Funput trên Windows"
  >
  <br>
  <sub>Cửa sổ Cài đặt — Tổng quan, Telex / Telex+ / VNI, khởi động cùng Windows.</sub>
</p>

## Tính năng

| | |
|---|---|
| ⌨️ **Ba kiểu gõ** | Telex · **Telex+** (thêm `w` đầu từ và phím tắt `[` `]`) · VNI |
| 🔤 **Kiểu đặt dấu** | Truyền thống (`hòa`, `khỏe`) hoặc hiện đại (`hoà`, `khoẻ`) |
| 🧠 **Gõ thông minh** | Tự khôi phục tiếng Anh · khôi phục tức thì · kiểm tra chính tả · tự động viết hoa |
| 🗂️ **Nhớ theo ứng dụng** | Bật tiếng Việt ở Word, tắt ở Terminal — Funput tự chuyển khi bạn đổi cửa sổ |
| 🌏 **Tự tắt khi đổi bàn phím** | Chuyển sang EN khi bạn dùng bàn phím tiếng Nhật, Hàn, Trung… |
| ✂️ **Gõ tắt** | Bảng viết tắt tự bung, khớp cả hoa lẫn thường; nhập được bảng gõ tắt UniKey |
| 🔄 **Chuyển mã** | Đổi qua lại giữa Unicode dựng sẵn, Unicode tổ hợp, TCVN3 (ABC) và VNI-Windows — dán văn bản hoặc chuyển cả tệp |
| 💾 **Xuất / nhập cấu hình** | Mang toàn bộ tuỳ chọn và gõ tắt sang máy khác bằng một tệp |
| 🚀 **Khởi động cùng Windows** | Tuỳ chọn, ghi vào registry `HKCU\…\Run` |

Không cần thêm Input Source như macOS hay Linux — Funput hoạt động ngay qua keyboard hook toàn cục.

## Yêu cầu

| | |
|---|---|
| **Hệ điều hành** | **Windows 10 phiên bản 1809 (build 17763)** trở lên · Windows 11 |
| **Kiến trúc** | **x86-64**. Windows on ARM chạy được qua emulation, nhưng không có bản build riêng |
| **Cài đặt** | Một file `.exe` portable — chưa có MSI/installer |
| **Runtime ngoài** | Không cần. Không WebView2, không .NET |

> [!NOTE]
> **Vì sao là build 17763?** Đó là mốc `window-vibrancy` cần để dựng nền Acrylic cho Control Center.
> Bản Windows cũ hơn không bị chặn chạy, nhưng không được thử nghiệm và flyout sẽ mất hiệu ứng nền.
>
> **Mica là Win11.** Trên Windows 10, cửa sổ Cài đặt dùng nền đục — cố ý như vậy, không phải lỗi:
> đường Acrylic thay thế khiến cửa sổ kéo/resize bị giật (xem [`src/ui/mica.rs`](src/ui/mica.rs)).

## Cài đặt

1. Tải `Funput-<version>.exe` từ [GitHub Releases](https://github.com/Funput/Funput/releases/latest).
2. Đặt file ở vị trí cố định, ví dụ `%LOCALAPPDATA%\Programs\Funput\` — **đừng để trong Downloads**.
3. Double-click. Icon **f** của Funput xuất hiện ở khay hệ thống.
4. Lần đầu chạy sẽ mở **Onboarding**; xong là gõ được ngay trên mọi ứng dụng.

Kiểm tra toàn vẹn file trước khi chạy — so kết quả với asset `.sha256` cùng bản phát hành:

```powershell
Get-FileHash .\Funput-*.exe -Algorithm SHA256
```

> [!IMPORTANT]
> **File bạn vừa tải sẽ tự đổi tên.** Lần chạy đầu tiên, `Funput-<version>.exe` tự nhân bản thành
> `Funput.exe` cạnh nó, khởi động lại từ file mới rồi **xoá file có số phiên bản**. Đây là hành vi
> cố ý: autostart và cập nhật cần một đường dẫn cố định. Từ đó trở đi bạn chỉ chạy `Funput.exe`.

> [!WARNING]
> **SmartScreen** có thể cảnh báo lần đầu (exe chưa ký Authenticode). Chỉ tải từ
> [GitHub Releases](https://github.com/Funput/Funput/releases) chính thức, rồi chọn
> **More info** → **Run anyway**.

## Dùng hằng ngày

### Khay hệ thống

| Thao tác | Chức năng |
|---|---|
| **Click trái** | Mở / đóng **Control Center** — bật tắt tiếng Việt, đổi kiểu gõ, bật nhanh chính tả / khôi phục tiếng Anh / viết hoa |
| **Click phải** | Cài đặt… · Chuyển mã… · Kiểm tra cập nhật… · Thoát |

Icon tray đổi **màu ↔ mono** theo trạng thái tiếng Việt, nên liếc một cái là biết đang gõ kiểu gì.

### Phím tắt

| Phím | Việc |
|---|---|
| **`Ctrl` + `` ` ``** | Bật / tắt tiếng Việt *(mặc định — đổi được sang `Ctrl Space` hoặc `Alt Shift`)* |
| **`Ctrl` `Shift` `Z`** | Lật lại từ vừa gõ *(mặc định tắt; bật trong **Cài đặt → Phím tắt**)* |

### Thử nhanh

| Kiểu gõ | Gõ | Ra |
|---|---|---|
| Telex | `tieesng vieejt` | tiếng việt |
| VNI | `xin chao2` | xin chào |

### Cấu hình lưu ở đâu

Funput ưu tiên **portable**: cấu hình nằm cạnh chính file exe để cả app đi theo USB được.

| Thứ tự | Vị trí | Khi nào dùng |
|---|---|---|
| 1 | `%FUNPUT_CONFIG%` | Khi biến môi trường này được đặt |
| 2 | `<thư mục chứa Funput.exe>\settings.json` | **Mặc định** — khi thư mục đó ghi được |
| 3 | `%APPDATA%\Funput\settings.json` | Dự phòng, khi thư mục exe chỉ đọc (ví dụ đặt trong `C:\Program Files\`) |

## Giới hạn đã biết

> [!CAUTION]
> **Không gõ được vào ứng dụng chạy quyền Administrator** (Task Manager, regedit, một số trình cài đặt).
> Funput chạy ở quyền thường (`asInvoker`) nên Windows chặn nó đưa phím vào cửa sổ elevated.
> Cách duy nhất: chạy Funput bằng **Run as administrator**.

- **Không tự kiểm tra cập nhật nền.** Chỉ chạy khi bạn bấm **Kiểm tra cập nhật…**.
- **Chưa có installer.** Bản phát hành là một file `.exe` portable, chưa có MSI.
- **Chưa ký Authenticode**, nên SmartScreen sẽ cảnh báo ở lần chạy đầu.

## Cập nhật

Tray → **Kiểm tra cập nhật…**. Funput đọc manifest `funput-windows.json` từ GitHub Releases, tải
`.exe` mới, **xác minh chữ ký Ed25519** (cùng public key với Sparkle trên macOS), thay file đang
chạy tại chỗ rồi tự khởi động lại — không cần đăng xuất.

## Gỡ cài đặt

Portable nên không có trình gỡ; dọn ba thứ:

```powershell
# 1. Thoát Funput từ menu tray, rồi xoá thư mục chứa app
Remove-Item -Recurse "$env:LOCALAPPDATA\Programs\Funput"

# 2. Xoá mục khởi động cùng Windows (nếu đã bật)
Remove-ItemProperty "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run" -Name Funput -EA SilentlyContinue

# 3. Xoá cấu hình dự phòng, nếu có
Remove-Item -Recurse "$env:APPDATA\Funput" -EA SilentlyContinue
```

Hướng dẫn đầy đủ hơn: [docs.funput.app — Windows](https://docs.funput.app/docs/install/windows).

---

## Dành cho developer

### Kiến trúc

Package độc lập `funput-windows`, **nằm ngoài Cargo workspace** của repo: nó kéo Slint và crate
`windows`, vốn chỉ build trên target Windows. Hệ quả cần nhớ — package này **không** thừa kế
`[profile.release]` lẫn `[lints]` của workspace, nên giữ bản sao tương đương trong `Cargo.toml`
của chính nó.

Khác mọi nền tảng khác (macOS/Linux qua [`funput-ffi`](../../crates/funput-ffi), Android qua
[`funput-jni`](../../crates/funput-jni)), shell này **link thẳng** Rust vào
[`funput-engine`](../../crates/funput-engine) / [`funput-desktop`](../../crates/funput-desktop).

Một file `.exe`, hai chương trình:

```text
Process nền   luôn chạy · giữ hook + tray · không nhúng runtime UI
│
├── WH_KEYBOARD_LL ............ phím người dùng gõ
├── WH_MOUSE_LL ............... click chuột dời caret
├── WinEvent FOREGROUND ....... đổi app → nhớ VI/EN theo từng app
│        │
│        └──▶ funput-desktop ──▶ funput-engine ──▶ SendInput
│                                  (Backspace + ký tự Unicode)
│
└── tray-icon ................. icon trạng thái + menu ngữ cảnh

Process UI    sinh theo yêu cầu · sống ngắn · chọn bằng cờ dòng lệnh
│
└── Slint (style Fluent · renderer Skia) · window-vibrancy
```

Cả **ba** hook chạy trên main thread của process nền — `WH_KEYBOARD_LL` bắt buộc phải có message
loop, và tray dùng chung vòng lặp đó. Mọi sự kiện synthesize ra bằng `SendInput` đều mang
`INJECT_TAG` trong `dwExtraInfo` để chính hook bỏ qua, tránh đệ quy.

Cửa sổ Slint là **process con**, chọn bằng cờ dòng lệnh; đóng cửa sổ là Windows thu hồi trọn vẹn
Skia, font cache và cấp phát của driver đồ hoạ:

| Cờ | Cửa sổ |
|---|---|
| `--settings` | Cài đặt |
| `--settings-check-update` | Cài đặt, mở thẳng **Giới thiệu** và kiểm tra cập nhật |
| `--onboarding` | Onboarding lần chạy đầu |
| `--control-center` | Flyout Control Center |
| `--convert` | Công cụ Chuyển mã |

Không cờ nào ⇒ chạy process nền, sau khi đã chuẩn hoá tên exe và **giành singleton**. Lần chạy
thứ hai không tự dựng tray: nó báo bản gốc mở Cài đặt rồi thoát.

### Cây thư mục

```text
platforms/windows/
  Cargo.toml · build.rs · ui/ · icons/
  src/
    main.rs        Định tuyến cờ · chuẩn hoá exe · singleton
    background/    hook/ (keyboard · mouse · foreground) · hotkey/ · inject/ · keymap · tray/
    shared/        shell/ (state toàn cục) · commands/ · update/ · settings_path · canonical_exe
    ui/            Cửa sổ Slint · control_center/ · mica.rs
  scripts/
    build-release.ps1 · build-release.sh
```

### Stack

| | |
|---|---|
| Rust | `1.98.1` ([`rust-toolchain.toml`](../../rust-toolchain.toml)) — Slint 1.17 cần ≥ 1.92 |
| Slint | `1.17` · winit · renderer **Skia** · style Fluent *(chọn trong [`build.rs`](build.rs))* |
| Win32 | `windows` 0.62 · `window-vibrancy` 0.8 — Mica (Win11) / Acrylic (Win10 1809+) |
| Tray | `tray-icon` 0.24 *(không WebView2)* |
| Cập nhật | `ureq` · `ed25519-dalek` · `self-replace` |

Crate dùng chung: [`funput-engine`](../../crates/funput-engine) ·
[`funput-desktop`](../../crates/funput-desktop/README.md) ·
[`funput-config`](../../crates/funput-config) ·
[`funput-convert`](../../crates/funput-convert/README.md) ·
[`funput-core`](../../crates/funput-core).
Định dạng cấu hình: [`../CONFIG_FORMAT.md`](../CONFIG_FORMAT.md).

### Build

Cần **Windows** + MSVC (workload “Desktop development with C++”). Skia chỉ có binary dựng sẵn cho
`*-windows-msvc` — cross-compile `windows-gnu` từ macOS/Linux sẽ thất bại.

```powershell
# Vòng lặp phát triển — profile dev, giống hệt job `windows` của CI
cargo clippy --all-targets -- -D warnings
cargo test
```

```powershell
# Bản phát hành, từ thư mục này
.\scripts\build-release.ps1
```

> [!TIP]
> Đừng lint/test bằng `--release`. Profile release ở đây bật `lto = "fat"` +
> `codegen-units = 1` trên cây phụ thuộc Slint + Skia: CI đo được **220 s clippy và 248 s test**
> khi cache nguội. CI cố tình chạy profile dev, và bạn cũng nên vậy.

Artifact trong `build/release/`:

| File | |
|---|---|
| `Funput-<version>.exe` | Asset để phát hành |
| `Funput.exe` | Tên ổn định, dùng cho autostart và chạy thử tại chỗ |
| `Funput-<version>.exe.sha256` | Checksum |

Test cần trỏ cấu hình đi chỗ khác thì đặt `FUNPUT_CONFIG` — nó thắng mọi lựa chọn vị trí khác.

### CI

| Workflow | Việc |
|---|---|
| [`ci.yml`](../../.github/workflows/ci.yml) | Job `windows`: clippy + test *(profile dev)*. Job `cargo-deny` có bước riêng cho shell này |
| [`build-windows.yml`](../../.github/workflows/build-windows.yml) | Build `Funput.exe` và `funput` CLI; job `windows-feed` chạy trên **macOS runner** để ký Ed25519 và phát `funput-windows.json` |
| [`release.yml`](../../.github/workflows/release.yml) | Gom artifact lên GitHub Releases |
| [`audit.yml`](../../.github/workflows/audit.yml) | Rà advisory của dependency |

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
