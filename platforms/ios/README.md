<p align="center">
  <img
    src="../../assets/horizontal-lockup/gradient.png"
    width="420"
    alt="Funput"
  >
</p>

<p align="center">
  <strong>Funput cho iOS</strong> — bàn phím tiếng Việt cho iPhone và iPad.<br>
  Tiện ích bàn phím UIKit · app cấu hình SwiftUI · engine Rust qua
  <code>funput-ffi</code>
</p>

<p align="center">
  <a href="https://apps.apple.com/vn/app/id6788829996">
    <img src="https://img.shields.io/badge/Tải_về-App_Store-0D96F6?style=for-the-badge&logo=appstore&logoColor=white" alt="Funput trên App Store">
  </a>
  <a href="https://docs.funput.app/docs/install/ios">
    <img src="https://img.shields.io/badge/Tài_liệu-Hướng_dẫn_cài_đặt-2563EB?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Hướng dẫn cài đặt">
  </a>
  <a href="https://github.com/Funput/Funput/issues">
    <img src="https://img.shields.io/badge/Hỗ_trợ-Báo_lỗi-E11D48?style=for-the-badge&logo=github&logoColor=white" alt="Báo lỗi">
  </a>
</p>

---

<p align="center">
  <img
    src="../../assets/screenshot/ios.png"
    height="420"
    alt="Bàn phím Funput trên iOS"
  >
  <br>
  <sub>Bàn phím Funput trên iPhone.</sub>
</p>

## Tính năng

| | |
|---|---|
| ⌨️ **Ba kiểu gõ** | Telex · **Telex nâng cao** (thêm `w` đầu từ và phím tắt `[` `]`) · VNI |
| 🧠 **Nhập liệu thông minh** | Khôi phục từ thông minh · khôi phục sớm · kiểm tra chính tả · tự viết hoa |
| 💬 **Gợi ý từ cá nhân** | Chỉ học chữ bạn gõ qua Funput, **lưu trên thiết bị** |
| 👆 **Cử chỉ thông minh** | Gõ đúp phím cách để chấm câu · giữ phím cách rồi kéo để di chuyển con trỏ · vuốt trái phím xoá để xoá cả từ |
| 🎨 **Theme** | 5 theme dựng sẵn, gồm cả Liquid Glass trên iOS 26 |
| 🔢 **Bố cục** | Bật hàng phím số · chọn kiểu Funput hoặc “Giống hệ thống” |
| 📋 **Lịch sử clipboard** | Chỉ lưu thứ bạn đã dán qua Funput, trên thiết bị |
| 📳 **Phản hồi khi gõ** | Rung · âm thanh click · xem trước phím |
| 😀 **Emoji và kaomoji** | Hai bảng riêng, tối ưu cho từng loại |

## Yêu cầu

