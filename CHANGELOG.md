<div align="center">

# 🧱 Minecraft GitHub Network Bridge 更新日志 ⛏️

<a href="#changes-zh">🇨🇳 简体中文</a> ·
<a href="#changes-en">🇬🇧 English</a> ·
<a href="#changes-ru">🇷🇺 Русский</a>

</div>

---

<a id="changes-zh"></a>

## 🇨🇳 `1.0.1` 快速订阅操作调整 🔄🧭

- ⛏️ 快速订阅页底部的“测试”改为“获取/刷新线路”。
- 🌐 保存订阅后立即下载并刷新线路，然后自动进入代理组。
- 🧹 删除代理组顶部重复的“获取/刷新线路”按钮。
- 🧭 代理组保留翻页、选线和 `↻` 节点延迟刷新。

## 🇨🇳 `1.0.0` 首版 🎮💎

### 🚀 实际使用

- 📦 单个 Mod JAR 内置 Mihomo，不需要另外安装 Sidecar 或 Clash。
- 🔗 支持快速粘贴 Clash/Mihomo YAML 订阅 URL。
- 🧱 支持新建带名称、描述和客户端标识的配置。
- 🌐 顶层只保留 **设置** 与 **代理组** 两个页签。
- 🛰️ 代理组显示订阅线路，点击即可持久选择。
- 🔄 增加 `20×20` 正方形 `↻` 按钮，通过临时 Mihomo 对所有节点执行真实 GitHub URL Test。
- 🧠 未点击不显示延迟；延迟只在当前界面内存中保存，重进游戏自动丢弃。

### 🛡️ 网络隔离

- 🎯 只有我的世界 Minecraft 进程中的 GitHub 域名进入内置代理。
- 🎮 Hypixel、Mojang、Microsoft 登录和其他域名固定直连。
- 🚫 不修改 Windows 系统代理、TUN、环境变量或其他程序。
- 🔌 Clash 系统代理/全局/规则/直连模式可以与 Mod 同时运行。
- 🧰 首选本地端口被 Clash 或其他程序占用时，自动选择并保存空闲端口。
- 🧹 Minecraft 退出时停止 Mod 拥有的 Mihomo，并等待端口释放。

### ⚙️ 设置与指令

- 🎛️ 增加快速订阅、新建配置、常规设置和代理组页面。
- 🧪 增加界面内 GitHub 测试与中文状态反馈。
- ⌨️ 增加 `status`、`config`、`proxies`、`test`、`reload`、`refresh`、`set enabled`、`set subscription`、`set proxy`。
- 🇨🇳 游戏界面的异常、校验和指令反馈全部提供中文提示。
- 🧩 修复反复返回选项页时出现两个“我的世界 GitHub”按钮的问题。

### 🔐 隐私、兼容与测试

- 🔒 订阅 URL、节点和运行配置只保存在当前 Minecraft 实例。
- 📜 项目与内置 Mihomo 使用 GPL-3.0，附带第三方许可证与 SHA-256。
- 🧪 增加 11 项测试，包括配置、域名隔离、订阅解析、选线、URL Test 和端口冲突。
- ✅ 在隔离 Test 实例验证 GitHub API `HTTP 200` 与退出清理。

---

<a id="changes-en"></a>

## 🇬🇧 `1.0.1` Quick Subscription Flow 🔄🧭

- Replaced the Quick Setup footer Test action with Get / Refresh Proxies.
- Saving a quick subscription now downloads it, refreshes the list, and opens Proxy Group.
- Removed the duplicate refresh action from Proxy Group; paging, selection, and latency testing remain there.

## 🇬🇧 `1.0.0` First Release 🎮💎

- 📦 One Fabric JAR with an embedded Mihomo runtime; no separate Sidecar installation.
- 🔗 Quick Clash/Mihomo YAML subscription setup and named configurations.
- 🌐 Two top-level tabs: **Settings** and **Proxy Group**.
- 🛰️ Persistent manual proxy selection from the imported subscription.
- 🔄 A square `↻` button performs real GitHub URL Tests through every node; latency is memory-only.
- 🎯 Only GitHub domains inside Minecraft are proxied; Hypixel, Mojang, Microsoft login, and other hosts stay direct.
- 🔌 Coexists with Clash system proxy modes and automatically changes the local port when the preferred port is occupied.
- 🧹 Stops the owned Mihomo process and waits for port release when Minecraft exits or the proxy changes.
- ⌨️ Full configuration, status, refresh, test, subscription, and proxy-selection commands.
- 🔐 Private subscriptions and runtime YAML remain local to the Minecraft instance.
- 🧪 Tests cover configuration, isolation, parsing, selection, URL Test latency, and port conflicts.

---

<a id="changes-ru"></a>

## 🇷🇺 `1.0.1` Быстрая подписка 🔄🧭

- Кнопка проверки на странице быстрой подписки заменена на получение и обновление узлов.
- После сохранения подписка загружается, список обновляется и открывается группа прокси.
- Дублирующая кнопка удалена из группы прокси; там остались страницы, выбор узла и измерение задержки.

## 🇷🇺 `1.0.0` Первый выпуск 🎮💎

- 📦 Один Fabric JAR со встроенным Mihomo; отдельный Sidecar не требуется.
- 🔗 Быстрая настройка YAML-подписки Clash/Mihomo и именованные конфигурации.
- 🌐 Две основные вкладки: **Настройки** и **Группа прокси**.
- 🛰️ Ручной выбор и сохранение узла из подписки.
- 🔄 Квадратная кнопка `↻` выполняет реальный GitHub URL Test для всех узлов; задержка хранится только в памяти.
- 🎯 Только домены GitHub внутри Minecraft используют прокси; Hypixel, Mojang, вход Microsoft и другие домены идут напрямую.
- 🔌 Совместная работа с режимами системного прокси Clash и автоматическая смена локального порта при конфликте.
- 🧹 При выходе из Minecraft процесс Mihomo останавливается, порт освобождается.
- ⌨️ Команды настройки, статуса, обновления, теста, подписки и выбора узла.
- 🔐 Приватные подписки и YAML остаются внутри текущей сборки Minecraft.
- 🧪 Тесты охватывают конфигурацию, изоляцию, подписки, выбор узла, задержку и конфликт портов.
