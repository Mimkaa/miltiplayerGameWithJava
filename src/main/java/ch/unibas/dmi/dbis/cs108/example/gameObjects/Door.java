package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.highscore.LevelTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

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
        Game currentGame = getParentGame();
        if (currentGame == null) return;

        for (GameObject go : currentGame.getGameObjects()) {
            if (go instanceof Key) {
                Key key = (Key) go;
                if (this.intersects(key)) {
                    System.out.println("You won the game!");

                    LevelTimer levelTimer = LevelTimer.getInstance();
                    levelTimer.stop();
                    long elapsedTime = levelTimer.getElapsedTimeInSeconds();

                    // shows a panel if the level is finished
                    Platform.runLater(() -> {
                        String message = "Level Completed and Time: " + elapsedTime + " seconds";

                        // shows panel
                        Label winLabel = new Label(message);
                        winLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: #008011; -fx-background-color: rgba(255,255,255,0.8);");
                        winLabel.setAlignment(Pos.CENTER);
                        CentralGraphicalUnit.getInstance().addNode(winLabel);

                        // save the time in a txt. file
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter("level_time.txt", true))) {
                            for (String username : currentGame.getUsers()) {
                                writer.write(username + " completed the Level in " + elapsedTime + " seconds \n");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                    // sends message that the level is finished
                    Message winMsg = new Message("WIN", new Object[]{"You won the game!"}, null);
                    sendMessage(winMsg);
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
    public float getX() { 
        return x; 
    }
    @Override
    public float getY() { 
        return y; 
    }
    @Override
    public float getWidth() { 
        return width; 
    }
    @Override
    public float getHeight() { 
        return height; 
    }
    @Override
    public void setX(float x) { 
        this.x = x; 
    }
    @Override
    public void setY(float y) { 
        this.y = y; 
    }
    @Override
    public void setWidth(float width) { 
        this.width = width; 
    }
    @Override
    public void setHeight(float height) { 
        this.height = height; 
    }
    @Override
    public Message createSnapshot() {
        // Pack the position, velocity, and acceleration into an Object array.
        Object[] params = new Object[]{
            x, y,   // Positionvelocity.x, velocity.y,   // Velocity
            
        };
        // Create a new message with type "SNAPSHOT" and an appropriate option (e.g., "UPDATE").
        Message snapshotMsg = new Message("SNAPSHOT", params, "GAME");
        
        // Set the concealed parameters so receivers know the source of the snapshot.
        snapshotMsg.setConcealedParameters(new String[]{ getId(), getGameId() });
        
        return snapshotMsg;
    }
}
