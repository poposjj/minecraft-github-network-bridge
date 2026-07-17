<a id="top"></a>

<div align="center">

# ⛏️ Minecraft GitHub Network Bridge 🧱

**让我的世界 Minecraft 模组只通过指定订阅线路连接 GitHub** 🎮🌐💎

<a href="#lang-zh">🇨🇳 <b>简体中文</b></a> ·
<a href="#lang-en">🇬🇧 <b>English</b></a> ·
<a href="#lang-ru">🇷🇺 <b>Русский</b></a>

</div>

---

<a id="lang-zh"></a>

# 🇨🇳 简体中文 ⛏️🧱

> 🆕 **项目类型：全新编写 / 原创实现**<br>
> 🎯 **作用范围：只处理我的世界 Minecraft 进程中的 GitHub 域名**<br>
> 🔐 **私人订阅不会进入 JAR、源码、日志或公开仓库**

## 🚀 先用起来：五步完成配置

1. 📥 下载 `minecraft-github-network-bridge-1.21.11-1.0.1.jar`。
2. 📂 把 JAR 放进当前我的世界 Minecraft 实例的 `mods` 文件夹。
3. ▶️ 启动游戏，打开 **选项 → 我的世界 GitHub**。
4. ⚙️ 进入 **设置 → 快速订阅**，粘贴 Clash/Mihomo 订阅 URL，然后点击底部的 **获取/刷新线路**。
5. 🌐 打开 **代理组**，选择一条线路；需要测速时点击右上角的 `↻` 小方形按钮。

看到绿色 `HTTP 200` 后，这个 Minecraft 实例中的兼容模组就可以通过所选线路访问 GitHub。✅💎

## 📦 安装要求

- 🎮 当前首版：Minecraft `1.21.11`
- 🧵 Fabric Loader `0.19.3+`
- 🧰 Fabric API `0.141.5+1.21.11+`
- ☕ Java `21+`
- 🪟 Windows x64
- 🧩 Mod Menu `17.0.0+` 可选

项目按多版本方式维护，不把未来发行限定在 Minecraft `1.21.11`；不同 Minecraft 版本会提供各自的构建 JAR。🧱🔨

## 🖥️ 每个页面和按键的详细说明

### ⚙️ 顶层页签：设置

`设置`收纳订阅创建和运行参数，内部有三个页面。

#### 🔗 快速订阅

- **订阅链接输入框**：粘贴 Clash/Mihomo YAML 订阅 URL。
- **用途**：使用默认名称、超时、更新周期和安全参数快速创建配置。
- **保存结果**：新的 URL 会清空旧线路选择，获取新订阅后重新选线。
- **获取/刷新线路**：替代原来的“测试”按钮；保存 URL、下载订阅、刷新列表，然后自动进入代理组。

#### 🧱 新建配置

- **名称**：给这份我的世界 Minecraft GitHub 配置起一个容易识别的名字。
- **描述**：记录用途，不参与网络连接。
- **订阅链接**：Clash/Mihomo YAML 订阅 URL。
- **User Agent**：订阅服务器识别下载客户端时使用；一般保留 `clash.meta`。

#### 🎛️ 常规设置

- **GitHub：开/关**：总开关。关闭后 Mod 停止内置 Mihomo，并把网络控制交回原来的 Minecraft 网络路径。
- **自动更新：开/关**：按设定分钟数更新订阅；失败时保留上一次可用缓存。
- **借用网络工具：开/关**：仅在下载订阅失败时尝试当前电脑上的本机网络工具。它不会让游戏流量长期依赖 Clash。
- **允许无效证书：开/关**：仅用于证书异常的私人订阅服务器，默认关闭。
- **请求超时**：下载订阅和测试连接的最长等待秒数。
- **更新间隔**：自动更新订阅的分钟数。

### 🌐 顶层页签：代理组

这里就是 Clash Verge 中“代理组/节点选择”的对应页面。导入订阅后，所有直接节点会列在这里。🛰️

