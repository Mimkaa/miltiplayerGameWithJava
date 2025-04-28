package ch.unibas.dmi.dbis.cs108.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StartGameTest {

    private static Process serverProcess;
    private static Path jar =
            Paths.get("build","libs","Think_Outside_The_Room-0.0.1-ALPHA-all.jar");


    @BeforeAll
    static void launchServer() throws Exception {
        assertTrue(Files.exists(jar),
                "Expected to find jar at " + jar.toAbsolutePath());

        serverProcess = new ProcessBuilder(
                "java", "-jar", jar.toString(),
                "server", "8888"
        )
                .redirectErrorStream(true)
                .inheritIO()
                .start();

        // give the server a moment to start up
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
    void serverShouldBindUdpPort8888() {
        // Try to bind our own UDP socket on 8888. If the server holds it, we'll get a SocketException.
        boolean boundByServer;
        try (DatagramSocket probe = new DatagramSocket(8888)) {
            // if we get here, the port was NOT bound by the server
            boundByServer = false;
        } catch (SocketException e) {
            // expected: server has bound UDP/8888
            boundByServer = true;
        }

        assertTrue(boundByServer, "Server did not bind UDP port 8888");
    }

    @Test
    void clientShouldStartAndNotImmediatelyExit() throws Exception {
        // Launch the client against our running server
        Process clientProcess = new ProcessBuilder(
                "java", "-jar", jar.toString(),
                "client", "localhost:8888", "testUser"
        )
                .redirectErrorStream(true)
                .inheritIO()
                .start();

        try {
            // Give the client a moment to attempt startup
            Thread.sleep(500);

            // If the client has already exited, that's a failure.
            try {
                int exitCode = clientProcess.exitValue();
                fail("Client exited immediately with code " + exitCode);
            } catch (IllegalThreadStateException ise) {
                // process is still running => good
            }
        } finally {
            // Clean up the client
            clientProcess.destroy();
            if (!clientProcess.waitFor(2, TimeUnit.SECONDS)) {
                clientProcess.destroyForcibly();
            }
        }
    }
}
