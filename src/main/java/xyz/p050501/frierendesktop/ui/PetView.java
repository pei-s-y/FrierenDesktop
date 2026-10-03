package xyz.p050501.frierendesktop.ui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import java.util.Objects;

/** Layout and styling only. The controller owns behavior. */
public final class PetView extends VBox {
    final Label bubble = new Label("旅途不必匆忙。今天也一起度过吧。");
    final Label status = new Label("陪伴中 · 亲密度 0");
    final Label cpu = new Label("CPU  —"), memory = new Label("内存  —");
    final ProgressBar cpuBar = new ProgressBar(0), memoryBar = new ProgressBar(0);
    final Button talk = button("聊聊天", "听一句旅途中的话"), pet = button("摸摸头", "也可以双击芙莉莲"), feed = button("喂点心", "每 30 秒可以投喂一次");
    final Button focus = button("开始专注", "开始或取消专注计时"), translate = button("翻译剪贴板", "将剪贴板文字发送到百度翻译");
    final ToggleButton rest = new ToggleButton("休息"), pin = new ToggleButton("置顶");
    final ComboBox<Integer> minutes = new ComboBox<>();
    final ComboBox<String> language = new ComboBox<>();
    final Label countdown = new Label("留一点时间，做喜欢的事。");
    final ImageView character;
    final VBox tools;
    final HBox header;
    final Button collapse = button("−", "展开或收起工具面板"), exit = button("×", "退出桌面精灵");

    public PetView() {
        getStyleClass().add("pet-root");
        setAlignment(Pos.BOTTOM_CENTER);
        setSpacing(8);
        Label title = new Label("FRIEREN  /  芙莉莲");
        title.getStyleClass().add("title");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        header = new HBox(6, title, spacer, collapse, exit);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        collapse.getStyleClass().add("icon-button"); exit.getStyleClass().add("icon-button");
        bubble.getStyleClass().add("speech"); bubble.setWrapText(true);
        bubble.setMaxWidth(Double.MAX_VALUE); bubble.setMinHeight(76); bubble.setPrefHeight(76); bubble.setMaxHeight(76);
        bubble.setTooltip(new Tooltip("点击气泡可查看完整内容"));
        Image image = new Image(Objects.requireNonNull(getClass().getResource("/frieren.gif"), "缺少 /frieren.gif").toExternalForm());
        if (image.isError()) throw new IllegalStateException("无法读取角色动图", image.getException());
        character = new ImageView(image); character.setFitHeight(260); character.setFitWidth(290); character.setPreserveRatio(true);
        StackPane scene = new StackPane(character); scene.setMinHeight(180); scene.setPrefHeight(220);
        character.fitHeightProperty().bind(scene.heightProperty().subtract(10));
        javafx.scene.shape.Rectangle portraitClip = new javafx.scene.shape.Rectangle();
        double ratio = image.getWidth() / image.getHeight();
        portraitClip.widthProperty().bind(javafx.beans.binding.Bindings.min(character.fitWidthProperty(), character.fitHeightProperty().multiply(ratio)));
        portraitClip.heightProperty().bind(portraitClip.widthProperty().divide(ratio));
        portraitClip.setArcWidth(28); portraitClip.setArcHeight(28); character.setClip(portraitClip);
        scene.getStyleClass().add("character-scene");
        Label hint = new Label("拖动角色移动 · 双击摸摸头 · 右键菜单"); hint.getStyleClass().addAll("hint", "gesture-hint");
        status.getStyleClass().add("status");
        HBox actions = new HBox(6, talk, pet, feed); actions.setAlignment(Pos.CENTER);
        for (Button b : new Button[]{talk, pet, feed}) { b.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(b, Priority.ALWAYS); }
        minutes.getItems().addAll(15, 25, 45); minutes.setValue(25); minutes.setAccessibleText("专注时长（分钟）");
        minutes.setTooltip(new Tooltip("专注时长（分钟）"));
        HBox focusRow = new HBox(8, minutes, new Label("分钟"), focus); focusRow.setAlignment(Pos.CENTER_LEFT);
        focus.getStyleClass().add("primary"); focus.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(focus, Priority.ALWAYS);
        countdown.getStyleClass().add("hint");
        language.getItems().addAll("译成中文", "译成英文"); language.setValue("译成中文"); language.setAccessibleText("翻译目标语言");
        HBox translation = new HBox(8, language, translate); translate.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(translate, Priority.ALWAYS);
        cpuBar.setMaxWidth(Double.MAX_VALUE); memoryBar.setMaxWidth(Double.MAX_VALUE);
        VBox cpuBox = new VBox(4, cpu, cpuBar), memBox = new VBox(4, memory, memoryBar);
        HBox metrics = new HBox(14, cpuBox, memBox); HBox.setHgrow(cpuBox, Priority.ALWAYS); HBox.setHgrow(memBox, Priority.ALWAYS);
        pin.setSelected(true);
        HBox options = new HBox(8, rest, pin); options.setAlignment(Pos.CENTER_RIGHT);
        tools = new VBox(10, actions, new Separator(), focusRow, countdown, translation, new Separator(), metrics, options);
        tools.getStyleClass().add("tools");
        getChildren().addAll(header, bubble, scene, status, hint, tools);
    }
    private static Button button(String text, String help) {
        Button button = new Button(text); button.setTooltip(new Tooltip(help)); return button;
    }
}
