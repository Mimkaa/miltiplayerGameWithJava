package ch.unibas.dmi.dbis.cs108.example.Cube;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CubeDrawer {
    /**
     * Draws a 3D-like cube on a 2D canvas by applying perspective projection.
     *
     * @param gc The GraphicsContext on which the cube will be drawn.
     * @param centerX The x-coordinate of the center of the canvas.
     * @param centerY The y-coordinate of the center of the canvas.
     * @param fov The field of view (controls how "deep" the cube appears).
     * @param size The size of the cube.
     */
    public void drawCube(GraphicsContext gc, double centerX, double centerY, double fov, double size) {
        // Cube vertices in 3D space (x, y, z)
        double[][] vertices = {
                {-size, -size, -size}, // Front-top-left
                {size, -size, -size},  // Front-top-right
                {size, size, -size},   // Front-bottom-right
                {-size, size, -size},  // Front-bottom-left
                {-size, -size, size},  // Back-top-left
                {size, -size, size},   // Back-top-right
                {size, size, size},    // Back-bottom-right
                {-size, size, size}    // Back-bottom-left
        };

        // Apply perspective transformation to each vertex
        double[][] projectedVertices = new double[8][2];
        for (int i = 0; i < 8; i++) {
            double x = vertices[i][0];
            double y = vertices[i][1];
            double z = vertices[i][2];

            // Perspective projection (simple version)
            double projectedX = x / (z / fov + 1) + centerX;
            double projectedY = y / (z / fov + 1) + centerY;

            projectedVertices[i][0] = projectedX;
            projectedVertices[i][1] = projectedY;
        }

        // Set line color
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        // Draw edges of the cube (connecting the appropriate vertices)
        drawCubeEdges(gc, projectedVertices);
    }

    /**
     * Draws the edges of the cube by connecting the projected vertices.
     *
     * @param gc The GraphicsContext on which the cube edges will be drawn.
     * @param vertices The 2D projected vertices of the cube.
     */
    private void drawCubeEdges(GraphicsContext gc, double[][] vertices) {
        // Connect front face (vertices 0, 1, 2, 3)
        gc.strokeLine(vertices[0][0], vertices[0][1], vertices[1][0], vertices[1][1]);
        gc.strokeLine(vertices[1][0], vertices[1][1], vertices[2][0], vertices[2][1]);
        gc.strokeLine(vertices[2][0], vertices[2][1], vertices[3][0], vertices[3][1]);
        gc.strokeLine(vertices[3][0], vertices[3][1], vertices[0][0], vertices[0][1]);

        // Connect back face (vertices 4, 5, 6, 7)
        gc.strokeLine(vertices[4][0], vertices[4][1], vertices[5][0], vertices[5][1]);
        gc.strokeLine(vertices[5][0], vertices[5][1], vertices[6][0], vertices[6][1]);
        gc.strokeLine(vertices[6][0], vertices[6][1], vertices[7][0], vertices[7][1]);
        gc.strokeLine(vertices[7][0], vertices[7][1], vertices[4][0], vertices[4][1]);

        // Connect front to back (edges between vertices 0-4, 1-5, 2-6, 3-7)
        gc.strokeLine(vertices[0][0], vertices[0][1], vertices[4][0], vertices[4][1]);
        gc.strokeLine(vertices[1][0], vertices[1][1], vertices[5][0], vertices[5][1]);
        gc.strokeLine(vertices[2][0], vertices[2][1], vertices[6][0], vertices[6][1]);
        gc.strokeLine(vertices[3][0], vertices[3][1], vertices[7][0], vertices[7][1]);
    }
}