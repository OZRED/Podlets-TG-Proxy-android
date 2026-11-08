# Podlets TG Proxy

<p align="center">
  <b>Универсальный сетевой центр и менеджер прокси для Telegram на Android</b><br>
  <b>All-in-One Telegram Proxy Client & Live MTProto Scanner for Android</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose">
  <img src="https://img.shields.io/badge/Core-Rust_JNA-DEA584?logo=rust&logoColor=white" alt="Rust Core">
  <img src="https://img.shields.io/badge/Status-Stable_v1.0.1-success" alt="Version">
</p>

---

[Русский](#русская-версия) | [English](#english)

---

<a name="русская-версия"></a>
## Русская версия

### Описание
**Podlets TG Proxy** — современное многофункциональное Android-приложение для обеспечения бесперебойного и стабильного доступа к Telegram в условиях сетевых ограничений и блокировок.

Приложение объединяет два мощных инструмента в едином интерфейсе Material 3:
1. **Локальный WebSocket / MTProto сервер**: Высокоскоростное ядро на Rust, поднимающее защищённый прокси-сервер прямо на устройстве (localhost `127.0.0.1`), маршрутизируя трафик Telegram через Cloudflare Workers/Pages или туннелирование FakeTLS.
2. **Умный сканер и чекер публичных MTProto-прокси**: Модуль автоматического сбора прокси из открытых каналов и лент TGStat с измерением реального пинга криптографического рукопожатия и подключением в Telegram в один клик.

---

### Ключевые возможности

* **Двухкомпонентная архитектура**:
  * Локальный прокси-сервер для постоянного личного туннеля с выбором портов и пула соединений.
  * Встроенный радар публичных прокси для быстрого поиска резервных серверов без ручного поиска по каналам.
* **Двухфазная проверка пинга и умный сканер**:
  * *Фаза 1*: Быстрый параллельный TCP-тест для моментального выявления доступных хостов.
  * *Фаза 2*: Полное MTProto рукопожатие с замером истинного времени отклика сервера.
  * *Проверка своих прокси*: Вставка и проверка любого списка прокси (текст или ссылки `tg://proxy?...`, `https://t.me/proxy?...`).
  * *Экспорт всех прокси*: Копирование всех проверенных рабочих прокси в буфер обмена в один клик.
  * *Пакетное открытие в Telegram*: Открытие Топ-5 лучших серверов стопкой карточек на экране Telegram или отправка всего списка проверенных прокси в чат/«Избранное».
  * *Автономный кэш*: Найденные серверы сохраняются на устройстве (`cached_proxies.json`) и доступны сразу при перезапуске.
* **Глубокая интеграция с Android**:
  * **Foreground Service**: Надежная фоновая работа службы прокси с быстрыми действиями в шторке («Подключить прокси», «Перезапустить», «Отключить»).
  * **Плитка в шторке (Quick Settings)**: Включение и отключение прокси прямо из шторки быстрых настроек Android.
  * **Автозапуск**: Автоматический запуск локального прокси при перезагрузке смартфона.
* **Журнал событий реального времени**:
  * Встроенный терминал логов процессов (открывается по кнопке «Логи» в шапке в виде шторки) с фильтрацией (Инфо / Ошибки / Предупреждения), очисткой и копированием отчёта.
* **Интерфейс нового поколения Material You**:
  * Поддержка динамических цветов Android 12+ (подстраивается под обои экрана).
  * Тёмная, светлая и системная темы, выбор акцентных палитр оформления.
  * Сохранение состояния: запоминание активной вкладки и режима сортировки между запусками приложения.

---

### Обзор экранов

| Экран | Назначение |
| :--- | :--- |
| **Локальный прокси** | Запуск/остановка сервера, монитор скорости и активных сессий, кнопка «Применить в Telegram» (`tg://proxy?...`), смена режима обхода, а также кнопка вызова терминала логов (шторка). |
| **Поиск прокси** | Список проверенных публичных серверов, цветовая шкала задержки (зелёный/жёлтый/красный), сортировка по задержке или реакциям, подключение в 1 клик, проверка своих прокси и экспорт списка. |
| **Настройки** | Настройка портов, секретов, доменов FakeTLS/Cloudflare, управление списком источников лент прокси (с кнопкой восстановления по умолчанию) и оформление. |
| **О проекте** | Официальные ссылки репозитория, баг-трекер, документация архитектуры, проверка обновлений и поддержка автора. |

---

### Загрузка и установка

1. Перейдите в раздел **[Releases](https://github.com/OZRED/Podlets-TG-Proxy-android/releases)**.
2. Скачайте APK под архитектуру вашего устройства:
   * `app-arm64-release.apk` — для большинства современных смартфонов (рекомендуется).
   * `app-universal-release.apk` — универсальная сборка под все архитектуры.
   * `app-arm32-release.apk` — для старых 32-битных устройств.
3. Установите APK на смартфон и запустите **Podlets TG Proxy**.

---

### Сборка проекта

```bash
# Клонирование репозитория
git clone https://github.com/OZRED/Podlets-TG-Proxy-android.git
cd Podlets-TG-Proxy-android

# Сборка Debug APK под ARM64
./gradlew.bat assembleArm64Debug

# Или сборка всех релизных APK скриптом (Windows)
build_apk.bat
```

---

### Безопасность и приватность
* **100% открытый исходный код**: Никаких скрытых трекеров, телеметрии или рекламных SDK.
* **Локальная работа**: Все сетевые проверки и локальный сервер функционируют исключительно на вашем устройстве.
* **Прямое соединение**: Приложение не собирает ваши учётные данные и передаёт трафик строго по защищённому протоколу MTProto.

---

### Авторство и лицензия
* **Разработчик**: [@OZRED](https://github.com/OZRED)
* **Репозиторий**: [Podlets-TG-Proxy-android](https://github.com/OZRED/Podlets-TG-Proxy-android)
* Распространяется под свободной лицензией **GPL-3.0**.

---

<a name="english"></a>
## English

### Overview
**Podlets TG Proxy** is a high-performance, modern Android application engineered to provide seamless, reliable connectivity to Telegram under any network restrictions. 

It uniquely combines two complementary proxy technologies into a unified, intuitive Material 3 interface:
1. **Local WebSocket / MTProto Proxy Server**: A resident high-performance Rust service running directly on your Android device (localhost `127.0.0.1`), routing Telegram traffic through Cloudflare Workers/Pages or FakeTLS evasion tunnels.
2. **Public MTProto Proxy Scanner & Ping Tester**: An intelligent live aggregator that discovers public MTProto proxies from verified channels, measures real handshake latencies in real time, and connects them with a single tap.

---

### Key Features

* **Dual-Engine Architecture**:
  * **Local Host Server**: Run your personal stealth proxy locally on Android with custom ports, DC routing, and pool management.
  * **Public Proxy Radar**: Discover, check, and sort live MTProto proxies from customizable community feeds.
* **Accurate 2-Phase Health Checking & Scanner**:
  * *Phase 1*: High-concurrency TCP socket ping for near-instant availability indication.
  * *Phase 2*: Full MTProto cryptographic handshake for genuine server responsiveness measurement.
  * *Custom Proxy Validation*: Input or paste arbitrary text containing `tg://proxy?...` or `https://t.me/proxy?...` to verify your own proxies.
  * *Bulk Export*: 1-tap top-bar action to copy all active proxies formatted as a `tg://proxy` list directly to clipboard.
  * *Bulk Open in Telegram*: 1-tap action to either open Top 5 fastest proxies stacked as native cards in Telegram, or send the full verified list directly to Telegram chat / Saved Messages.
  * *Persistent Cache*: Discovered proxies are cached on-device (`cached_proxies.json`) so they remain available instantly across app restarts.
* **Deep Android System Integration**:
  * **Foreground Service**: Keeps the local proxy active reliably in the background with rich status notification actions (Connect proxy in Telegram, Restart, Disconnect).
  * **Quick Settings Tile**: Toggle proxy on/off directly from the Android quick settings notification shade.
  * **Boot Autostart**: Optional launch upon device reboot (`RECEIVE_BOOT_COMPLETED`).
* **Real-time Diagnostics & Logs**:
  * Integrated in-app terminal (hosted as an on-demand modal bottom sheet in Local Proxy) capturing real-time logcat events across both Rust daemon and Kotlin network scanners with level filters and export.
* **Material You (Material 3) UI**:
  * Dynamic colors matching your system wallpaper on Android 12+.
  * Dark, Light, and System themes with customizable accent palettes (Indigo, Emerald, Crimson, Amber, and more).
  * State persistence: Active tab and search sorting preferences are remembered across app launches.

---

### App Tabs Overview

| Tab | Functionality |
| :--- | :--- |
| **Local Proxy** | One-tap server start/stop, real-time bandwidth & active sessions monitor, direct "Apply in Telegram" button (`tg://proxy?...`), profile configuration, and top-bar "Logs" button opening a full real-time terminal sheet. |
| **Proxy Scanner** | Live list of tested MTProto servers, color-coded latency badges (green / orange / red), sorting by ping or rating, 1-tap connect, single/bulk copy, and custom proxy verification. |
| **Settings** | Local proxy port & secret keys, custom Cloudflare & FakeTLS domains, custom feed URLs management (with 1-tap default restore), and themes. |
| **About** | Official repository links, issue reporting, architecture documentation, developer support, and in-app update checker. |

---

### Download & Installation

1. Navigate to the **[Releases](https://github.com/OZRED/Podlets-TG-Proxy-android/releases)** page.
2. Download the APK corresponding to your device architecture:
   * `app-arm64-release.apk` — for most modern 64-bit Android smartphones (recommended).
   * `app-arm32-release.apk` — for older 32-bit devices.
   * `app-universal-release.apk` — universal build for any supported architecture.
3. Install the APK on your device and launch **Podlets TG Proxy**.

---

### Building from Source

#### Prerequisites
* **JDK 17** or newer installed and configured in `JAVA_HOME`.
* **Android SDK** with Platform 35 and Build Tools installed.
* *(Optional, for rebuilding Rust core)* **Rust toolchain** (`cargo`) with Android NDK targets. Pre-compiled native libraries (`libtgwsproxy.so`) are already bundled in `app/src/main/jniLibs/`.

#### Build Commands
```bash
# Clone the repository
git clone https://github.com/OZRED/Podlets-TG-Proxy-android.git
cd Podlets-TG-Proxy-android

# Build Debug APK
./gradlew assembleArm64Debug

# Build Release APKs via batch script (Windows)
build_apk.bat
```

Output APK files are located at:
`app/build/outputs/apk/`

---

### Security & Privacy
* **100% Open Source**: No tracking SDKs, analytical bloatware, or hidden network telemetry.
* **Local Operation**: All proxy discovery, ping benchmarks, and server routing run on-device.
* **Direct Connection**: No intermediary servers inspect or collect your Telegram credentials.

---

### Authors & License
* **Developer**: [@OZRED](https://github.com/OZRED)
* **Repository**: [Podlets-TG-Proxy-android](https://github.com/OZRED/Podlets-TG-Proxy-android)
* Licensed under **GPL-3.0**.
