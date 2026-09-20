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

<div align="center">
  <table>
    <tr>
      <td align="center" valign="top" width="20%">
        <a href="https://apps.apple.com/vn/app/id6788829996">
          <picture>
            <source media="(prefers-color-scheme: dark)" srcset="https://funput.app/ios-dark.svg">
            <img src="https://funput.app/ios.svg" alt="iOS" width="40" height="40">
          </picture>
          <br><strong>iOS</strong><br>
          <sub>App Store</sub>
        </a>
      </td>
      <td align="center" valign="top" width="20%">
        <a href="https://play.google.com/store/apps/details?id=app.funput.funput">
          <picture>
            <source media="(prefers-color-scheme: dark)" srcset="https://funput.app/android-dark.svg">
            <img src="https://funput.app/android.svg" alt="Android" width="40" height="40">
          </picture>
          <br><strong>Android</strong><br>
          <sub>Google Play</sub>
        </a>
      </td>
      <td align="center" valign="top" width="20%">
        <a href="https://github.com/Funput/Funput/releases/latest">
          <picture>
            <source media="(prefers-color-scheme: dark)" srcset="https://funput.app/apple-dark.svg">
            <img src="https://funput.app/apple.svg" alt="macOS" width="40" height="40">
          </picture>
          <br><strong>macOS</strong><br>
          <sub>Tải xuống</sub>
        </a>
      </td>
      <td align="center" valign="top" width="20%">
        <a href="https://github.com/Funput/Funput/releases/latest">
          <picture>
            <source media="(prefers-color-scheme: dark)" srcset="https://funput.app/windows-dark.svg">
            <img src="https://funput.app/windows.svg" alt="Windows" width="40" height="40">
          </picture>
          <br><strong>Windows</strong><br>
          <sub>Tải xuống</sub>
        </a>
      </td>
      <td align="center" valign="top" width="20%">
        <a href="https://docs.funput.app/docs/install/linux">
          <picture>
            <source media="(prefers-color-scheme: dark)" srcset="https://funput.app/linux-dark.svg">
            <img src="https://funput.app/linux.svg" alt="Linux" width="40" height="40">
          </picture>
          <br><strong>Linux</strong><br>
          <sub>Hướng dẫn</sub>
        </a>
      </td>
    </tr>
  </table>
</div>

## Giao diện

<div align="center">
  <table>
    <tr>
      <td align="center" valign="middle" width="50%">
        <a href="assets/screenshot/ios.png">
          <img src="assets/screenshot/ios.png" alt="Bàn phím Funput trên iOS" height="380">
        </a>
        <br><strong>iOS</strong>
      </td>
      <td align="center" valign="middle" width="50%">
        <a href="assets/screenshot/android.png">
          <img src="assets/screenshot/android.png" alt="Bàn phím Funput trên Android" height="380">
        </a>
        <br><strong>Android</strong>
      </td>
    </tr>
  </table>
  <table>
    <tr>
      <td align="center" valign="middle" width="33%">
        <a href="assets/screenshot/macOS.png">
          <img src="assets/screenshot/macOS.png" alt="Giao diện Funput trên macOS" height="210">
        </a>
        <br><strong>macOS</strong>
      </td>
      <td align="center" valign="middle" width="33%">
        <a href="assets/screenshot/windows.png">
          <img src="assets/screenshot/windows.png" alt="Giao diện Funput trên Windows" height="210">
        </a>
        <br><strong>Windows</strong>
      </td>
      <td align="center" valign="middle" width="33%">
        <a href="assets/screenshot/linux.png">
          <img src="assets/screenshot/linux.png" alt="Giao diện Funput trên Linux" height="210">
        </a>
        <br><strong>Linux</strong>
      </td>
    </tr>
  </table>
</div>

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
Nếu thấy Funput hữu ích, bạn có thể ủng hộ dự án qua [GitHub Sponsors](https://github.com/sponsors/Funput) hoặc chuyển khoản — hoặc đơn giản là ấn [Star](https://github.com/Funput/Funput).

<p align="center">
  <a href="https://github.com/sponsors/Funput">
    <img src="https://img.shields.io/badge/GitHub-Sponsor-EA4AAA?style=for-the-badge&logo=githubsponsors&logoColor=white" alt="Ủng hộ Funput trên GitHub Sponsors">
  </a>
  <a href="https://github.com/Funput/Funput">
    <img src="https://img.shields.io/github/stars/Funput/Funput?style=for-the-badge&logo=github&logoColor=white&color=F59E0B" alt="Star Funput trên GitHub">
  </a>
</p>

<p align="center">
  <img src="assets/donate/qr.png" width="360" alt="QR ủng hộ Funput">
  <br>
  <sub>Made with ❤️ by Funput</sub>
</p>

## Lời cảm ơn

Funput được xây dựng nhờ cộng đồng.
Đặc biệt cảm ơn:

<div align="center">
  <table>
    <tr>
      <td align="center" valign="top" width="33%">
        <a href="https://github.com/zenfas">
          <img src="https://github.com/zenfas.png?size=160" width="80" height="80" alt="zenfas">
          <br><strong>zenfas</strong>
        </a>
      </td>
      <td align="center" valign="top" width="33%">
        <a href="https://github.com/quyleanh">
          <img src="https://github.com/quyleanh.png?size=160" width="80" height="80" alt="Quy Le Anh">
          <br><strong>Quy Le Anh</strong><br>
          <sub>@quyleanh</sub>
        </a>
      </td>
      <td align="center" valign="top" width="33%">
        <a href="https://github.com/huyle0406">
          <img src="https://github.com/huyle0406.png?size=160" width="80" height="80" alt="huyle0406">
          <br><strong>huyle0406</strong>
        </a>
      </td>
    </tr>
  </table>
</div>

## License

[MIT](LICENSE) — © Funput