- **`↻` 正方形按钮**：刷新所有节点的 GitHub 实际延迟。它会短暂启动隔离的 Mihomo 测试进程，逐节点访问 GitHub，结束后立即关闭。
- **未点击 `↻`**：不显示任何延迟。
- **点击 `↻` 后**：可用线路显示 `[xx ms]`，失败显示 `[--]`。
- **延迟缓存**：只保存在当前设置界面的内存中；关闭界面或重进游戏后不保留。
- **线路按钮**：点击线路即选中，前面显示 `[*]`；选择会写入私有配置，下次启动继续使用。
- **`<` / `>`**：订阅节点很多时翻到上一页或下一页。

### 🧪 底部公共按键

- **获取/刷新线路**：只在快速订阅页显示，保存订阅并刷新代理组。
- **测试**：在新建配置、常规设置和代理组页面显示；保存当前设置，等待内置连接准备完成，然后请求 GitHub API。`HTTP 200` 表示可用。
- **取消**：返回上一个页面，不主动保存当前输入框中尚未提交的修改。
- **完成**：保存设置并返回；后台会根据变化重建 GitHub-only 配置。

## 🎯 它代理什么，不代理什么

### ✅ 会代理

- 当前 Minecraft Java 进程中遵循 JVM `ProxySelector` 的 GitHub HTTP/HTTPS 请求。
- `github.com`、`api.github.com`、`raw.githubusercontent.com`、Release 下载等配置中的 GitHub 域名及其子域名。

### 🚫 不会代理

- Hypixel、Mojang、Microsoft 登录及其他非 GitHub 域名。
- 浏览器、PCL、Git、GitHub CLI、Discord 或 Windows 其他程序。
- Windows 系统代理、虚拟网卡设置和环境变量。

因此打开系统代理、Clash Verge 规则/全局/直连模式或虚拟网卡模式时，Mod 仍只决定 Minecraft 内 GitHub 域名的 JVM 路径。订阅首次下载可以临时借用当前网络工具；下载成功后，GitHub 流量使用 JAR 内置 Mihomo，关闭 Clash 仍可继续使用缓存。若 Clash 或其他程序占用了首选端口，Mod 会自动选择并保存另一个空闲本地端口，不需要关闭 Clash。🛡️🎮

## 📁 自动创建的文件

首次运行后自动出现，不需要安装 Sidecar 或其他组件：

```text
config/minecraft-github-network-bridge.properties
config/minecraft-github-network-bridge/runtime/mihomo.exe
config/minecraft-github-network-bridge/runtime/subscription-source.yaml
config/minecraft-github-network-bridge/runtime/minecraft-github-only.yaml
```

- 🔐 `properties`：私人设置、订阅 URL、当前线路。
- 📦 `mihomo.exe`：从同一个 Mod JAR 自动释放的内置运行程序。
- 🔗 `subscription-source.yaml`：私人订阅缓存。
- 🎯 `minecraft-github-only.yaml`：只监听 `127.0.0.1`、关闭 TUN/局域网/DNS 监听的运行配置。

## ⌨️ 所有游戏指令

| 指令 | 详细作用 |
| --- | --- |
| `/githubbridge` | 显示当前开关、配置、所选线路和内置连接状态。 |
| `/githubbridge status` | 与根指令相同，只查看状态，不修改设置。 |
| `/githubbridge config` | 打开 **设置** 页面。 |
| `/githubbridge proxies` | 直接打开 **代理组** 页面。 |
| `/githubbridge test` | 请求 GitHub API 并返回 HTTP 状态与耗时。 |
| `/githubbridge reload` | 重新读取私人 properties，并更新订阅。 |
| `/githubbridge refresh` | 重新下载订阅、重建线路和 GitHub-only 配置。 |
| `/githubbridge set enabled on` | 开启我的世界 Minecraft GitHub 网络桥接。 |
| `/githubbridge set enabled off` | 关闭桥接并停止 Mod 自己启动的 Mihomo。 |
| `/githubbridge set subscription <URL>` | 保存新的订阅 URL，并开始获取线路。 |
| `/githubbridge set proxy <线路名>` | 从当前订阅中选择指定线路；名称包含空格时直接完整输入。 |

## 🔐 私密信息规则

