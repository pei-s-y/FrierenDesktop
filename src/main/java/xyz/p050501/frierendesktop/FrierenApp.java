package xyz.p050501.frierendesktop;

//叠甲，注释基本全是ai写的，我看了眼应该没什么问题，笨人刚开始学这个架构做的第一个小项目，做的不好请多批评
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.util.Random;

/**
 * 芙莉莲桌面精灵核心应用类 (Frieren Desktop Pet Core)
 * <p>
 * 该类继承自 JavaFX 的 Application 类，是整个图形界面的生命周期入口。
 * 采用了模块化设计，包含：视觉渲染、交互监听、硬件状态监控、网络异步请求等模块。
 */
public class FrierenApp extends Application {

    // ==========================================
    // 1. 全局状态与资源定义区 (类的成员变量)
    // ==========================================
    private double xOffset = 0;
    private double yOffset = 0;

    // 随机数发生器。
    private final Random random = new Random();

    /* * 百度翻译 API 凭证。
     * 这里我嵌套的自己的百度翻译API，可以换成自己的。
     * 此处仅为本地桌面应用测试使用。
     */
    private final String BAIDU_APP_ID = " 换成你的";
    private final String BAIDU_SECRET_KEY = "换成你的";

    /* * 动态台词数据字典。将数据与逻辑分离（硬编码台词 -> 数组存储），方便日后扩展或替换为读取本地 JSON/TXT。
     */
    private final String[] generalQuotes = {
            "人类的寿命真的很短暂呢。",
            "这是搜集民间魔法的重要一环。",
            "辛美尔也曾夸奖过这个日程安排。",
            "我正在寻找一种能让铜像变干净的魔法。",
            "不要一直戳我，人类的时间是很宝贵的。"
    };
    private final String[] morningQuotes = {"早起也是一种修行，虽然对精灵来说没什么意义。"};
    private final String[] nightQuotes = {"深夜是魔力最活跃的时候，还不睡吗？"};

    /* * 闲置监控状态寄存器。
     * lastInteractionTime: 记录系统上一次触发鼠标事件的时间戳（毫秒级）。
     * IDLE_LIMIT: 闲置阈值，设定为 60000 毫秒 (60秒)。
     */
    private long lastInteractionTime = System.currentTimeMillis();
    private final long IDLE_LIMIT = 60000;

    // ==========================================
    // 2. 主程序生命周期入口
    // ==========================================

    /**
     * JavaFX 程序的启动核心方法。
     *
     * @param primaryStage 主舞台，可以理解为 Windows 操作系统分配给该程序的一个“空画框”。
     */
    @Override
    public void start(Stage primaryStage) {
        // 第一阶段：组件实例化 (制造零件)
        ImageView imageView = createCharacterImage();
        if (imageView == null) return; // 容错处理：若核心图片加载失败，直接阻断程序运行

        Label speechBubble = createSpeechBubble();
        Label systemHud = createSystemHud();

        // 第二阶段：事件绑定 (连线打通，依赖注入)
        // 将三个组件引用传入交互方法中，互相打通作用域
        setupInteractions(primaryStage, imageView, speechBubble, systemHud);

        // 第三阶段：启动后台驻留任务 (类似单片机里的定时中断程序)
        startIdleTimer(speechBubble);
        startSystemMonitor(systemHud, speechBubble);

        // 第四阶段：UI 布局组装与渲染 (将画装入画框并挂上墙)
        setupWindowAndShow(primaryStage, imageView, speechBubble, systemHud);
    }

    // ==========================================
    // 3. 模块化方法区 (组件工厂与事件绑定)
    // ==========================================

    /**
     * 构建核心角色动画组件。
     *
     * @return 配置好的 ImageView 对象，失败则返回 null。
     */
    private ImageView createCharacterImage() {
        // 使用相对路径从类的类路径(Classpath，即 resources 目录)下读取资源，这是打包 exe/jar 后不报错的关键
        URL imageUrl = getClass().getResource("/frieren.gif");
        if (imageUrl == null) {
            System.err.println("【系统故障】: 找不到图片文件！");
            return null;
        }
        ImageView imageView = new ImageView(new Image(imageUrl.toExternalForm()));
        imageView.setFitHeight(300);       // 锁定物理高度
        imageView.setPreserveRatio(true);  // 开启长宽比锁定，防止图片被拉伸变形
        return imageView;
    }

