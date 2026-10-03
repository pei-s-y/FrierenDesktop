package xyz.p050501.frierendesktop;
import javafx.application.Application;
import javafx.stage.Stage;
import xyz.p050501.frierendesktop.ui.PetController;
public class FrierenApp extends Application {
    private PetController controller;
    @Override public void start(Stage stage) { controller = new PetController(stage); controller.show(); }
    @Override public void stop() { if (controller != null) controller.close(); }
    public static void main(String[] args) { launch(args); }
}
