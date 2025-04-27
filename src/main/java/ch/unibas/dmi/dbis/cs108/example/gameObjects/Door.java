package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
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
    private boolean hasWon = false;

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
                    if (!hasWon) {
                        hasWon = true;
                        System.out.println("You won the game!");

                        LevelTimer levelTimer = LevelTimer.getInstance();
                        levelTimer.stop();
                        float elapsedTime = LevelTimer.getInstance().getElapsedTimeInSeconds();

                        // sends message that the level is finished
                        if (parentGame.isAuthoritative()) {
                        Message winMsg = new Message("WIN", new Object[]{"You won the game!",elapsedTime}, "RESPONSE");

                        Server.getInstance().broadcastMessageToAll(winMsg);
                        System.out.println("sending Win Message");
                        break;
}                   }
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
        //not used
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