    /**
     * 构建动态对话气泡。
     *
     * @return 配置好的 Label 对象。
     */
    private Label createSpeechBubble() {
        Label bubble = new Label();
        // Java 15+ 引入的文本块语法 (""")，极其适合编写 CSS 或 JSON 字符串，保持代码整洁
        String cssStyle = """
                -fx-background-color: rgba(255,255,255,0.85); /* 底色：85%不透明度白色 */
                -fx-padding: 10px 15px;                       /* 内边距：上下10，左右15 */
                -fx-background-radius: 15px;                  /* 背景圆角 */
                -fx-border-color: #cccccc;                    /* 边框颜色 */
                -fx-border-radius: 15px;                      /* 边框圆角 */
                -fx-font-size: 14px;                          /* 字体大小 */
                -fx-font-family: 'Microsoft YaHei';           /* 字体类型 */
                """;
        bubble.setStyle(cssStyle);
        bubble.setVisible(false); // 默认不可见
        bubble.setWrapText(true); // 开启文本自动换行限制
        bubble.setMaxWidth(250);  // 最大宽度 250px，配合换行使用
        return bubble;
    }

    /**
     * 构建极客风格的硬件系统监控面板 (HUD)。
     *
     * @return 配置好的 Label 对象。
     */
    private Label createSystemHud() {
        Label hud = new Label("CPU: --% | RAM: --%");
        hud.setStyle(
                "-fx-text-fill: #00FF00;" +
                "-fx-background-color: rgba(0,0,0,0.3);" +
                "-fx-padding: 2px 8px;" +
                "-fx-background-radius: 5px;"
        );
        // 使用等宽字体 (Consolas)，确保当占用率从 9% 变成 10% 时，面板宽度不会剧烈抖动
        hud.setFont(Font.font("Consolas", 12));
        hud.setVisible(false);
        return hud;
    }

