package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

public class MovementCodec {
    public static final String MOVE_TYPE = "MOVE";
    
    // Encodes a movement message by including the type, player's name, and the two parameters.
    public static String encodeMovement(String playerName, float xoffset, float yoffset) {
        return MOVE_TYPE + "," + playerName + "," + xoffset + "," + yoffset;
    }
    
    // Decodes a movement message and returns a MovementData object.
    public static MovementData decodeMovement(String data) {
        String[] parts = data.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid movement data: " + data);
        }
        String type = parts[0];
        if (!MOVE_TYPE.equals(type)) {
            throw new IllegalArgumentException("Unexpected message type: " + type);
        }
        String playerName = parts[1];
        float xoffset = Float.parseFloat(parts[2]);
        float yoffset = Float.parseFloat(parts[3]);
        return new MovementData(type, playerName, xoffset, yoffset);
    }
}
