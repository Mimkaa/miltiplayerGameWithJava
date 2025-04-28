package ch.unibas.dmi.dbis.cs108.example.gameLogicTests;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GravityEngine;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.IGravityAffected;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PhysicsAndCollisionTest {
    /**
     * A stub GameObject that also implements IGravityAffected.
     * It simply accumulates the total deltaTime passed to applyGravity().
     */
    static class StubGravityObject extends GameObject implements IGravityAffected {
        float totalGravityTime = 0f;

        StubGravityObject(String name, String gameId) {
            super(name, gameId);
        }

        @Override
        public float getMass() {
            return 0;
        }

        @Override public void applyGravity(float deltaTime) {
            totalGravityTime += deltaTime;
        }

        // Minimal implementations of abstract methods:
        @Override public float getX()                     { return 0; }
        @Override public float getY()                     { return 0; }
        @Override public float getWidth()                 { return 0; }
        @Override public float getHeight()                { return 0; }
        @Override public void setX(float x)               { }
        @Override public void setY(float y)               { }
        @Override public void setWidth(float width)       { }
        @Override public void setHeight(float height)     { }
        @Override public void myUpdateLocal()             { }
        @Override public void myUpdateLocal(float dt)     { }


        @Override protected void myUpdateGlobal(Message m) { }
        @Override public void draw(javafx.scene.canvas.GraphicsContext gc) { }
        @Override public Object[] getConstructorParamValues() { return new Object[0]; }
        @Override public Message createSnapshot()         { return null; }
    }

    /**
     * A plain GameObject stub (not gravity affected) to test that it is ignored by GravityEngine.
     */
    static class StubPlainObject extends GameObject {
        StubPlainObject(String name, String gameId) {
            super(name, gameId);
        }
        // Minimal implementations:
        @Override public float getX()                     { return 0; }
        @Override public float getY()                     { return 0; }
        @Override public float getWidth()                 { return 0; }
        @Override public float getHeight()                { return 0; }
        @Override public void setX(float x)               { }
        @Override public void setY(float y)               { }
        @Override public void setWidth(float width)       { }
        @Override public void setHeight(float height)     { }
        @Override public void myUpdateLocal()             { }
        @Override public void myUpdateLocal(float dt)     { }
        @Override protected void myUpdateGlobal(Message m) { }
        @Override public void draw(javafx.scene.canvas.GraphicsContext gc) { }
        @Override public Object[] getConstructorParamValues() { return new Object[0]; }
        @Override public Message createSnapshot()         { return null; }
    }

    @Test
    void testGravityEngine_appliesOnlyToGravityAffected() {
        StubGravityObject gravObj = new StubGravityObject("g", "game");
        StubPlainObject     plain = new StubPlainObject("p", "game");

        List<GameObject> all = Arrays.asList(gravObj, plain);

        float dt = 0.016f;  // 1 frame at 60fps
        GravityEngine.updateGravity(all, dt);

        // Grav object should have exactly dt added
        assertEquals(dt, gravObj.totalGravityTime, 1e-6,
                "GravityEngine should call applyGravity(dt) on IGravityAffected");

        // Plain object does not have that field—just ensure no exception, and gravObj only changed
        // (we can't inspect plain, but absence of errors and correct gravObj is enough)
    }


    //
    // Collision resolution tests
    //

    static class StubCollisionObject extends GameObject {
        float x, y, w, h;

        StubCollisionObject(String name, String gameId, float x, float y, float w, float h) {
            super(name, gameId);
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

        @Override public float getX()         { return x; }
        @Override public float getY()         { return y; }
        @Override public float getWidth()     { return w; }
        @Override public float getHeight()    { return h; }
        @Override public void setX(float x)   { this.x = x; }
        @Override public void setY(float y)   { this.y = y; }
        @Override public void setWidth(float width)  { this.w = width; }
        @Override public void setHeight(float height){ this.h = height; }
        @Override public void myUpdateLocal()          { }
        @Override public void myUpdateLocal(float dt)  { }
        @Override protected void myUpdateGlobal(Message m){ }
        @Override public void draw(javafx.scene.canvas.GraphicsContext gc){}
        @Override public Object[] getConstructorParamValues(){ return new Object[0]; }
        @Override public Message createSnapshot()      { return null; }
    }

    @Test
    void testResolveCollision_twoMovableSymmetric() {
        // A at (0,0) size 4x4; B at (3,0) size 4x4 => overlapX=1, overlapY=4
        StubCollisionObject a = new StubCollisionObject("A","g", 0,0,4,4);
        StubCollisionObject b = new StubCollisionObject("B","g", 3,0,4,4);

        // both movable (default)
        a.resolveCollision(b);

        // each should move by half the overlap (0.5)
        assertEquals(-0.5f, a.getX(), 1e-6, "A should move left by half overlap");
        assertEquals(3.5f, b.getX(), 1e-6, "B should move right by half overlap");
    }

    @Test
    void testResolveCollision_bothStatic_noMovement() {
        StubCollisionObject a = new StubCollisionObject("A","g", 0,0,4,4);
        StubCollisionObject b = new StubCollisionObject("B","g", 3,0,4,4);

        // make both non-movable
        a.setMovable(false);
        b.setMovable(false);

        a.resolveCollision(b);
        // Positions should remain unchanged
        assertEquals(0f, a.getX(), 1e-6);
        assertEquals(3f, b.getX(), 1e-6);
    }

    @Test
    void testResolveCollision_vertical() {
        // A at (0,0) 4×4; B at (0,3) 4×4 => overlapY=1
        StubCollisionObject a = new StubCollisionObject("A","g", 0,0,4,4);
        StubCollisionObject b = new StubCollisionObject("B","g", 0,3,4,4);

        a.resolveCollision(b);
        // vertical axis is smaller (1 < 4), so each moves by 0.5
        assertEquals(-0.5f, a.getY(), 1e-6, "A should move up by half overlap");
        assertEquals(3.5f, b.getY(), 1e-6, "B should move down by half overlap");
    }
}
