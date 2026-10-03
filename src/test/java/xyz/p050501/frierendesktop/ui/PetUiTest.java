package xyz.p050501.frierendesktop.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "uiTests", matches = "true")
class PetUiTest {
    @Test void rendersAndExercisesCompanionControls() throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.startup(() -> Platform.setImplicitExit(false));
        Platform.runLater(() -> {
            PetController controller = null;
            try {
                Stage stage = new Stage(); controller = new PetController(stage); controller.show();
                PetView view = (PetView) stage.getScene().getRoot();
                view.applyCss(); view.layout();
                assertTrue(view.tools.getBoundsInParent().getMaxY() <= view.getHeight(), "Tools must fit in window");
                view.pet.fire(); assertTrue(view.status.getText().contains("亲密度 1"));
                view.feed.fire(); assertTrue(view.status.getText().contains("亲密度 6"));
                view.feed.fire(); assertTrue(view.status.getText().contains("亲密度 6"));
                view.focus.fire(); assertEquals("取消专注", view.focus.getText()); assertTrue(view.minutes.isDisabled());
                view.rest.fire(); assertTrue(view.status.getText().contains("休息中"));
                view.pet.fire(); assertTrue(view.status.getText().contains("亲密度 6"));
                view.rest.fire(); view.focus.fire(); assertFalse(view.minutes.isDisabled());
                view.pin.fire(); assertFalse(stage.isAlwaysOnTop()); view.pin.fire();
                view.bubble.setText("旅途不必匆忙。今天也一起度过吧。");
                view.character.setScaleX(1); view.character.setScaleY(1);
                snapshot(view, "target/ui-expanded.png");
                view.collapse.fire(); assertFalse(view.tools.isManaged());
                snapshot(view, "target/ui-compact.png");
                view.collapse.fire(); assertTrue(view.tools.isManaged());
                for (var node : view.tools.lookupAll(".button")) {
                    if (node instanceof Button button) assertTrue(button.getWidth() > 0);
                }
                stage.close(); done.complete(null);
            } catch (Throwable e) { done.completeExceptionally(e); }
            finally { if (controller != null) controller.close(); }
        });
        try { done.get(30, TimeUnit.SECONDS); }
        finally { Platform.exit(); }
    }
    private static void snapshot(PetView view, String path) throws Exception {
        view.applyCss(); view.layout();
        WritableImage image = view.snapshot(null, null);
        BufferedImage output = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) for (int x = 0; x < output.getWidth(); x++)
            output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        ImageIO.write(output, "png", Path.of(path).toFile());
    }
}
