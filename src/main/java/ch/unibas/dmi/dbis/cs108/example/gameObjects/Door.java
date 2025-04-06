package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class Door extends GameObject {

    private float x, y, width, height;

    public Door(String name, float x, float y, float width, float height, String gameId) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        // Make the door static: not movable.
        setMovable(false);
    }

    @Override
    public void myUpdateLocal(float deltaTime) {
        // A static door never changes its position.
        // Instead, check for the win condition.
        Game currentGame = GameContext.getGameById(getGameId());
        if (currentGame == null) return;
        for (GameObject go : currentGame.getGameObjects()) {
            if (go instanceof Key) {
                Key key = (Key) go;
                if (this.intersects(key)) {
                    System.out.println("You won the game!");
                    Message winMsg = new Message("WIN", new Object[]{"You won the game!"}, null);
                    sendMessage(winMsg);
                    // Display a win overlay message in the UI.
                    Platform.runLater(() -> {
                        Label winLabel = new Label("You won the game!");
                        winLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: green; -fx-background-color: rgba(255,255,255,0.8);");
                        winLabel.setAlignment(Pos.CENTER);
                        CentralGraphicalUnit.getInstance().addNode(winLabel);
                    });
                    break;
                }
            }
        }
    }

    @Override
    public void myUpdateLocal() {
        // Not used.
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        // Door does not process global updates.
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(getX(), getY(), getWidth(), getHeight());
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), getX() + getWidth() / 2 - textWidth / 2, getY() - 5);
    }

    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{getName(), x, y, width, height, getGameId()};
    }

    @Override
    public float getX() { return x; }
    @Override
    public float getY() { return y; }
    @Override
    public float getWidth() { return width; }
    @Override
    public float getHeight() { return height; }
    @Override
    public void setX(float x) { this.x = x; }
    @Override
    public void setY(float y) { this.y = y; }
    @Override
    public void setWidth(float width) { this.width = width; }
    @Override
    public void setHeight(float height) { this.height = height; }
}
