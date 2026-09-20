<p align="right">
  <a href="README.md">Tiếng Việt</a> · <strong>English</strong>
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
  <strong>Funput</strong> is an open-source Vietnamese input method and keyboard that is lightweight and privacy-focused.<br>
  It supports Telex and VNI across iOS, Android, macOS, Windows, and Linux, delivering a familiar and consistent Vietnamese typing experience across all your devices.
</p>

## Get started

<p align="center">
  <a href="https://github.com/Funput/Funput/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest_release-22C55E?style=for-the-badge&logo=github&logoColor=white" alt="Download the latest Funput release">
  </a>
  <a href="https://docs.funput.app/docs/install/">
    <img src="https://img.shields.io/badge/Documentation-Installation_guide-2563EB?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Read the Funput installation guide">
  </a>
  <a href="https://github.com/Funput/Funput/issues">
    <img src="https://img.shields.io/badge/Support-Report_an_issue-E11D48?style=for-the-badge&logo=github&logoColor=white" alt="Report a Funput issue">
  </a>
</p>

## Install by platform

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
          <sub>Download</sub>
        </a>
      </td>
      <td align="center" valign="top" width="20%">
        <a href="https://github.com/Funput/Funput/releases/latest">
          <picture>
            <source media="(prefers-color-scheme: dark)" srcset="https://funput.app/windows-dark.svg">
            <img src="https://funput.app/windows.svg" alt="Windows" width="40" height="40">
          </picture>
          <br><strong>Windows</strong><br>
          <sub>Download</sub>
        </a>
      </td>
      <td align="center" valign="top" width="20%">
        <a href="https://docs.funput.app/docs/install/linux">
          <picture>
            <source media="(prefers-color-scheme: dark)" srcset="https://funput.app/linux-dark.svg">
            <img src="https://funput.app/linux.svg" alt="Linux" width="40" height="40">
          </picture>
          <br><strong>Linux</strong><br>
          <sub>Install guide</sub>
        </a>
      </td>
    </tr>
  </table>
</div>

## Interface

<div align="center">
  <table>
    <tr>
      <td align="center" valign="middle" width="50%">
        <a href="assets/screenshot/ios.png">
          <img src="assets/screenshot/ios.png" alt="Funput keyboard on iOS" height="380">
        </a>
        <br><strong>iOS</strong>
      </td>
      <td align="center" valign="middle" width="50%">
        <a href="assets/screenshot/android.png">
          <img src="assets/screenshot/android.png" alt="Funput keyboard on Android" height="380">
        </a>
        <br><strong>Android</strong>
      </td>
    </tr>
  </table>
  <table>
    <tr>
      <td align="center" valign="middle" width="33%">
        <a href="assets/screenshot/macOS.png">
          <img src="assets/screenshot/macOS.png" alt="Funput on macOS" height="210">
        </a>
        <br><strong>macOS</strong>
      </td>
      <td align="center" valign="middle" width="33%">
        <a href="assets/screenshot/windows.png">
          <img src="assets/screenshot/windows.png" alt="Funput on Windows" height="210">
        </a>
        <br><strong>Windows</strong>
      </td>
      <td align="center" valign="middle" width="33%">
        <a href="assets/screenshot/linux.png">
          <img src="assets/screenshot/linux.png" alt="Funput on Linux" height="210">
        </a>
        <br><strong>Linux</strong>
      </td>
    </tr>
  </table>
</div>

## Architecture

All five platforms share one Rust core. Each shell talks to the core through the
right bridge: [`funput-ffi`](crates/funput-ffi) (C ABI) for iOS, macOS, and Linux;
[`funput-jni`](crates/funput-jni) for Android; a direct
[`funput-engine`](crates/funput-engine) link for Windows.

<p align="center">
  <a href="assets/design/architecture.png">
    <img
      src="assets/design/architecture.png"
      width="960"
      alt="Funput architecture: iOS, macOS, and Linux via funput-ffi; Android via funput-jni; Windows via direct link; all converging on funput-engine and funput-core"
    >
  </a>
  <br>
  <sub><a href="assets/design/architecture.png">Click for full-size image</a></sub>
</p>

## Project status

Funput is under active development. Features and architecture may continue to
evolve during the project's early releases.

Bug reports, discussions, and contributions are welcome.

## Support

Funput is a community project — free for everyone.
If you find Funput useful, you're welcome to support the project via [GitHub Sponsors](https://github.com/sponsors/Funput) or bank transfer — or simply open the [repo](https://github.com/Funput/Funput) and click Star in the top right.

<p align="center">
  <a href="https://github.com/sponsors/Funput">
    <img src="https://img.shields.io/badge/GitHub-Sponsor-EA4AAA?style=for-the-badge&logo=githubsponsors&logoColor=white" alt="Sponsor Funput on GitHub">
  </a>
  <a href="https://github.com/Funput/Funput">
    <img src="https://img.shields.io/github/stars/Funput/Funput?style=for-the-badge&logo=github&logoColor=white&color=F59E0B" alt="Star Funput on GitHub">
  </a>
</p>

<p align="center">
  <img src="assets/donate/qr.png" width="360" alt="Donate to Funput">
  <br>
  <sub>Made with ❤️ by Funput</sub>
</p>

## Acknowledgements

Funput exists thanks to the community.
Special thanks to:

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

