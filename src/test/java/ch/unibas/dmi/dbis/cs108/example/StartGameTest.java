package ch.unibas.dmi.dbis.cs108.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StartGameTest {

    private static Process serverProcess;
    private static final Path JAR =
            Paths.get("build","libs","Think_Outside_The_Room-0.0.1-ALPHA-all.jar");
    private static final int SERVER_PORT = 8888;

    @BeforeAll
    static void launchServer() throws Exception {
        assertTrue(Files.exists(JAR),
                "Expected to find jar at " + JAR.toAbsolutePath());

        serverProcess = new ProcessBuilder(
                "java","-jar", JAR.toString(),
                "server", String.valueOf(SERVER_PORT)
        )
                .redirectErrorStream(true)
                .inheritIO()
                .start();

        // kurz warten, bis der Server hochgefahren ist
        Thread.sleep(1000);
    }

    @AfterAll
    static void tearDownServer() throws Exception {
        if (serverProcess != null) {
            serverProcess.destroy();
            if (!serverProcess.waitFor(3, TimeUnit.SECONDS)) {
                serverProcess.destroyForcibly();
            }
        }
    }

    @Test
    void serverShouldBindUdpPort8888() throws Exception {
        try (DatagramSocket probe = new DatagramSocket(SERVER_PORT)) {
            fail("Port " + SERVER_PORT + " war nicht vom Server belegt");
        } catch (SocketException e) {
            // gewuenscht: Server belegt den Port
        }
    }

    @Test
    void clientHeadlessCreatesAndJoinsGame() throws Exception {
        String user     = "testUser";
        String gameName = "MyGame";

        Process client = new ProcessBuilder(
                "java","-jar", JAR.toString(),
                "client-headless",
                "localhost",
                String.valueOf(SERVER_PORT),
                user,
                gameName
        )
                .redirectErrorStream(true)
                .start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream())
        );

        String line;
        boolean joined = false;
        long deadline = System.currentTimeMillis() + 5000;  // Timeout 5s

        while (System.currentTimeMillis() < deadline
                && (line = reader.readLine()) != null) {
            if (line.contains("Joined game: " + gameName)) {
                joined = true;
                break;
            }
        }

        client.destroy();
        client.waitFor(2, TimeUnit.SECONDS);

        assertTrue(joined, "Client hat das Game nicht erfolgreich erstellt und gejoint");
    }
}
