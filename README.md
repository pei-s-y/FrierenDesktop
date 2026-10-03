# 芙莉莲桌面旅伴 · Frieren Desktop

基于 Java 25 / JavaFX 的轻量桌面精灵，保留原有芙莉莲动画。

## 功能

- 奶白与浅紫色界面、对话气泡、系统资源进度条；工具面板可收起。
- 拖动角色或标题栏移动，双击摸摸头；支持右键菜单及置顶开关。
- 聊天、摸头、点心投喂与亲密度（本次运行有效，上限 100）；投喂冷却 30 秒。
- 15 / 25 / 45 分钟专注计时，支持取消，结束时提醒休息。
- 休息模式暂停普通提醒；专注计时仍继续，结束提醒仍显示。
- CPU 与物理内存监控，CPU 高负载提醒间隔至少 2 分钟。
- 5 分钟未与本应用互动时提醒喝水。这不是全系统键鼠闲置检测。
- 手动翻译剪贴板文字到中文或英文；点击气泡查看完整结果，也可选中复制。

## 运行

安装 JDK 25，确保 `JAVA_HOME` 指向 JDK，并将其 `bin` 目录加入 PATH。

Windows：

```powershell
.\mvnw.cmd clean test javafx:run
```

macOS / Linux（需要图形桌面）：

```sh
sh mvnw clean test javafx:run
```

翻译是可选功能。在启动前设置自己的百度翻译凭证：

```powershell
$env:BAIDU_APP_ID = "你的 App ID"
$env:BAIDU_SECRET_KEY = "你的密钥"
.\mvnw.cmd javafx:run
```

点击“翻译剪贴板”会将当前剪贴板文字发送到百度翻译服务；其他操作不读取剪贴板。单次限制 6000 UTF-8 字节，请求超时 15 秒。没有凭证时，其余功能照常使用。

## 模块边界

| 目录 / 类 | 职责 |
| --- | --- |
| `FrierenApp` / `Launcher` | 启动和退出生命周期 |
| `ui/PetView` / `pet.css` | 组件布局与视觉样式 |
| `ui/PetController` | 绑定用户操作、调度与窗口行为 |
| `ui/BubblePresenter` | 单一气泡计时器，避免旧消息隐藏新消息 |
| `model/CompanionState` | 亲密度、投喂冷却、休息与专注状态，无 JavaFX 依赖 |
| `service/QuoteService` | 分时段台词 |
| `service/SystemMonitor` | JMX 资源采样，不支持的指标显示为 — |
| `service/TranslationService` | HTTP 请求、签名、超时及响应解析 |

修改主题请编辑 `pet.css`；新增台词请编辑 `QuoteService`。新增互动规则放在 model，再由 controller 绑定到 view。

## 验证

```powershell
.\mvnw.cmd test
# 需要可用图形桌面；执行 UI 冒烟测试并生成 target/ui-*.png
.\mvnw.cmd "-DuiTests=true" test
```

测试覆盖投喂冷却、亲密度上限、休息与专注相互作用、计时取消、时间边界和翻译异常响应。UI 测试检查主要按钮、面板收起展开与布局，并生成真实 JavaFX 截图。在线翻译需要有效的个人凭证单独验证。
