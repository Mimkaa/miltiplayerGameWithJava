package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;

public class GrabbableProcessor implements FeatureProcessor {
    private static final float GRAB_RADIUS = 50.0f;

    @Override
    public void process(Game game, float dt) {
        for (GameObject go : game.getGameObjects()) {
            if (!(go instanceof IGrabbable)) continue;
            IGrabbable g = (IGrabbable) go;

            // Wenn gerade gegriffenes Objekt – an Spieler haengen
            if (g.isGrabbed()) {
                // 1) Spieler-Objekt finden
                GameObject player = game.getGameObjects().stream()
                        .filter(o -> o.getId().equals(g.getGrabbedBy()))
                        .findFirst().orElse(null);
                if (player == null) continue;

                // 2) Geometrie-Methoden auf GameObject aufrufen
                float px = player.getX();
                float py = player.getY();

                // Entweder so:
                float objWidth  = go.getWidth();
                float objHeight = go.getHeight();

                // … oder explizit casten:
                // GameObject grabbedObj = (GameObject) g;
                // float objWidth  = grabbedObj.getWidth();
                // float objHeight = grabbedObj.getHeight();

                // 3) Position setzen (IGrabbable kennt nur setPos)
                g.setPos(
                        px + player.getWidth() / 2 - objWidth  / 2,
                        py                      - objHeight
                );
            }
        }
    }


    /**
     *      Can also be outsourced for ‘Press E button’ - here only passive updating.
     */
}
