package ch.unibas.dmi.dbis.cs108.example.gameObjects;

public interface IGrabbable {
    void onGrab(String playerId);
    void onRelease();
    boolean isGrabbed();
    String getGrabbedBy();

    String getName();
}
