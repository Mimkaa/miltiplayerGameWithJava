package ch.unibas.dmi.dbis.cs108.example.Cube;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CubeDrawer {

    private double angleX = 0;  // Rotation angle around X-axis
    private double angleY = 0;  // Rotation angle around Y-axis

    public double getAngleX() {
        return angleX;
    }

    public void setAngleX(double angleX) {
        this.angleX = angleX;
    }

    public double getAngleY() {
        return angleY;
    }

    public void setAngleY(double angleY) {
        this.angleY = angleY;
    }

    /**
     * Draws a 3D-like cube on a 2D canvas by applying perspective projection.
     */
    public void drawCube(GraphicsContext gc, double centerX, double centerY, double fov, double size) {

        System.out.println("drawing cube with fov " + fov + "and size " + size);
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

        // Apply rotation to the vertices
        double[][] rotatedVertices = new double[8][3];  // Store rotated vertices

        for (int i = 0; i < 8; i++) {
            double x = vertices[i][0];
            double y = vertices[i][1];
            double z = vertices[i][2];

            // Rotate around the X-axis
            double tempY = y * Math.cos(angleX) - z * Math.sin(angleX);
            double tempZ = y * Math.sin(angleX) + z * Math.cos(angleX);
            y = tempY;
            z = tempZ;

            // Rotate around the Y-axis
            double tempX = x * Math.cos(angleY) + z * Math.sin(angleY);
            z = -x * Math.sin(angleY) + z * Math.cos(angleY);
            x = tempX;

            // Store rotated vertices
            rotatedVertices[i][0] = x;
            rotatedVertices[i][1] = y;
            rotatedVertices[i][2] = z;
        }

        // Apply perspective projection to the rotated vertices
        double[][] projectedVertices = new double[8][2];
        for (int i = 0; i < 8; i++) {
            double x = rotatedVertices[i][0];
            double y = rotatedVertices[i][1];
            double z = rotatedVertices[i][2];

            // Perspective projection
            double projectedX = x / (z / fov + 1) + centerX;
            double projectedY = y / (z / fov + 1) + centerY;

            projectedVertices[i][0] = projectedX;
            projectedVertices[i][1] = projectedY;
        }

        // Draw edges of the cube
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        drawCubeEdges(gc, projectedVertices);
    }

    /**
     * Draws the edges of the cube by connecting the projected vertices.
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

    public double[][] getProjectedVertices(double fov, double size, double centerX, double centerY) {
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

        // Apply rotation to the vertices
        double[][] rotatedVertices = new double[8][3];  // Store rotated vertices

        for (int i = 0; i < 8; i++) {
            double x = vertices[i][0];
            double y = vertices[i][1];
            double z = vertices[i][2];

            // Rotate around the X-axis
            double tempY = y * Math.cos(angleX) - z * Math.sin(angleX);
            double tempZ = y * Math.sin(angleX) + z * Math.cos(angleX);
            y = tempY;
            z = tempZ;

            // Rotate around the Y-axis
            double tempX = x * Math.cos(angleY) + z * Math.sin(angleY);
            z = -x * Math.sin(angleY) + z * Math.cos(angleY);
            x = tempX;

            // Store rotated vertices
            rotatedVertices[i][0] = x;
            rotatedVertices[i][1] = y;
            rotatedVertices[i][2] = z;
        }

        // Apply perspective projection to the rotated vertices
        double[][] projectedVertices = new double[8][2];
        for (int i = 0; i < 8; i++) {
            double x = rotatedVertices[i][0];
            double y = rotatedVertices[i][1];
            double z = rotatedVertices[i][2];

            // Perspective projection
            double projectedX = x / (z / fov + 1) + centerX;
            double projectedY = y / (z / fov + 1) + centerY;

            projectedVertices[i][0] = projectedX;
            projectedVertices[i][1] = projectedY;
        }

        return projectedVertices;
    }

    public boolean isMouseOverCube(double mx, double my,
                                   double centerX, double centerY,
                                   double fov, double size) {

        double[][] v = getProjectedVertices(fov, size, centerX, centerY);

        // Front-Face = Vertices 0–3
        double minX = Math.min(Math.min(v[0][0], v[1][0]), Math.min(v[2][0], v[3][0]));
        double maxX = Math.max(Math.max(v[0][0], v[1][0]), Math.max(v[2][0], v[3][0]));
        double minY = Math.min(Math.min(v[0][1], v[1][1]), Math.min(v[2][1], v[3][1]));
        double maxY = Math.max(Math.max(v[0][1], v[1][1]), Math.max(v[2][1], v[3][1]));

        return mx >= minX && mx <= maxX && my >= minY && my <= maxY;
    }

    /** +10 radiant */
    public void rotateBy(double deg) {
        double rad = Math.toRadians(deg);
        angleX += rad;
        angleY += rad;
    }

}