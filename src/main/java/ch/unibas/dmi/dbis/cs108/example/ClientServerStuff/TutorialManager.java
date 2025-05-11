package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.input.KeyCode;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import java.util.Set;

/**
 * Handles the step‑by‑step keyboard tutorial.
 * Each step completes when the expected key is pressed (detected on release).
 */
public class TutorialManager {

    private final String[] steps = {
        "Press Right Arrow Key to move right",
        "Press Left Arrow Key to move left",
        "Press Up Arrow Key to jump",
        "Press E to grab",
        "Press F to enter throwing mode",
        "Press R to throw"
    };

    private int pointer = 0;                       // current step index

    /** Call once every frame   (ONLY while startedFlag == false). */
    public void update() {
        if (pointer >= steps.length) return;       // tutorial finished

        Set<KeyCode> released = KeyboardState.getAndClearReleasedKeys();
        if (released.isEmpty()) return;

        switch (pointer) {
            case 0 -> { if (released.contains(KeyCode.RIGHT)) pointer++; }
            case 1 -> { if (released.contains(KeyCode.LEFT))  pointer++; }
            case 2 -> { if (released.contains(KeyCode.UP))    pointer++; }
            case 3 -> { if (released.contains(KeyCode.E))     pointer++; }
            case 4 -> { if (released.contains(KeyCode.F))     pointer++; }
            case 5 -> { if (released.contains(KeyCode.R))     pointer++; }
        }
    }

    /** Renders the current hint plus a pointer arrow. */
    public void draw(GraphicsContext gc) {
        if (pointer >= steps.length) return;       // nothing left to show

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 18));

        double baseX = 100;
        double baseY = 100;

        // Draw arrow
        //gc.fillText("→", baseX, baseY);

        // Draw current instruction
        gc.fillText(steps[pointer], baseX + 25, baseY);

        gc.restore(); 
    }
}
