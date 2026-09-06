<p align="center">
  <img
    src="../../assets/horizontal-lockup/gradient.png"
    width="420"
    alt="Funput"
  >
</p>

<p align="center">
  <strong>Funput cho Android</strong> — bàn phím tiếng Việt cho điện thoại và máy tính bảng.<br>
  Kotlin + Jetpack Compose · vẽ phím bằng Canvas · engine Rust qua
  <code>funput-jni</code>
</p>

<p align="center">
  <a href="mailto:hello@funput.app">
    <img src="https://img.shields.io/badge/Kiểm_thử_khép_kín-Gửi_email_tham_gia-22C55E?style=for-the-badge&logo=gmail&logoColor=white" alt="Tham gia kiểm thử">
  </a>
  <a href="https://docs.funput.app/docs/install/android">
    <img src="https://img.shields.io/badge/Tài_liệu-Hướng_dẫn_cài_đặt-2563EB?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Hướng dẫn cài đặt">
  </a>
  <a href="https://github.com/Funput/Funput/issues">
    <img src="https://img.shields.io/badge/Hỗ_trợ-Báo_lỗi-E11D48?style=for-the-badge&logo=github&logoColor=white" alt="Báo lỗi">
  </a>
</p>

---

<p align="center">
  <img
    src="../../assets/screenshot/android.png"
    height="420"
    alt="Bàn phím Funput trên Android"
  >
  <br>
  <sub>Bàn phím Funput trên Android.</sub>
</p>

## Tính năng

| | |
|---|---|
| ⌨️ **Ba kiểu gõ** | Telex · **Telex nâng cao** (thêm `w` đầu từ và phím tắt `[` `]`) · VNI |
| 🧠 **Gõ thông minh** | Tự khôi phục tiếng Anh · khôi phục tức thì · kiểm tra chính tả · tự động viết hoa |
| 💬 **Gợi ý từ cá nhân** | Chỉ học chữ bạn gõ qua Funput, **lưu trên máy** |
| 👆 **Cử chỉ thông minh** | Gõ đúp phím cách để chấm câu · giữ phím cách rồi kéo để di chuyển con trỏ mọi hướng · vuốt trái phím xoá để xoá cả từ |
| 🎨 **7 theme dựng sẵn** | Slate · Ink · Paper · Glass Dark · Glass Light · Blossom · Orchid |
| 🖌️ **Theme tự tạo** | Tự chỉnh màu và ảnh nền, lưu thành theme riêng |
| 🔢 **Tuỳ chỉnh bố cục** | Bật hàng phím số · chỉnh kích thước phím |
| 😀 **Bảng emoji** | Emoji picker của AndroidX, nạp trễ để bàn phím mở nhanh |
| 📳 **Âm thanh và rung** | Chỉnh riêng cho từng loại phản hồi |

## Yêu cầu

| | |
|---|---|
| **Hệ điều hành tối thiểu** | **Android 8.0 (Oreo, API 26)** trở lên |
| **Thiết bị** | Điện thoại và máy tính bảng · `arm64-v8a` và `x86_64` |
| **Cài đặt** | Đang ở vòng **kiểm thử khép kín**, chưa lên Play Store công khai |

> [!IMPORTANT]
> **Android 8.0 là sàn cứng.** `minSdk = 26` được đặt ở **toàn bộ sáu module** của dự án, nên
> Google Play sẽ không cho cài trên máy chạy Android thấp hơn.
>
> Kiểm tra máy bạn: **Cài đặt → Giới thiệu điện thoại → Phiên bản Android**.

## Cài đặt

> [!NOTE]
> **Funput chưa có trên Play Store công khai.** Google Play bắt buộc một vòng kiểm thử khép kín
> đủ **14 ngày** với số tester tối thiểu trước khi cho xuất bản, và Funput đang chạy lại vòng đó.
>
> Muốn dùng thử và giúp Funput lên Play Store, gửi email tới
> [hello@funput.app](mailto:hello@funput.app) — nên dùng Gmail gắn với tài khoản Google Play của
> bạn. Chúng tôi sẽ mời bạn vào nhóm kiểm thử khép kín.

Cài xong app **chưa gõ được ngay** — Android bắt buộc bật bàn phím thủ công. App tự dò xem bạn
đang ở bước nào và hiện thẻ **Thiết lập Funput** với đúng nút cần bấm:

1. **Bật Funput** trong cài đặt bàn phím của Android.
2. **Chọn Funput** khi đang gõ — chạm biểu tượng bàn phím ở thanh điều hướng, hoặc giữ phím
   🌐 / phím cách tuỳ máy.

Xong bước hai, thẻ đổi thành **Funput đã sẵn sàng**.

## Dùng hằng ngày

### Thử nhanh

| Kiểu gõ | Gõ | Ra |
|---|---|---|
| Telex | `tieesng vieejt` | tiếng việt |
| VNI | `xin chao2` | xin chào |

Mặc định lần đầu chạy là **VNI**. Đổi trong **Cài đặt → Bàn phím → Kiểu gõ**.

> [!TIP]
> **VNI luôn cần hàng phím số** để nhập dấu, nên Funput tự bật hàng số khi bạn chọn VNI. Với các
> kiểu Telex thì hàng số là tuỳ chọn — bật hay tắt trong **Cài đặt → Bàn phím → Hàng phím số**.

### Cử chỉ

