<p align="right">
  <strong>Tiếng Việt</strong> · <a href="README.en.md">English</a>
</p>

<p align="center">
  <img
    src="assets/horizontal-lockup/gradient.png"
    width="520"
    alt="Funput"
  >
</p>

---

<p align="center">
  <strong>Funput</strong> là bộ gõ / bàn phím tiếng Việt mã nguồn mở, nhẹ và ưu tiên quyền riêng tư.<br>
  Hỗ trợ Telex và VNI trên iOS, Android, macOS, Windows và Linux, mang lại trải nghiệm gõ tiếng Việt quen thuộc và nhất quán trên mọi thiết bị.
</p>

## Bắt đầu

<p align="center">
  <a href="https://github.com/Funput/Funput/releases/latest">
    <img src="https://img.shields.io/badge/Tải_xuống-Bản_mới_nhất-22C55E?style=for-the-badge&logo=github&logoColor=white" alt="Tải phiên bản Funput mới nhất">
  </a>
  <a href="https://docs.funput.app/docs/install/">
    <img src="https://img.shields.io/badge/Tài_liệu-Hướng_dẫn_cài_đặt-2563EB?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Hướng dẫn cài đặt Funput">
  </a>
  <a href="https://github.com/Funput/Funput/issues">
    <img src="https://img.shields.io/badge/Hỗ_trợ-Báo_lỗi-E11D48?style=for-the-badge&logo=github&logoColor=white" alt="Báo lỗi Funput">
  </a>
</p>

## Cài đặt theo nền tảng

| Nền tảng | Trạng thái | Cài đặt |
|---|---|---|
| iOS | Đã phát hành | [Tải trên App Store](https://apps.apple.com/vn/app/id6788829996) |
| macOS | Đã phát hành | [Tải bản mới nhất](https://github.com/Funput/Funput/releases/latest) |
| Windows | Đã phát hành | [Tải bản mới nhất](https://github.com/Funput/Funput/releases/latest) |
| Linux | Đã phát hành | [Xem hướng dẫn](https://docs.funput.app/docs/install/linux) |
| Android | Kiểm thử khép kín | [Gửi email để tham gia](mailto:hello@funput.app) |

> [!NOTE]
> Google Play yêu cầu kiểm thử khép kín đủ **14 ngày** với số tester tối thiểu trước khi xuất bản. Vòng trước chưa đạt ngưỡng đó, nên Funput phải chạy lại vòng 14 ngày.
>
> Nếu bạn dùng Android và muốn giúp đưa Funput lên Play Store, hãy gửi email tới [hello@funput.app](mailto:hello@funput.app) (nên dùng Gmail gắn với tài khoản Google Play). Chúng tôi sẽ mời bạn vào nhóm kiểm thử khép kín.

## Giao diện

<p align="center">
  <img src="assets/screenshot/ios.png" height="420" alt="Bàn phím Funput trên iOS">
  &nbsp;&nbsp;
  <img src="assets/screenshot/android.png" height="420" alt="Bàn phím Funput trên Android">
  <br>
  <sub>Bàn phím Funput trên iOS và Android.</sub>
</p>

<p align="center">
  <img src="assets/screenshot/macOS.png" height="280" alt="Giao diện cài đặt Funput trên macOS">
  &nbsp;&nbsp;
  <img src="assets/screenshot/windows.png" height="280" alt="Giao diện cài đặt Funput trên Windows">
  <br>
  <sub>Giao diện cài đặt trên macOS và Windows.</sub>
</p>

## Kiến trúc

Năm nền tảng dùng chung lõi Rust. Mỗi shell giao tiếp với core qua bridge phù hợp:
[`funput-ffi`](crates/funput-ffi) (C ABI) cho iOS, macOS và Linux;
[`funput-jni`](crates/funput-jni) cho Android; link trực tiếp
[`funput-engine`](crates/funput-engine) cho Windows.

<p align="center">
  <a href="assets/design/architecture.png">
    <img
      src="assets/design/architecture.png"
      width="960"
      alt="Kiến trúc Funput: iOS, macOS, Linux qua funput-ffi; Android qua funput-jni; Windows link trực tiếp; tất cả hội về funput-engine và funput-core"
    >
  </a>
  <br>
  <sub><a href="assets/design/architecture.png">Nhấn để xem ảnh gốc</a></sub>
</p>

## Trạng thái

Funput đang được phát triển tích cực. Tính năng và kiến trúc có thể tiếp tục thay đổi trong các phiên bản đầu.

Bug report, thảo luận và đóng góp đều được chào đón.

## Ủng hộ

Funput là dự án vì cộng đồng — miễn phí cho tất cả mọi người.
Nếu thấy Funput hữu ích, bạn có thể ủng hộ dự án qua [GitHub Sponsors](https://github.com/sponsors/Funput) hoặc chuyển khoản:

<p align="center">
  <a href="https://github.com/sponsors/Funput">
    <img src="https://img.shields.io/badge/GitHub-Sponsor-EA4AAA?style=for-the-badge&logo=githubsponsors&logoColor=white" alt="Ủng hộ Funput trên GitHub Sponsors">
  </a>
</p>

<p align="center">
  <img src="assets/donate/qr.png" width="360" alt="QR ủng hộ Funput">
  <br>
  <sub>Made with ❤️ by Funput</sub>
</p>

## License

[MIT](LICENSE) — © Funput

