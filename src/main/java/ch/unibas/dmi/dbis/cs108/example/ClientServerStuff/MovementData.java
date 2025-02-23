package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

public class MovementData {
    private String type;
    private String playerName;
    private float xoffset;
    private float yoffset;

    // Constructor with type, playerName, xoffset, and yoffset.
    public MovementData(String type, String playerName, float xoffset, float yoffset) {
        this.type = type;
        this.playerName = playerName;
        this.xoffset = xoffset;
        this.yoffset = yoffset;
    }

    public String getType() {
        return type;
    }

    public String getPlayerName() {
        return playerName;
    }

    public float getXoffset() {
        return xoffset;
    }

    public float getYoffset() {
        return yoffset;
    }

    @Override
    public String toString() {
        return "MovementData{" +
               "type='" + type + '\'' +
               ", playerName='" + playerName + '\'' +
               ", xoffset=" + xoffset +
               ", yoffset=" + yoffset +
               '}';
    }
}
