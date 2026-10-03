package xyz.p050501.frierendesktop.ui;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.util.Duration;

/** One timer prevents an older message from hiding a newer one. FX thread only. */
final class BubblePresenter implements AutoCloseable {
    private final Label label;
    private final PauseTransition timer = new PauseTransition();
    BubblePresenter(Label label) { this.label = label; timer.setOnFinished(e -> label.setVisible(false)); }
    void show(String text, double seconds) {
        timer.stop(); label.setText(text); label.setVisible(true);
        if (seconds > 0) { timer.setDuration(Duration.seconds(seconds)); timer.playFromStart(); }
    }
    boolean visible() { return label.isVisible(); }
    @Override public void close() { timer.stop(); }
}