    /**
     * 核心交互路由：集中配置所有的鼠标事件监听器。
     *
     * @param stage        主窗口，用于移动位置
     * @param imageView    角色图片，事件的触发源
     * @param speechBubble 对话气泡，用于反馈文字
     * @param systemHud    监控面板，用于反馈硬件信息
     */
    private void setupInteractions(Stage stage, ImageView imageView, Label speechBubble, Label systemHud) {
        // 设置鼠标悬停指针样式
        imageView.setCursor(Cursor.HAND);

        // --- 拖拽位移逻辑 ---
        imageView.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
            lastInteractionTime = System.currentTimeMillis(); // 重置闲置计时器
        });

        imageView.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
            lastInteractionTime = System.currentTimeMillis();
        });

        // --- HUD 面板显示控制 ---
        imageView.setOnMouseEntered(event -> systemHud.setVisible(true));
        imageView.setOnMouseExited(event -> systemHud.setVisible(false));

        // --- 右键上下文菜单 ---
        ContextMenu contextMenu = new ContextMenu();

        MenuItem translateItem = new MenuItem("帮我翻译剪贴板");
        translateItem.setOnAction(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                String fullText = clipboard.getString();
                String shortText = fullText.length() > 30 ? fullText.substring(0, 30) + "..." : fullText;

                // UI 反馈优先：立即让气泡显示正在翻译，提升用户体验
                updateBubbleSafely("正在咏唱翻译魔法...", speechBubble, stage, 0);

                // 发起网络请求（非阻塞式）
                performBaiduTranslate(fullText, shortText, speechBubble, stage);
            } else {
                updateBubbleSafely("你的剪贴板里好像没有文字哦。", speechBubble, stage, 2);
            }
        });

        MenuItem exitItem = new MenuItem("结束休眠 (退出程序)");
        exitItem.setOnAction(event -> {
            Platform.exit(); // 通知 JavaFX 渲染线程安全退出
            System.exit(0);  // 强行终止底层的 JVM 进程
        });

        contextMenu.getItems().addAll(translateItem, exitItem);
        // 将右键请求事件与菜单绑定
        imageView.setOnContextMenuRequested(event ->
                contextMenu.show(imageView, event.getScreenX(), event.getScreenY())
        );

        // --- 戳一戳互动逻辑 ---
        imageView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // 拦截双击事件
                updateBubbleSafely(getDynamicQuote(), speechBubble, stage, 3);
            }
            lastInteractionTime = System.currentTimeMillis();
        });
    }

    /**
     * 智能台词抽取器。
     *
     * @return 根据当前系统时钟，返回适宜的台词字符串。
     */
    private String getDynamicQuote() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 9) {
            return morningQuotes[random.nextInt(morningQuotes.length)];
        } else if (hour >= 22 || hour < 5) {
            return nightQuotes[random.nextInt(nightQuotes.length)];
        } else {
            return generalQuotes[random.nextInt(generalQuotes.length)];
        }
    }

    /**
     * 界面装配工厂：将散落的组件装配至 VBox 布局容器，并脱去 Windows 的系统边框。
     */
    private void setupWindowAndShow(Stage stage, ImageView imageView, Label speechBubble, Label systemHud) {
        // VBox (Vertical Box) 是垂直线性布局，内部组件从上到下排列
        VBox root = new VBox();
        root.setAlignment(Pos.BOTTOM_CENTER);                // 底边居中对齐，确保图片底部不乱跑
        root.setStyle("-fx-background-color: transparent;"); // 容器全透明

        // 设置外边距 (Insets)，参数顺序为：上、右、下、左
        VBox.setMargin(speechBubble, new Insets(0, 0, 10, 0)); // 气泡下方留出 10px 间隙
        VBox.setMargin(systemHud, new Insets(5, 0, 0, 0));     // HUD 上方留出 5px 间隙

        // 核心装配：顺序决定了上下位置 (气泡最上，图片居中，HUD在脚底)
        root.getChildren().addAll(speechBubble, imageView, systemHud);

        // 场景层：容纳布局，同样设为透明
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        // 舞台层：系统级窗口配置
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT); // 移除标题栏、最小化/关闭按钮
        stage.setAlwaysOnTop(true);              // 设置为置顶窗口 (类似游戏里的最高渲染层级)
        stage.show();
    }

    // ==========================================
    // 4. 后台服务与任务调度区 (后台线程)
    // ==========================================

    /**
     * 闲置状态监听守护进程。
     * 原理：使用 JavaFX 的 Timeline 动画引擎，生成一个每秒触发一次的时钟脉冲。
     */
    private void startIdleTimer(Label speechBubble) {
        Timeline idleTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            long now = System.currentTimeMillis();
            // 逻辑门：只有当 间隔>阈值 且 当前气泡不在显示中 时，才触发发呆提醒
            if (now - lastInteractionTime > IDLE_LIMIT && !speechBubble.isVisible()) {
                speechBubble.setText("人类，你已经发呆很久了。");
                speechBubble.setVisible(true);

                // PauseTransition 是一个非阻塞的延迟触发器
                PauseTransition pause = new PauseTransition(Duration.seconds(3));
                pause.setOnFinished(e -> speechBubble.setVisible(false)); // 3秒后回调隐藏
                pause.play();

                lastInteractionTime = now; // 状态刷新
            }
        }));
        idleTimer.setCycleCount(Timeline.INDEFINITE); // 设置时间轴为无限循环
        idleTimer.play();
    }

    /**
     * 底层硬件资源探针。
     * 原理：利用 JMX (Java Management Extensions) 接口直接读取操作系统内核数据。
     */
    private void startSystemMonitor(Label systemHud, Label speechBubble) {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        Timeline monitorTimer = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            // 探针 1：CPU 负载率
            double cpuLoad = osBean.getCpuLoad() * 100;
            if (cpuLoad < 0) cpuLoad = 0;

            // 探针 2：物理内存占用率
            long totalMem = osBean.getTotalMemorySize();
            long freeMem = osBean.getFreeMemorySize();
            long usedMem = totalMem - freeMem;
            double memUsage = (double) usedMem / totalMem * 100;

            // 格式化数据流并推送到 UI
            String hudText = String.format("CPU: %02d%% | RAM: %02d%%", (int)cpuLoad, (int)memUsage);
            systemHud.setText(hudText);

            // 过载中断报警：CPU 超过 85% 触发高优警告
            if (cpuLoad > 85 && !speechBubble.isVisible()) {
                speechBubble.setText("⚠️ 警告：当前空间魔力异常飙升，请检查是否有失控的魔法程序！");
                speechBubble.setVisible(true);

                PauseTransition pause = new PauseTransition(Duration.seconds(5));
                pause.setOnFinished(e -> speechBubble.setVisible(false));
                pause.play();
                lastInteractionTime = System.currentTimeMillis();
            }
        }));

        monitorTimer.setCycleCount(Timeline.INDEFINITE);
        monitorTimer.play();
    }

    /**
     * 百度翻译 API 异步请求引擎。
     * * @param fullText  完整的长文本，用于发送给百度后台
     * @param shortText 缩略短文本，用于在 UI 界面展示原文（防止过长）
     * @param speechBubble 气泡组件，用于回显结果
     * @param stage 窗口对象，用于处理窗口大小自适应
     */
    private void performBaiduTranslate(String fullText, String shortText, Label speechBubble, Stage stage) {
        // MD5 安全校验算法要求加入随机盐值防止重放攻击
        String salt = String.valueOf(System.currentTimeMillis());
        String src = BAIDU_APP_ID + fullText + salt + BAIDU_SECRET_KEY;
        String sign = getMD5(src);

        try {
            // 构造符合 RESTful 标准的 GET 请求 URL，并对中文参数强制 URL 编码
            String url = String.format(
                    "https://fanyi-api.baidu.com/api/trans/vip/translate?q=%s&from=auto&to=zh&appid=%s&salt=%s&sign=%s",
                    URLEncoder.encode(fullText, StandardCharsets.UTF_8),
                    BAIDU_APP_ID, salt, sign
            );

            // 核心知识点：异步网络请求 (Asynchronous I/O)
            // sendAsync() 内部会开启一个新的子线程去等待网络响应，主线程（UI线程）会立刻继续往下走。
            // 这样芙莉莲的动图就不会因为网络卡顿而卡死。
            HttpClient.newHttpClient().sendAsync(
                    HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            ).thenApply(HttpResponse::body).thenAccept(responseBody -> {
                // thenAccept 里的代码块是“回调函数”，当网络响应到达时，由【非UI的子线程】执行
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(responseBody); // 将 JSON 字符串反序列化为树状对象

                    if (root.has("error_code")) {
                        String errorMsg = root.get("error_msg").asText();
                        updateBubbleSafely("魔法解析失败: " + errorMsg, speechBubble, stage, 4);
                        return;
                    }

                    // JsonNode 寻址：解析形如 {"trans_result":[{"dst":"翻译结果"}]} 的结构
                    String result = root.get("trans_result").get(0).get("dst").asText();
                    updateBubbleSafely("原文: " + shortText + "\n翻译: " + result, speechBubble, stage, 6);

                } catch (Exception e) {
                    updateBubbleSafely("翻译魔法阵坍塌 (JSON解析异常)", speechBubble, stage, 3);
                }
            }).exceptionally(ex -> {
                // 异常捕获块：处理断网、DNS解析失败等网络底层异常
                updateBubbleSafely("无法连接到魔法网络 (网络中断)", speechBubble, stage, 3);
                return null;
            });

        } catch (Exception e) {
            updateBubbleSafely("咒语构造错误...", speechBubble, stage, 2);
        }
    }

    /**
     * MD5 数字摘要算法封装。
     */
    private String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 线程安全 (Thread-Safe) 的 UI 更新封装器。
     * * 核心规则：JavaFX (包括 Android、WPF) 强制要求，所有对 UI 界面元素的修改操作，
     * 必须且只能由【主 UI 线程】执行。否则会报 IllegalStateException。
     * * @param text     要在气泡中显示的文本
     * @param duration 气泡存活时长 (秒)。若传 0 则不会自动消失。
     */
    private void updateBubbleSafely(String text, Label speechBubble, Stage stage, int duration) {
        // Platform.runLater 的作用：将大括号里的任务，打包推送到主 UI 线程的任务队列中排队等待执行。
        Platform.runLater(() -> {
            speechBubble.setText(text);
            speechBubble.setVisible(true);

            // 窗口高度自适应修正逻辑
            // 1. 记录窗口当前的绝对底边 Y 坐标
            double oldBottomY = stage.getY() + stage.getHeight();

            // 2. 告诉操作系统：“文字变了，帮我重新计算窗口物理大小”
            stage.sizeToScene();

            // 3. 窗口变大后默认向下延展。为了保持芙莉莲脚部位置不动，向上提升 Y 坐标
            stage.setY(oldBottomY - stage.getHeight());

            // 自动消散定时器
            if (duration > 0) {
                PauseTransition delay = new PauseTransition(Duration.seconds(duration));
                delay.setOnFinished(e -> speechBubble.setVisible(false));
                delay.play();
            }
        });
    }

    // ==========================================
    // 5. 虚拟机钩子
    // ==========================================
    public static void main(String[] args) {
        launch(args); // 唤醒 JavaFX 引擎
    }
}