- 🚫 不要把真实订阅 URL 写入 Issue、README 或公开配置样例。
- 🚫 不要提交 `config/`、运行时 YAML、Token 或节点信息。
- ✅ 发布 JAR 只包含通用代码与官方 Mihomo 二进制。
- ✅ 日志不打印订阅 URL、节点密码或完整 YAML。
- ✅ Mod 退出时关闭自己启动的 Mihomo，释放本地端口。

## 🧰 配置文件字段

公开样例位于 `config/minecraft-github-network-bridge.example.properties`。主要字段：

| 字段 | 作用 |
| --- | --- |
| `enabled` | 总开关。 |
| `profileName` / `profileDescription` | 配置名称与描述。 |
| `subscriptionUrl` | 私人 Clash/Mihomo YAML 订阅。 |
| `selectedProxyName` | 代理组当前选中的线路。 |
| `subscriptionUserAgent` | 下载订阅时的客户端标识。 |
| `subscriptionTimeoutSeconds` | 订阅请求超时。 |
| `subscriptionUpdateMinutes` | 自动更新间隔。 |
| `subscriptionUseCurrentProxy` | 直连失败后是否借用本机网络工具下载订阅。 |
| `subscriptionAllowInsecure` | 是否允许无效 TLS 证书。 |
| `subscriptionAutoUpdate` | 是否自动更新。 |
| `localProxyPort` | 内置 GitHub-only 本机端口，默认 `17897`。 |
| `githubDomains` | 允许进入代理的 GitHub 域名表。 |

## ✅ 已验证

- 🧪 11 项自动化测试，包括真实私人订阅解析、手动选线排序、GitHub URL Test 延迟与 Clash 端口占用回归。
- 🎮 Minecraft `1.21.11` + Fabric `0.19.3` 实机加载通过。
- 🌐 所选线路访问 GitHub API 返回 `HTTP 200`。
- 🎯 Hypixel 与非 GitHub 域名保持 `DIRECT`。
- 🧹 游戏退出后 `17897` 和 Mod 拥有的 Mihomo 均停止。
- 🖥️ 游戏选项页重复进入只保留一个“我的世界 GitHub”按钮。

## 🔨 从源码构建

```powershell
./gradlew clean test build
```

输出位于 `build/libs/`。项目采用 `GPL-3.0-only`；内置 Mihomo `v1.19.28` 的来源与许可证见 `THIRD_PARTY_NOTICES.md` 和 `LICENSES/`。⚖️

