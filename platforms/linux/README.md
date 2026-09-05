<p align="center">
  <img
    src="../../assets/horizontal-lockup/gradient.png"
    width="420"
    alt="Funput"
  >
</p>

<p align="center">
  <strong>Funput cho Linux</strong> — bộ gõ tiếng Việt cho <b>Fcitx5</b> và <b>IBus</b>.<br>
  Hai shell · một composer dùng chung · engine Rust qua <code>funput-ffi</code>
</p>

<p align="center">
  <a href="https://repo.funput.app">
    <img src="https://img.shields.io/badge/Kho_phần_mềm-repo.funput.app-22C55E?style=for-the-badge&logo=linux&logoColor=white" alt="Kho Funput">
  </a>
  <a href="https://docs.funput.app/docs/install/linux">
    <img src="https://img.shields.io/badge/Tài_liệu-Hướng_dẫn_cài_đặt-2563EB?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Hướng dẫn cài đặt">
  </a>
  <a href="https://github.com/Funput/Funput/issues">
    <img src="https://img.shields.io/badge/Hỗ_trợ-Báo_lỗi-E11D48?style=for-the-badge&logo=github&logoColor=white" alt="Báo lỗi">
  </a>
</p>

---

## Tính năng

| | |
|---|---|
| ⌨️ **Ba kiểu gõ** | Telex · **Telex nâng cao** (thêm `w` đầu từ và phím tắt `[` `]`) · VNI |
| 🔤 **Kiểu đặt dấu** | Truyền thống (`hòa`, `khỏe`) hoặc hiện đại (`hoà`, `khoẻ`) |
| 🧠 **Gõ thông minh** | Tự khôi phục tiếng Anh · khôi phục tức thì · kiểm tra chính tả · tự động viết hoa |
| ✂️ **Gõ tắt** | Bảng viết tắt tự bung, tuỳ chọn khớp cả hoa lẫn thường |
| 🔄 **Chuyển mã** | Đổi qua lại giữa Unicode dựng sẵn, Unicode tổ hợp, TCVN3 (ABC) và VNI-Windows |
| ⚙️ **App Cài đặt GTK4** | Dùng chung cho cả hai shell, giao diện libadwaita |
| 📝 **Chế độ không preedit** | Gõ thẳng vào tài liệu thay vì hiện preedit — cứu được Chrome. [Chi tiết](#chế-độ-không-preedit) |
| 🔁 **Nạp lại cấu hình tức thì** | `inotify` theo dõi `settings.json`, không cần khởi động lại |

## Yêu cầu

| | |
|---|---|
| **Framework** | **Fcitx5** hoặc **IBus** — chọn đúng cái mà phiên desktop đang chạy |
| **Distro** | Debian/Ubuntu (`.deb`) · Fedora/openSUSE (`.rpm`) · Arch (`pacman`) |
| **Kiến trúc** | x86-64 |
| **Quyền root** | Cần, trừ khi dùng `install.sh --user` |

| Framework | Gói | Dùng khi |
|---|---|---|
| **Fcitx5** | `funput` | **Khuyến nghị.** Chạy được với cả những client mà IBus không với tới, ví dụ WPS Office |
| IBus | `funput-ibus` | Bạn muốn dùng đúng thứ GNOME/Ubuntu đã nối sẵn và không cần đụng biến môi trường |

Fcitx5 là mặc định của `install.sh` trên **mọi** desktop. Đổi lại, ngoài KDE bạn phải đặt biến
môi trường cho session rồi đăng nhập lại — xem [mục Cài đặt](#cài-đặt).

> [!CAUTION]
> **Đừng cài cả hai gói cho cùng một phiên desktop.** Chạy hai bộ gõ song song sẽ tranh nhau
> phím và cho ra hành vi khó lần ra. Chọn đúng một framework mà session đang dùng.

## Cài đặt

Cách khuyến nghị là **kho `repo.funput.app`** — kho apt/dnf/zypper/pacman có ký GPG, nên
`apt upgrade` hay `dnf upgrade` sẽ nâng cấp Funput như mọi gói hệ thống khác. Thêm kho một lần,
sau đó quên nó đi.

Nhanh nhất là để script tự dò distro rồi cấu hình kho:

```bash
curl -fsSL https://raw.githubusercontent.com/Funput/Funput/main/platforms/linux/install.sh | bash
```

Script cài gói **Fcitx5** trên **mọi** desktop; `--ibus` mới chọn bản IBus. Trước đây nó đoán
theo `XDG_CURRENT_DESKTOP` (KDE → Fcitx5, còn lại → IBus), và điều đó trao cho phần lớn người
dùng đúng cái shell **không với tới nổi** một client kiểu WPS.

| Cờ | Việc |
|---|---|
| `--ibus` · `--fcitx5` | Chọn framework |
| `--dry-run` | In ra mọi lệnh sẽ chạy, **không chạy gì cả** |
| `--no-repo` | Bỏ qua kho, tải thẳng asset từ GitHub Releases — **không tự cập nhật** |
| `--version vX.Y.Z` | Cài đúng một phiên bản (ngầm định `--no-repo`) |
| `--user` | Cài vào `~/.local`, **không cần root** |

> [!IMPORTANT]
> **Ngoài KDE, cài xong Fcitx5 vẫn chưa gõ được ngay.** Phiên desktop của bạn đang nối vào
> **IBus**, nên Fcitx5 không nhận được gì cho tới khi bạn đặt biến môi trường cho session rồi
> **đăng xuất đăng nhập lại**. Script nói điều này trước khi cài, và in lại các biến đó sau khi
> cài xong.
>
> Funput **không** tự đặt `GTK_IM_MODULE` / `QT_IM_MODULE` giúp bạn — chúng thuộc về session,
> không thuộc về chúng tôi, và đặt sai còn hại hơn không đặt. Giá trị đúng phụ thuộc
> **loại session trước tiên**:
>
> | Session | Cần đặt |
> |---|---|
> | **X11** | Bộ ba kinh điển `XMODIFIERS` + `GTK_IM_MODULE` + `QT_IM_MODULE` |
> | **Wayland · GNOME** *(và sway)* | `XMODIFIERS=@im=fcitx` và `QT_IM_MODULE=fcitx` — hoặc `QT_IM_MODULES=wayland;fcitx` trên Qt 6.8.2+ — **để trống** `GTK_IM_MODULE` |
> | **Wayland · KDE** | Chỉ `XMODIFIERS=@im=fcitx` |
>
> Dưới Wayland, GTK 3/4 tự nói chuyện với compositor qua `text-input-v3`, nên đặt cả bộ ba
> khiến KWin nhấp nháy cửa sổ gợi ý. `install.sh` đọc `$XDG_SESSION_TYPE` và chỉ in đúng khối
> áp dụng cho bạn. Trên Debian/Ubuntu việc này thuộc về `im-config` *(vốn không làm gì trong
> phiên Wayland — hook của nó ship ở trạng thái tắt)*, trên Fedora là `fcitx5-autostart`.
> Nguồn: [Using Fcitx 5 on Wayland](https://fcitx-im.org/wiki/Using_Fcitx_5_on_Wayland) ·
> [Setup Fcitx 5](https://fcitx-im.org/wiki/Setup_Fcitx_5).

> [!NOTE]
> `install.sh` được phát hành dưới dạng một dòng `curl … | bash`, tức là đòi hỏi rất nhiều
> lòng tin. Nên nó **in kế hoạch ra trước khi làm**, có `--dry-run`, và mọi asset lấy từ GitHub
> Releases đều được đối chiếu với **SHA-256 do API của release công bố** — các bản phát hành
> không kèm file `.sha256`, nên thật ra chưa bao giờ có chuyện tự kiểm checksum bằng tay.

Các bước thêm kho thủ công cho từng distro nằm ở
[docs.funput.app — Linux](https://docs.funput.app/docs/install/linux) và
[repo.funput.app](https://repo.funput.app) (có sẵn nút copy lệnh).

> [!NOTE]
> **Không có quyền root?** Dùng `install.sh --user`: nó đặt engine vào `~/.local`, không đụng
> tới trình quản lý gói. Cần sẵn daemon IBus hoặc Fcitx5 trên máy. IBus dùng được ngay; Fcitx5
> còn ghi thêm `~/.config/environment.d` nên phải đăng xuất rồi đăng nhập lại.

### Bật bộ gõ

**IBus (GNOME / Ubuntu):** Settings → Keyboard → Input Sources → **+** → Vietnamese → **Funput**.

**Fcitx5:** Fcitx5 Configuration → **+** → bỏ chọn "Only Show Current Language" → **Funput**.

Nếu chưa thấy Funput trong danh sách:

- **IBus:** chạy `ibus restart`, hoặc đăng xuất rồi đăng nhập lại.
- **Fcitx5:** đăng xuất rồi đăng nhập lại.

> [!CAUTION]
> **Trên KDE, đừng chạy `fcitx5 -r`.** KWin khởi động Fcitx5 từ Virtual Keyboard KCM và trao
> cho nó một socket mà tiến trình thay thế **không kế thừa được**, nên khởi động lại daemon sẽ
> làm hỏng việc nhập liệu của mọi client Wayland cho tới lần đăng nhập kế tiếp. Đăng xuất rồi
> đăng nhập lại mới là cách đúng.

## Dùng hằng ngày

### Phím tắt

| Phím | Việc |
|---|---|
| **`Ctrl` + `` ` ``** | Bật / tắt tiếng Việt *(mặc định)* |
| **`Ctrl` `Shift` `Z`** | Lật lại từ vừa gõ *(mặc định tắt)* |

Đổi được trong app **Cài đặt**. Phím chuyển có 5 lựa chọn: `Ctrl` + `` ` ``, `Ctrl Space`,
`Alt Shift`, `Super Space`, `Ctrl Shift Space`.

### Thử nhanh

| Kiểu gõ | Gõ | Ra |
|---|---|---|
| Telex | `tieesng vieejt` | tiếng việt |
| VNI | `xin chao2` | xin chào |

### Cấu hình lưu ở đâu

```
~/.config/Funput/settings.json
```

Sửa bằng app **Cài đặt**, hoặc sửa tay file JSON cũng được — một watcher `inotify` sẽ nạp lại
ngay, không cần khởi động lại bộ gõ.

### Chuyển mã

Có hai lối vào, vì Linux **không có khay hệ thống** như chỗ Windows đặt nó:

- **Cài đặt → Chung → “Công cụ chuyển mã”**
- Mục **Funput Chuyển mã** trong menu ứng dụng *(chạy `funput-settings --convert`)*

Cả hai đều tới cùng một process.

## Giới hạn đã biết

> [!IMPORTANT]
> **Không có tính năng nhớ VI/EN theo từng ứng dụng trên Linux.** Trên GNOME/Wayland, mọi
> ứng dụng đều báo tên tiến trình là `gnome-shell`, không phải app thật — nên một chính sách
> dựa trên giá trị đó sẽ áp cho tất cả cùng lúc. macOS và Windows có tính năng này; Linux thì
> không, và đó là giới hạn của nền tảng chứ không phải thiếu sót chưa làm.

- **Chrome không hiện preedit.** Commit vào bình thường, nhưng mọi thứ cần client tự vẽ và quản
  lý composition thì không. Đây là phía Chrome, Funput không với tới được —
  [chế độ không preedit](#chế-độ-không-preedit) chính là câu trả lời cho Chrome.
- **WPS Office không vẽ preedit của client.** Nó lại khai là *có* năng lực Preedit, nên panel
  preedit của Fcitx5 cũng bị bỏ qua, khiến từ đang gõ vô hình cho tới khi nhấn Space. Chế độ
  không preedit cũng không chạy được: WPS không hiện thực `Qt::ImSurroundingText`, và ép
  xoá-rồi-commit sẽ làm nát chữ (`nguyen64` trả về `nguyenênễn`). Vì vậy **shell Fcitx5 tự vẽ
  panel preedit** cho mọi tên tiến trình nằm trong allowlist ở `hidden_preedit.cpp` (hiện là
  `wps`, `wpp`, `et`, `wpspdf`), đồng thời vẫn gửi client preedit để không mất lần flush khi
  focus-out. Thêm một client kiểu này chỉ là thêm một cái tên vào danh sách đó. **IBus không có
  kênh nào chạy được ở đây** — ForwardKeyEvent đã bị bỏ, surrounding text thì không bao giờ
  tới — nên IBus được để yên.
- **Bỏ dấu sau Backspace chỉ chạy ở chế độ không preedit.** Ở chế độ preedit, Backspace chỉ rút
  ngắn composition, nên `phủ` ␣ ⌫ `s` ra chữ `s` thường.
- **Một số client làm mất từ đang gõ dở khi đổi focus.** Cả hai shell giao việc flush cho
  framework; client nào bỏ preedit thay vì commit thì mất từ đó.
- **App Cài đặt ghi đè nguyên `settings.json`.** Nó serialize struct của chính nó lên file, nên
  một khoá nó không biết sẽ bị xoá ở lần bạn đổi thiết lập kế tiếp.
- **Chưa có AppStream metainfo**, nên `funput-settings` không xuất hiện trong GNOME Software
  hay KDE Discover.

## Gỡ cài đặt

```bash
sudo apt remove funput funput-ibus funput-settings     # Debian / Ubuntu
sudo dnf remove funput funput-ibus funput-settings     # Fedora / openSUSE
sudo pacman -R funput funput-settings                  # Arch
```

Xoá luôn cấu hình nếu muốn sạch hẳn:

```bash
rm -rf ~/.config/Funput
```

Cách gỡ kho `repo.funput.app` nằm ở [docs.funput.app — Linux](https://docs.funput.app/docs/install/linux#gỡ-kho-funput-nếu-cần).

---

## Dành cho developer

### Kiến trúc

Hai shell input-method — **Fcitx5** và **IBus** — chồng lên **một composer dùng chung**, và
composer đó điều khiển engine Rust ([`crates/funput-engine`](../../crates/funput-engine)) qua
C ABI của [`funput-ffi`](../../crates/funput-ffi).

Điểm mấu chốt: **shell không quyết định gì cả.** Nó dịch sự kiện phím của framework thành
`funput::KeyEvent`, đưa cho `funput::Composer`, rồi thi hành `funput::ComposePlan` nhận về. Mọi
chuyện *nên làm gì* nằm hết trong `common/compose/`. Trước khi có tách bạch này, cây quyết định
tồn tại hai bản — mỗi shell một — và chúng đã trôi khỏi nhau.

```text
        Ứng dụng đang gõ
               │  sự kiện phím
               ▼
   Fcitx5 addon          IBus engine
   libfunput.so          ibus-engine-funput
               │  chỉ dịch sự kiện, không quyết định
               ▼
   common/compose/   Composer — toàn bộ luật gõ, MỘT bản duy nhất
               │     không tham chiếu ký hiệu nào của Fcitx5/IBus
               ▼
   funput-ffi ──▶ funput-engine ──▶ funput-core
               │
               ▼
   ComposePlan ──▶ shell thi hành: preedit · commit · deleteChars
```

Thứ thật sự khác nhau giữa hai shell chỉ là preedit tới client bằng đường nào, và **ai commit
nó khi mất focus** — Fcitx5 dùng `setClientPreedit` cộng watcher focus-out, IBus dùng
`IBUS_ENGINE_PREEDIT_COMMIT`. Cả hai đều có chú thích tại chỗ.

Mô hình này giống hệt các nền tảng desktop khác: `crates/funput-desktop/src/key.rs` là đúng cái
classifier ấy cho shell hook trên Windows, và `inject.rs` của nó là đúng cái tách bạch “core trả
về kế hoạch, shell thi hành”.

### Cây thư mục

```text
common/                 C++ thuần, không framework, dùng chung cho cả hai shell
  compose/                Luật gõ: một bản, không ký hiệu Fcitx5/IBus
    plan.h                  ComposePlan — shell được yêu cầu làm gì
    key/
      event.h                 Mods / KeyEvent / hằng keysym (không phụ thuộc gì)
      classify.h/.cpp         KeyKind + ý nghĩa của một phím
      boundary.h              Kiểm tra ranh giới từ
    composer/
      composer.h              Máy trạng thái (engine + settings + VI/EN)
      keys.cpp                onKey(): cây quyết định cho mỗi phím
      state.cpp               Settings, VI/EN, và các lối thoát không phải phím
      nonpreedit.h            Chế độ commit-khi-gõ, và cách đánh giá một client
      nonpreedit.cpp          Composer làm gì với đánh giá đó
  settings/               ~/.config/Funput/settings.json
    settings.h              Model mọi shell đều đọc, gồm cả công tắc gõ tắt
    lookup.cpp              File nằm đâu; loại trừ theo app
    io.cpp                  Parse và lưu (JSON)
    watch.h/.cpp            Watcher inotify để nạp lại tức thì
  ffi/                    C ABI của funput-ffi
    handle.h                Bọc RAII quanh handle của engine
    utf8.h                  Chuyển đổi UTF-8 <-> UTF-32
  tests/                  doctest, cây thư mục soi gương phần trên
fcitx5/src/             Addon Fcitx5 -> libfunput.so
  funput_input.cpp        Một phím: chuẩn hoá, hỏi composer
  funput_client.cpp       Nói chuyện với client, cả hai chiều
  hidden_preedit.cpp      Danh sách client tự giấu preedit (allowlist)
ibus/src/               IBus engine -> ibus-engine-funput
  engine.h                Kiểu GObject công khai
  engine/                 internal.h, object.cpp, callbacks.cpp, client.cpp
settings-gtk/           App Cài đặt GTK4 + libadwaita (crate cargo riêng,
                        và là gói riêng — xem mục Build)
  src/settings_window/    mỗi trang preferences một submodule
    shortcuts/              công tắc gõ tắt, các hàng, trạng thái rỗng
  src/convert/            cửa sổ Chuyển mã — xem bên dưới
packaging/              hai file .desktop, metadata kho apt/dnf/pacman
```

Mỗi thư mục giữ tối đa năm file; vượt quá thì tách theo mối quan tâm chứ không theo kích thước.
Mọi file ở đây bị giới hạn **150 dòng** bởi `scripts/check-loc.sh`, kể cả file test.

### Build

Cài dependency cho shell bạn định đóng gói:

```bash
sudo apt-get install cmake nlohmann-json3-dev fcitx5 libfcitx5core-dev libfcitx5utils-dev libfcitx5config-dev ibus libibus-1.0-dev libglib2.0-dev libgtk-4-dev libadwaita-1-dev librsvg2-dev
```

Rồi chạy từ thư mục `app/` của repo:

```bash
FUNPUT_FRAMEWORK=all platforms/linux/build.sh
```

`FUNPUT_FRAMEWORK` nhận `fcitx5`, `ibus` hoặc `all`; `FUNPUT_PKG` nhận `deb` hoặc `rpm` (mặc
định theo host). Mỗi shell là một project CMake cấp cao riêng, nên đóng gói độc lập được.

**Ra ba gói, không phải hai:**

| Gói | Chứa |
|---|---|
| `funput` | Addon Fcitx5 |
| `funput-ibus` | Engine IBus |
| `funput-settings` | App GUI, launcher và icon |

Bốn file của GUI trước kia được **cả hai** shell ship, khiến dpkg từ chối giải nén gói thứ hai —
nội dung giống hệt nhau vẫn tính là xung đột file, nên cài cả hai là bất khả thi trên
Debian/Ubuntu. Giờ `funput-settings` được build bất kể bạn yêu cầu framework nào, vì cả hai đều
phụ thuộc vào nó.

Phụ thuộc đó ghim **đúng phiên bản** (`funput-settings (= <version>)`), và đây mới là phần đáng
giữ: app Cài đặt ghi lại `settings.json` từ struct của chính nó, nên một bản cũ hơn addon sẽ xoá
mất những thiết lập mà addon đã học được từ lúc ấy. Trình quản lý gói bây giờ từ chối đúng cái
lệch phiên bản từng gây ra chuyện đó.

### Test

`common/` build và test được độc lập, **không cần cài Fcitx5 lẫn IBus** — đó chính là thứ mà
việc `compose/` không link ký hiệu framework nào mua về. Chỉ cần `cmake`,
`nlohmann-json3-dev` và cargo:

```bash
cmake -S platforms/linux -B platforms/linux/build/tests -DFUNPUT_BUILD_TESTS=ON
cmake --build platforms/linux/build/tests --parallel
ctest --test-dir platforms/linux/build/tests --output-on-failure
```

Test link `libfunput_ffi.so` thật, nên nó chạy qua engine Rust bằng C ABI chứ không phải mock.
`platforms/linux/CMakeLists.txt` tồn tại chỉ vì việc này — đóng gói không bao giờ đi qua nó, và
`FUNPUT_BUILD_TESTS` mặc định `OFF` nên người đóng gói distro không bao giờ kích hoạt bước tải
framework.

> [!WARNING]
> CI chạy phần này ở mọi pull request (`ci.yml`, job `linux-common`). Nhưng **hai shell chỉ được
> biên dịch bởi workflow phát hành**, nên thay đổi trong `fcitx5/` hay `ibus/` vẫn cần build tại
> máy trước khi merge.

### Phát hành

Ba kênh, cùng ăn từ một bản phát hành:

- **`repo.funput.app`** — kho apt, dnf/zypper và pacman có ký, host trên GitHub Pages, dựng bởi
  `.github/workflows/publish-repo.yml`. Đây là kênh **duy nhất** có nâng cấp tự động, và là thứ
  `install.sh` cấu hình mặc định. Xem `packaging/repo/README.md`.
- **GitHub Releases** — chính các file `.deb`/`.rpm`, cộng cây `.tar.gz` portable đứng sau
  `install.sh --user`.
- **`install.sh`** — lớp mặt tiền dò-rồi-cấu-hình cho hai kênh trên.

**Arch nằm trong kênh thứ nhất**, không phải một kênh riêng. Nó không có release asset — một
distro rolling chẳng dùng gì `.deb` hay `.rpm` — nên job `pacman` trong `publish-repo.yml` *tự
build* từ `packaging/arch/PKGBUILD.in` rồi phục vụ kết quả dưới `arch/x86_64/`. AUR mới là chỗ
thông thường cho công thức này và nó được viết theo đúng quy ước AUR, nhưng không có gì tải nó
lên: Arch đang tạm ngưng đăng ký tài khoản mới. `arch-recipe.yml` chạy `makepkg` thật trên các
pull request đụng vào công thức, để lỗi bị bắt trước khi làm hỏng một lần publish.

Cái giá của binary trên distro rolling là chúng link thư viện của **ngày build**, nên một cú
soname bump ở fcitx5, ibus hay gtk4 sẽ làm chúng gãy. Dependency không diễn đạt được ràng buộc
đó: gói `fcitx5` và `ibus` của Arch không khai `provides` theo soname (chỉ gtk4, libadwaita và
glib2 có), nên pacman chẳng có gì để kiểm và không thể cảnh báo. Vì vậy cách chữa là **một lịch
chạy, không phải một dependency** — `publish-repo.yml` còn chạy vào ngày 1 và 15, build lại bản
phát hành hiện tại trên nền Arch hiện tại, với `pkgrel` render thành ngày build để pacman thấy
có nâng cấp dù `pkgver` không đổi. Đó là công dụng của `@PKGREL@` trong template; AUR render nó
thành `1`.

### Chuyển mã

Bộ chuyển bảng mã (`settings-gtk/src/convert/`) **dùng chung cho cả hai shell** theo nghĩa mạnh
nhất: nó không bao giờ hỏi shell nào đang chạy, không đọc `settings.json`, và không đụng tới
engine. Fcitx5 và IBus đều phụ thuộc gói `funput-settings` ở đúng phiên bản, nên cả hai đều có
nó mà không phải đổi một dòng C++ nào.

Mọi thứ về việc *một lần chuyển mã là gì và tốn gì* nằm trong
[`crates/funput-convert`](../../crates/funput-convert/README.md), dùng chung với cửa sổ Slint
trên Windows — cảnh báo mất chữ, luật xử lý hàng loạt, tên thư mục `Đã chuyển mã` và cách tránh
trùng tên bằng `vanban (2).txt`. Viết hai lần là cách để hai nền tảng bắt đầu bất đồng về cùng
một tài liệu. Phần ở lại đây là kéo-thả, clipboard, hộp thoại, và đẩy I/O file ra khỏi luồng UI.

`src/convert/state.rs` là file duy nhất dưới `convert/` có quyết định gì, và cũng là file duy
nhất có test. Các file `ui/*` đọc state rồi set property, không quyết định gì. Một chốt
`refreshing` canh mọi tín hiệu mà một lần refresh bắn ra — thiếu nó, refresh set dropdown sẽ tái
nhập qua `notify::selected` và panic vì `borrow_mut` lồng nhau.

### Chế độ không preedit

Mặc định tắt; công tắc nằm trong Cài đặt mục **Kiểu gõ**, hoặc đặt thẳng `"nonPreedit": true`
trong `~/.config/Funput/settings.json`. Kiểu nào thì watcher cũng nhận ra ngay, không cần khởi
động lại. **Cả hai shell đều thi hành nó.**

Thay vì đỗ từ đang gõ trong một preedit, mỗi phím commit thẳng vào tài liệu rồi sửa lại thứ phím
trước đã viết — `Effect::Replace` mang theo “xoá N ký tự, rồi commit cái này”. Đó đúng là chỉ thị
mà shell hook trên Windows đã thi hành (`crates/funput-desktop/src/inject.rs`), và N tới thẳng từ
engine dưới dạng `FunputResult::backspace`, nên các nền tảng giữ **một** hành vi chứ không phải
ba. Backspace lùi vào một từ đã xong sẽ mở lại từ đó, nên `phủ` ␣ ⌫ `s` cho ra `phú`; Windows giữ
một bản bóng của thứ nó đã gõ để làm việc này (`retone.rs`), còn ở đây thì đọc thẳng tài liệu là
được.

Một Backspace khi không có gì đang soạn sẽ do **chính chúng ta** thi hành, không chuyển cho app,
để việc xoá đi cùng một kênh với mọi sửa chữa khác. Để app tự làm chính là thứ đã làm hỏng việc
bỏ dấu lại trong thanh địa chỉ Chrome: app sửa sau lưng ta rồi vứt bỏ sửa chữa được phát ở phím
kế tiếp. Việc tiếp quản chỉ xảy ra khi **có bằng chứng dương** rằng tài liệu đang đọc là mới —
một thay đổi ta dự đoán rồi thấy nó xảy ra thật. Không có bằng chứng đó thì phím đi thẳng qua, và
chính điều này giữ cho một vùng bôi đen bằng chuột vẫn xoá được.

#### Đo được gì, và suy ra gì

Trên GNOME/Wayland, nơi Fcitx5 được với tới qua frontend **ibus** và GNOME Shell là client của
mọi ứng dụng:

- **Cờ capability nói dối theo cả hai chiều.** Có context báo không có `SurroundingText` trong
  khi nó chạy hoàn hảo, có context báo `caps: []` — không có cả `Preedit` — trong khi preedit rõ
  ràng vẫn chạy. Không shell nào gác theo cờ; thứ client **thực sự làm** mới quyết định.
- **`program()` luôn là `gnome-shell`**, không bao giờ là app thật. Đó là lý do Linux không có
  danh sách VI/EN theo từng app.
- **`deleteSurroundingText` đếm ký tự, không đếm byte** — kiểm bằng `ế`, một ký tự và ba byte.
  Vì vậy `ComposePlan::deleteChars` là số ký tự từ đầu đến cuối; một test chỉ dùng ASCII sẽ
  không bắt được khác biệt này.
- **Client chỉ trả lời 61% số commit**, và kênh này chết rồi sống lại ngay trong một phiên, nên
  không được phép chờ nó.
- **Một chuỗi commit/delete/commit phát ra không nghỉ sẽ phá nát chữ của người dùng**: `;;;` trả
  về `;;y` trong cả chín lần thử.
- **Nhưng gõ tay không thể tạo ra chuỗi đó.** Một `Replace` cần cú phím khiến engine viết lại
  phần đuôi. Hai lần liên tiếp chưa bao giờ gần nhau hơn 49 ms, và giữ phím cũng không giúp gì —
  sau lần nhấn thứ hai engine ngừng viết lại, các lần lặp đi thẳng qua, để lại 500 ms giữa hai
  `Replace` khi auto-repeat, chậm gấp mười lần gõ tay.

Ba điều cuối cộng lại là lý do **các lần ghi cố tình không được tuần tự hoá**. Chờ client xác
nhận lần ghi này trước khi phát lần sau sẽ tốn ~25 ms mỗi phím và treo hẳn ở những commit nó
không bao giờ trả lời — một cái giá chắc chắn để đổi lấy một thất bại mà gõ tay không với tới
được.

Không chờ trước khi ghi **không** đồng nghĩa với không bao giờ kiểm tra sau khi ghi. Việc lẫn lộn
hai điều đó từng để thanh địa chỉ Chrome làm hỏng chữ: nó nhận commit nhưng bỏ
`deleteSurroundingText` đi kèm, nên `phủ` ra thành `phủú`. Phép kiểm chẳng tốn gì — hai shell vốn
đã đọc tài liệu ở mỗi phím. Sau một `Replace{N, T}` phát ra trên tài liệu `D`, lần đọc kế tiếp
chỉ có thể là một trong ba chuỗi, và chúng khác nhau:

| Đọc được | Nghĩa là |
|---|---|
| `D` bớt N ký tự, rồi `T` | Thành công |
| `D` rồi `T` | Client đã bỏ lệnh xoá — **tắt chế độ này cho client đó** |
| `D` | Client chưa trả lời — chưa có phán quyết |

Chỉ trường hợp ở giữa là một phán quyết, và sự dè dặt đó mới là điểm mấu chốt: coi “không giống
thứ tôi mong đợi” là thất bại sẽ tắt chế độ này ở 61% số commit không được trả lời — chữa một
client hỏng bằng cách làm hỏng tính năng cho tất cả.

Phán quyết rút lui hẹp đúng bằng phạm vi thất bại. Một sửa chữa bị bỏ **sau khi mở lại một từ**
chỉ khiến mất khả năng bỏ dấu lại: đó là hình dạng duy nhất từng thấy hỏng. Bất kỳ sửa chữa nào
khác bị bỏ thì mất cả chế độ, vì không còn tin được thứ gì nó viết ra.

Phán quyết là một **chốt**, không phải một biến. Shell có thể tái khẳng định chế độ — IBus làm
vậy ở mỗi phím — và không được phép hồi sinh một chế độ mà client đã bị bắt quả tang làm hỏng;
chỉ `Composer::onFocusChanged()` xoá chốt, vì một client mới là một câu hỏi mới.

Cái giá là việc phát hiện chậm một nhịp, nên từ đầu tiên vẫn bị hỏng trước khi chế độ rút lui.
Bắt sớm hơn nghĩa là phải ghi chữ dò vào tài liệu của người dùng, và điều đó không nằm trên bàn.

#### Về phía IBus

Bốn sự thật về API, mỗi cái tốn vài vòng mới tìm ra, và không cái nào được header nói cho biết:

- **Đăng ký nhận surrounding text là theo từng input context, không phải theo engine.** IBus ghi
  tài liệu cho `ibus_engine_get_surrounding_text()` với out-param null như cách đăng ký và bảo
  gọi nó “trong enable handler” — nhưng enable chỉ chạy một lần, và engine nào chỉ hỏi ở đó sẽ
  không nhận được gì trong suốt phần còn lại của phiên. **Phải hỏi lại ở mỗi lần focus-in.** Cho
  tới khi tìm ra điều này, chế độ không preedit chưa từng một lần hoạt động trên IBus, trong khi
  trông như thể có.
- **Nhưng hỏi không phải là thứ làm nó tới — daemon khử trùng lặp.**
  `bus_engine_proxy_set_surrounding_text()` chỉ gọi xuống engine khi text, con trỏ hoặc anchor
  **khác** với thứ nó chuyển tiếp lần trước. Cache đó nằm trên **engine proxy**, được gieo sẵn
  bằng `("", 0, 0)`, và **không bao giờ bị xoá khi đổi focus** — chỉ khi proxy bị huỷ. Nên một
  client trả lời bằng chuỗi rỗng là **vô hình** với engine, dù có hỏi bao nhiêu lần đi nữa.
  WPS Office đúng là client đó: trình soạn thảo của nó không hiện thực
  `Qt::ImSurroundingText`, nên Qt trả lời mọi yêu cầu bằng `("", 0, 0)` — đúng bằng giá trị
  gieo — và `set_surrounding_text` không nổ lấy một lần, trong khi `SetSurroundingText` đáp
  xuống daemon **mười sáu lần một phút**. Đếm ở phía client rồi gọi đó là “có hỗ trợ” chính là
  cái bẫy; **payload mới là sự thật**.

  Cũng chính cache đó khiến việc `focusIn()` xoá `sawSurroundingText` trở thành một lỗi tiềm
  ẩn: daemon sẽ không lặp lại chính nó cho một focus mới, nên sau khi bị xoá, cờ đó chỉ có thể
  trở lại nếu tài liệu **thay đổi**. Chỗ phải sửa là phía xoá cờ, không phải phía hỏi — thử
  hỏi lại rồi, không xê dịch gì. Chưa làm: chưa client nào ở đây lộ triệu chứng, và đoán một
  bản vá cho một client chính là cách mà đám cờ capability được tin tưởng lần đầu.
- **Đọc ngược lại không được.** `ibus_engine_get_surrounding_text()` trả về text null ngay sau
  khi client vừa gửi một chuỗi hoàn toàn tốt. Bản sao đáng tin duy nhất là bản đến trong vfunc
  `set_surrounding_text`, nên engine giữ nó trong `EngineState`.
- **`g_debug` không tới tay ai.** ibus-daemon trỏ stdio của engine nó sinh ra vào `/dev/null` bất
  kể daemon được khởi động kiểu gì, nên chẩn đoán ở đây phải ghi ra file.

Việc đếm theo ký tự được xác nhận lần thứ hai từ phía này, không cần suy luận: client báo con trỏ
ở 4 cho `phủ ` (năm byte) và 3 cho `phủ` (bốn byte).

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