| Cử chỉ | Việc |
|---|---|
| **Gõ đúp phím cách** | Chèn dấu chấm và một khoảng trắng |
| **Giữ phím cách rồi kéo** | Rê con trỏ **theo mọi hướng**, không chỉ trái phải |
| **Vuốt trái trên phím xoá** | Xoá cả từ thay vì từng ký tự |

Cả ba nằm chung một công tắc **Cử chỉ thông minh**.

### Cài đặt có gì

| Mục | Chỉnh được gì |
|---|---|
| **Bàn phím** | Kiểu gõ · hàng phím số · kích thước phím |
| **Gõ thông minh** | Khôi phục tiếng Anh · khôi phục sớm · kiểm tra chính tả · tự viết hoa · gợi ý từ cá nhân |
| **Giao diện** | Chọn theme dựng sẵn hoặc tự tạo theme riêng |
| **Âm thanh & rung** | Phản hồi khi gõ |
| **Cử chỉ** | Công tắc cử chỉ thông minh |

Cấu hình lưu bằng **Preferences DataStore**; gỡ app là mất sạch, gồm cả theme tự tạo và gợi ý
từ cá nhân.

## Giới hạn đã biết

- **Chưa lên Play Store công khai** — xem mục [Cài đặt](#cài-đặt).
- **Chưa có công cụ Chuyển mã.** Đổi bảng mã hiện chỉ có trên bản desktop.
- **Chỉ `arm64-v8a` và `x86_64`.** Máy 32-bit không nằm trong phạm vi hỗ trợ.

## Gỡ cài đặt

Gỡ app Funput như mọi app khác. Android tự bỏ bàn phím khỏi danh sách, cùng toàn bộ dữ liệu
trong DataStore.

Muốn giữ app nhưng tạm ngưng bàn phím thì tắt Funput trong cài đặt bàn phím của Android.

---

## Dành cho developer

### Kiến trúc

Shell và giao diện bàn phím viết bằng **Kotlin**; toàn bộ luật gõ tiếng Việt vẫn tới từ engine
Rust dùng chung, qua **JNI** ([`funput-jni`](../../crates/funput-jni)).

IME sở hữu composing span của Android, còn
[`funput-engine`](../../crates/funput-engine) vẫn là **nguồn chân lý duy nhất** cho luật Telex,
Telex nâng cao và VNI.

| Module | Trách nhiệm |
|---|---|
| `app` | Host Compose cho onboarding, cài đặt, trình tạo theme và bản xem trước bàn phím |
| `ime` | Vòng đời input-method của Android và cầu nối tới ô nhập đang focus |
| `keyboard-ui` | Điều hướng giữa các panel và emoji picker AndroidX nạp trễ |
| `keyboard-renderer` | Bố cục co giãn, chạm, trợ năng và vẽ bằng Canvas |
| `theme-runtime` | Hợp đồng theme có version, kiểm tra hợp lệ, phân giải token, truy cập asset an toàn |
| `theme-store` | Lưu theme tự tạo: JSON, bản nháp và kho asset |

Chiều phụ thuộc là **một chiều, cố ý**:

```text
app ──▶ ime ──▶ keyboard-ui ──▶ keyboard-renderer ──▶ theme-runtime
 │       │           │                                     ▲
 │       └──▶ theme-store ────────────────────────────────┤
 │                                                         │
 ├──▶ keyboard-renderer ──────────────────────────────────┤
 └──▶ theme-runtime ──────────────────────────────────────┘
```

`theme-runtime` là lá — nó không phụ thuộc module nào khác trong dự án.

### Build

Mở thư mục này bằng Android Studio bản stable mới nhất, hoặc chạy:

```bash
./gradlew :app:assembleDebug
```

Gradle sẽ cross-compile `funput-jni` cho `arm64-v8a` và `x86_64`. Cài thư viện chuẩn Rust tương
ứng một lần:

```bash
rustup target add aarch64-linux-android x86_64-linux-android
```

| | |
|---|---|
| Kotlin | Jetpack Compose *(app)* + Canvas *(bàn phím)* |
| `compileSdk` · `targetSdk` | **37** |
| `minSdk` | **26** — đặt ở cả sáu module |
| NDK | `29.0.14206865` — ghim để build native tái lập được |
| Application ID | `app.funput.funput` |

### Phát hành

`Deploy Android` ([`deploy-android.yml`](../../.github/workflows/deploy-android.yml)) chạy **thủ
công**, giống bản iOS: mở Actions, chọn nhánh muốn ship, gõ marketing version và chọn track. Nó
build `.aab` đã ký, tải lên Google Play, và đính kèm ProGuard mapping để crash report còn đọc
được.

> [!IMPORTANT]
> **Version code không được commit.** Nó suy ra từ số thứ tự lần chạy workflow, nên phát hành
> không cần bump version trong cây mã, và hai lần deploy không bao giờ giành cùng một mã.
> `versionCode` / `versionName` trong `app/build.gradle.kts` chỉ là thứ một bản build tại máy
> nhận được; CI truyền `-Pfunput.versionCode` và `-Pfunput.versionName` đè lên.

`dry_run` build và ký nhưng không tải lên. Dù chọn kiểu nào, `.aab`, SHA-256 của nó và file
mapping đều được đính vào mỗi lần chạy.

Các secret mà workflow cần được liệt kê ngay ở đầu file đó. **Tải lên không phải là phát hành:**
bản build tới đúng track bạn chọn, còn đẩy nó lên production vẫn là một hành động có chủ ý trong
Play Console.

Kiến trúc dài hạn: [`docs/ARCHITECTURE_PROPOSAL.md`](docs/ARCHITECTURE_PROPOSAL.md).

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
