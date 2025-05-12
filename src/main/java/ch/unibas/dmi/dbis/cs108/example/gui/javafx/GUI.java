package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.LoginScreen;
import ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;
import static ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.SoundManager.*;


/**
 * Launches the JavaFX GUI and sizes the window to the current screen bounds
 * to avoid Canvas reallocation lag when maximizing.
 */
public class GUI extends Application {

    @Override
    public void start(Stage stage) {
        if (ThinkOutsideTheRoom.gameContext == null) {
            LoginScreen login = new LoginScreen(stage, ThinkOutsideTheRoom.chosenUserName);
            Scene loginScene = new Scene(login, 600, 600);
            stage.setScene(loginScene);
            stage.setTitle("Login");
            stage.show();
            return;
        }

        // 1) Kick off your game context & network loop
        GameContext context = ThinkOutsideTheRoom.gameContext;
        context.start();
        Platform.runLater(context::startGameLoop);

        // 2) Grab your CGU and size to screen
        CentralGraphicalUnit cgu = CentralGraphicalUnit.getInstance();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double screenW = bounds.getWidth();
        double screenH = bounds.getHeight();

        Scene gameScene = new Scene(cgu.getMainContainer(), screenW, screenH);
        stage.setScene(gameScene);
        stage.setTitle("Game");
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.show();

        // 3) Start background music
        initBackgroundMusic();
        playBackground();

        // 4) F11 fullscreen toggle
        gameScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        // 5) Start your continuous render loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                GraphicsContext gc = cgu.getGraphicsContext();
                // clear
                gc.clearRect(0, 0, cgu.getContainerWidth(), cgu.getContainerHeight());
                // draw background + all objects
                cgu.draw(gc);
            }
        }.start();

        // 6) Ensure keyboard focus
        Platform.runLater(() -> cgu.getMainContainer().requestFocus());
    }


    public static void main(String[] args) {
        launch(args);
    }
}
