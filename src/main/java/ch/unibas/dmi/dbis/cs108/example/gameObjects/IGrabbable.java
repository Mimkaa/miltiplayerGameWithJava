package ch.unibas.dmi.dbis.cs108.example.gameObjects;

public interface IGrabbable {
    void setVelocity(float vx, float vy);
    void onGrab(String playerId);
    void onRelease();
    boolean isGrabbed();
    String getGrabbedBy();
    void setPos(float x, float y);
    String getName();
}
