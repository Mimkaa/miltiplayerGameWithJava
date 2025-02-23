package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

public class Player {
    private String name;
    private float x;
    private float y;
    private float radius;

    public Player(String name, float x, float y, float radius) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
    
    @Override
    public String toString() {
        return "Player{" +
               "name='" + name + '\'' +
               ", x=" + x +
               ", y=" + y +
               ", radius=" + radius +
               '}';
    }
}