[⬆️ 返回语言按钮](#top)

---

<a id="lang-en"></a>

# 🇬🇧 English ⛏️🧱

> 🆕 **Type: original implementation**<br>
> 🎯 **Scope: GitHub domains inside the Minecraft process only**<br>
> 🔐 **Private subscriptions are never embedded in releases or the repository**

## 🚀 Quick start

1. 📥 Download `minecraft-github-network-bridge-1.21.11-1.0.1.jar`.
2. 📂 Put it in the Minecraft instance's `mods` folder.
3. ▶️ Open **Options → Minecraft GitHub**.
4. ⚙️ Open **Settings → Quick Setup**, paste a Clash/Mihomo YAML subscription URL, then click **Get / Refresh Proxies** in the footer.
5. 🌐 Open **Proxy Group** and choose a proxy. Use the small `↻` button to measure GitHub latency when needed.

Green `HTTP 200` means compatible Minecraft mods can reach GitHub through the selected proxy. ✅

## 📦 Requirements

Minecraft `1.21.11`, Fabric Loader `0.19.3+`, Fabric API `0.141.5+1.21.11+`, Java `21+`, and Windows x64. Mod Menu `17.0.0+` is optional. Future releases are planned as separate builds for multiple Minecraft versions. 🎮

## 🖥️ Every page and control

### ⚙️ Settings tab

- **Quick Setup → Subscription URL**: creates a configuration with practical defaults. A new URL clears the old proxy selection.
- **Quick Setup → Get / Refresh Proxies**: replaces the old Test action, saves the URL, downloads the subscription, and opens the refreshed Proxy Group.
- **New Config → Name**: friendly configuration name.
- **New Config → Description**: local note; it does not affect networking.
- **New Config → Subscription URL**: private Clash/Mihomo YAML URL.
- **New Config → User Agent**: subscription download identity; normally keep `clash.meta`.
- **General → GitHub**: master on/off switch.
- **General → Auto update**: periodically refreshes the subscription and keeps the last working cache on failure.
- **General → Network tool**: after a direct download failure, temporarily uses a detected local network tool only to fetch the subscription.
- **General → Invalid certificates**: accepts invalid subscription TLS certificates; off by default.
- **Request timeout / Update interval**: request seconds and refresh minutes.

### 🌐 Proxy Group tab

- **Square `↻` button**: runs a real GitHub URL Test through every node using a temporary isolated Mihomo process.
- **Before `↻` is clicked**: no latency is displayed.
- **After `↻` is clicked**: `[xx ms]` is working, `[--]` failed.
- **Latency cache**: memory only; closing the screen or restarting Minecraft discards it.
- **Proxy row**: selects and persists that proxy; `[*]` marks the current selection.
- **`<` / `>`**: previous and next page for large subscriptions.

### 🧪 Common bottom buttons

- **Get / Refresh Proxies**: appears on Quick Setup and refreshes the subscription and Proxy Group.
- **Test**: appears on New Config, General, and Proxy Group; it saves current values and requests the GitHub API.
- **Cancel**: returns without intentionally saving unfinished input.
- **Done**: saves and rebuilds the private GitHub-only profile when required.

## 🎯 Network scope

GitHub domains used by compatible mods go through one loopback port, normally `127.0.0.1:17897`. If Clash already owns that port, the mod automatically selects and saves another free port. Hypixel, Mojang, Microsoft login, and every non-GitHub host stay direct. The mod does not change Windows proxy settings, TUN, environment variables, browsers, PCL, Git, or GitHub CLI. 🛡️

The JAR contains Mihomo. No separate Sidecar installation is needed. The runtime and private cache appear automatically under `config/minecraft-github-network-bridge/`. Closing Minecraft stops the owned process and releases the port. 🧹

## ⌨️ Commands

| Command | Meaning |
| --- | --- |
| `/githubbridge` or `status` | Show enabled state, profile, selected proxy, endpoint, and built-in runtime state. |
| `/githubbridge config` | Open Settings. |
| `/githubbridge proxies` | Open Proxy Group directly. |
| `/githubbridge test` | Test GitHub and show HTTP status and latency. |
| `/githubbridge reload` | Reload the private properties and update. |
| `/githubbridge refresh` | Download the subscription and rebuild proxies. |
| `/githubbridge set enabled <on\|off>` | Enable or disable the bridge. |
| `/githubbridge set subscription <URL>` | Save a subscription URL. |
| `/githubbridge set proxy <name>` | Select a proxy from the current subscription. |

## 🔐 Privacy, build, and license

Never publish a real subscription URL. Runtime YAML, tokens, proxy credentials, and `config/` stay local. Build with `./gradlew clean test build`. The project is `GPL-3.0-only`; see `THIRD_PARTY_NOTICES.md` for the bundled Mihomo `v1.19.28`. ⚖️

[⬆️ Language buttons](#top)

---

<a id="lang-ru"></a>

# 🇷🇺 Русский ⛏️🧱

> 🆕 **Тип: оригинальная реализация**<br>
> 🎯 **Область: только домены GitHub внутри процесса Minecraft**<br>
> 🔐 **Приватная подписка не попадает в JAR, исходный код или репозиторий**

## 🚀 Быстрый запуск

1. 📥 Скачайте `minecraft-github-network-bridge-1.21.11-1.0.1.jar`.
2. 📂 Поместите JAR в папку `mods` нужной сборки Minecraft.
3. ▶️ Откройте **Настройки → Minecraft GitHub**.
4. ⚙️ В **Настройки → Быстрая подписка** вставьте URL YAML-подписки Clash/Mihomo и нажмите нижнюю кнопку **Получить / обновить прокси**.
5. 🌐 Откройте **Группу прокси** и выберите узел. Маленькая кнопка `↻` измеряет задержку GitHub.

Зелёный `HTTP 200` означает, что совместимые моды Minecraft могут обращаться к GitHub. ✅

## 📦 Требования

Minecraft `1.21.11`, Fabric Loader `0.19.3+`, Fabric API `0.141.5+1.21.11+`, Java `21+`, Windows x64. Mod Menu `17.0.0+` необязателен. Для других версий Minecraft будут выпускаться отдельные JAR. 🎮

## 🖥️ Все страницы и элементы

### ⚙️ Вкладка «Настройки»

- **Быстрая подписка → URL подписки**: создаёт конфигурацию со стандартными параметрами; новый URL сбрасывает старый выбор узла.
- **Быстрая подписка → Получить / обновить прокси**: заменяет старую проверку, сохраняет URL, загружает подписку и открывает обновлённую группу прокси.
- **Новая конфигурация → Название**: понятное имя локальной конфигурации.
- **Описание**: локальная заметка, не влияет на сеть.
- **URL подписки**: приватная YAML-подписка Clash/Mihomo.
- **User Agent**: идентификатор загрузки; обычно `clash.meta`.
- **GitHub**: главный переключатель.
- **Автообновление**: периодически обновляет подписку и сохраняет последний рабочий кэш при ошибке.
- **Сетевой инструмент**: при ошибке прямой загрузки временно использует локальный сетевой инструмент только для подписки.
- **Недействительные сертификаты**: разрешает ошибочные TLS-сертификаты подписки; по умолчанию выключено.
- **Тайм-аут / Интервал**: секунды ожидания и минуты между обновлениями.

### 🌐 Вкладка «Группа прокси»

- **Квадратная кнопка `↻`**: выполняет реальный GitHub URL Test для всех узлов во временном изолированном Mihomo.
- **До нажатия `↻`**: задержка не показывается.
- **После нажатия**: `[xx ms]` — узел работает, `[--]` — проверка не прошла.
- **Кэш задержки**: только в памяти; закрытие экрана или перезапуск Minecraft удаляет его.
- **Строка узла**: выбирает и сохраняет прокси; `[*]` отмечает выбранный.
- **`<` / `>`**: предыдущая и следующая страница.

### 🧪 Нижние кнопки

- **Получить / обновить прокси**: показывается на странице быстрой подписки и обновляет список узлов.
- **Проверить**: показывается в новой конфигурации, общих настройках и группе прокси; сохраняет параметры и проверяет GitHub API.
- **Отмена**: возвращается без намеренного сохранения незавершённого ввода.
- **Готово**: сохраняет и при необходимости перестраивает GitHub-only профиль.

## 🎯 Область сети

Только GitHub-запросы совместимых модов Minecraft идут через локальный порт, обычно `127.0.0.1:17897`. Если Clash уже занял этот порт, мод автоматически выбирает и сохраняет другой свободный порт. Hypixel, Mojang, вход Microsoft и остальные домены используют прямой путь. Мод не меняет системный прокси Windows, TUN, переменные среды, браузер, PCL, Git или GitHub CLI. 🛡️

Mihomo встроен в JAR, отдельный Sidecar не нужен. Файлы появляются автоматически в `config/minecraft-github-network-bridge/`. При выходе из Minecraft процесс останавливается, порт освобождается. 🧹

## ⌨️ Команды

| Команда | Действие |
| --- | --- |
| `/githubbridge` или `status` | Показывает состояние, профиль, выбранный прокси и встроенный сервис. |
| `/githubbridge config` | Открывает настройки. |
| `/githubbridge proxies` | Сразу открывает группу прокси. |
| `/githubbridge test` | Проверяет GitHub и показывает HTTP-код и задержку. |
| `/githubbridge reload` | Перечитывает приватный файл настроек. |
| `/githubbridge refresh` | Обновляет подписку и список прокси. |
| `/githubbridge set enabled <on\|off>` | Включает или выключает мост. |
| `/githubbridge set subscription <URL>` | Сохраняет URL подписки. |
| `/githubbridge set proxy <имя>` | Выбирает узел текущей подписки. |

## 🔐 Приватность, сборка и лицензия

Не публикуйте настоящий URL подписки. YAML, токены, данные прокси и `config/` остаются локальными. Сборка: `./gradlew clean test build`. Лицензия проекта — `GPL-3.0-only`; сведения о встроенном Mihomo `v1.19.28` находятся в `THIRD_PARTY_NOTICES.md`. ⚖️

[⬆️ Кнопки языков](#top)
