package ch.unibas.dmi.dbis.cs108.example.gameLogicTests;

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

/**
 * Integration tests for starting the server and headless client.
 * <p>
 * Verifies that:
 * <ul>
 *   <li>The server binds to the configured UDP port.</li>
 *   <li>A headless client invocation can create and join a new game session.</li>
 * </ul>
 * </p>
 */
class StartGameTest {

    /** Handle to the server process. */
    private static Process serverProcess;

    /** Path to the executable fat-jar. */
    private static final Path JAR =
            Paths.get("build","libs","Think_Outside_The_Room-0.0.1-ALPHA-all.jar");

    /** UDP port that the server should bind to. */
    private static final int SERVER_PORT = 8888;

    /**
     * Starts the server in a background process before all tests.
     * <p>Fails if the JAR cannot be found or the server does not launch.</p>
     */
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

        // Give the server a moment to finish startup.
        Thread.sleep(1000);
    }

    /**
     * Stops the server process after all tests have run.
     * <p>Attempts a graceful shutdown, then forcibly destroys if necessary.</p>
     */
    @AfterAll
    static void tearDownServer() throws Exception {
        if (serverProcess != null) {
            serverProcess.destroy();
            if (!serverProcess.waitFor(3, TimeUnit.SECONDS)) {
                serverProcess.destroyForcibly();
            }
        }
    }

    /**
     * Verifies that the server has bound the expected UDP port.
     * <p>Attempting to bind the same port locally should fail with a {@link SocketException}.</p>
     */
    @Test
    void serverShouldBindUdpPort8888() throws Exception {
        try (DatagramSocket probe = new DatagramSocket(SERVER_PORT)) {
            fail("Port " + SERVER_PORT + " was not bound by the server");
        } catch (SocketException e) {
            // Expected: server holds the port
        }
    }

    /**
     * Launches a headless client that creates and joins a game, then
     * asserts that the confirmation line is printed to standard out.
     */
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
        long deadline = System.currentTimeMillis() + 5000;  // 5-second timeout

        while (System.currentTimeMillis() < deadline
                && (line = reader.readLine()) != null) {
            if (line.contains("Joined game: " + gameName)) {
                joined = true;
                break;
            }
        }

        client.destroy();
        client.waitFor(2, TimeUnit.SECONDS);

        assertTrue(joined, "Client did not successfully create and join the game");
    }
}
