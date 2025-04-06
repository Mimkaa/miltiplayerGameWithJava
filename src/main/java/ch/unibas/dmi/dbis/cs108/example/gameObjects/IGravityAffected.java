package ch.unibas.dmi.dbis.cs108.example.gameObjects;

public interface IGravityAffected {
    float getMass();
    void applyGravity(float deltaTime);
}