| | |
|---|---|
| **Hệ điều hành tối thiểu** | **iOS / iPadOS 18.6** trở lên |
| **Thiết bị** | iPhone và iPad |
| **Cài đặt** | Miễn phí trên [App Store](https://apps.apple.com/vn/app/id6788829996) |
| **Quyền** | Cần bật **Cho phép truy cập đầy đủ** — [xem vì sao](#vì-sao-cần-cho-phép-truy-cập-đầy-đủ) |

> [!IMPORTANT]
> **iOS 18.6 là sàn cứng.** Cả app lẫn tiện ích bàn phím đều khai `MinimumOSVersion` là `18.6`
> *(kiểm chứng được trong chính bản đã đóng gói)*, nên App Store sẽ không cho cài trên máy chạy
> iOS thấp hơn.
>
> Kiểm tra máy bạn: **Cài đặt → Cài đặt chung → Giới thiệu → Phiên bản phần mềm**.
>
> Giao diện **Liquid Glass cần iOS 26**. Trên iOS 18–25 bàn phím vẫn chạy đầy đủ, chỉ là các
> theme kính đổi sang nền đặc tương đương.

## Cài đặt

Cài xong app **chưa gõ được ngay** — iOS bắt buộc bạn thêm bàn phím thủ công. Ba bước:

1. **Cài Funput** từ [App Store](https://apps.apple.com/vn/app/id6788829996), rồi mở app một lần.
2. **Thêm bàn phím:** Cài đặt → Cài đặt chung → Bàn phím → Các bàn phím → **Thêm bàn phím mới…** → **Funput**.
3. **Bật quyền:** trong danh sách Các bàn phím, chọn **Funput** → bật **Cho phép truy cập đầy đủ**.

Sau đó, ở bất kỳ ô nhập nào, chạm giữ phím 🌐 và chọn **Funput**.

Thẻ **Hoàn tất thiết lập Funput** trong app tự dò xem bạn đã làm tới bước nào và mở thẳng đúng
trang Cài đặt tương ứng.

### Vì sao cần “Cho phép truy cập đầy đủ”

iOS chạy tiện ích bàn phím trong sandbox riêng. Nếu không có quyền này, tiện ích **không đọc được
App Group** — nơi app lưu mọi thiết lập của bạn. Hệ quả rất cụ thể: bàn phím sẽ không biết bạn
chọn Telex hay VNI, không lấy được theme, không thấy gợi ý từ cá nhân, và không rung hay phát
tiếng khi gõ.

> [!NOTE]
> **Funput không gửi thứ bạn gõ đi đâu cả.** Tiện ích bàn phím **không chứa một dòng code mạng
> nào** — không `URLSession`, không kết nối ra ngoài. Privacy manifest khai
> `NSPrivacyTracking = false` và **không thu thập dữ liệu người dùng**
> *(`NSPrivacyCollectedDataTypes` rỗng)*. Gợi ý từ cá nhân và lịch sử clipboard nằm trên máy bạn.
>
> Kiểm chứng được: [`Keyboard/PrivacyInfo.xcprivacy`](Keyboard/PrivacyInfo.xcprivacy) và
> toàn bộ [`Keyboard/`](Keyboard).

## Dùng hằng ngày

### Chuyển bàn phím

Chạm giữ phím 🌐 rồi chọn **Funput**. Muốn bỏ bớt bàn phím thừa trong vòng lặp thì vào
Cài đặt → Cài đặt chung → Bàn phím → Các bàn phím → **Sửa**.

### Thử nhanh

| Kiểu gõ | Gõ | Ra |
|---|---|---|
| Telex | `tieesng vieejt` | tiếng việt |
| VNI | `xin chao2` | xin chào |

### Cử chỉ

| Cử chỉ | Việc |
|---|---|
| **Gõ đúp phím cách** | Chèn dấu chấm và một khoảng trắng |
| **Giữ phím cách rồi kéo** | Biến bàn phím thành trackpad để rê con trỏ |
| **Vuốt trái trên phím xoá** | Xoá cả từ thay vì từng ký tự |

Cả ba nằm chung một công tắc **Cử chỉ thông minh** trong Cài đặt.

### Cấu hình lưu ở đâu

Trong App Group `group.app.funput.funput`, dùng chung giữa app và tiện ích bàn phím. Không có file
cấu hình để chép tay; gỡ app là mất sạch.

## Giới hạn đã biết

- **Không có “Cho phép truy cập đầy đủ” thì gần như không dùng được.** Đây là ràng buộc của iOS
  với mọi bàn phím bên thứ ba, không riêng Funput.
- **Liquid Glass chỉ có trên iOS 26.** Máy cũ hơn nhận nền đặc tương đương.
- **Chưa có công cụ Chuyển mã.** Đổi bảng mã hiện chỉ có trên bản desktop.

## Cập nhật

Qua App Store như mọi app khác. Bật **Cập nhật ứng dụng tự động** trong
Cài đặt → App Store nếu muốn không phải nghĩ tới nó nữa.

## Gỡ cài đặt

Xoá app Funput là iOS tự gỡ luôn tiện ích bàn phím khỏi danh sách Các bàn phím, cùng toàn bộ dữ
liệu trong App Group — gồm cả gợi ý từ cá nhân và lịch sử clipboard.

Muốn giữ app nhưng tạm ngưng bàn phím thì vào Cài đặt → Cài đặt chung → Bàn phím → Các bàn phím →
**Sửa** → xoá Funput khỏi danh sách.

---

## Dành cho developer

### Kiến trúc

Hai bundle rời nhau, nối bằng một App Group. App cấu hình viết bằng **SwiftUI**; bàn phím viết
bằng **UIKit** vì nó cần kiểm soát chạm và vòng đời ở mức thấp hơn nhiều so với những gì SwiftUI
cho phép. Logic dùng chung nằm trong **FunputKit**, một SPM package cục bộ.

Khác macOS *(preedit qua `setMarkedText`)*, iOS **commit thẳng** vào document —
đây là lựa chọn thiết kế, không phải giới hạn của nền tảng.

```text
Funput.app      containing app — app.funput.funput
│
├── Onboarding .............. hướng dẫn 3 bước bật bàn phím
├── Cài đặt ................. SwiftUI · theme · gợi ý · cử chỉ
└── App Group ............... group.app.funput.funput
             │
             │   UserDefaults(suiteName:) — đây là lý do cần
             │   “Cho phép truy cập đầy đủ”
             ▼
Keyboard.appex  tiện ích bàn phím — app.funput.funput.Keyboard
│
├── UIInputViewController ... vòng đời · chiều cao · traits
├── KeyboardTouch* .......... chạm · rollover · key repeat
├── KeyboardRenderer ........ vẽ phím bằng Core Animation
└── KeyboardInput ........... phím → engine
             │
             └──▶ FunputCore.xcframework ──▶ funput-engine
                  (funput-ffi, C ABI, link tĩnh)
```

Tài liệu kiến trúc chi tiết: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md),
[`docs/INPUT_PIPELINE_2_ARCHITECTURE.md`](docs/INPUT_PIPELINE_2_ARCHITECTURE.md),
[`docs/INPUT_PIPELINE_3_ARCHITECTURE.md`](docs/INPUT_PIPELINE_3_ARCHITECTURE.md).

### Cây thư mục

```text
platforms/ios/
  Funput.xcodeproj
  Funput/                  Containing app (SwiftUI)
    App/ Settings/ About/ Appearance/ Components/
  Keyboard/                Tiện ích bàn phím (UIKit)
    Controller/              UIInputViewController + gestures, height, traits, input
    Document/ Suggestions/ Clipboard/ Presentation/ Diagnostics/
    Info.plist · Keyboard.entitlements · PrivacyInfo.xcprivacy
  Packages/FunputKit/      SPM cục bộ, 11 module dùng chung
  Frameworks/              FunputCore.xcframework — sinh ra, không commit
  FunputTests/ FunputUITests/
  Scripts/  docs/
```

### Stack

| | |
|---|---|
| Swift | `6.3`, Swift language mode 6 · SwiftUI *(app)* + UIKit *(bàn phím)* |
| Deployment target | **`18.6`** cho cả app, bàn phím và test target |
| Engine | [`funput-ffi`](../../crates/funput-ffi) đóng thành `FunputCore.xcframework`, link tĩnh |
| Cấu hình chia sẻ | App Group `group.app.funput.funput` + `UserDefaults(suiteName:)` |
| Phát hành | App Store Connect → TestFlight → App Store |

> [!WARNING]
> **Cài đặt cấp project là `IPHONEOS_DEPLOYMENT_TARGET = 26.5`, không phải 18.6.** Mọi target hiện
> có đều ghi đè xuống 18.6 nên giá trị đó vô hại — nhưng một target **mới** thêm vào mà quên đặt
> giá trị riêng sẽ âm thầm thừa kế 26.5, tức là loại bỏ gần hết thiết bị người dùng. Đặt
> `IPHONEOS_DEPLOYMENT_TARGET` một cách tường minh cho mọi target mới.

### Build

Cần **macOS** + Xcode, cộng Rust toolchain để dựng `FunputCore.xcframework`.

```bash
# Lần đầu: dựng xcframework từ funput-ffi (device + simulator)
./Scripts/bootstrap-ios.sh
```

```bash
# Test logic dùng chung, không cần Xcode project
./Scripts/test-funput-kit.sh
```

```bash
# Build + test đầy đủ
xcodebuild -project Funput.xcodeproj -scheme Funput \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' test
```

Giới hạn kích thước file Swift: **150 dòng/file**, tối đa **5 file/thư mục tính năng** —
[`Scripts/check-swift-loc.sh`](Scripts/check-swift-loc.sh), và CI có gác cổng này.

### CI

| Workflow | Việc |
|---|---|
| [`ci.yml`](../../.github/workflows/ci.yml) | Job `shell-line-budget` chạy `check-swift-loc.sh`. **Không** compile hay test Swift |
| [`deploy-ios.yml`](../../.github/workflows/deploy-ios.yml) | `workflow_dispatch` thủ công: test FunputKit · build `.ipa` · ký Apple Distribution · tải lên App Store Connect → TestFlight |

> [!WARNING]
> **Test iOS không chạy trên pull request.** Chúng chỉ nằm trong `deploy-ios.yml`, mà workflow đó
> chạy thủ công và **cố ý không** nối vào `release.yml` — mỗi lần tải lên là đốt một build number.
> Hãy chạy `./Scripts/test-funput-kit.sh` tại máy trước khi mở PR.

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
