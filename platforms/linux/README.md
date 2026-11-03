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
| 🔠 **Phụ âm đầu mở rộng** | Cho phép `z`, `f`, `w`, `j` đầu từ (`zô`, `fải`, `wá`, `jờ`), chọn từng chữ — mặc định tắt |
| ✂️ **Gõ tắt** | Bảng viết tắt tự bung, tuỳ chọn khớp cả hoa lẫn thường |
| 🔄 **Chuyển mã** | Đổi qua lại giữa Unicode dựng sẵn, Unicode tổ hợp, TCVN3 (ABC) và VNI-Windows |
| ⚙️ **App Cài đặt GTK4** | Dùng chung cho cả hai shell, giao diện libadwaita |
| 📝 **Gõ thẳng, không gạch chân** | Chữ vào thẳng ô nhập thay vì đợi ở dòng gạch chân — cứu được Chrome. **Luôn bật**; app nào không nhận được thì tự lùi về gạch chân. [Chi tiết](#chế-độ-không-preedit) |
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
[docs.funput.app — Linux](https://docs.funput.app/docs/install/linux).

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
| **`Ctrl` `Shift` `Z`** | Lật từ đang gõ *(mặc định tắt; bật ở **Cài đặt → Phím tắt**)* |

Đổi được trong app **Cài đặt**. Phím chuyển có 5 lựa chọn: `Ctrl` + `` ` ``, `Ctrl Space`,
`Alt Shift`, `Super Space`, `Ctrl Shift Space`. Phím lật từ chọn `Ctrl Shift Z` hoặc
`Ctrl Shift X`.

**Lật từ** đổi từ *đang gõ* giữa dạng tiếng Việt và chữ gõ gốc, và bấm lần nữa thì lật lại.
Telex gõ `mas` ra `má`; bấm phím lật thì `má` thành `mas`, bấm tiếp lại thành `má`. Nó chỉ tác
động trước khi từ kết thúc, nên sau Space hay dấu câu thì không còn gì để lật. Lựa chọn giữ
nguyên đến hết từ đó, kể cả khi Space đến. Từ không có dạng tiếng Việt khác chữ đã gõ (`the`)
thì phím này không làm gì.

### Thử nhanh

| Kiểu gõ | Gõ | Ra |
|---|---|---|
| Telex | `tieesng vieejt` | tiếng việt |
| VNI | `xin chao2` | xin chào |

### Chữ gõ thẳng, và khi nào vẫn thấy gạch chân

Mặc định chữ vào thẳng ô nhập: bạn không thấy dòng gạch chân, và đổi cửa sổ giữa chừng thì từ
đang gõ vẫn còn nguyên. Việc này không có công tắc. Funput tự quyết cho từng app, và tự lùi về
kiểu gạch chân khi app không nhận được cách gõ thẳng. Bạn vẫn có thể thấy gạch chân ở các
trường hợp sau:

- App không gửi văn bản quanh con trỏ cho bộ gõ, ví dụ terminal hoặc các app tự vẽ ô nhập.
- WPS Office. Shell Fcitx5 biết trước app này không nhận được nên không thử gõ thẳng.
- Từ đầu tiên sau khi chuyển focus, cho tới khi app gửi văn bản lần đầu. Với IBus, ô trống
  ngay sau một ô trống có thể gạch chân đúng từ đầu tiên.
- App bỏ lệnh xoá đi kèm lúc sửa chữ. Funput nhận ra điều đó sau một từ hỏng, hoặc sớm hơn nếu
  app trả lời văn bản rỗng hai lần liên tiếp, rồi giữ app đó ở kiểu gạch chân cho tới hết lần
  focus. Chuyển sang app khác thì gõ thẳng trở lại ngay.

Nếu một app ra sai chữ ở từ đầu rồi mới chuyển sang gạch chân, đó là cách Funput nhận ra app
không nhận được gõ thẳng. Hãy báo lỗi kèm tên tiến trình của app. Với Fcitx5, thêm một app vào
danh sách `hidden_preedit.cpp` là đủ để Funput bỏ qua bước thử.

Phím lật từ và gõ tắt chạy ở cả hai kiểu. Bỏ dấu lại sau Backspace (`phủ`, Space, Backspace,
`s` ra `phú`) thì chỉ chạy ở kiểu gõ thẳng.

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
- **Editor của Cursor/VS Code bỏ `deleteSurroundingText`.** Nó nhận commit bình thường nhưng bỏ
  lệnh xoá đi kèm, nên `Phu` `1` sẽ ra `Phuú`. Tệ hơn, nó **trả lời surrounding text rỗng** ở mọi
  phím — đủ để bật [chế độ không preedit](#chế-độ-không-preedit) nhưng không đủ để kiểm chứng
  bất cứ lần ghi nào. Đây là phía Chromium/EditContext, Funput không với tới được, nên
  `BlindWrites` đứng dậy sau hai câu trả lời rỗng — trước lần sửa đầu tiên có lệnh xoá. Khung
  chat của Cursor là input DOM thường nên không dính.
- **IBus không biết tên client, nên một client bỏ lệnh xoá bị thử lại ở mỗi lần focus.** Phán
  quyết gắn với tên client mà IBus không có tên nào để đưa, và lấy object path của engine làm tên
  là sai: engine có thể dùng chung cho nhiều app, khi đó một lần từ chối tắt chế độ cho tất cả.
  Client **luôn** trả lời rỗng thì tự an toàn bên IBus — daemon khử trùng lặp `("",0,0)` nên
  chế độ không bao giờ bật với nó. Còn client trả lời tài liệu thật nhưng bỏ lệnh xoá thì mất một
  từ mỗi lần quay lại; chưa có cách đo hay đặt tên đáng tin, và không còn công tắc để tắt.
- **IBus, ô trống ngay sau một ô trống: từ đầu tiên có gạch chân.** Daemon không chuyển payload
  trùng với lần trước, nên engine không biết client đã lên tiếng cho tới khi tài liệu đổi; từ thứ
  hai trở đi thì chữ vào thẳng.
- **Phím điều khiển được phân loại theo ký tự C0, không theo keysym.** Cả Fcitx5 lẫn IBus đều
  ánh xạ Return → CR, Tab → HT, Escape → ESC, nên `classify()` phải loại chúng ra *trước* phép
  thử ranh giới từ — `isBoundary()` cố tình nhận `\t \n \r` vì
  `adoptWordBeforeBackspace()` dò ranh giới trong tài liệu dự đoán, mà gốc là surrounding text
  thật, nơi xuống dòng đúng là ranh giới. Thiếu bước đó thì Enter bị nuốt và CR được nối vào chuỗi
  commit.
- **Bỏ dấu sau Backspace chỉ chạy ở chế độ không preedit.** Ở chế độ preedit, Backspace chỉ rút
  ngắn composition, nên `phủ` ␣ ⌫ `s` ra chữ `s` thường.
- **Ở kiểu gạch chân, một số client làm mất từ đang gõ dở khi đổi focus.** Cả hai shell giao
  việc flush cho framework; client nào bỏ preedit thay vì commit thì mất từ đó. Kiểu gõ thẳng
  không bị lỗi này vì từ đã nằm trong tài liệu từng phím một.
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

Cách gỡ kho `repo.funput.app` nằm ở
[docs.funput.app — Xử lý sự cố](https://docs.funput.app/docs/install/linux/troubleshooting#remove-repo).

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
   ComposePlan ──▶ shell thi hành: Replace (xoá N ký tự rồi commit), preedit hoặc commit
```

`Replace` là đường mặc định: mỗi phím được commit thẳng vào tài liệu rồi sửa lại bằng
`deleteSurroundingText`. Preedit chỉ còn là đường lùi cho client không nhận được cách đó.

Thứ thật sự khác nhau giữa hai shell nằm ở ba chỗ. Thứ nhất là cách đọc văn bản quanh con trỏ
và thi hành `Replace`. Thứ hai là cách gọi tên client để nhớ phán quyết: Fcitx5 có tên tiến
trình và sự kiện focus-in, IBus không có tên nào để đưa. Thứ ba là đường lùi sang preedit, và
**ai commit nó khi mất focus**: Fcitx5 dùng `setClientPreedit` cộng watcher focus-out, IBus
dùng `IBUS_ENGINE_PREEDIT_COMMIT`. Cả hai đều có chú thích tại chỗ.

> [!TIP]
> Shell Fcitx5 có README riêng: [`fcitx5/README.md`](fcitx5/README.md) — panel preedit, allowlist
> client tự giấu preedit, biến môi trường theo session, và các đường dẫn cài đặt của addon.

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
      nonpreedit/backspace.cpp  Ai xoá một ký tự đã nằm trong tài liệu, và mở lại từ đã xong
      nonpreedit/clients.h    Phán quyết đó thuộc về client nào
      nonpreedit/verdict.h    Verdict, Observation trả cho shell, và client không bao giờ cho xem tài liệu
      nonpreedit/selection.h  Hậu tố gợi ý sau caret (Sheets/Excel)
      nonpreedit/document/
        predicted.h             Journal các thay đổi dự đoán: k trên n lần ghi đã xác nhận
        dropped.h               Phép so "lệnh xoá bị bỏ", cho từng mục và cho cả chuỗi mục
        observe.h               Một lần đọc đáng giá gì với chế độ (inSync, onTrack)
  trace/                  Trace chẩn đoán, mặc định tắt — xem "Gửi trace khi báo lỗi"
    trace.h/.cpp            Công tắc, ring buffer và giới hạn kích thước
    sink.h/.cpp             File trace: quyền 0700/0600, ghi qua descriptor, xoay vòng
    events.cpp              Mỗi loại dòng ghi gì
    events/names.h          Tên wire của các enum composer
    summary/text.h          Số ký tự + hash có khoá, thay cho văn bản
    summary/siphash.h       SipHash-2-4, PRF đứng sau hash đó
  settings/               ~/.config/Funput/settings.json
    settings.h              Model mọi shell đều đọc, gồm cả công tắc gõ tắt
    lookup.cpp              File nằm đâu; loại trừ theo app
    io.cpp                  Parse và lưu (JSON)
    io/names.h/.cpp         Tên wire của từng enum (telex, ctrl_space…)
    onsets/letters.h/.cpp   Phụ âm đầu mở rộng z/f/w/j (`extraOnsets`)
    watch.h/.cpp            Watcher inotify để nạp lại tức thì
  ffi/                    C ABI của funput-ffi
    handle.h                Bọc RAII quanh handle của engine
    onsets.h                Bộ chữ phụ âm đầu -> mặt nạ `ONSET_*`
    utf8.h                  Chuyển đổi UTF-8 <-> UTF-32
  tests/                  doctest, cây thư mục soi gương phần trên
fcitx5/src/             Addon Fcitx5 -> libfunput.so
  funput_input.cpp        Một phím: chuẩn hoá, hỏi composer
  funput_client.cpp       Nói chuyện với client, cả hai chiều
  hidden_preedit.cpp      Danh sách client tự giấu preedit (allowlist)
ibus/src/               IBus engine -> ibus-engine-funput
  engine.h                Kiểu GObject công khai
  engine/                 internal.h, object.cpp, callbacks.cpp, focus.cpp, client.cpp
settings-gtk/           App Cài đặt GTK4 + libadwaita (crate cargo riêng,
                        và là gói riêng — xem mục Build)
  src/settings_window/    mỗi trang preferences một submodule
    shortcuts/              công tắc gõ tắt, các hàng, trạng thái rỗng
    typing/extra_onsets/    phụ âm đầu mở rộng: quy tắc bấm, một dòng mỗi chữ, chú thích
  src/convert/            cửa sổ Chuyển mã — xem bên dưới
    ui/casing/              thanh Kiểu chữ: các chip, và hàng "Đang áp"
packaging/              hai file .desktop, metadata kho apt/dnf/pacman
```

Mỗi thư mục giữ tối đa năm file; vượt quá thì tách theo mối quan tâm chứ không theo kích thước.
`src/convert/ui/` hiện **đủ năm file**, nên widget tiếp theo của cửa sổ Chuyển mã sẽ phải mở một
thư mục con chứ không thêm file vào đó.
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

Không file nào dưới `convert/` quyết định một lần chuyển mã ra sao: các file `ui/*` đọc `View`
rồi set property, và `crates/funput-convert` giữ toàn bộ phần còn lại. Ngoại lệ duy nhất là
`ui/casing.rs`, file `ui/*` duy nhất có test — bốn hàm thuần của nó quyết định phần **trình bày**
của thanh Kiểu chữ (chip nào sáng, dòng `Đang áp` đọc ra sao, công tắc nào đang trên màn hình, và
thanh có sống hay không), và cả bốn kiểm được mà không cần màn hình.

Một chốt `refreshing` canh mọi tín hiệu mà một lần refresh bắn ra — thiếu nó, refresh set dropdown
sẽ tái nhập qua `notify::selected` và panic vì `borrow_mut` lồng nhau. `gtk::Switch` của thanh
Kiểu chữ cũng vậy qua `notify::active`, nên hai công tắc đi qua `widget::connect_switch` chứ không
tự nối tay. Nút thì không cần: `clicked` không bao giờ nổ vì một lần ghi property.

### Chế độ không preedit

**Luôn bật**, không còn là một thiết lập: app Cài đặt không có công tắc cho nó và `settings.json`
không còn khoá nào. Thay vào đó mỗi client được quyết định riêng — chế độ bật khi client đã gửi
surrounding text, và **tự lùi về preedit** khi không: client chưa lên tiếng, client nằm trong
danh sách biết là không chạy được, hay client bị bắt quả tang bỏ lệnh xoá. **Cả hai shell đều
thi hành nó.**

Khoá `"nonPreedit"` còn sót trong một `settings.json` cũ bị bỏ qua, kể cả khi là `false`; app Cài
đặt và addon đều xoá nó ở lần lưu kế tiếp. Bỏ công tắc cũng là bỏ lối thoát cho một app mà chế
độ này không chạy được, nên các lưới an toàn bên dưới — phán quyết, `BlindWrites` và danh sách
client đã biết — phải tự lo thay người dùng.

Thay vì đỗ từ đang gõ trong một preedit, mỗi phím commit thẳng vào tài liệu rồi sửa lại thứ phím
trước đã viết — `Effect::Replace` mang theo “xoá N ký tự, rồi commit cái này”. Đó đúng là chỉ thị
mà shell hook trên Windows đã thi hành (`crates/funput-desktop/src/inject.rs`), và N tới thẳng từ
engine dưới dạng `FunputResult::backspace`, nên các nền tảng giữ **một** hành vi chứ không phải
ba. Backspace lùi vào một từ đã xong sẽ mở lại từ đó, nên `phủ` ␣ ⌫ `s` cho ra `phú`. Windows giữ
một bản bóng của thứ nó đã gõ để làm việc này (`CommittedTail` trong `retone.rs`); ở đây cũng vậy:
từ được lấy từ **tài liệu dự đoán** (`nonpreedit/document/predicted.h`, xem bên dưới), không phải
từ bản surrounding text vừa đọc, vì bản đọc đó có thể trễ một phím.

Một Backspace khi không có gì đang soạn sẽ do **chính chúng ta** thi hành, không chuyển cho app,
để việc xoá đi cùng một kênh với mọi sửa chữa khác. Để app tự làm chính là thứ đã làm hỏng việc
bỏ dấu lại trong thanh địa chỉ Chrome: app sửa sau lưng ta rồi vứt bỏ sửa chữa được phát ở phím
kế tiếp. Việc tiếp quản chỉ xảy ra khi **có bằng chứng dương** rằng tài liệu đang đọc là mới —
**mọi** thay đổi đã ghi trong journal đều được thấy là đã xảy ra thật, không chỉ một phần. Không
có bằng chứng đó thì phím đi thẳng qua, và chính điều này giữ cho một vùng bôi đen bằng chuột vẫn
xoá được.

Bằng chứng đó phải sống sót qua cả những thay đổi mà **app tự làm**: dấu cách gõ khi không có gì
đang soạn, và Backspace rút ngắn một từ đang đứng trong tài liệu. Cả hai đều đoán trước được, nên
composer ghi nhận chúng y như một sửa chữa của chính mình (`Composer::appPerforms()`) để lần đọc kế
tiếp xác nhận tài liệu vẫn đang được theo dõi. Nhờ vậy `phủ` ␣ ␣ ⌫ ⌫ `s` vẫn ra `phú`; trước đây
dấu cách thứ hai làm đứt chuỗi bằng chứng và `s` được gõ như chữ thường.

Những bản ghi này được tạo cả khi tài liệu chưa đồng bộ, và chính journal bên dưới làm điều đó an
toàn. Với một bản ghi duy nhất thì không: client trả lời trễ một phím đọc tài liệu như nó đứng
trước phím kế trước, và hai thay đổi giống hệt nhau liên tiếp (␣␣, ⌫⌫) làm bản đọc cũ trùng đúng
với điều bản ghi mới nhất dự đoán — Funput tự thực hiện Backspace và mở lại một từ dài hơn một ký
tự, `phủ` ␣ ␣ ⌫ `s` thành `phủú`. Journal giữ mọi thay đổi chưa xác nhận, nên bản đọc cũ ấy trùng
tiền tố n − 1 chứ không phải n: client *đang theo dõi* nhưng chưa *đồng bộ*, Backspace vẫn là của
app, và từ được mở lại (nếu có) lấy từ tài liệu dự đoán chứ không từ bản đọc. Đổi lại, chính bản
ghi ngoài đồng bộ là thứ cho client trễ bỏ dấu lại được: `phur` ␣ ⌫ `s` ra `phú` ở đó vì Backspace
nhường cho app vẫn vào journal. Chỉ dấu cách thường mới được ghi ở ranh giới từ: Tab thì app thụt
lề hoặc hoàn thành chứ không chèn, còn `(`, `[`, `"` thì editor tự đóng cặp, và lệnh xoá một ký tự
của Funput sẽ bỏ lại dấu đóng mà Backspace của chính app xoá cùng cặp. Những bản ghi này không bao
giờ buộc tội được client qua phép so ba chuỗi: bản ghi không xoá gì thì "đã áp dụng" và "bị bỏ" là
cùng một chuỗi, còn bản ghi không ghi gì thì phép so đã loại khỏi phán quyết từ trước. Riêng bản
ghi có ghi chữ — dấu cách — vẫn được `BlindWrites` đếm như một câu trả lời, y như những chữ cái app
tự gõ xưa nay.

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
thứ tôi mong đợi” là thất bại sẽ tắt chế độ này ở 39% số commit không được trả lời — chữa một
client hỏng bằng cách làm hỏng tính năng cho tất cả.

#### Journal các thay đổi dự đoán

Bảng trên là trường hợp **một lần ghi** của một cơ chế tổng quát hơn. Composer không chỉ nhớ lần
sửa cuối cùng, mà giữ một **journal ngắn** (`PredictedDocument`, tối đa 16 mục; quá ngưỡng thì
journal coi như mất và lần đọc được trả lời kế tiếp bắt đầu lại từ chính nó, không phán quyết) gồm
bản đọc gần nhất mà client **đã trả lời** cùng mọi thay đổi phát ra sau đó — của Funput lẫn của
app. Mỗi lần đọc surrounding text được so với từng **tiền tố** của journal: tài liệu sau 0, 1, … n
thay đổi. Tiền tố **cũ nhất** trùng với bản đọc cho biết **k trên n** lần ghi đã được xác nhận; n − k
mục còn lại được giữ nguyên chờ lần đọc sau. Cũ nhất chứ không phải dài nhất: một bản đọc cũ *có
thể* trông mới hơn thực tế khi các thay đổi triệt tiêu nhau — `dru` rồi ⌫ đọc là `dr`, tức tài liệu
hai bước trước — và coi nó là mới sẽ vứt đi những mục mà client trễ còn chưa báo, sau đó không bản
đọc nào của client ấy còn khớp; giữ mọi mục còn lại thì luôn nhất quán dù client đang ở trạng thái
nào. Với n = 1, tiền tố thứ nhất và thứ hai chính là dòng ba và dòng một của bảng, còn phép so "`D`
rồi `T`" được áp dụng cho từng mục chưa xác nhận (`nonpreedit/document/dropped.h`).

Lý do cần journal là client **trả lời trễ một phím** — cache nội dung của Firefox đa tiến trình
(xem RFC #640, mục 4.4). Bản đọc của client đó không bao giờ bằng "lần sửa cuối đã áp dụng", nên
với một bản ghi duy nhất tài liệu không bao giờ được coi là đồng bộ và `phur` ␣ ⌫ `s` cho ra
`phủs`. Tệ hơn, hai thay đổi giống hệt nhau liên tiếp (`␣␣`, `⌫⌫`) khiến một bản đọc cũ *trông như*
đã xác nhận: ⌫ thứ ba của `gox` ␣ ⌫⌫⌫ bị tiếp quản trên bản đọc trễ, và `tiến` được mở lại trong
khi tài liệu thật chỉ còn `tiế`. Với journal, bản đọc cũ đó trùng tiền tố n − 1 chứ không phải n.

Hai thao tác cần bằng chứng dùng journal với hai ngưỡng khác nhau:

| Thao tác | Điều kiện | Lý do |
|---|---|---|
| Funput tự thi hành Backspace (`backspaceOutsideWord`) | **k = n** (`inSync`) | Xoá là ghi vào tài liệu; bản đọc trễ một phím chính là kịch bản vùng chọn cũ trong thanh địa chỉ Chrome |
| Mở lại từ để bỏ dấu (`adoptWordBeforeBackspace`) | **k ≥ 1** (`onTrack`), Backspace vừa gõ đã vào journal, và lần xác nhận không **mơ hồ** (xem dưới) | Không ghi gì cả; từ được lấy từ **tài liệu dự đoán** (`predicted()`), nơi Backspace vừa gõ đã được tính vào |

k = 0 **không** phải bằng chứng: bản đọc bằng đúng gốc journal là thứ một client im lặng, hay đã
ngừng trả lời, cũng cho ra.

Journal bảo đảm điều gì, và không bảo đảm điều gì. Với client áp dụng mọi thay đổi theo đúng thứ
tự, tài liệu dự đoán chính là tài liệu thật dù trễ bao nhiêu phím — **miễn là không có phím di
chuyển caret xen vào**. Mũi tên, Home/End, Enter, tổ hợp như Ctrl+A, công tắc VI/EN và flush đều
làm composer quên journal (`NonPreeditState::forget`, đúng việc `CommittedTail::clear` làm trên
Windows), và lần đọc được trả lời kế tiếp bắt đầu lại từ chỗ caret thật sự đứng; chỉ phím modifier
(Shift trước một chữ hoa) là không. Trước đây `phur` ␣ ← ⌫ `s` trên client trễ một phím mở lại
`phủ` từ một dự đoán mà caret đã rời khỏi và cho ra `pú␣`. Hai phần dư còn lại được nêu ở cuối mục
này. Trong phạm vi đó, từ được mở lại luôn là hậu tố của những gì thật sự có trước caret, và lần sửa
kế tiếp chỉ xoá trong phạm vi đó. Client bỏ một lệnh xoá thì giữ **nhiều** chữ
hơn dự đoán chứ không ít hơn, nên lần sửa ấy có thể rơi nhầm ký tự nhưng không bao giờ xoá thứ
không tồn tại — và bản đọc sau đó bắt được nó (`RefuseRetone`), kể cả khi client bỏ **một chuỗi**
lệnh xoá: lần ghi kế tiếp đọc ra gốc cộng mọi chữ đã ghi mà không lệnh xoá nào được áp dụng, và
`dropped.h` so cả chuỗi ấy. Chuỗi chỉ cho `RefuseRetone` khi **mọi** lệnh xoá bị buộc tội đều là sửa
chữa sau khi mở lại từ; có một lệnh xoá thường trong đó thì client bỏ cả sửa chữa thường, và đó là
`RefuseMode`. Phép so này không thể buộc tội client trung thực dù trễ: bản đọc của client ấy luôn
là một tiền tố, mà phán quyết chỉ được hỏi khi bản đọc không trùng tiền tố nào — và chỉ trên một
gốc journal đã **tin** (xem dưới).

Một điểm mơ hồ được ghi rõ thay vì giấu đi: một lệnh xoá không kèm chữ bị client bỏ, theo sau là
một lệnh y hệt được áp dụng, sẽ được đọc là lệnh đầu đã áp dụng và lệnh sau còn chờ. Chỉ bản đọc
không phân biệt được hai trường hợp này, và trễ là trường hợp journal sinh ra để phục vụ. Journal
đánh dấu lần xác nhận ấy là **mơ hồ** và không mở lại từ trên nó, vì dự đoán khi đó có thể đi trước
tài liệu một lệnh xoá — `tiếng` ␣ ␣ ⌫⌫⌫ từng mở lại `tiến` khi tài liệu còn `tiếng`; lần ghi kế
tiếp bắt được client đó qua phép so chuỗi. Cái giá: client trễ một phím không mở lại được từ sau
⌫⌫⌫ liên tiếp, còn sau một ⌫ thường thì mục đứng trước nó có ghi chữ và không bao giờ mơ hồ.

**Im lặng không bao giờ dịch journal.** Khi client chưa trả lời kể từ phím trước, bản đọc shell đưa
xuống chỉ là câu trả lời cũ trong cache: composer để nguyên journal, không phán quyết, và coi phím
đó là chưa đồng bộ, chưa theo dõi (bất biến 3 của RFC). Chỉ bản đọc được trả lời mới khớp tiền tố,
bắt đầu lại gốc, hay đưa ra phán quyết.

**Khi nào journal tin gốc của mình, và bảo đảm điều gì.** Bản đọc không trùng tiền tố nào thì
journal bắt đầu lại từ nó nhưng giữ các mục đang chờ ở trạng thái **tạm**: với client trễ, đó là
những thay đổi còn trên đường (khớp từ gốc mới, không bỏ qua mục nào); với client đúng hạn vừa im
lặng vài phím, đó là những thay đổi đã nằm trong bản đọc (khớp khi bỏ qua chúng). Một cách bỏ qua
chỉ được xét khi nó **khả dĩ**: các mục bị bỏ qua, áp dụng theo thứ tự ngay trước gốc, để lại một
hậu tố mà gốc phải kết thúc bằng nó — `m`, `n`, `o` gõ trước một bản đọc `` thì không thể đã được áp
dụng (`nonpreedit/document/match.h`). Trong các cách khớp khả dĩ, cách **ít mục được coi là đã áp
dụng nhất** thắng; hai cách khớp ở hai mức bỏ qua khác nhau kể hai câu chuyện khác nhau về gốc, và
lần khớp đó bị đánh dấu **tranh chấp**. Rà soát độc lập đã thấy quy tắc cũ — bỏ qua nhiều nhất thắng
— xoá nhầm mục rồi buộc tội client trung thực khi các chữ lặp lại (`mnommm⌫mi` trễ 3 phím).

Journal chỉ **tin** gốc của mình (`trusted`, `nonpreedit/document/trust.h`) trong ba trường hợp, và chỉ khi tin mới mở lại từ, tự
thi hành Backspace hay đưa ra phán quyết — chưa tin thì chỉ theo dõi, và bản đọc không trùng tiền
tố nào chỉ làm nó bắt đầu lại, không phán quyết:

1. Sau **đổi focus**, bản đọc đầu tiên là ô như lúc focus tìm thấy nó, nên được tin ngay (không có
   mục nào đang chờ).
2. Sau **phím dời caret**, client trễ sẽ trả lời *trước hết* bằng những trạng thái nó chưa báo —
   đúng bằng số mục còn chờ lúc nhấn phím — rồi trạng thái mà phím đó tạo ra. Journal coi đúng
   chừng ấy bản đọc là **cũ theo cấu trúc**: chúng chỉ bắt đầu lại journal, không khớp gì cả. Không
   có quy tắc này, một client trễ bảy phím phát lại chính những phím nó đã gõ trước mũi tên, và
   phím gõ sau mũi tên trùng với chúng (`grhfip⏎psu⇧dj⌫ ←u⌫dd`) làm journal tin nhầm rồi buộc tội.
3. Qua những bản đọc ấy, một lần khớp **ở mức bỏ qua 0, không tranh chấp, xác nhận một mục có ghi
   chữ** làm gốc thành chính xác với client áp dụng mọi thay đổi theo thứ tự. Xác nhận chỉ gồm lệnh
   xoá thì *giữ* được niềm tin nhưng không *tạo* ra nó: mũi tên trái bớt một ký tự khỏi bản đọc y hệt
   một ⌫ đang chờ (`dwija etfmw⌫o←h u⌫ ⌫uf` trễ 4 phím từng bị buộc tội vì thế). Bắt đầu lại, hay
   một lần khớp tranh chấp, làm mất niềm tin.

Với client trung thực — áp dụng mọi thay đổi theo thứ tự, trễ bao nhiêu phím cũng được, im lặng
từng quãng hay chỉ trả lời khi có thay đổi — hệ quả đo được: **0 phán quyết từ journal** và **0 lần
sửa chữa rơi ra ngoài từ** trên hơn 160 000 lượt chạy ngẫu nhiên (trễ 0–7 phím, ba mô hình trả lời,
có mũi tên trái, Enter, Shift và click có reset), và các test giới hạn trong
`tests/compose/composer/nonpreedit/fuzz/` giữ con số đó. Cái giá: sau một phím dời caret, client trễ
k phím mất khả năng bỏ dấu lại trong k + 2 phím; client im lặng g phím trước đó mất thêm chừng ấy.

Hai phần dư được nêu tên thay vì giấu. Số bản đọc cũ được tính từ độ trễ **đo được trước** phím dời
caret; client trễ *thêm* ngay sau đó nằm ngoài bảo đảm. Và **click mà client không gửi reset**: GTK
và Qt reset input method khi click, Fcitx5 trả lời bằng `flush()` nên journal quên như sau một phím
dời caret; client không làm vậy để lại những mục không bao giờ đặt được vào dòng mới, journal bắt
đầu lại ở mỗi bản đọc — không phán quyết, không mở lại từ — cho tới khi đầy (16 mục) hoặc một phím
tình cờ đánh vần đúng mục cũ. `BlindWrites` chỉ đếm câu trả lời rỗng khi journal **dự đoán có
chữ** và câu trả lời không xác nhận mục nào: người dùng xoá hết chữ rồi client trễ báo lại việc đó
không phải là Cursor, còn một ⌫ đang chờ trên gốc rỗng thì không còn được viện làm lý do — trước
đây nó cho Cursor viết tiếp hơn chục phím (`⌫ tieengs vieejt nam` nát ba từ). Phán quyết `BlindWrites`
với client trung thực trễ từ hai phím, trên ô trống, sau một phím không đổi gì (Shift trước chữ
hoa, mũi tên hay ⌫ ở cột 0) là cái giá đã đo của ngưỡng hai và được ghim trong `blind_tests.cpp`.

Một hậu tố gợi ý **sau** caret — Google Sheets trên Firefox bôi đen phần còn lại của ô khớp —
không phải client bỏ lệnh xoá. `Replace` mang `deleteAfterChars` để gỡ hậu tố đó trước khi sửa
ký tự đang gõ, nên `chư` cộng gợi ý `ứ` vẫn thành `chứ` chứ không còn `chưứ`. Vùng bôi đen của
người dùng (select-all, kéo ngược) vẫn để phím đi thẳng qua.

Vùng bôi đen **trước** caret — nhấp đúp vào từ vừa gõ, kéo chuột qua nó — còn thêm một việc: phím
kế tiếp thay hoặc xoá chính vùng đó, nên từ engine đang giữ không còn nữa. Trước đây engine vẫn
giữ nguyên `tieng`, Backspace chỉ rút nó đi một chữ trong khi ứng dụng xoá cả vùng, và từ gõ sau
thừa kế phần còn lại: `vieejt` ra `énvieejt`, tức "xoá chữ rồi gõ lại tiếng Việt không được" cho
tới khi có một dấu cách. Giờ `Composer::onKey()` bỏ từ trước khi xử lý một phím gõ, dấu cách hay
Backspace gặp vùng bôi đen như thế; còn Backspace trên hậu tố Sheets thì chỉ để ứng dụng xoá
hậu tố, không rút chữ nào khỏi từ.

Còn lại **một client mà phép so ba chuỗi không bao giờ xử được**: client trả lời tài liệu rỗng ở
mọi lần đọc. Xoá N ký tự khỏi chuỗi rỗng vẫn là chuỗi rỗng, nên cả ba dòng của bảng trên gộp làm
một và không lần ghi nào có phán quyết. Đo được ở editor Cursor: nó trả lời surrounding text ở
**mọi** phím — đủ để bật chế độ — rồi trả lời rỗng ở mọi phím, trong khi bỏ **mọi** lệnh xoá.

Câu trả lời là **`BlindWrites`** (`nonpreedit/verdict.h`): client trả lời rỗng hai lần liên tiếp
trong khi đang bị ghi vào thì đó là một phán quyết `RefuseMode`. Hai điểm quyết định tính đúng
của nó:

1. **Đếm câu trả lời, không đếm sự im lặng.** Một lần ghi không được trả lời cũng đọc ra chuỗi
   rỗng y hệt một câu trả lời rỗng — và 39% số commit không bao giờ được trả lời. Vì vậy shell
   phải đưa xuống thêm một bit: *client có lên tiếng kể từ phím trước hay không*
   (`observeDocument(..., answered)`). Bản đầu của quy tắc này đếm ký tự đã ghi bất kể có trả
   lời hay không, và nó **tắt chế độ ngay trong Chrome** — đúng client mà tính năng sinh ra để
   phục vụ. Im lặng là “chưa có phán quyết”; trả lời bằng chuỗi rỗng mới là phán quyết.
2. **Xoá đếm ngay khi đọc được bất cứ thứ gì.** Một ô trống thật cũng trả lời rỗng, nhưng chỉ
   tới khi commit đầu tiên đáp xuống.

Ngưỡng là **hai**: đủ sớm để đứng dậy trước lần sửa đầu tiên có `deleteChars > 0` — thứ duy nhất
làm hỏng chữ — và đủ muộn để một lần đua với commit đang bay không buộc tội được ai.

> [!WARNING]
> Đã thử và đã **revert**: gác ở đầu vào, tức chỉ bật chế độ khi `textBeforeCaret()` khác rỗng.
> Nghe hợp lý, và nó chặn Cursor sạch sẽ — nhưng nó cũng khiến chế độ **không bao giờ bật trong
> Chrome**. Một câu trả lời đáng giá bao nhiêu phải được quyết **sau** một lần ghi, không phải
> trước.

Phán quyết rút lui hẹp đúng bằng phạm vi thất bại. Một sửa chữa bị bỏ **sau khi mở lại một từ**
chỉ khiến mất khả năng bỏ dấu lại: đó là hình dạng duy nhất từng thấy hỏng. Bất kỳ sửa chữa nào
khác bị bỏ thì mất cả chế độ, vì không còn tin được thứ gì nó viết ra.

Phán quyết là một **chốt**, không phải một biến. Shell có thể tái khẳng định chế độ — IBus làm
vậy ở mỗi phím — và không được phép hồi sinh một chế độ mà client đã bị bắt quả tang làm hỏng.
Chốt đó **thuộc về client**, không thuộc về một lần focus: `Composer::onFocusChanged()` nhận tên
client và chỉ mở lại câu hỏi khi đó thật sự là một client khác.

Sự khác biệt ấy không phải chuyện tinh vi. Fcitx5 gọi `activate()` **cả khi capability đổi**,
không riêng khi đổi focus — chú thích trong `deactivate()` nói đúng điều đó — nên một client hay
thay đổi capability sẽ xoá phán quyết chỉ một hai phím sau khi nó được đưa ra. Editor của Cursor
là client như vậy: chế độ tự bật lại rồi phá tiếp từ sau, lặp vô hạn thay vì hỏng đúng một lần.
Bộ nhớ nằm ở `compose/composer/nonpreedit/clients.h`, giới hạn tám client gần nhất. Tên client
là `InputContext::program()` khi nó chỉ đúng một app. **Tên rỗng không bao giờ được nhớ**: shell
IBus không có tên để đưa, và trên GNOME/Wayland mọi client đều trả lời `gnome-shell` — nhớ cái
tên đó là tắt chế độ cho tất cả.

Uuid của input context từng được dùng thế chỗ, như một cái tên "hẹp hơn", và đó là một sai lầm đo
được: `DebugInfo` của Fcitx5 trên GNOME/Wayland ghi `Group [wayland:] has 1 InputContext(s)`
(`program:gnome-shell frontend:ibus`), vì gnome-shell giữ **một** context cho mọi app và chỉ
focus lại nó. Tự kiểm được bằng:

```bash
dbus-send --session --print-reply --dest=org.fcitx.Fcitx5 /controller \
  org.fcitx.Fcitx.Controller1.DebugInfo
```

Một phán quyết ghi dưới uuid đó tắt chế độ cho cả Chrome cho tới khi khởi động lại Fcitx5, và
nhìn từ ngoài là "bị gạch chân rồi không bao giờ trở lại". Giờ `clientId()` trả rỗng, và
`activate()` chỉ gọi `onFocusChanged()` ở **focus-in thật** (`InputContextFocusIn`), không phải ở
mỗi lần capability đổi. Hai việc đó cùng giữ được cả hai điều cần: phán quyết sống sót qua việc
Cursor đổi capability liên tục, và chết cùng lần focus nó được đưa ra.

Cái giá là việc phát hiện chậm một nhịp, nên từ đầu tiên vẫn bị hỏng trước khi chế độ rút lui.
Bắt sớm hơn nghĩa là phải ghi chữ dò vào tài liệu của người dùng, và điều đó không nằm trên bàn.

#### Client đã biết là không chạy được thì không bao giờ được thử

Shell Fcitx5 đã có danh sách client tự giấu preedit (`hidden_preedit.cpp`, hiện là họ WPS), và
`applyNonPreeditMode()` đọc luôn danh sách đó: chờ chế độ bị từ chối là mất một từ nát
(`nguyen64` ra `nguyenênễn`), một cái giá không còn ai gánh khi công tắc đã bỏ. Danh sách chỉ gồm
tên tiến trình, không bao giờ cờ capability hay việc thiếu surrounding text — cả hai nói dối, và
sẽ kéo cả những client đang chạy tốt vào. IBus không biết tên client nên không có đường tương
đương.

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
  kể daemon được khởi động kiểu gì, nên chẩn đoán ở đây phải ghi ra file — đó là
  [trace](#gửi-trace-khi-báo-lỗi) bên dưới. Cùng lý do đó, công tắc của trace nằm trong
  `settings.json` chứ không phải biến môi trường: engine do daemon sinh ra không thừa kế shell
  của bạn.

Việc đếm theo ký tự được xác nhận lần thứ hai từ phía này, không cần suy luận: client báo con trỏ
ở 4 cho `phủ ` (năm byte) và 3 cho `phủ` (bốn byte).

### Gửi trace khi báo lỗi

Những lỗi như [#640](https://github.com/Funput/Funput/issues/640) phụ thuộc vào *thời điểm* client
trả lời surrounding text, nên không tái hiện được trên máy của người sửa, và cũng không thấy được qua
log của framework. Vì vậy cả hai shell có chung một **trace chẩn đoán** (`common/trace/`): ghi những
gì shell thấy ở mỗi phím ra một file, cùng một định dạng cho Fcitx5 lẫn IBus. **Mặc định tắt**, và
khi bật thì **không ghi nội dung gõ** — Funput là bộ gõ ưu tiên quyền riêng tư, và một trace ghi
mọi phím chỉ cách keylogger đúng một thiết lập.

**Bật.** Thêm một khoá ẩn vào `~/.config/Funput/settings.json` (app Cài đặt không có công tắc cho
nó):

```json
{ "trace": true }
```

Watcher `inotify` nạp lại ngay, không cần khởi động lại bộ gõ, và việc bật/tắt không làm rơi từ
đang gõ dở. Đây là công tắc chính, vì engine do `fcitx5`/`ibus-daemon` sinh ra nên biến môi trường
đặt trong shell của bạn không tới được nó. `FUNPUT_TRACE=1` (hoặc `FUNPUT_TRACE=raw`) vẫn được đọc
một lần lúc khởi động cho trường hợp bạn tự chạy engine từ terminal; biến này chỉ bật thêm, không
tắt được thứ file đã bật.

> [!NOTE]
> App Cài đặt ghi đè nguyên `settings.json` từ struct của chính nó (xem
> [Giới hạn đã biết](#giới-hạn-đã-biết)), nên khoá ẩn biến mất ở lần bạn đổi một thiết lập bất kỳ
> — và trace tắt theo, đúng như thiết kế: một công tắc không ai nhìn thấy không được tự giữ mình
> bật. Thêm lại khoá sau khi đổi thiết lập nếu còn cần trace.

**File.** `$XDG_CACHE_HOME/funput/trace.jsonl`, thường là `~/.cache/funput/trace.jsonl`, mỗi dòng
một object JSON. Thư mục `funput` được tạo với quyền `0700` và file với quyền `0600` ngay từ lúc tạo
(không có khoảng hở chmod sau đó); file do bản cũ để lại cũng được siết về `0600` khi mở. Kích thước
có giới hạn: shell giữ ~2000 dòng gần nhất trong bộ nhớ, và khi file vượt 1 MiB thì cắt rồi ghi lại
đúng phần đó, nên file luôn chứa đoạn cuối của phiên và không bao giờ phình to; một dòng đơn lẻ vượt
cả giới hạn (một `reading` raw của tài liệu rất dài) được thay bằng dòng `note` nói nó đã bị bỏ. Khi
engine khởi động lại mà file đã quá nửa giới hạn, file được xoay sang `trace.jsonl.1` (một thế hệ,
cũng `0600`) rồi mới ghi tiếp — để trace của phiên vừa treo không bị phiên mới ghi đè. Mỗi dòng được
ghi thẳng qua `write(2)`, vì phiên đáng trace nhất có thể là phiên treo.

**Nội dung mặc định.** Mỗi dòng có `t` (mili-giây theo đồng hồ đơn điệu kể từ dòng `start` gần nhất
phía trên) và `ev`:

| `ev` | Nghĩa |
|---|---|
| `start` | Shell (`fcitx5`/`ibus`), giờ thực, pid, và trace có đang ở chế độ raw không |
| `focus` | Client vừa nhận focus, theo tên framework báo (`gnome-shell` trên GNOME/Wayland; IBus chưa có tên để báo) |
| `key` | Loại phím sau khi phân loại (`Compose`, `Boundary`, `Backspace`, `Shortcut`…). Keysym và modifier chỉ ghi cho phím mà *danh tính* là hành vi cần chẩn đoán — Backspace, mũi tên, tổ hợp như Ctrl+A — không ghi cho phím sinh ký tự (Shift trên một chữ cái là chữ hoa, tức là nội dung) |
| `reading` | Văn bản trước caret mà shell đọc được, client có trả lời kể từ phím trước không, có vùng chọn không, và kết luận của composer: `verdict`, `inSync`, cùng `acked`/`pending` — số thay đổi trong journal mà bản đọc xác nhận trên tổng số đang chờ |
| `plan` | Composer yêu cầu shell làm gì: `effect`, `consumed`, và với `Replace` là `deleteChars`/`deleteAfterChars` |
| `mode` | Gõ thẳng vừa bật hay tắt, và vì sao (`surrounding`, `silent`, `refused`, `hidden-preedit`) |
| `adopt` | Backspace có mở lại được từ đứng trước nó không |
| `note` | `reset`, `focus-out`, đổi VI/EN |

Mọi chuỗi — văn bản quanh caret, chữ được commit — chỉ xuất hiện dưới dạng `{"chars": N, "hash":
"…"}`: số ký tự (không phải byte, để đọc thẳng với `deleteChars`) và một hash **SipHash-2-4** 64 bit
dưới một khoá 128 bit ngẫu nhiên theo từng tiến trình. Khoá chỉ nằm trong bộ nhớ của engine, không
bao giờ được ghi; SipHash là một PRF, nên không có khoá thì không hash nào nói gì về chuỗi của nó —
không dò được một từ ngắn bằng từ điển, và cũng không suy ra khoá từ bất kỳ số cặp (chuỗi, hash) nào
đã biết (một hash FNV có salt, như bản đầu, thì làm được cả hai). Chuỗi rỗng chỉ có `chars: 0`,
không hash. Trong cùng một trace, hai chuỗi bằng nhau vẫn cho cùng hash — đủ để thấy hai lần đọc là
cùng một tài liệu, hay một lệnh xoá đã đi quá một ký tự. Ký tự của phím chữ cũng là nội dung, nên
không có trong file.

Những gì file **có** ghi nguyên văn, kể cả ở chế độ mặc định, để bạn biết trước khi đăng lên issue:

- Tên client ở dòng `focus`, đúng như framework báo — `gnome-shell`, `firefox`, hay
  `org.keepassxc.KeePassXC`. Đây là thông tin thiết yếu để biết client nào trục trặc, nhưng cũng cho
  thấy bạn đã dùng ứng dụng nào trong lúc trace bật.
- Keysym và modifier của phím không sinh ký tự (Backspace, mũi tên, Ctrl+A), loại của mọi phím, số
  ký tự của mọi chuỗi, và giờ thực ở dòng `start`.

Ví dụ `phur` ␣ ⌫ `s` → `phú`, ở chế độ mặc định (lấy từ một lần chạy thật; chỉ bỏ dòng `focus`):

```jsonl
{"t":0,"ev":"start","shell":"fcitx5","wall":"2026-11-03T14:56:54Z","pid":22965,"raw":false}
{"t":0,"ev":"key","kind":"Boundary"}
{"t":0,"ev":"reading","before":{"chars":3,"hash":"d408edacde3f07da"},"answered":true,"selection":false,"selectedAfter":0,"verdict":"Unknown","inSync":true,"acked":1,"pending":1}
{"t":0,"ev":"plan","effect":"None","consumed":false}
{"t":0,"ev":"key","kind":"Backspace","keysym":"ff08"}
{"t":0,"ev":"reading","before":{"chars":4,"hash":"ca8b0ce8d5a34fa8"},"answered":true,"selection":false,"selectedAfter":0,"verdict":"Unknown","inSync":true,"acked":1,"pending":1}
{"t":0,"ev":"plan","effect":"Replace","consumed":true,"text":{"chars":0},"deleteChars":1,"deleteAfterChars":0}
{"t":0,"ev":"adopt","taken":true}
{"t":0,"ev":"key","kind":"Compose"}
{"t":0,"ev":"reading","before":{"chars":3,"hash":"d408edacde3f07da"},"answered":true,"selection":false,"selectedAfter":0,"verdict":"Unknown","inSync":true,"acked":1,"pending":1}
{"t":0,"ev":"plan","effect":"Replace","consumed":true,"text":{"chars":1,"hash":"51fe10172f2a4f08"},"deleteChars":1,"deleteAfterChars":0}
```

Hai dòng `reading` có cùng hash `d408edacde3f07da` là cùng một tài liệu (`phủ`), dù không dòng nào nói
ra chữ đó.

**Chế độ raw.** Khi tóm tắt không đủ — cần biết client trả về *chuỗi gì* — thêm:

```json
{ "trace": true, "traceRaw": true }
```

Từ dòng `start` kế tiếp (`"raw": true`), mỗi tóm tắt mang thêm trường `text`, và dòng `key` của
mọi phím mang thêm `keysym`, modifier cùng ký tự.

> [!WARNING]
> Với `traceRaw`, file chứa **mọi thứ bạn gõ** trong lúc bật — kể cả mật khẩu — và cả văn bản quanh
> caret mà client gửi về. Chỉ bật trong vài phút để tái hiện đúng lỗi, tắt ngay, và **đọc lại file
> trước khi gửi cho bất kỳ ai**.

**Kèm trace, chạy ba phép thử** ở [RFC §4.4](../../docs/features/linux-direct-typing-robustness.md#44-nguyên-nhân-c--firefox-xoá-theo-cache-của-chính-nó-giả-thuyết)
để tách phía cầu nối (mutter/gnome-shell) khỏi phía Firefox:

1. Chạy Firefox trong phiên **X11**, hoặc với `GTK_IM_MODULE=ibus` để đi qua module IBus của GTK
   thay vì mutter. Lỗi còn thì thủ phạm là Firefox, không phải cầu nối.
2. Chạy `MOZ_LOG=IMEHandler:5 firefox` và tìm `DeleteText(), FAILED` trong log, cạnh dòng `plan`
   có `deleteChars` tương ứng trong trace.
3. Với Firefox snap/flatpak, xác nhận module IM nào thực sự được dùng trong sandbox.

Gửi file trace, log Firefox nếu có, và tên phiên (Wayland/X11, phiên bản GNOME) vào issue.

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
