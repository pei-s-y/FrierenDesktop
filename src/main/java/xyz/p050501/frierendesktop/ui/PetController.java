package xyz.p050501.frierendesktop.ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;
import xyz.p050501.frierendesktop.model.CompanionState;
import xyz.p050501.frierendesktop.service.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;

public final class PetController implements AutoCloseable {
    private final Stage stage;
    private final PetView view = new PetView();
    private final CompanionState state = new CompanionState();
    private final QuoteService quotes = new QuoteService();
    private final SystemMonitor monitor = new SystemMonitor();
    private final TranslationService translation = new TranslationService();
    private final BubblePresenter bubbles = new BubblePresenter(view.bubble);
    private final Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
    private final ScaleTransition bounce = new ScaleTransition(Duration.millis(140), view.character);
    private CompletableFuture<String> pending;
    private Instant lastInteraction = Instant.now(), lastWarning = Instant.MIN;
    private double offsetX, offsetY, pressX, pressY;
    private boolean dragged, closed;
    private int ticks;

    public PetController(Stage stage) {
        this.stage = stage;
        view.setPrefWidth(352);
        Scene scene = new Scene(view); scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/xyz/p050501/frierendesktop/pet.css").toExternalForm());
        stage.initStyle(StageStyle.TRANSPARENT); stage.setScene(scene); stage.setAlwaysOnTop(true); stage.setResizable(false);
        stage.setTitle("芙莉莲 · 桌面旅伴");
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> lastInteraction = Instant.now());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> lastInteraction = Instant.now());
        view.talk.setOnAction(e -> say(state.resting() ? "等我醒来，再听你说旅途的故事。" : quotes.next(LocalTime.now())));
        view.pet.setOnAction(e -> pet());
        view.feed.setOnAction(e -> { say(state.feed(Instant.now())); animate(); refresh(); });
        view.rest.setOnAction(e -> {
            state.setResting(view.rest.isSelected());
            view.character.setOpacity(state.resting() ? 0.6 : 1);
            view.rest.setText(state.resting() ? "唤醒" : "休息");
            say(state.resting() ? "晚安。专注计时仍会继续，其他提醒先暂停。" : "睡得很好。我们继续旅行吧。"); refresh();
        });
        view.pin.setOnAction(e -> stage.setAlwaysOnTop(view.pin.isSelected()));
        view.exit.setOnAction(e -> Platform.exit());
        view.collapse.setOnAction(e -> {
            boolean expanded = !view.tools.isVisible();
            double bottom = stage.getY() + stage.getHeight();
            view.tools.setVisible(expanded); view.tools.setManaged(expanded); view.collapse.setText(expanded ? "−" : "+");
            stage.sizeToScene(); stage.setY(bottom - stage.getHeight()); clampToScreen();
        });
        view.focus.setOnAction(e -> {
            if (state.focusing()) { state.cancelFocus(); say("计时已取消。准备好了再出发。"); }
            else { state.startFocus(Instant.now(), java.time.Duration.ofMinutes(view.minutes.getValue())); say("我会陪你。专注结束时，再叫你休息。"); }
            refresh();
        });
        view.translate.setOnAction(e -> translate());
        view.bubble.setOnMouseClicked(e -> showFullMessage());
        installDrag(view.character); installDrag(view.header);
        view.character.setOnMouseClicked(e -> { if (!dragged && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) pet(); });
        ContextMenu menu = new ContextMenu();
        MenuItem chat = new MenuItem("聊聊天"), rest = new MenuItem("休息 / 唤醒"), translate = new MenuItem("翻译剪贴板"), exit = new MenuItem("退出");
        chat.setOnAction(e -> view.talk.fire()); rest.setOnAction(e -> view.rest.fire()); translate.setOnAction(e -> view.translate.fire()); exit.setOnAction(e -> Platform.exit());
        menu.getItems().addAll(chat, rest, translate, new SeparatorMenuItem(), exit);
        view.character.setOnContextMenuRequested(e -> menu.show(view.character, e.getScreenX(), e.getScreenY()));
        bounce.setFromX(1); bounce.setFromY(1); bounce.setToX(1.045); bounce.setToY(0.96); bounce.setAutoReverse(true); bounce.setCycleCount(2);
        ticker.setCycleCount(Animation.INDEFINITE);
    }
    public void show() {
        stage.show();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMaxX() - stage.getWidth() - 24); stage.setY(bounds.getMaxY() - stage.getHeight() - 24);
        clampToScreen(); bubbles.show(quotes.next(LocalTime.now()), 8); ticker.play();
    }
    private void pet() { say(state.pet()); animate(); refresh(); }
    private void animate() { if (!state.resting()) { bounce.stop(); bounce.playFromStart(); } }
    private void say(String text) { bubbles.show(text, 8); }
    private void refresh() {
        view.status.setText((state.resting() ? "休息中" : state.focusing() ? "专注陪伴中" : "陪伴中") + " · 亲密度 " + state.affection());
        view.focus.setText(state.focusing() ? "取消专注" : "开始专注"); view.minutes.setDisable(state.focusing());
        long seconds = state.remainingSeconds(Instant.now());
        view.countdown.setText(state.focusing() ? String.format("还剩 %02d:%02d · 我会安静陪你", seconds / 60, seconds % 60) : "留一点时间，做喜欢的事。");
    }
    private void tick() {
        Instant now = Instant.now();
        if (state.finishFocus(now)) say("专注完成！站起来伸个懒腰，喝口水吧。亲密度 +10");
        refresh();
        if (++ticks % 2 == 0) {
            SystemMonitor.Snapshot sample = monitor.sample();
            metric(view.cpu, view.cpuBar, "CPU", sample.cpu()); metric(view.memory, view.memoryBar, "内存", sample.memory());
            if (!state.resting() && !state.focusing() && sample.cpu() > 0.85 && !bubbles.visible() && now.isAfter(lastWarning.plusSeconds(120))) {
                say("魔力有些过载了，检查一下正在运行的程序吧。"); lastWarning = now;
            }
        }
        if (!state.resting() && !state.focusing() && !bubbles.visible() && now.isAfter(lastInteraction.plusSeconds(300))) {
            say("好久没聊天了。忙碌的时候，也记得喝水哦。"); lastInteraction = now;
        }
    }
    private void metric(Label label, ProgressBar bar, String name, double value) {
        boolean available = Double.isFinite(value) && value >= 0;
        label.setText(name + (available ? String.format("  %.0f%%", value * 100) : "  —"));
        bar.setProgress(available ? Math.min(1, value) : 0);
    }
    private void translate() {
        if (pending != null && !pending.isDone()) return;
        String text = Clipboard.getSystemClipboard().getString();
        view.translate.setDisable(true); bubbles.show("正在咏唱翻译魔法……", 0);
        pending = translation.translate(text, view.language.getValue().equals("译成英文") ? "en" : "zh");
        pending.whenComplete((result, error) -> Platform.runLater(() -> {
            if (closed) return;
            view.translate.setDisable(false);
            if (error == null) bubbles.show(result, 0);
            else {
                Throwable cause = error;
                while (cause.getCause() != null && !(cause instanceof IllegalStateException) && !(cause instanceof IllegalArgumentException)) cause = cause.getCause();
                say(cause instanceof IllegalStateException || cause instanceof IllegalArgumentException ? cause.getMessage() : "无法连接翻译服务，请检查网络后重试。");
            }
        }));
    }
    private void showFullMessage() {
        Dialog<Void> dialog = new Dialog<>(); dialog.initOwner(stage); dialog.setTitle("芙莉莲的留言");
        TextArea text = new TextArea(view.bubble.getText()); text.setEditable(false); text.setWrapText(true); text.setPrefSize(420, 220);
        dialog.getDialogPane().setContent(text); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); dialog.show();
    }
    private void installDrag(javafx.scene.Node node) {
        node.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY || isButton(e.getTarget())) return;
            offsetX = e.getScreenX() - stage.getX(); offsetY = e.getScreenY() - stage.getY(); pressX = e.getScreenX(); pressY = e.getScreenY(); dragged = false;
        });
        node.setOnMouseDragged(e -> {
            if (!e.isPrimaryButtonDown() || isButton(e.getTarget())) return;
            if (Math.hypot(e.getScreenX() - pressX, e.getScreenY() - pressY) > 4) dragged = true;
            if (dragged) { stage.setX(e.getScreenX() - offsetX); stage.setY(e.getScreenY() - offsetY); }
        });
        node.setOnMouseReleased(e -> { if (dragged) clampToScreen(); });
    }
    private boolean isButton(Object target) {
        if (!(target instanceof javafx.scene.Node node)) return false;
        for (; node != null; node = node.getParent()) if (node instanceof ButtonBase) return true;
        return false;
    }
    private void clampToScreen() {
        var screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Rectangle2D b = (screens.isEmpty() ? Screen.getPrimary() : screens.getFirst()).getVisualBounds();
        stage.setX(Math.max(b.getMinX(), Math.min(stage.getX(), b.getMaxX() - stage.getWidth())));
        stage.setY(Math.max(b.getMinY(), Math.min(stage.getY(), b.getMaxY() - stage.getHeight())));
    }
    @Override public void close() {
        closed = true; ticker.stop(); bounce.stop(); bubbles.close();
        if (pending != null) pending.cancel(true);
        translation.close();
    }
}
